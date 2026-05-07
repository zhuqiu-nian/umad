// ApollonianInternalNode.h
#ifndef APOLLONIAN_INTERNAL_NODE_H
#define APOLLONIAN_INTERNAL_NODE_H

#include "ApollonianNode.h"
#include "../../../include/interfaces/MetricDistance.h"
#include <memory>
#include <vector>

class ApollonianInternalNode : public ApollonianNode {
public:
    ApollonianInternalNode(
        DataPtr c1,
        DataPtr c2,
        long double c1_ratio,
        long double c2_ratio,
        std::unique_ptr<ApollonianNode> leftChild,
        std::unique_ptr<ApollonianNode> midChild,
        std::unique_ptr<ApollonianNode> rightChild,
        std::shared_ptr<MetricDistance> dist,
        long double M1,
        long double M2);

    std::vector<DataPtr> rangeSearch(const MetricData& q, long double r, long long* distanceCount) override;
    DataList getAll() const override;

private:
    DataPtr c1_, c2_;
    long double c1_ratio_, c2_ratio_;
    std::unique_ptr<ApollonianNode> left_, mid_, right_;
    std::shared_ptr<MetricDistance> dist_;

    // === 新增成员 ===
    long double M1_; // left 子树中所有点到 c1_ 的最大距离
    long double M2_; // right 子树中所有点到 c2_ 的最大距离

    // Helper: check if child can be pruned or fully included
    enum class SearchAction { SEARCH, SKIP, FULL };
    SearchAction decideAction(long double a, long double b, long double r, bool isLeft) const;
};

#endif