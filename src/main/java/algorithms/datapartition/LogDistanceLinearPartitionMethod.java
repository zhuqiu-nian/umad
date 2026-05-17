package algorithms.datapartition;

import db.type.IndexObject;
import index.logdistance.LogDistanceTransform;
import index.structure.LogDistancePartitionResults;
import index.structure.PartitionResults;
import metric.Metric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Binary median split by a line in 2D log-distance pivot space.
 */
public class LogDistanceLinearPartitionMethod implements PartitionMethod, Serializable
{
    private static final long serialVersionUID = 8173411604248587161L;

    private final double epsilonDistance;
    private final double w1;
    private final double w2;
    private final double comparisonEpsilon;

    public LogDistanceLinearPartitionMethod()
    {
        this(1.0, -1.0);
    }

    public LogDistanceLinearPartitionMethod(double w1, double w2)
    {
        this(w1, w2,
                LogDistanceTransform.DEFAULT_EPSILON_DISTANCE,
                LogDistanceTransform.DEFAULT_COMPARISON_EPSILON);
    }

    public LogDistanceLinearPartitionMethod(double w1, double w2,
                                            double epsilonDistance,
                                            double comparisonEpsilon)
    {
        if (w1 == 0.0 && w2 == 0.0)
        {
            throw new IllegalArgumentException("at least one boundary weight must be non-zero");
        }
        if (!(epsilonDistance > 0.0))
        {
            throw new IllegalArgumentException("epsilonDistance must be positive");
        }
        this.epsilonDistance = epsilonDistance;
        this.w1 = w1;
        this.w2 = w2;
        this.comparisonEpsilon = comparisonEpsilon;
    }

    public double getW1()
    {
        return w1;
    }

    public double getW2()
    {
        return w2;
    }

    public double getEpsilonDistance()
    {
        return epsilonDistance;
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
            throw new IllegalArgumentException("Log-distance split requires exactly 2 pivots");
        }
        if (numPartitions != 2)
        {
            throw new IllegalArgumentException("Log-distance split requires exactly 2 partitions");
        }
        if (size < 0)
        {
            throw new IllegalArgumentException("size must be non-negative");
        }

        LogDistanceTransform transform = new LogDistanceTransform(epsilonDistance);
        double[] scores = new double[size];
        for (int i = 0; i < size; i++)
        {
            IndexObject x = data.get(first + i);
            scores[i] = score(transform, metric, pivots, x);
        }

        double tau = median(scores);
        List<IndexObject> left = new ArrayList<>();
        List<IndexObject> right = new ArrayList<>();
        List<IndexObject> equal = new ArrayList<>();

        for (int i = 0; i < size; i++)
        {
            IndexObject x = data.get(first + i);
            double score = scores[i];
            if (score < tau)
            {
                left.add(x);
            }
            else if (score > tau)
            {
                right.add(x);
            }
            else
            {
                equal.add(x);
            }
        }

        for (IndexObject x : equal)
        {
            if (left.size() <= right.size())
            {
                left.add(x);
            }
            else
            {
                right.add(x);
            }
        }

        if (size > 1 && (left.isEmpty() || right.isEmpty()))
        {
            left.clear();
            right.clear();
            for (int i = 0; i < size; i++)
            {
                if (i < size / 2)
                {
                    left.add(data.get(first + i));
                }
                else
                {
                    right.add(data.get(first + i));
                }
            }
        }

        List<List<? extends IndexObject>> subDataList = new ArrayList<>(2);
        subDataList.add(left);
        subDataList.add(right);
        return new LogDistancePartitionResults(subDataList, pivots,
                epsilonDistance, w1, w2, tau, comparisonEpsilon,
                childPivotDistanceRanges(metric, pivots, subDataList));
    }

    private double[][][] childPivotDistanceRanges(Metric metric, IndexObject[] pivots,
                                                  List<List<? extends IndexObject>> partitions)
    {
        double[][][] ranges = new double[partitions.size()][pivots.length][2];
        for (int child = 0; child < partitions.size(); child++)
        {
            for (int pivot = 0; pivot < pivots.length; pivot++)
            {
                ranges[child][pivot][0] = Double.POSITIVE_INFINITY;
                ranges[child][pivot][1] = Double.NEGATIVE_INFINITY;
            }
            for (IndexObject x : partitions.get(child))
            {
                for (int pivot = 0; pivot < pivots.length; pivot++)
                {
                    double distance = metric.getDistance(x, pivots[pivot]);
                    if (distance < ranges[child][pivot][0])
                    {
                        ranges[child][pivot][0] = distance;
                    }
                    if (distance > ranges[child][pivot][1])
                    {
                        ranges[child][pivot][1] = distance;
                    }
                }
            }
            for (int pivot = 0; pivot < pivots.length; pivot++)
            {
                if (ranges[child][pivot][0] == Double.POSITIVE_INFINITY)
                {
                    ranges[child][pivot][0] = 0.0;
                    ranges[child][pivot][1] = Double.POSITIVE_INFINITY;
                }
            }
        }
        return ranges;
    }

    private double score(LogDistanceTransform transform, Metric metric,
                         IndexObject[] pivots, IndexObject x)
    {
        double y1 = transform.transformPointDistance(metric.getDistance(x, pivots[0]));
        double y2 = transform.transformPointDistance(metric.getDistance(x, pivots[1]));
        double score = weightedTerm(w1, y1) + weightedTerm(w2, y2);
        if (Double.isNaN(score))
        {
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

    private double median(double[] values)
    {
        if (values.length == 0)
        {
            return 0.0;
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    @Override
    public String toString()
    {
        return "LogDistanceLinearPartitionMethod{" +
                "w1=" + w1 +
                ", w2=" + w2 +
                ", epsilonDistance=" + epsilonDistance +
                ", comparisonEpsilon=" + comparisonEpsilon +
                '}';
    }
}
