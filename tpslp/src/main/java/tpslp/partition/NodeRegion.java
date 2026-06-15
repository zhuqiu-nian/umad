package tpslp.partition;

import tpslp.geometry.Interval;

import java.io.Serializable;

public interface NodeRegion extends Serializable
{
    boolean contains(double[] coordinates);

    boolean intersects(Interval[] queryBox);
}
