#ifndef KTHOUTLIER_H
#define KTHOUTLIER_H
#include "OutlierDefinition.h"

class CKthOutlier:public COutlierDefinition
{
private:
	int k;
	double weight;
	/**k nearest neighbor distances*/
	//double* knnd;
	//int *knn;
	int neighborNum;
	CKNN *knn;
public:
    /**none parameter constructor*/
    CKthOutlier(void);
	CKthOutlier(int _k);
    
	int getK();
	//void setKnnd(double* _knnd);
	//double* getKnnd();
	//virtual int* getKnn();
	virtual CKNN* getKnn();
	virtual void setWeight();
	virtual double getWeight();
	void reset();
	virtual void setNeighborNum(int num);
	virtual int getNeighborNum();
	virtual COutlierDefinition *CreateInstance(int _k);
	/**destructor*/
    virtual ~CKthOutlier(void);
};

#endif