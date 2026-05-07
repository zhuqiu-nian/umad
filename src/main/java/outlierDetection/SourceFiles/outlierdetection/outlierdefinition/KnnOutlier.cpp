#include "../../../HeaderFiles/outlierdetection/outlierdefinition/KnnOutlier.h"

/**none parameter constructor*/
CKnnOutlier::CKnnOutlier(void)
{

}

CKnnOutlier::CKnnOutlier(const int _k)
{
	k = _k;
	/*knnd = new double[k];
	knn = new int[k];
	//memset(knnd, std::numeric_limits<double>::max(), k*sizeof(double));
	for(int i=0; i<k; i++)
	{
		knnd[i] = std::numeric_limits<double>::max();
		knn[i] = -1;
	}*/
	knn = new CKNN[k];
	weight = std::numeric_limits<double>::max();
	neighborNum = 0;
}

/**destructor*/
CKnnOutlier::~CKnnOutlier(void)
{
	//delete[] knnd;
	delete[] knn;
}

int CKnnOutlier::getK()
{
	return k;
}

double CKnnOutlier::getWeight()
{
	return weight;
}

void CKnnOutlier::setWeight()
{
	if(knn[0].dis == std::numeric_limits<double>::max())
	{
		weight = std::numeric_limits<double>::max();
	}
	else
	{
		double sum = 0;
		for(int i=0; i<k; i++)
		{
			sum += knn[i].dis;
		}
		weight = sum;
	}
}


CKNN* CKnnOutlier::getKnn()
{
	return knn;
}

void CKnnOutlier::reset()
{
	if(neighborNum!=-1)
	{
		neighborNum = 0;
		weight = std::numeric_limits<double>::max();
		for(int i=0; i<k; i++)
		{
			knn[i].dataID = -1;
			knn[i].dis = std::numeric_limits<double>::max();
		}
	}
}

void CKnnOutlier::setNeighborNum(int num)
{
	neighborNum = num;
}

int CKnnOutlier::getNeighborNum()
{
	return neighborNum;
}

COutlierDefinition* CKnnOutlier::CreateInstance(int _k)
{
	return new CKnnOutlier(_k);
}