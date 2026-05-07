#ifndef OUTLIERDETECTOR_H
#define OUTLIERDETECTOR_H
#include "../../../HeaderFiles/outlierdetection/outlierdefinition/OutlierDefinition.h"
#include "../../metricdistance/MetricDistance.h"

class COutlierDetector
{
private:
	
public:
	COutlierDetector();
	virtual CMetricDistance* getMetric()=0;
	virtual CKNN* getOutlier(int p1, int p2)=0;
	~COutlierDetector();
};

#endif