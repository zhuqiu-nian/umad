package tpslp.partition;

import db.type.IndexObject;
import index.logdistance.LogDistanceBoundaryOptimizer;
import index.logdistance.LogDistanceLearningConfig;
import metric.Metric;
import tpslp.coordinate.CoordinateMap;
import tpslp.coordinate.LogDistanceMap;

import java.util.ArrayList;
import java.util.List;

public final class LearnedLogDistancePartitionLearner implements PartitionLearner
{
    private static final long serialVersionUID = 1L;

    private final LogDistanceLearningConfig config;
    private final List<? extends IndexObject> trainingQueries;
    private final double queryRadius;

    public LearnedLogDistancePartitionLearner(List<? extends IndexObject> trainingQueries,
                                              double queryRadius)
    {
        this(new LogDistanceLearningConfig(), trainingQueries, queryRadius);
    }

    public LearnedLogDistancePartitionLearner(LogDistanceLearningConfig config,
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
    public PartitionPlan learn(Metric metric, IndexObject[] pivots, CoordinateMap coordinateMap,
                               List<? extends IndexObject> data)
    {
        if (pivots.length != 2)
        {
            throw new IllegalArgumentException("learned log partition requires exactly 2 pivots");
        }
        if (!(coordinateMap instanceof LogDistanceMap))
        {
            throw new IllegalArgumentException("learned log partition requires LogDistanceMap");
        }

        LogDistanceLearningConfig effectiveConfig = effectiveConfig((LogDistanceMap) coordinateMap);
        LogDistanceBoundaryOptimizer.Result model = new LogDistanceBoundaryOptimizer().optimize(
                metric, pivots, data, 0, data.size(), trainingQueries, queryRadius,
                effectiveConfig);

        double[] weights = new double[]{model.getW1(), model.getW2()};
        double[] scores = scores(metric, pivots, effectiveConfig, data, weights);
        PartitionPlan plan = LinearLearningSupport.binaryPlan(data, weights, model.getTau(), scores);
        if (plan.getChildren().size() <= 1)
        {
            return fallback(data, weights, scores);
        }
        return plan;
    }

    private LogDistanceLearningConfig effectiveConfig(LogDistanceMap map)
    {
        if (config.getEpsilonDistance() == map.getEpsilonDistance())
        {
            return config;
        }
        return new LogDistanceLearningConfig(config.getAngleCount(),
                config.getTauQuantiles(), config.getMinBalance(),
                config.getTrainingQuerySampleSize(), config.getMedoidCandidateCount(),
                config.getMedoidIterations(), map.getEpsilonDistance(),
                config.getComparisonEpsilon(), config.getValidationFraction(),
                config.getTopCandidates(), config.getBoxPenaltyWeight(),
                config.getChildHitPenaltyWeight());
    }

    private double[] scores(Metric metric, IndexObject[] pivots,
                            LogDistanceLearningConfig effectiveConfig,
                            List<? extends IndexObject> data, double[] weights)
    {
        double[] scores = new double[data.size()];
        for (int i = 0; i < data.size(); i++)
        {
            scores[i] = LogDistanceBoundaryOptimizer.score(data.get(i), metric, pivots,
                    effectiveConfig.getEpsilonDistance(), weights[0], weights[1]);
        }
        return scores;
    }

    private PartitionPlan fallback(List<? extends IndexObject> data, double[] weights,
                                   double[] scores)
    {
        double threshold = ThresholdStrategy.MAX_GAP.threshold(scores);
        return LinearLearningSupport.binaryPlan(data, weights, threshold, scores);
    }
}
