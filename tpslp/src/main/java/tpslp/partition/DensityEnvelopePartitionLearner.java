package tpslp.partition;

import db.type.IndexObject;
import metric.Metric;
import tpslp.coordinate.CoordinateMap;
import tpslp.geometry.Interval;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Separates dense components in transformed pivot space and represents each
 * component by a conservative MBR or PCA-slab envelope. Non-clustered points are
 * kept in a background child.
 */
public final class DensityEnvelopePartitionLearner implements PartitionLearner
{
    private static final long serialVersionUID = 1L;

    public enum Envelope
    {
        MBR,
        PCA_SLAB
    }

    private static final int NOISE = -1;
    private static final int UNVISITED = Integer.MIN_VALUE;

    private final int minPts;
    private final double epsilon;
    private final Envelope envelope;
    private final double marginFraction;
    private final int minimumClusterSize;
    private final double automaticEpsilonQuantile;

    public DensityEnvelopePartitionLearner()
    {
        this(5, -1.0, Envelope.PCA_SLAB, 0.02, 8, 0.35);
    }

    public DensityEnvelopePartitionLearner(int minPts, double epsilon, Envelope envelope,
                                           double marginFraction, int minimumClusterSize)
    {
        this(minPts, epsilon, envelope, marginFraction, minimumClusterSize, 0.35);
    }

    public DensityEnvelopePartitionLearner(int minPts, double epsilon, Envelope envelope,
                                           double marginFraction, int minimumClusterSize,
                                           double automaticEpsilonQuantile)
    {
        if (minPts < 2)
        {
            throw new IllegalArgumentException("minPts must be at least 2");
        }
        if (envelope == null)
        {
            throw new IllegalArgumentException("envelope must not be null");
        }
        if (marginFraction < 0.0)
        {
            throw new IllegalArgumentException("marginFraction must be non-negative");
        }
        if (automaticEpsilonQuantile <= 0.0 || automaticEpsilonQuantile >= 1.0)
        {
            throw new IllegalArgumentException("automaticEpsilonQuantile must be in (0,1)");
        }
        this.minPts = minPts;
        this.epsilon = epsilon;
        this.envelope = envelope;
        this.marginFraction = marginFraction;
        this.minimumClusterSize = Math.max(2, minimumClusterSize);
        this.automaticEpsilonQuantile = automaticEpsilonQuantile;
    }

    @Override
    public PartitionPlan learn(Metric metric, IndexObject[] pivots, CoordinateMap coordinateMap,
                               List<? extends IndexObject> data)
    {
        double[][] coordinates = LinearLearningSupport.coordinates(metric, pivots, coordinateMap, data);
        if (coordinates.length < minimumClusterSize * 2)
        {
            return fallback(metric, pivots, coordinateMap, data);
        }

        int[] labels = dbscan(coordinates, epsilon > 0.0 ? epsilon : automaticEpsilon(coordinates));
        Map<Integer, List<Integer>> clusters = new HashMap<>();
        List<Integer> background = new ArrayList<>();
        for (int i = 0; i < labels.length; i++)
        {
            if (labels[i] == NOISE)
            {
                background.add(i);
            }
            else
            {
                clusters.computeIfAbsent(labels[i], ignored -> new ArrayList<>()).add(i);
            }
        }

        List<PartitionPlan.ChildPartition> children = new ArrayList<>();
        for (List<Integer> cluster : clusters.values())
        {
            if (cluster.size() < minimumClusterSize)
            {
                background.addAll(cluster);
                continue;
            }
            children.add(new PartitionPlan.ChildPartition(objects(data, cluster),
                    envelopeRegion(coordinates, cluster)));
        }

        if (!background.isEmpty())
        {
            children.add(new PartitionPlan.ChildPartition(objects(data, background),
                    mbrRegion(coordinates, background)));
        }
        if (children.size() <= 1)
        {
            return fallback(metric, pivots, coordinateMap, data);
        }
        return new PartitionPlan(children);
    }

    private PartitionPlan fallback(Metric metric, IndexObject[] pivots,
                                   CoordinateMap coordinateMap,
                                   List<? extends IndexObject> data)
    {
        return new DensityValleyPartitionLearner(0.1).learn(metric, pivots, coordinateMap, data);
    }

    private NodeRegion envelopeRegion(double[][] coordinates, List<Integer> cluster)
    {
        if (envelope == Envelope.MBR)
        {
            return mbrRegion(coordinates, cluster);
        }
        return pcaSlabRegion(coordinates, cluster);
    }

    private NodeRegion pcaSlabRegion(double[][] coordinates, List<Integer> cluster)
    {
        double[][] subset = subset(coordinates, cluster);
        int dimension = coordinates[0].length;
        List<LinearSlab> slabs = new ArrayList<>();

        double[] first = LinearLearningSupport.normalizeOrFallback(
                LinearLearningSupport.firstPrincipalDirection(subset), dimension);
        addProjectionSlab(slabs, subset, first);

        for (int i = 0; i < dimension; i++)
        {
            double[] axis = new double[dimension];
            axis[i] = 1.0;
            addProjectionSlab(slabs, subset, axis);
        }
        return new LinearSlabRegion(slabs);
    }

    private void addProjectionSlab(List<LinearSlab> slabs, double[][] points, double[] direction)
    {
        LinearBoundary boundary = new LinearBoundary(direction);
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (double[] point : points)
        {
            double score = boundary.score(point);
            low = Math.min(low, score);
            high = Math.max(high, score);
        }
        double margin = Math.max(1e-9, (high - low) * marginFraction);
        slabs.add(new LinearSlab(boundary, low - margin, high + margin));
    }

    private MbrRegion mbrRegion(double[][] coordinates, List<Integer> indices)
    {
        int dimension = coordinates[0].length;
        double[] low = new double[dimension];
        double[] high = new double[dimension];
        for (int i = 0; i < dimension; i++)
        {
            low[i] = Double.POSITIVE_INFINITY;
            high[i] = Double.NEGATIVE_INFINITY;
        }
        for (int index : indices)
        {
            for (int i = 0; i < dimension; i++)
            {
                low[i] = Math.min(low[i], coordinates[index][i]);
                high[i] = Math.max(high[i], coordinates[index][i]);
            }
        }
        List<Interval> intervals = new ArrayList<>();
        for (int i = 0; i < dimension; i++)
        {
            double margin = Math.max(1e-9, (high[i] - low[i]) * marginFraction);
            intervals.add(new Interval(low[i] - margin, high[i] + margin));
        }
        return new MbrRegion(intervals);
    }

    private int[] dbscan(double[][] coordinates, double eps)
    {
        int[] labels = new int[coordinates.length];
        for (int i = 0; i < labels.length; i++)
        {
            labels[i] = UNVISITED;
        }
        int clusterId = 0;
        for (int i = 0; i < coordinates.length; i++)
        {
            if (labels[i] != UNVISITED)
            {
                continue;
            }
            List<Integer> neighbors = neighbors(coordinates, i, eps);
            if (neighbors.size() < minPts)
            {
                labels[i] = NOISE;
                continue;
            }
            expandCluster(coordinates, labels, i, neighbors, clusterId, eps);
            clusterId++;
        }
        return labels;
    }

    private void expandCluster(double[][] coordinates, int[] labels, int seed,
                               List<Integer> seedNeighbors, int clusterId, double eps)
    {
        labels[seed] = clusterId;
        ArrayDeque<Integer> queue = new ArrayDeque<>(seedNeighbors);
        while (!queue.isEmpty())
        {
            int point = queue.removeFirst();
            if (labels[point] == NOISE)
            {
                labels[point] = clusterId;
            }
            if (labels[point] != UNVISITED)
            {
                continue;
            }
            labels[point] = clusterId;
            List<Integer> neighbors = neighbors(coordinates, point, eps);
            if (neighbors.size() >= minPts)
            {
                queue.addAll(neighbors);
            }
        }
    }

    private List<Integer> neighbors(double[][] coordinates, int point, double eps)
    {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i++)
        {
            if (LinearLearningSupport.distance(coordinates[point], coordinates[i]) <= eps)
            {
                result.add(i);
            }
        }
        return result;
    }

    private double automaticEpsilon(double[][] coordinates)
    {
        double[] kthDistances = new double[coordinates.length];
        for (int i = 0; i < coordinates.length; i++)
        {
            double[] distances = new double[coordinates.length - 1];
            int offset = 0;
            for (int j = 0; j < coordinates.length; j++)
            {
                if (i != j)
                {
                    distances[offset++] = LinearLearningSupport.distance(coordinates[i], coordinates[j]);
                }
            }
            java.util.Arrays.sort(distances);
            int index = Math.min(Math.max(0, minPts - 2), distances.length - 1);
            kthDistances[i] = distances[index];
        }
        java.util.Arrays.sort(kthDistances);
        int index = (int) Math.round(automaticEpsilonQuantile * (kthDistances.length - 1));
        index = Math.max(0, Math.min(kthDistances.length - 1, index));
        return kthDistances[index];
    }

    private List<IndexObject> objects(List<? extends IndexObject> data, List<Integer> indices)
    {
        List<IndexObject> objects = new ArrayList<>();
        for (int index : indices)
        {
            objects.add(data.get(index));
        }
        return objects;
    }

    private double[][] subset(double[][] coordinates, List<Integer> indices)
    {
        double[][] subset = new double[indices.size()][];
        for (int i = 0; i < indices.size(); i++)
        {
            subset[i] = coordinates[indices.get(i)];
        }
        return subset;
    }
}
