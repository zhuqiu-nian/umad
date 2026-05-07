// ApollonianTree.h
#ifndef APOLLONIAN_TREE_H
#define APOLLONIAN_TREE_H

#include "ApollonianNode.h"
#include "../../../include/interfaces/MetricDistance.h"
#include "../../../src/PivotSelector/PivotSelector.h"
#include <memory>
#include <vector>

class ApollonianTree {
public:
    static std::unique_ptr<ApollonianNode> bulkLoad(
        const std::vector<DataPtr>& data,
        int distanceType,
        int dataType,
        PivotSelector::SelectionMethod method
    );

    static void runApollonianRangeSearch(
        const std::vector<std::shared_ptr<MetricData>>& dataset,
        int distanceType,
        int dataType
    );

    static long long getDistanceCalculations() { return distanceCalculations_; }
    static void resetDistanceCounter() { distanceCalculations_ = 0; }

private:
    static std::pair<long double, long double> computeOptimalRatios(
        const std::vector<DataPtr>& data,
        const DataPtr& c1,
        const DataPtr& c2,
        const MetricDistance& dist);

    static std::vector<std::vector<DataPtr>> splitIntoThree(
        const std::vector<DataPtr>& data,
        const DataPtr& c1,
        const DataPtr& c2,
        long double c1_ratio,
        long double c2_ratio,
        const MetricDistance& dist
    );

    static long long distanceCalculations_;
};

#endif