#include "../../../HeaderFiles/outlierdetection/detectionalgorithm/iORCA.h"
#include "../../../HeaderFiles/metricdata/KddCup99.h"
#include "../../../HeaderFiles/metricdata/DoubleVectorClass.h"
#include "../../../HeaderFiles/metricdata/DNAClass.h"
#include "../../../HeaderFiles/index/PivotSelectionMethod.h"
#include <algorithm>
#include <stdlib.h>
#include <boost/random.hpp>
#include <ctime>

template<typename T>
bool insertQueue(T& data, T* dataQueue, int k, bool isDescend);
ROCPoint* getROC(bool* trueState, int n, double totalOutlierNum, int size);
double getAccuracy(bool* trueState, int n);
double getAUC(ROCPoint* ROC, int n);
void outputROC(ROCPoint* ROC, int n);
double findAvg(CKNN* Block, int blockSize);
int getStartID(CKNN* Block, int blockSize, double avg);
int getSpiralOrder(int size, int startID, int i);
bool greaterCKNN(const CKNN& a, const CKNN& b);
CKNN getKth(CKNN nums[], int len, int id, int k);

CiORCA::CiORCA(vector<shared_ptr<CMetricData>>& md, COutlierDefinition* _od, CMetricDistance* metrictype, CPivotSelectionMethod* _psm, int _n, int _k, int _b)
{
	metricData = &md;
	outlierDefinition = _od;
	metric = metrictype;
	psm = _psm;
	n = _n;
	k = _k;
	blockSize = _b;
}

CMetricDistance* CiORCA::getMetric()
{
	return metric;
}


CKNN* CiORCA::getOutlier(int p1, int p2)
{
	ofstream result("D:\\Data\\result\\result.txt", ios::app);
	//result<<"iORCA:"<<endl;
	ofstream outlierDegree("D:\\Data\\result\\outlierDegree.txt", ios::app);
	ofstream cutoffvalue("D:\\Data\\result\\cutoffvalue.txt", ios::app);
	long indexStartTime = clock();
	/**cutoff value*/
	double c = 0;
	unsigned long long int disComputeTimes = 0;
	int size = metricData->size();

	boost::mt19937 rng(time(0));
	boost::uniform_int<> ui(0, size - 1);

	int pivotSearchSize = 0;
	pivotSearchSize = blockSize < size ? blockSize : size;
	vector<int> pivot;
	pivot.push_back(ui(rng));//pivot[0]=0;
	//cout<<"pivots: "<<pivot[0]<<endl;
	/**top n outliers, from small to large*/
	CKNN* TOP_N = new CKNN[n];

	for (int i = 0; i < n; i++)
	{
		TOP_N[i].dataID = 0;
		TOP_N[i].dis = 0;
	}
	/**the block process every time*/
	CKNN* B = new CKNN[blockSize];
	for (int i = 0; i < blockSize; i++)
	{
		B[i].dataID = 0;
		B[i].dis = 0;
	}

	CKNN* index = new CKNN[size];
	for (int i = 0; i < size; i++)
	{
		index[i].dataID = i;
		index[i].dis = metric->getDistance((*metricData)[i].get(), (*metricData)[pivot[0]].get(), size, p1, p2);
		disComputeTimes++;
	}
	sort(index, index + size, greaterCKNN);

	long indexStopTime = clock();

	COutlierDefinition** od = new COutlierDefinition * [size];
	for (int i = 0; i < size; i++)
	{
		od[i] = outlierDefinition->CreateInstance(k);
		od[i]->setState(true);
	}

	CKNN* knn;
	long startTime = clock();
	double dis = 0;
	int neighborNum = 0;
	int i = 0, j = 0;
	int validNum = 0;

	int realBlockSize = 0;
	int avgDis = 0;
	int startID = 0;
	int orderID = 0;
	double kDisOfPivot = index[size - k].dis;
	bool isStop = false;

	while (i < size)
	{
		if (isStop)
			break;

		B[j] = index[i];
		j++;

		if ((j == blockSize) || (i == size - 1))
		{
			cout << "c = " << c << endl;
			if (B[0].dis + kDisOfPivot < c)
			{
				isStop = true;
			}
			if ((i == size - 1) && (size % blockSize))
				realBlockSize = size % blockSize;
			else
				realBlockSize = blockSize;
			avgDis = findAvg(B, realBlockSize);
			startID = getStartID(B, realBlockSize, avgDis);
			for (int d = 0; d < size; d++)
			{
				orderID = getSpiralOrder(size, startID, d);
				for (int b = 0; b < realBlockSize; b++)
				{
					if ((od[B[b].dataID]->getState() == true) && (B[b].dataID != index[orderID].dataID))
					{
						dis = metric->getDistance((*metricData)[B[b].dataID].get(), (*metricData)[index[orderID].dataID].get(), size, p1, p2);
						disComputeTimes++;
						knn = od[B[b].dataID]->getKnn();
						CKNN tempKnn(index[orderID].dataID, dis);
						if (insertQueue(tempKnn, knn, k, true))
						{
							od[B[b].dataID]->setWeight();
							/**When an object's closest neighbors achieve a score lower than the cutoff value we remove the object because it can no longer be an outlier*/
							if (od[B[b].dataID]->getWeight() < c)
							{
								od[B[b].dataID]->setState(false);
							}
						}
					}
				}
			}

			/**compute current TOP N outliers.*/
			for (int b = 0; b < j; b++)
			{
				if (od[B[b].dataID]->getState() == true)
				{
					double weight = od[B[b].dataID]->getWeight();
					int dataID = B[b].dataID;
					CKNN tempKNN(dataID, weight);
					if (insertQueue(tempKNN, TOP_N, n, false))
						validNum++;
					c = TOP_N[n - 1].dis;
				}
			}
			j = 0;
		}
		i++;
	}
	long stopTime = clock();
	cout << "Distance computing times: " << disComputeTimes << endl;
	cout << "time:" << stopTime - startTime << endl;
	result << size << "\t" << k << "\t" << n << "\t" <<  indexStopTime - indexStartTime << "\t" << stopTime - startTime << "\t" << disComputeTimes << endl;
	result.close();
	delete[] B;
	for (int i = 0; i < size; i++)
	{
		delete[] od[i];
	}
	delete[] od;
	delete[] index;
	return TOP_N;
}

CiORCA::~CiORCA()
{
}