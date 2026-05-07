#include "../../../HeaderFiles/outlierdetection/outlierdefinition/KNN.h"


CKNN::CKNN()
{
	dataID = -1;
	dis = std::numeric_limits<double>::max();
}

CKNN::CKNN(int _k, double _knnd)
{
	dataID = _k;
	dis = _knnd;
}

void CKNN::reset()
{
	dataID = -1;
	dis = std::numeric_limits<double>::max();
}

bool CKNN::operator < (CKNN &_KNN)
{
	if(this->dis < _KNN.dis)
		return true;
	else
		return false;
}

bool CKNN::operator == (CKNN &_KNN)
{
	if(this->dis == _KNN.dis)
		return true;
	else
		return false;
}

bool CKNN::operator > (CKNN &_KNN)
{
	if(this->dis > _KNN.dis)
		return true;
	else
		return false;
}

CKNN& CKNN::operator = (const CKNN &_KNN)
{
	this->dis = _KNN.dis;
	this->dataID = _KNN.dataID;
	return *this;
}

CKNN::~CKNN()
{
}