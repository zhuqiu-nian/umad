#ifndef OUTLIERDEFINITION_H
#define OUTLIERDEFINITION_H
#include <memory>
#include "../../../HeaderFiles/outlierdetection/outlierdefinition/KNN.h"

class COutlierDefinition
{
private:
	bool isActive;

public:
    /**none parameter constructor*/
    COutlierDefinition(void);
	virtual int getK();
	virtual void setKnnd(CKNN* _knnd, int k);
	//virtual double* getKnnd();
	//virtual int* getKnn();
	virtual CKNN* getKnn();
	void setState(bool state);
	bool getState();
	virtual void setWeight();
	virtual void setNKWeight();
	virtual double getNKWeight();
	virtual double getWeight();
	virtual void setNeighborNum(int num);
	virtual int getNeighborNum();
	virtual COutlierDefinition *CreateInstance(int _k);
	virtual COutlierDefinition *CreateInstance(int _k, int _n);
	/**destructor*/
    virtual ~COutlierDefinition(void);
};

#endif
