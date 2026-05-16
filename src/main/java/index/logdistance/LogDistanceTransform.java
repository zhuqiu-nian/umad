package index.logdistance;

/**
 * Utilities for the log-distance pivot transform Y = log(d).
 *
 * <p>Point distances are clipped by epsilon so pivots and duplicate points can
 * be placed at finite coordinates during tree construction. Query intervals
 * remain conservative: if the raw distance interval touches zero, the
 * transformed lower bound is negative infinity.</p>
 */
public class LogDistanceTransform
{
    public static final double DEFAULT_EPSILON_DISTANCE = 1e-12;
    public static final double DEFAULT_COMPARISON_EPSILON = 1e-12;

    private final double epsilonDistance;

    public LogDistanceTransform(double epsilonDistance)
    {
        if (!(epsilonDistance > 0.0))
        {
            throw new IllegalArgumentException("epsilonDistance must be positive");
        }
        this.epsilonDistance = epsilonDistance;
    }

    public double getEpsilonDistance()
    {
        return epsilonDistance;
    }

    public double transformPointDistance(double distance)
    {
        if (distance < 0.0)
        {
            throw new IllegalArgumentException("distance must be non-negative");
        }
        return Math.log(Math.max(distance, epsilonDistance));
    }

    public Interval transformQueryInterval(double rawLow, double rawHigh)
    {
        if (rawLow < 0.0 || rawHigh < rawLow)
        {
            throw new IllegalArgumentException("invalid raw interval: [" + rawLow + ", " + rawHigh + "]");
        }

        double low = rawLow == 0.0 ? Double.NEGATIVE_INFINITY : Math.log(rawLow);
        double high = Math.log(Math.max(rawHigh, epsilonDistance));
        return new Interval(low, high);
    }

    public static Interval queryDistanceInterval(double queryToPivotDistance, double radius)
    {
        double low = Math.max(0.0, queryToPivotDistance - radius);
        double high = queryToPivotDistance + radius;
        return new Interval(low, high);
    }

    public static Interval linearScoreBounds(double[] weights, Interval[] intervals)
    {
        if (weights.length != intervals.length)
        {
            throw new IllegalArgumentException("weights and intervals must have the same length");
        }

        double min = 0.0;
        double max = 0.0;
        for (int i = 0; i < weights.length; i++)
        {
            double weight = weights[i];
            if (weight == 0.0)
            {
                continue;
            }

            double termMin;
            double termMax;
            if (weight > 0.0)
            {
                termMin = weight * intervals[i].getLow();
                termMax = weight * intervals[i].getHigh();
            }
            else
            {
                termMin = weight * intervals[i].getHigh();
                termMax = weight * intervals[i].getLow();
            }

            min += termMin;
            max += termMax;
            if (Double.isNaN(min) || Double.isNaN(max))
            {
                return new Interval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            }
        }
        return new Interval(min, max);
    }

    public static final class Interval
    {
        private final double low;
        private final double high;

        public Interval(double low, double high)
        {
            if (Double.isNaN(low) || Double.isNaN(high))
            {
                throw new IllegalArgumentException("interval bound must not be NaN");
            }
            if (low > high)
            {
                throw new IllegalArgumentException("interval low must not exceed high: [" + low + ", " + high + "]");
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

        @Override
        public String toString()
        {
            return "[" + low + ", " + high + "]";
        }
    }
}
