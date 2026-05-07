/** @file DoubleVectorClass.cpp
* @describe a kind of object
* @author Honglong Xu
* @version 2014-10-03
*/

#include "../../HeaderFiles/metricdata/DoubleVectorClass.h"
#include <sstream>

/**@brief none parameter constructor*/
CDoubleVectorClass::CDoubleVectorClass()
{
    dim=0;
    dataList=0;
	isNormal=true;
} 

/**@brief constructor with two parameters
* @param data a double array represents the liner structure
* @param length length of the liner structure
*/
CDoubleVectorClass::CDoubleVectorClass(double *data,int length,bool _isNormal)
{
    dim = length;
	dataList = new double[dim];
    for (int i = 0;i<dim;i++)
    {
        dataList[i]=data[i];
    }
	isNormal = _isNormal;
}

/**@brief destructure*/
CDoubleVectorClass::~CDoubleVectorClass()
{
    delete[](dataList); 
}

/**@brief get the data list encapsulated in the objects
* @return return the memory address of the data list
*/
double* CDoubleVectorClass::getData() const
{
    return dataList;
}

/**@brief get the length of the data list
*@return return an value of int represents the length of the data list.
*/
int CDoubleVectorClass::getLen() const
{
    return dim;
}

bool CDoubleVectorClass::getState()
{
	return isNormal;
}

/**@brief load raw data from hard disk file and package the data into a objects of CDoubleVectorClass,then return the vector with all generated objects
* @param fileName name of the file that contains all the raw data waiting to be load
* @param maxDataNum maximum number of data list to be load from the file
* @param dimension length of each data list
* @return  return a vector contains all the objects generated before.
*/
vector<shared_ptr<CMetricData> >* CDoubleVectorClass::loadData(string fileName,int maxDataNum,int dimension,int &outlierNum)
{
    ifstream in(fileName.c_str());
    if(!in)
    {
		cout<<"open raw data file:"<<fileName<<" failed!"<<endl;
        exit(0);
    }
    int num, dim, i, j;
	/**actual normal object number*/
	int ann = 0;
	/**actual outlier number*/
	int aon = 0;
    vector<shared_ptr<CMetricData> > *a=new vector<shared_ptr<CMetricData> >;
    double* data=NULL;
    shared_ptr<CDoubleVectorClass> temp=shared_ptr<CDoubleVectorClass>();
    string str="";
	int tag = 0;
	bool _isNormal = true;
    in >> dim >> num;	
    getline(in,str);
    dim = dim>dimension ? dimension:dim;
    num = num>maxDataNum ? maxDataNum:num;
	if(outlierNum>num)
	{
		cout<<"wrong outlierNum!"<<endl;
		exit(0);
	}
	int inlierNum = num-outlierNum;
    for(i=0;i<num;i++)
    {
        getline(in,str);
        stringstream newStr(str);
        data = new double[dim];
        for(j=0; j<dim; j++)
        {
            newStr>>data[j];
        }
		newStr>>tag;
		if(tag == 1)
		{
			_isNormal = true;
			if(ann<inlierNum)
			{
				temp.reset(new CDoubleVectorClass(data, dim, _isNormal));
				a->push_back(temp);
				ann++;
			}
			else
			{
				i--;
			}
		}
		else
		{
			_isNormal = false;
			if(aon<outlierNum || outlierNum<=0)
			{
				temp.reset(new CDoubleVectorClass(data, dim, _isNormal));
				a->push_back(temp);
				aon++;//cout<<" "<<aon;
			}
			else
			{
				i--;
			}
		}
		/*if(i<2)
		{
			for(int j=0; j<dim; j++)
				cout<<data[j]<<"  ";
			cout<<"tag="<<tag<<endl;
		}*/
    }
	outlierNum = aon;
	//cout<<"Actually read dataset size: "<<i<<"  outlier number: "<<outlierNum<<endl;
	cout<<"Actually read dataset size: "<<i<<endl;
    return a;
}

/*
int CDoubleVectorClass ::writeExternal(ofstream &out)
{
    int size=0;
    out.write((char*) (&dim),sizeof(int));
    size += sizeof(int);
    out.write((char*)dataList,dim*sizeof(double));
    size += dim*sizeof(double);
    return size;
}

int CDoubleVectorClass ::readExternal(ifstream &in)
{
    int size=0;
    in.read((char*)(&dim),sizeof(int));    
    size+=sizeof(int);
    dataList = new double[dim];
    in.read((char*)dataList,dim*sizeof(double));    
    size+=dim*sizeof(double);
    return size;
}
*/

/**@brief return the name of a instance of this class
*@return return the name of a instance of this class
*/
CreateFuntion CDoubleVectorClass::getConstructor()
{
    CreateFuntion constructor =& CreateInstance;
    return constructor;
}

void* CDoubleVectorClass:: CreateInstance()
{
    return new CDoubleVectorClass();
}