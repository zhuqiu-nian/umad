package tpslp.coordinate;

import db.type.IndexObject;
import index.powerdistance.PowerDistanceTransform;
import metric.Metric;
import tpslp.geometry.Interval;

public final class PowerDistanceMap implements CoordinateMap
{
    private static final long serialVersionUID = 1L;

    private final PowerDistanceTransform transform;

    public PowerDistanceMap(double rho)
    {
        this(rho, PowerDistanceTransform.DEFAULT_EPSILON_DISTANCE);
    }

    public PowerDistanceMap(double rho, double epsilonDistance)
    {
        this.transform = new PowerDistanceTransform(rho, epsilonDistance);
    }

    public double getRho()
    {
        return transform.getRho();
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
            PowerDistanceTransform.Interval raw = transform.transformQueryInterval(
                    Math.max(0.0, queryToPivot - radius), queryToPivot + radius);
            intervals[i] = new Interval(raw.getLow(), raw.getHigh());
        }
        return intervals;
    }

    @Override
    public String name()
    {
        return "power";
    }
}
