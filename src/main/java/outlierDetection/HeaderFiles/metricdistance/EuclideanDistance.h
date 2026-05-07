#ifndef EUCLIDEANDISTANCE_H
#define EUCLIDEANDISTANCE_H
#include "MetricDistance.h"

class CEuclideanDistance : public CMetricDistance
{
public:
	CEuclideanDistance();
	~CEuclideanDistance();
	virtual double getDistance(CMetricData*,CMetricData*);
	virtual double getDistance(double *array1,double *array2,int d);
	virtual double getDistance(CMetricData* _obj1, CMetricData* _obj2, int length, int start, int end);
};

#endif





