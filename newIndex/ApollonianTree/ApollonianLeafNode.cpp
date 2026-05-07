// ApollonianLeafNode.cpp
#include "../../../include/index_structure/ApollonianTree/ApollonianLeafNode.h"
#include "../../../include/utils/MetricSpaceSearch.h"
#include <iostream>

ApollonianLeafNode::ApollonianLeafNode(
    const DataList& data,
    int distanceType,
    int dataType)
    : dataList_(data), isEmpty_(true)
{
    if (data.empty()) return;

    try {
        // 叶节点：选 min(2, N) 个 pivot 构建 PivotTable（可选）
        std::vector<int> pivots;
        int k = std::min(2, static_cast<int>(data.size()));
        for (int i = 0; i < k; ++i) pivots.push_back(i);

        pivotTable_ = std::make_unique<PivotTable>(data, pivots, distanceType, dataType);
        isEmpty_ = false;
    }
    catch (...) {
        isEmpty_ = true;
    }
}

std::vector<DataPtr> ApollonianLeafNode::rangeSearch(const MetricData& q, long double r, long long* distanceCount) {
    if (!pivotTable_) {
        return {};
    }
    return pivotTable_->search(q, r, distanceCount);
}

DataList ApollonianLeafNode::getAll() const {
    return dataList_;
}