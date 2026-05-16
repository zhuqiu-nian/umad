package algorithms.datapartition;

import db.type.IndexObject;
import index.powerdistance.PowerDistanceBoundaryOptimizer;
import index.powerdistance.PowerDistanceLearningConfig;
import index.structure.PartitionResults;
import index.structure.PowerDistancePartitionResults;
import metric.Metric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PowerDistanceLearnedPartitionMethod implements PartitionMethod, Serializable
{
    private static final long serialVersionUID = -3187483777221483054L;

    private final PowerDistanceLearningConfig config;
    private transient List<? extends IndexObject> trainingQueries;
    private final double queryRadius;
    private transient PowerDistanceBoundaryOptimizer.Result rootModel;
    private transient boolean rootModelCaptured;

    public PowerDistanceLearnedPartitionMethod(List<? extends IndexObject> trainingQueries,
                                               double queryRadius)
    {
        this(new PowerDistanceLearningConfig(), trainingQueries, queryRadius);
    }

    public PowerDistanceLearnedPartitionMethod(PowerDistanceLearningConfig config,
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

    public PowerDistanceLearningConfig getConfig()
    {
        return config;
    }

    public PowerDistanceBoundaryOptimizer.Result getRootModel()
    {
        return rootModel;
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
                                      int first, int size,
                                      int numPartitions, int maxLS)
    {
        if (pivots.length != 2)
        {
            throw new IllegalArgumentException("learned power split requires exactly 2 pivots");
        }
        if (numPartitions != 2)
        {
            if (numPartitions < 2)
            {
                throw new IllegalArgumentException("learned power split requires at least 2 partitions");
            }
        }

        PowerDistanceBoundaryOptimizer optimizer = new PowerDistanceBoundaryOptimizer();
        PowerDistanceBoundaryOptimizer.Result model = optimizer.optimize(metric,
                pivots, data, first, size, trainingQueries, queryRadius, config,
                numPartitions);
        if (!rootModelCaptured)
        {
            rootModel = model;
            rootModelCaptured = true;
        }

        double[] thresholds = model.getThresholds();
        List<List<IndexObject>> partitions = new ArrayList<>(numPartitions);
        for (int i = 0; i < numPartitions; i++)
        {
            partitions.add(new ArrayList<>());
        }
        for (int i = 0; i < size; i++)
        {
            IndexObject x = data.get(first + i);
            double score = PowerDistanceBoundaryOptimizer.score(x, metric, pivots,
                    model.getRho(), config.getEpsilonDistance(),
                    model.getW1(), model.getW2());
            partitions.get(partitionIndex(score, thresholds)).add(x);
        }

        if (size > 1 && hasEmptyPartition(partitions))
        {
            List<List<? extends IndexObject>> subDataList = fallbackPartitions(data,
                    first, size, numPartitions);
            return new PowerDistancePartitionResults(subDataList, pivots,
                    model.getRho(), config.getEpsilonDistance(),
                    0.0, 0.0, new double[0], config.getComparisonEpsilon());
        }

        List<List<? extends IndexObject>> subDataList = new ArrayList<>(numPartitions);
        subDataList.addAll(partitions);
        return new PowerDistancePartitionResults(subDataList, pivots,
                model.getRho(), config.getEpsilonDistance(),
                model.getW1(), model.getW2(), thresholds,
                config.getComparisonEpsilon());
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
        return "PowerDistanceLearnedPartitionMethod{" +
                "config=" + config +
                ", queryRadius=" + queryRadius +
                '}';
    }
}
