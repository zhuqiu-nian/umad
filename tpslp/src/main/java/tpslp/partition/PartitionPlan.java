package tpslp.partition;

import db.type.IndexObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PartitionPlan implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final List<ChildPartition> children;

    public PartitionPlan(List<ChildPartition> children)
    {
        if (children == null)
        {
            throw new IllegalArgumentException("children must not be null");
        }
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    public List<ChildPartition> getChildren()
    {
        return children;
    }

    public static final class ChildPartition implements Serializable
    {
        private static final long serialVersionUID = 1L;

        private final List<IndexObject> data;
        private final NodeRegion region;

        public ChildPartition(List<IndexObject> data, NodeRegion region)
        {
            if (data == null || region == null)
            {
                throw new IllegalArgumentException("data and region must not be null");
            }
            this.data = Collections.unmodifiableList(new ArrayList<>(data));
            this.region = region;
        }

        public List<IndexObject> getData()
        {
            return data;
        }

        public NodeRegion getRegion()
        {
            return region;
        }
    }
}
