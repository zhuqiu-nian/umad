package index.search;

import db.type.IndexObject;
import db.type.DoubleIndexObjectPair;
import index.structure.GHInternalNode;
import index.structure.Node;
import index.type.NodeSearchAction;
import manager.ObjectIOManager;
import metric.Metric;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * GH树的范围搜索类，该类需要实现其父类方法{@link RangeCursor#willTheSubTreeFurtherSearch(Node, Metric, IndexObject, double, double[])}。
 */
public class GHRangeCursor extends RangeCursor
{
    /**
     * 范围查询构造函数
     *
     * @param oiom        io读写对象
     * @param metric      搜索使用的距离函数
     * @param rootAddress 要搜索的索引树的根节点
     */
    public GHRangeCursor(ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(oiom, metric, rootAddress);
    }

    /**
     * 范围查询构造函数
     *
     * @param query       查询对象
     * @param oiom        io读写对象
     * @param metric      搜索使用的距离函数
     * @param rootAddress 要搜索的索引树的根节点
     */
    public GHRangeCursor(RangeQuery query, ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(query, oiom, metric, rootAddress);
    }

    /**
     * {@link RangeCursor#internalSearch(Node, Hashtable)}调用该方法获取对每个孩子的处理方法。
     *
     * <p>
     * 通过枚举类{@link NodeSearchAction}标记{@link Node}的每个子节点的处理方法。
     * 如果{@code NodeSearchAction[i] == RESULTNONE}表示第i个孩子节点被剪枝，稍后的搜索不进入该孩子。
     * 如果{@code NodeSearchAction[i] == RESULTUNKNOWN}表示第i个孩子节点不能排除，稍后的搜索进入该孩子。
     * 如果{@code NodeSearchAction[i] == RESULTALL}表示第i个孩子节点的所有数据都应该被添加到结果集中。
     * </p>
     *
     * @param node                 需要搜索的节点
     * @param metric               距离函数
     * @param query                查询对象
     * @param radius               查询半径
     * @param queryToPivotDistance 查询对象到该节点的各个支撑点的距离
     * @return 标记孩子状态的数组
     * @see NodeSearchAction
     */
    @Override
    public NodeSearchAction[] willTheSubTreeFurtherSearch(Node node, Metric metric, IndexObject query, double radius, double[] queryToPivotDistance, Hashtable<IndexObject, Double> memoryHashTable, LinkedList<DoubleIndexObjectPair> currentResult)
    {
        if(!(node instanceof GHInternalNode))
        {
            throw new RuntimeException("传入的节点必须是GHInternalNode");
        }

        GHInternalNode aNode = (GHInternalNode)node;
        NodeSearchAction[] actions = new NodeSearchAction[2];
        //将所有孩子的搜索状态初始化为带搜索状态
        Arrays.fill(actions, NodeSearchAction.RESULTUNKNOWN);
        //如果d(q, p1) - d(q, p2) > 2*radius，那么可以排除左子树
        if(queryToPivotDistance[0] - queryToPivotDistance[1] > 2*radius)
            actions[0] = NodeSearchAction.RESULTNONE;
        //如果d(q, p2) - d(q, p1) > 2*radius，那么可以排除右子树
        if(queryToPivotDistance[1] - queryToPivotDistance[0] > 2*radius)
            actions[1] = NodeSearchAction.RESULTNONE;

        return actions;
    }
}
