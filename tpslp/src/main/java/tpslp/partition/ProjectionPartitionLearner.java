package tpslp.partition;

import db.type.IndexObject;
import metric.Metric;
import tpslp.coordinate.CoordinateMap;
import tpslp.geometry.Interval;

import java.util.List;

/**
 * Learns one oblique linear split from the data distribution in transformed
 * pivot space. The PCA variant uses the principal data direction. The
 * query-adjusted variant penalizes directions with wide query-box projections.
 */
public final class ProjectionPartitionLearner implements PartitionLearner
{
    private static final long serialVersionUID = 1L;

    public enum Mode
    {
        PCA,
        QUERY_ADJUSTED_PCA
    }

    private final Mode mode;
    private final List<? extends IndexObject> trainingQueries;
    private final double queryRadius;
    private final ThresholdStrategy thresholdStrategy;

    public ProjectionPartitionLearner(Mode mode)
    {
        this(mode, null, 0.0, ThresholdStrategy.MAX_GAP);
    }

    public ProjectionPartitionLearner(Mode mode, List<? extends IndexObject> trainingQueries,
                                      double queryRadius, ThresholdStrategy thresholdStrategy)
    {
        if (mode == null || thresholdStrategy == null)
        {
            throw new IllegalArgumentException("mode and thresholdStrategy must not be null");
        }
        if (queryRadius < 0.0)
        {
            throw new IllegalArgumentException("queryRadius must be non-negative");
        }
        this.mode = mode;
        this.trainingQueries = trainingQueries;
        this.queryRadius = queryRadius;
        this.thresholdStrategy = thresholdStrategy;
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

        double[] direction = mode == Mode.QUERY_ADJUSTED_PCA
                ? queryAdjustedDirection(metric, pivots, coordinateMap, data, coordinates)
                : LinearLearningSupport.firstPrincipalDirection(coordinates);
        direction = LinearLearningSupport.normalizeOrFallback(direction, coordinates[0].length);

        double[] scores = LinearLearningSupport.scores(coordinates, direction);
        double threshold = threshold(metric, pivots, coordinateMap, data, direction, scores);
        return LinearLearningSupport.binaryPlan(data, direction, threshold, scores);
    }

    private double threshold(Metric metric, IndexObject[] pivots, CoordinateMap coordinateMap,
                             List<? extends IndexObject> data, double[] direction, double[] scores)
    {
        if (trainingQueries == null || trainingQueries.isEmpty())
        {
            return thresholdStrategy.threshold(scores);
        }
        double bestThreshold = thresholdStrategy.threshold(scores);
        double bestCost = Double.POSITIVE_INFINITY;
        for (double candidate : LinearLearningSupport.thresholdCandidates(scores))
        {
            double cost = LinearLearningSupport.queryVisitCost(metric, pivots, coordinateMap,
                    data, trainingQueries, queryRadius, direction, new double[]{candidate}, scores);
            if (cost < bestCost)
            {
                bestCost = cost;
                bestThreshold = candidate;
            }
        }
        return bestThreshold;
    }

    private double[] queryAdjustedDirection(Metric metric, IndexObject[] pivots,
                                            CoordinateMap coordinateMap,
                                            List<? extends IndexObject> data,
                                            double[][] coordinates)
    {
        double[][] dataCovariance = LinearLearningSupport.covariance(coordinates);
        double[] queryPenalty = averageQueryHalfWidths(metric, pivots, coordinateMap, data);
        return LinearLearningSupport.diagonalGeneralizedPrincipalDirection(dataCovariance, queryPenalty);
    }

    private double[] averageQueryHalfWidths(Metric metric, IndexObject[] pivots,
                                            CoordinateMap coordinateMap,
                                            List<? extends IndexObject> data)
    {
        int dimension = coordinateMap.dimension(pivots.length);
        double[] widths = new double[dimension];
        List<? extends IndexObject> queries =
                trainingQueries == null || trainingQueries.isEmpty() ? data : trainingQueries;
        int used = 0;
        for (IndexObject query : queries)
        {
            Interval[] box = coordinateMap.mapQuery(metric, pivots, query, queryRadius);
            for (int i = 0; i < dimension; i++)
            {
                double width = box[i].getHigh() - box[i].getLow();
                widths[i] += width * width;
            }
            used++;
        }
        if (used == 0)
        {
            for (int i = 0; i < dimension; i++)
            {
                widths[i] = 1.0;
            }
            return widths;
        }
        for (int i = 0; i < dimension; i++)
        {
            widths[i] = widths[i] / used + 1e-9;
        }
        return widths;
    }
}
