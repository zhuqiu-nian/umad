package index.structure;

import db.type.IndexObject;

import java.util.List;

/**
 * 该类为VPIndex的结果类，在{@code PartitionResults}类的基础上额外存储了
 * 每个扇出中的数据到该内部节点的每个支撑点的距离的上下界。
 * 例如：{@code lowerRange[i][j]}代表了第i个扇出中的数据到第j个支撑点的距离的下界。
 */
public class VPPartitionResults extends PartitionResults
{

    //存储各个扇出到当前划分的每个支撑点的距离的上下界，目前使用范围有：VPIndex的搜索排除。
    public double[][] lowerRange;
    public double[][] upperRange;

    /**
     * 用于封装VPIndex结果的{@link VPPartitionResults}类的构造函数
     *
     * @param subDataList 数据的划分结果
     * @param pivotSet    采用的支撑点集合
     * @param lowerRange  扇出中数据到支撑点距离的下界
     * @param upperRange  扇出中数据到支撑点距离的上界
     */
    public VPPartitionResults(List<List<? extends IndexObject>> subDataList, IndexObject[] pivotSet, double[][] lowerRange, double[][] upperRange)
    {
        super(subDataList, pivotSet);
        this.lowerRange = lowerRange;
        this.upperRange = upperRange;
    }

    /**
     * 使用该方法获取一个VP索引树类型的内部节点
     *
     * @param pivotSet     支撑点集合
     * @param childAddress 内部节点中的孩子节点指针数组。
     * @return 返回VP索引内部节点实例
     */
    @Override
    public InternalNode getInstanceOfInternalNode(IndexObject[] pivotSet, long[] childAddress)
    {
        return new VPInternalNode(pivotSet, getDataSize(), childAddress, this.lowerRange, this.upperRange);
    }
}
