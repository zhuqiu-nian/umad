#ifndef SQAI_METRIC_DNACLASSMETRIC_H

#define SQAI_METRIC_DNACLASSMETRIC_H



/**@file DNAMetric.h

 * @brief A metric to calculate the distance of two index objects,

 * and this metric is especial for DNA sequence objects.

 * @author Yaoda Liu(2011150337@email.szu.edu.cn)

 * @date 2013/3/18

 *

 * This class defines a metric to calculate the distance of

 * two DNA sequence, and then, for the final goal, build the index.

*/



#include <iostream>

#include "MetricDistance.h"

#include "../metricdata/DNAClass.h"



using namespace std;



/**

 * @class

 * @brief This class defines a metric to compute distance of two DNA sequence.

*/

class CDNAClassMetric:

	public CMetricDistance

{

public:



    /**

     * @brief A edit distance matrix of each two dna symbols. 

     */

	static double EditDistanceMatrix[DNASYMBOLNUMBER][DNASYMBOLNUMBER];



    /**

     * @brief A no parameter constructor, do nothing.

     * @return void

     */

    CDNAClassMetric();



    /** 

     * @brief A destructor, do nothing

     * @return void

     */

    ~CDNAClassMetric();



    /**

     * @brief This method return two IndexObjects' distance.

     * @return  Return a double type distance of two objects.

     */

	virtual double getDistance(CMetricData* one, CMetricData* two);



    /**

     * @brief  This method return two DNA' distance.

     *          Sum up edit distance of two DNA.

     * @return   Return a double type of distance of two objects.

     */

	double getDistance(CDNAClass* one, CDNAClass* two);
    double getDistance(CMetricData* _obj1, CMetricData* _obj2, int length, int start, int end);


};



#endif  

// SQAI_METRIC_DNAMETRIC_H