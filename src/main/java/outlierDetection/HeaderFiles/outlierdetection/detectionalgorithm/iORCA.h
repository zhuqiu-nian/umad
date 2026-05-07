#ifndef IORCA_H
#define IORCA_H
#include "../../../HeaderFiles/outlierdetection/detectionalgorithm/OutlierDetector.h"
//#include "../../../HeaderFiles/outlierdetection/outlierdefinition/OutlierDefinition.h"
#include "../../metricdata/MetricData.h"
#include "../../metricdistance/MetricDistance.h"
#include "../../index/PivotSelectionMethod.h"

class CiORCA:public COutlierDetector
{
private:
	vector<shared_ptr<CMetricData>> *metricData;
	COutlierDefinition *outlierDefinition;
	CMetricDistance *metric;
	CPivotSelectionMethod *psm;
	int n;  //TOP-n
	int k;  //kNN
	/**the number of objects process every time*/
	int blockSize; 

public:
	CiORCA(vector<shared_ptr<CMetricData>> &md, COutlierDefinition *_od, CMetricDistance *metrictype, CPivotSelectionMethod *_psm, int _n, int _k, int _b);
	virtual CMetricDistance* getMetric();
	virtual CKNN* getOutlier(int p1, int p2);
	~CiORCA();
};

#endif