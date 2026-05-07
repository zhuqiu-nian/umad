// ApollonianTree.cpp
#include "../../../include/index_structure/ApollonianTree/ApollonianTree.h"
#include "../../../include/index_structure/ApollonianTree/ApollonianInternalNode.h"
#include "../../../include/index_structure/ApollonianTree/ApollonianLeafNode.h"
#include "../../../include/utils/MetricSpaceSearch.h"
#include "../../PivotSelector/PivotSelector.h"
#include "../../../include/utils/Solution.h"
#include <iostream>
#include <algorithm>
#include <random>

const int MaxLeafSize = 20;
long long ApollonianTree::distanceCalculations_ = 0;

std::pair<long double, long double> ApollonianTree::computeOptimalRatios(
    const std::vector<DataPtr>& data,
    const DataPtr& c1,
    const DataPtr& c2,
    const MetricDistance& dist)
{
    if (data.empty()) {
        return { 0.5L, 2.0L };
    }

    std::vector<long double> ratios;
    ratios.reserve(data.size());

    for (const auto& x : data) {
        long double d1 = dist.distance(*c1, *x);
        long double d2 = dist.distance(*c2, *x);

        if (d2 == 0) {
            ratios.push_back(std::numeric_limits<long double>::max());
        }
        else {
            ratios.push_back(d1 / d2);
        }
    }

    std::sort(ratios.begin(), ratios.end());
    size_t n = ratios.size();

    if (n == 1) {
        long double r = ratios[0];
        if (r <= 0 || r == std::numeric_limits<long double>::max()) {
            return { 0.5L, 2.0L };
        }
        return { r * 0.9L, r * 1.1L };
    }

    size_t idx1 = n / 3;
    size_t idx2 = (2 * n) / 3;
    long double c1_ratio = ratios[idx1];
    long double c2_ratio = ratios[idx2];

    if (c1_ratio <= 0) c1_ratio = std::numeric_limits<long double>::min();
    if (c2_ratio <= c1_ratio) {
        c2_ratio = (c1_ratio < 1.0L) ? c1_ratio * 2.0L : c1_ratio + 1.0L;
    }


    return { c1_ratio, c2_ratio };
}

std::vector<std::vector<DataPtr>> ApollonianTree::splitIntoThree(
    const std::vector<DataPtr>& data,
    const DataPtr& c1,
    const DataPtr& c2,
    long double c1_ratio,
    long double c2_ratio,
    const MetricDistance& dist)
{
    std::vector<DataPtr> left, mid, right;
    for (const auto& x : data) {
        long double d1 = dist.distance(*c1, *x);
        long double d2 = dist.distance(*c2, *x);
        if (d2 == 0) {
            right.push_back(x);
        }
        else {
            long double ratio = d1 / d2;
            if (ratio < c1_ratio) {
                left.push_back(x);
            }
            else if (ratio > c2_ratio) {
                right.push_back(x);
            }
            else {
                mid.push_back(x);
            }
        }
    }

    return { left, mid, right };
}

std::unique_ptr<ApollonianNode> ApollonianTree::bulkLoad(
    const std::vector<DataPtr>& data,
    int distanceType,
    int dataType,
    PivotSelector::SelectionMethod method)
{
    if (data.size() <= MaxLeafSize) {
        return std::make_unique<ApollonianLeafNode>(data, distanceType, dataType);
    }

    auto dist = MetricSpaceSearch::createDistanceFunction(distanceType, dataType);

    std::vector<int> indices = PivotSelector::selectPivots(data, 2, dist, method, 2);
    DataPtr c1 = data[indices[0]];
    DataPtr c2 = data[indices[1]];

    std::vector<DataPtr> restData;
    for (const auto& x : data) {
        if (x != c1 && x != c2) {
            restData.push_back(x);
        }
    }

    auto [c1_ratio, c2_ratio] = computeOptimalRatios(restData, c1, c2, *dist);
    auto parts = splitIntoThree(restData, c1, c2, c1_ratio, c2_ratio, *dist);

    auto leftChild = bulkLoad(parts[0], distanceType, dataType, method);
    auto midChild = bulkLoad(parts[1], distanceType, dataType, method);
    auto rightChild = bulkLoad(parts[2], distanceType, dataType, method);

    // === 计算 M1 和 M2 ===
    long double M1 = 0.0L, M2 = 0.0L;

    if (!parts[0].empty()) {
        for (const auto& x : parts[0]) {
            long double d = dist->distance(*c1, *x);
            if (d > M1) M1 = d;
        }
    }

    if (!parts[2].empty()) {
        for (const auto& x : parts[2]) {
            long double d = dist->distance(*c2, *x);
            if (d > M2) M2 = d;
        }
    }

    return std::make_unique<ApollonianInternalNode>(
        c1, c2, c1_ratio, c2_ratio,
        std::move(leftChild),
        std::move(midChild),
        std::move(rightChild),
        std::move(dist),
        M1, M2  // ← 传入 M1, M2
        );
}

// 修改 runApollonianRangeSearch：移除 c1/c2 输入
void ApollonianTree::runApollonianRangeSearch(
    const std::vector<std::shared_ptr<MetricData>>& dataset,
    int distanceType,
    int dataType)
{
    if (dataset.empty()) {
        std::cerr << "数据集为空，无法执行查询。" << std::endl;
        return;
    }

    // 不再要求用户输入 c1/c2！
    PivotSelector::SelectionMethod method = PivotSelector::selectPivotMethodFromUser();

    std::cout << "\n注意：阿波罗尼斯树（AT）使用自适应划分比例（基于数据分布的1/3和2/3分位数），无需手动指定 c1/c2。\n";

    auto treeRoot = ApollonianTree::bulkLoad(dataset, distanceType, dataType, method);

    // ========== 查询点输入逻辑（保持不变）==========
    int querySource;
    std::cout << "\n请选择查询点来源：\n"
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

    // ========== 执行范围搜索 ==========
    const MetricData& query = *queryPtr;
    long long localDistanceCount = 0;
    auto results = treeRoot->rangeSearch(query, threshold, &localDistanceCount);

    // 过滤掉查询点自身
    std::vector<std::shared_ptr<MetricData>> filteredResults;
    for (const auto& item : results) {
        if (item.get() != queryPtr.get()) {
            filteredResults.push_back(item);
        }
    }

    // ========== 输出结果 ==========
    std::cout << "\n- 查询对象 #" << queryPtr << ": " << queryPtr->toString() << std::endl;
    std::cout << "\n找到匹配项数量（不包括查询对象自身）: " << filteredResults.size() << std::endl;

    if (!filteredResults.empty()) {
        std::cout << "以下是匹配项：" << std::endl;
        for (size_t i = 0; i < filteredResults.size(); ++i) {
            std::cout << "  - 匹配项 #" << (i + 1) << ": " << filteredResults[i]->toString() << std::endl;
        }
    }
    else {
        std::cout << "未找到任何匹配项。" << std::endl;
    }

    std::cout << "\n本次查询共调用距离函数: " << localDistanceCount << " 次" << std::endl;
}