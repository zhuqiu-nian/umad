package index.structure;

import db.type.IndexObject;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

/**
 * 所有索引类型的叶子节点的父类，在实现一个新的索引树类型的时候，该索引树的叶子节点需要继承该抽象类。
 * <p>
 * 该类没有抽象方法，当子类继承该类的时候，只需要添加对应索引树类型叶子节点的对应属性。
 * </p>
 */
public abstract class LeafNode extends Node
{
    private static final long serialVersionUID = -4809824415187941629L;
    IndexObject[] data;




    //标记数组 与 data等长 默认为0 若该数据同时作为支撑点与数据点 则 置为 1
    private boolean[] isPivotData;
    /**
     * 无参数构造方法(兼容序列话，尽量不要调用)
     */
    public LeafNode()
    {
    }

    /**
     * 叶子节点类{@link LeafNode}的构造函数
     *
     * @param pivots 支撑点集合
     * @param data   数据集合
     * @param size   数据集大小
     */
    public LeafNode(IndexObject[] pivots, IndexObject[] data, int size)
    {
        super(pivots, size);
        if (data == null) throw new IllegalArgumentException("Data cannot be null");
        this.data = data;
    }

    /**
     * 获取当前叶子节点中数据的大小
     *
     * @return 返回所存储的数据集的大小
     */
    public int getDataSize()
    {
        return data.length;
    }

    /**
     * 以字符串的形式返回当前叶子节点的信息
     *
     * @return 当前叶子节点的信息
     */
    @Override
    public String toString()
    {
        StringBuilder leaf = new StringBuilder("LeafNode{" + "pivotSet=" + Arrays.toString(pivotSet) + ", dataSize=" + dataSize + '\n');
        for (int i = 0; i < this.dataSize; i++)
        {
            leaf.append(this.data[i]+" , ");
        }
        leaf.append('}' + "\n");
        return leaf.toString();
    }

    /**
     * 获取指定下标的数据
     *
     * @param dataIndex 要获取的数据的下标
     * @return 返回第dataIndex个数据
     */
    public IndexObject getDataOf(int dataIndex)
    {
        return data[dataIndex];
    }

    /**
     * 获取该节点所有的数据
     *
     * @return 返回第dataIndex个数据
     */
    public IndexObject[] getAllData()
    {
        return data;
    }

    public void setData(IndexObject[] data) {
        this.data = data;
    }


    /**
     * 对叶子节点进行写入
     *
     * @param out 输出对象
     * @throws IOException 写入对象时发生IO异常
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeInt(data.length);
        for (int i = 0; i < data.length; i++)
        {
            out.writeObject(data[i]);
        }
        if (isPivotData != null) {
            for (int i = 0; i < isPivotData.length; i++) {
                out.writeBoolean(isPivotData[i]);
            }
        }else {
            for (int i = 0; i < data.length; i++)
            {
                out.writeBoolean(false);
            }
        }

}

    /**
     * 读取叶子节点
     *
     * @param in 输入对象
     * @throws IOException            读当前叶子节点时发生IO异常
     * @throws ClassNotFoundException 对象类型错误
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        data = new IndexObject[in.readInt()];
        for (int i = 0; i < data.length; i++)
        {
            data[i] = (IndexObject) in.readObject();
        }
        isPivotData = new boolean[data.length];
        for (int i = 0; i < isPivotData.length; i++)
        {
            isPivotData[i] =  in.readBoolean();
        }

    }

    public void setIsPivotData(boolean[] isPivotData) {
        this.isPivotData = isPivotData;
    }
    public boolean[] getIsPivotData() {
        return isPivotData;
    }
}
