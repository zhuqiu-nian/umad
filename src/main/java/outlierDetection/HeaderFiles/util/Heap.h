//Heap.h

#pragma once
#include <assert.h>

using namespace std;

template <typename T>
void mswap(T &a, T &b)
{
    T tmp = a;
    a = b;
    b = tmp;
}

template <typename T,typename Compare = std::less<T>>
class Heap
{
public:
    int hSize ;    //堆空间
    int hCurNum;//堆内已占用空间
    T *data;

private:
    Compare comp;//比较函数
public:
    Heap(int size)
    {
        hSize = size;
        assert(hSize>0);
        data = new T[hSize];
        hCurNum = 0;
    };

	Heap(T *_data, int size)
    {
        hSize = size;
        assert(hSize>0);
        data = _data;
        hCurNum = 0;
    };

	void reset()
    {
        hCurNum = 0;
    };

    ~Heap(void)
    {
        if(data!=NULL)
            delete []data;
    };

    void headAdd(T num)
    {
        if (hCurNum==hSize)
        {
            if (comp(num,data[0]))//greater 大顶堆 保留最小的K个数；less 小顶堆 保留最大的K个数
                return;
            data[0]=num;
            HeapFixDown(0,hCurNum);
        }
        else
        {
            data[hCurNum++]=num;
            HeapFixUp(hCurNum-1);
        }
    };
    //最大堆排序后得到升序序列；最小堆排序后得到降序序列
    void sort()
    {
        for (int i=hCurNum-1; i >=1 ; --i)
        {
            mswap(data[i],data[0]);
            HeapFixDown(0,i);
        }
    }

	void sortK(int k)
    {
        for (int i=hCurNum-1; i >=hCurNum-k; --i)
        {
            mswap(data[i],data[0]);
            HeapFixDown(0,i);
        }
    }

    T GetHnum()//获取最大堆的最小值或者最小堆的最大值
    {
        return data[0];
    };

    void HeapFixUp(int index)
    {
        //assert (index < hCurNum);
		if(index>hCurNum)
			cout<<"index > hCurNum"<<endl;
        T tmp=data[index];
        int j = (index - 1)/2;//父节点
        while(j>=0 && index !=0)
        {
            if(comp(data[j],tmp))
                break;
            data[index]=data[j];
            index = j;
            j = (index - 1)/2;
        }
        data[index]=tmp;
    };

    //从节点index开始进行向下调整
    void HeapFixDown(int index, int n)
    {
        //assert(index<hCurNum);
        //assert(n<hCurNum);
		if(index>hCurNum)
			cout<<"(index>hCurNum)"<<endl;
        T tmp=data[index];
        int j = index*2+1;
        while(j<n)
        {
            if(j+1 < n && comp(data[j+1],data[j]))//大顶堆中左右孩子找最大的，小顶堆左右孩子找最小的
                ++j;
            if(comp(tmp,data[j]))
                break;
            data[index]=data[j];
            index = j;
            j = index*2+1;
        }
        data[index]=tmp;
    };
};

