#include "../../../HeaderFiles/outlierdetection/detectionalgorithm/HOD.h"
#include "../../../HeaderFiles/metricdata/DoubleVector.h"
#include "../../../HeaderFiles/metricdata/KddCup99.h"
#include "../../../HeaderFiles/metricdata/DoubleVectorClass.h"
#include "../../../HeaderFiles/metricdata/DNAClass.h"
#include "../../../HeaderFiles/outlierdetection/outlierdefinition/HODC.h"
#include "../../../../../HeaderFiles/outlierdetection/outlierdefinition/KNN.h"
//#include <queue>
#include "windows.h"

template<typename T>
bool insertQueue(T &data, T *dataQueue, int k, bool insertPosition);

ROCPoint* getROC(bool *trueState, int n, double totalOutlierNum, int size);
double getAccuracy(bool *trueState, int n);
double getAUC(ROCPoint* ROC, int n);
void outputROC(ROCPoint* ROC, int n);

/**Insert into Outlier candidateSet, which is from large to small*/
bool insertQueue(shared_ptr<CHODC> &data, vector<shared_ptr<CHODC> > &dataQueue);


CHOD::CHOD(vector<shared_ptr<CMetricData> > &md, COutlierDefinition *_od, CMetricDistance *metrictype, int _n, int _k, int _b)
{
	metricData = &md;
	outlierDefinition = _od;
	metric = metrictype;
	n = _n;
	k = _k;
	blockSize = _b;
}

CMetricDistance* CHOD::getMetric()
{
	return metric;
}

/**upgrate from ORCA algorithm, keep the cutoff value c, current TOP_N KnnOutliers and hide outlier candidateSet int the memory.*/
/**Hide outlier candidateSet are the objects that have larger NKWeight than c, and NKWeight is total distances from nth to (n+k-1)th nearest neighbors.*/
CKNN* CHOD::getOutlier(int outlierNum, int parameter)
{
	//int minI=500;
	/**cutoff value*/
	double c = 0;
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
	int *B = new int[blockSize];
	memset(B, 0, blockSize*sizeof(int));
	/**distance of k nearest neighbors, from large to small*/
	CKNN *knn;
	int i = 0, j = 0;
	double dis = 0;
	//CKNN tempKNN(0,0);
	//for(int i=0;i<n;i++)
	//	TOP_NQ.push(tempKNN);
	/**Hide outlier candidateSet, descending order.*/
	vector<shared_ptr<CHODC> > candidateSet;
	
	int size = metricData->size();
	int normalNum = size - outlierNum;
	double outlierDistance = 0;
	double normalDistance = 0;
	double avgDistance = 0;
	double outlierToNormal = 0;
	/*for(int i=0; i<size; i++)
	{
		for(int j=0; j<size; j++)
		{
			avgDistance += metric->getDistance((*metricData)[i].get(),(*metricData)[j].get());
			if((!((CDoubleVectorClass*)((*metricData)[i].get()))->getState())&&(!((CDoubleVectorClass*)((*metricData)[j].get()))->getState()))
			{
				outlierDistance += metric->getDistance((*metricData)[i].get(),(*metricData)[j].get());
			}
			if((((CDoubleVectorClass*)((*metricData)[i].get()))->getState())&&(((CDoubleVectorClass*)((*metricData)[j].get()))->getState()))
			{
				normalDistance += metric->getDistance((*metricData)[i].get(),(*metricData)[j].get());
			}
			if((!((CDoubleVectorClass*)((*metricData)[i].get()))->getState())&&(((CDoubleVectorClass*)((*metricData)[j].get()))->getState()))
			{
				outlierToNormal += metric->getDistance((*metricData)[i].get(),(*metricData)[j].get());
			}
		}
	}
	avgDistance /= size*(size-1);
	outlierDistance /= outlierNum*(outlierNum-1);
	normalDistance /= normalNum*(normalNum-1);
	outlierToNormal /= normalNum*outlierNum;
	cout<<"avgDistance:"<<avgDistance<<endl;
	cout<<"outlierDistance:"<<outlierDistance<<endl;
	cout<<"normalDistance:"<<normalDistance<<endl;
	cout<<"outlierToNormal:"<<outlierToNormal<<endl;*/

	COutlierDefinition **od = new COutlierDefinition*[size];
	for(int i=0; i<size; i++)
	{
		od[i] = outlierDefinition->CreateInstance(k,n);
		od[i]->setState(true);
	}
	int validNum = 0;
	long startTime = GetTickCount();
	int searchTimes = 0;
	int inlierNum = 0;
	while(i<size)
	{
		B[j] = i;
		j++;
		
		if((j == blockSize)||(i == size-1))
		{
			cout<<"c="<<c<<endl;
			searchTimes = 0;
			for(int d=0; d<size; d++)
			{
				for(int b=0; b<j; b++)
				{
					if((od[B[b]]->getState() == true) && (B[b] !=d))
					{
						dis = metric->getDistance((*metricData)[B[b]].get(),(*metricData)[d].get());
						CKNN tempKNN(d,dis);
						/**Try to insert into knn*/
						knn = od[B[b]]->getKnn();
						if(insertQueue(tempKNN,knn,k+n-1,true))
						{
							od[B[b]]->setWeight();
							od[B[b]]->setNKWeight();
							/**Never set false until its weight and NKWeight are smaller than the cutoff value.  This is different from ORCA algorithm*/
							//if((od[B[b]]->getWeight()<c) && (od[B[b]]->getNKWeight()<c))
							if(od[B[b]]->getNKWeight()<c)
							{
								od[B[b]]->setState(false);
								inlierNum ++;
							}
						}
					}
				}
			}
			
			/**compute TOP N KnnOutliers and candidateSet which have higher weight than Nth KnnOutlier.*/
			for(int b=0; b<j; b++)
			{
				if((od[B[b]]->getState() == true))
				{
					double weight = od[B[b]]->getWeight();
					int dataID = B[b];
					CKNN tempKNN(dataID,weight);
					/**Try to insert into TOP_N KnnOutliers */
					if(insertQueue(tempKNN, TOP_N, n, false))
						validNum++;
					//if((TOP_NQ.size()<n)||(TOP_NQ.size()!=0 && weight>TOP_NQ.top().dis))
					
						//cout<<"push:"<<tempKNN.dataID<<" "<<tempKNN.dis<<endl;
						//TOP_NQ.push(tempKNN);
					
					//while(TOP_NQ.size()>n)
					//	TOP_NQ.pop();
					/**update the cutoff value c.  Remember TOP_N is ascending order. */
					c = TOP_N[n-1].dis;
					//c = TOP_NQ.top().dis;
					//cout<<"c="<<c<<endl;
					//cout<<"size:"<<candidateSet.size()<<endl;
					vector<shared_ptr<CHODC> >::iterator it;
					/**remove object from candidate if its NKWeight is smaller than the cutoff value c.*/
					if(!candidateSet.empty())
					{
						it = --candidateSet.end();
					
						while(it!=candidateSet.begin() && (*it)->getNKWeight()<c)
						{
							candidateSet.erase(it--);
						}
					}
					od[B[b]]->setNKWeight();
					shared_ptr<CHODC> tempHODC(new CHODC(dataID, k, n, od[B[b]]->getNKWeight(), od[dataID]->getKnn()));
					
					/**insert to outlier candidateSet if its NKWeight larger than the cutoff value c*/
					if(od[B[b]]->getNKWeight() > c)
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
	double *dataList;
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
		//dataList = ((CDNAClass*)((*metricData)[TOP_N[i].dataID].get()))->getData();
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
	double ORCAAccuracy = getAccuracy(trueState, n);
	cout<<endl<<"n:"<<n<<" outlierNum:"<<outlierNum<<" size:"<<size<<"  ORCAAccuracy:"<<ORCAAccuracy<<endl;
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
		//cout<<"Outlier dataID:"<<TOP_N[i].dataID<<"  Weight:"<<TOP_N[i].dis<<"  "<<endl;
		//dataList = ((CKddCup99*)((*metricData)[TOP_N[i].dataID].get()))->getData();
		if(!((CDNAClass*)((*metricData)[TOP_N[i].dataID].get()))->getState())
		{
			trueState[i] = false;
			HODscore++;
			sumScore += HODscore;
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
	double HODAccuracy = getAccuracy(trueState, n);
	cout<<endl<<"n:"<<n<<" outlierNum:"<<outlierNum<<" size:"<<size<<"  HODAccuracy:"<<HODAccuracy<<endl;
	roc = getROC(trueState,n,outlierNum,size);
	HODauc = getAUC(roc,n);
	outputROC(roc,n);
	cout<<"HODscore:"<<HODscore<<"  HODauc:"<<HODauc<<"  sumScore:"<<sumScore<<endl;
	cout<<"time:"<<stopTime-startTime<<endl;
	file2<<"HODscore:"<<HODscore<<"  HODauc:"<<HODauc<<endl;
	result<<size<<"\t"<<k<<"\t"<<n<<"\t"<<ORCAscore<<"\t"<<HODscore<<"\t"<<ORCAauc<<"\t"<<HODauc<<"\t"<<candidateSet.size()<<"\t"<<stopTime-startTime<<endl;
	result.close();
	delete[] B;
	for(int i=0; i<size; i++)
	{
		delete[] od[i];
	}
	delete[] od;
	delete[] trueState;
	candidateSet.clear();
	file2.close();
	return TOP_N;
}

CHOD::~CHOD()
{
}