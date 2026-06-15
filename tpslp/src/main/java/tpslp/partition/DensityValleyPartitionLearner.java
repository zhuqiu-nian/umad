package tpslp.partition;

import db.type.IndexObject;
import metric.Metric;
import tpslp.coordinate.CoordinateMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Selects a deterministic direction whose one-dimensional projection has a
 * strong low-density valley. This is a data-distribution alternative to
 * balanced quantile splitting.
 */
public final class DensityValleyPartitionLearner implements PartitionLearner
{
    private static final long serialVersionUID = 1L;

    private final double minimumSideFraction;
    private final List<? extends IndexObject> trainingQueries;
    private final double queryRadius;

    public DensityValleyPartitionLearner()
    {
        this(0.1, null, 0.0);
    }

    public DensityValleyPartitionLearner(double minimumSideFraction)
    {
        this(minimumSideFraction, null, 0.0);
    }

    public DensityValleyPartitionLearner(double minimumSideFraction,
                                         List<? extends IndexObject> trainingQueries,
                                         double queryRadius)
    {
        if (minimumSideFraction < 0.0 || minimumSideFraction >= 0.5)
        {
            throw new IllegalArgumentException("minimumSideFraction must be in [0, 0.5)");
        }
        if (queryRadius < 0.0)
        {
            throw new IllegalArgumentException("queryRadius must be non-negative");
        }
        this.minimumSideFraction = minimumSideFraction;
        this.trainingQueries = trainingQueries;
        this.queryRadius = queryRadius;
    }

    @Override
    public PartitionPlan learn(Metric metric, IndexObject[] pivots, CoordinateMap coordinateMap,
                               List<? extends IndexObject> data)
    {
        double[][] coordinates = LinearLearningSupport.coordinates(metric, pivots, coordinateMap, data);
        if (coordinates.length < 2)
        {
            return LinearLearningSupport.leafPlan(data, coordinateMap.dimension(pivots.length));
        }

        List<double[]> directions = candidateDirections(coordinates);
        Candidate best = null;
        for (double[] direction : directions)
        {
            direction = LinearLearningSupport.normalizeOrFallback(direction, coordinates[0].length);
            double[] scores = LinearLearningSupport.scores(coordinates, direction);
            Candidate candidate = bestValley(direction, scores);
            if (candidate != null && trainingQueries != null && !trainingQueries.isEmpty())
            {
                candidate = candidate.withQueryCost(LinearLearningSupport.queryVisitCost(
                        metric, pivots, coordinateMap, data, trainingQueries, queryRadius,
                        direction, new double[]{candidate.threshold}, scores));
            }
            if (candidate != null && (best == null || candidate.score > best.score))
            {
                best = candidate;
            }
        }

        if (best == null)
        {
            double[] direction = LinearLearningSupport.normalizeOrFallback(
                    LinearLearningSupport.firstPrincipalDirection(coordinates), coordinates[0].length);
            double[] scores = LinearLearningSupport.scores(coordinates, direction);
            return LinearLearningSupport.binaryPlan(data, direction,
                    ThresholdStrategy.MAX_GAP.threshold(scores), scores);
        }
        return LinearLearningSupport.binaryPlan(data, best.direction, best.threshold, best.scores);
    }

    private List<double[]> candidateDirections(double[][] coordinates)
    {
        int dimension = coordinates[0].length;
        List<double[]> directions = new ArrayList<>();
        directions.add(LinearLearningSupport.firstPrincipalDirection(coordinates));
        for (int i = 0; i < dimension; i++)
        {
            double[] axis = new double[dimension];
            axis[i] = 1.0;
            directions.add(axis);
        }
        for (int i = 0; i < dimension; i++)
        {
            for (int j = i + 1; j < dimension; j++)
            {
                double[] diff = new double[dimension];
                diff[i] = 1.0;
                diff[j] = -1.0;
                directions.add(diff);

                double[] sum = new double[dimension];
                sum[i] = 1.0;
                sum[j] = 1.0;
                directions.add(sum);
            }
        }
        return directions;
    }

    private Candidate bestValley(double[] direction, double[] scores)
    {
        List<Projection> projections = new ArrayList<>();
        for (int i = 0; i < scores.length; i++)
        {
            projections.add(new Projection(scores[i], i));
        }
        projections.sort(Comparator.comparingDouble(p -> p.score));

        int minSide = Math.max(1, (int) Math.ceil(scores.length * minimumSideFraction));
        double range = projections.get(projections.size() - 1).score - projections.get(0).score;
        if (range <= 0.0)
        {
            return null;
        }

        double bestScore = Double.NEGATIVE_INFINITY;
        double bestThreshold = 0.0;
        for (int i = minSide - 1; i < projections.size() - minSide; i++)
        {
            double left = projections.get(i).score;
            double right = projections.get(i + 1).score;
            double gap = right - left;
            double balance = Math.min(i + 1, projections.size() - i - 1) / (double) projections.size();
            double valleyScore = (gap / range) * Math.sqrt(balance);
            if (valleyScore > bestScore)
            {
                bestScore = valleyScore;
                bestThreshold = (left + right) * 0.5;
            }
        }
        if (bestScore <= 0.0)
        {
            return null;
        }
        return new Candidate(direction, bestThreshold, scores, bestScore);
    }

    private static final class Projection
    {
        private final double score;
        @SuppressWarnings("unused")
        private final int index;

        private Projection(double score, int index)
        {
            this.score = score;
            this.index = index;
        }
    }

    private static final class Candidate
    {
        private final double[] direction;
        private final double threshold;
        private final double[] scores;
        private final double score;
        private final double queryCost;

        private Candidate(double[] direction, double threshold, double[] scores, double score)
        {
            this(direction, threshold, scores, score, Double.POSITIVE_INFINITY);
        }

        private Candidate(double[] direction, double threshold, double[] scores,
                          double score, double queryCost)
        {
            this.direction = direction;
            this.threshold = threshold;
            this.scores = scores;
            this.score = score;
            this.queryCost = queryCost;
        }

        private Candidate withQueryCost(double queryCost)
        {
            double queryScore = -queryCost + score * 1e-6;
            return new Candidate(direction, threshold, scores, queryScore, queryCost);
        }
    }
}
