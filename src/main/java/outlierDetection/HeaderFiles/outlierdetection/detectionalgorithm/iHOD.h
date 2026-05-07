#ifndef IHOD_H
#define IHOD_H
#include "../../../HeaderFiles/outlierdetection/detectionalgorithm/OutlierDetector.h"
//#include "../../../HeaderFiles/outlierdetection/outlierdefinition/OutlierDefinition.h"
#include "../../metricdata/MetricData.h"
#include "../../metricdistance/MetricDistance.h"
#include "../../../HeaderFiles/index/PivotSelectionMethod.h"

class CiHOD:public COutlierDetector
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
	/**the number of pivots*/
	int v;

public:
	CiHOD(vector<shared_ptr<CMetricData>> &md, COutlierDefinition *_od, CMetricDistance *metrictype, CPivotSelectionMethod *psm, int _n, int _k, int _b, int _v);
	virtual CMetricDistance* getMetric();
	virtual CKNN* getOutlier(int outlierNum, int parameter);
	~CiHOD();
};

#endif