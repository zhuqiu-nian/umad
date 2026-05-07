// RGHInternalNode.h
#ifndef RGH_INTERNAL_NODE_H
#define RGH_INTERNAL_NODE_H

#include "RGHNode.h"
#include "../../../include/interfaces/MetricDistance.h"
#include <memory>
#include <vector>

/**
 * @brief RGH-Tree 内部节点
 */
class RGHInternalNode : public RGHNode {
private:
    std::shared_ptr<MetricData> leftPivot_;   // Pl
    std::shared_ptr<MetricData> rightPivot_;  // Pr
    long double splitRatio_;                  // R

    // --- 新增：用于优化的元数据 ---

    long double leftSubtreeRadius_;           // dl: 左子树覆盖半径
    long double rightSubtreeRadius_;          // dr: 右子树覆盖半径

    // Ol_Or[0] = d(Pl, V.left.Pl), Ol_Or[1] = d(Pl, V.left.Pr)
    // Ol_Or[2] = d(Pr, V.right.Pl), Ol_Or[3] = d(Pr, V.right.Pr)
    long double childDistances_[4];

    std::unique_ptr<RGHNode> leftChild_;
    std::unique_ptr<RGHNode> rightChild_;
    std::shared_ptr<MetricDistance> dist_;

public:
    /**
     * @brief 构造内部节点
     */
    RGHInternalNode(
        std::shared_ptr<MetricData> leftPivot,
        std::shared_ptr<MetricData> rightPivot,
        long double splitRatio,
        long double leftSubtreeRadius,
        long double rightSubtreeRadius,
        const std::array<long double, 4>& childDistances, // 传入预计算的距离数组
        std::unique_ptr<RGHNode> left,
        std::unique_ptr<RGHNode> right,
        std::shared_ptr<MetricDistance> dist
    );

    std::vector<std::shared_ptr<MetricData>> rangeSearch(
        const MetricData& q,
        long double r,
        long long* distanceCount
    ) override;

    std::vector<std::shared_ptr<MetricData>> getAll() const override;

    // 接口实现
    std::shared_ptr<MetricData> getLeftPivot() const override { return leftPivot_; }
    std::shared_ptr<MetricData> getRightPivot() const override { return rightPivot_; }
    long double getLeftRadius() const override { return leftSubtreeRadius_; }
    long double getRightRadius() const override { return rightSubtreeRadius_; }

    // --- [新增] 为了调试回溯所需的接口 ---

    // 1. 获取划分比率 R
    long double getSplitRatio() const { return splitRatio_; }

    // 2. 获取子节点指针 (用于递归遍历树)
    const std::unique_ptr<RGHNode>& getLeftChild() const { return leftChild_; }
    const std::unique_ptr<RGHNode>& getRightChild() const { return rightChild_; }

    // 3. 获取预计算的距离数组 (用于打印调试信息)
    // 返回 const 引用，避免拷贝
    const long double* getChildDistances() const { return childDistances_; }

};

#endif // RGH_INTERNAL_NODE_H