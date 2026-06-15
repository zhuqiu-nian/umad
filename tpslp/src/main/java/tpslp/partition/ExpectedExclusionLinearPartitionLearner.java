package tpslp.partition;

import db.type.IndexObject;
import metric.Metric;
import tpslp.coordinate.CoordinateMap;
import tpslp.geometry.Interval;

import java.util.List;

/**
 * Fixed-direction learner whose thresholds are selected by expected exclusion
 * power. It is intended for isolating the usefulness of Vadicamo et al.'s
 * threshold theory from direction-learning effects.
 */
public final class ExpectedExclusionLinearPartitionLearner implements PartitionLearner
{
    private static final long serialVersionUID = 1L;

    private final LinearBoundary boundary;
    private final List<? extends IndexObject> trainingQueries;
    private final double queryRadius;
    private final ExpectedExclusionThresholdSelector selector;

    public ExpectedExclusionLinearPartitionLearner(LinearBoundary boundary,
                                                   List<? extends IndexObject> trainingQueries,
                                                   double queryRadius,
                                                   int maxThresholds)
    {
        this(boundary, trainingQueries, queryRadius,
                new ExpectedExclusionThresholdSelector(maxThresholds));
    }

    public ExpectedExclusionLinearPartitionLearner(LinearBoundary boundary,
                                                   List<? extends IndexObject> trainingQueries,
                                                   double queryRadius,
                                                   ExpectedExclusionThresholdSelector selector)
    {
        if (boundary == null || selector == null)
        {
            throw new IllegalArgumentException("boundary and selector must not be null");
        }
        if (queryRadius < 0.0)
        {
            throw new IllegalArgumentException("queryRadius must be non-negative");
        }
        this.boundary = boundary;
        this.trainingQueries = trainingQueries;
        this.queryRadius = queryRadius;
        this.selector = selector;
    }

    @Override
    public PartitionPlan learn(Metric metric, IndexObject[] pivots, CoordinateMap coordinateMap,
                               List<? extends IndexObject> data)
    {
        if (boundary.dimension() != coordinateMap.dimension(pivots.length))
        {
            throw new IllegalArgumentException("boundary dimension does not match coordinate map");
        }
        double[][] coordinates = LinearLearningSupport.coordinates(metric, pivots, coordinateMap, data);
        double[] scores = new double[coordinates.length];
        for (int i = 0; i < coordinates.length; i++)
        {
            scores[i] = boundary.score(coordinates[i]);
        }
        Interval[] queryIntervals = LinearLearningSupport.projectedQueryIntervals(metric,
                pivots, coordinateMap, trainingQueries, queryRadius, boundary.getWeights());
        double[] thresholds = selector.select(scores, queryIntervals);
        return LinearLearningSupport.multiThresholdPlan(data, boundary.getWeights(),
                thresholds, scores);
    }
}
