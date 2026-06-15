package tpslp.partition;

import tpslp.geometry.Interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LinearSlabRegion implements NodeRegion
{
    private static final long serialVersionUID = 1L;

    private final List<LinearSlab> slabs;

    public LinearSlabRegion(List<LinearSlab> slabs)
    {
        if (slabs == null || slabs.isEmpty())
        {
            throw new IllegalArgumentException("slabs must not be empty");
        }
        this.slabs = Collections.unmodifiableList(new ArrayList<>(slabs));
    }

    public List<LinearSlab> getSlabs()
    {
        return slabs;
    }

    @Override
    public boolean contains(double[] coordinates)
    {
        for (LinearSlab slab : slabs)
        {
            if (!slab.contains(coordinates))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean intersects(Interval[] queryBox)
    {
        for (LinearSlab slab : slabs)
        {
            if (!slab.intersects(queryBox))
            {
                return false;
            }
        }
        return true;
    }
}
