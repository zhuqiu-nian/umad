//计算机学报论文
#include "../../../HeaderFiles/outlierdetection/detectionalgorithm/DPiORCA.h"
#include "../../../HeaderFiles/metricdata/DoubleVector.h"
#include "../../../HeaderFiles/metricdata/KddCup99.h"
#include "../../../HeaderFiles/metricdata/DoubleVectorClass.h"
#include "../../../HeaderFiles/metricdata/DNAClass.h"
#include "../../../HeaderFiles/index/DensityPivotSelection.h"
#include "../../../HeaderFiles/index/DensityDisParPivotSelection.h"
#include "../../../HeaderFiles/index/FFTPivotSelectionMethod.h"
#include "windows.h"
#include <psapi.h>
#include <algorithm>
#include <stdlib.h>
#include <time.h>
#include <boost/random.hpp>
#include <ctime>


#pragma comment(lib,"psapi.lib") 

template<typename T>
extern bool insertQueue(T &data, T *dataQueue, int k, bool isDescend);
ROCPoint* getROC(bool *trueState, int n, double totalOutlierNum, int size);
double getAccuracy(bool *trueState, int n);
double getAUC(ROCPoint* ROC, int n);
void outputROC(ROCPoint* ROC, int n);
double findAvg(CKNN *Block, int blockSize);
int getStartID(CKNN *Block, int blockSize, double avg);
int getSpiralOrder(int size, int startID, int i);
bool greaterCKNN(const CKNN &a, const CKNN &b);

CDPiORCA::CDPiORCA(vector<shared_ptr<CMetricData>> &md, COutlierDefinition *_od, CMetricDistance *metrictype, int _n, int _k, int _b)
{
	metricData = &md;
	outlierDefinition = _od;
	metric = metrictype;
	n = _n;
	k = _k;
	blockSize = _b;
}

CMetricDistance* CDPiORCA::getMetric()
{
	return metric;
}


CKNN* CDPiORCA::getOutlier(int outlierNum, int parameter)
{
	ofstream result("D:\\Data\\result\\result.txt",ios::app);
	ofstream outlierDegree("D:\\Data\\result\\outlierDegree.txt",ios::app);
	ofstream cutoffvalue("D:\\Data\\result\\cutoffvalue.txt",ios::app);
	long indexStartTime = GetTickCount();
	/**cutoff value*/
	double c = 0;
	unsigned long long int disComputeTimes = 0;
	int size = metricData->size();

	boost::mt19937 rng(time(0));
	boost::uniform_int<> ui(0, size-1);
	
	//CDensityDisParPivotSelection *psm1 = 0;
	CDensityPivotSelection *psm1 = 0;
	CFFTPivotSelectionMethod *psm2 = 0;
	int pivotSearchSize = 0;
	pivotSearchSize = blockSize<size ? blockSize:size;
	//vector<int> pivot;
	vector<int> denPivot;
	vector<int> borderPivot;
	//pivot = psm1->selectPivots(metric, *metricData, 0, pivotSearchSize, 1, 0);
	int first = parameter;
	denPivot = psm1->selectPivots(metric, *metricData, first, pivotSearchSize, 1, first);
	//denPivot.push_back( ui(rng) );//pivot[0]=0;
	cout<<"denPivots: "<<denPivot[0]<<endl;
	int numPivots = 2;
	borderPivot = psm2->selectPivots(metric, *metricData, first, pivotSearchSize, numPivots+1, first);
	cout<<"borderPivots: "<<borderPivot[0]<<","<<borderPivot[1]<<endl;
	for(int i=0; i<numPivots; i++)
	{
		borderPivot[i] = borderPivot[i+1];
	}
	//borderPivot[0] = borderPivot[1];
	//borderPivot[1] = borderPivot[2];
	cout<<"borderPivots: "<<borderPivot[0]<<","<<borderPivot[1]<<endl;
	/**top n outliers, from small to large*/
	CKNN *TOP_N = new CKNN[n];

	for(int i=0; i<n; i++)
	{
		TOP_N[i].dataID = 0;
		TOP_N[i].dis = 0;
	}
	/**the block process every time*/
	CKNN *B = new CKNN[blockSize];
	for(int i=0; i<blockSize; i++)
	{
		B[i].dataID = 0;
		B[i].dis = 0;
	}

	CKNN *index = new CKNN[size];
	double **pivotSpace = new double *[size];
	for(int i=0; i<size; i++)
	{
		pivotSpace[i] = new double[numPivots];
		for(int j=0; j<numPivots; j++)
		{
			pivotSpace[i][j] = 0;
		}
	}
	for(int i=0; i<size; i++)
	{
		index[i].dataID = i;
		index[i].dis = metric->getDistance((*metricData)[i].get(),(*metricData)[denPivot[0]].get());
		disComputeTimes++;
		for(int j=0; j<numPivots; j++)
		{
			pivotSpace[i][j] = metric->getDistance((*metricData)[i].get(),(*metricData)[borderPivot[j]].get());
			disComputeTimes++;
		}
	}
	sort(index, index+size, greaterCKNN);
	long indexStopTime = GetTickCount();

	COutlierDefinition **od = new COutlierDefinition*[size];
	for(int i=0; i<size; i++)
	{
		od[i] = outlierDefinition->CreateInstance(k);
		od[i]->setState(true);
		//od[i]->setNeighborNum(0);
	}

	CKNN *knn;
	long startTime = GetTickCount();
	double dis = 0;
	int neighborNum = 0;
	int i = 0, j = 0;
	int validNum = 0;
	
	int realBlockSize = 0;
	int avgDis = 0;
	int startID = 0;
	int orderID = 0;
	double kDisOfPivot = index[size-k].dis;
	bool isStop = false;
	bool isKNN = true;

	while(i<size)
	{
		if(isStop)
			break;
		
		B[j] = index[i];
		j++;
		
		if((j == blockSize)||(i == size-1))
		{
			if(B[0].dis + kDisOfPivot < c)
			{
				isStop = true;
			}
			if((i == size-1) && (size%blockSize))
				realBlockSize = size%blockSize;
			else
				realBlockSize = blockSize;
			avgDis = findAvg(B, realBlockSize);
			startID = getStartID(B, realBlockSize, avgDis);
			//cout<<"c="<<c<<endl;
			for(int d=0; d<size; d++)
			{
				orderID = getSpiralOrder(size, startID, d);
				for(int b=0; b<realBlockSize; b++)
				{
					if((od[B[b].dataID]->getState() == true) && (B[b].dataID != index[orderID].dataID))
					{
						knn = od[B[b].dataID]->getKnn();
						for(int p=0; p<numPivots; p++ )
						{
							if(abs(pivotSpace[B[b].dataID][p]-pivotSpace[index[orderID].dataID][p]) > knn[0].dis)
							{
								isKNN = false;
								break;
							}
						}
						if(isKNN)
						{
							dis = metric->getDistance((*metricData)[B[b].dataID].get(),(*metricData)[index[orderID].dataID].get());
							disComputeTimes++;
							CKNN tempKnn(index[orderID].dataID, dis);
							if(insertQueue(tempKnn, knn, k, true))
							{
								od[B[b].dataID]->setWeight();
								if(od[B[b].dataID]->getWeight() < c)
								{
									od[B[b].dataID]->setState(false);
								}
							}
						}
					}
					isKNN = true;
				}
			}
			
			/**compute current TOP N outliers.*/
			for(int b=0; b<j; b++)
			{
				if(od[B[b].dataID]->getState() == true)
				{
					double weight = od[B[b].dataID]->getWeight();
					int dataID = B[b].dataID;
					CKNN tempKNN(dataID,weight);
					if(insertQueue(tempKNN, TOP_N, n, false))
						validNum++;
					c = TOP_N[n-1].dis;
				}
			}
			j = 0;
		}
		i++;
	}
	long stopTime = GetTickCount();
	/*for(int i=0; i<n; i++)
	{
		cout<<"Outlier dataID:"<<TOP_N[i].dataID<<"  Weight:"<<TOP_N[i].dis<<endl;
	}*/
	cout<<"Distance computing times: "<<disComputeTimes<<endl;
	double *dataList;
	int nnID = 0;
	int score = 0;
	ROCPoint *roc;
	double auc = 0;
	bool *trueState = new bool[n];
	for(int i=0; i<n; i++)
	{
		trueState[i] = true;
	}
	if(validNum < n)
	{
		n = validNum;
	}
	for(int i=0; i<n; i++)
	{
		if(!((CDoubleVectorClass*)((*metricData)[TOP_N[i].dataID].get()))->getState())
		{
			//cout<<"outlier"<<endl;
			trueState[i] = false;
			score++;
		}
	}
	cout<<"trueState:"<<endl;
	for(int i=0; i<n; i++)
	{
		cout<<trueState[i];
		if(i%10==9)
			cout<<" ";
		if(i%50==49)
			cout<<endl;
		//outlierDegree<<i<<"\t"<<TOP_N[i].dis<<endl;
	}
	//cout<<endl<<"n:"<<n<<" outlierNum:"<<outlierNum<<" size:"<<size<<endl;
	//double iORCAAccuracy = getAccuracy(trueState, n);
	//cout<<"iORCAAccuracy:"<<iORCAAccuracy<<endl;
	roc = getROC(trueState,n,outlierNum,size);
	auc = getAUC(roc,n);
	outputROC(roc,n);
	cout<<"time:"<<stopTime-startTime<<endl;
	cout<<"score:"<<score<<"  AUC:"<<auc<<endl;
	HANDLE handle = GetCurrentProcess();
	PROCESS_MEMORY_COUNTERS pmc;
	GetProcessMemoryInfo(handle, &pmc, sizeof(pmc));
	cout<<"WorkingSetSize:"<<pmc.WorkingSetSize/1024.0<<"KB"<<endl;
	cout<<"PeakWorkingSetSize:"<<pmc.PeakWorkingSetSize/1024.0<<"KB"<<endl;
	result<<size<<"\t"<<k<<"\t"<<n<<"\t"<<score<<"\t"<<auc<<"\t"<<pmc.WorkingSetSize / 1024.0<<"\t"<<pmc.PeakWorkingSetSize / 1024.0<<"\t"<<indexStopTime-indexStartTime<<"\t"<<stopTime-startTime<<"\t"<<disComputeTimes<<endl;
	result.close();
	delete[] B;
	for(int i=0; i<size; i++)
	{
		delete[] od[i];
	}
	delete[] od;
	delete[] index;
	delete[] trueState;
	return TOP_N;
}

CDPiORCA::~CDPiORCA()
{
}