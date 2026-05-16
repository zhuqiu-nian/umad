package index.structure;

import algorithms.datapartition.PartitionMethod;
import algorithms.pivotselection.PivotSelectionMethod;
import db.type.IndexObject;
import index.search.Cursor;
import index.search.IATRangeCursor;
import index.search.Query;
import index.search.RangeQuery;
import index.type.HierarchicalPivotSelectionMode;
import metric.Metric;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * IAT (Improved Apollonian Tree).
 */
public class IATIndex extends AbstractIndex
{
    private static final long serialVersionUID = 8108240423051548570L;

    private final PivotSelectionMethod pivotSelectionMethod;
    private final PartitionMethod partitionMethod;

    public IATIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric,
                    int maxLeafSize, int numPivot, int numPartitions,
                    PivotSelectionMethod pivotSelectionMethod, PartitionMethod partitionMethod)
    {
        super(indexPrefix, data, metric, maxLeafSize, numPivot, numPartitions);
        this.pivotSelectionMethod = pivotSelectionMethod;
        this.partitionMethod = partitionMethod;
    }

    public IATIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric,
                    int maxLeafSize, int numPivot, int numPartitions,
                    HierarchicalPivotSelectionMode hierarchicalPivotSelectionMode,
                    PivotSelectionMethod pivotSelectionMethod, PartitionMethod partitionMethod)
    {
        super(indexPrefix, data, metric, maxLeafSize, numPivot, numPartitions,
                hierarchicalPivotSelectionMode, null);
        this.pivotSelectionMethod = pivotSelectionMethod;
        this.partitionMethod = partitionMethod;
    }

    @Override
    public void buildTree()
    {
        if (this.numPivot != 2)
        {
            throw new IllegalArgumentException("IAT requires exactly 2 pivots, but got " + this.numPivot);
        }
        if (this.numPartitions != 3)
        {
            throw new IllegalArgumentException("IAT requires exactly 3 partitions, but got " + this.numPartitions);
        }
        super.buildTree();
    }

    @Override
    int[] pivotSelection(Metric metric, List<? extends IndexObject> candidateSet,
                         List<? extends IndexObject> evaluationSet, int numPivot)
    {
        return pivotSelectionMethod.selectPivots(metric, candidateSet, numPivot);
    }

    @Override
    PartitionResults partition(Metric metric, IndexObject[] pivotSet,
                               List<? extends IndexObject> data, int numPartitions)
    {
        return partitionMethod.partition(metric, pivotSet, data, numPartitions, maxLeafSize);
    }

    @Override
    protected long localBulkLoad(PartitionResults partition, IndexObject[] pivots, int numPartitions) throws IOException
    {
        long[] childAddresses = new long[partition.getNumPartition()];
        double[][][] childPivotDistances = new double[partition.getNumPartition()][2][2];
        double[][] childSubtreeRadii = new double[partition.getNumPartition()][2];

        for (int i = 0; i < partition.getNumPartition(); i++)
        {
            fill(childPivotDistances[i], -1.0);
            Arrays.fill(childSubtreeRadii[i], -1.0);

            List<? extends IndexObject> partitionData = partition.getPartitionOf(i);
            int[] newPivots = pivotSelection(metric, partitionData, partitionData, this.numPivot);
            IndexObject[] newPivotsSet = new IndexObject[newPivots.length];
            for (int j = 0; j < newPivotsSet.length; j++)
            {
                newPivotsSet[j] = partitionData.get(newPivots[j]);
            }
            partitionData.removeAll(Arrays.asList(newPivotsSet));
            this.allPivotSet.addAll(Arrays.asList(newPivotsSet));

            long childAddress;
            if (partitionData.size() > this.maxLeafSize)
            {
                PartitionResults partitionResults = partition(metric, newPivotsSet, partitionData, numPartitions);
                childAddress = localBulkLoad(partitionResults, newPivotsSet, numPartitions);
            }
            else
            {
                childAddress = createAndWriteLeafNode(newPivotsSet, partitionData);
            }

            childAddresses[i] = childAddress;
            try
            {
                Node childNode = (Node) oiom.readObject(childAddress);
                populateChildMetadata(pivots, childNode, childPivotDistances[i], childSubtreeRadii[i]);
            }
            catch (Exception e)
            {
                throw new IOException("Failed to read IAT child node", e);
            }
        }

        if (partition instanceof IATPartitionResults)
        {
            InternalNode node = ((IATPartitionResults) partition).getInstanceOfInternalNode(
                    pivots, childAddresses, childPivotDistances, childSubtreeRadii);
            return writeInternalNode(node);
        }

        return writeInternalNode(partition.getInstanceOfInternalNode(pivots, childAddresses));
    }

    private void populateChildMetadata(IndexObject[] parentPivots, Node childNode,
                                       double[][] distances, double[] radii)
    {
        int childPivotCount = Math.min(2, childNode.getNumPivots());
        for (int parentIndex = 0; parentIndex < 2; parentIndex++)
        {
            for (int childPivotIndex = 0; childPivotIndex < childPivotCount; childPivotIndex++)
            {
                distances[parentIndex][childPivotIndex] =
                        metric.getDistance(parentPivots[parentIndex], childNode.getPivotOf(childPivotIndex));
            }
        }

        double[] subtreeRadii = computeSubtreeRadii(childNode);
        for (int i = 0; i < Math.min(2, subtreeRadii.length); i++)
        {
            radii[i] = subtreeRadii[i];
        }
        if (childNode.getNumPivots() >= 2)
        {
            double pivotDistance = metric.getDistance(childNode.getPivotOf(0), childNode.getPivotOf(1));
            radii[0] = Math.max(radii[0], pivotDistance);
            radii[1] = Math.max(radii[1], pivotDistance);
        }
    }

    private double[] computeSubtreeRadii(Node childNode)
    {
        double[] radii = new double[2];
        if (childNode instanceof IATInternalNode)
        {
            return ((IATInternalNode) childNode).getSubtreeCoverRadii();
        }
        if (childNode instanceof PivotTable)
        {
            PivotTable pivotTable = (PivotTable) childNode;
            double[][] distanceTable = pivotTable.getDistanceTable();
            for (double[] row : distanceTable)
            {
                for (int i = 0; i < Math.min(2, row.length); i++)
                {
                    radii[i] = Math.max(radii[i], row[i]);
                }
            }
        }
        return radii;
    }

    private void fill(double[][] values, double value)
    {
        for (double[] row : values)
        {
            Arrays.fill(row, value);
        }
    }

    @Override
    public Cursor search(Query q)
    {
        if (q instanceof RangeQuery)
        {
            return new IATRangeCursor((RangeQuery) q, oiom, metric, root);
        }
        throw new UnsupportedOperationException("IATIndex only supports RangeQuery, but got: " + q.getClass());
    }

    @Override
    public Cursor getCursor()
    {
        return new IATRangeCursor(this.oiom, this.metric, this.root);
    }

    @Override
    public String toString()
    {
        return "IATIndex{" +
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
