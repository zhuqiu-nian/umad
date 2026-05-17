package algorithms.pivotselection;

import db.type.IndexObject;
import index.logdistance.LogDistanceBoundaryOptimizer;
import index.logdistance.LogDistanceLearningConfig;
import metric.Metric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LogDistanceMedoidPairPivotSelectionMethod
        implements PivotSelectionMethod, Serializable
{
    private static final long serialVersionUID = 1200751104460328027L;

    private final LogDistanceLearningConfig config;
    private transient List<? extends IndexObject> trainingQueries;
    private final double queryRadius;

    public LogDistanceMedoidPairPivotSelectionMethod(List<? extends IndexObject> trainingQueries,
                                                     double queryRadius)
    {
        this(new LogDistanceLearningConfig(), trainingQueries, queryRadius);
    }

    public LogDistanceMedoidPairPivotSelectionMethod(LogDistanceLearningConfig config,
                                                     List<? extends IndexObject> trainingQueries,
                                                     double queryRadius)
    {
        if (config == null)
        {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
        this.trainingQueries = trainingQueries;
        this.queryRadius = queryRadius;
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

        LogDistanceBoundaryOptimizer optimizer = new LogDistanceBoundaryOptimizer();
        LogDistanceLearningConfig pairConfig = pairScoringConfig();
        int[] fftPair = PivotSelectionMethods.FFT.selectPivots(metric, subset, 2);
        int bestI = fftPair.length > 0 ? fftPair[0] : candidates[0];
        int bestJ = fftPair.length > 1 ? fftPair[1] : candidates[1];
        LogDistanceBoundaryOptimizer.Result bestResult = optimizer.optimize(
                metric, new IndexObject[]{subset.get(bestI), subset.get(bestJ)},
                subset, 0, subset.size(), trainingQueries, queryRadius, pairConfig);
        double bestPairDistance = metric.getDistance(subset.get(bestI), subset.get(bestJ));
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
                LogDistanceBoundaryOptimizer.Result result = optimizer.optimize(
                        metric, new IndexObject[]{subset.get(i), subset.get(j)},
                        subset, 0, subset.size(), trainingQueries, queryRadius,
                        pairConfig);
                if (result.getEstimatedDistanceCost() < bestResult.getEstimatedDistanceCost()
                        || (result.getEstimatedDistanceCost() == bestResult.getEstimatedDistanceCost()
                        && pairDistance > bestPairDistance))
                {
                    bestResult = result;
                    bestI = i;
                    bestJ = j;
                    bestPairDistance = pairDistance;
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
                max = Math.max(max, metric.getDistance(data.get(candidates[a]),
                        data.get(candidates[b])));
            }
        }
        return max;
    }

    private int[] medoidCandidates(Metric metric, List<? extends IndexObject> data)
    {
        int candidateCount = Math.min(config.getMedoidCandidateCount(), data.size());
        int candidateLimit = Math.min(data.size(), Math.max(candidateCount, candidateCount * 2));
        int[] fftCenters = PivotSelectionMethods.FFT.selectPivots(metric, data,
                candidateLimit);
        int[] centers = Arrays.copyOf(fftCenters, Math.min(candidateCount, fftCenters.length));
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
            if (unique.size() >= candidateCount)
            {
                break;
            }
        }
        for (int center : fftCenters)
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

    private LogDistanceLearningConfig pairScoringConfig()
    {
        return new LogDistanceLearningConfig(
                Math.min(config.getAngleCount(), 8),
                compactQuantiles(config.getTauQuantiles()),
                config.getMinBalance(),
                Math.min(config.getTrainingQuerySampleSize(), 64),
                config.getMedoidCandidateCount(),
                config.getMedoidIterations(),
                config.getEpsilonDistance(),
                config.getComparisonEpsilon(),
                config.getValidationFraction(),
                Math.min(config.getTopCandidates(), 8),
                config.getBoxPenaltyWeight(),
                config.getChildHitPenaltyWeight());
    }

    private double[] compactQuantiles(double[] configured)
    {
        if (configured.length <= 3)
        {
            return configured;
        }
        return new double[]{0.4, 0.5, 0.6};
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

    private List<Integer> membersOf(int[] assignment, int center)
    {
        List<Integer> members = new ArrayList<>();
        for (int i = 0; i < assignment.length; i++)
        {
            if (assignment[i] == center)
            {
                members.add(i);
            }
        }
        return members;
    }

    private int medoid(Metric metric, List<? extends IndexObject> data,
                       List<Integer> members)
    {
        int best = members.get(0);
        double bestSum = Double.POSITIVE_INFINITY;
        for (int candidate : members)
        {
            double sum = 0.0;
            for (int other : members)
            {
                sum += metric.getDistance(data.get(candidate), data.get(other));
            }
            if (sum < bestSum)
            {
                bestSum = sum;
                best = candidate;
            }
        }
        return best;
    }

    private int[] adjust(int first, int[] indexes)
    {
        int[] result = new int[indexes.length];
        for (int i = 0; i < indexes.length; i++)
        {
            result[i] = first + indexes[i];
        }
        return result;
    }
}
