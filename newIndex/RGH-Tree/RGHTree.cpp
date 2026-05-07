// RGHTree.cpp
#include "../../../include/index_structure/RGH-Tree/RGHTree.h"
#include "../../../include/index_structure/RGH-Tree/RGHInternalNode.h"
#include "../../../include/index_structure/RGH-Tree/RGHLeafNode.h"
#include "../../../include/interfaces/MetricDistance.h"
#include "../../../include/utils/MetricSpaceSearch.h"
#include <iostream>
#include <algorithm>
#include <limits>
#include <random>
#include <array> 

const int MaxRGHLeafSize = 20; // 与 VPT 相同的叶子大小阈值
long long RGHTree::distanceCalculations_ = 0;

std::unique_ptr<RGHNode> RGHTree::bulkLoad(
    const std::vector<std::shared_ptr<MetricData>>& data,
    int distanceType,
    int dataType,
    PivotSelector::SelectionMethod method
) {
    if (data.size() <= MaxRGHLeafSize) {
        return std::make_unique<RGHLeafNode>(data, distanceType, dataType, method);
    }

    auto dist = MetricSpaceSearch::createDistanceFunction(distanceType, dataType);

    // 1. 选择参考点对
    std::vector<int> indices = PivotSelector::selectPivots(data, 2, dist, method, 2);
    DataPtr leftPivot = data[indices[0]];
    DataPtr rightPivot = data[indices[1]];

    // 2. 计算分割比率
    long double splitRatio = calculateSplitRatio(data, leftPivot, rightPivot, dist);

    // 3. 划分数据
    std::vector<std::shared_ptr<MetricData>> leftData, rightData;
    partitionData(data, leftPivot, rightPivot, splitRatio, leftData, rightData, dist);

    // 4. 递归构建子树 (先构建子树，以便获取子树的元数据)
    auto leftChild = bulkLoad(leftData, distanceType, dataType, method);
    auto rightChild = bulkLoad(rightData, distanceType, dataType, method);

    // 5. 计算左右子树的覆盖半径 (dl, dr)
    long double leftSubtreeRadius = 0.0L;
    for (const auto& item : leftData) {
        long double d = dist->distance(*item, *leftPivot);
        leftSubtreeRadius = std::max(leftSubtreeRadius, d);
    }
    long double rightSubtreeRadius = 0.0L;
    for (const auto& item : rightData) {
        long double d = dist->distance(*item, *rightPivot);
        rightSubtreeRadius = std::max(rightSubtreeRadius, d);
    }

    // 6. 【关键修改】计算子节点参考点到当前节点参考点的距离
    // 我们需要计算:
    // d(Pl, V.left.Pl), d(Pl, V.left.Pr)
    // d(Pr, V.right.Pl), d(Pr, V.right.Pr)
    std::array<long double, 4> childDists = {};

    // 只有当子节点是内部节点时，才有 getLeftPivot/RightPivot
    // 如果子节点是叶子节点，这些距离概念上不存在或为0，但在搜索逻辑中，
    // 如果子节点是叶子，我们通常直接遍历，不会用到这种深层剪枝，或者可以设一个极大值跳过检查。
    // 为了简化，这里假设子节点也是内部节点（因为数据量大时大部分是）。
    // 实际工程中需要 dynamic_cast 判断类型。

    auto* leftInt = dynamic_cast<RGHInternalNode*>(leftChild.get());
    auto* rightInt = dynamic_cast<RGHInternalNode*>(rightChild.get());

    // 默认设为 -1 表示无效（即如果是叶子节点）
    childDists[0] = leftInt ? dist->distance(*leftPivot, *leftInt->getLeftPivot()) : -1.0L;
    childDists[1] = leftInt ? dist->distance(*leftPivot, *leftInt->getRightPivot()) : -1.0L;
    childDists[2] = rightInt ? dist->distance(*rightPivot, *rightInt->getLeftPivot()) : -1.0L;
    childDists[3] = rightInt ? dist->distance(*rightPivot, *rightInt->getRightPivot()) : -1.0L;

    return std::make_unique<RGHInternalNode>(
        leftPivot, rightPivot, splitRatio,
        leftSubtreeRadius, rightSubtreeRadius,
        childDists,
        std::move(leftChild), std::move(rightChild), dist
        );
}

long double RGHTree::calculateSplitRatio(
    const std::vector<std::shared_ptr<MetricData>>& data,
    const std::shared_ptr<MetricData>& leftPivot,
    const std::shared_ptr<MetricData>& rightPivot,
    const std::shared_ptr<MetricDistance>& dist
) {
    std::vector<long double> ratios;
    for (const auto& item : data) {
        if (item == leftPivot || item == rightPivot) continue;

        long double dist_lp = dist->distance(*item, *leftPivot);
        long double dist_rp = dist->distance(*item, *rightPivot);

        if (dist_rp != 0.0L) {
            ratios.push_back(dist_lp / dist_rp);
        }
    }

    if (ratios.empty()) return 1.0L; // 如果所有比值都无效，则返回默认值

    std::sort(ratios.begin(), ratios.end());
    // 返回比率的中位数
    return ratios[ratios.size() / 2];
}

void RGHTree::partitionData(
    const std::vector<std::shared_ptr<MetricData>>& data,
    const std::shared_ptr<MetricData>& leftPivot,
    const std::shared_ptr<MetricData>& rightPivot,
    long double splitRatio,
    std::vector<std::shared_ptr<MetricData>>& leftData,
    std::vector<std::shared_ptr<MetricData>>& rightData,
    const std::shared_ptr<MetricDistance>& dist
) {
    std::vector<std::shared_ptr<MetricData>> Cl; // Ratio < R
    std::vector<std::shared_ptr<MetricData>> Cr; // Ratio > R
    std::vector<std::shared_ptr<MetricData>> E;  // Ratio == R

    // 1. 遍历数据，将其归入 Cl, Cr, 或 E
    for (const auto& item : data) {
        if (item == leftPivot || item == rightPivot) continue;

        long double dist_lp = dist->distance(*item, *leftPivot);
        long double dist_rp = dist->distance(*item, *rightPivot);

        // 避免除以零
        if (dist_rp == 0.0L) {
            // 如果 dist_rp 为 0 但 dist_lp 不为 0，比率为无穷大，归入 Cr
            // 如果都为 0，则是重合点，通常归入任意一边即可，这里归入 Cr
            Cr.push_back(item);
            continue;
        }

        long double ratio = dist_lp / dist_rp;

        // 使用一个极小值来处理浮点数比较，或者直接使用 long double 的精度
        if (ratio < splitRatio) {
            Cl.push_back(item);
        }
        else if (ratio > splitRatio) {
            Cr.push_back(item);
        }
        else {
            E.push_back(item);
        }
    }

    // 2. 计算需要分配的数量以达到平衡
    // 目标：|Dl| ≈ |Dr|
    // 已知：Dl = Cl + El, Dr = Cr + Er
    // 设 a = |Cl| - |Cr|, b = |E|
    int a = static_cast<int>(Cl.size()) - static_cast<int>(Cr.size());
    int b = static_cast<int>(E.size());

    // 根据论文公式：
    // El 的数量应为 [(b + a + 1) / 2]
    // Er 的数量应为 [(b - a) / 2]
    // 其中 [] 表示取整。在 C++ 中，整数除法即为向下取整。
    int countEl = (b + a + 1) / 2;
    int countEr = (b - a) / 2;

    // 防御性编程：防止因浮点误差或其他原因导致的计算错误（理论上不会发生，但为了安全）
    if (countEl < 0) countEl = 0;
    if (countEr < 0) countEr = 0;
    // 确保总和不超过 E 的大小
    if (countEl + countEr > b) countEl = b - countEr;

    // 3. 分配 E 中的元素
    // 我们只需要简单地将 E 的前 countEl 个给左子树，剩下的给右子树
    // 因为 E 中的元素比率相同，顺序不重要
    for (int i = 0; i < b; ++i) {
        if (i < countEl) {
            leftData.push_back(E[i]);
        }
        else {
            rightData.push_back(E[i]);
        }
    }

    // 4. 加入 Cl 和 Cr
    leftData.insert(leftData.end(), Cl.begin(), Cl.end());
    rightData.insert(rightData.end(), Cr.begin(), Cr.end());

    // 调试检查（可选）：
    // if (abs((int)leftData.size() - (int)rightData.size()) > 1) {
    //     std::cerr << "平衡性破坏！" << std::endl;
    // }
}
void RGHTree::runRGHRangeSearch(
    const std::vector<std::shared_ptr<MetricData>>& dataset,
    int distanceType,
    int dataType
) {
    if (dataset.empty()) {
        std::cerr << "数据集为空，无法执行查询。" << std::endl;
        return;
    }

    std::cout << "\n注意：RGH-Tree 在每个节点使用两个参考点。\n"
        << "算法将在每个层级动态选择这些点。\n";

    PivotSelector::SelectionMethod method = PivotSelector::selectPivotMethodFromUser();

    auto treeRoot = RGHTree::bulkLoad(dataset, distanceType, dataType, method);

    int querySource;
    std::cout << "请选择查询点来源：\n"
        << "1 - 从现有数据集中选择\n"
        << "2 - 自定义输入新查询点\n"
        << "请输入选项编号：";
    std::cin >> querySource;

    if (std::cin.fail() || (querySource != 1 && querySource != 2)) {
        std::cin.clear();
        std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
        std::cout << "输入无效，请选择 1 或 2。\n";
        return;
    }

    std::shared_ptr<MetricData> queryPtr;
    if (querySource == 1) {
        int queryIndex;
        std::cout << "请选择一个查询对象索引（0 到 " << dataset.size() - 1 << "）：";
        std::cin >> queryIndex;

        if (std::cin.fail() || queryIndex < 0 || queryIndex >= static_cast<int>(dataset.size())) {
            std::cin.clear();
            std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
            std::cout << "输入无效，请输入有效的索引范围。\n";
            return;
        }
        queryPtr = dataset[queryIndex];
    }
    else {
        try {
            queryPtr = inputCustomQuery(dataType);
        }
        catch (const std::exception& e) {
            std::cout << "自定义输入失败：" << e.what() << std::endl;
            return;
        }
    }

    long double threshold;
    std::cout << "请输入查询半径 r: ";
    std::cin >> threshold;

    if (threshold < 0) {
        std::cerr << "查询半径不能为负数。" << std::endl;
        return;
    }

    const MetricData& query = *queryPtr;
    RGHTree::resetDistanceCalculations();

    auto results = treeRoot->rangeSearch(query, threshold, &RGHTree::distanceCalculations_);

    // 过滤掉查询对象本身
    std::vector<std::shared_ptr<MetricData>> filteredResults;
    for (const auto& item : results) {
        if (item.get() != queryPtr.get()) {
            filteredResults.push_back(item);
        }
    }

    std::cout << "\n- 查询对象 #" << queryPtr << ": " << queryPtr->toString() << std::endl;
    std::cout << "\n找到匹配项数量（不包括查询对象自身）: " << filteredResults.size() << std::endl;

    if (!filteredResults.empty()) {
        std::cout << "以下是匹配项：" << std::endl;
        for (size_t i = 0; i < filteredResults.size(); ++i) {
            std::cout << "  - 匹配项 #" << i + 1 << ": " << filteredResults[i]->toString() << std::endl;
        }
    }
    else {
        std::cout << "未找到任何匹配项。" << std::endl;
    }

    std::cout << "\n本次查询共调用距离函数: " << RGHTree::getDistanceCalculations() << " 次" << std::endl;
}

void RGHTree::resetDistanceCalculations() {
    distanceCalculations_ = 0;
}

long long RGHTree::getDistanceCalculations() {
    return distanceCalculations_;
}