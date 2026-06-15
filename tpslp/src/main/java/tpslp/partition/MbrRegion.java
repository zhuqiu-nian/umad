package tpslp.partition;

import tpslp.geometry.Interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MbrRegion implements NodeRegion
{
    private static final long serialVersionUID = 1L;

    private final List<Interval> intervals;

    public MbrRegion(List<Interval> intervals)
    {
        if (intervals == null || intervals.isEmpty())
        {
            throw new IllegalArgumentException("intervals must not be empty");
        }
        this.intervals = Collections.unmodifiableList(new ArrayList<>(intervals));
    }

    public List<Interval> getIntervals()
    {
        return intervals;
    }

    @Override
    public boolean contains(double[] coordinates)
    {
        if (coordinates.length != intervals.size())
        {
            throw new IllegalArgumentException("coordinate dimension mismatch");
        }
        for (int i = 0; i < intervals.size(); i++)
        {
            if (coordinates[i] < intervals.get(i).getLow()
                    || coordinates[i] > intervals.get(i).getHigh())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean intersects(Interval[] queryBox)
    {
        if (queryBox.length != intervals.size())
        {
            throw new IllegalArgumentException("query box dimension mismatch");
        }
        for (int i = 0; i < intervals.size(); i++)
        {
            if (!queryBox[i].intersects(intervals.get(i)))
            {
                return false;
            }
        }
        return true;
    }
}
