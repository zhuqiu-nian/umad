package tpslp.index;

import algorithms.pivotselection.PivotSelectionMethod;
import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import metric.Metric;
import tpslp.coordinate.CoordinateMap;
import tpslp.geometry.Interval;
import tpslp.partition.PartitionLearner;
import tpslp.partition.PartitionPlan;
import tpslp.prune.IntersectionPruner;
import tpslp.prune.QueryBoxPruner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class TpslpIndex implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final Metric metric;
    private final CoordinateMap coordinateMap;
    private final PartitionLearner partitionLearner;
    private final IntersectionPruner pruner;
    private final IndexObject[] specifiedRootPivots;
    private final PivotSelectionMethod pivotSelectionMethod;
    private final int numPivots;
    private final int maxLeafSize;
    private final TpslpNode root;

    private TpslpIndex(Builder builder)
    {
        this.metric = builder.metric;
        this.coordinateMap = builder.coordinateMap;
        this.partitionLearner = builder.partitionLearner;
        this.pruner = builder.pruner;
        this.specifiedRootPivots = builder.pivots == null ? null : builder.pivots.clone();
        this.pivotSelectionMethod = builder.pivotSelectionMethod;
        this.numPivots = builder.resolvePivotCount();
        this.maxLeafSize = builder.maxLeafSize;
        this.root = build(new ArrayList<>(builder.data), true);
    }

    public static Builder builder(List<? extends IndexObject> data, Metric metric)
    {
        return new Builder(data, metric);
    }

    public List<DoubleIndexObjectPair> rangeSearch(IndexObject query, double radius)
    {
        if (radius < 0.0)
        {
            throw new IllegalArgumentException("radius must be non-negative");
        }
        List<DoubleIndexObjectPair> results = new ArrayList<>();
        search(root, query, radius, results);
        return results;
    }

    public TpslpSearchStats rangeSearchWithStats(IndexObject query, double radius)
    {
        if (radius < 0.0)
        {
            throw new IllegalArgumentException("radius must be non-negative");
        }
        TpslpSearchStats stats = new TpslpSearchStats();
        search(root, query, radius, stats);
        return stats;
    }

    public IndexObject[] getPivots()
    {
        return root.getPivots();
    }

    public TpslpNode getRoot()
    {
        return root;
    }

    private TpslpNode build(List<IndexObject> data, boolean rootNode)
    {
        if (data.size() <= Math.max(maxLeafSize, numPivots))
        {
            return TpslpNode.leaf(data);
        }

        IndexObject[] localPivots = resolvePivotsForNode(data, rootNode);
        PartitionPlan plan = partitionLearner.learn(metric, localPivots, coordinateMap, data);
        if (plan.getChildren().size() <= 1)
        {
            return TpslpNode.leaf(data);
        }

        List<TpslpNode> children = new ArrayList<>();
        List<tpslp.partition.NodeRegion> regions = new ArrayList<>();
        for (PartitionPlan.ChildPartition child : plan.getChildren())
        {
            children.add(build(child.getData(), false));
            regions.add(child.getRegion());
        }
        return TpslpNode.internal(localPivots, children, regions, data.size());
    }

    private IndexObject[] resolvePivotsForNode(List<IndexObject> data, boolean rootNode)
    {
        if (rootNode && specifiedRootPivots != null)
        {
            return specifiedRootPivots.clone();
        }
        if (pivotSelectionMethod != null)
        {
            int[] selected = pivotSelectionMethod.selectPivots(metric, data, numPivots);
            IndexObject[] resolved = new IndexObject[selected.length];
            for (int i = 0; i < selected.length; i++)
            {
                resolved[i] = data.get(selected[i]);
            }
            return resolved;
        }
        IndexObject[] resolved = new IndexObject[numPivots];
        for (int i = 0; i < numPivots; i++)
        {
            resolved[i] = data.get(i);
        }
        return resolved;
    }

    private void search(TpslpNode node, IndexObject query, double radius,
                        List<DoubleIndexObjectPair> results)
    {
        if (node.isLeaf())
        {
            for (IndexObject point : node.getLeafData())
            {
                double distance = metric.getDistance(query, point);
                if (distance <= radius)
                {
                    results.add(new DoubleIndexObjectPair(distance, point));
                }
            }
            return;
        }

        Interval[] queryBox = coordinateMap.mapQuery(metric, node.getPivots(), query, radius);
        for (int i = 0; i < node.getChildren().size(); i++)
        {
            if (pruner.shouldVisit(queryBox, node.getChildRegions().get(i)))
            {
                search(node.getChildren().get(i), query, radius, results);
            }
        }
    }

    private void search(TpslpNode node, IndexObject query, double radius,
                        TpslpSearchStats stats)
    {
        if (node.isLeaf())
        {
            stats.leafNodeVisited();
            for (IndexObject point : node.getLeafData())
            {
                double distance = metric.getDistance(query, point);
                stats.distanceComputed();
                if (distance <= radius)
                {
                    stats.addResult(new DoubleIndexObjectPair(distance, point));
                }
            }
            return;
        }

        stats.internalNodeVisited();
        IndexObject[] pivots = node.getPivots();
        Interval[] queryBox = coordinateMap.mapQuery(metric, pivots, query, radius);
        stats.queryPivotDistancesComputed(pivots.length);
        for (int i = 0; i < node.getChildren().size(); i++)
        {
            if (pruner.shouldVisit(queryBox, node.getChildRegions().get(i)))
            {
                search(node.getChildren().get(i), query, radius, stats);
            }
        }
    }

    public static final class Builder
    {
        private final List<? extends IndexObject> data;
        private final Metric metric;
        private CoordinateMap coordinateMap;
        private PartitionLearner partitionLearner;
        private IntersectionPruner pruner = new QueryBoxPruner();
        private IndexObject[] pivots;
        private PivotSelectionMethod pivotSelectionMethod;
        private int numPivots;
        private int maxLeafSize = 32;

        private Builder(List<? extends IndexObject> data, Metric metric)
        {
            if (data == null || data.isEmpty())
            {
                throw new IllegalArgumentException("data must not be empty");
            }
            if (metric == null)
            {
                throw new IllegalArgumentException("metric must not be null");
            }
            this.data = data;
            this.metric = metric;
        }

        public Builder coordinateMap(CoordinateMap coordinateMap)
        {
            this.coordinateMap = coordinateMap;
            return this;
        }

        public Builder partitionLearner(PartitionLearner partitionLearner)
        {
            this.partitionLearner = partitionLearner;
            return this;
        }

        public Builder pruner(IntersectionPruner pruner)
        {
            this.pruner = pruner;
            return this;
        }

        public Builder pivots(IndexObject... pivots)
        {
            this.pivots = pivots == null ? null : pivots.clone();
            return this;
        }

        public Builder pivotSelection(PivotSelectionMethod method, int numPivots)
        {
            this.pivotSelectionMethod = method;
            this.numPivots = numPivots;
            return this;
        }

        public Builder maxLeafSize(int maxLeafSize)
        {
            this.maxLeafSize = maxLeafSize;
            return this;
        }

        public TpslpIndex build()
        {
            if (coordinateMap == null)
            {
                throw new IllegalStateException("coordinateMap is required");
            }
            if (partitionLearner == null)
            {
                throw new IllegalStateException("partitionLearner is required");
            }
            if (pruner == null)
            {
                throw new IllegalStateException("pruner is required");
            }
            if (maxLeafSize < 1)
            {
                throw new IllegalStateException("maxLeafSize must be positive");
            }
            return new TpslpIndex(this);
        }

        private IndexObject[] resolvePivots()
        {
            if (pivots != null)
            {
                return pivots.clone();
            }
            if (pivotSelectionMethod == null || numPivots <= 0)
            {
                throw new IllegalStateException("pivots or pivotSelection must be provided");
            }
            int[] selected = pivotSelectionMethod.selectPivots(metric, data, numPivots);
            IndexObject[] resolved = new IndexObject[selected.length];
            for (int i = 0; i < selected.length; i++)
            {
                resolved[i] = data.get(selected[i]);
            }
            return resolved;
        }

        private int resolvePivotCount()
        {
            if (numPivots > 0)
            {
                return numPivots;
            }
            if (pivots != null && pivots.length > 0)
            {
                return pivots.length;
            }
            throw new IllegalStateException("pivots or pivotSelection must be provided");
        }
    }
}
