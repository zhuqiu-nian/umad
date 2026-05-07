package index.structure;

import db.type.IndexObject;

/**
 * GH树的内部节点，该内部节点相对于{@link InternalNode}未存储额外的信息。
 */
public class GHInternalNode extends InternalNode
{
    private static final long serialVersionUID = 7782626353326161252L;

    /**
     * 构造函数（兼容序列化）
     */
    public GHInternalNode()
    {
        super();
    }

    /**
     * GH树内部节点的构造函数
     *
     * @param pivots 支撑点集合
     * @param size 以该节点为根的子树的数据总量
     * @param childAddresses 孩子节点的指针
     */
    public GHInternalNode(IndexObject[] pivots, int size, long[] childAddresses)
    {
        super(pivots, size, childAddresses);
    }
}
