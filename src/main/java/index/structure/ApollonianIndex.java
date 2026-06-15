package index.structure;

import algorithms.datapartition.PartitionMethod;
import algorithms.pivotselection.EvaluationPivotSelectionMethod;
import algorithms.pivotselection.PivotSelectionMethod;
import db.type.IndexObject;
import index.search.ApollonianRangeCursor;
import index.search.Cursor;
import index.search.Query;
import index.search.RangeQuery;
import index.type.HierarchicalPivotSelectionMode;
import metric.Metric;

import java.util.ArrayList;
import java.util.List;

/**
 * Apollonian Tree 索引实现
 * 使用两个 pivot (c1, c2) 和比率进行三分划分
 */
public class ApollonianIndex extends AbstractIndex
{
    private static final long serialVersionUID = 8008240423051548570L;

    /**
     * 使用默认 LOCAL 模式构建 Apollonian Tree
     *
     * @param indexPrefix          索引文件前缀
     * @param data                 数据列表
     * @param metric               距离函数
     * @param maxLeafSize          叶子节点最大容量
     * @param numPivot             支撑点数目（必须是2）
     * @param numPartitions        划分块数（必须是3）
     * @param pivotSelectionMethod 支撑点选择方法
     * @param partitionMethod      分区方法
     */
    public ApollonianIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric,
                           int maxLeafSize, int numPivot, int numPartitions,
                           PivotSelectionMethod pivotSelectionMethod, PartitionMethod partitionMethod)
    {
        super(indexPrefix, data, metric, maxLeafSize, numPivot, numPartitions);
        this.pivotSelectionMethod = pivotSelectionMethod;
        this.partitionMethod = partitionMethod;
    }

    /**
     * 使用指定模式构建 Apollonian Tree
     */
    public ApollonianIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric,
                           int maxLeafSize, int numPivot, int numPartitions,
                           HierarchicalPivotSelectionMode hierarchicalPivotSelectionMode,
                           PivotSelectionMethod pivotSelectionMethod, PartitionMethod partitionMethod)
    {
        super(indexPrefix, data, metric, maxLeafSize, numPivot, numPartitions,
              hierarchicalPivotSelectionMode, null);
        this.pivotSelectionMethod = pivotSelectionMethod;
        this.partitionMethod = partitionMethod;
    }

    /**
     * 验证参数是否合法
     */
    @Override
    public void buildTree()
    {
        // Apollonian Tree 固定使用 2 个 pivot 和 3 个分区
        if (this.numPivot != 2)
        {
            throw new IllegalArgumentException("Apollonian Tree requires exactly 2 pivots, but got " + this.numPivot);
        }
        if (this.numPartitions != 3)
        {
            throw new IllegalArgumentException("Apollonian Tree requires exactly 3 partitions, but got " + this.numPartitions);
        }
        super.buildTree();
    }

    private final PivotSelectionMethod pivotSelectionMethod;
    private final PartitionMethod partitionMethod;

    /**
     * 支撑点选择方法
     * Apollonian 使用两个 pivot，选择方法需要返回 2 个支撑点
     */
    @Override
    int[] pivotSelection(Metric metric, List<? extends IndexObject> candidateSet,
                         List<? extends IndexObject> evaluationSet, int numPivot)
    {
        if (pivotSelectionMethod instanceof EvaluationPivotSelectionMethod)
        {
            return ((EvaluationPivotSelectionMethod) pivotSelectionMethod)
                    .selectPivots(metric, candidateSet, evaluationSet, numPivot);
        }
        return pivotSelectionMethod.selectPivots(metric, candidateSet, numPivot);
    }

    /**
     * 数据划分方法
     * Apollonian 使用基于比率的三分划分
     */
    @Override
    PartitionResults partition(Metric metric, IndexObject[] pivotSet,
                               List<? extends IndexObject> data, int numPartitions)
    {
        return partitionMethod.partition(metric, pivotSet, data, numPartitions, maxLeafSize);
    }

    @Override
    List<? extends IndexObject> selectPivotCandicateSet(Metric metric, List<? extends IndexObject> data, int pivotCandidateSetSize)
    {
        if (pivotCandidateSetSize >= data.size())
        {
            return data;
        }

        List<IndexObject> candidates = new ArrayList<>(pivotCandidateSetSize);
        for (int i = 0; i < pivotCandidateSetSize; i++)
        {
            int index = (int) Math.floor(i * (data.size() - 1.0) / (pivotCandidateSetSize - 1.0));
            candidates.add(data.get(index));
        }
        return candidates;
    }

    /**
     * 搜索方法
     * 根据查询类型返回对应的 Cursor
     */
    @Override
    public Cursor search(Query q)
    {
        if (q instanceof RangeQuery)
        {
            return new ApollonianRangeCursor((RangeQuery) q, oiom, metric, root);
        }
        else
        {
            throw new UnsupportedOperationException("ApollonianIndex only supports RangeQuery, but got: " + q.getClass());
        }
    }

    /**
     * 获取 Cursor 实例
     */
    @Override
    public Cursor getCursor()
    {
        return new ApollonianRangeCursor(this.oiom, this.metric, this.root);
    }

    @Override
    public String toString()
    {
        return "ApollonianIndex{" +
                "hierarchicalPivotSelectionMode=" + hierarchicalPivotSelectionMode +
                ", root=" + root +
                ", totalSize=" + totalSize +
                ", maxLeafSize=" + maxLeafSize +
                ", numPivot=" + numPivot +
                ", numPartitions=" + numPartitions +
                ", pivotSelectionMethod=" + pivotSelectionMethod +
                ", partitionMethod=" + partitionMethod +
                '}';
    }
}
