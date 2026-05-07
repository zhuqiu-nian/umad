package index.structure;

import algorithms.datapartition.PartitionMethod;
import algorithms.pivotselection.PivotSelectionMethod;
import db.type.IndexObject;
import index.search.Cursor;
import index.search.PCTRangeCursor;
import index.search.Query;
import index.search.RangeQuery;
import index.type.HierarchicalPivotSelectionMode;
import metric.Metric;

import java.util.List;

/**
 * PCT(Pivot Space Cluster Tree)索引树的核心类，该类实现一个一个PCT索引。
 *
 * <p>PCT索引主要是用于数据有明显聚类关系的相似性搜索问题。该索引和VP等传统的度量空间索引区别主要在于划分部分。传统的度量空间索引的划分大多数都
 * 没有考虑到数据的分布问题，所以可能会导致原本应该划分到一个数据块的数据划分到了不同的分支，这样相似性搜索的时候就不能较准确的剪枝。而剪枝操作的
 * 多少直接关系到了我们搜索性能的好坏(一般剪枝越多，距离计算次数越少)。
 * <p>为了解决这一问题，我们提出了PCT(K-Means Partition)划分。该划分先将数据映射到支撑点空间之中，然后对数据进行K-Means聚类，将划分边界划分
 * 到聚类边界上，也就是划分边界落到了数据较为稀疏的地方。然后再对每一个聚类块进行相同的操作，这样递归的划分下去，直到每个块的数据都可以装到一个叶子
 * 节点中为止。
 * <p>
 *     经过实验验证，PCT索引在均匀数据集上和MVPT性能相当，在真实数据集上，PCT性能略微由于MVPT。而且PCT完全解耦了支撑点个数和划分块数的关系。
 *     一般情况下我们认为支撑点的个数应该和数据的本征维度相关，而划分块数应该和数据的类别数目相关，PCT完全解耦了两者，这样面对低维多类别数目时
 *     的数据时，PCT索引树使用起来更加契合。
 * </p>
 * <p>PCT树的搜索操作请参考{@link PCTRangeCursor}。
 *
 * @see PCTRangeCursor
 * @author liulinfeng 2021/4/4
 */
public class PCTIndex extends AbstractIndex
{
    private static final long                 serialVersionUID = -8399251275793642486L;
    //支撑点选择方法
    private              PivotSelectionMethod pivotSelectionMethod;
    //划分方法
    private              PartitionMethod      partitionMethod;

    /**
     * 使用局部选点方法构建索引。
     *
     * @param indexPrefix          输出的索引文件的前缀名
     * @param data                 用于构建索引的数据
     * @param metric               构建索引使用的距离函数
     * @param maxLeafSize          叶子节点的最大容量
     * @param numPivot             支撑点数目
     * @param numPartitions        每层的划分块数
     * @param pivotSelectionMethod 支撑点选择方法
     * @param partitionMethod      数据划分方法
     */
    public PCTIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric, int maxLeafSize, int numPivot,
                    int numPartitions, PivotSelectionMethod pivotSelectionMethod, PartitionMethod partitionMethod)
    {
        super(indexPrefix, data, metric, maxLeafSize, numPivot, numPartitions);
        this.pivotSelectionMethod = pivotSelectionMethod;
        this.partitionMethod = partitionMethod;
    }

    /**
     * 使用指定模式构建索引。构建索引时的K-Means的最大迭代此处maxIter=500，聚类中心的最小移动距离是10^-4.
     *
     * @param indexPrefix                    输出的索引文件的前缀名
     * @param data                           用于构建索引的数据
     * @param metric                         构建索引使用的距离函数
     * @param maxLeafSize                    叶子节点的最大容量
     * @param numPivot                       支撑点数目
     * @param numPartitions                  每层的划分块数
     * @param hierarchicalPivotSelectionMode 构建索引的模式，可选值有[LOCAL, GLOBAL, MIX]
     * @param pivotSelectionMethod           支撑点选择方法
     * @param partitionMethod                数据划分方法
     * @param specifyPivots                  使用GLOBAL建树时 传入该参数 直接使用传入的数据作为支撑点
     */
    public PCTIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric, int maxLeafSize, int numPivot,
                    int numPartitions, HierarchicalPivotSelectionMode hierarchicalPivotSelectionMode,
                    PivotSelectionMethod pivotSelectionMethod, PartitionMethod partitionMethod, IndexObject[] specifyPivots)
    {
        super(indexPrefix, data, metric, maxLeafSize, numPivot, numPartitions, hierarchicalPivotSelectionMode, specifyPivots);
        this.pivotSelectionMethod = pivotSelectionMethod;
        this.partitionMethod = partitionMethod;
    }

    /**
     * 在索引中查找指定的{@link Query}。
     *
     * @param q 搜索对象
     * @see Cursor
     */
    @Override
    public Cursor search(Query q)
    {
        if (q instanceof RangeQuery)
            return new PCTRangeCursor((RangeQuery) q, oiom, metric, root);
        else
            throw new UnsupportedOperationException("Unsupported query db " + q.getClass());
    }


    /**
     * 支撑点选择方法,该方法为KMP建树过程的划分提供支撑点集合,
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
        return pivotSelectionMethod.selectPivots(metric, candidateSet, numPivot);
    }

    /**
     * KMP的索引划分采用的是将数据集映射到支撑点空间之后运行K-Means聚类算法，该聚类算法的距离函数必须是L2距离。
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
        return partitionMethod.partition(metric, pivotSet, data, numPartitions, this.maxLeafSize);
    }

    /**
     * 返回一个对应索引树类型的Cursor
     *
     * @return 对应索引树类型的Cursor
     */
    @Override
    public Cursor getCursor()
    {
        return new PCTRangeCursor(this.oiom, this.metric, this.root);
    }
}
