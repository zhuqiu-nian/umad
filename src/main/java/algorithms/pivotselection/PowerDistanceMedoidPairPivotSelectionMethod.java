package algorithms.pivotselection;

import db.type.IndexObject;
import index.powerdistance.PowerDistanceBoundaryOptimizer;
import index.powerdistance.PowerDistanceLearningConfig;
import metric.Metric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PowerDistanceMedoidPairPivotSelectionMethod
        implements PivotSelectionMethod, Serializable
{
    private static final long serialVersionUID = 6685395780100955416L;

    private final PowerDistanceLearningConfig config;
    private transient List<? extends IndexObject> trainingQueries;
    private final double queryRadius;
    private final int numPartitions;

    public PowerDistanceMedoidPairPivotSelectionMethod(List<? extends IndexObject> trainingQueries,
                                                       double queryRadius)
    {
        this(new PowerDistanceLearningConfig(), trainingQueries, queryRadius);
    }

    public PowerDistanceMedoidPairPivotSelectionMethod(PowerDistanceLearningConfig config,
                                                       List<? extends IndexObject> trainingQueries,
                                                       double queryRadius)
    {
        this(config, trainingQueries, queryRadius, 2);
    }

    public PowerDistanceMedoidPairPivotSelectionMethod(PowerDistanceLearningConfig config,
                                                       List<? extends IndexObject> trainingQueries,
                                                       double queryRadius,
                                                       int numPartitions)
    {
        if (config == null)
        {
            throw new IllegalArgumentException("config must not be null");
        }
        if (numPartitions < 2)
        {
            throw new IllegalArgumentException("numPartitions must be at least 2");
        }
        this.config = config;
        this.trainingQueries = trainingQueries;
        this.queryRadius = queryRadius;
        this.numPartitions = numPartitions;
    }

    @Override
    public int[] selectPivots(Metric metric, List<? extends IndexObject> data,
                              int numPivots)
    {
        return selectPivots(metric, data, 0, data.size(), numPivots);
    }

    @Override
    public int[] selectPivots(Metric metric, List<? extends IndexObject> data,
                              int first, int dataSize, int numPivots)
    {
        if (numPivots != 2)
        {
            return PivotSelectionMethods.FFT.selectPivots(metric, data, first,
                    dataSize, numPivots);
        }
        if (dataSize <= numPivots)
        {
            int[] result = new int[dataSize];
            for (int i = 0; i < dataSize; i++)
            {
                result[i] = first + i;
            }
            return result;
        }

        List<IndexObject> subset = new ArrayList<>();
        for (int i = 0; i < dataSize; i++)
        {
            subset.add(data.get(first + i));
        }

        int[] candidates = medoidCandidates(metric, subset);
        if (candidates.length <= 2)
        {
            return adjust(first, candidates);
        }

        PowerDistanceBoundaryOptimizer optimizer = new PowerDistanceBoundaryOptimizer();
        PowerDistanceBoundaryOptimizer.Result bestResult = null;
        int bestI = candidates[0];
        int bestJ = candidates[1];
        double maxPairDistance = maxPairDistance(metric, subset, candidates);
        double minPairDistance = maxPairDistance * 0.25;
        for (int a = 0; a < candidates.length; a++)
        {
            for (int b = a + 1; b < candidates.length; b++)
            {
                int i = candidates[a];
                int j = candidates[b];
                double pairDistance = metric.getDistance(subset.get(i), subset.get(j));
                if (pairDistance < minPairDistance)
                {
                    continue;
                }
                IndexObject[] pivots = new IndexObject[]{
                        subset.get(i), subset.get(j)
                };
                PowerDistanceBoundaryOptimizer.Result result = optimizer.optimize(
                        metric, pivots, subset, 0, subset.size(),
                        trainingQueries, queryRadius, config, numPartitions);
                if (better(result, bestResult))
                {
                    bestResult = result;
                    bestI = i;
                    bestJ = j;
                }
            }
        }
        return new int[]{first + bestI, first + bestJ};
    }

    private double maxPairDistance(Metric metric, List<? extends IndexObject> data,
                                   int[] candidates)
    {
        double max = 0.0;
        for (int a = 0; a < candidates.length; a++)
        {
            for (int b = a + 1; b < candidates.length; b++)
            {
                double distance = metric.getDistance(data.get(candidates[a]),
                        data.get(candidates[b]));
                if (distance > max)
                {
                    max = distance;
                }
            }
        }
        return max;
    }

    private int[] medoidCandidates(Metric metric, List<? extends IndexObject> data)
    {
        int candidateCount = Math.min(config.getMedoidCandidateCount(), data.size());
        int[] centers = PivotSelectionMethods.FFT.selectPivots(metric, data,
                candidateCount);
        if (centers.length == 0)
        {
            return new int[0];
        }

        int[] assignment = new int[data.size()];
        Arrays.fill(assignment, -1);
        for (int iteration = 0; iteration < config.getMedoidIterations(); iteration++)
        {
            assign(metric, data, centers, assignment);
            int[] nextCenters = centers.clone();
            for (int c = 0; c < centers.length; c++)
            {
                List<Integer> members = membersOf(assignment, c);
                if (!members.isEmpty())
                {
                    nextCenters[c] = medoid(metric, data, members);
                }
            }
            centers = nextCenters;
        }

        Set<Integer> unique = new LinkedHashSet<>();
        for (int center : centers)
        {
            unique.add(center);
        }
        for (int center : PivotSelectionMethods.FFT.selectPivots(metric, data,
                candidateCount))
        {
            unique.add(center);
            if (unique.size() >= candidateCount)
            {
                break;
            }
        }
        for (int i = 0; i < data.size() && unique.size() < candidateCount; i++)
        {
            unique.add(i);
        }

        int[] result = new int[unique.size()];
        int index = 0;
        for (Integer value : unique)
        {
            result[index++] = value;
        }
        return result;
    }

    private void assign(Metric metric, List<? extends IndexObject> data,
                        int[] centers, int[] assignment)
    {
        for (int i = 0; i < data.size(); i++)
        {
            double bestDistance = Double.POSITIVE_INFINITY;
            int bestCenter = 0;
            for (int c = 0; c < centers.length; c++)
            {
                double distance = metric.getDistance(data.get(i), data.get(centers[c]));
                if (distance < bestDistance)
                {
                    bestDistance = distance;
                    bestCenter = c;
                }
            }
            assignment[i] = bestCenter;
        }
    }

    private List<Integer> membersOf(int[] assignment, int cluster)
    {
        List<Integer> members = new ArrayList<>();
        for (int i = 0; i < assignment.length; i++)
        {
            if (assignment[i] == cluster)
            {
                members.add(i);
            }
        }
        return members;
    }

    private int medoid(Metric metric, List<? extends IndexObject> data,
                       List<Integer> members)
    {
        double bestSum = Double.POSITIVE_INFINITY;
        int best = members.get(0);
        for (int candidate : members)
        {
            double sum = 0.0;
            IndexObject c = data.get(candidate);
            for (int other : members)
            {
                sum += metric.getDistance(c, data.get(other));
            }
            if (sum < bestSum)
            {
                bestSum = sum;
                best = candidate;
            }
        }
        return best;
    }

    private int[] adjust(int first, int[] local)
    {
        int[] adjusted = new int[local.length];
        for (int i = 0; i < local.length; i++)
        {
            adjusted[i] = first + local[i];
        }
        return adjusted;
    }

    private boolean better(PowerDistanceBoundaryOptimizer.Result candidate,
                           PowerDistanceBoundaryOptimizer.Result best)
    {
        if (best == null)
        {
            return true;
        }
        if (candidate.getScore() > best.getScore() + 1e-12)
        {
            return true;
        }
        if (candidate.getScore() < best.getScore() - 1e-12)
        {
            return false;
        }
        if (candidate.getBalanceScore() > best.getBalanceScore() + 1e-12)
        {
            return true;
        }
        if (candidate.getBalanceScore() < best.getBalanceScore() - 1e-12)
        {
            return false;
        }
        return candidate.getMarginScore() > best.getMarginScore() + 1e-12;
    }

    @Override
    public String toString()
    {
        return "PowerDistanceMedoidPairPivotSelectionMethod{" +
                "config=" + config +
                ", queryRadius=" + queryRadius +
                ", numPartitions=" + numPartitions +
                '}';
    }
}
