#ifndef ORCA_H
#define ORCA_H
#include "../../../HeaderFiles/outlierdetection/detectionalgorithm/OutlierDetector.h"
//#include "../../../HeaderFiles/outlierdetection/outlierdefinition/OutlierDefinition.h"
#include "../../metricdata/MetricData.h"
#include "../../metricdistance/MetricDistance.h"

class CORCA:public COutlierDetector
{
private:
	vector<shared_ptr<CMetricData>> *metricData;
	COutlierDefinition *outlierDefinition;
	CMetricDistance *metric;
	int n;  //TOP-n
	int k;  //kNN
	/**the number of objects process every time*/
	int blockSize; 

public:
	CORCA(vector<shared_ptr<CMetricData>> &md, COutlierDefinition *_od, CMetricDistance *metrictype, int _n, int _k, int _b);
	virtual CMetricDistance* getMetric();
	virtual CKNN* getOutlier(int p1, int p2);
	~CORCA();
};

#endif