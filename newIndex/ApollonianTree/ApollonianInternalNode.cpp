// ApollonianInternalNode.cpp
#include "../../../include/index_structure/ApollonianTree/ApollonianInternalNode.h"
#include <limits>
#include <cmath>

// 构造函数：新增 M1, M2 参数
ApollonianInternalNode::ApollonianInternalNode(
    DataPtr c1,
    DataPtr c2,
    long double c1_ratio,
    long double c2_ratio,
    std::unique_ptr<ApollonianNode> leftChild,
    std::unique_ptr<ApollonianNode> midChild,
    std::unique_ptr<ApollonianNode> rightChild,
    std::shared_ptr<MetricDistance> dist,
    long double M1,
    long double M2)
    : c1_(std::move(c1)), c2_(std::move(c2)),
    c1_ratio_(c1_ratio), c2_ratio_(c2_ratio),
    left_(std::move(leftChild)),
    mid_(std::move(midChild)),
    right_(std::move(rightChild)),
    dist_(std::move(dist)),
    M1_(M1), M2_(M2)
{}

DataList ApollonianInternalNode::getAll() const {
    DataList all = { c1_, c2_ };
    if (left_) { auto l = left_->getAll();  all.insert(all.end(), l.begin(), l.end()); }
    if (mid_) { auto m = mid_->getAll();   all.insert(all.end(), m.begin(), m.end()); }
    if (right_) { auto r = right_->getAll(); all.insert(all.end(), r.begin(), r.end()); }
    return all;
}

// === 删除了 decideAction 函数（不再需要）===

std::vector<DataPtr> ApollonianInternalNode::rangeSearch(
    const MetricData& q, long double r, long long* distanceCount)
{
    std::vector<DataPtr> result;

    // Step 1: Compute d1 = d(q, c1), d2 = d(q, c2)
    long double d1 = dist_->distance(q, *c1_);
    long double d2 = dist_->distance(q, *c2_);
    if (distanceCount) (*distanceCount) += 2;

    // Add pivots if within range
    if (d1 <= r) result.push_back(c1_);
    if (d2 <= r) result.push_back(c2_);

    // ====== 定理 1-3：子树包含规则 ======
    bool fullLeft = (left_ != nullptr) && (d1 + M1_ <= r);
    bool fullRight = (right_ != nullptr) && (d2 + M2_ <= r);

    // ====== 定理 1-2：子树排除规则（注意：使用 >） ======
    bool pruneLeft = (left_ != nullptr) && (d1 - c1_ratio_ * d2 > (c1_ratio_ + 1) * r);
    bool pruneRight = (right_ != nullptr) && (c2_ratio_ * d2 - d1 > (c2_ratio_ + 1) * r);

    // ====== 定理 1-1：结果定位规则（注意：使用 >=） ======
    bool onlyLeft = (c1_ratio_ * d2 - d1 >= (c1_ratio_ + 1) * r);  // 所有结果必在左
    bool onlyRight = (d1 - c2_ratio_ * d2 >= (c2_ratio_ + 1) * r);  // 所有结果必在右

    // ====== 处理左子树 ======
    if (left_) {
        if (fullLeft) {
            auto full = left_->getAll();
            result.insert(result.end(), full.begin(), full.end());
        }
        else if (!pruneLeft && (onlyLeft || !onlyRight)) {
            auto res = left_->rangeSearch(q, r, distanceCount);
            result.insert(result.end(), res.begin(), res.end());
        }
        // 否则：被 pruneLeft 排除，或 onlyRight 成立 → 跳过
    }

    // ====== 处理中子树 ======
    if (mid_) {
        // 中子树无法用定理1-2/1-3剪枝，但可被定理1-1跳过
        if (!onlyLeft && !onlyRight) {
            auto res = mid_->rangeSearch(q, r, distanceCount);
            result.insert(result.end(), res.begin(), res.end());
        }
        // 否则：onlyLeft 或 onlyRight 成立 → 跳过中子树
    }

    // ====== 处理右子树 ======
    if (right_) {
        if (fullRight) {
            auto full = right_->getAll();
            result.insert(result.end(), full.begin(), full.end());
        }
        else if (!pruneRight && (onlyRight || !onlyLeft)) {
            auto res = right_->rangeSearch(q, r, distanceCount);
            result.insert(result.end(), res.begin(), res.end());
        }
        // 否则：被 pruneRight 排除，或 onlyLeft 成立 → 跳过
    }

    return result;
}