package algorithms.datapartition;

import db.type.IndexObject;
import index.powerdistance.PowerDistanceTransform;
import index.structure.PartitionResults;
import index.structure.PowerDistancePartitionResults;
import metric.Metric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Binary median split by a line in 2D power-distance pivot space.
 */
public class PowerDistanceLinearPartitionMethod implements PartitionMethod, Serializable
{
    private static final long serialVersionUID = 8173411604248587160L;

    private final double rho;
    private final double epsilonDistance;
    private final double w1;
    private final double w2;
    private final double comparisonEpsilon;

    public PowerDistanceLinearPartitionMethod(double rho)
    {
        this(rho, 1.0, -1.0);
    }

    public PowerDistanceLinearPartitionMethod(double rho, double w1, double w2)
    {
        this(rho, w1, w2,
                PowerDistanceTransform.DEFAULT_EPSILON_DISTANCE,
                PowerDistanceTransform.DEFAULT_COMPARISON_EPSILON);
    }

    public PowerDistanceLinearPartitionMethod(double rho, double w1, double w2,
                                              double epsilonDistance,
                                              double comparisonEpsilon)
    {
        if (rho == 0.0)
        {
            throw new IllegalArgumentException("rho must not be zero");
        }
        if (w1 == 0.0 && w2 == 0.0)
        {
            throw new IllegalArgumentException("at least one boundary weight must be non-zero");
        }
        if (!(epsilonDistance > 0.0))
        {
            throw new IllegalArgumentException("epsilonDistance must be positive");
        }
        this.rho = rho;
        this.epsilonDistance = epsilonDistance;
        this.w1 = w1;
        this.w2 = w2;
        this.comparisonEpsilon = comparisonEpsilon;
    }

    public double getRho()
    {
        return rho;
    }

    public double getW1()
    {
        return w1;
    }

    public double getW2()
    {
        return w2;
    }

    @Override
    public PartitionResults partition(Metric metric, IndexObject[] pivots,
                                      List<? extends IndexObject> data,
                                      int numPartitions, int maxLS)
    {
        return partition(metric, pivots, data, 0, data.size(), numPartitions, maxLS);
    }

    @Override
    public PartitionResults partition(Metric metric, IndexObject[] pivots,
                                      List<? extends IndexObject> data,
                                      int first, int size, int numPartitions, int maxLS)
    {
        if (pivots.length != 2)
        {
            throw new IllegalArgumentException("Power-distance split requires exactly 2 pivots");
        }
        if (numPartitions != 2)
        {
            if (numPartitions < 2)
            {
                throw new IllegalArgumentException("Power-distance split requires at least 2 partitions");
            }
        }
        if (size < 0)
        {
            throw new IllegalArgumentException("size must be non-negative");
        }

        PowerDistanceTransform transform = new PowerDistanceTransform(rho, epsilonDistance);
        double[] scores = new double[size];
        for (int i = 0; i < size; i++)
        {
            IndexObject x = data.get(first + i);
            scores[i] = score(transform, metric, pivots, x);
        }

        double[] thresholds = quantileThresholds(scores, numPartitions);
        List<List<IndexObject>> partitions = partitions(data, first, size, scores,
                thresholds, numPartitions);
        if (hasEmptyPartition(partitions))
        {
            List<List<? extends IndexObject>> subDataList = fallbackPartitions(data,
                    first, size, numPartitions);
            return new PowerDistancePartitionResults(subDataList, pivots,
                    rho, epsilonDistance, 0.0, 0.0, new double[0],
                    comparisonEpsilon);
        }

        List<List<? extends IndexObject>> subDataList = new ArrayList<>(numPartitions);
        subDataList.addAll(partitions);
        return new PowerDistancePartitionResults(subDataList, pivots,
                rho, epsilonDistance, w1, w2, thresholds, comparisonEpsilon);
    }

    private double score(PowerDistanceTransform transform, Metric metric,
                         IndexObject[] pivots, IndexObject x)
    {
        double y1 = transform.transformPointDistance(metric.getDistance(x, pivots[0]));
        double y2 = transform.transformPointDistance(metric.getDistance(x, pivots[1]));
        double score = weightedTerm(w1, y1) + weightedTerm(w2, y2);
        if (Double.isNaN(score))
        {
            double t1 = weightedTerm(w1, y1);
            double t2 = weightedTerm(w2, y2);
            if (t1 == Double.POSITIVE_INFINITY || t2 == Double.POSITIVE_INFINITY)
            {
                return Double.POSITIVE_INFINITY;
            }
            if (t1 == Double.NEGATIVE_INFINITY || t2 == Double.NEGATIVE_INFINITY)
            {
                return Double.NEGATIVE_INFINITY;
            }
            return 0.0;
        }
        return score;
    }

    private double weightedTerm(double weight, double value)
    {
        if (weight == 0.0)
        {
            return 0.0;
        }
        return weight * value;
    }

    private double[] quantileThresholds(double[] values, int numPartitions)
    {
        double[] thresholds = new double[numPartitions - 1];
        if (values.length == 0)
        {
            return thresholds;
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        for (int i = 1; i < numPartitions; i++)
        {
            int index = (int) Math.round(((double) i / (double) numPartitions)
                    * (sorted.length - 1));
            thresholds[i - 1] = sorted[index];
        }
        return thresholds;
    }

    private List<List<IndexObject>> partitions(List<? extends IndexObject> data,
                                               int first, int size,
                                               double[] scores,
                                               double[] thresholds,
                                               int numPartitions)
    {
        List<List<IndexObject>> partitions = new ArrayList<>(numPartitions);
        for (int i = 0; i < numPartitions; i++)
        {
            partitions.add(new ArrayList<>());
        }
        for (int i = 0; i < size; i++)
        {
            partitions.get(partitionIndex(scores[i], thresholds)).add(data.get(first + i));
        }
        return partitions;
    }

    private int partitionIndex(double score, double[] thresholds)
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

    private boolean hasEmptyPartition(List<List<IndexObject>> partitions)
    {
        for (List<IndexObject> partition : partitions)
        {
            if (partition.isEmpty())
            {
                return true;
            }
        }
        return false;
    }

    private List<List<? extends IndexObject>> fallbackPartitions(List<? extends IndexObject> data,
                                                                 int first, int size,
                                                                 int numPartitions)
    {
        List<List<? extends IndexObject>> partitions = new ArrayList<>(numPartitions);
        for (int i = 0; i < numPartitions; i++)
        {
            partitions.add(new ArrayList<>());
        }
        for (int i = 0; i < size; i++)
        {
            int partitionIndex = Math.min(numPartitions - 1,
                    (int) ((long) i * (long) numPartitions / Math.max(size, 1)));
            @SuppressWarnings("unchecked")
            List<IndexObject> partition = (List<IndexObject>) partitions.get(partitionIndex);
            partition.add(data.get(first + i));
        }
        return partitions;
    }

    @Override
    public String toString()
    {
        return "PowerDistanceLinearPartitionMethod{" +
                "rho=" + rho +
                ", w1=" + w1 +
                ", w2=" + w2 +
                ", epsilonDistance=" + epsilonDistance +
                ", comparisonEpsilon=" + comparisonEpsilon +
                '}';
    }
}
