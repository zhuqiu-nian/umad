/**@file DensityPeakPivotSelection.cpp
 * @brief  select pivots with density peak
 * @author Honglong Xu
 * @date 2016-8-4
*/

#include "../../HeaderFiles/index/DensityPeakPivotSelection.h"
#include "../../../../../HeaderFiles/outlierdetection/outlierdefinition/KNN.h"
#include <algorithm>
#include <cfloat>
#include <stdlib.h>
#include <time.h>

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

CDensityPeakPivotSelection::CDensityPeakPivotSelection()
{
}

CDensityPeakPivotSelection::~CDensityPeakPivotSelection()
{
}

vector<int> CDensityPeakPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int numPivots)
{
	 return selectPivots(metric, data,0,data.size(),numPivots);
}

vector<int> CDensityPeakPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> >& data, int first, int dataSize, int numPivots)
{
	        int firstPivot = first; // Math.floor(first + Math.random() *
                                    // dataSize);
            return selectPivots(metric,data,first, dataSize, numPivots,firstPivot);
}

vector<int> CDensityPeakPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int first, int dataSize, int numPivots, int firstPivot)
{
	double *initDistance = new double[dataSize];
	int disComputeTimes = 0;
	double tempDistance = 0;
	CKNN farestPoint(0, 0);
	for(int i=first; i<first+dataSize; i++)
	{
		tempDistance = metric->getDistance(data[firstPivot].get(),data[i].get());
		disComputeTimes++;
		initDistance[i-first] = tempDistance;
		if(tempDistance > farestPoint.dis)
		{
			farestPoint.dataID = i;
			farestPoint.dis = tempDistance;
		}
	}
	cout<<"farestPoint.dataID: "<<farestPoint.dataID<<endl;
	double cutoffDistance = 0.4*farestPoint.dis;
	int *rho = new int[dataSize];
	for(int i=0; i<dataSize; i++)
	{
		rho[i] = 0;
	}
	for(int i=first; i<first+dataSize; i++)
	{
		for(int j=i+1; j<first+dataSize; j++)
		{
			if(abs(initDistance[i-first]-initDistance[j-first]) < cutoffDistance)
			{
				tempDistance = metric->getDistance(data[i].get(),data[j].get());
				disComputeTimes++;
				if(tempDistance < cutoffDistance)
				{
					rho[i-first]++;
					rho[j-first]++;
				}
			}
		}
	}
	int maxID = 0;
	int maxRHO = 0;
	for(int i=0; i<dataSize; i++)
	{
		if(maxRHO < rho[i])
		{
			maxRHO = rho[i];
			maxID = i+first;
		}
		//cout<<"rho:"<<rho[i]<<"  maxRHO:"<<maxRHO<<endl;
	}
	cout<<"disComputeTimes of pivot selection: "<<disComputeTimes<<endl;
	vector<int> result;
	result.push_back(maxID);
	delete[] rho;
	return result;
}

