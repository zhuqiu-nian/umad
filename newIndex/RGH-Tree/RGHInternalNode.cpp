// RGHInternalNode.cpp
#include "../../../include/index_structure/RGH-Tree/RGHTree.h"
#include "../../../include/index_structure/RGH-Tree/RGHInternalNode.h"
#include "../../../include/index_structure/RGH-Tree/RGHLeafNode.h"
#include "../../../include/interfaces/MetricDistance.h"
#include "../../../include/utils/MetricSpaceSearch.h"
#include <iostream>
#include <cmath>
#include <iostream>
#include <array> 

RGHInternalNode::RGHInternalNode(
    std::shared_ptr<MetricData> leftPivot,
    std::shared_ptr<MetricData> rightPivot,
    long double splitRatio,
    long double leftSubtreeRadius,
    long double rightSubtreeRadius,
    const std::array<long double, 4>& childDistances,
    std::unique_ptr<RGHNode> left,
    std::unique_ptr<RGHNode> right,
    std::shared_ptr<MetricDistance> dist
) : leftPivot_(leftPivot), rightPivot_(rightPivot), splitRatio_(splitRatio),
leftSubtreeRadius_(leftSubtreeRadius), rightSubtreeRadius_(rightSubtreeRadius),
leftChild_(std::move(left)), rightChild_(std::move(right)), dist_(dist)
{
    for (int i = 0; i < 4; ++i) childDistances_[i] = childDistances[i];
}

std::vector<std::shared_ptr<MetricData>> RGHInternalNode::rangeSearch(
    const MetricData& q,
    long double r,
    long long* distanceCount
) {
    std::vector<std::shared_ptr<MetricData>> result;

    // --- 定义浮点数容差 ---
    const long double EPS = 1e-12L;

    // (a)(1) 检查当前节点的参考点 Pl 和 Pr
    long double d_q_pl = dist_->distance(q, *leftPivot_);
    long double d_q_pr = dist_->distance(q, *rightPivot_);
    if (distanceCount) (*distanceCount) += 2;

    if (d_q_pl <= r) result.push_back(leftPivot_);
    if (d_q_pr <= r) result.push_back(rightPivot_);

    // ---------------------------------------------------------
    // (a)(2) 尝试剪枝左子树 (若成功则搜右)
    // ---------------------------------------------------------
    // 逻辑：如果 (d(Q,Pl)−r)/(d(Q,Pr)+r) ≥ R，说明查询圆完全偏向右侧，左子树无结果
    if ( (d_q_pl - r) / (d_q_pr + r) >= splitRatio_ - EPS) {

        bool searchRight = false;
        auto* rightIntNode = dynamic_cast<RGHInternalNode*>(rightChild_.get());

        if (rightIntNode) {
            // 【关键修复 1】获取右子节点自身的覆盖半径
            // 这里的 radius_l/r 是右子节点内部定义的，代表其 Pivot 到最远后代的距离
            // 假设你的类里有 getLeftRadius() 和 getRightRadius() 方法
            long double right_child_radius_l = rightIntNode->getLeftRadius();
            long double right_child_radius_r = rightIntNode->getRightRadius();

            // 第二层 If: 预检查 (利用三角不等式估算下界)
            // childDistances_[2] = d(Pr, V.right.Pl)
            // childDistances_[3] = d(Pr, V.right.Pr)
            long double checkValL = std::abs(d_q_pr - childDistances_[2]);
            long double checkValR = std::abs(d_q_pr - childDistances_[3]);

            // 【关键修复 2】使用子节点的半径进行比较
            if (checkValL <= right_child_radius_l + r + EPS ||
                checkValR <= right_child_radius_r + r + EPS) {

                // 第三层 If: 精确检查
                long double d_q_right_pl = dist_->distance(q, *rightIntNode->getLeftPivot());
                long double d_q_right_pr = dist_->distance(q, *rightIntNode->getRightPivot());
                if (distanceCount) (*distanceCount) += 2;

                // 【关键修复 3】再次确保使用子节点的半径
                if (d_q_right_pl <= right_child_radius_l + r + EPS ||
                    d_q_right_pr <= right_child_radius_r + r + EPS) {
                    searchRight = true;

                }
            }
        }
        else {
            // 如果是叶子节点，无法进行基于 pivot 的检查，保守起见必须搜
            searchRight = true;
        }

        if (searchRight) {
            auto res = rightChild_->rangeSearch(q, r, distanceCount);
            result.insert(result.end(), res.begin(), res.end());
        }
    }

    // ---------------------------------------------------------
    // (a)(3) 尝试剪枝右子树 (若成功则搜左)
    // ---------------------------------------------------------
    // 逻辑：如果 (d(Q,Pl)+r)/(d(Q,Pr)−r) ≤ R，说明查询圆完全偏向左侧，右子树无结果
    else if (d_q_pr > r&& (d_q_pl + r) / (d_q_pr - r) <= splitRatio_ + EPS) {

        bool searchLeft = false;
        auto* leftIntNode = dynamic_cast<RGHInternalNode*>(leftChild_.get());

        if (leftIntNode) {
            // 【关键修复 4】获取左子节点自身的覆盖半径
            long double left_child_radius_l = leftIntNode->getLeftRadius();
            long double left_child_radius_r = leftIntNode->getRightRadius();

            // 第二层 If: 预检查
            // childDistances_[0] = d(Pl, V.left.Pl)
            // childDistances_[1] = d(Pl, V.left.Pr)
            long double checkValL = std::abs(d_q_pl - childDistances_[0]);
            long double checkValR = std::abs(d_q_pl - childDistances_[1]);

            // 【关键修复 5】使用子节点的半径进行比较
            if (checkValL <= left_child_radius_l + r + EPS ||
                checkValR <= left_child_radius_r + r + EPS) {

                // 第三层 If: 精确检查
                long double d_q_left_pl = dist_->distance(q, *leftIntNode->getLeftPivot());
                long double d_q_left_pr = dist_->distance(q, *leftIntNode->getRightPivot());
                if (distanceCount) (*distanceCount) += 2;

                // 【关键修复 6】再次确保使用子节点的半径
                if (d_q_left_pl <= left_child_radius_l + r + EPS ||
                    d_q_left_pr <= left_child_radius_r + r + EPS) {
                    searchLeft = true;
                }
            }
        }
        else {
            // 如果是叶子节点，保守起见必须搜
            searchLeft = true;
        }

        if (searchLeft) {
            auto res = leftChild_->rangeSearch(q, r, distanceCount);
            result.insert(result.end(), res.begin(), res.end());
        }
    }

    // ---------------------------------------------------------
    // 兜底：如果都不满足剪枝条件（落入中间模糊地带），两边都搜
    // ---------------------------------------------------------
    else {
        auto leftRes = leftChild_->rangeSearch(q, r, distanceCount);
        auto rightRes = rightChild_->rangeSearch(q, r, distanceCount);
        result.insert(result.end(), leftRes.begin(), leftRes.end());
        result.insert(result.end(), rightRes.begin(), rightRes.end());
    }

    return result;
}

std::vector<std::shared_ptr<MetricData>> RGHInternalNode::getAll() const {
    std::vector<std::shared_ptr<MetricData>> result;
    auto leftAll = leftChild_->getAll();
    auto rightAll = rightChild_->getAll();
    result.insert(result.end(), leftAll.begin(), leftAll.end());
    result.insert(result.end(), rightAll.begin(), rightAll.end());
    return result;
}