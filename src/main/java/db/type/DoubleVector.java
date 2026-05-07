package db.type;

import db.TableManager;
import db.table.Table;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;


/**
 * 代表空间向量的类，而且向量的坐标是Double类型
 */
public class DoubleVector extends IndexObject
{

    private static final long serialVersionUID = 1275897899226331100L;

    Table table;

    /**
     * double array to store the data
     */
    double[] data;

    public DoubleVector()
    {
    }

    /**
     * 使用double数组构建向量
     *
     * @param data 数据
     */
    public DoubleVector(double[] data)
    {
        this.data = data.clone();
    }

    //    /**
    //     * Builds an instance from a space-separated {@link String} of doubles.
    //     *
    //     * @param table
    //     * @param rowID
    //     * @param dataString
    //     */

    /**
     * 从字符串中构建实例，字符串中的Double数据之间以空格分隔
     *
     * @param table      绑定的数据表
     * @param rowID      行ID
     * @param dataString 字符串
     */
    public DoubleVector(Table table, int rowID, String dataString)
    {
        super(rowID);
        this.table = table;
        String[] row = dataString.split("\\s+");
        data = new double[row.length];
        for (int i = 0; i < row.length; i++)
        {
            data[i] = Double.parseDouble(row[i]);
        }
    }

    //    /**
    //     * Builds an instance from a double array.
    //     *
    //     * @param rowID
    //     * @param data
    //     *            the double array containning all the elements. cannot be null
    //     */

    /**
     * 从一个Double类型的数组中构建{@link DoubleVector}实例
     *
     * @param table 绑定的数据表
     * @param rowID 行ID
     * @param data  用来构建向量的数组数据，这个数组必须包含所有元素，不能为空
     */
    public DoubleVector(Table table, int rowID, double[] data)
    {
        super(rowID);
        if (data == null) throw new IllegalArgumentException("null data constructing DoubleVector");
        this.table = table;
        this.data  = data.clone();
    }

    //    /**
    //     * @return the double array
    //     */

    /**
     * 获取向量值
     *
     * @return 返回向量值
     */
    public double[] getData()
    {
        return data;
    }

    //    /**
    //     * @return the dimension ( length) of the vector
    //     */

    /**
     * 获取向量模长
     *
     * @return 返回向量长度
     */
    public int size()
    {
        return data.length;
    }

    /**
     * 将压缩的数据进行解压。
     *
     * <pre>
     *     例如，原向量是二维向量(0.5,0.4),rowIDLength=2,
     *     则拓展后返回值为{(0.5,0.4),(0.5,0.4)}。
     * </pre>
     *
     * @see IndexObject#expand()
     */
    public IndexObject[] expand()
    {
        IndexObject[] dbO = new IndexObject[rowIDLength];
        for (int i = 0; i < rowIDLength; i++)
        {
            dbO[i] = new DoubleVector(table, rowIDStart + i, data);
        }
        return dbO;
    }

    /*
     * (non-Javadoc)
     *
     * @see type.IndexObject#compareTo(type.IndexObject)
     */

    /**
     * 比较该向量与传入向量的大小
     *
     * @param oThat 用于比较的向量
     * @return <pre>
     *             0     传入的向量和该向量是同一个点或者两者模长和值完全相等
     *             -1    传入的向量模长大于该向量或者两者模长相等但是传入的向量值大于该向量
     *             1     传入的向量模长小于该向量或者两者模长相等但是传入的向量值小于该向量
     *         </pre>
     * @see IndexObject#compareTo(IndexObject)
     */
    public int compareTo(IndexObject oThat)
    {
        if (!(oThat instanceof DoubleVector)) throw new ClassCastException("not compatible");
        return compareTo((DoubleVector) oThat);
    }

    /**
     * 比较该向量与传入向量的大小
     *
     * @param oThat 用于比较的向量
     * @return <pre>
     *             0     传入的向量和该向量是同一个点或者两者模长和值完全相等
     *             -1    传入的向量模长大于该向量或者两者模长相等但是传入的向量值大于该向量
     *             1     传入的向量模长小于该向量或者两者模长相等但是传入的向量值小于该向量
     *         </pre>
     */
    public int compareTo(DoubleVector oThat)
    {
        DoubleVector that = oThat;
        if (this == that) return 0;

        if (this.size() < that.size()) return -1;
        else if (this.size() > that.size()) return 1;
        else
        {
            for (int i = 0; i < this.size(); i++)
            {
                double double1 = data[i];
                double double2 = that.data[i];
                if (double1 < double2) return -1;
                else if (double1 > double2) return 1;
            }
            return 0;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object that)
    {
        if (!(that instanceof DoubleVector)) return false;
        return Arrays.equals(this.data, ((DoubleVector) that).data);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    // taken from Joshua Bloch's Effective Java
    public int hashCode()
    {
        int result = 17;
        for (int i = 0; i < data.length; i++)
        {
            long _long = Double.doubleToLongBits(data[i]);
            result = 37 * result + (int) (_long ^ (_long >>> 32));
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer("[DoubleVector, length:");
        sb.append(data.length).append(" data:").append(data[0]);
        for (int i = 1; i < data.length; i++)
             sb.append(", ").append(data[i]);
        sb.append("]");
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        data = new double[in.readInt()];
        for (int i = 0; i < data.length; i++)
        {
            data[i] = in.readDouble();
        }
        String indexPrefix = (String) in.readObject();
        table = TableManager.getTableManager(indexPrefix).getTable(in.readInt());
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeInt(data.length);
        for (int i = 0; i < data.length; i++)
        {
            out.writeDouble(data[i]);
        }
        out.writeObject(table.getTableManagerName());
        out.writeInt(table.getTableLocation());
    }
}
