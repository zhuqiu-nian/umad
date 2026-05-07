#ifndef STOCK_H
#define STOCK_H

#include "MetricData.h"
#include "../util/ObjectFactory.h"
//#include "CDate.h"
#include <fstream>
#include <string>
#include<memory>

using namespace std;

#ifdef linux
#include <tr1/memory>
using std::tr1::shared_ptr;
#else
#include <memory>
#endif


class CStock :
    public CMetricData
{
public:
    CStock();
    CStock(int _stockID, string _stockName, bool _isNormal, int _priceNum, int* _dateList, double* _priceList);
    virtual ~CStock();
	static vector<std::shared_ptr<CMetricData> >* loadData(string fileName,int maxDataNum,int dimension,int &outlierNum);
    double* getData() const;
    int getStockID() const;
    string getStockName() const;
    int* getStockDate() const;
    int getLen() const;
	virtual bool getState();
    static CreateFuntion getConstructor();
    static void* CreateInstance();

private:
    int stockID;
    string stockName;
    bool isNormal;
    int priceNum;
    //CDate* dateList;
    int* dateList;
    double* priceList;
};
#endif