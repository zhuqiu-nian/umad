package tpslp.index;

import db.type.IndexObject;
import tpslp.partition.NodeRegion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TpslpNode implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final IndexObject[] pivots;
    private final List<IndexObject> leafData;
    private final List<TpslpNode> children;
    private final List<NodeRegion> childRegions;
    private final int dataSize;

    private TpslpNode(IndexObject[] pivots, List<IndexObject> leafData, List<TpslpNode> children,
                      List<NodeRegion> childRegions, int dataSize)
    {
        this.pivots = pivots == null ? null : pivots.clone();
        this.leafData = leafData == null ? null : Collections.unmodifiableList(new ArrayList<>(leafData));
        this.children = children == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(children));
        this.childRegions = childRegions == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(childRegions));
        this.dataSize = dataSize;
    }

    public static TpslpNode leaf(List<IndexObject> data)
    {
        return new TpslpNode(null, data, null, null, data.size());
    }

    public static TpslpNode internal(IndexObject[] pivots, List<TpslpNode> children,
                                     List<NodeRegion> regions, int dataSize)
    {
        if (pivots == null || pivots.length == 0)
        {
            throw new IllegalArgumentException("internal node pivots must not be empty");
        }
        if (children.size() != regions.size())
        {
            throw new IllegalArgumentException("children and regions must have the same size");
        }
        return new TpslpNode(pivots, null, children, regions, dataSize);
    }

    public boolean isLeaf()
    {
        return leafData != null;
    }

    public List<IndexObject> getLeafData()
    {
        return leafData;
    }

    public IndexObject[] getPivots()
    {
        return pivots == null ? null : pivots.clone();
    }

    public List<TpslpNode> getChildren()
    {
        return children;
    }

    public List<NodeRegion> getChildRegions()
    {
        return childRegions;
    }

    public int getDataSize()
    {
        return dataSize;
    }
}
