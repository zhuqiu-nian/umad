/**@file DensityFarestPivotSelection.cpp
 * @brief  select pivots with density first, then FFT algorithm
 * @author Honglong Xu
 * @date 2015-10-22
*/

#include "../../HeaderFiles/index/DensityDisParPivotSelection.h"
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

CDensityDisParPivotSelection::CDensityDisParPivotSelection()
{
}

CDensityDisParPivotSelection::~CDensityDisParPivotSelection()
{
}

vector<int> CDensityDisParPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int numPivots)
{
	 return selectPivots(metric, data,0,data.size(),numPivots);
}

vector<int> CDensityDisParPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> >& data, int first, int dataSize, int numPivots)
{
	        int firstPivot = first; // Math.floor(first + Math.random() *
                                    // dataSize);
            return selectPivots(metric,data,first, dataSize, numPivots,firstPivot);
}

vector<int> CDensityDisParPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int first, int dataSize, int numPivots, int firstPivot)
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
	int newFirstPivot = 0;
	double farestDistance = 0;
	double tempDistance = 0;
	for(int i=first; i<first+dataSize; i++)
	{
		
		tempDistance = metric->getDistance(data[firstPivot].get(),data[i].get());
		disComputeTimes++;
		if(farestDistance < tempDistance)
		{
			newFirstPivot = i;
			farestDistance = tempDistance;
		}
	}
	//newFirstPivot = firstPivot;
	for(int i=first; i<first+dataSize; i++)
	{
		index[i-first].dataID = i;
		index[i-first].dis = metric->getDistance(data[newFirstPivot].get(),data[i].get());
		disComputeTimes++;
	}
	sort(index, index+dataSize);

	int segNum = 0;
	segNum = dataSize<numPivots*2 ? dataSize : numPivots*2;
	if(segNum < 10)
		segNum = 10;

	int perSeg = dataSize/segNum;
	CKNN *segIndex = new CKNN[segNum];
	CKNN *neighborNum = new CKNN[segNum];
	double segDis = index[dataSize-1].dis/segNum;
	//cout<<"segDis:"<<segDis<<endl;
	for(int i=0; i<segNum; i++)
	{
		segIndex[i].dataID = getIndexID(index, dataSize, i*segDis);
		//cout<<i<<"\t"<<segIndex[i].dataID<<endl;
	}
	for(int i=0; i<segNum-1; i++)
	{
		segIndex[i].dis = segIndex[i+1].dataID - segIndex[i].dataID;
	}
	segIndex[segNum-1].dis = dataSize - segIndex[segNum-1].dataID;
	for(int i=0; i<segNum; i++)
	{
		neighborNum[i].dataID = i;
		neighborNum[i].dis = segIndex[i].dis;
	}
	sort(segIndex, segIndex+segNum,greaterCKNN);
	sort(neighborNum, neighborNum+segNum,greaterCKNN);
	/*for(int i=0; i<segNum; i++)
	{
		cout<<segIndex[i].dataID<<"\t"<<segIndex[i].dis<<endl;
	}*/
	vector<int> pivotCandidateSet;
	for(int i=0; i<segNum; i++)
	{
		pivotCandidateSet.push_back(index[segIndex[i].dataID+int(segIndex[i].dis/2)].dataID);
		//cout<<"pivotCandidateSet:"<<pivotCandidateSet[i]<<"\t"<<segIndex[i].dis<<"\t"<<neighborNum[i].dataID<<endl;
	}
	//cout<<"disComputeTimes: "<<disComputeTimes<<endl;
	 vector<int> result;
	 int pivotID = 0;
	 result.push_back(pivotCandidateSet[0]);
	 for(int i=1; i<segNum; i++)
	 {
		 if(!(neighborNum[i].dataID + neighborNum[i+1].dataID-2*neighborNum[i-1].dataID))
		 {
			 if(neighborNum[i-1].dataID >= segNum/2)
			 {
				 pivotID = neighborNum[i].dataID < neighborNum[i+1].dataID ? pivotCandidateSet[i] : pivotCandidateSet[i+1];
				 result.push_back(pivotID);
				 i++;
			 }
			 else
			 {
				 pivotID = neighborNum[i].dataID > neighborNum[i+1].dataID ? pivotCandidateSet[i] : pivotCandidateSet[i+1];
				 result.push_back(pivotID);
				 i++;
			 }
		 }
		 else
		 {
			 pivotID = pivotCandidateSet[i];
			 result.push_back(pivotID);
			 if(segIndex[i].dis < perSeg/10)
			 {
				 break;
			 }
			  //pivotID = abs(neighborNum[i-1].dataID-neighborNum[i].dataID) > abs(neighborNum[i-1].dataID-neighborNum[i+1].dataID) ? pivotCandidateSet[i] : pivotCandidateSet[i+1];
		 }
		 if(result.size()==numPivots)
			 break;

		 /*pivotID = 2*i;
		 if(segIndex[pivotID].dis > perSeg/10)
		 {
			result.push_back(pivotCandidateSet[pivotID]);
		 }
		 else
		 {
			 result.push_back(pivotCandidateSet[pivotID-1]);
			 if(i<numPivots-1)
			 {
				 result.push_back(pivotCandidateSet[segNum-1]);
				 break;
			 }
			 else
			 {
				 break;
			 }
		 }*/
	 }
	 //result[1] = pivotCandidateSet[1];
	 return result;
}

