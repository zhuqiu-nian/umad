package index.logdistance;

import index.logdistance.LogDistanceTransform.Interval;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LogDistanceTransformTest
{
    @Test
    public void transformsPositiveInterval()
    {
        LogDistanceTransform transform = new LogDistanceTransform(1e-12);

        Interval interval = transform.transformQueryInterval(1.0, 4.0);

        assertEquals(0.0, interval.getLow(), 1e-12);
        assertEquals(Math.log(4.0), interval.getHigh(), 1e-12);
    }

    @Test
    public void transformsZeroTouchingIntervalConservatively()
    {
        LogDistanceTransform transform = new LogDistanceTransform(1e-12);

        Interval touchesZero = transform.transformQueryInterval(0.0, 4.0);
        assertTrue(Double.isInfinite(touchesZero.getLow()));
        assertTrue(touchesZero.getLow() < 0.0);
        assertEquals(Math.log(4.0), touchesZero.getHigh(), 1e-12);

        Interval zeroOnly = transform.transformQueryInterval(0.0, 0.0);
        assertTrue(Double.isInfinite(zeroOnly.getLow()));
        assertEquals(Math.log(1e-12), zeroOnly.getHigh(), 1e-12);
    }

    @Test
    public void clipsPointDistanceAtEpsilon()
    {
        LogDistanceTransform transform = new LogDistanceTransform(1e-12);

        assertEquals(Math.log(1e-12), transform.transformPointDistance(0.0), 1e-12);
        assertEquals(Math.log(2.0), transform.transformPointDistance(2.0), 1e-12);
    }

    @Test
    public void computesLinearBoundsWithMixedWeights()
    {
        Interval bounds = LogDistanceTransform.linearScoreBounds(
                new double[]{1.0, -1.0},
                new Interval[]{new Interval(0.0, 2.0), new Interval(1.0, 3.0)});

        assertEquals(-3.0, bounds.getLow(), 1e-12);
        assertEquals(1.0, bounds.getHigh(), 1e-12);
    }

    @Test
    public void infiniteScoreBoundsRemainConservative()
    {
        Interval bounds = LogDistanceTransform.linearScoreBounds(
                new double[]{1.0, -1.0},
                new Interval[]{new Interval(Double.NEGATIVE_INFINITY, 2.0), new Interval(1.0, 3.0)});

        assertTrue(Double.isInfinite(bounds.getLow()));
        assertTrue(bounds.getLow() < 0.0);
        assertEquals(1.0, bounds.getHigh(), 1e-12);
    }
}
