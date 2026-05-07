package index.structure;

import db.type.IndexObject;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

/**
 * 该类为所有类型内部节点的父类，在实现一个新的索引树类型的时候，该索引树的内部节点需要继承该抽象类。
 * <p>
 * 该类没有抽象方法，当子类继承该类的时候，只需要添加对应索引树类型内部节点的对应属性。
 * </p>
 */
abstract public class InternalNode extends Node
{
    private static final long serialVersionUID = -8299155768760784438L;
    long[] childAddresses;

    /**
     * 无参数构造方法(兼容序列话，尽量不要调用)
     */
    public InternalNode()
    {
    }

    /**
     * 内部节点类{@link InternalNode}的构造函数
     *
     * @param pivots         支撑点
     * @param size           当前内部节点所涉及的数据大小
     * @param childAddresses 当前内部节点的孩子指针
     */
    public InternalNode(IndexObject[] pivots, int size, long[] childAddresses)
    {
        super(pivots, size);
        if (childAddresses == null) throw new IllegalArgumentException("InternalNode childAddresses cannot be null");
        this.childAddresses = childAddresses;
    }

    /**
     * 获取当前内部节点所有的孩子节点
     *
     * @return 返回孩子节点的数目
     *         the number of children.
     */
    public int getNumChildren()
    {
        return childAddresses.length;
    }

    /**
     * 获取第 childIndex 个孩子指针
     * Return the address of a child
     *
     * @param childIndex 要获取的孩子节点下标
     *                   index of the child to be return
     * @return 返回第 childIndex 个孩子
     *         the address of the desired child node in the node file.
     **/
    public long getChildOf(int childIndex)
    {
        return childAddresses[childIndex];
    }

    /**
     * 设置第 childIndex 个孩子的指针
     *
     * @param index        要设置的孩子节点下标
     * @param childAddress 要设置的指针
     */
    public void setChildOf(int index, long childAddress)
    {
        childAddresses[index] = childAddress;
    }

    /**
     * 以字符串的形式返回当前内部节点的信息
     *
     * @return 当前内部节点的信息
     */
    @Override
    public String toString()
    {
        return "InternalNode{" + "childAddresses=" + Arrays.toString(childAddresses) + ", pivotSet=" + Arrays.toString(pivotSet) + ", dataSize=" + dataSize + '}';
    }

    /**
     * 对内部节点进行写入
     *
     * @param out 输出对象
     * @throws IOException 写入对象时发生IO异常
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeInt(childAddresses.length);
        for (int i = 0; i < childAddresses.length; i++)
        {
            out.writeLong(childAddresses[i]);
        }
    }

    /**
     * 读取内部节点
     *
     * @param in 输入对象
     * @throws IOException            读当前内部节点时发生IO异常
     * @throws ClassNotFoundException 对象类型错误
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        childAddresses = new long[in.readInt()];
        for (int i = 0; i < childAddresses.length; i++)
        {
            childAddresses[i] = in.readLong();
        }
    }
}
