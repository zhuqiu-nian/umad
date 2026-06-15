package tpslp.partition;

import db.type.IndexObject;
import metric.Metric;
import tpslp.coordinate.CoordinateMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LinearSlabPartitionLearner implements PartitionLearner
{
    private static final long serialVersionUID = 1L;

    private final List<LinearBoundary> boundaries;
    private final int partitionsPerBoundary;

    public LinearSlabPartitionLearner(List<LinearBoundary> boundaries, int partitionsPerBoundary)
    {
        if (boundaries == null || boundaries.isEmpty())
        {
            throw new IllegalArgumentException("boundaries must not be empty");
        }
        if (partitionsPerBoundary < 2)
        {
            throw new IllegalArgumentException("partitionsPerBoundary must be at least 2");
        }
        this.boundaries = List.copyOf(boundaries);
        this.partitionsPerBoundary = partitionsPerBoundary;
    }

    @Override
    public PartitionPlan learn(Metric metric, IndexObject[] pivots, CoordinateMap coordinateMap,
                               List<? extends IndexObject> data)
    {
        int dimension = coordinateMap.dimension(pivots.length);
        for (LinearBoundary boundary : boundaries)
        {
            if (boundary.dimension() != dimension)
            {
                throw new IllegalArgumentException("boundary dimension does not match coordinate map");
            }
        }

        double[][] coordinates = new double[data.size()][];
        double[][] scores = new double[boundaries.size()][data.size()];
        for (int i = 0; i < data.size(); i++)
        {
            coordinates[i] = coordinateMap.mapPoint(metric, pivots, data.get(i));
            for (int j = 0; j < boundaries.size(); j++)
            {
                scores[j][i] = boundaries.get(j).score(coordinates[i]);
            }
        }

        double[][] thresholds = new double[boundaries.size()][];
        for (int i = 0; i < boundaries.size(); i++)
        {
            thresholds[i] = quantileThresholds(scores[i], partitionsPerBoundary);
        }

        Map<String, MutableChild> children = new LinkedHashMap<>();
        for (int i = 0; i < data.size(); i++)
        {
            int[] buckets = new int[boundaries.size()];
            for (int j = 0; j < boundaries.size(); j++)
            {
                buckets[j] = bucketOf(scores[j][i], thresholds[j]);
            }
            String key = Arrays.toString(buckets);
            MutableChild child = children.computeIfAbsent(key,
                    ignored -> new MutableChild(regionFor(buckets, thresholds)));
            child.data.add(data.get(i));
        }

        List<PartitionPlan.ChildPartition> partitions = new ArrayList<>();
        for (MutableChild child : children.values())
        {
            partitions.add(new PartitionPlan.ChildPartition(child.data, child.region));
        }
        return new PartitionPlan(partitions);
    }

    private double[] quantileThresholds(double[] values, int partitionCount)
    {
        double[] thresholds = new double[partitionCount - 1];
        if (values.length == 0)
        {
            return thresholds;
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        for (int i = 1; i < partitionCount; i++)
        {
            int index = (int) Math.round(((double) i / (double) partitionCount)
                    * (sorted.length - 1));
            thresholds[i - 1] = sorted[index];
        }
        return thresholds;
    }

    private int bucketOf(double score, double[] thresholds)
    {
        for (int i = 0; i < thresholds.length; i++)
        {
            if (score <= thresholds[i])
            {
                return i;
            }
        }
        return thresholds.length;
    }

    private LinearSlabRegion regionFor(int[] buckets, double[][] thresholds)
    {
        List<LinearSlab> slabs = new ArrayList<>();
        for (int i = 0; i < buckets.length; i++)
        {
            int bucket = buckets[i];
            double low = bucket == 0 ? Double.NEGATIVE_INFINITY : thresholds[i][bucket - 1];
            double high = bucket == thresholds[i].length ? Double.POSITIVE_INFINITY : thresholds[i][bucket];
            slabs.add(new LinearSlab(boundaries.get(i), low, high));
        }
        return new LinearSlabRegion(slabs);
    }

    private static final class MutableChild
    {
        private final List<IndexObject> data = new ArrayList<>();
        private final NodeRegion region;

        private MutableChild(NodeRegion region)
        {
            this.region = region;
        }
    }
}
