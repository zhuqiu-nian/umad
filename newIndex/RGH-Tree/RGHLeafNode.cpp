// RGHLeafNode.cpp
#include "../../../include/index_structure/RGH-Tree/RGHLeafNode.h"
#include "../../../include/interfaces/MetricDistance.h"
#include "../../../include/utils/MetricSpaceSearch.h"
#include "../../PivotSelector/PivotSelector.h"
#include <stdexcept>

RGHLeafNode::RGHLeafNode(
    const std::vector<std::shared_ptr<MetricData>>& data,
    int distanceType,
    int dataType,
    PivotSelector::SelectionMethod method
) : dataList_(data), distanceType_(distanceType), dataType_(dataType), isEmpty_(true) {
    if (data.empty()) return;

    try {
        auto dist = MetricSpaceSearch::createDistanceFunction(distanceType, dataType);
        // 在叶子节点中也使用 PivotSelector 动态选择一个支撑点
        std::vector<int> pivots = PivotSelector::selectPivots(data, 1, dist, method, 0.35);
        pivotTable_ = std::make_unique<PivotTable>(data, pivots, distanceType, dataType);
        isEmpty_ = false;
    }
    catch (const std::exception& e) {
        std::cerr << "[RGHLeafNode] 构造 PivotTable 失败: " << e.what() << std::endl;
        isEmpty_ = true;
    }
}

std::vector<std::shared_ptr<MetricData>> RGHLeafNode::rangeSearch(
    const MetricData& q,
    long double r,
    long long* distanceCount
) {
    if (!pivotTable_) {
        return {}; // 如果 PivotTable 构建失败，返回空结果
    }
    return pivotTable_->search(q, r, distanceCount);
}

std::vector<std::shared_ptr<MetricData>> RGHLeafNode::getAll() const {
    return dataList_;
}