package index.structure;

import algorithms.datapartition.PartitionMethod;
import algorithms.pivotselection.PivotSelectionMethod;
import db.type.IndexObject;
import index.search.Cursor;
import index.search.KNNQuery;
import index.search.LogDistanceRangeCursor;
import index.search.Query;
import index.search.RangeQuery;
import index.type.HierarchicalPivotSelectionMode;
import metric.Metric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogDistanceIndex extends AbstractIndex
{
    private static final long serialVersionUID = 9208240423051548580L;

    private final PivotSelectionMethod pivotSelectionMethod;
    private final PartitionMethod partitionMethod;

    public LogDistanceIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric,
                            int maxLeafSize, PivotSelectionMethod pivotSelectionMethod,
                            PartitionMethod partitionMethod)
    {
        this(indexPrefix, data, metric, maxLeafSize, HierarchicalPivotSelectionMode.LOCAL,
                pivotSelectionMethod, partitionMethod, null);
    }

    public LogDistanceIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric,
                            int maxLeafSize, HierarchicalPivotSelectionMode mode,
                            PivotSelectionMethod pivotSelectionMethod,
                            PartitionMethod partitionMethod,
                            IndexObject[] specifyPivots)
    {
        super(indexPrefix, data, metric, maxLeafSize, 2, 2, mode, specifyPivots);
        this.pivotSelectionMethod = pivotSelectionMethod;
        this.partitionMethod = partitionMethod;
    }

    @Override
    public void buildTree()
    {
        if (this.numPivot != 2 || this.numPartitions != 2)
        {
            throw new IllegalArgumentException("LogDistanceIndex requires 2 pivots and 2 partitions");
        }
        super.buildTree();
    }

    @Override
    int[] pivotSelection(Metric metric, List<? extends IndexObject> candidateSet,
                         List<? extends IndexObject> evaluationSet, int numPivot)
    {
        if (pivotSelectionMethod == null)
        {
            throw new IllegalStateException("pivotSelectionMethod is required unless fixed global pivots are supplied");
        }
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
        long[] childAddress = new long[partition.getNumPartition()];
        for (int i = 0; i < partition.getNumPartition(); i++)
        {
            List<IndexObject> partitionData = copyPartition(partition, i);
            childAddress[i] = buildLocalChild(pivots, partitionData, numPartitions);
        }
        InternalNode node = partition.getInstanceOfInternalNode(pivots, childAddress);
        return writeInternalNode(node);
    }

    private long buildLocalChild(IndexObject[] parentPivots, List<IndexObject> partitionData,
                                 int numPartitions) throws IOException
    {
        if (partitionData.size() <= this.maxLeafSize || partitionData.size() <= this.numPivot)
        {
            return createAndWriteLeafNode(parentPivots, partitionData);
        }

        IndexObject[] childPivots = selectPivotSet(partitionData);
        partitionData.removeAll(Arrays.asList(childPivots));
        this.allPivotSet.addAll(Arrays.asList(childPivots));

        if (partitionData.size() <= this.maxLeafSize || partitionData.size() <= this.numPivot)
        {
            return createAndWriteLeafNode(childPivots, partitionData);
        }

        PartitionResults childPartition = partition(metric, childPivots, partitionData, numPartitions);
        return localBulkLoad(childPartition, childPivots, numPartitions);
    }

    private List<IndexObject> copyPartition(PartitionResults partition, int partitionIndex)
    {
        List<IndexObject> copy = new ArrayList<>();
        copy.addAll(partition.getPartitionOf(partitionIndex));
        return copy;
    }

    private IndexObject[] selectPivotSet(List<? extends IndexObject> data)
    {
        int[] pivotIndexes = pivotSelection(metric, data, data, this.numPivot);
        if (pivotIndexes.length == 0)
        {
            throw new IllegalStateException("pivot selection returned no pivots");
        }
        IndexObject[] pivotSet = new IndexObject[pivotIndexes.length];
        for (int i = 0; i < pivotSet.length; i++)
        {
            pivotSet[i] = data.get(pivotIndexes[i]);
        }
        return pivotSet;
    }

    @Override
    public Cursor search(Query q)
    {
        if (q instanceof RangeQuery)
        {
            return new LogDistanceRangeCursor((RangeQuery) q, oiom, metric, root);
        }
        else if (q instanceof KNNQuery)
        {
            throw new UnsupportedOperationException("LogDistanceIndex does not support KNNQuery yet");
        }
        else
        {
            throw new UnsupportedOperationException("Unsupported query db " + q.getClass());
        }
    }

    @Override
    public Cursor getCursor()
    {
        return new LogDistanceRangeCursor(this.oiom, this.metric, this.root);
    }
}
