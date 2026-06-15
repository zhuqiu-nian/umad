package tpslp.prune;

import tpslp.geometry.Interval;
import tpslp.partition.NodeRegion;

public final class QueryBoxPruner implements IntersectionPruner
{
    private static final long serialVersionUID = 1L;

    @Override
    public boolean shouldVisit(Interval[] queryBox, NodeRegion region)
    {
        return region.intersects(queryBox);
    }
}
