#pragma once
// RGHNode.h
#ifndef RGH_NODE_H
#define RGH_NODE_H

#include "../../../include/interfaces/MetricData.h"
#include <memory>
#include <vector>

using DataPtr = std::shared_ptr<MetricData>;
using DataList = std::vector<DataPtr>;


/**
 * @brief RGH-Tree 节点的抽象基类
 */
class RGHNode {
public:
    virtual ~RGHNode() = default;

    /**
     * @brief 范围查询
     * @param q 查询点
     * @param r 查询半径
     * @param distanceCount 距离计算次数统计指针
     * @return 符合条件的数据点列表
     */
    virtual std::vector<std::shared_ptr<MetricData>> rangeSearch(
        const MetricData& q,
        long double r,
        long long* distanceCount
    ) = 0;

    /**
     * @brief 获取节点下的所有数据点
     * @return 所有数据点的列表
     */
    virtual std::vector<std::shared_ptr<MetricData>> getAll() const = 0;

    // --- 为支持复杂剪枝而新增的接口 ---

    /**
     * @brief 获取左参考点 (仅内部节点有效)
     */
    virtual std::shared_ptr<MetricData> getLeftPivot() const { return nullptr; }

    /**
     * @brief 获取右参考点 (仅内部节点有效)
     */
    virtual std::shared_ptr<MetricData> getRightPivot() const { return nullptr; }

    /**
     * @brief 获取左子树的覆盖半径 (仅内部节点有效)
     */
    virtual long double getLeftRadius() const { return -1.0L; }

    /**
     * @brief 获取右子树的覆盖半径 (仅内部节点有效)
     */
    virtual long double getRightRadius() const { return -1.0L; }
};

#endif // RGH_NODE_H