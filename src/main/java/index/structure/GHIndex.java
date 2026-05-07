package index.structure;

import algorithms.datapartition.PartitionMethod;
import algorithms.pivotselection.PivotSelectionMethod;
import db.type.IndexObject;
import index.search.*;
import index.type.HierarchicalPivotSelectionMode;
import metric.Metric;

import java.util.List;

/**
 * 该类为最原始的GHT的索引类，划分数目和支撑点数目必须为2。
 */
public class GHIndex extends AbstractIndex
{
    private static final long serialVersionUID = -3957625646309609591L;
    private final PivotSelectionMethod pivotSelectionMethod;
    private final PartitionMethod partitionMethod;

    /**
     * 使用动态方法构建索引。
     * @param indexPrefix   输出的索引文件的前缀名
     * @param data          用于构建索引的数据
     * @param metric        构建索引使用的距离函数
     * @param maxLeafSize   叶子节点的最大容量
     * @param numPivot      支撑点数目
     * @param numPartitions 每层的划分块数
     * @param pivotSelectionMethod 支撑点选择算法
     * @param partitionMethod   划分方法
     */
    public GHIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric, int maxLeafSize, int numPivot,
                   int numPartitions, PivotSelectionMethod pivotSelectionMethod, PartitionMethod partitionMethod)
    {
        super(indexPrefix, data, metric, maxLeafSize, numPivot, numPartitions);
        if(numPartitions != 2 || numPivot !=2)
            throw new RuntimeException("支撑点数目和划分数目必须都为2");
        this.pivotSelectionMethod = pivotSelectionMethod;
        this.partitionMethod = partitionMethod;
    }

    /**
     * 使用指定模式构建索引。
     * @param indexPrefix                    输出的索引文件的前缀名
     * @param data                           用于构建索引的数据
     * @param metric                         构建索引使用的距离函数
     * @param maxLeafSize                    叶子节点的最大容量
     * @param numPivot                       支撑点数目
     * @param numPartitions                  每层的划分块数
     * @param hierarchicalPivotSelectionMode 构建索引的模式，可选值有[LOCAL, GLOBAL, MIX]
     * @param pivotSelectionMethod           支撑点选择方法
     * @param partitionMethod                划分方法
     */
    public GHIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric, int maxLeafSize, int numPivot,
                   int numPartitions, HierarchicalPivotSelectionMode hierarchicalPivotSelectionMode, PivotSelectionMethod pivotSelectionMethod, PartitionMethod partitionMethod)
    {
        super(indexPrefix, data, metric, maxLeafSize, numPivot, numPartitions, hierarchicalPivotSelectionMode, null);
        if(numPartitions != 2 || numPivot !=2)
            throw new RuntimeException("支撑点数目和划分数目必须都为2");
        if(hierarchicalPivotSelectionMode == HierarchicalPivotSelectionMode.GLOBAL)
            throw new RuntimeException("传统GH划分不可以使用固定的支撑点组合");
        this.pivotSelectionMethod = pivotSelectionMethod;
        this.partitionMethod = partitionMethod;
    }

    /**
     * 支撑点选择方法,为建树过程的划分提供支撑点集合,
     * 该方法最终返回int型数组，数组存储从支撑点候选集中选择的支撑点下标。
     *
     * @param metric        选择支撑点使用的距离函数
     * @param candidateSet  支撑点候选集
     * @param evaluationSet 支撑点评价集
     * @param numPivot      支撑点数目
     * @return 返回被选择的支撑点在原数据列表data中的下标
     */
    @Override
    int[] pivotSelection(Metric metric, List<? extends IndexObject> candidateSet, List<? extends IndexObject> evaluationSet, int numPivot)
    {
        if(numPivot != 2)
            throw new RuntimeException("支撑点个数必须为2");
        return pivotSelectionMethod.selectPivots(metric, candidateSet, numPivot);
    }

    /**
     * 构建GH索引树时使用的划分数据方法。
     *
     * @param metric       划分采用的距离函数{@link Metric}
     * @param pivotSet     划分使用的支撑点集合
     * @param data         要划分的数据
     * @param numPartitons 要划分的块数
     * @return 划分结果
     * @see PartitionResults
     */
    @Override
    PartitionResults partition(Metric metric, IndexObject[] pivotSet, List<? extends IndexObject> data, int numPartitons)
    {
        return partitionMethod.partition(metric, pivotSet, data, numPartitions, maxLeafSize);
    }

    /**
     * 用该方法进行GHT的搜索
     *
     * @param q 搜索对象
     * @see Cursor
     */
    @Override
    public Cursor search(Query q)
    {
        if (q instanceof RangeQuery) return new GHRangeCursor((RangeQuery) q, oiom, metric, root);
        else if (q instanceof KNNQuery)
            throw new UnsupportedOperationException("Unsupported KNNQuery db " + q.getClass());
        else throw new UnsupportedOperationException("Unsupported query db " + q.getClass());
    }

    /**
     * 返回一个对应索引树类型的Cursor
     *
     * @return 对应索引树类型的Cursor
     */
    @Override
    public Cursor getCursor()
    {
        return new GHRangeCursor(this.oiom, this.metric, this.root);
    }
}
