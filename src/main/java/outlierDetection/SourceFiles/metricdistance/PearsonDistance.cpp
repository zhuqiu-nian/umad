//#define CHECKPOINTER
#include "../../HeaderFiles/metricdistance/PearsonDistance.h"
#include "../../HeaderFiles/metricdata/Stock.h"
#include <cmath>

/** no parameter constructor function*/
CPearsonDistance::CPearsonDistance()
{
}

/** destructor function*/
CPearsonDistance::~CPearsonDistance()
{
}

double CPearsonDistance::getDistance(double*v1, double*v2,int length)
{
#ifdef CHECKPOINTER
	if(v1==NULL || v2==NULL)
		cout<<"pointer v1 or v2 is null"<<endl;
#endif
    double _distance = 0;
    /*
     * 协方差计算
     * ∑XY-∑X*∑Y/N
     * @param {array} source 源K线数据
     * @param {array} data 对比的K线数据,data.length=source.length
     * @param {string} field 参数
     */
     //calcCov = function(source, data, field) {
    double calcCov = 0;
    double mulE = 0;
    double sourceE = 0;
    double dataE = 0;
    for (int i = 0; i < length; i++)
    {
        mulE += v1[i] * v2[i];
        sourceE += v1[i];
        dataE += v2[i];
    }
    calcCov = mulE - sourceE * dataE / length;

    /*
     * 皮尔森分母计算
     * Math.sqrt((∑X^2-(∑X)^2/N)*((∑Y^2-(∑Y)^2/N))
     * @param {array} source 源K线数据
     * @param {array} data 对比的K线数据,data.length=source.length
     * @param {string} field 参数
     */
    double pearsonDeno = 0;
    double sourceSquareAdd = 0;
    double sourceAdd = 0;
    double dataSquareAdd = 0;
    double dataAdd = 0;
    for (int i = 0; i < length; i++)
    {
        sourceSquareAdd += v1[i] * v1[i];
        sourceAdd += v1[i];
        dataSquareAdd += v2[i] * v2[i];
        dataAdd += v2[i];
    }
    pearsonDeno = sqrt(abs((sourceSquareAdd - sourceAdd * sourceAdd / length) * (dataSquareAdd - dataAdd * dataAdd / length)));
    return 1.0 - calcCov / pearsonDeno;
}

double CPearsonDistance::getDistance(CMetricData* _obj1, CMetricData* _obj2, int length, int start, int end)
{
#ifdef CHECKPOINTER
    if (_obj1 == NULL || _obj2 == NULL)
        cout << "pointer v1 or v2 is null" << endl;
#endif
    if (start > end)
    {
        cout << "Error: start is larger than end!" << endl;
        exit(0);
    }
    else if (end > length)
    {
        cout << "Error: end is larger than length!" << endl;
        exit(0);
    }
    double* v1 = ((CStock*)_obj1)->getData();
    double* v2 = ((CStock*)_obj2)->getData();
    double _distance = 0;
    double calcCov = 0;
    double mulE = 0;
    double sourceE = 0;
    double dataE = 0;
    for (int i = start; i < end; i++)
    {
        mulE += v1[i] * v2[i];
        sourceE += v1[i];
        dataE += v2[i];
    }
    calcCov = mulE - sourceE * dataE / end;

    double pearsonDeno = 0;
    double sourceSquareAdd = 0;
    double sourceAdd = 0;
    double dataSquareAdd = 0;
    double dataAdd = 0;
    for (int i = start; i < end; i++)
    {
        sourceSquareAdd += v1[i] * v1[i];
        sourceAdd += v1[i];
        dataSquareAdd += v2[i] * v2[i];
        dataAdd += v2[i];
    }
    pearsonDeno = sqrt(abs((sourceSquareAdd - sourceAdd * sourceAdd / end) * (dataSquareAdd - dataAdd * dataAdd / end)));
    return 1.0 - calcCov / pearsonDeno;
}

double CPearsonDistance::getDistance(CMetricData *_obj1,CMetricData *_obj2)
{
#ifdef CHECKPOINTER
	if(_obj1==NULL || _obj2==NULL)
		cout<<"null pointer parameters"<<endl;
#endif
	return getDistance(((CStock*)_obj1)->getData(),((CStock*)_obj2)->getData(),((CStock*)_obj1)->getLen());
}











