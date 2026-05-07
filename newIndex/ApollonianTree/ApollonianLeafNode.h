// ApollonianLeafNode.h
#ifndef APOLLONIAN_LEAF_NODE_H
#define APOLLONIAN_LEAF_NODE_H

#include "ApollonianNode.h"
#include "../PivotTable/PivotTable.h"

class ApollonianLeafNode : public ApollonianNode {
public:
    ApollonianLeafNode(const DataList& data, int distanceType, int dataType);
    std::vector<DataPtr> rangeSearch(const MetricData& q, long double r, long long* distanceCount) override;
    DataList getAll() const override;

private:
    DataList dataList_;
    std::unique_ptr<PivotTable> pivotTable_;
    bool isEmpty_;
};

#endif