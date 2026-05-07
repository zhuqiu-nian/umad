package index.structure;

import db.type.IndexObject;

import java.util.List;
import java.util.Vector;

/**
 * 该类为完全线性划分树CPIndex的结果类，在{@code PartitionResults}类的基础上额外存储了
 * 每个扇出中的数据映射到该内部节点的法向量组的每个法向量上的范围。
 * 每个扇出中的数据中存储划分出该扇出的法向量组 normalVectorsGroup
 * 每个扇出的数据到该内部节点的每个支撑点的距离的最远值 longestDistanceToPivots  为了在范围查询时，可以知道该扇出到每个节点的最远距离，来判断是否可以利用包含关系，直接将该扇出的数据作为结果返回
 * 例如：{@code lowerRange[i][j]}代表了第i个扇出中的数据到第j个支撑点的距离的下界。
 */
public class CPPartitionResults extends PartitionResults
{

    //存储各个扇出映射到当前划分的每个法向量的截距的上下界，目前使用范围有：CPIndex的搜索排除。
    public double[][] lowerRange;
    public double[][] upperRange;

    List<Vector<Double>> normalVectorsGroup;    // 存储法向量组
    double[][] longestDistanceToPivots;         //每个扇出的数据到该内部节点的每个支撑点的距离的最远值
                                                //为了在范围查询时，可以知道该扇出到每个节点的最远距离，来判断是否可以利用包含关系，直接将该扇出的数据作为结果返回
    /**
     * 用于封装VPIndex结果的{@link VPPartitionResults}类的构造函数
     *
     * @param subDataList 数据的划分结果
     * @param pivotSet    采用的支撑点集合
     * @param lowerRange  扇出中数据到支撑点距离的下界
     * @param upperRange  扇出中数据到支撑点距离的上界
     */
    public CPPartitionResults(List<List<? extends IndexObject>> subDataList, IndexObject[] pivotSet, double[][] lowerRange, double[][] upperRange, List<Vector<Double>> vectors, double[][] longestDistanceToPivots)
    {
        super(subDataList, pivotSet);
        this.lowerRange = lowerRange;
        this.upperRange = upperRange;
        this.normalVectorsGroup = vectors;
        this.longestDistanceToPivots = longestDistanceToPivots;
    }

    /**
     * 使用该方法获取一个VP索引树类型的内部节点
     *
     * @param pivotSet     支撑点集合
     * @param childAddress 内部节点中的孩子节点指针数组。
     * @return 返回VP索引内部节点实例
     */
    @Override
    public CPInternalNode getInstanceOfInternalNode(IndexObject[] pivotSet, long[] childAddress)
    {
        return new CPInternalNode(pivotSet, getDataSize(), childAddress, this.lowerRange, this.upperRange, normalVectorsGroup, longestDistanceToPivots);
    }
}
