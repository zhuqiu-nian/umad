#include "../../../HeaderFiles/outlierdetection/outlierdefinition/OutlierDefinition.h"


/**none parameter constructor*/
COutlierDefinition::COutlierDefinition(void)
{

}

/**destructor*/
COutlierDefinition::~COutlierDefinition(void)
{

}

int COutlierDefinition::getK()
{
	return 0;
}

void COutlierDefinition::setKnnd(CKNN* _knnd, int k)
{
}

/*double* COutlierDefinition::getKnnd()
{
	return 0;
}

int* COutlierDefinition::getKnn()
{
	return 0;
}*/

CKNN* COutlierDefinition::getKnn()
{
	return 0;
}

void COutlierDefinition::setState(bool state)
{
	isActive = state;
}

bool COutlierDefinition::getState()
{
	return isActive;
}

void COutlierDefinition::setWeight()
{

}

double COutlierDefinition::getWeight()
{
	return 0;
}

void COutlierDefinition::setNKWeight()
{

}
double COutlierDefinition::getNKWeight()
{
	return 0;
}

void COutlierDefinition::setNeighborNum(int num)
{

}

int COutlierDefinition::getNeighborNum()
{
	return 0;
}

COutlierDefinition* COutlierDefinition::CreateInstance(int _k)
{
	return 0;
}

COutlierDefinition* COutlierDefinition::CreateInstance(int _k, int _n)
{
	return 0;
}