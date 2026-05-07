#ifndef KNN_H
#define KNN_H
#include <memory>

typedef struct
{
	double TPR;
	double FPR;
} ROCPoint;

class CKNN
{
public:
	double dis;
	int dataID;

	CKNN();
    CKNN(int _dataID, double _dis);
	void reset();
	bool operator < (CKNN &_KNN);
	bool operator == (CKNN &_KNN);
	bool operator > (CKNN &_KNN);
	CKNN& operator = (const CKNN &_KNN);
    ~CKNN(void);
};

#endif