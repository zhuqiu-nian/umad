package tpslp.prune;

import tpslp.geometry.Interval;
import tpslp.partition.NodeRegion;

import java.io.Serializable;

public interface IntersectionPruner extends Serializable
{
    boolean shouldVisit(Interval[] queryBox, NodeRegion region);
}
