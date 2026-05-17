package algorithms.datapartition;

import db.type.IndexObject;
import index.logdistance.LogDistanceBoundaryOptimizer;
import index.logdistance.LogDistanceLearningConfig;
import index.structure.LogDistancePartitionResults;
import index.structure.PartitionResults;
import metric.Metric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LogDistanceLearnedPartitionMethod implements PartitionMethod, Serializable
{
    private static final long serialVersionUID = -4879150679835374653L;

    private final LogDistanceLearningConfig config;
    private transient List<? extends IndexObject> trainingQueries;
    private final double queryRadius;
    private transient LogDistanceBoundaryOptimizer.Result rootModel;
    private transient boolean rootModelCaptured;

    public LogDistanceLearnedPartitionMethod(List<? extends IndexObject> trainingQueries,
                                             double queryRadius)
    {
        this(new LogDistanceLearningConfig(), trainingQueries, queryRadius);
    }

    public LogDistanceLearnedPartitionMethod(LogDistanceLearningConfig config,
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

    public LogDistanceBoundaryOptimizer.Result getRootModel()
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
            throw new IllegalArgumentException("learned log split requires exactly 2 pivots");
        }
        if (numPartitions != 2)
        {
            throw new IllegalArgumentException("learned log split requires exactly 2 partitions");
        }

        LogDistanceBoundaryOptimizer optimizer = new LogDistanceBoundaryOptimizer();
        LogDistanceBoundaryOptimizer.Result model = optimizer.optimize(metric,
                pivots, data, first, size, trainingQueries, queryRadius, config);
        if (!rootModelCaptured)
        {
            rootModel = model;
            rootModelCaptured = true;
        }

        List<IndexObject> left = new ArrayList<>();
        List<IndexObject> right = new ArrayList<>();
        for (int i = 0; i < size; i++)
        {
            IndexObject x = data.get(first + i);
            double score = LogDistanceBoundaryOptimizer.score(x, metric, pivots,
                    config.getEpsilonDistance(), model.getW1(), model.getW2());
            if (score <= model.getTau())
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
                config.getEpsilonDistance(), model.getW1(), model.getW2(),
                model.getTau(), config.getComparisonEpsilon(),
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
}
