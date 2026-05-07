#ifndef KNNOUTLIER_H
#define KNNOUTLIER_H
#include "OutlierDefinition.h"

class CKnnOutlier:public COutlierDefinition
{
private:
	int k;
	double weight;
	/**the number of neighbors have been found so far*/
	int neighborNum;
	//int *knn;
	CKNN *knn;

public:
    /**none parameter constructor*/
    CKnnOutlier(void);
	CKnnOutlier(const int _k);

	int getK();
	//void setKnnd(double* _knnd);
	//double* getKnnd();
	//virtual int* getKnn();
	virtual CKNN* getKnn();
	void reset();
	virtual void setWeight();
	virtual double getWeight();
	void setState(bool state);
	bool getState();
	virtual void setNeighborNum(int num);
	virtual int getNeighborNum();
	virtual COutlierDefinition *CreateInstance(int _k);
	/**destructor*/
    virtual ~CKnnOutlier(void);
};

#endif