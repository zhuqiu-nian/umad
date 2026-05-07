package index.structure;

import db.type.IndexObject;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Apollonian Tree 的内部节点
 * 使用两个 pivot (c1, c2) 和两个比率 (c1_ratio, c2_ratio) 进行三分划分
 * left: ratio < c1_ratio
 * mid: c1_ratio <= ratio <= c2_ratio
 * right: ratio > c2_ratio
 */
public class ApollonianInternalNode extends InternalNode
{
    private static final long serialVersionUID = 1234567890123456789L;

    private double c1_ratio;  // 左分区比率阈值
    private double c2_ratio;  // 右分区比率阈值
    private double M1;        // left 分区中数据到 c1 的最大距离
    private double M2;        // right 分区中数据到 c2 的最大距离

    public ApollonianInternalNode()
    {
        super();
    }

    /**
     * 构造函数
     *
     * @param pivots    支撑点集合 [c1, c2]
     * @param size      数据大小
     * @param childAddresses 孩子节点地址 [left, mid, right]
     * @param c1_ratio  左分区比率阈值
     * @param c2_ratio  右分区比率阈值
     * @param M1        left 分区到 c1 的最大距离
     * @param M2        right 分区到 c2 的最大距离
     */
    public ApollonianInternalNode(IndexObject[] pivots, int size, long[] childAddresses,
                                   double c1_ratio, double c2_ratio, double M1, double M2)
    {
        super(pivots, size, childAddresses);
        this.c1_ratio = c1_ratio;
        this.c2_ratio = c2_ratio;
        this.M1 = M1;
        this.M2 = M2;
    }

    /**
     * 获取 c1（第一个支撑点）
     */
    public IndexObject getC1()
    {
        return pivotSet[0];
    }

    /**
     * 获取 c2（第二个支撑点）
     */
    public IndexObject getC2()
    {
        return pivotSet[1];
    }

    /**
     * 获取 c1_ratio
     */
    public double getC1Ratio()
    {
        return c1_ratio;
    }

    /**
     * 获取 c2_ratio
     */
    public double getC2Ratio()
    {
        return c2_ratio;
    }

    /**
     * 获取 M1
     */
    public double getM1()
    {
        return M1;
    }

    /**
     * 获取 M2
     */
    public double getM2()
    {
        return M2;
    }

    /**
     * 获取左孩子地址
     */
    public long getLeftChild()
    {
        return childAddresses[0];
    }

    /**
     * 获取中间孩子地址
     */
    public long getMidChild()
    {
        return childAddresses[1];
    }

    /**
     * 获取右孩子地址
     */
    public long getRightChild()
    {
        return childAddresses[2];
    }

    /**
     * 获取指定孩子的上下界
     * @param childIndex 孩子索引 (0=left, 1=mid, 2=right)
     * @return [下界, 上界] 数组，由于 Apollonian 使用比率，不需要传统上下界
     */
    public double[] getChildRange(int childIndex)
    {
        // Apollonian 不使用传统距离范围，使用比率判断
        // 返回 null 表示使用比率判断
        return null;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeDouble(c1_ratio);
        out.writeDouble(c2_ratio);
        out.writeDouble(M1);
        out.writeDouble(M2);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        c1_ratio = in.readDouble();
        c2_ratio = in.readDouble();
        M1 = in.readDouble();
        M2 = in.readDouble();
    }

    @Override
    public String toString()
    {
        return "ApollonianInternalNode{" +
                "c1=" + (pivotSet.length > 0 ? pivotSet[0] : "null") +
                ", c2=" + (pivotSet.length > 1 ? pivotSet[1] : "null") +
                ", c1_ratio=" + c1_ratio +
                ", c2_ratio=" + c2_ratio +
                ", M1=" + M1 +
                ", M2=" + M2 +
                ", dataSize=" + dataSize +
                '}';
    }
}