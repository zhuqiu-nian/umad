#ifndef KDDCUP99_H
#define KDDCUP99_H

#include "MetricData.h"
#include "../util/ObjectFactory.h"
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

/** @file KddCup99.h
 * @describe a kind of object
 * @author Honglong Xu
 * @version 2014-08-17
*/

/**
* @class CKddCup99
* @author Honglong Xu
*
* 
*/
class CKddCup99 :
    public CMetricData
{
public:
    /**@brief none parameter constructor*/
    CKddCup99();
	CKddCup99(double *data,int _numLen,int _cateLen,bool _isNormal);
	//CKddCup99(double *data,int length,bool _isNormal);
	virtual ~CKddCup99();
	//static vector<shared_ptr<CMetricData> >* loadData(string fileName,int maxDataNum,int numLen,int cateLen);
	static vector<std::shared_ptr<CMetricData> >* loadData(string fileName,int maxDataNum,int dimension,int &outlierNum);
	double* getData() const;
	virtual bool getState();
	int getNumLen() const;
	int getCateLen() const;
	//int getLen() const;
	//virtual int writeExternal(ofstream &out);
	//virtual int readExternal(ifstream &in);
	static CreateFuntion getConstructor();
	static void* CreateInstance();

private:
	int numLen;
	int cateLen;
	bool isNormal;
	double* dataList;
};
#endif