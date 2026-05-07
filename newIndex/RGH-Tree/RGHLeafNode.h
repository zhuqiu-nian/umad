#pragma once
// RGHLeafNode.h
#ifndef RGH_LEAF_NODE_H
#define RGH_LEAF_NODE_H

#include "RGHNode.h"
#include "../../../include/interfaces/MetricData.h"
#include "../../../include/utils/Solution.h" // Assuming PivotTable is defined here
#include "../../../src/PivotSelector/PivotSelector.h"
#include "../PivotTable/PivotTable.h"
#include <memory>
#include <vector>

/**
 * @brief RGH-Tree 叶子节点
 * 存储实际数据点，并使用 PivotTable 进行优化查询
 */
class RGHLeafNode : public RGHNode {
private:
    std::vector<std::shared_ptr<MetricData>> dataList_; ///< 存储的数据点
    int distanceType_;                                  ///< 距离类型
    int dataType_;                                      ///< 数据类型
    std::unique_ptr<PivotTable> pivotTable_;            ///< 用于优化查询的 PivotTable
    bool isEmpty_;                                      ///< 是否为空节点

public:
    /**
     * @brief 构造叶子节点
     * @param data 要存储的数据点列表
     * @param distanceType 距离类型
     * @param dataType 数据类型
     * @param method 支撑点选择方法
     */
    RGHLeafNode(
        const std::vector<std::shared_ptr<MetricData>>& data,
        int distanceType,
        int dataType,
        PivotSelector::SelectionMethod method
    );

    /**
     * @brief 在当前叶子节点中执行范围查询
     * @param q 查询点
     * @param r 查询半径
     * @param distanceCount 距离计算次数统计指针
     * @return 符合条件的数据点列表
     */
    std::vector<std::shared_ptr<MetricData>> rangeSearch(
        const MetricData& q,
        long double r,
        long long* distanceCount
    ) override;

    /**
     * @brief 获取当前节点存储的所有数据点
     * @return 所有数据点的列表
     */
    std::vector<std::shared_ptr<MetricData>> getAll() const override;

    // --- [新增] 为了调试回溯所需的接口 ---

   // 1. 获取当前叶子节点存储的所有原始数据
   // 调试程序可以通过遍历这个 vector 来判断目标对象是否真的在这里
    const std::vector<std::shared_ptr<MetricData>>& getDataList() const {
        return dataList_;
    }

    // 2. (可选) 如果你想看 PivotTable 里的支撑点
    // 注意：这取决于 PivotTable 是否有公开接口。
    // 如果没有，你可以简单返回 dataList_ 中的前几个元素（如果 PivotTable 选的是前几个），
    // 或者暂时不实现这个，仅依靠 dataList_ 进行调试通常也够了。
};



#endif // RGH_LEAF_NODE_H