/**@file DensityPeakFarestPivotSelection.cpp
 * @brief  select pivots with density peak and farest
 * @author Honglong Xu
 * @date 2018-9-23
*/

#include "../../HeaderFiles/index/DensityPeakFarestPivotSelection.h"
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
int getStartID(CKNN *Block, int blockSize, double avg);
int getSpiralOrder(int size, int startID, int i);
int *randSelect(int totalSize, int randSize);

CDensityPeakFarestPivotSelection::CDensityPeakFarestPivotSelection()
{
}

CDensityPeakFarestPivotSelection::~CDensityPeakFarestPivotSelection()
{
}

vector<int> CDensityPeakFarestPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int numPivots)
{
	 return selectPivots(metric, data,0,data.size(),numPivots);
}

vector<int> CDensityPeakFarestPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> >& data, int first, int dataSize, int numPivots)
{
	        int firstPivot = first; // Math.floor(first + Math.random() *
                                    // dataSize);
            return selectPivots(metric,data,first, dataSize, numPivots,firstPivot);
}

vector<int> CDensityPeakFarestPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int first, int dataSize, int numPivots, int firstPivot)
{
	//boost::mt19937 rng(time(0));
	//boost::uniform_int<> ui(0, size-1);
	long StartTime = GetTickCount();
	int randSize = 5000;
	int *rannum = randSelect(dataSize, randSize);
	long StopTime = GetTickCount();
	cout<<"rand time: "<<StopTime-StartTime<<endl;
	int disComputeTimes = 0;
	double tempDistance = 0;
	CKNN *index = new CKNN[randSize];
	double **pivotSpace = new double *[randSize];
	CKNN farestPoint(0, 0);
	for(int i=0; i<randSize; i++)
	{
		pivotSpace[i] = new double[numPivots];
		for(int j=0; j<numPivots; j++)
		{
			pivotSpace[i][j] = 0;
		}
	}
	for(int i=0; i<randSize; i++)
	{
		tempDistance = metric->getDistance(data[rannum[i]].get(),data[rannum[0]].get());
		disComputeTimes++;
		index[i].dataID = rannum[i];
		index[i].dis = tempDistance;
		for(int j=0; j<numPivots; j++)
		{
			pivotSpace[i][j] = tempDistance;
		}
		if(tempDistance > farestPoint.dis)
		{
			farestPoint.dataID = rannum[i];
			farestPoint.dis = tempDistance;
		}
	}
	sort(index, index+randSize, greaterCKNN);

	double cutoffDistance = 0.4*farestPoint.dis;
	int *rho = new int[randSize];
	for(int i=0; i<randSize; i++)
	{
		rho[i] = 0;
	}
	for(int i=0; i<randSize; i++)
	{
		for(int j=i+1; j<randSize; j++)
		{
			if(abs(pivotSpace[i][0]-pivotSpace[j][0]) < cutoffDistance)
			{
				tempDistance = metric->getDistance(data[rannum[i]].get(),data[rannum[j]].get());
				disComputeTimes++;
				if(tempDistance < cutoffDistance)
				{
					rho[i]++;
					rho[j]++;
				}
			}
		}
	}
	int maxID = 0;
	int maxRHO = 0;
	for(int i=0; i<randSize; i++)
	{
		if(maxRHO < rho[i])
		{
			maxRHO = rho[i];
			maxID = rannum[i];
		}
		//cout<<"rho:"<<rho[i]<<"  maxRHO:"<<maxRHO<<endl;
	}
	cout<<"disComputeTimes of pivot selection: "<<disComputeTimes<<endl;
	vector<int> result;
	result.push_back(maxID);
	//result.push_back(farestPoint.dataID);
	return result;
}

