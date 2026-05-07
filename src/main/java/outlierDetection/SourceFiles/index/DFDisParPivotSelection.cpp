/**@file DensityFarestPivotSelection.cpp
 * @brief  select pivots with density first, then FFT algorithm
 * @author Honglong Xu
 * @date 2015-10-22
*/

#include "../../HeaderFiles/index/DFDisParPivotSelection.h"
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

CDFDisParPivotSelection::CDFDisParPivotSelection()
{
}

CDFDisParPivotSelection::~CDFDisParPivotSelection()
{
}

vector<int> CDFDisParPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int numPivots)
{
	 return selectPivots(metric, data,0,data.size(),numPivots);
}

vector<int> CDFDisParPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> >& data, int first, int dataSize, int numPivots)
{
	        int firstPivot = first; // Math.floor(first + Math.random() *
                                    // dataSize);
            return selectPivots(metric,data,first, dataSize, numPivots,firstPivot);
}

vector<int> CDFDisParPivotSelection::selectPivots(CMetricDistance *metric, vector<shared_ptr<CMetricData> > &data,int first, int dataSize, int numPivots, int firstPivot)
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
	cout<<"segDis:"<<segDis<<endl;
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
		cout<<"pivotCandidateSet:"<<pivotCandidateSet[i]<<"\t"<<segIndex[i].dis<<"\t"<<neighborNum[i].dataID<<endl;
	}
	//cout<<"disComputeTimes: "<<disComputeTimes<<endl;
	bool* isCenter = new bool[segNum];
	double* minDist = new double[segNum];
	for (int i = 0; i < segNum; i++)
	{
		isCenter[i] = false;
		minDist[i] = DBL_MAX;
	}
	 isCenter[pivotCandidateSet[0]] = true;
	 int* indices = new int[numPivots]; // indices is used to record the offsets of the pivots in the original data list
	 indices[0] = pivotCandidateSet[0];
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
					 indices[centerSize] = pivotCandidateSet[i]; // save the index the current farthest point
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
		 result.push_back(indices[i]);
	 delete [] isCenter;
	 delete [] minDist;
	 delete [] indices;
	 return result;
	 return result;
}

