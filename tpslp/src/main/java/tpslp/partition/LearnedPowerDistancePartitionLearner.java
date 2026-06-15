package tpslp.partition;

import db.type.IndexObject;
import index.powerdistance.PowerDistanceBoundaryOptimizer;
import index.powerdistance.PowerDistanceLearningConfig;
import metric.Metric;
import tpslp.coordinate.CoordinateMap;
import tpslp.coordinate.PowerDistanceMap;

import java.util.ArrayList;
import java.util.List;

public final class LearnedPowerDistancePartitionLearner implements PartitionLearner
{
    private static final long serialVersionUID = 1L;

    private final PowerDistanceLearningConfig config;
    private final List<? extends IndexObject> trainingQueries;
    private final double queryRadius;
    private final int numPartitions;

    public LearnedPowerDistancePartitionLearner(List<? extends IndexObject> trainingQueries,
                                                double queryRadius)
    {
        this(new PowerDistanceLearningConfig(), trainingQueries, queryRadius, 2);
    }

    public LearnedPowerDistancePartitionLearner(PowerDistanceLearningConfig config,
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
    public PartitionPlan learn(Metric metric, IndexObject[] pivots, CoordinateMap coordinateMap,
                               List<? extends IndexObject> data)
    {
        if (pivots.length != 2)
        {
            throw new IllegalArgumentException("learned power partition requires exactly 2 pivots");
        }
        if (!(coordinateMap instanceof PowerDistanceMap))
        {
            throw new IllegalArgumentException("learned power partition requires PowerDistanceMap");
        }

        PowerDistanceMap map = (PowerDistanceMap) coordinateMap;
        PowerDistanceLearningConfig effectiveConfig = effectiveConfig(map);
        PowerDistanceBoundaryOptimizer.Result model = new PowerDistanceBoundaryOptimizer().optimize(
                metric, pivots, data, 0, data.size(), trainingQueries, queryRadius,
                effectiveConfig, numPartitions);

        double[] scores = scores(metric, pivots, effectiveConfig, model, data);
        return multiThresholdPlan(data, new double[]{model.getW1(), model.getW2()},
                model.getThresholds(), scores);
    }

    private PowerDistanceLearningConfig effectiveConfig(PowerDistanceMap map)
    {
        return new PowerDistanceLearningConfig(new double[]{map.getRho()},
                config.getAngleCount(), config.getTauQuantiles(),
                config.getMinBalance(), config.getTrainingQuerySampleSize(),
                config.getMedoidCandidateCount(), config.getMedoidIterations(),
                map.getEpsilonDistance(), config.getComparisonEpsilon(),
                config.getValidationFraction(), config.getTopCandidates(),
                config.getBoxPenaltyWeight(), config.getChildHitPenaltyWeight());
    }

    private double[] scores(Metric metric, IndexObject[] pivots,
                            PowerDistanceLearningConfig effectiveConfig,
                            PowerDistanceBoundaryOptimizer.Result model,
                            List<? extends IndexObject> data)
    {
        double[] scores = new double[data.size()];
        for (int i = 0; i < data.size(); i++)
        {
            scores[i] = PowerDistanceBoundaryOptimizer.score(data.get(i), metric, pivots,
                    model.getRho(), effectiveConfig.getEpsilonDistance(),
                    model.getW1(), model.getW2());
        }
        return scores;
    }

    private PartitionPlan multiThresholdPlan(List<? extends IndexObject> data,
                                             double[] weights,
                                             double[] thresholds,
                                             double[] scores)
    {
        LinearBoundary boundary = new LinearBoundary(weights);
        List<List<IndexObject>> partitions = new ArrayList<>(thresholds.length + 1);
        for (int i = 0; i <= thresholds.length; i++)
        {
            partitions.add(new ArrayList<>());
        }
        for (int i = 0; i < scores.length; i++)
        {
            partitions.get(partitionIndex(scores[i], thresholds)).add(data.get(i));
        }

        List<PartitionPlan.ChildPartition> children = new ArrayList<>();
        for (int i = 0; i < partitions.size(); i++)
        {
            if (partitions.get(i).isEmpty())
            {
                continue;
            }
            double low = i == 0 ? Double.NEGATIVE_INFINITY : thresholds[i - 1];
            double high = i == thresholds.length ? Double.POSITIVE_INFINITY : thresholds[i];
            children.add(new PartitionPlan.ChildPartition(partitions.get(i),
                    new LinearSlabRegion(List.of(new LinearSlab(boundary, low, high)))));
        }
        if (children.size() <= 1)
        {
            return LinearLearningSupport.leafPlan(data, weights.length);
        }
        return new PartitionPlan(children);
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
}
