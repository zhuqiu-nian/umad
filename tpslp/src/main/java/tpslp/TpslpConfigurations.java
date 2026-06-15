package tpslp;

import index.logdistance.LogDistanceLearningConfig;
import index.powerdistance.PowerDistanceLearningConfig;
import tpslp.partition.BoundaryFactories;
import tpslp.partition.DensityEnvelopePartitionLearner;
import tpslp.partition.DensityValleyPartitionLearner;
import tpslp.partition.ExpectedExclusionLinearPartitionLearner;
import tpslp.partition.ExpectedExclusionThresholdSelector;
import tpslp.partition.LearnedLogDistancePartitionLearner;
import tpslp.partition.LearnedPowerDistancePartitionLearner;
import tpslp.partition.LinearBoundary;
import tpslp.partition.LinearSlabPartitionLearner;
import tpslp.partition.PartitionLearner;
import tpslp.partition.ProjectionPartitionLearner;
import tpslp.partition.ThresholdStrategy;

import db.type.IndexObject;
import java.util.List;

public final class TpslpConfigurations
{
    private TpslpConfigurations()
    {
    }

    public static PartitionLearner vp(int dimension, int pivotIndex, int partitions)
    {
        return new LinearSlabPartitionLearner(
                List.of(BoundaryFactories.vp(dimension, pivotIndex)), partitions);
    }

    public static PartitionLearner mvp(int dimension, int partitionsPerPivot)
    {
        return new LinearSlabPartitionLearner(
                BoundaryFactories.mvp(dimension), partitionsPerPivot);
    }

    public static PartitionLearner gh(int partitions)
    {
        return new LinearSlabPartitionLearner(List.of(BoundaryFactories.gh()), partitions);
    }

    public static PartitionLearner cghtDifferenceAndSum(int partitionsPerBoundary)
    {
        return new LinearSlabPartitionLearner(BoundaryFactories.cght(), partitionsPerBoundary);
    }

    public static PartitionLearner cghtSum(int partitions)
    {
        return new LinearSlabPartitionLearner(List.of(BoundaryFactories.cghtSum()), partitions);
    }

    public static PartitionLearner rgh(double lambda, int partitions)
    {
        return new LinearSlabPartitionLearner(List.of(BoundaryFactories.rgh(lambda)), partitions);
    }

    public static PartitionLearner cpLike(int dimension, int partitionsPerBoundary)
    {
        return new LinearSlabPartitionLearner(
                BoundaryFactories.cpLike(dimension), partitionsPerBoundary);
    }

    public static PartitionLearner freeLine(double[] weights, int partitions)
    {
        return new LinearSlabPartitionLearner(List.of(new LinearBoundary(weights)), partitions);
    }

    public static PartitionLearner expectedExclusion(double[] weights,
                                                     List<? extends IndexObject> trainingQueries,
                                                     double queryRadius,
                                                     int maxThresholds)
    {
        return new ExpectedExclusionLinearPartitionLearner(new LinearBoundary(weights),
                trainingQueries, queryRadius, maxThresholds);
    }

    public static PartitionLearner expectedExclusion(double[] weights,
                                                     List<? extends IndexObject> trainingQueries,
                                                     double queryRadius,
                                                     ExpectedExclusionThresholdSelector selector)
    {
        return new ExpectedExclusionLinearPartitionLearner(new LinearBoundary(weights),
                trainingQueries, queryRadius, selector);
    }

    public static PartitionLearner pca()
    {
        return new ProjectionPartitionLearner(ProjectionPartitionLearner.Mode.PCA);
    }

    public static PartitionLearner pca(ThresholdStrategy thresholdStrategy)
    {
        return new ProjectionPartitionLearner(ProjectionPartitionLearner.Mode.PCA,
                null, 0.0, thresholdStrategy);
    }

    public static PartitionLearner queryAdjustedPca(List<? extends IndexObject> trainingQueries,
                                                    double queryRadius,
                                                    ThresholdStrategy thresholdStrategy)
    {
        return new ProjectionPartitionLearner(
                ProjectionPartitionLearner.Mode.QUERY_ADJUSTED_PCA,
                trainingQueries, queryRadius, thresholdStrategy);
    }

    public static PartitionLearner densityValley()
    {
        return new DensityValleyPartitionLearner();
    }

    public static PartitionLearner densityValley(double minimumSideFraction)
    {
        return new DensityValleyPartitionLearner(minimumSideFraction);
    }

    public static PartitionLearner densityValley(double minimumSideFraction,
                                                 List<? extends IndexObject> trainingQueries,
                                                 double queryRadius)
    {
        return new DensityValleyPartitionLearner(minimumSideFraction,
                trainingQueries, queryRadius);
    }

    public static PartitionLearner densityEnvelope()
    {
        return new DensityEnvelopePartitionLearner();
    }

    public static PartitionLearner densityEnvelope(int minPts, double epsilon,
                                                   DensityEnvelopePartitionLearner.Envelope envelope,
                                                   double marginFraction,
                                                   int minimumClusterSize)
    {
        return new DensityEnvelopePartitionLearner(minPts, epsilon, envelope,
                marginFraction, minimumClusterSize);
    }

    public static PartitionLearner densityEnvelope(int minPts, double epsilon,
                                                   DensityEnvelopePartitionLearner.Envelope envelope,
                                                   double marginFraction,
                                                   int minimumClusterSize,
                                                   double automaticEpsilonQuantile)
    {
        return new DensityEnvelopePartitionLearner(minPts, epsilon, envelope,
                marginFraction, minimumClusterSize, automaticEpsilonQuantile);
    }

    public static PartitionLearner learnedLog(List<? extends IndexObject> trainingQueries,
                                              double queryRadius)
    {
        return new LearnedLogDistancePartitionLearner(trainingQueries, queryRadius);
    }

    public static PartitionLearner learnedLog(LogDistanceLearningConfig config,
                                              List<? extends IndexObject> trainingQueries,
                                              double queryRadius)
    {
        return new LearnedLogDistancePartitionLearner(config, trainingQueries, queryRadius);
    }

    public static PartitionLearner learnedPower(List<? extends IndexObject> trainingQueries,
                                                double queryRadius)
    {
        return new LearnedPowerDistancePartitionLearner(trainingQueries, queryRadius);
    }

    public static PartitionLearner learnedPower(PowerDistanceLearningConfig config,
                                                List<? extends IndexObject> trainingQueries,
                                                double queryRadius,
                                                int numPartitions)
    {
        return new LearnedPowerDistancePartitionLearner(config, trainingQueries,
                queryRadius, numPartitions);
    }
}
