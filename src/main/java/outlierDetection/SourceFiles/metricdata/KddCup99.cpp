/** @file KddCup99.cpp
* @describe a kind of object
* @author Honglong Xu
* @version 2014-08-17
*/

#include "../../HeaderFiles/metricdata/KddCup99.h"
#include <sstream>

string& strReplace(string& str,const string& old_value,const string& new_value)     
{
	for(string::size_type   pos(0);   pos!=string::npos;   pos+=new_value.length())
	{
		if((pos=str.find(old_value,pos))!=string::npos)
			str.replace(pos,old_value.length(),new_value);
		else
			break;
	}     
	return str;
}


CKddCup99::CKddCup99()
{
    //dim=0;
	numLen=0;
	cateLen=0;
	dataList=0;
	isNormal=true;
}

//CKddCup99::CKddCup99(double *data,int length,bool _isNormal)
CKddCup99::CKddCup99(double *data,int _numLen,int _cateLen,bool _isNormal)
{
	//int dim = length;
	numLen = _numLen;
	cateLen = _cateLen;
	int dim = numLen+cateLen;
	dataList = new double[dim];
    for (int i = 0;i<dim;i++)
    {
        dataList[i]=data[i];
    }
	isNormal = _isNormal;
}

CKddCup99::~CKddCup99()
{
	delete[] dataList;
}

double* CKddCup99::getData() const
{
    return dataList;
}

int CKddCup99::getNumLen() const
{
    return numLen;
}

int CKddCup99::getCateLen() const
{
    return cateLen;
}

bool CKddCup99::getState()
{
	return isNormal;
}

vector<shared_ptr<CMetricData> >* CKddCup99::loadData(string fileName,int maxDataNum,int dimension,int &outlierNum)
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
	//int dimension = numLen+cateLen;
    vector<shared_ptr<CMetricData> > *a=new vector<shared_ptr<CMetricData> >;
    double* data=NULL;
	bool _isNormal = true;
    shared_ptr<CKddCup99> temp=shared_ptr<CKddCup99>();
    string str="";
    in >> dim >> num;
    getline(in,str);
    //dim = dim>dimension ? dimension:dim;
	if(dimension>dim)
	{
		cout<<"Wrong dimension!"<<endl;
		exit(0);
	}
    num = num>maxDataNum ? maxDataNum:num;
	if(outlierNum>num)
	{
		cout<<"Wrong outlier number! It must be less than dataset size."<<endl;
		exit(0);
	}
	int inlierNum = num-outlierNum;
	int _numLen = dimension;
	int _cateLen = dim - dimension;
	double* tempData = new double[dim-4];

	map<string,int> mapint;
	//int mapValue = 0;
	int k = 0;
	string tempStr = "";
    for(i=0;i<num;i++)
    {
        getline(in,str);
        stringstream newStr(strReplace(str,","," "));
		//stringstream newStr(str);
        data = new double[dim];
		newStr>>data[0];
		for(j=1; j<4; j++)
		{
			newStr>>tempStr;
			if(mapint.count(tempStr))
			{
				data[j+33] = mapint[tempStr];
			}
			else
			{
				mapint.insert(pair<string,int>(tempStr,k));
				//cout<<i<<"  "<<tempStr<<"  "<<k<<endl;
				data[j+33] = k;
				k++;
			}
		}
		for(j=4; j<dim-1; j++)
        {
            newStr>>tempData[j-4];
        }
		newStr>>tempStr;
		
		//2,3,4,7,12,21,22
		//2,7,16,17
		data[1] = tempData[0];
		data[2] = tempData[1];
		data[37] = tempData[2];
		data[38] = tempData[7];
		data[39] = tempData[16];
		data[40] = tempData[17];
		for(int j=3; j<7; j++)
		{
			data[j] = tempData[j];
		}
		for(int j=7; j<15; j++)
		{
			data[j] = tempData[j+1];
		}
		for(int j=15; j<34; j++)
		{
			data[j] = tempData[j+3];
		}

		if(!strcmp(tempStr.c_str(),"normal."))
		{
			_isNormal = true;
			if(ann<inlierNum)
			{
				temp.reset(new CKddCup99(data,_numLen,_cateLen,_isNormal));
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
				temp.reset(new CKddCup99(data,_numLen,_cateLen,_isNormal));
				a->push_back(temp);
				aon++;
			}
			else
			{
				i--;
			}
		}
    }
	outlierNum = aon;
	cout<<"Actually read dataset size: "<<i<<"  outlier number: "<<outlierNum<<endl;
    return a;
}
/*
int CKddCup99 ::writeExternal(ofstream &out)
{
    int size=0;
	int dim=41;
    //out.write((char*) (&numLen),sizeof(int));
    //size += sizeof(int);
	//out.write((char*) (&cateLen),sizeof(int));
    //size += sizeof(int);
	out.write((char*)dataList,dim*sizeof(double));
    size += dim*sizeof(double);
	return size;
}

int CKddCup99 ::readExternal(ifstream &in)
{
	int size=0;
	int dim=41;
	dataList = new double[dim];
	in.read((char*)dataList,dim*sizeof(double));    
    size+=dim*sizeof(double);
    return size;
}
*/
CreateFuntion CKddCup99::getConstructor()
{
    CreateFuntion constructor =& CreateInstance;
    return constructor;
}

void* CKddCup99:: CreateInstance()
{
    return new CKddCup99();
}