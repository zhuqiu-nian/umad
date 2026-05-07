/** @file OutlierDetection
* @Entry of using this database.
* @author Honglong Xu
* @date 2014 9 15
* @version 0.x
*
*/

#define _CRT_SECURE_NO_WARNINGS
#include "../../HeaderFiles/metricdata/KddCup99.h"
#include "../../HeaderFiles/metricdata/DoubleVectorClass.h"
#include "../../HeaderFiles/metricdata/DNAClass.h"
#include "../../HeaderFiles/metricdata/Stock.h"
#include "../../HeaderFiles/metricdistance/MetricDistance.h"
#include "../../HeaderFiles/metricdistance/EuclideanDistance.h"
#include "../../HeaderFiles/outlierdetection/detectionalgorithm/OutlierDetector.h"
#include "../../HeaderFiles/outlierdetection/detectionalgorithm/ORCA.h"
#include "../../HeaderFiles/outlierdetection/detectionalgorithm/iORCA.h"
#include "../../HeaderFiles/outlierdetection/outlierdefinition/OutlierDefinition.h"
#include "../../HeaderFiles/outlierdetection/outlierdefinition/KnnOutlier.h"
#include "../../HeaderFiles/outlierdetection/outlierdefinition/KthOutlier.h"
#include "../../HeaderFiles/index/PivotSelectionMethod.h"
#include <ctime>

#if defined(_MSC_VER)
//#include "../../HeaderFiles/util/getopt_win.h"
#include <tchar.h>
#else _MSC_VER
#include "../../HeaderFiles/util/getopt.h"
#define ARG_NONE no_argument
#define ARG_NULL no_argument
#define ARG_REQ required_argument
#define getopt_long_a getopt_long
#define optarg_a optarg
#define option_a option
#endif
#include <iostream>
#include <string>
#include <fstream>
#include <sstream>
#include <cstring>
#include <cstdlib>
//#include "windows.h"

using namespace std;


template<typename type>
type stringToNumber(const char str[])
{

	istringstream iss(str);
	type data;
	iss>>data;
	return data;
}

extern void getMetricByType(CMetricDistance *&metric,char *dataType);
void getPivotSelectionMethod(char *pivotSelectionMethod,CPivotSelectionMethod *&psm,int fftscale,int setA,int setN);
void loadDataByType(char * dataType, vector<shared_ptr<CMetricData> > *&dataList, char *dataFileName,int size,int dim,int &fragmentLength);
int Binarysearch(int arr[], int key, int left, int right);
/**
* @This is the main entry point for 
* 
* The basic commandline options are:
* @param 
*/


int main(int argc, char** argv)
{
	ofstream file1("D:\\Data\\result\\detectionResult.txt",ios::app);
	ofstream file2("D:\\Data\\result\\parameter.txt",ios::app);
	int c;
	/**dataset size.*/
	int size = 1588;
	int k = 5;//0.005 * size;
	int n = 10;//0.01 * size;
	int startDate = 20200401; //日K线开始时间
	int endDate = 20210331; //日K线结束时间
	if (argc >= 6)  //如果后面带5个以上参数，则5个参数分别为p1、p2、k、n、size的值。
	{
		startDate = strtol(argv[1], NULL, 10);
		endDate = strtol(argv[2], NULL, 10);
		k = strtol(argv[3], NULL, 10);
		n = strtol(argv[4], NULL, 10);
		size = strtol(argv[5], NULL, 10);
	}
	else if (argc == 5)  //如果后面带4个参数，则4个参数分别为p1、p2、k、n的值。
	{
		startDate = strtol(argv[1], NULL, 10);
		endDate = strtol(argv[2], NULL, 10);
		k = strtol(argv[3], NULL, 10);
		n = strtol(argv[4], NULL, 10);
	}
	else if (argc == 4)  //如果后面带3个参数，则3个参数分别为p1、p2、k的值。
	{
		startDate = strtol(argv[1], NULL, 10);
		endDate = strtol(argv[2], NULL, 10);
		k = strtol(argv[1], NULL, 10);
	}
	else if (argc == 3)  //如果后面带2个参数，则3个参数分别为p1、p2的值。
	{
		startDate = strtol(argv[1], NULL, 10);
		endDate = strtol(argv[2], NULL, 10);
	}
	else if (argc == 2)  //如果后面带1个参数，则该参数分别为p1的值。
	{
		startDate = strtol(argv[1], NULL, 10);
	}

	double D = 0.1;
	int e = k;
	int m = 100;
	int repeatTimes = 10;
	
	/**The number of dataset's numberical attributes, not include the categorical attributes.*/
	int dim = 972;  //价格维度
	/**If outlierNum>0, then the program will use it, else use the default outlier number, which is decided by fact of dataset.*/
	int outlierNum = 0;
	char *outlierDefinition = "KthOutlier";
	//char *dataType = "kddcup99";
	//char *dataType = "vectorclass";
	//char *dataType = "dnaclass";
	//char *dataType = "peptide";
	char* dataType = "stock";
	ofstream result("D:\\Data\\result\\result.txt",ios::app);
	char *detectionAlgorithm = "iORCA";
	result<<detectionAlgorithm<<endl;
	char *pivotSelectionMethod = "DensityPeakFarestPivotSelection";
	int v = 1;
	int fftscale = 0;
	int setA = 0;
	int setN = 0;
	vector<shared_ptr<CMetricData> > *rawData=0;
	CMetricDistance *metric=0;
	//char *dataFileName="kddcup.data.corrected";
	//char *dataFileName="kddcup.data_10_percent_corrected";
	//char *dataFileName="hawii.txt";
	//char *dataFileName="GaussianDistribution.txt";
	//char *dataFileName="thyroid-ann.txt";
	//char *dataFileName="ann-test.txt";
	//char *dataFileName="shuttletest.txt";
	//char *dataFileName="Synthetic Dataset.txt";
	//char *dataFileName="letter-recognition-Z.txt";
	//char *dataFileName="ionosphere.txt";
	//char *dataFileName="optdigits.txt";
	//char *dataFileName="3.txt";
	//char *dataFileName="ICMP.txt";
	//char *dataFileName="TCP.txt";
	//char *dataFileName="arab1.con";
	//char *dataFileName="splice23.txt";
	//char *dataFileName="shuttle.trn";  //58000,9,vectorclass
	char* dataFileName = "stock.txt";
	//char *dataFileName="pendigits.txt";
	//char *dataFileName="splice.txt";  //Molecular Biology数据集
	//char *dataFileName="sat.trn";  //Landsat Satellite数据集
	//char *dataFileName="reprocessed.hungarian.txt";
	//char *dataFileName="breast-cancer-wisconsin(Diagnostic).txt";
	//char *dataFileName="pendigits_tra10percent.txt";
	//char *dataFileName="covertype24(order).txt";
	//char *dataFileName="spambase.txt";

	COutlierDetector *OutlierDetector = 0;
	COutlierDefinition *OutlierDefinition = 0;
	CPivotSelectionMethod *psm = 0;
	CKNN *TOP_N_Outlier = 0; 

	getMetricByType(metric, dataType);
	getPivotSelectionMethod(pivotSelectionMethod, psm, fftscale, setA, setN);

	char newDataFileName[100]="D:\\Data\\VS2019\\MSOD\\data\\";
	strcat(newDataFileName,dataFileName);
	loadDataByType(dataType,rawData,newDataFileName,size,dim,outlierNum);
	int realSize = rawData->size();  //实际读取的数据量
	int dateNum = ((CStock*)((*rawData)[0].get()))->getLen()/4;  //获取日期数量，即价格数量的四分之一（每天有4个价格）
	int* stockDateList = ((CStock*)((*rawData)[0].get()))->getStockDate();  //获取日期数组（指针），因为所有股票的K线日期都一样，所以获取第0只股票就行了。
	int p1 = 0;  //开始日期的数组下标
	int p2 = 0;  //结束日期的数组下标
	if (stockDateList[0] > startDate)  //stockDateList[0]为第1个日期
	{
		cout << "开始日期不能早于" << stockDateList[0] << endl;
		exit(0);
	}
	cout << "dateNum=" << dateNum << endl;
	if (stockDateList[dateNum - 1] < endDate) //stockDateList[realSize - 1]为最后一个日期
	{
		cout << "结束日期不能晚于" << stockDateList[dateNum - 1] << endl;
		exit(0);
	}
	//按照开始日期、结束日期，确定数组下标
	for (int i = 0; i < dateNum; i++)
	{
		p1 = Binarysearch(stockDateList, startDate, 0, dateNum - 1);
		p2 = Binarysearch(stockDateList, endDate, 0, dateNum - 1);
	}
	cout << "开始日期：" << startDate << "\t结束日期：" << endDate << endl;
	cout << "数组下标p1=" << p1 << "\tp2=" << p2 << endl;
	cout<<"outlierDefinition: "<<outlierDefinition<<"  detectionAlgorithm: "<<detectionAlgorithm<<endl;
	cout<<"size:"<< realSize <<"  k: "<<k<<"  n: "<<n<<endl;
	file2<<endl<<"outlierDefinition: "<<outlierDefinition<<"  detectionAlgorithm: "<<detectionAlgorithm<<endl;
	file2<<"size:"<< realSize <<"  k: "<<k<<endl;
	//int maxValue = 0;
	//int minValue = 0;
	int dd = dim;
	double *pDataList;
	double *maxDataList = new double[dd];
	double *minDataList = new double[dd];
	if(!strcmp(dataType,"kddcup99"))
	{
		pDataList = ((CKddCup99*)((*rawData)[0].get()))->getData();
		for(int i=0; i<dd; i++)
		{
			maxDataList[i] = pDataList[i];
			minDataList[i] = pDataList[i];
		}
		for(int i=0; i<rawData->size(); i++)
		{
			pDataList = ((CKddCup99*)((*rawData)[i].get()))->getData();
			for(int j=0; j<dd; j++)
			{
				maxDataList[j] = pDataList[j]>maxDataList[j] ? pDataList[j]:maxDataList[j];
				minDataList[j] = pDataList[j]<minDataList[j] ? pDataList[j]:minDataList[j];
			}
		}
		for(int i=0; i<rawData->size(); i++)
		{
			pDataList = ((CKddCup99*)((*rawData)[i].get()))->getData();
			for(int j=0; j<dd; j++)
			{
				if(minDataList[j]<maxDataList[j])
					pDataList[j] = (pDataList[j]-minDataList[j])/(maxDataList[j]-minDataList[j]);
			}
		}
	}
	else if(!strcmp(dataType,"vectorclass"))
	{
		dd = dim;
		maxDataList = new double[dd];
		minDataList = new double[dd];
		pDataList = ((CDoubleVectorClass*)((*rawData)[0].get()))->getData();
		for(int i=0; i<dd; i++)
		{
			maxDataList[i] = pDataList[i];
			minDataList[i] = pDataList[i];
		}
		for(int i=0; i<rawData->size(); i++)
		{
			pDataList = ((CDoubleVectorClass*)((*rawData)[i].get()))->getData();
			for(int j=0; j<dd; j++)
			{
				maxDataList[j] = pDataList[j]>maxDataList[j] ? pDataList[j]:maxDataList[j];
				minDataList[j] = pDataList[j]<minDataList[j] ? pDataList[j]:minDataList[j];
			}
		}
		for(int i=0; i<rawData->size(); i++)
		{
			pDataList = ((CDoubleVectorClass*)((*rawData)[i].get()))->getData();
			for(int j=0; j<dd; j++)
			{
				if(minDataList[j]<maxDataList[j])
					pDataList[j] = (pDataList[j]-minDataList[j])/(maxDataList[j]-minDataList[j]);
			}
		}
	}

	if(strcmp(outlierDefinition,"KnnOutlier")==0)
	{
		OutlierDefinition = new CKnnOutlier(k);
	}
	else if(strcmp(outlierDefinition,"KthOutlier")==0)
	{
		OutlierDefinition = new CKthOutlier(k);
	}
	else
	{
		cout<<"No such outlier definition!"<<endl;
	}

	if(strcmp(detectionAlgorithm,"ORCA")==0)
	{
		OutlierDetector = new CORCA(*rawData, OutlierDefinition, metric, n, k, m);
	}
	else if(strcmp(detectionAlgorithm,"iORCA")==0)
	{
		OutlierDetector = new CiORCA(*rawData, OutlierDefinition, metric, psm, n, k, m);
	}
	else
	{
		cout<<"No such detection algorithm!"<<endl;
	}
	long startTime = clock();
	TOP_N_Outlier = OutlierDetector->getOutlier(4*p1, 4*p2+3);
	long stopTime = clock();
	file1<<"No."<<"\t"<<"Object ID"<<"\t"<<"Outlier degree"<<endl;
	cout << "No." << "\t" << "Object ID" << "\t" << "Outlier degree" << endl;
	for(int i=0; i<n; i++)
	{
		file1<<i<<"\t"<<TOP_N_Outlier[i].dataID<<"\t"<<TOP_N_Outlier[i].dis<<endl;
		cout << i << "\t" << TOP_N_Outlier[i].dataID << "\t" << TOP_N_Outlier[i].dis << "\t"<< ((CStock*)((*rawData)[i].get()))->getStockID()<<"\t"<< ((CStock*)((*rawData)[i].get()))->getStockName()<<endl;
	}
	delete[] TOP_N_Outlier;
	delete[] maxDataList;
	delete[] minDataList;
	file2.close();
	return 0;
}

