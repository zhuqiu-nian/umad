#ifndef PEARSONDISTANCE_H
#define PEARSONDISTANCE_H
#include "MetricDistance.h"

class CPearsonDistance : public CMetricDistance
{
public:
	CPearsonDistance();
	~CPearsonDistance();
	double getDistance(CMetricData*,CMetricData*);
	double getDistance(double*v1, double*v2,int length);
	double getDistance(CMetricData* _obj1, CMetricData* _obj2, int length, int start, int end);
};

#endif





