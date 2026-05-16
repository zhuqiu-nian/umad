package index.structure;

import algorithms.datapartition.PartitionMethod;
import algorithms.pivotselection.PivotSelectionMethod;
import db.type.IndexObject;
import index.search.RGHRangeCursor;
import index.search.Cursor;
import index.search.Query;
import index.search.RangeQuery;
import index.type.HierarchicalPivotSelectionMode;
import metric.Metric;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * RGH-Tree 索引实现
 * 使用两个 pivot (Pl, Pr) 和单一比率进行二分划分
 * left: ratio < splitRatio
 * right: ratio >= splitRatio
 *
 * 构建顺序（严格遵循C++版本）：
 * 1. 先递归构建左右子树
 * 2. 计算覆盖半径 dl, dr（当前pivots到各子集的最大距离）
 * 3. 计算childDistances（当前pivots到子节点pivots的距离）
 * 4. 创建当前内部节点
 */
public class RGHIndex extends AbstractIndex
{
    private static final long serialVersionUID = 9008240423051548570L;

    /**
     * 使用默认 LOCAL 模式构建 RGH-Tree
     */
    public RGHIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric,
                     int maxLeafSize, int numPivot, int numPartitions,
                     PivotSelectionMethod pivotSelectionMethod, PartitionMethod partitionMethod)
    {
        super(indexPrefix, data, metric, maxLeafSize, numPivot, numPartitions);
        this.pivotSelectionMethod = pivotSelectionMethod;
        this.partitionMethod = partitionMethod;
    }

    /**
     * 使用指定模式构建 RGH-Tree
     */
    public RGHIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric,
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
        // RGH-Tree 固定使用 2 个 pivot 和 2 个分区
        if (this.numPivot != 2)
        {
            throw new IllegalArgumentException("RGH-Tree requires exactly 2 pivots, but got " + this.numPivot);
        }
        if (this.numPartitions != 2)
        {
            throw new IllegalArgumentException("RGH-Tree requires exactly 2 partitions, but got " + this.numPartitions);
        }
        super.buildTree();
    }

    private final PivotSelectionMethod pivotSelectionMethod;
    private final PartitionMethod partitionMethod;

    /**
     * 支撑点选择方法
     */
    @Override
    int[] pivotSelection(Metric metric, List<? extends IndexObject> candidateSet,
                         List<? extends IndexObject> evaluationSet, int numPivot)
    {
        return pivotSelectionMethod.selectPivots(metric, candidateSet, numPivot);
    }

    /**
     * 数据划分方法
     */
    @Override
    PartitionResults partition(Metric metric, IndexObject[] pivotSet,
                                List<? extends IndexObject> data, int numPartitions)
    {
        return partitionMethod.partition(metric, pivotSet, data, numPartitions, maxLeafSize);
    }

    /**
     * RGH专有的bulkLoad方法
     * 严格遵循C++版本的构建顺序：
     * 1. 先递归构建左右子树
     * 2. 计算���盖半径
     * 3. 计算childDistances
     * 4. 创建当前内部节点
     */
    @Override
    protected long localBulkLoad(PartitionResults partition, IndexObject[] pivots, int numPartitions) throws IOException
    {
        if (partition.getDataSize() <= this.maxLeafSize)
        {
            List<IndexObject> allData = new java.util.ArrayList<>();
            for (int i = 0; i < partition.getNumPartition(); i++)
            {
                allData.addAll(partition.getPartitionOf(i));
            }
            return createAndWriteLeafNode(pivots, allData);
        }

        // C++ bulkLoad 会先保留完整 leftData/rightData 用于当前节点元数据，
        // 递归构建子树时才在子调用中选择并移除子 pivot。Java 的接口要求
        // 显式传入子 pivot，因此这里用两份副本：完整副本算半径，构建副本删 pivot。
        List<IndexObject> leftAllData = copyPartition(partition, 0);
        List<IndexObject> rightAllData = copyPartition(partition, 1);

        IndexObject[] leftPivots = selectPivotSet(leftAllData);
        IndexObject[] rightPivots = selectPivotSet(rightAllData);
        this.allPivotSet.addAll(Arrays.asList(leftPivots));
        this.allPivotSet.addAll(Arrays.asList(rightPivots));

        List<IndexObject> leftBuildData = new java.util.ArrayList<>(leftAllData);
        List<IndexObject> rightBuildData = new java.util.ArrayList<>(rightAllData);
        leftBuildData.removeAll(Arrays.asList(leftPivots));
        rightBuildData.removeAll(Arrays.asList(rightPivots));

        long leftChildAddress = buildChildSubtree(leftPivots, leftBuildData, numPartitions);
        long rightChildAddress = buildChildSubtree(rightPivots, rightBuildData, numPartitions);

        Node leftChildNode;
        Node rightChildNode;
        try {
            leftChildNode = (Node) oiom.readObject(leftChildAddress);
            rightChildNode = (Node) oiom.readObject(rightChildAddress);
        } catch (Exception e) {
            throw new IOException("Failed to read child nodes: " + e.getMessage(), e);
        }

        double dl = 0.0;
        for (IndexObject obj : leftAllData)
        {
            double d = metric.getDistance(pivots[0], obj);
            if (d > dl) dl = d;
        }

        double dr = 0.0;
        for (IndexObject obj : rightAllData)
        {
            double d = metric.getDistance(pivots[1], obj);
            if (d > dr) dr = d;
        }

        // 对齐 C++：childDistances 只在子节点是 RGHInternalNode 时有效；
        // 如果子节点是叶子，搜索阶段会保守直接搜索该叶子。
        double[] childDistances = new double[]{-1.0, -1.0, -1.0, -1.0};
        if (leftChildNode instanceof RGHInternalNode)
        {
            RGHInternalNode leftInt = (RGHInternalNode) leftChildNode;
            childDistances[0] = metric.getDistance(pivots[0], leftInt.getLeftPivot());
            childDistances[1] = metric.getDistance(pivots[0], leftInt.getRightPivot());
        }

        if (rightChildNode instanceof RGHInternalNode)
        {
            RGHInternalNode rightInt = (RGHInternalNode) rightChildNode;
            childDistances[2] = metric.getDistance(pivots[1], rightInt.getLeftPivot());
            childDistances[3] = metric.getDistance(pivots[1], rightInt.getRightPivot());
        }

        double splitRatio = ((RGHPartitionResults) partition).getSplitRatio();
        long[] childAddresses = new long[]{leftChildAddress, rightChildAddress};

        RGHInternalNode node = new RGHInternalNode(pivots, partition.getDataSize(), childAddresses,
                splitRatio, dl, dr, childDistances);

        return writeInternalNode(node);
    }

    private List<IndexObject> copyPartition(PartitionResults partition, int partitionIndex)
    {
        List<IndexObject> copy = new java.util.ArrayList<>();
        copy.addAll(partition.getPartitionOf(partitionIndex));
        return copy;
    }

    private IndexObject[] selectPivotSet(List<? extends IndexObject> data)
    {
        int[] pivotIndexes = pivotSelection(metric, data, data, this.numPivot);
        IndexObject[] pivotSet = new IndexObject[pivotIndexes.length];
        for (int i = 0; i < pivotSet.length; i++)
        {
            pivotSet[i] = data.get(pivotIndexes[i]);
        }
        return pivotSet;
    }

    private long buildChildSubtree(IndexObject[] childPivots, List<IndexObject> childData, int numPartitions) throws IOException
    {
        if (childData.size() > this.maxLeafSize)
        {
            PartitionResults childPartition = partition(metric, childPivots, childData, numPartitions);
            return localBulkLoad(childPartition, childPivots, numPartitions);
        }
        return createAndWriteLeafNode(childPivots, childData);
    }

    /**
     * 搜索方法
     */
    @Override
    public Cursor search(Query q)
    {
        if (q instanceof RangeQuery)
        {
            return new RGHRangeCursor((RangeQuery) q, oiom, metric, root);
        }
        else
        {
            throw new UnsupportedOperationException("RGHIndex only supports RangeQuery, but got: " + q.getClass());
        }
    }

    /**
     * 获取 Cursor 实例
     */
    @Override
    public Cursor getCursor()
    {
        return new RGHRangeCursor(this.oiom, this.metric, this.root);
    }

    @Override
    public String toString()
    {
        return "RGHIndex{" +
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
