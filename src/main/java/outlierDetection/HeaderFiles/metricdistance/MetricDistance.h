#ifndef METRIC_H
#define METRIC_H
#include "../metricdata/MetricData.h"

class CMetricDistance
{
public:
        CMetricDistance();
		~CMetricDistance();
        virtual double getDistance(CMetricData*,CMetricData*)=0;
        virtual double getDistance(CMetricData*, CMetricData*, int, int, int)=0;
};

#endif