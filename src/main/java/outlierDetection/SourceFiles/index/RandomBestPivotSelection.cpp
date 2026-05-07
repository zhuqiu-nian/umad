/**@file RandomBestPivotSelection.cpp
 * @brief  select pivots with density peak
 * @author Honglong Xu
 * @date 2018-10-1
*/

#include "../../HeaderFiles/index/RandomBestPivotSelection.h"
#include "../../../../../HeaderFiles/outlierdetection/outlierdefinition/KNN.h"
#include <algorithm>
#include <cfloat>
#include <stdlib.h>
#include <time.h>
#include "windows.h"

/**
 * @brief check the array of pivots, remove the duplicate.
 * @param metric
 * @param data
 * @param pivots
 * @return 
 */
vector<int> removeDuplicate(CMetricDistance *metric,vector<shared_ptr<CMetricData> > data,vector<int> &pivots,int first,int dataSize);
int getIndexID(CKNN *Block, int blockSize, double avg);
bool greaterCKNN(const CKNN &a, const CKNN &b);
int *randSelect(int totalSize, int randSize);

CRandomBestPivotSelection::CRandomBestPivotSelection()
{
}

CRandomBestPivotSelection::~CRandomBestPivotSelection()
{
}

vector<int> CRandomBestPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int numPivots)
{
	 return selectPivots(metric, data,0,data.size(),numPivots);
}

vector<int> CRandomBestPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> >& data, int ranPivotSize, int dataSize, int numPivots)
{
	        int firstPivot = ranPivotSize; // Math.floor(first + Math.random() * dataSize);
            return selectPivots(metric,data,ranPivotSize, dataSize, numPivots,firstPivot);
}

vector<int> CRandomBestPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int ranPivotSize, int dataSize, int numPivots, int firstPivot)
{
	//随机抽取ranPivotSize个点，取其中密度最大者为支撑点，截断距离以随机点之最大距离为依据计算。
	int *ranPivots = randSelect(dataSize, ranPivotSize);
	int disComputeTimes = 0;
	double tempDistance = 0;
	double maxDistance = 0;
	int maxID1 = 0;
	int maxID2 = 0;
	
	for(int i=0; i<ranPivotSize; i++)
	{
		for(int j=i+1; j<ranPivotSize; j++)
		{
			tempDistance = metric->getDistance(data[ranPivots[i]].get(),data[ranPivots[j]].get());
			disComputeTimes++;
			if(tempDistance > maxDistance)
			{
				maxDistance = tempDistance;
				maxID1 = ranPivots[i];
				maxID2 = ranPivots[j];
			}
		}
	}
	
	double cutoffDistance = 0.4*maxDistance;
	double *pivotSpace = new double[dataSize];
	for(int i=0; i<dataSize; i++)
	{
		pivotSpace[i] = 0;
	}
	cout<<"ranPivotSize="<<ranPivotSize<<", dataSize="<<dataSize<<endl;
	cout<<"maxID1="<<maxID1<<", maxID2="<<maxID2<<endl;
	for(int i=0; i<dataSize; i++)
	{
		pivotSpace[i] = metric->getDistance(data[i].get(),data[maxID1].get());
		disComputeTimes++;
		//cout<<i<<endl;
	}

	int *rho = new int[ranPivotSize];
	for(int i=0; i<dataSize; i++)
	{
		for(int j=0; j<ranPivotSize; j++)
		{
			if(abs(pivotSpace[i] - pivotSpace[ranPivots[j]]) < cutoffDistance)
			{
				tempDistance = metric->getDistance(data[i].get(),data[ranPivots[j]].get());
				disComputeTimes++;
				if(tempDistance < cutoffDistance)
				{
					rho[j]++;
				}
			}
		}
	}
	
	int maxID = 0;
	int maxRHO = 0;
	for(int i=0; i<ranPivotSize; i++)
	{
		if(maxRHO < rho[i])
		{
			maxRHO = rho[i];
			maxID = ranPivots[i];
		}
		//cout<<"rho:"<<rho[i]<<"  maxRHO:"<<maxRHO<<endl;
	}
	cout<<"disComputeTimes of pivot selection: "<<disComputeTimes<<endl;
	vector<int> result;
	result.push_back(maxID);
	delete[] ranPivots;
	delete[] pivotSpace;
	delete[] rho;
	return result;
}

