/** @file EuclideanHamming.cpp
 * @classes about metirc methods
 * @author Honglong Xu
 * @version 2014-08-18
*/

//#define CHECKPOINTER
#include "../../HeaderFiles/metricdistance/EuclideanHamming.h"
#include "../../HeaderFiles/metricdata/KddCup99.h"
#include <cmath>

/** no parameter constructor function*/
CEuclideanHamming::CEuclideanHamming()
{
}

/** destructor function*/
CEuclideanHamming::~CEuclideanHamming()
{
}
//无意义，勿调用
double CEuclideanHamming::getDistance(CMetricData* _obj1, CMetricData* _obj2, int length, int start, int end)
{
#ifdef CHECKPOINTER
	if (v1 == NULL || v2 == NULL)
		cout << "pointer v1 or v2 is null" << endl;
#endif
	double* v1 = ((CKddCup99*)_obj1)->getData();
	double* v2 = ((CKddCup99*)_obj2)->getData();
	return getDistance(v1, v2, length, end);
}

/** compute the distance of two double array. 
* @param[in] v1 receives a pointer which points to a double array
* @param[in] v2 receives a pointer which points to a double array
* @param[in] _length receives a int value which representst he dimension of the array
* @return returns a double which represents the distance of the two given arrays
*/
double CEuclideanHamming::getDistance(double *v1,double *v2,int _numLen,int _cateLen)
{
#ifdef CHECKPOINTER
	if(v1==NULL || v2==NULL)
		cout<<"pointer v1 or v2 is null"<<endl;
#endif

	double _distance=0;
	//int numLen = 34;
	//int cateLen = 7;
	for(int i=0;i<_numLen;i++)
    {
		_distance+=pow((v1[i]-v2[i]),2);
    }
	for(int i=_numLen;i<_numLen+_cateLen;i++)
    {
		if(v1[i]!=v2[i])
		{
			_distance+=1;
		}
    }
	return sqrt(_distance);
}

/** compute the distance of two objects of class CMetricData, or its subclass. 
* @param[in] _obj1 receives a pointer of object
* @param[in] _obj2 receives a pointer of object
* @return return a double value which represents the distance of the two given objects
*/
double CEuclideanHamming::getDistance(CMetricData *_obj1,CMetricData *_obj2)
{
#ifdef CHECKPOINTER
	if(_obj1==NULL || _obj2==NULL)
		cout<<"null pointer parameters"<<endl;
#endif

	return getDistance(((CKddCup99*)_obj1)->getData(),((CKddCup99*)_obj2)->getData(),((CKddCup99*)_obj2)->getNumLen(),((CKddCup99*)_obj2)->getCateLen());
}