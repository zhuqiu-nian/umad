#ifndef EUCLIDEANHAMMING_H
#define EUCLIDEANHAMMING_H
#include "MetricDistance.h"

/** @file EuclideanHamming.h
* @classes about metirc methods
* @author Honglong Xu
* @version 2014-08-18
*/

/**
* @class CEuclideanHamming
* @brief abstract class about metric methods
* @author Honglong Xu
*
* this class will generate the distance of two object
*/

class CEuclideanHamming :
	public CMetricDistance
{
public:
	CEuclideanHamming();
	~CEuclideanHamming();

	/**compute the distance of tow objects
	*@return distance of two CMetricData object
	*/
	virtual double getDistance(CMetricData*,CMetricData*);

	/**compute the distance of two array of dimention d
	*@return distance of two double array
	*/
	virtual double getDistance(double* v1,double* v2,int _numLen,int _cateLen);
	virtual double getDistance(CMetricData* _obj1, CMetricData* _obj2, int length, int start, int end);
};
/**@}*/
#endif





