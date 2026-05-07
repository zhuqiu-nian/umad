// ApollonianNode.h
#ifndef APOLLONIAN_NODE_H
#define APOLLONIAN_NODE_H

#include "../../../include/interfaces/MetricData.h"
#include <vector>
#include <memory>

using DataPtr = std::shared_ptr<MetricData>;
using DataList = std::vector<DataPtr>;

class ApollonianNode {
public:
    virtual ~ApollonianNode() = default;
    virtual std::vector<DataPtr> rangeSearch(const MetricData& q, long double r, long long* distanceCount) = 0;
    virtual DataList getAll() const = 0;
};

#endif