#include "../../HeaderFiles/metricdata/Stock.h"
#include <sstream>

CStock::CStock()
{
	stockID = 0;
	//stockName = NULL;
	isNormal = true;
	priceNum = 0;
	dateList = NULL;
	priceList = NULL;
} 

CStock::CStock(int _stockID, string _stockName, bool _isNormal, int _priceNum, int* _dateList, double* _priceList)
{
	stockID = _stockID;
	stockName = _stockName;
    priceNum = _priceNum;
    int dateNum = priceNum / 4;
	dateList = new int[dateNum];
    for (int i = 0;i< dateNum;i++)
    {
		dateList[i]= _dateList[i];
    }
	priceList = new double[priceNum];
	for (int i = 0; i < priceNum; i++)
	{
		priceList[i] = _priceList[i];
	}
	isNormal = _isNormal;
}

CStock::~CStock()
{
    delete[](dateList);
	delete[](priceList);
}

double* CStock::getData() const
{
    return priceList;
}

int CStock::getStockID() const
{
    return stockID;
}
string CStock::getStockName() const
{
    return stockName;
}
int* CStock::getStockDate() const
{
    return dateList;
}

int CStock::getLen() const
{
    return priceNum;
}

bool CStock::getState()
{
	return isNormal;
}

vector<shared_ptr<CMetricData> >* CStock::loadData(string fileName,int maxDataNum,int dimension,int &outlierNum)
{
    ifstream in(fileName.c_str());
    if(!in)
    {
		cout<<"open raw data file:"<<fileName<<" failed!"<<endl;
        exit(0);
    }
    int num, dim, i, j;
    int _stockID, _dateNum;
    //char* _stockName;
    string _stockName;
    bool _isNormal = true;
    vector<shared_ptr<CMetricData> > *a=new vector<shared_ptr<CMetricData> >;
    double* data=NULL;
    int* _dateList = NULL;
    shared_ptr<CStock> temp=shared_ptr<CStock>();
    string str="";
    in >> dim >> num;
    getline(in,str);
    dim = dim>dimension ? dimension:dim;
    num = num>maxDataNum ? maxDataNum:num;
    //cout << "dim:" << dim << " num:" << num << " "<<endl;
    int dateNum = dim/4;
    _dateList = new int[dateNum];
    getline(in, str);
    stringstream newStr(str);
    for (i = 0; i < dateNum; i++)
    {
        newStr >> _dateList[i];
    }
    double* tempdata = new double[dim];
    for(i=0;i<num;i++)
    {
        getline(in,str);
        stringstream newStr(str);
        newStr >> _stockID;
        newStr >> _stockName;
        //cout << "_stockID:" << _stockID << " _stockName:" << _stockName << " ";
        data = new double[dim];
        for(j=0; j< dim; j++)
        {
            newStr>>data[j];
            tempdata[j] = data[j];
        }
		temp.reset(new CStock(_stockID, _stockName, _isNormal, dim, _dateList, data));
		a->push_back(temp);
    }
    cout << "data:\n";
	cout<<"Actually read dataset size: "<<i<<endl;

    return a;
}

CreateFuntion CStock::getConstructor()
{
    CreateFuntion constructor =& CreateInstance;
    return constructor;
}

void* CStock:: CreateInstance()
{
    return new CStock();
}