package tpslp.coordinate;

import db.type.IndexObject;
import metric.Metric;
import tpslp.geometry.Interval;

import java.io.Serializable;

public interface CoordinateMap extends Serializable
{
    int dimension(int pivotCount);

    double[] mapPoint(Metric metric, IndexObject[] pivots, IndexObject point);

    Interval[] mapQuery(Metric metric, IndexObject[] pivots, IndexObject query, double radius);

    String name();
}
