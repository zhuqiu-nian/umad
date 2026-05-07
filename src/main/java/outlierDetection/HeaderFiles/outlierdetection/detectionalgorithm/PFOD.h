#ifndef PFOD_H
#define PFOD_H
#include "../../../HeaderFiles/outlierdetection/detectionalgorithm/OutlierDetector.h"
//#include "../../../HeaderFiles/outlierdetection/outlierdefinition/OutlierDefinition.h"
#include "../../metricdata/MetricData.h"
#include "../../metricdistance/MetricDistance.h"

class CPFOD:public COutlierDetector
{
private:
	vector<shared_ptr<CMetricData>> *metricData;
	COutlierDefinition *outlierDefinition;
	CMetricDistance *metric;
	int n;  //TOP-n
	int k;  //kNN
	int m;
	/**the number of objects process every time*/
	int blockSize; 

public:
	CPFOD(vector<shared_ptr<CMetricData>> &md, COutlierDefinition *_od, CMetricDistance *metrictype, int _n, int _k, int _b, int _m);
	static double getTempWeight(CKNN * knn, int kNum);
	virtual CMetricDistance* getMetric();
	virtual CKNN* getOutlier(int outlierNum, int parameter);
	~CPFOD();
};

#endif