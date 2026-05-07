#include "../../../HeaderFiles/outlierdetection/detectionalgorithm/ORCA.h"
#include "../../../HeaderFiles/metricdata/KddCup99.h"
#include "../../../HeaderFiles/metricdata/DoubleVectorClass.h"
#include "../../../HeaderFiles/metricdata/DNAClass.h"
#include <ctime>

template<typename T>
bool insertQueue(T &data, T *dataQueue, int k, bool isDescend);
ROCPoint* getROC(bool *trueState, int n, double totalOutlierNum, int size);
double getAccuracy(bool *trueState, int n);
double getAUC(ROCPoint* ROC, int n);
void outputROC(ROCPoint* ROC, int n);

CORCA::CORCA(vector<shared_ptr<CMetricData>> &md, COutlierDefinition *_od, CMetricDistance *metrictype, int _n, int _k, int _b)
{
	metricData = &md;
	outlierDefinition = _od;
	metric = metrictype;
	n = _n;
	k = _k;
	blockSize = _b;
}

CMetricDistance* CORCA::getMetric()
{
	return metric;
}

CKNN* CORCA::getOutlier(int p1, int p2)
{
	ofstream result("D:\\Data\\result\\result.txt",ios::app);
	//result<<"ORCA:"<<endl;
	ofstream cutoffvalue("D:\\Data\\result\\cutoffvalue.txt",ios::app);
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

	int size = metricData->size();

	COutlierDefinition **od = new COutlierDefinition*[size];
	for(int i=0; i<size; i++)
	{
		od[i] = outlierDefinition->CreateInstance(k);
		od[i]->setState(true);
		od[i]->setNeighborNum(0);
	}

	CKNN *knn;
	long startTime = clock();
	double dis = 0;
	int neighborNum = 0;
	int i = 0, j = 0;
	int validNum = 0;
	unsigned long long int disComputeTimes = 0;

	while(i<size)
	{
		B[j] = i;
		j++;

		if((j == blockSize)||(i == size-1))
		{
			cout<<"c="<<c<<endl;
			//cutoffvalue<<c<<endl;
			for(int d=0; d<size; d++)
			{
				for(int b=0; b<j; b++)
				{
					if((od[B[b]]->getState() == true) && (B[b] !=d))
					{
						dis = metric->getDistance((*metricData)[B[b]].get(),(*metricData)[d].get());
						disComputeTimes++;
						neighborNum = od[B[b]]->getNeighborNum();
						knn = od[B[b]]->getKnn();
						CKNN tempKnn(d,dis);
						if(insertQueue(tempKnn, knn, k, true))
						{
							if(od[B[b]]->getNeighborNum() < k)
							{
								od[B[b]]->setNeighborNum(neighborNum+1);
							}
							od[B[b]]->setWeight();
							/**When an object's closest neighbors achieve a score lower than the cutoff value we remove the object because it can no longer be an outlier*/
							if(od[B[b]]->getWeight() < c)
							{
								od[B[b]]->setState(false);
							}
						}	
					}
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
			//cout<<"Distance computing times: "<<disComputeTimes<<"  Cut off value: "<<c<<endl;
			//disComputeTimes = 0;
		}
		i++;
	}
	long stopTime = clock();
	/*for(int i=0; i<n; i++)
	{
		cout<<"Outlier dataID:"<<TOP_N[i].dataID<<"  Weight:"<<TOP_N[i].weight<<endl;
	}*/
	cout<<"Distance computing times: "<<disComputeTimes<<endl;
	/*
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
		//if(!((CDNAClass*)((*metricData)[TOP_N[i].dataID].get()))->getState())
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
	}
	roc = getROC(trueState,n,outlierNum,size);
	auc = getAUC(roc,n);
	outputROC(roc,n);
	cout<<"score:"<<score<<"  AUC:"<<auc<<endl;
	delete[] trueState;
	*/
	result<<size<<"\t"<<k<<"\t"<<n<<"\t"<<"\t"<<stopTime-startTime<<"\t"<<disComputeTimes<<endl;
	result.close();
	cout<<"time:"<<stopTime-startTime<<endl;
	delete[] B;
	for(int i=0; i<size; i++)
	{
		delete[] od[i];
	}
	delete[] od;
	return TOP_N;
}

CORCA::~CORCA()
{
}