package tpslp.partition;

import db.type.IndexObject;
import metric.Metric;
import tpslp.coordinate.CoordinateMap;

import java.io.Serializable;
import java.util.List;

public interface PartitionLearner extends Serializable
{
    PartitionPlan learn(Metric metric, IndexObject[] pivots, CoordinateMap coordinateMap,
                        List<? extends IndexObject> data);
}
