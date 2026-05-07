package db.type;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 所有数据类型点的抽象类
 */
public abstract class IndexObject implements Externalizable, Comparable<IndexObject>
{

    /**
     * 数据在原来数据文件内的行号
     **/
    int rowIDStart;
    /**
     * 在原来的数据文件中，相同数据的个数。
     **/
    int rowIDLength;

    /**
     * 空的构造函数（为了兼容序列化）
     */
    public IndexObject()
    {
    }

    /**
     * 构造函数
     *
     * @param rowID 该对象在源文件中的行号
     */
    public IndexObject(int rowID)
    {
        this.rowIDStart  = rowID;
        this.rowIDLength = 1;
    }

    /**
     * 设置数据在文件中的行号
     *
     * @param rowID 设置的行号
     */
    public void setRowID(int rowID)
    {
        this.rowIDStart = rowID;
    }

    /**
     * 获取数据在源文件中的行号
     *
     * @return 返回数据在源文件中的行号
     */
    public int getRowID()
    {
        return rowIDStart;
    }

    /**
     * 设置这个数据对象的行长，只有在压缩数据的时候才需要。压缩数据的原理如下：
     * 例如向量数据文件如下：
     * 行号      数据
     * 1     1.0 2.0 3.0
     * 2     1.0 2.0 3.0
     * 3     1.0 2.2 4.0
     * ......
     * 则当对数据进行压缩的时候，IndexObject={(1.0 2.0 3.0),rowIDStart=1,rowIDLength=2}
     *
     * @param length 设置的行长
     */
    public void setRowIDLength(int length)
    {
        rowIDLength = length;
    }

    /**
     * 获取数据的行长
     *
     * @return 返回数据的行长
     */
    public int getRowIDLength()
    {
        return rowIDLength;
    }

    /**
     * 获取数据维数
     *
     * @return 返回数据维数
     */
    public abstract int size();

    /**
     * 对该数据进行解压缩，如果数据压缩过，就返回解压缩的数据，否则返回原数据。
     *
     * @return 解压缩的数据
     */
    public abstract IndexObject[] expand();

    /**
     * 比较该对象与传入对象的大小
     *
     * @param oThat 用于比较的对象
     * @return <pre>
     *             0     完全相等
     *             -1    传入的对象大于该对象
     *             1     传入的对象小于该对象
     *         </pre>
     */
    public abstract int compareTo(IndexObject oThat);

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.rowIDStart);
        out.writeInt(this.rowIDLength);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        rowIDStart  = in.readInt();
        rowIDLength = in.readInt();
    }

}
