/**@file DensityFarestPivotSelection.cpp
 * @brief  select pivots with density first, then FFT algorithm
 * @author Honglong Xu
 * @date 2015-10-22
*/

#include "../../HeaderFiles/index/DensityFarestPivotSelection.h"
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

CDensityFarestPivotSelection::CDensityFarestPivotSelection()
{
}

CDensityFarestPivotSelection::~CDensityFarestPivotSelection()
{
}

vector<int> CDensityFarestPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int numPivots)
{
	 return selectPivots(metric, data,0,data.size(),numPivots);
}

vector<int> CDensityFarestPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> >& data, int first, int dataSize, int numPivots)
{
	        int firstPivot = first; // Math.floor(first + Math.random() *
                                    // dataSize);
            return selectPivots(metric,data,first, dataSize, numPivots,firstPivot);
}

vector<int> CDensityFarestPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int first, int dataSize, int numPivots, int firstPivot)
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
	//int newFirstPivot = firstPivot;
	for(int i=first; i<first+dataSize; i++)
	{
		index[i-first].dataID = i;
		index[i-first].dis = metric->getDistance(data[newFirstPivot].get(),data[i].get());
		disComputeTimes++;
	}
	sort(index, index+dataSize);
	//cout<<index[0].dis<<"\t"<<index[dataSize-1].dis<<endl;
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
	segIndex[segNum-1].dis = (index[dataSize-1].dis - index[segIndex[segNum-1].dataID].dis)/(dataSize-(segNum-1)*perSeg)*perSeg;
	sort(segIndex, segIndex+segNum);
	/*for(int i=0; i<segNum; i++)
	{
		cout<<i<<"  "<<segIndex[i].dataID<<"  "<<segIndex[i].dis<<endl;
	}*/
	vector<int> pivotCandidateSet;
	for(int i=0; i<segNum; i++)
	{
		pivotCandidateSet.push_back(index[segIndex[i].dataID+perSeg/2].dataID);
		cout<<"pivotCandidateSet:"<<pivotCandidateSet[i]<<endl;
	}
	//cout<<"disComputeTimes: "<<disComputeTimes<<endl;

	bool* isCenter = new bool[segNum];
	double* minDist = new double[segNum];
	for (int i = 0; i < segNum; i++)
	{
		isCenter[i] = false;
		minDist[i] = DBL_MAX;
	}
	 //isCenter[pivotCandidateSet[0]] = true;
	isCenter[0] = true;
	 int* indices = new int[numPivots]; // indices is used to record the offsets of the pivots in the original data list
	 indices[0] = 0;  //pivotCandidateSet[0];
	 for (int i = 1; i < numPivots; i++)
		 indices[i] = -1;
	 // transparently firstPivot is found already 
	 for (int centerSize = 1; centerSize < numPivots; centerSize++)
	 {
		 double currMaxDist = 0;
		 shared_ptr<CMetricData> const lastCenter = data[indices[centerSize - 1]];
		 for (int i = 0; i < segNum; i++)
		 {
			 if (isCenter[i] == false) // if the point is not a center, we should calculate the distance
									   // between this point and the set of Centers, for each centerSize we
									   // grasp one Center form the set of Centers.
			 {
				 double tempDist = metric->getDistance(data[pivotCandidateSet[i]].get(), lastCenter.get());
				 minDist[i] = (tempDist < minDist[i]) ? tempDist : minDist[i];
				 if (minDist[i] > currMaxDist)
				 {
					 indices[centerSize] = i;  //pivotCandidateSet[i]; // save the index the current farthest point
					 currMaxDist = minDist[i];
				 }
			 }
		 }
		 if (indices[centerSize] == -1)
			 break;
		 else
			 isCenter[indices[centerSize]] = true;
	 }
	 int returnSize = 0;
	 while ((returnSize < numPivots) && (indices[returnSize] >= 0))
		 returnSize++;
	 // to decide the size of the result vector.
	 if (returnSize > numPivots)
		 returnSize = numPivots;
	 vector<int> result;
	 for(int i=0; i<returnSize; i++)
		 result.push_back(pivotCandidateSet[indices[i]]);
	 delete [] isCenter;
	 delete [] minDist;
	 delete [] indices;
	 return result;
}

