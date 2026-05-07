#include "../../../HeaderFiles/outlierdetection/outlierdefinition/KthOutlier.h"


/**none parameter constructor*/
CKthOutlier::CKthOutlier(void)
{

}

CKthOutlier::CKthOutlier(int _k)
{
	k = _k;
	/*knnd = new double[k];
	knn = new int[k];
	for(int i=0; i<k; i++)
	{
		knnd[i] = std::numeric_limits<double>::max();
		knn[i] = -1;
	}*/
	knn = new CKNN[k];
	for(int i=0; i<k; i++)
	{
		knn[i].dis = std::numeric_limits<double>::max();
		knn[i].dataID = -1;
	}
	weight = std::numeric_limits<double>::max();
	neighborNum = 0;
}

/**destructor*/
CKthOutlier::~CKthOutlier(void)
{
	//delete[] knnd;
	delete[] knn;
}

int CKthOutlier::getK()
{
	return k;
}

double CKthOutlier::getWeight()
{
	return weight;
}

void CKthOutlier::setWeight()
{
	weight = knn[0].dis;
}

/*void CKthOutlier::setKnnd(double* _knnd)
{
	memcpy(knnd, _knnd, k*sizeof(double));
}

double* CKthOutlier::getKnnd()
{
	return knnd;
}

int* CKthOutlier::getKnn()
{
	return knn;
}*/

CKNN* CKthOutlier::getKnn()
{
	return knn;
}

void CKthOutlier::reset()
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

void CKthOutlier::setNeighborNum(int num)
{
	neighborNum = num;
}

int CKthOutlier::getNeighborNum()
{
	return neighborNum;
}

COutlierDefinition* CKthOutlier::CreateInstance(int _k)
{
	return new CKthOutlier(_k);
}