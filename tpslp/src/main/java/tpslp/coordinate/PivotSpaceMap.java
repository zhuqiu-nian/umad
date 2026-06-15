package tpslp.coordinate;

import db.type.IndexObject;
import metric.Metric;
import tpslp.geometry.Interval;

/**
 * The standard pivot-space coordinate map.
 */
public final class PivotSpaceMap implements CoordinateMap
{
    private static final long serialVersionUID = 1L;

    @Override
    public int dimension(int pivotCount)
    {
        return pivotCount;
    }

    @Override
    public double[] mapPoint(Metric metric, IndexObject[] pivots, IndexObject point)
    {
        double[] coordinates = new double[pivots.length];
        for (int i = 0; i < pivots.length; i++)
        {
            coordinates[i] = metric.getDistance(point, pivots[i]);
        }
        return coordinates;
    }

    @Override
    public Interval[] mapQuery(Metric metric, IndexObject[] pivots, IndexObject query, double radius)
    {
        if (radius < 0.0)
        {
            throw new IllegalArgumentException("radius must be non-negative");
        }
        Interval[] intervals = new Interval[pivots.length];
        for (int i = 0; i < pivots.length; i++)
        {
            double queryToPivot = metric.getDistance(query, pivots[i]);
            intervals[i] = new Interval(Math.max(0.0, queryToPivot - radius),
                    queryToPivot + radius);
        }
        return intervals;
    }

    @Override
    public String name()
    {
        return "pivot-space";
    }
}
