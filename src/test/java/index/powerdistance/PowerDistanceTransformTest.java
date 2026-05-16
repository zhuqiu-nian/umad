package index.powerdistance;

import index.powerdistance.PowerDistanceTransform.Interval;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PowerDistanceTransformTest
{
    @Test
    public void transformsPositiveRhoInterval()
    {
        PowerDistanceTransform transform = new PowerDistanceTransform(2.0, 1e-12);

        Interval interval = transform.transformQueryInterval(1.0, 3.0);

        assertEquals(1.0, interval.getLow(), 1e-12);
        assertEquals(9.0, interval.getHigh(), 1e-12);
    }

    @Test
    public void transformsNegativeRhoIntervalConservatively()
    {
        PowerDistanceTransform transform = new PowerDistanceTransform(-2.0, 1e-12);

        Interval interval = transform.transformQueryInterval(1.0, 4.0);
        assertEquals(1.0 / 16.0, interval.getLow(), 1e-12);
        assertEquals(1.0, interval.getHigh(), 1e-12);

        Interval touchesZero = transform.transformQueryInterval(0.0, 4.0);
        assertEquals(1.0 / 16.0, touchesZero.getLow(), 1e-12);
        assertTrue(Double.isInfinite(touchesZero.getHigh()));

        Interval zeroOnly = transform.transformQueryInterval(0.0, 0.0);
        assertEquals(Math.pow(1e-12, -2.0), zeroOnly.getLow(), Math.pow(1e-12, -2.0) * 1e-12);
        assertTrue(Double.isInfinite(zeroOnly.getHigh()));
    }

    @Test
    public void computesLinearBoundsWithMixedWeights()
    {
        Interval bounds = PowerDistanceTransform.linearScoreBounds(
                new double[]{1.0, -1.0},
                new Interval[]{new Interval(1.0, 4.0), new Interval(2.0, 8.0)});

        assertEquals(-7.0, bounds.getLow(), 1e-12);
        assertEquals(2.0, bounds.getHigh(), 1e-12);
    }
}
