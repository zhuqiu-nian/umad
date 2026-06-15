package tpslp;

import db.type.DoubleVector;
import db.type.IndexObject;
import metric.LMetric;
import metric.Metric;
import org.junit.Test;
import tpslp.coordinate.PivotSpaceMap;
import tpslp.partition.BoundaryFactories;
import tpslp.partition.LinearSlabPartitionLearner;
import tpslp.partition.PartitionPlan;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TpslpPartitionLearnerTest
{
    @Test
    public void generatedRegionsContainTheirChildPoints()
    {
        Metric metric = LMetric.EuclideanDistanceMetric;
        List<IndexObject> data = sampleData();
        IndexObject[] pivots = new IndexObject[]{data.get(0), data.get(1)};
        PivotSpaceMap map = new PivotSpaceMap();
        LinearSlabPartitionLearner learner = new LinearSlabPartitionLearner(
                BoundaryFactories.cght(), 2);

        PartitionPlan plan = learner.learn(metric, pivots, map, data);

        for (PartitionPlan.ChildPartition child : plan.getChildren())
        {
            for (IndexObject point : child.getData())
            {
                assertTrue(child.getRegion().contains(map.mapPoint(metric, pivots, point)));
            }
        }
    }

    private List<IndexObject> sampleData()
    {
        List<IndexObject> data = new ArrayList<>();
        for (int i = 0; i < 24; i++)
        {
            data.add(new DoubleVector(null, i, new double[]{i / 6.0, (i % 6) / 5.0}));
        }
        return data;
    }
}
