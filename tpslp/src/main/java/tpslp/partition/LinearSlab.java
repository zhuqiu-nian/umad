package tpslp.partition;

import tpslp.geometry.Interval;

import java.io.Serializable;

public final class LinearSlab implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final LinearBoundary boundary;
    private final Interval interval;

    public LinearSlab(LinearBoundary boundary, double low, double high)
    {
        this(boundary, new Interval(low, high));
    }

    public LinearSlab(LinearBoundary boundary, Interval interval)
    {
        if (boundary == null || interval == null)
        {
            throw new IllegalArgumentException("boundary and interval must not be null");
        }
        this.boundary = boundary;
        this.interval = interval;
    }

    public LinearBoundary getBoundary()
    {
        return boundary;
    }

    public Interval getInterval()
    {
        return interval;
    }

    public boolean contains(double[] coordinates)
    {
        double score = boundary.score(coordinates);
        return score >= interval.getLow() && score <= interval.getHigh();
    }

    public boolean intersects(Interval[] queryBox)
    {
        return boundary.project(queryBox).intersects(interval);
    }
}
