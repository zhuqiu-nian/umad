package tpslp;

import db.type.DoubleVector;
import db.type.IndexObject;
import metric.LMetric;
import metric.Metric;
import org.junit.Test;
import tpslp.coordinate.LogDistanceMap;
import tpslp.coordinate.PowerDistanceMap;
import tpslp.coordinate.PivotSpaceMap;
import tpslp.geometry.Interval;
import tpslp.partition.LinearBoundary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TpslpCoordinateMapTest
{
    private final Metric metric = LMetric.EuclideanDistanceMetric;
    private final IndexObject p0 = new DoubleVector(null, 1, new double[]{0.0, 0.0});
    private final IndexObject p1 = new DoubleVector(null, 2, new double[]{2.0, 0.0});
    private final IndexObject q = new DoubleVector(null, 3, new double[]{1.0, 0.0});

    @Test
    public void pivotSpaceMapBuildsTriangleInequalityQueryBox()
    {
        Interval[] box = new PivotSpaceMap().mapQuery(metric, new IndexObject[]{p0, p1}, q, 0.25);

        assertEquals(0.75, box[0].getLow(), 1e-12);
        assertEquals(1.25, box[0].getHigh(), 1e-12);
        assertEquals(0.75, box[1].getLow(), 1e-12);
        assertEquals(1.25, box[1].getHigh(), 1e-12);
    }

    @Test
    public void logDistanceMapUsesConservativeZeroLowerBound()
    {
        Interval[] box = new LogDistanceMap().mapQuery(metric, new IndexObject[]{p0}, p0, 0.5);

        assertEquals(Double.NEGATIVE_INFINITY, box[0].getLow(), 0.0);
        assertEquals(Math.log(0.5), box[0].getHigh(), 1e-12);
    }

    @Test
    public void powerDistanceMapHandlesNegativeRho()
    {
        Interval[] box = new PowerDistanceMap(-1.0).mapQuery(metric, new IndexObject[]{p0}, p0, 0.5);

        assertEquals(2.0, box[0].getLow(), 1e-12);
        assertEquals(Double.POSITIVE_INFINITY, box[0].getHigh(), 0.0);
    }

    @Test
    public void linearBoundaryProjectsMixedSignWeights()
    {
        LinearBoundary boundary = new LinearBoundary(2.0, -1.0, 0.0);
        Interval projection = boundary.project(new Interval[]{
                new Interval(1.0, 3.0),
                new Interval(4.0, 5.0),
                new Interval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        });

        assertEquals(-3.0, projection.getLow(), 1e-12);
        assertEquals(2.0, projection.getHigh(), 1e-12);
        assertTrue(projection.intersects(new Interval(1.5, 9.0)));
    }
}
