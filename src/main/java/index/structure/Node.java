package index.structure;

import db.type.IndexObject;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

/**
 * 索引树的抽象节点对象
 * An index node is a node of a database index tree.
 * 这个抽象类是所有索引数据结构的基础类，例如radius-base(RBT), general hyper-plane (GHT) 和 vantage point (VP).
 * This interface is the base class for all different index structures, such as radius-base(RBT), general hyper-plane (GHT) and vantage point (VP).
 */
public abstract class Node implements Externalizable
{
    private static final long serialVersionUID = -1780322896404487946L;
    IndexObject[] pivotSet;
    /**
     * 以该节点为根的索引树所存储的数据集大小(包括该层的支撑点)
     */
    int           dataSize;

    /**
     * 无参数构造方法(兼容序列话，尽量不要调用)
     */
    public Node()
    {
    }

    /**
     * 节点类{@link Node}的构造函数
     *
     * @param pivotSet 支撑点集合
     * @param dataSize 以该节点为根的索引树所存储的数据集大小(包括该层的支撑点)
     */
    public Node(IndexObject[] pivotSet, int dataSize)
    {
        if (pivotSet == null) throw new IllegalArgumentException("pivot set cannot be null");
        this.pivotSet = pivotSet;

        if (dataSize < 0) throw new IllegalArgumentException("size cannot be less than '0'");
        this.dataSize = dataSize;
    }

    /**
     * 获取当前节点中支撑点的数目
     *
     * @return 支撑点数目
     *         the number of pivots.
     */
    public int getNumPivots()
    {
        return pivotSet.length;
    }

    /**
     * 获取当前节点中数据的大小
     *
     * @return 以该节点为根的索引树所存储的数据集大小
     *         the number of data points in the subtree with the current node as the root.
     */
    public int getDataSize()
    {
        return dataSize;
    }

    /**
     * 返回第pivotIndex个支撑点
     * Return a reference to a pivot
     *
     * @param pivotIndex index of the pivot to be return
     * @return 返回第pivotIndex个支撑点
     *         the key value of the pivot
     */
    public IndexObject getPivotOf(int pivotIndex)
    {
        return pivotSet[pivotIndex];
    }

    /**
     * 返回该节点所有的支撑点
     * Return all pivots
     *
     * @return 返回该节点所有的支撑点
     *         all pivots
     */
    public IndexObject[] getAllPivots()
    {
        return pivotSet;
    }
    /**
     * 以字符串的形式返回当前节点的信息
     *
     * @return 当前节点的信息
     */
    @Override
    public String toString()
    {
        return "Node{" + "pivotSet=" + Arrays.toString(pivotSet) + ", dataSize=" + dataSize + '}';
    }

    /**
     * 对节点进行写入
     *
     * @param out 输出对象
     * @throws IOException 写入对象时发生IO异常
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(pivotSet.length);
        for (IndexObject indexObject : pivotSet)
        {
            out.writeObject(indexObject);
        }
        out.writeInt(dataSize);
    }

    /**
     * 读取节点
     *
     * @param in 输入对象
     * @throws IOException            读当前节点时发生IO异常
     * @throws ClassNotFoundException 对象类型错误
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        pivotSet = new IndexObject[in.readInt()];
        for (int i = 0; i < pivotSet.length; i++)
        {
            pivotSet[i] = (IndexObject) in.readObject();
        }
        dataSize = in.readInt();
    }
}
