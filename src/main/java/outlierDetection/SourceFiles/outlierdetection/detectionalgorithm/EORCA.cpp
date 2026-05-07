#include "../../../HeaderFiles/outlierdetection/detectionalgorithm/EORCA.h"
#include "../../../HeaderFiles/metricdata/DoubleVector.h"
#include "../../../HeaderFiles/metricdata/KddCup99.h"
#include "../../../HeaderFiles/metricdata/DoubleVectorClass.h"
#include "windows.h"
#include <psapi.h>

#pragma comment(lib,"psapi.lib") 

template<typename T>
extern bool insertQueue(T &data, T *dataQueue, int k, bool isDescend);
ROCPoint* getROC(bool *trueState, int n, double totalOutlierNum, int size);
double getAccuracy(bool *trueState, int n);
double getAUC(ROCPoint* ROC, int n);
void outputROC(ROCPoint* ROC, int n);

CEORCA::CEORCA(vector<shared_ptr<CMetricData>> &md, COutlierDefinition *_od, CMetricDistance *metrictype, int _n, int _k, int _b, int _m)
{
	metricData = &md;
	outlierDefinition = _od;
	metric = metrictype;
	n = _n;
	k = _k;
	blockSize = _b;
	m = _m;
}

double CEORCA::getTempWeight(CKNN * knn, int kNum)
{
	if(knn[0].dis == (std::numeric_limits<double>::max)())
	{
		return (std::numeric_limits<double>::max)();
	}
	else
	{
		double sum = 0;
		for(int i=0; i<kNum; i++)
		{
			sum += knn[i].dis;
		}
		return sum;
	}
}

CMetricDistance* CEORCA::getMetric()
{
	return metric;
}

CKNN* CEORCA::getOutlier(int outlierNum, int parameter)
{
	ofstream file3("D:\\Data\\result\\result.txt",ios::app);
	/**cutoff value*/
	double c = 0;
	/**top n outliers, from small to large*/
	CKNN *TOP_N = new CKNN[n];

	for(int i=0; i<n; i++)
	{
		TOP_N[i].dataID = 0;
		TOP_N[i].dis = 0;
	}
	/**the block process every time*/
	int *B = new int[blockSize];
	memset(B, 0, blockSize*sizeof(int));
	/**distance of k nearest neighbors, from large to small*/
	/*double *knnd;// = new double[k];
	//memset(knnd, 0, k*sizeof(double));
	int *knn;*/

	int size = metricData->size();

	COutlierDefinition **od = new COutlierDefinition*[size];
	for(int i=0; i<size; i++)
	{
		od[i] = outlierDefinition->CreateInstance(k,m);
		od[i]->setState(true);
		//od[i]->setNeighborNum(0);
	}

	CKNN *knn;
	CKNN **tempKNN = new CKNN*[blockSize];
	for(int i=0; i<blockSize; i++)
	{
		tempKNN[i] = new CKNN[m+k];
	}
	long startTime = GetTickCount();
	double dis = 0;
	//int neighborNum = 0;
	int validNum = 0;
	double tempWeight = 0;
	int i = 0, j = 0;
	int disComputeTimes = 0;
	while(i<size)
	{
		B[j] = i;
		j++;

		if((j == blockSize)||(i == size-1))
		{
			for(int bs=0; bs<blockSize; bs++)
			{
				for(int mk=0; mk<m+k; mk++)
				{
					tempKNN[bs][mk].reset();
				}
			}
			for(int d=0; d<size; d++)
			{
				for(int b=0; b<j; b++)
				{
					if((od[B[b]]->getState() == true) && (B[b] !=d))
					{
						dis = metric->getDistance((*metricData)[B[b]].get(),(*metricData)[d].get());
						disComputeTimes++;
						CKNN tempKnn(d,dis);
						if(insertQueue(tempKnn, tempKNN[b], m+k, true))
						{
							tempWeight = getTempWeight(tempKNN[b], k);
							if(tempWeight < c)
							{
								od[B[b]]->setState(false);
							}
						}	
					}
				}
			}
			for(int b=0; b<j; b++)
			{
				if(od[B[b]]->getState() == true)
				{
					od[B[b]]->setKnnd(tempKNN[b], k);
					od[B[b]]->setWeight();
				}
			}
			/**compute current TOP N outliers.*/
			for(int b=0; b<j; b++)
			{
				if(od[B[b]]->getState() == true)
				{
					double weight = od[B[b]]->getWeight();
					int dataID = B[b];
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
		cout<<"Outlier dataID:"<<TOP_N[i].dataID<<"  Weight:"<<TOP_N[i].weight<<endl;
	}*/
	cout<<"disComputeTimes: "<<disComputeTimes<<endl;
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
		if(!((CKddCup99*)((*metricData)[TOP_N[i].dataID].get()))->getState())
		{
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
	}
	//double CNEDAccuracy = getAccuracy(trueState, n);
	//cout<<"CNEDAccuracy:"<<CNEDAccuracy<<endl;
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
	file3<<size<<"\t"<<k<<"\t"<<m<<"\t"<<n<<"\t"<<score<<"\t"<<auc<<"\t"<<pmc.WorkingSetSize / 1024.0<<"\t"<<pmc.PeakWorkingSetSize / 1024.0<<"\t"<<stopTime-startTime<<endl;
	file3.close();
	delete[] B;
	for(int i=0; i<size; i++)
	{
		delete[] od[i];
	}
	delete[] od;
	//delete[] knnd;
	return TOP_N;
}

CEORCA::~CEORCA()
{
}