#include "../../../HeaderFiles/outlierdetection/detectionalgorithm/iHOD.h"
#include "../../../HeaderFiles/metricdata/DoubleVector.h"
#include "../../../HeaderFiles/metricdata/KddCup99.h"
#include "../../../HeaderFiles/metricdata/DoubleVectorClass.h"
#include "../../../HeaderFiles/metricdata/DNAClass.h"
#include "../../../HeaderFiles/outlierdetection/outlierdefinition/HODC.h"
#include "../../../HeaderFiles/outlierdetection/outlierdefinition/HKODC.h"
#include "../../../../../HeaderFiles/outlierdetection/outlierdefinition/KNN.h"
#include "../../../HeaderFiles/index/DensityPivotSelection.h"
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
/**Insert into Outlier candidateSet, which is from large to small*/
bool insertQueue(shared_ptr<CHODC> &data, vector<shared_ptr<CHODC> > &dataQueue);
bool insertQueue(shared_ptr<CHKODC> &data, vector<shared_ptr<CHKODC> > &dataQueue);

CiHOD::CiHOD(vector<shared_ptr<CMetricData>> &md, COutlierDefinition *_od, CMetricDistance *metrictype, CPivotSelectionMethod *_psm, int _n, int _k, int _b, int _v)
{
	metricData = &md;
	outlierDefinition = _od;
	metric = metrictype;
	psm = _psm;
	n = _n;
	k = _k;
	blockSize = _b;
	v = _v;
}

CMetricDistance* CiHOD::getMetric()
{
	return metric;
}


CKNN* CiHOD::getOutlier(int outlierNum, int parameter)
{
	//int minI=500;
	/**cutoff value*/
	double c = 0;
	int disComputeTimes = 0;
	int size = metricData->size();

	boost::mt19937 rng(time(0));
	boost::uniform_int<> ui(0, size-1);
	
	CDensityPivotSelection *psm1 = 0;
	int pivotSearchSize = 0;
	pivotSearchSize = blockSize<size ? blockSize:size;
	vector<int> pivot;
	int first = parameter;
	pivot = psm1->selectPivots(metric, *metricData, first, pivotSearchSize, 1, first);
	//pivot.push_back( ui(rng) );
	cout<<"pivots: "<<pivot[0]<<endl;
	/**top n outliers, descending*/
	CKNN *TOP_N = new CKNN[n];
	for(int i=0; i<n; i++)
	{
		TOP_N[i].dataID = 0;
		TOP_N[i].dis = 0;
	}
	//priority_queue<CKNN> TOP_NQ;
	/**output result to file*/
	//ofstream file1("D:\\Data\\PSO\\result.txt",ios::app);
	ofstream file2("D:\\Data\\result\\parameter.txt",ios::app);
	ofstream result("D:\\Data\\result\\result.txt",ios::app);
	ofstream file4("D:\\Data\\result\\ScoreOfK.txt",ios::app);
	/**the block process every time*/
	CKNN *B = new CKNN[blockSize];
	for(int i=0; i<blockSize; i++)
	{
		B[i].dataID = 0;
		B[i].dis = 0;
	}
	/**distance of k nearest neighbors, from large to small*/
	CKNN *knn;
	int i = 0, j = 0;
	double dis = 0;

	/**Hide outlier candidateSet, descending order.*/
	vector<shared_ptr<CHKODC> > candidateSet;
	
	
	CKNN *index = new CKNN[size];
	for(int i=0; i<size; i++)
	{
		index[i].dataID = i;
		index[i].dis = metric->getDistance((*metricData)[i].get(),(*metricData)[pivot[0]].get());
		disComputeTimes++;
	}
	sort(index, index+size, greaterCKNN);

	COutlierDefinition **od = new COutlierDefinition*[size];
	for(int i=0; i<size; i++)
	{
		od[i] = outlierDefinition->CreateInstance(k,n);
		od[i]->setState(true);
	}
	int validNum = 0;
	
	int realBlockSize = 0;
	int avgDis = 0;
	int startID = 0;
	int orderID = 0;
	double kDisOfPivot = index[size-k-n].dis;
	bool isStop = false;
	long startTime = GetTickCount();
	while(i<size)
	{
		//if(isStop)
		//	break;
		B[j] = index[i];
		j++;
		
		if((j == blockSize)||(i == size-1))
		{
			if(B[0].dis + kDisOfPivot < c)
			{
				break;
			}
			if((i == size-1) && (size%blockSize))
				realBlockSize = size%blockSize;
			else
				realBlockSize = blockSize;
			avgDis = findAvg(B, realBlockSize);
			startID = getStartID(B, realBlockSize, avgDis);
			for(int d=0; d<size; d++)
			{
				orderID = getSpiralOrder(size, startID, d);
				for(int b=0; b<j; b++)
				{
					knn = od[B[b].dataID]->getKnn();
					
					if((od[B[b].dataID]->getState() == true) && (fabs(B[b].dis-index[orderID].dis)<knn[0].dis) && (B[b].dataID != index[orderID].dataID))
					{
						dis = metric->getDistance((*metricData)[B[b].dataID].get(),(*metricData)[index[orderID].dataID].get());
						disComputeTimes++;
						CKNN tempKNN(index[orderID].dataID, dis);
						/**Try to insert into knn*/
						//knn = od[B[b].dataID]->getKnn();
						if(insertQueue(tempKNN,knn,k+n-1,true))
						{
							od[B[b].dataID]->setWeight();
							od[B[b].dataID]->setNKWeight();
							/**Never set false until its weight and NKWeight are smaller than the cutoff value.  This is different from ORCA algorithm*/
							if(od[B[b].dataID]->getNKWeight()<c)
							{
								od[B[b].dataID]->setState(false);
							}
						}
					}
				}
			}
			
			/**compute TOP N KnnOutliers and candidateSet which have higher weight than Nth KnnOutlier.*/
			for(int b=0; b<j; b++)
			{
				if((od[B[b].dataID]->getState() == true))
				{
					double weight = od[B[b].dataID]->getWeight();
					int dataID = B[b].dataID;
					CKNN tempKNN(dataID,weight);
					/**Try to insert into TOP_N KnnOutliers */
					if(insertQueue(tempKNN, TOP_N, n, false))
						validNum++;

					/**update the cutoff value c.  Remember TOP_N is ascending order. */
					c = TOP_N[n-1].dis;

					vector<shared_ptr<CHKODC> >::iterator it;
					/**remove object from candidate if its NKWeight is smaller than the cutoff value c.*/
					if(!candidateSet.empty())
					{
						it = --candidateSet.end();
					
						while(it!=candidateSet.begin() && (*it)->getNKWeight()<c)
						{
							candidateSet.erase(it--);
						}
					}
					od[B[b].dataID]->setNKWeight();
					shared_ptr<CHKODC> tempHODC(new CHKODC(dataID, k, n, od[B[b].dataID]->getNKWeight(), od[dataID]->getKnn()));
					
					/**insert to outlier candidateSet if its NKWeight larger than the cutoff value c*/
					if(od[B[b].dataID]->getNKWeight() > c)
					{
						insertQueue(tempHODC,candidateSet);
					}
					
				}
			}
			j = 0;
		}
		i++;
	}
	cout<<"n="<<n<<"  candidateSet size:"<<candidateSet.size()<<endl;
	file2<<"n="<<n<<"  candidateSet size:"<<candidateSet.size()<<endl;
	//file3<<size<<"\t"<<n<<"\t"<<candidateSet.size()<<endl;
	//file3.close();
	//double *dataList;
	int ORCAscore = 0;
	int sumScore = 0;
	bool *trueState = new bool[n];
	ROCPoint *roc;
	double ORCAauc = 0;
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
		//cout<<"Outlier dataID:"<<TOP_N[i].dataID<<"  Weight:"<<TOP_N[i].dis<<" ";
		//dataList = ((CKddCup99*)((*metricData)[TOP_N[i].dataID].get()))->getData();
		if(!((CDNAClass*)((*metricData)[TOP_N[i].dataID].get()))->getState())
		{
			trueState[i] = false;
			ORCAscore++;
			sumScore += ORCAscore;
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
	}
	double iORCAAccuracy = getAccuracy(trueState, n);
	cout<<endl<<"n:"<<n<<" outlierNum:"<<outlierNum<<" size:"<<size<<" iORCAAccuracy:"<<iORCAAccuracy<<endl;
	roc = getROC(trueState,n,outlierNum,size);
	ORCAauc = getAUC(roc,n);
	outputROC(roc,n);
	cout<<"ORCAscore:"<<ORCAscore<<"  ORCAauc:"<<ORCAauc<<"  sumScore:"<<sumScore<<endl;
	file2<<"ORCAscore:"<<ORCAscore<<"  ORCAauc:"<<ORCAauc<<endl;
	/**compute real TOP N outliers from candidateSet.*/
	/**check every object of candidateSet, set its neighborFlag false if its neighbor is the same to the outlier at the front*/
	for(int j=0; j<candidateSet.size(); j++)
	{
		if(candidateSet[j]->getID() == TOP_N[0].dataID)
			candidateSet[j]->setTopNFlag(false);
	}
	bool topNFlag = true;
	int t=0;
	int knnsID=0, topnID=0;
	//for(int i=n-2; i>=0; i--)
	for(int i=1; i<n; i++)
	{
		topNFlag = true;
		for(int j=0; j<candidateSet.size(); j++)
		{
			knn = candidateSet[j]->getKnn();
			for(int s=0; s<k+n-1; s++)
			{
				if(knn[s].dataID == TOP_N[i-1].dataID)
				{
					candidateSet[j]->setNeighborFlag(s);
				}
			}		
		}
		for(int j=0; j<candidateSet.size(); j++)
		{
			//candidateSet[j]->setWeight(minI);
			candidateSet[j]->setWeight();
			if(candidateSet[j]->getTopNFlag())
			{
				if(topNFlag)
				{
					TOP_N[i].dataID = candidateSet[j]->getID();
					TOP_N[i].dis = candidateSet[j]->getWeight();
					t=j;
					topNFlag = false;
				}
				else
				{
					if(candidateSet[j]->getWeight() > TOP_N[i].dis)
					{
						TOP_N[i].dataID = candidateSet[j]->getID();
						TOP_N[i].dis = candidateSet[j]->getWeight();
						t=j;
					}
				}
			}
		}
		candidateSet[t]->setTopNFlag(false);
	}
	long stopTime = GetTickCount();
	//cout<<"minI:"<<minI<<endl;
	
	int nnID = 0;
	int HODscore = 0;
	sumScore = 0;
	double HODauc = 0;
	for(int i=0; i<n; i++)
	{
		trueState[i] = true;
	}
	
	for(int i=0; i<n; i++)
	{
		//dataList = ((CKddCup99*)((*metricData)[TOP_N[i].dataID].get()))->getData();
		if(!((CDNAClass*)((*metricData)[TOP_N[i].dataID].get()))->getState())
		{
			trueState[i] = false;
			HODscore++;
			sumScore += HODscore;
		}
	}
	/*for(int i=0; i<n; i++)
	{
		if(!trueState[i])
		{
			cout<<i<<" "<<TOP_N[i].dataID<<"\t"<<TOP_N[i].dis<<"\t"<<trueState[i]<<endl;
			cout<<((CDNAClass*)((*metricData)[TOP_N[i].dataID].get()))->getSequence()<<endl;
		}
	}*/
	cout<<"trueState:"<<endl;
	for(int i=0; i<n; i++)
	{
		cout<<trueState[i];
		if(i%10==9)
			cout<<" ";
		if(i%50==49)
			cout<<endl;
	}
	double iHODAccuracy = getAccuracy(trueState, n);
	cout<<endl<<"n:"<<n<<" outlierNum:"<<outlierNum<<" size:"<<size<<" iHODAccuracy:"<<iHODAccuracy<<endl;
	roc = getROC(trueState,n,outlierNum,size);
	HODauc = getAUC(roc,n);
	outputROC(roc,n);
	cout<<"HODscore:"<<HODscore<<"  HODauc:"<<HODauc<<"  sumScore:"<<sumScore<<endl;
	cout<<"time:"<<stopTime-startTime<<endl;
	file2<<"HODscore:"<<HODscore<<"  HODauc:"<<HODauc<<endl;
	result<<size<<"\t"<<k<<"\t"<<n<<"\t"<<ORCAscore<<"\t"<<HODscore<<"\t"<<ORCAauc<<"\t"<<HODauc<<"\t"<<candidateSet.size()<<"\t"<<stopTime-startTime<<"\t"<<disComputeTimes<<endl;
	result.close();
	delete[] B;
	for(int i=0; i<size; i++)
	{
		delete[] od[i];
	}
	delete[] od;
	delete[] index;
	delete[] trueState;
	candidateSet.clear();
	pivot.clear();
	file2.close();
	return TOP_N;
}

CiHOD::~CiHOD()
{
}