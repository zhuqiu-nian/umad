package tpslp.geometry;

import java.io.Serializable;

public final class Interval implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final double low;
    private final double high;

    public Interval(double low, double high)
    {
        if (Double.isNaN(low) || Double.isNaN(high))
        {
            throw new IllegalArgumentException("interval bounds must not be NaN");
        }
        if (low > high)
        {
            throw new IllegalArgumentException("low must not exceed high");
        }
        this.low = low;
        this.high = high;
    }

    public double getLow()
    {
        return low;
    }

    public double getHigh()
    {
        return high;
    }

    public boolean intersects(Interval other)
    {
        return Math.max(low, other.low) <= Math.min(high, other.high);
    }

    @Override
    public String toString()
    {
        return "[" + low + ", " + high + "]";
    }
}
