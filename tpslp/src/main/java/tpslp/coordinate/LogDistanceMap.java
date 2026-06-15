package tpslp.coordinate;

import db.type.IndexObject;
import index.logdistance.LogDistanceTransform;
import metric.Metric;
import tpslp.geometry.Interval;

public final class LogDistanceMap implements CoordinateMap
{
    private static final long serialVersionUID = 1L;

    private final LogDistanceTransform transform;

    public LogDistanceMap()
    {
        this(LogDistanceTransform.DEFAULT_EPSILON_DISTANCE);
    }

    public LogDistanceMap(double epsilonDistance)
    {
        this.transform = new LogDistanceTransform(epsilonDistance);
    }

    public double getEpsilonDistance()
    {
        return transform.getEpsilonDistance();
    }

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
            coordinates[i] = transform.transformPointDistance(metric.getDistance(point, pivots[i]));
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
            LogDistanceTransform.Interval raw = transform.transformQueryInterval(
                    Math.max(0.0, queryToPivot - radius), queryToPivot + radius);
            intervals[i] = new Interval(raw.getLow(), raw.getHigh());
        }
        return intervals;
    }

    @Override
    public String name()
    {
        return "log";
    }
}
