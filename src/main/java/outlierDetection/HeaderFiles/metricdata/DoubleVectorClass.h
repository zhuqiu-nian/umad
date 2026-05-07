#ifndef DOUBLEVECTORCLASS_H
#define DOUBLEVECTORCLASS_H

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



/** @file DoubleVectorClass.h
 * @describe a kind of object
 * @author Honglong Xu
 * @version 2014-10-03
*/

/**
* @class CDoubleVectorClass
* @brief object with liner data structure
* @author Honglong Xu
*
* This class represents space vectors, where each element is a double but the last one is class tag.
*/

class CDoubleVectorClass :
    public CMetricData
{
public:
    /**@brief none parameter constructor*/
    CDoubleVectorClass();

    /**@brief constructor with two parameters
     * @param data a double array represents the liner structure
     * @param length length of the liner structure
     */
    CDoubleVectorClass(double *data,int length,bool _isNormal);

    /**@brief destructure*/
    virtual ~CDoubleVectorClass();

    /**@brief load raw data from hard disk file and package the data into a objects of CDoubleVector,then return the vector with all generated objects
     * @param fileName name of the file that contains all the raw data waiting to be load
     * @param maxDataNum maximum number of data list to be load from the file
     * @param dimension length of each data list
     * @return  return a vector contains all the objects generated before.
     */
	static vector<std::shared_ptr<CMetricData> >* loadData(string fileName,int maxDataNum,int dimension,int &outlierNum);

    /**@brief get the data list encapsulated in the objects
     * @return return the memory address of the data list
     */
    double* getData() const;

    /**@brief get the length of the data list
     *@return return an value of int represents the length of the data list.
     */
    int getLen() const;
	virtual bool getState();
    
    /*
    virtual int writeExternal(ofstream &out);
    virtual int readExternal(ifstream &in);
    */

    /**@brief return the name of a instance of this class
     *@return return the name of a instance of this class
     */
    static CreateFuntion getConstructor();
    static void* CreateInstance();

private:
    /**length of the data list*/
    int dim;
	bool isNormal;
    /**the data list*/
    double* dataList;
};
#endif