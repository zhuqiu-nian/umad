#include "../../HeaderFiles/outlierdetection/outlierdefinition/KNN.h"
#include <vector>
#include <fstream>
#include <boost/dynamic_bitset.hpp>
#include <bitset>
#include <iostream>
#include <stdlib.h>
#include <boost\random.hpp>
#include <memory>
#include "../../HeaderFiles/metricdistance/MetricDistance.h"
#include "../../HeaderFiles/metricdistance/EuclideanDistance.h"
#include "../../HeaderFiles/metricdistance/DNAClassMetric.h"
#include "../../HeaderFiles/metricdistance/EuclideanHamming.h"
#include "../../HeaderFiles/metricdistance/PearsonDistance.h"
#include "../../HeaderFiles/metricdata/DNAClass.h"
#include "../../HeaderFiles/metricdata/KddCup99.h"
#include "../../HeaderFiles/metricdata/DoubleVectorClass.h"
#include "../../HeaderFiles/metricdata/Stock.h"
#include "../../HeaderFiles/index/PivotSelectionMethod.h"
#include "../../HeaderFiles/index/FFTPivotSelectionMethod.h"

using namespace std;
using namespace boost;

#ifdef linux
#include <sys/timeb.h>
#include "/usr/include/sys/times.h"
#include <tr1/memory>
using std::tr1::shared_ptr;
using std::tr1::dynamic_pointer_cast;
#else
#ifdef _WIN32
//#include <Windows.h>
//#include <psapi.h>
#include <memory>
//#pragma comment(lib,"psapi.lib")
#endif
#endif

template<typename T>
bool insertQueue(T &data, T *dataQueue, int k, bool insertPosition)
{
	//insert from first.  dataQueue is descending.
	if((insertPosition)&&(data < dataQueue[0]))
	{
		dataQueue[0] = data;
		for(int i=1; i<k; i++)
		{
			if(dataQueue[i-1] < dataQueue[i])
			{
				data = dataQueue[i-1];
				dataQueue[i-1] = dataQueue[i];
				dataQueue[i] = data;
			}
		}
		return true;
	}
	//insert from last.  dataQueue is descending.
	else if((!insertPosition)&&(data > dataQueue[k-1]))
	{
		dataQueue[k-1] = data;
		for(int i=k-2; i>=0; i--)
		{
			if(dataQueue[i+1] > dataQueue[i])
			{
				data = dataQueue[i+1];
				dataQueue[i+1] = dataQueue[i];
				dataQueue[i] = data;
			}
		}
		return true;
	}
	return false;
}

template bool insertQueue<CKNN>(CKNN &data, CKNN *dataQueue, int k, bool insertPosition);
template bool insertQueue<double>(double &data, double *dataQueue, int k, bool insertPosition);

double getAccuracy(bool *trueState, int n)
{
	ofstream accuracyResult("D:\\Data\\result\\accuracyResult.txt",ios::app);
	double realOutlierNum = 0;
	double sumAccuracy = 0;
	double Accuracy = 0;
	for(int i=0; i<n; i++)
	{
		if(!trueState[i])
		{
			realOutlierNum++;
		}
		Accuracy= realOutlierNum/(i+1);
		sumAccuracy += Accuracy;
		//cout<<"realOutlierNum/(i+1):"<<realOutlierNum/(i+1)<<"  sumAccuracy:"<<sumAccuracy<<endl;
		if(i%10==9)
		{
			cout<<"TOP "<<i+1<<" average Accuracy: "<<sumAccuracy/(i+1)<<endl;
		}
		accuracyResult<<Accuracy<<"\t"<<sumAccuracy/(i+1)<<endl;
	}
	return sumAccuracy/n;
}

ROCPoint* getROC(bool *trueState, int n, double totalOutlierNum, int size)
{
	double totalNormalNum = size - totalOutlierNum;
	int currentOutlierNum = 0;
	int currentNormalNum = 0;
	ROCPoint* ROC = new ROCPoint[n];
	//ofstream file1("D:\\Data\\result\\num.txt",ios::app);
	for(int i=0; i<n; i++)
	{
		if(!trueState[i])
		{
			currentOutlierNum++;
			ROC[i].TPR = currentOutlierNum/totalOutlierNum;
			ROC[i].FPR = currentNormalNum/totalNormalNum;
		}
		else
		{
			currentNormalNum++;
			ROC[i].TPR = currentOutlierNum/totalOutlierNum;
			ROC[i].FPR = currentNormalNum/totalNormalNum;
		}
	}
	if(currentOutlierNum == totalOutlierNum)
	{
		printf("ROC is complete!\n");
	}
	else
	{
		printf("ROC is not complete!\n");
	}
	return ROC;
}

double getAUC(ROCPoint* ROC, int n)
{
	double area = 0;
	area += 0.5 * ROC[0].TPR * ROC[0].FPR;
	for(int i=1; i<n; i++)
	{
		area += 0.5 * (ROC[i].TPR+ROC[i-1].TPR)*(ROC[i].FPR-ROC[i-1].FPR);
	}
	area += 0.5 * (1.0+ROC[n-1].TPR)*(1.0-ROC[n-1].FPR);
	return area;
}

void outputROC(ROCPoint* ROC, int n)
{
	ofstream file1("D:\\Data\\result\\roc.txt",ios::app);
	file1<<endl;
	for(int i=1; i<n; i++)
	{
		file1<<ROC[i].FPR<<"\t"<<ROC[i].TPR<<endl;
	}
	file1.close();
}

double getLRDen(CKNN *knn, bool *neighborFlag, int n, int k)
{
	int m=n+k-2;
	int p = 0;
	double LRDen = 0;
	while(p<k && m>=0)
	{
		if(neighborFlag[m])
		{
			LRDen = knn[m].dis;
			p++;
		}
		m--;
	}
	return LRDen;
}

/*
void showMemoryInfo(char * resultsFileName)
{
	char resultFile[100] = "./";
	strcat(resultFile, resultsFileName);
	ofstream output(resultFile, ofstream::app);
	HANDLE handle = GetCurrentProcess();
	PROCESS_MEMORY_COUNTERS pmc;
	GetProcessMemoryInfo(handle, &pmc, sizeof(pmc));
	output << "内存" << pmc.WorkingSetSize / 1048576.0 << " MB/峰值" << pmc.PeakWorkingSetSize / 1048576.0 << "MB" << endl;
	output << "当前虚拟内存使用：" << pmc.PagefileUsage / 1048576.0 << " MB/ 峰值虚拟内存使用：" << pmc.PeakPagefileUsage / 1048576.0 << " MB " << endl;
}
*/

double findAvg(CKNN *Block, int blockSize)
{
	double sum = 0;
	for(int i=0; i<blockSize; i++)
	{
		sum += Block[i].dis;
	}
	return sum/blockSize;
}

int getStartID(CKNN *Block, int blockSize, double avg)
{
	 int low=0, high=blockSize-1, mid;
	 while(low <= high)
	 {
		 mid = (low+high)/2;
		 if(Block[mid].dis == avg)
		 {
			 return mid;
		 }
		 else if(Block[mid].dis > avg)
		 {
		   low=mid+1;
		 }
		 else
			 high=mid-1;
	 }
	 return low>high?low:high;
}

int getIndexID(CKNN *Block, int blockSize, double avg)
{
	 int low=0, high=blockSize-1, mid;
	 while(low <= high)
	 {
		 //cout<<"low="<<low<<"\thigh="<<high<<endl;
		 //cout<<"lowdis="<<Block[low].dis<<"\thighdis="<<Block[high].dis<<endl;
		 mid = (low+high)/2;
		 if(Block[mid].dis == avg)
		 {
			 return mid;
		 }
		 else if(Block[mid].dis < avg)
		 {
		   low=mid+1;
		 }
		 else
			 high=mid-1;
	 }
	 return high;
}

int getSpiralOrder(int size, int startID, int i)
{
	if(startID<size/2 && i>2*startID)
	{
		return i;
	}
	else if(startID>size/2 && i>=2*(size-startID))
	{
		return size-i-1;
	}
	else
	{
		if(i%2)
		{
			return startID-(i+1)/2;
		}
		else
		{
			return startID+i/2;
		}
	}
}

bool greaterCKNN(const CKNN &a, const CKNN &b)
{
	return a.dis > b.dis;
}

int getBitNum(int n)
{
	int bits = 6;
	int dimBits = 64/n;
	{
		for(int i=5; i>=1; i--)
		{
			if((2<<i)>dimBits && (2<<(i-1))<=dimBits)
			{
				bits = i;
				break;
			}
		}
	}
	return bits;
}

unsigned long long int deTransposed(unsigned long long int  *X, const int n, const int bits)
{
	const int bitNum = 2<<(bits-1);  //pow(2,bits);
	unsigned long long int sum = 0;
	for(int i=0; i<bitNum; i++)
	{
		for(int j=0; j<n; j++)
		{
			sum += ((X[j]>>i)&1)<<(n*i+n-j-1);
		}
	}
	return sum;
}

/// <summary>
/// Convert between Hilbert index and N-dimensional points.
/// 
/// The Hilbert index is expressed as an array of transposed bits. 
/// 
/// Example: 5 bits for each of n=3 coordinates.
/// 15-bit Hilbert integer = A B C D E F G H I J K L M N O is stored
/// as its Transpose                        ^
/// X[0] = A D G J M                    X[2]|  7
/// X[1] = B E H K N        <------->       | /X[1]
/// X[2] = C F I L O                   axes |/
///        high low                         0------> X[0]
///        
/// NOTE: This algorithm is derived from work done by John Skilling and published in "Programming the Hilbert curve".
/// (c) 2004 American Institute of Physics.
/// 
/// </summary>

/// <summary>
/// Convert the Hilbert index into an N-dimensional point expressed as a vector of uints.
///
/// Note: In Skilling's paper, this function is named TransposetoAxes.
/// </summary>
/// <param name="transposedIndex">The Hilbert index stored in transposed form.</param>
/// <param name="bits">Number of bits per coordinate.</param>
/// <returns>Coordinate vector.</returns>
int* HilbertAxes(int* X, const int n, int bits)
{
	//var X = (int[])transposedIndex.Clone();
	//int n = X.Length; // n: Number of dimensions
	int N = 2U << (bits - 1), P, Q, t;
	int i;
	// Gray decode by H ^ (H/2)
	t = X[n - 1] >> 1;
	// Corrected error in Skilling's paper on the following line. The appendix had i >= 0 leading to negative array index.
	for (i = n - 1; i > 0; i--)
		X[i] ^= X[i - 1];
	X[0] ^= t;
	// Undo excess work
	for (Q = 2; Q != N; Q <<= 1)
	{
		P = Q - 1;
		for (i = n - 1; i >= 0; i--)
			if ((X[i] & Q) != 0U)
				X[0] ^= P; // invert
			else
			{
				t = (X[0] ^ X[i]) & P;
				X[0] ^= t;
				X[i] ^= t;
			}
	} // exchange
	return X;
}

/// <summary>
/// Given the axes (coordinates) of a point in N-Dimensional space, find the distance to that point along the Hilbert curve.
/// That distance will be transposed; broken into pieces and distributed into an array.
/// 
/// The number of dimensions is the length of the hilbertAxes array.
///
/// Note: In Skilling's paper, this function is called AxestoTranspose.
/// </summary>
/// <param name="hilbertAxes">Point in N-space.</param>
/// <param name="bits">Depth of the Hilbert curve. If bits is one, this is the top-level Hilbert curve.</param>
/// <returns>The Hilbert distance (or index) as a transposed Hilbert index.</returns>
unsigned long long int HilbertIndexTransposed(unsigned long long int* X, const int n, int bits)
{
	//var X = (uint[])hilbertAxes.Clone();
	//var n = hilbertAxes.Length; // n: Number of dimensions
	int M = 1U << (bits - 1), P, Q, t;
	int i;
	// Inverse undo
	for (Q = M; Q > 1; Q >>= 1)
	{
		P = Q - 1;
		for (i = 0; i < n; i++)
			if ((X[i] & Q) != 0)
				X[0] ^= P; // invert
			else
			{
				t = (X[0] ^ X[i]) & P;
				X[0] ^= t;
				X[i] ^= t;
			}
	} // exchange
	// Gray encode
	for (i = 1; i < n; i++)
		X[i] ^= X[i - 1];
	t = 0;
	for (Q = M; Q > 1; Q >>= 1)
		if ((X[n - 1] & Q)!=0)
			t ^= Q - 1;
	for (i = 0; i < n; i++)
		X[i] ^= t;
	return deTransposed(X, n, bits);
}

int getDoubleBitNum(double maxValue)
{
	int intValue = ceil(maxValue);
	int bitNum = 0;
	while(intValue)
	{
		intValue>>=1;
		bitNum++;
	}
	return bitNum;
}

unsigned long long int doubleShiftToInt(double d, int shiftNum)
{
	long long llx = *(long long*)&d; 
	llx += shiftNum * 0x0010000000000000LL; 
	return (unsigned long long int )*(double*)&llx; 
} 

//独立不重复抽取
int *randSelect(int totalSize, int randSize)
{
	boost::mt19937 rng(time(0));
	//boost::uniform_int<> ui(0, totalSize-1);
	int *startArray = new int[totalSize];
	int seed = 0;
	for(int i=0; i<totalSize; i++)
	{
		startArray[i] = i;
	}
	int *randArray = new int[randSize];
	for(int i=0; i<randSize; i++)
	{
		boost::uniform_int<> ui(0, totalSize-i-1);
		seed = ui(rng);
		randArray[i] = startArray[seed];
		startArray[seed] = startArray[totalSize-i-1];
	}
	delete[] startArray;
	return randArray;
}

template<class Type>
int BinarySearch(Type a[],const Type& x,int n)
{
    int left=0;
    int right=n-1;
    while(left<=right)
    {
        int middle=(left+right)/2;
        if(a[middle]==x)
            return middle;
        if(x>=a[middle])
            left=middle+1;
        else
            right=middle-1;
    }
    //return -1;
	return right;
}

//把正常点集中到数组前面，异常点集中到后面
void rejectOutlier(int isOutlier[], int n)
{
	int j = n-1;
	for(int i=0; i<j; i++)
	{
		if(isOutlier[i]==-1)
		{
			while(isOutlier[j])
			{
				j--;
			}
			for(; j>i; j--)
			{
				if(isOutlier[i]>=0)
				{
					isOutlier[i] = j;
					isOutlier[j] = i;
				}
			}
		}
	}
}

void forwardOutlier(int *Outlier, int outlierNum, CKNN *index, int indexSize)
{
	int k=0;  //对调至前面的离群点下标
	CKNN temp;
	for(int i=0; i<indexSize; i++)
	{
		for(int j=0; j<outlierNum-k; j++)
		{
			if(Outlier[j] == index[i].dataID)
			{
				//index[k].dataID = index[i].dataID;
				//index[k].dis = index[i].dis;
				//temp = index[k];
				//index[k] = index[i];
				//index[i] = temp;
				swap(index[k], index[i]);  //把索引序列中的离群点对调至前面
				swap(Outlier[j], Outlier[outlierNum-k]);  //把已在索引队列中搜索到的离群点移至后面，不再搜索
				k++;
				break;
			}
		}
	}
}

vector<int> selectInliers(CMetricDistance *metric, vector<std::shared_ptr<CMetricData> > &data, int *inlier, int inlierNum, int selectInlierNum)
{
	bool* isCenter = new bool[inlierNum];
	double* minDist = new double[inlierNum];
	for (int i = 0; i < inlierNum; i++)
	{
		isCenter[i] = false;
		minDist[i] = DBL_MAX;
	}
	 isCenter[0] = true;
	 int* indices = new int[selectInlierNum]; // indices is used to record the offsets of the pivots in the original data list
	 indices[0] = inlier[0];
	 for (int i = 1; i < selectInlierNum; i++)
		 indices[i] = -1;

	 // transparently firstPivot is found already 
	 for (int centerSize = 1; centerSize < selectInlierNum; centerSize++)
	 {
		 double currMaxDist = 0;
		 std::shared_ptr<CMetricData> const lastCenter = data[indices[centerSize - 1]];
		 for (int i = 0; i < inlierNum; i++)
		 {
			 if (isCenter[i] == false) // if the point is not a center, we should calculate the distance
									   // between this point and the set of Centers, for each centerSize we
									   // grasp one Center form the set of Centers.
			 {
				 double tempDist = metric->getDistance(data[inlier[i]].get(), lastCenter.get());
				 minDist[i] = (tempDist < minDist[i]) ? tempDist : minDist[i];
				 if (minDist[i] > currMaxDist)
				 {
					 indices[centerSize] = inlier[i]; // save the index the current farthest point
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
	 while ((returnSize < selectInlierNum) && (indices[returnSize] >= 0))
		 returnSize++;
	 // to decide the size of the result vector.
	 if (returnSize > selectInlierNum)
		 returnSize = selectInlierNum;
	 vector<int> result;
	 for(int i=0; i<returnSize; i++)
		 result.push_back(indices[i]);
	 delete [] isCenter;
	 delete [] minDist;
	 delete [] indices;
	 return result;
}

//查找2个有序数据的第K小的数
int getKth(int *nums1, int start1, int end1, int *nums2, int start2, int end2, int k)
{
	int len1 = end1 - start1 + 1;//num1 的长度
	int len2 = end2 - start2 + 1;//num2 的长度
	//让 len1 的长度小于 len2，这样就能保证如果有数组空了，一定是 len1
	if(len1 > len2)
		return getKth(nums2, start2, end2, nums1, start1, end1, k);
	if(len1 == 0)
		return nums2[start2 + k - 1];
	if(k == 1)
		return min(nums1[start1], nums2[start2]);
	int i = start1 + min(len1, k / 2) - 1;
	int j = start2 + min(len2, k / 2) - 1;
	if (nums1[i] > nums2[j])
	{
		return getKth(nums1, start1, end1, nums2, j + 1, end2, k - (j - start2 + 1));//最后一个参数表示的是 K 减去已经比较过的数了。
	}
	else
	{
		return getKth(nums1, i + 1, end1, nums2, start2, end2, k - (i - start1 + 1));
    }
}

//在一维有序数组中搜索k最近邻
int getKth(int *nums, int len, int id, int k)
{
	int start1 = 0, end1 = 0, start2 = 0, end2 = 0;
	if(id == 0 || id == len-1)
	{
		return nums[abs(id-k)];
	}
	start1 = id - 1;
	end1 = id - k;
	start2 = id + 1;
	end2 = id + k;
	if(id < k)
	{
		start1 = id - 1;
		end1 = 0;
		//start2 = id + 1;
		//end2 = id + k;
	}
	if(id > len - 1 - k)
	{
		//start1 = id - 1;
		//end1 = id - k;
		start2 = id + 1;
		end2 = len - 1;  
	}

	int len1 = start1 - end1 + 1;//num1 的长度
	int len2 = end2 - start2 + 1;//num2 的长度
	
	if(len1 == 0)
		return nums[start2 + k - 1];
	if(len2 == 0)
		return nums[start1 - k + 1];
	int klen = k;
	int i = start1 - min(len1, k / 2) + 1;
	int j = start2 + min(len2, k / 2) - 1;
	while(klen>1)
	{
		if(nums[i] > nums[j])
		{
			start2 = j+1;
			len2 = end2 - start2 + 1;
			klen = klen - (j - start2 + 1);
		}
		else
		{
			start1 = i-1;
			len1 = start1 - end1 + 1;
			klen = klen - (i - start1 + 1);
		}
	}
	if(klen == 1)
		return min(nums[start1], nums[start2]);
}

double dis(CKNN a, CKNN b)
{
	return abs(a.dis-b.dis);
}

double dis(double a, double b)
{
	return abs(a-b);
}

//在一维有序数组中搜索k最近邻
CKNN getKth(CKNN nums[], int len, int id, int k)
{
	int start1 = 0, end1 = 0, start2 = 0, end2 = 0;
	if(id == 0 || id == len-1)
	{
		//return nums[abs(id-k)];
		return CKNN(abs(id-k), dis(nums[id], nums[abs(id-k)]));  //返回第k近邻的数组下标（而不是dataID）及其与下标为id的元素之距离
	}
	start1 = id - 1;
	end1 = id - k;
	start2 = id + 1;
	end2 = id + k;
	if(id < k)
	{
		start1 = id - 1;
		end1 = 0;
	}
	if(id > len - 1 - k)
	{
		start2 = id + 1;
		end2 = len - 1;  
	}

	int len1 = start1 - end1 + 1;//num1 的长度
	int len2 = end2 - start2 + 1;//num2 的长度
	
	if(len1 == 0)
		//return nums[start2 + k - 1];
		return CKNN(start2 + k - 1, dis(nums[id], nums[start2 + k - 1]));
	if(len2 == 0)
		//return nums[start1 - k + 1];
		return CKNN(start1 - k + 1, dis(nums[id], nums[start1 - k + 1]));
	int klen = k;
	int i = start1 - min(len1, k / 2) + 1;
	int j = start2 + min(len2, k / 2) - 1;
	while(klen>1)
	{
		if(dis(nums[id],nums[i]) > dis(nums[id],nums[j]))
		{
			klen = klen - (j - start2 + 1);
			
			start2 = j+1;
			len2 = end2 - start2 + 1;
			if(len2 <= 0)
				//return nums[start1 - klen + 1];
				return CKNN(start1 - klen + 1, dis(nums[id], nums[start1 - klen + 1]));
			i = start1 - min(len1, klen / 2) + 1;
			j = start2 + min(len2, klen / 2) - 1;
		}
		else
		{
			klen = klen - (start1 - i + 1);
			start1 = i-1;//0
			len1 = start1 - end1 + 1;
			if(len1 <= 0)
				//return nums[start2 + klen - 1];
				return CKNN(start2 + klen - 1, dis(nums[id], nums[start2 + klen - 1]));
			i = start1 - min(len1, klen / 2) + 1;
			j = start2 + min(len2, klen / 2) - 1;
		}
		if(klen == 1)
			//return min(nums[start1], nums[start2]);
		{
			double disOfStart1 = dis(nums[id], nums[start1]);
			double disOfStart2 = dis(nums[id], nums[start2]);
			return disOfStart1 < disOfStart2 ? CKNN(start1, disOfStart1) : CKNN(start2, disOfStart2);
		}
	}
	if(klen == 1)
		//return min(nums[start1], nums[start2]);
	{
		double disOfStart1 = dis(nums[id], nums[start1]);
		double disOfStart2 = dis(nums[id], nums[start2]);
		return disOfStart1 < disOfStart2 ? CKNN(start1, disOfStart1) : CKNN(start2, disOfStart2);
	}
}

void loadDataByType(char * dataType, vector<std::shared_ptr<CMetricData> > *&dataList, char *dataFileName,int size,int dim,int &fragmentLength)
{
	if(!strcmp(dataType,"dnaclass"))
	{
		CObjectFactory factory();
		CObjectFactory::objectRegister("dnaclass",CDNAClass::getConstructor());
		dataList = CDNAClass::loadData(dataFileName,size,fragmentLength);
	}
	else if(!strcmp(dataType,"kddcup99"))
	{
		CObjectFactory factory();
		CObjectFactory::objectRegister("vector",CKddCup99::getConstructor());
		dataList=CKddCup99::loadData(dataFileName,size,dim,fragmentLength);
	}
	else if(!strcmp(dataType,"vectorclass"))
	{
		CObjectFactory factory();
		CObjectFactory::objectRegister("vectorclass",CDoubleVectorClass::getConstructor());
		dataList=CDoubleVectorClass::loadData(dataFileName,size,dim,fragmentLength);
	}
	else if (!strcmp(dataType, "stock"))
	{
		CObjectFactory factory();
		CObjectFactory::objectRegister("stock", CStock::getConstructor());
		dataList = CStock::loadData(dataFileName, size, dim, fragmentLength);
	}
}

void getMetricByType(CMetricDistance *&metric,char *dataType)
{
	if(!strcmp(dataType,"dnaclass")) 
	{
		metric = new CDNAClassMetric ;
	}
	else if(!strcmp(dataType,"kddcup99"))
	{
		metric = new CEuclideanHamming;
	}
	else if(!strcmp(dataType,"vectorclass"))
	{
		metric = new CEuclideanDistance;
	}
	else if (!strcmp(dataType, "stock"))
	{
		metric = new CPearsonDistance;
	}
}

void getPivotSelectionMethod(char *pivotSelectionMethod,CPivotSelectionMethod *&psm,int fftscale,int setA,int setN)
{
	if(!strcmp(pivotSelectionMethod,"fft"))
	{
		psm=new CFFTPivotSelectionMethod;
	}
	else
	{
		cout<<"Random pivot selection method"<<endl;
	}
}
//在升序数组arr中搜索，如果搜索到了，返回数组下标，如果搜索不到，返回比它大的最接近的下标或最大者下标right。
int Binarysearch(int arr[], int key, int left, int right)    //升序数组，关键字，左值（开始），右值（结束）。
{
	//int left = 0;                      
	//int right = n-1;
	int mid = 0;
	if (key > arr[right])  //如果拟搜索的值大于全部值，则返回最大者即arr[right]
		return right;
	while (left <= right)
	{
		int mid = (left + right) / 2;
		if (key < arr[mid])        // 在中间值左边
		{
			right = mid - 1;
		}
		else if (key > arr[mid])    //在中间值右边
		{
			left = mid + 1;
		}
		else                    //中间值等于关键字
		{
			return mid;    //找到了返回下标
		}

	}
	return left;     //找不到的情况下，返回比它大的最接近的下标
}