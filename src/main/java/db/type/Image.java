package db.type;

import db.TableManager;
import db.table.Table;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * 这个是图片对象的关键类。该类存储代表图片对象关键值的一系列浮点数。该类是专为UMAD图片数据库设计的类，
 * 可能不适用其他图片对象。
 */
public class Image extends IndexObject
{

    /**
     *
     */
    private static final long serialVersionUID = -6207971406392822640L;

    private Table table;

    /**
     * Floating numbers represent the features for an image.
     */
    private float[] m_Feas;

    private double[] max_Dist = {1.0, 60.0, 1.0};

    public Image()
    {
    }

    public Image(Table table, int rowID, float[] feas)
    {
        this(table, rowID, feas, null);
    }

    /**
     * 构造函数
     *
     * @param table   存储数据的table对象
     * @param rowID   行号
     * @param feas    定义了特征值的一个浮动数组。
     *                an array of floats over which the feature values are defined.
     * @param maxDist 最大距离数组
     */
    public Image(Table table, int rowID, float[] feas, double[] maxDist)
    {
        super(rowID);
        this.table = table;
        m_Feas     = new float[feas.length];
        for (int i = 0; i < feas.length; i++)
             m_Feas[i] = feas[i];
        if (maxDist != null)
        {
            max_Dist = new double[maxDist.length];
            for (int i = 0; i < max_Dist.length; i++)
                 max_Dist[i] = maxDist[i];
        }
    }

    /**
     * 获取相应下标对应的特征值
     *
     * @param index 下标
     * @return 返回相应下标的特征值
     */
    public float getFeature(int index)
    {
        return m_Feas[index];
    }

    @Override
    //TODO javadoc
    public int size()
    {
        return m_Feas.length;
    }

    /* (non-Javadoc)
     * @see type.IndexObject#expand()
     */
    @Override
    public IndexObject[] expand()
    {
        IndexObject[] dbO = new IndexObject[rowIDLength];
        for (int i = 0; i < rowIDLength; i++)
        {
            dbO[i] = new Image(table, rowIDStart + i, m_Feas, max_Dist);
        }
        return dbO;
    }


    @Override
    public int compareTo(IndexObject oThat)
    {
        if (!(oThat instanceof Image)) throw new Error("not compatible");
        Image that = (Image) oThat;
        if (this == that) return 0;
        if (this.m_Feas.length < that.m_Feas.length) return -1;
        else if (this.m_Feas.length > that.m_Feas.length) return 1;
        else
        {
            for (int i = 0; i < m_Feas.length; i++)
            {
                if (m_Feas[i] < that.m_Feas[i]) return -1;
                else if (m_Feas[i] > that.m_Feas[i]) return 1;
            }
            return 0;
        }

    }


    public boolean equals(Object other)
    {
        if (other == this) return true;
        if (!(other instanceof Image)) return false;
        Image image = (Image) other;
        if (this.m_Feas.length != image.m_Feas.length) return false;
        for (int i = 0; i < this.m_Feas.length; i++)
            if (Math.abs(m_Feas[i] - image.m_Feas[i]) > 1.0e-10)
            {
                return false;
            }
        return true;
    }


    public int hashCode()
    {
        int result = 17;
        for (int i = 0; i < m_Feas.length; i++)
        {
            result = 37 * result + Float.floatToIntBits(m_Feas[i]);
        }
        return result;
    }


    public String toString()
    {
        StringBuffer result = new StringBuffer("ImageKeyObject, length :");
        result.append(m_Feas.length).append(", offset : ");
        for (int i = 0; i < m_Feas.length; i++)
             result.append(m_Feas[i]).append(", ");
        return result.toString();
    }


    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeInt(m_Feas.length);
        for (int i = 0; i < m_Feas.length; ++i)
             out.writeFloat(m_Feas[i]);
        out.writeInt(max_Dist.length);
        for (int i = 0; i < max_Dist.length; i++)
        {
            out.writeDouble(max_Dist[i]);
        }
        out.writeObject(table.getTableManagerName());
        out.writeInt(table.getTableLocation());
    }


    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        this.m_Feas = new float[in.readInt()];
        for (int i = 0; i < m_Feas.length; ++i)
             m_Feas[i] = in.readFloat();
        this.max_Dist = new double[in.readInt()];
        for (int i = 0; i < max_Dist.length; i++)
        {
            max_Dist[i] = in.readDouble();
        }
        String indexPrefix = (String) in.readObject();
        table = TableManager.getTableManager(indexPrefix).getTable(in.readInt());
    }
}
