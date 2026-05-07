//Density Peak Pivot Selection Ð¡Î¢ÂÛÎÄ
#ifndef DPPOD_H
#define DPPOD_H
#include "../../../HeaderFiles/outlierdetection/detectionalgorithm/OutlierDetector.h"
//#include "../../../HeaderFiles/outlierdetection/outlierdefinition/OutlierDefinition.h"
#include "../../metricdata/MetricData.h"
#include "../../metricdistance/MetricDistance.h"

class CDPPOD:public COutlierDetector
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
	CDPPOD(vector<shared_ptr<CMetricData>> &md, COutlierDefinition *_od, CMetricDistance *metrictype, int _n, int _k, int _b);
	virtual CMetricDistance* getMetric();
	virtual CKNN* getOutlier(int outlierNum, int parameter);
	~CDPPOD();
};

#endif