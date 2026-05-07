// RGHTree.h
#ifndef RGH_TREE_H
#define RGH_TREE_H

#include "RGHNode.h"
#include "../../../include/interfaces/MetricData.h"
#include "../../../include/utils/Solution.h" // For PivotTable
#include "../../../src/PivotSelector/PivotSelector.h"
#include <memory>
#include <vector>

/**
 * @brief RGH-Tree 主类
 */
class RGHTree {
private:
    static long long distanceCalculations_;

    // 辅助函数声明
    static std::pair<std::shared_ptr<MetricData>, std::shared_ptr<MetricData>> selectPivotPair(
        const std::vector<std::shared_ptr<MetricData>>& data,
        const std::shared_ptr<MetricDistance>& dist
    );

    static long double calculateSplitRatio(
        const std::vector<std::shared_ptr<MetricData>>& data,
        const std::shared_ptr<MetricData>& leftPivot,
        const std::shared_ptr<MetricData>& rightPivot,
        const std::shared_ptr<MetricDistance>& dist
    );

    static void partitionData(
        const std::vector<std::shared_ptr<MetricData>>& data,
        const std::shared_ptr<MetricData>& leftPivot,
        const std::shared_ptr<MetricData>& rightPivot,
        long double splitRatio,
        std::vector<std::shared_ptr<MetricData>>& leftData,
        std::vector<std::shared_ptr<MetricData>>& rightData,
        const std::shared_ptr<MetricDistance>& dist
    );

public:
    RGHTree() = default;

    static std::unique_ptr<RGHNode> bulkLoad(
        const std::vector<std::shared_ptr<MetricData>>& data,
        int distanceType,
        int dataType,
        PivotSelector::SelectionMethod method
    );

    static void runRGHRangeSearch(
        const std::vector<std::shared_ptr<MetricData>>& dataset,
        int distanceType,
        int dataType
    );

    static void resetDistanceCalculations();
    static long long getDistanceCalculations();
};

#endif // RGH_TREE_H
