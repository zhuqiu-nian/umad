package index.structure;

import db.type.IndexObject;

import java.util.List;

/**
 * Apollonian Tree 的划分结果类
 * 存储三分划分的left、mid、right三个分区，以及每个分区到c1/c2的M1/M2值
 */
public class ApollonianPartitionResults extends PartitionResults
{
    private double M1;  // left 分区中数据到 c1 的最大距离
    private double M2;  // right 分区中数据到 c2 的最大距离
    private double c1_ratio;  // 左分区比率阈值
    private double c2_ratio;  // 右分区比率阈值

    /**
     * Apollonian 划分结果构造函数
     *
     * @param subDataList 数据的划分结果 [left, mid, right]
     * @param pivotSet    支撑点集合 [c1, c2]
     * @param c1_ratio    左分区比率阈值
     * @param c2_ratio    右分区比率阈值
     * @param M1          left 分区到 c1 的最大距离
     * @param M2          right 分区到 c2 的最大距离
     */
    public ApollonianPartitionResults(List<List<? extends IndexObject>> subDataList,
                                       IndexObject[] pivotSet,
                                       double c1_ratio, double c2_ratio,
                                       double M1, double M2)
    {
        super(subDataList, pivotSet);
        this.c1_ratio = c1_ratio;
        this.c2_ratio = c2_ratio;
        this.M1 = M1;
        this.M2 = M2;
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
     * 获取 left 分区数据
     */
    public List<? extends IndexObject> getLeftPartition()
    {
        return getPartitionOf(0);
    }

    /**
     * 获取 mid 分区数据
     */
    public List<? extends IndexObject> getMidPartition()
    {
        return getPartitionOf(1);
    }

    /**
     * 获取 right 分区数据
     */
    public List<? extends IndexObject> getRightPartition()
    {
        return getPartitionOf(2);
    }

    /**
     * 创建 Apollonian 内部节点实例
     *
     * @param pivotSet     支撑点集合
     * @param childAddress 孩子节点地址数组 [left, mid, right]
     * @return Apollonian 内部节点实例
     */
    @Override
    public InternalNode getInstanceOfInternalNode(IndexObject[] pivotSet, long[] childAddress)
    {
        return new ApollonianInternalNode(pivotSet, getDataSize(), childAddress,
                                           c1_ratio, c2_ratio, M1, M2);
    }
}