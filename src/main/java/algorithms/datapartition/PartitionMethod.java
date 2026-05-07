package algorithms.datapartition;

import db.type.IndexObject;
import index.structure.PartitionResults;
import metric.Metric;

import java.util.List;


/**
 * 划分方法的抽象接口
 *
 * @author Willard
 */
public interface PartitionMethod
{
    /**
     * 执行划分操作
     * @param metric 划分使用的距离函数
     * @param pivots 划分使用的支撑点集合
     * @param data 待划分的数据集
     * @param numPartitions 划分的块数
     * @param maxLS 建树的叶子结点的最大大小
     * @return 划分结果
     */
    PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int numPartitions, int maxLS);

    /**
     * 执行划分操作
     * @param metric 划分使用的距离函数
     * @param pivots 划分使用的支撑点集合
     * @param data 待划分的数据集
     * @param first 数据的起始位置
     * @param size 数据大小
     * @param numPartitions 划分的块数
     * @param maxLS 建树的叶子结点的最大大小
     * @return 划分结果
     */
    PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int first, int size, int numPartitions, int maxLS);
}
