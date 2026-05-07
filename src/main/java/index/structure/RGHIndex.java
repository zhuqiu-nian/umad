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
        // 如果数据量小于等于叶子容量，创建叶子节点
        if (partition.getDataSize() <= this.maxLeafSize)
        {
            // 获取所有分区的数据来创建叶子节点
            List<IndexObject> allData = new java.util.ArrayList<>();
            for (int i = 0; i < partition.getNumPartition(); i++)
            {
                allData.addAll(partition.getPartitionOf(i));
            }
            return createAndWriteLeafNode(pivots, allData);
        }

        // 获取划分后的数据（left和right）
        List<? extends IndexObject> leftData = partition.getPartitionOf(0);
        List<? extends IndexObject> rightData = partition.getPartitionOf(1);

        // ===== 步骤1：先递归构建左右子树 =====
        // 为左子树选择pivots并递归构建
        int[] leftPivotsIdx = pivotSelection(metric, leftData, leftData, this.numPivot);
        IndexObject[] leftPivots = new IndexObject[leftPivotsIdx.length];
        for (int i = 0; i < leftPivots.length; i++)
        {
            leftPivots[i] = leftData.get(leftPivotsIdx[i]);
        }
        // 从数据集中删除被选择的pivots
        leftData.removeAll(Arrays.asList(leftPivots));

        // 为右子树选择pivots并递归构建
        int[] rightPivotsIdx = pivotSelection(metric, rightData, rightData, this.numPivot);
        IndexObject[] rightPivots = new IndexObject[rightPivotsIdx.length];
        for (int i = 0; i < rightPivots.length; i++)
        {
            rightPivots[i] = rightData.get(rightPivotsIdx[i]);
        }
        rightData.removeAll(Arrays.asList(rightPivots));

        // 递归构建左子树（如果数据量大）
        long leftChildAddress;
        if (leftData.size() > this.maxLeafSize)
        {
            PartitionResults leftPartition = partition(metric, leftPivots, leftData, numPartitions);
            leftChildAddress = localBulkLoad(leftPartition, leftPivots, numPartitions);
        }
        else
        {
            leftChildAddress = createAndWriteLeafNode(leftPivots, leftData);
        }

        // 递归构建右子树（如果数据量大）
        long rightChildAddress;
        if (rightData.size() > this.maxLeafSize)
        {
            PartitionResults rightPartition = partition(metric, rightPivots, rightData, numPartitions);
            rightChildAddress = localBulkLoad(rightPartition, rightPivots, numPartitions);
        }
        else
        {
            rightChildAddress = createAndWriteLeafNode(rightPivots, rightData);
        }

        // ===== 步骤2：读取子节点对象，获取子节点的pivots =====
        // 需要读取已写入的子节点来获取其pivots，以便计算childDistances
        Node leftChildNode = null;
        Node rightChildNode = null;
        try {
            leftChildNode = (Node) oiom.readObject(leftChildAddress);
            rightChildNode = (Node) oiom.readObject(rightChildAddress);
        } catch (Exception e) {
            throw new IOException("Failed to read child nodes: " + e.getMessage(), e);
        }

        IndexObject leftChildLeftPivot = null;
        IndexObject leftChildRightPivot = null;
        IndexObject rightChildLeftPivot = null;
        IndexObject rightChildRightPivot = null;

        if (leftChildNode instanceof RGHInternalNode)
        {
            RGHInternalNode leftInt = (RGHInternalNode) leftChildNode;
            leftChildLeftPivot = leftInt.getLeftPivot();
            leftChildRightPivot = leftInt.getRightPivot();
        }
        else if (leftChildNode instanceof LeafNode)
        {
            LeafNode leftLeaf = (LeafNode) leftChildNode;
            // 叶子节点的pivots
            leftChildLeftPivot = leftLeaf.getPivotOf(0);
            leftChildRightPivot = leftLeaf.getPivotOf(1);
        }

        if (rightChildNode instanceof RGHInternalNode)
        {
            RGHInternalNode rightInt = (RGHInternalNode) rightChildNode;
            rightChildLeftPivot = rightInt.getLeftPivot();
            rightChildRightPivot = rightInt.getRightPivot();
        }
        else if (rightChildNode instanceof LeafNode)
        {
            LeafNode rightLeaf = (LeafNode) rightChildNode;
            rightChildLeftPivot = rightLeaf.getPivotOf(0);
            rightChildRightPivot = rightLeaf.getPivotOf(1);
        }

        // ===== 步骤3：计算覆盖半径 dl 和 dr =====
        // dl: 当前pivot(Pl)到左子集中所有数据点的最大距离
        // dr: 当前pivot(Pr)到右子集中所有数据点的最大距离
        double dl = 0.0;
        for (IndexObject obj : leftData)
        {
            double d = metric.getDistance(pivots[0], obj);
            if (d > dl) dl = d;
        }
        double dr = 0.0;
        for (IndexObject obj : rightData)
        {
            double d = metric.getDistance(pivots[1], obj);
            if (d > dr) dr = d;
        }

        // ===== 步骤4：计算childDistances[4] =====
        // childDistances[0] = d(Pl, V.left.Pl) - 当前左Pivot到左子节点的左Pivot
        // childDistances[1] = d(Pl, V.left.Pr) - 当前左Pivot到左子节点的右Pivot
        // childDistances[2] = d(Pr, V.right.Pl) - 当前右Pivot到右子节点的左Pivot
        // childDistances[3] = d(Pr, V.right.Pr) - 当前右Pivot到右子节点的右Pivot
        double[] childDistances = new double[4];

        // 如果子节点是内部节点或有有效的pivots，则计算距离
        if (leftChildLeftPivot != null && leftChildRightPivot != null)
        {
            childDistances[0] = metric.getDistance(pivots[0], leftChildLeftPivot);
            childDistances[1] = metric.getDistance(pivots[0], leftChildRightPivot);
        }
        else
        {
            childDistances[0] = -1.0;
            childDistances[1] = -1.0;
        }

        if (rightChildLeftPivot != null && rightChildRightPivot != null)
        {
            childDistances[2] = metric.getDistance(pivots[1], rightChildLeftPivot);
            childDistances[3] = metric.getDistance(pivots[1], rightChildRightPivot);
        }
        else
        {
            childDistances[2] = -1.0;
            childDistances[3] = -1.0;
        }

        // ===== 步骤5：创建当前内部节点 =====
        // 直接使用简化构造函数
        // dl = 左子集到当前Pl的最大距离
        // dr = 右子集到当前Pr的最大距离

        double splitRatio = ((RGHPartitionResults) partition).getSplitRatio();

        long[] childAddresses = new long[]{leftChildAddress, rightChildAddress};

        RGHInternalNode node = new RGHInternalNode(pivots, partition.getDataSize(), childAddresses,
                splitRatio, dl, dr, childDistances);

        return writeInternalNode(node);
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