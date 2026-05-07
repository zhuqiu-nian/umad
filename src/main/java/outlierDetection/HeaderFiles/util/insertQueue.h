#include "../../HeaderFiles/outlierdetection/outlierdefinition/KNN.h"

template<class T>
bool insertQueue(T data, T *dataQueue, int k, bool isDescend)
{
	if((isDescend == true)&&(data < dataQueue[0]))
	{
		dataQueue[0] = data;
		for(int i=1; i<k; i++)
		{
			if(dataQueue[i-1] < dataQueue[i])
			{
				data = dataQueue[i-1];
				dataQueue[i-1] = dataQueue[i];
				dataQueue[i] = data;
			}
		}
		return true;
	}
	else if((isDescend == false)&&(data > dataQueue[0]))
	{
		dataQueue[0] = data;
		for(int i=1; i<k; i++)
		{
			if(dataQueue[i-1] > dataQueue[i])
			{
				data = dataQueue[i-1];
				dataQueue[i-1] = dataQueue[i];
				dataQueue[i] = data;
			}
		}
		return true;
	}
	return false;
}

bool insertQueue(CKNN data, CKNN *dataQueue, int k, bool isDescend)
{
	if((isDescend == true)&&(data < dataQueue[0]))
	{
		dataQueue[0] = data;
		for(int i=1; i<k; i++)
		{
			if(dataQueue[i-1] < dataQueue[i])
			{
				data = dataQueue[i-1];
				dataQueue[i-1] = dataQueue[i];
				dataQueue[i] = data;
			}
		}
		return true;
	}
	else if((isDescend == false)&&(data > dataQueue[0]))
	{
		dataQueue[0] = data;
		for(int i=1; i<k; i++)
		{
			if(dataQueue[i-1] > dataQueue[i])
			{
				data = dataQueue[i-1];
				dataQueue[i-1] = dataQueue[i];
				dataQueue[i] = data;
			}
		}
		return true;
	}
	return false;
}