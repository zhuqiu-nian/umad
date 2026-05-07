package index.search;

import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.structure.InternalNode;
import index.structure.LeafNode;
import index.structure.Node;
import index.structure.VPInternalNode;
import index.type.NodeSearchAction;
import manager.ObjectIOManager;
import metric.CountedMetric;
import metric.Metric;
import util.Debug;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * VP树范围搜索的实体类，该类继承抽象类{@link RangeCursor}实现父类的抽象方法{@link RangeCursor#willTheSubTreeFurtherSearch(Node, Metric, IndexObject, double, double[])}
 * 通过该抽象方法判断搜索的时候对于每一个内部节点的扇出的处理方法。
 */
public class VPRangeCursor extends RangeCursor
{
    /**
     * 范围查询构造函数
     *
     * @param oiom        io读写对象
     * @param metric      搜索使用的距离函数
     * @param rootAddress 要搜索的索引树的根节点
     */
    public VPRangeCursor(ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(oiom, metric, rootAddress);
    }

    /**
     * {@link VPRangeCursor}类的构造函数
     *
     * @param query       查询对象
     * @param oiom        io读写对象
     * @param metric      搜索使用的距离函数
     * @param rootAddress 要搜索的索引树的根节点
     */
    public VPRangeCursor(RangeQuery query, ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(query, oiom, metric, rootAddress);
    }

    /**
     * {@link RangeCursor#internalSearch(Node, Hashtable)}调用该方法获取对每个孩子的处理方法。
     *
     * <p>
     * 不同种类的索引树实现该方法，通过枚举类{@link NodeSearchAction}标记{@link Node}的每个子节点的处理方法。
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

        VPInternalNode aNode = (VPInternalNode) node;
        //初始化标记数组
        NodeSearchAction[] nodeSearchActions = new NodeSearchAction[aNode.getNumChildren()];
        Arrays.fill(nodeSearchActions, NodeSearchAction.RESULTUNKNOWN);

        //逐个判断每个扇出的处理方法
        for (int i = 0; i < aNode.getNumChildren(); i++)
        {
            //拿到该孩子内存储的数据到每个支撑点距离的上下界限
            double[][] range = aNode.getChildPredicate(i);
             int cuntNew = 0;
            //遍历每个支撑点，根据查询对象到支撑点的距离、搜索半径以及孩子的上下界标记孩子的状态
            for (int j = 0; j < aNode.getNumPivots(); j++)
            {
                //当一个支撑点满足 dis(q, p_j) + 第i个扇出的数据到第j个支撑点的上界 <= 查询半径， 那么第i个扇出的所有数据都在查询范围内
                if (range[1][j] + queryToPivotDistance[j] <= radius)
                {
                    nodeSearchActions[i] = NodeSearchAction.RESULTALL;
                    break;
                }

                //当一个支撑点满足 dis(q, p_j) + 查询半径 < 第i个扇出的数据到第j个支撑点的下界， 那么第i个扇出的所有数据都不在查询范围内
                //当一个支撑点满足 dis(q, p_j) - 查询半径 > 第i个扇出的数据到第j个支撑点的上界， 那么第i个扇出的所有数据都不在查询范围内
                if (queryToPivotDistance[j] + radius < range[0][j] || queryToPivotDistance[j] - radius > range[1][j])
                {
                    nodeSearchActions[i] = NodeSearchAction.RESULTNONE;
                    break;
                }
            }
        }
        return nodeSearchActions;
    }
}
