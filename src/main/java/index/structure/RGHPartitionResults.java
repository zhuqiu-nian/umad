package index.structure;

import db.type.IndexObject;

import java.util.List;

/**
 * RGH-Tree 的划分结果
 * 使用两个 pivot 进行二分划分，包含子节点的覆盖半径信息
 */
public class RGHPartitionResults extends PartitionResults
{
    private static final long serialVersionUID = 9008240423051548572L;

    private final double splitRatio;     // 分割比率 R
    private final double leftMaxDist;  // Left 分区到 Pl 的最大距离 (dl)
    private final double rightMaxDist; // Right 分区到 Pr 的最大距离 (dr)

    // 新增: 子节点自己的覆盖半径
    private final double leftChildRadiusL;  // 左子节点自己的left半径到其Pl
    private final double leftChildRadiusR;  // 左子节点自己的right半径到其Pr
    private final double rightChildRadiusL; // 右子节点自己的left半径到其Pl
    private final double rightChildRadiusR; // 右子节点自己的right半径到其Pr

    /**
     * 构造函数 (完整版本)
     *
     * @param subDataList 子数据列表 [left, right]
     * @param pivots 支撑点 [Pl, Pr]
     * @param splitRatio 分割比率
     * @param leftMaxDist 左分区最大距离 (dl)
     * @param rightMaxDist 右分区最大距离 (dr)
     * @param leftChildRadiusL 左子节点left半径
     * @param leftChildRadiusR 左子节点right半径
     * @param rightChildRadiusL 右子节点left半径
     * @param rightChildRadiusR 右子节点right半径
     */
    public RGHPartitionResults(List<List<? extends IndexObject>> subDataList,
                               IndexObject[] pivots, double splitRatio,
                               double leftMaxDist, double rightMaxDist,
                               double leftChildRadiusL, double leftChildRadiusR,
                               double rightChildRadiusL, double rightChildRadiusR)
    {
        super(subDataList, pivots);
        this.splitRatio = splitRatio;
        this.leftMaxDist = leftMaxDist;
        this.rightMaxDist = rightMaxDist;
        this.leftChildRadiusL = leftChildRadiusL;
        this.leftChildRadiusR = leftChildRadiusR;
        this.rightChildRadiusL = rightChildRadiusL;
        this.rightChildRadiusR = rightChildRadiusR;
    }

    /**
     * 简化构造函数 (兼容旧代码)
     * 默认使用父节点的半径作为子节点半径
     */
    public RGHPartitionResults(List<List<? extends IndexObject>> subDataList,
                               IndexObject[] pivots, double splitRatio,
                               double leftMaxDist, double rightMaxDist)
    {
        this(subDataList, pivots, splitRatio, leftMaxDist, rightMaxDist,
             leftMaxDist, leftMaxDist, rightMaxDist, rightMaxDist);
    }

    /**
     * 获取分割比率
     */
    public double getSplitRatio()
    {
        return splitRatio;
    }

    /**
     * 获取左分区最大距离
     */
    public double getLeftMaxDist()
    {
        return leftMaxDist;
    }

    /**
     * 获取右分区最大距离
     */
    public double getRightMaxDist()
    {
        return rightMaxDist;
    }

    /**
     * 获取左子节点left半径
     */
    public double getLeftChildRadiusL()
    {
        return leftChildRadiusL;
    }

    /**
     * 获取左子节点right半径
     */
    public double getLeftChildRadiusR()
    {
        return leftChildRadiusR;
    }

    /**
     * 获取右子节点left半径
     */
    public double getRightChildRadiusL()
    {
        return rightChildRadiusL;
    }

    /**
     * 获取右子节点right半径
     */
    public double getRightChildRadiusR()
    {
        return rightChildRadiusR;
    }

    /**
     * ��取左分区数据
     */
    public List<? extends IndexObject> getLeftData()
    {
        return listOfPartitions.get(0);
    }

    /**
     * 获取右分区数据
     */
    public List<? extends IndexObject> getRightData()
    {
        return listOfPartitions.get(1);
    }

    /**
     * 创建内部节点实例
     */
    @Override
    public InternalNode getInstanceOfInternalNode(IndexObject[] pivotSet, long[] childAddress)
    {
        // 构建childDistances数组 (这里暂时设为-1，因为partition阶段还没有加载子节点)
        double[] childDistances = new double[]{-1, -1, -1, -1};

        return new RGHInternalNode(pivotSet, getDataSize(), childAddress,
                splitRatio, leftMaxDist, rightMaxDist, childDistances,
                new double[]{leftChildRadiusL, leftChildRadiusR},
                new double[]{rightChildRadiusL, rightChildRadiusR});
    }

    @Override
    public String toString()
    {
        return "RGHPartitionResults{" +
                "splitRatio=" + splitRatio +
                ", dl=" + leftMaxDist +
                ", dr=" + rightMaxDist +
                ", leftChildRadiusL=" + leftChildRadiusL +
                ", leftChildRadiusR=" + leftChildRadiusR +
                ", rightChildRadiusL=" + rightChildRadiusL +
                ", rightChildRadiusR=" + rightChildRadiusR +
                ", leftSize=" + (listOfPartitions.size() > 0 ? listOfPartitions.get(0).size() : 0) +
                ", rightSize=" + (listOfPartitions.size() > 1 ? listOfPartitions.get(1).size() : 0) +
                '}';
    }
}