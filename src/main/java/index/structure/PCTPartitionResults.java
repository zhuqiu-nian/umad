package index.structure;

import db.type.IndexObject;

import java.util.List;

/**
 * PCT的划分结果类，该类对比通用的划分结果PartitionResult类需要多存储每一块聚类结果的聚类中心和半径。
 * @author liulinfeng 2021/4/4
 */
public class PCTPartitionResults extends PartitionResults
{
    private double[][] centroids;
    //保存该块内的点的坐标在每个维度上的最大值
    private double[][] maxCoordinateSingleDim;
    //保存该块内点的坐标在每个维度上的最小值
    private double[][] minCoordinateSingleDim;
    private double[] radius;


    /**
     * KMP(K-Means Partition)的划分结果类的构造函数
     *
     * @param subDataList 数据的划分结果
     * @param pivotSet    采用的支撑点集合
     * @param centroids   每一块的聚类中心
     * @param maxCoordinateSingleDim 该块内的点的坐标在每个维度的最大值
     * @param minCoordinateSingleDim 该块内的点在每个维度的最小值
     */
    public PCTPartitionResults(List<List<? extends IndexObject>> subDataList, IndexObject[] pivotSet, double[][] centroids,
                               double[][] maxCoordinateSingleDim, double[][] minCoordinateSingleDim, double[] radius)
    {
        super(subDataList, pivotSet);
        this.centroids = centroids;
        this.maxCoordinateSingleDim = maxCoordinateSingleDim;
        this.minCoordinateSingleDim = minCoordinateSingleDim;
        this.radius = radius;
    }

    /**
     * 使用该方法获取一个KMP树的内部节点。
     *
     * @param pivotSet     支撑点集合
     * @param childAddress 内部节点中的孩子节点指针数组。
     * @return 对应索引树类型的内部节点实例
     */
    @Override
    public InternalNode getInstanceOfInternalNode(IndexObject[] pivotSet, long[] childAddress)
    {
        return new PCTInternalNode(pivotSet, this.getDataSize(), childAddress, centroids, maxCoordinateSingleDim,
                minCoordinateSingleDim, radius);
    }
}
