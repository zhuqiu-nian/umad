package index.structure;

import algorithms.datapartition.GNATPartitionMethods;
import algorithms.datapartition.PartitionMethod;
import algorithms.pivotselection.PivotSelectionMethod;
import algorithms.pivotselection.PivotSelectionMethods;

import db.type.IndexObject;
import index.search.*;
import index.type.HierarchicalPivotSelectionMode;
import metric.Metric;

import java.util.ArrayList;
import java.util.List;

public class GNATIndex extends AbstractIndex
{
    private static final long serialVersionUID = 6706995587267568299L;
    private final PivotSelectionMethod pivotSelectionMethod;
    private final PartitionMethod      partitionMethod;

    /**
     * 默认使用“LOCAL”模式建立GNAT-tree。
     *
     * @param indexPrefix          输出的索引文件的名称
     * @param data                 用于构建索引的数据
     * @param metric               构建索引使用的距离函数
     * @param maxLeafSize          叶子节点的最大容量
     * @param numPivot             支撑点数目
     * @param pivotSelectionMethod 支撑点选择方法
     * @param partitionMethod       划分方法
     */
    public GNATIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric, int maxLeafSize, int numPivot,
                     PivotSelectionMethod pivotSelectionMethod, PartitionMethod partitionMethod)
    {
        super(indexPrefix, data, metric, maxLeafSize, numPivot, numPivot);
        this.numPartitions = numPivot;
        this.pivotSelectionMethod = pivotSelectionMethod;
        this.partitionMethod      = partitionMethod;
    }

    /**
     * 使用指定模式构建GNAT-tree。默认 specifyPivots 为 null
     *
     * @param indexPrefix                    输出的索引文件的名称
     * @param data                           用于构建索引的数据
     * @param metric                         构建索引使用的距离函数
     * @param maxLeafSize                    叶子节点的最大容量
     * @param numPivot                       支撑点个数
     * @param hierarchicalPivotSelectionMode 构建索引的模式，可选值有[LOCAL, GLOBAL, MIX]
     * @param pivotSelectionMethod           支撑点选择方法
     * @param partitionMethod                划分方法
     */
    public GNATIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric, int maxLeafSize, int numPivot, HierarchicalPivotSelectionMode hierarchicalPivotSelectionMode, PivotSelectionMethod pivotSelectionMethod, PartitionMethod partitionMethod)
    {
        super(indexPrefix, data, metric, maxLeafSize, numPivot, numPivot, hierarchicalPivotSelectionMode, null);
        this.numPartitions = numPivot;
        this.pivotSelectionMethod = pivotSelectionMethod;
        this.partitionMethod      = partitionMethod;
    }

    /**
     * 使用指定模式构建GNAT-tree。
     *
     * @param indexPrefix                    输出的索引文件的名称
     * @param data                           用于构建索引的数据
     * @param metric                         构建索引使用的距离函数
     * @param maxLeafSize                    叶子节点的最大容量
     * @param numPivot                       支撑点数目
     * @param hierarchicalPivotSelectionMode 构建索引的模式，可选值有[LOCAL, GLOBAL, MIX]
     * @param pivotSelectionMethod           支撑点选择方法
     * @param partitionMethod                划分方法
     * @param specifyPivots                  在global模式下 使用固定支撑点  不需使用时置为null
     */
    public GNATIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric, int maxLeafSize, int numPivot, HierarchicalPivotSelectionMode hierarchicalPivotSelectionMode, PivotSelectionMethod pivotSelectionMethod, PartitionMethod partitionMethod, IndexObject[] specifyPivots)
    {
        super(indexPrefix, data, metric, maxLeafSize, numPivot, numPivot, hierarchicalPivotSelectionMode, specifyPivots);
        this.numPartitions = numPivot;
        this.pivotSelectionMethod = pivotSelectionMethod;
        this.partitionMethod      = partitionMethod;
    }

    /**
     * 抽象的支撑点选择方法,子类应通过实现该方法为建树过程的划分提供支撑点集合,
     * 该方法最终返回int型数组，数组存储从支撑点候选集中选择的支撑点下标。
     *
     * <p>
     * 该方法由AbstractIndex的子类实现。
     * 目前，支撑点选择的模式有三种：LOCAL、GLOBAL和MIX。
     * 在建树过程中，支撑点候选集{@param candidateSet}和支撑点评价集{@param evaluationSet}的差异在于：
     * 1.当使用“LOCAL”模式选择支撑点时，候选集和评价集均为当前划分中的数据；
     * 2.当使用“GLOBAL”模式选择支撑点时，候选集和评价集均为全体数据；
     * 3.当使用“MIX”模式选择支撑点时，候选集{@param candidateSet}为全体数据的抽样，评价集{@param evaluationSet}为当前划分的数据，
     * 候选集由{@link AbstractIndex}的该类的pivotCandidateSet属性提供。
     * </p>
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
        return pivotSelectionMethod.selectPivots(metric, candidateSet, numPivot);
    }

    /**
     * 构建索引树时使用的划分数据方法。子类需要实现该方法，向{@link AbstractIndex#buildTree()}操作提供数据划分方式。
     *
     * <p>
     * 可以使用系统提供的划分方法{@link PartitionMethod}的//todo 补全
     * </p>
     *
     * @param metric        划分采用的距离函数{@link Metric}
     * @param pivotSet      划分使用的支撑点集合
     * @param data          要划分的数据
     * @param numPartitions 要划分的块数
     * @return 划分结果
     * @see PartitionResults
     */
    @Override
    PartitionResults partition(Metric metric, IndexObject[] pivotSet, List<? extends IndexObject> data, int numPartitions)
    {
        if (!(partitionMethod instanceof GNATPartitionMethods)) throw new IllegalArgumentException("划分方法和索引树不匹配，此处需要GNATPartitionMethods");
        return partitionMethod.partition(metric, pivotSet, data, numPartitions, maxLeafSize);
    }

    /**
     * 在索引中查找指定的{@link Query}，在该方法中新建索引类型相应的{@link Cursor}。
     *
     * @param q 搜索对象
     * @return 返回搜索结果
     * @see Cursor
     */
    @Override
    public Cursor search(Query q)
    {
        if (q instanceof RangeQuery) return new GNATRangeCursor((RangeQuery) q, oiom, metric, root);
        else if (q instanceof KNNQuery)
            throw new UnsupportedOperationException("Unsupported KNNQuery db " + q.getClass());
        else throw new UnsupportedOperationException("Unsupported query db " + q.getClass());
    }

    /**
     * 以字符串的形式返回当GNATIndex的信息
     *
     * @return 当GNATIndex的信息
     */
    @Override
    public String toString()
    {
        StringBuilder toString = new StringBuilder("GNATIndex{" + "hierarchicalPivotSelectionMode=" + hierarchicalPivotSelectionMode + ", root=" + root + ", totalSize=" + totalSize + ", maxLeafSize=" + maxLeafSize + ", metric=" + metric + ", indexPrefix='" + indexPrefix + '\'' + ", numPivot=" + numPivot + ", numPartitions=" + numPartitions);
        if (hierarchicalPivotSelectionMode == HierarchicalPivotSelectionMode.MIX)
        {
            toString.append(", pivotCandidateSetSize=" + pivotCandidateSetSize);
        }
        return toString.append("pivotSelectionMethod=" + pivotSelectionMethod + ", partitionMethod=" + partitionMethod + '}').toString();
    }

    /**
     * 生成支撑点候选集。
     *
     * <p>
     * 在{@link HierarchicalPivotSelectionMode}选择为MIX模式的时候，需要重载实现该方法。通过该方法从传入的data中抽样
     * pivotCandidateSetSize({@link AbstractIndex#setPivotCandidateSetSize(int)}数目的支撑点候选集合。在调用
     * {@link AbstractIndex#buildTree()}的时候，会通过该方法获取支撑点候选集，进行后续的建树操作。
     * </p>
     *
     * @param metric                距离函数的实例。
     * @param data                  被抽样的数据集
     * @param pivotCandidateSetSize 需要的支撑点候选集的大小，默认大小为//todo 选择的最优
     * @return 支撑点候选集合
     */
    @Override
    List<? extends IndexObject> selectPivotCandicateSet(Metric metric, List<? extends IndexObject> data, int pivotCandidateSetSize) {
        int[]            pivots;  //选择的支撑点的下标
        List<IndexObject>   pivotSet = new ArrayList<>();  //将下标转成相应的支撑点对象
        PivotSelectionMethod psm = PivotSelectionMethods.FFT;
        pivots = psm.selectPivots(metric, data, pivotCandidateSetSize);
        for (int i = 0; i < pivots.length; i++)pivotSet.add(data.get(pivots[i]));
        return pivotSet;
    }

    /**
     * 返回一个对应索引树类型的Cursor
     *
     * @return 对应索引树类型的Cursor
     */
    @Override
    public Cursor getCursor()
    {
        return new GNATRangeCursor(this.oiom, this.metric, this.root);
    }
}
