/**@file DensityPivotSelection.cpp
 * @brief  select pivots with density
 * @author Honglong Xu
 * @date 2015-10-22
*/

#include "../../HeaderFiles/index/DensityPivotSelection.h"
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

CDensityPivotSelection::CDensityPivotSelection()
{
}

CDensityPivotSelection::~CDensityPivotSelection()
{
}

vector<int> CDensityPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int numPivots)
{
	 return selectPivots(metric, data,0,data.size(),numPivots);
}

vector<int> CDensityPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> >& data, int first, int dataSize, int numPivots)
{
	        int firstPivot = first; // Math.floor(first + Math.random() *
                                    // dataSize);
            return selectPivots(metric,data,first, dataSize, numPivots,firstPivot);
}

vector<int> CDensityPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int first, int dataSize, int numPivots, int firstPivot)
{
	//ofstream pivotSelectionResult("D:\\Data\\result\\pivotSelectionResult.txt",ios::app);
	if (numPivots >= dataSize) //if the number of pivots required is larger than dataSize then return all the points in the data(duplicate removed).
	{
		vector<int>* pivots=new vector<int>;
		for (int i = first; i < first+dataSize + 0; i++)
			pivots->push_back(i);
		return removeDuplicate(metric, data, *pivots,first,dataSize);
	}
	int disComputeTimes = 0;
	CKNN *index = new CKNN[dataSize];
	for(int i=first; i<first+dataSize; i++)
	{
		index[i-first].dataID = i;
		index[i-first].dis = metric->getDistance((data)[firstPivot].get(),(data)[i].get());
		disComputeTimes++;
	}
	sort(index, index+dataSize);
	/*for(int i=0; i<dataSize; i++)
	{
		pivotSelectionResult<<index[i].dataID<<"\t"<<index[i].dis<<endl;
	}
	pivotSelectionResult.close();*/
	int segNum = 0;
	segNum = dataSize<numPivots*2 ? dataSize : numPivots*2;
	if(segNum < 10)
		segNum = 10;

	int perSeg = dataSize/segNum;
	CKNN *segIndex = new CKNN[segNum];
	for(int i=0; i<segNum-1; i++)
	{
		segIndex[i].dataID = i*perSeg;
		segIndex[i].dis = index[segIndex[i].dataID+perSeg].dis - index[segIndex[i].dataID].dis;
	}
	segIndex[segNum-1].dataID = (segNum-1)*perSeg;
	segIndex[segNum-1].dis = index[dataSize-1].dis - index[segIndex[segNum-1].dataID].dis;
	sort(segIndex, segIndex+segNum);
	/*for(int i=0; i<segNum; i++)
	{
		cout<<i<<"  "<<segIndex[i].dataID<<"  "<<segIndex[i].dis<<endl;
	}*/
	vector<int> pivots;
	for(int i=0; i<numPivots; i++)
	{
		pivots.push_back(index[segIndex[i].dataID+perSeg/2].dataID);
	}
	cout<<"disComputeTimes: "<<disComputeTimes<<endl;
	return pivots;
}

