package tpslp.partition;

import tpslp.geometry.Interval;

import java.io.Serializable;
import java.util.Arrays;

public final class LinearBoundary implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final double[] weights;

    public LinearBoundary(double... weights)
    {
        if (weights == null || weights.length == 0)
        {
            throw new IllegalArgumentException("weights must not be empty");
        }
        boolean nonZero = false;
        for (double weight : weights)
        {
            if (Double.isNaN(weight))
            {
                throw new IllegalArgumentException("weight must not be NaN");
            }
            nonZero |= weight != 0.0;
        }
        if (!nonZero)
        {
            throw new IllegalArgumentException("at least one weight must be non-zero");
        }
        this.weights = weights.clone();
    }

    public int dimension()
    {
        return weights.length;
    }

    public double[] getWeights()
    {
        return weights.clone();
    }

    public double score(double[] coordinates)
    {
        if (coordinates.length != weights.length)
        {
            throw new IllegalArgumentException("coordinate dimension mismatch");
        }
        double score = 0.0;
        for (int i = 0; i < weights.length; i++)
        {
            score += term(weights[i], coordinates[i]);
        }
        return sanitize(score);
    }

    public Interval project(Interval[] intervals)
    {
        if (intervals.length != weights.length)
        {
            throw new IllegalArgumentException("interval dimension mismatch");
        }
        double low = 0.0;
        double high = 0.0;
        for (int i = 0; i < weights.length; i++)
        {
            double weight = weights[i];
            if (weight == 0.0)
            {
                continue;
            }
            if (weight > 0.0)
            {
                low += term(weight, intervals[i].getLow());
                high += term(weight, intervals[i].getHigh());
            }
            else
            {
                low += term(weight, intervals[i].getHigh());
                high += term(weight, intervals[i].getLow());
            }
        }
        if (Double.isNaN(low) || Double.isNaN(high))
        {
            return new Interval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        }
        return new Interval(low, high);
    }

    private double term(double weight, double value)
    {
        if (weight == 0.0)
        {
            return 0.0;
        }
        return weight * value;
    }

    private double sanitize(double value)
    {
        if (Double.isNaN(value))
        {
            return 0.0;
        }
        return value;
    }

    @Override
    public String toString()
    {
        return "LinearBoundary" + Arrays.toString(weights);
    }
}
