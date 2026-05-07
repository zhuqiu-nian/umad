package index.search;

import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.structure.GNATInternalNode;
import index.structure.Node;
import index.type.NodeSearchAction;
import manager.ObjectIOManager;
import metric.Metric;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * GNAT的搜索过程的实现类。其搜索依靠划分的时候存储的任意一个划分块到其他划分块的最小半径和最大半径。任意两个划分块之间判断能否排除对方划分块的依据
 * 是：
 *     如果[d(x,p)-r, d(x,p)+r]与range(p, Dq)没有交集，则可以排除q。
 *     其中，p是支撑点，x是查询点。
 */
public class GNATRangeCursor extends RangeCursor
{
    /**
     * 范围查询构造函数
     *
     * @param oiom        io读写对象
     * @param metric      搜索使用的距离函数
     * @param rootAddress 要搜索的索引树的根节点
     */
    public GNATRangeCursor(ObjectIOManager oiom, Metric metric, long rootAddress)
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
    public GNATRangeCursor(RangeQuery query, ObjectIOManager oiom, Metric metric, long rootAddress)
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
        if(!(node instanceof GNATInternalNode))
        {
            throw new RuntimeException("传入的节点必须是GNATInternalNode");
        }

        GNATInternalNode aNode = (GNATInternalNode)node;
        int numPivots = aNode.getNumPivots();
        NodeSearchAction[] actions = new NodeSearchAction[numPivots];
        //将所有孩子的搜索状态初始化为带搜索状态
        Arrays.fill(actions, NodeSearchAction.RESULTUNKNOWN);
        for (int i = 0; i < numPivots; i++)
        {
            double dis_i = queryToPivotDistance[i];
            double r_i = aNode.getRadius(i);
            double l_r_radius = r_i + radius;
            //使用数据块的最大半径排除
            if ((dis_i + r_i) < radius){
                actions[i] = NodeSearchAction.RESULTALL;
            }else if(dis_i > l_r_radius){
                actions[i] = NodeSearchAction.RESULTNONE;
            }
            //使用gnat的排除方案
            double[][] range = aNode.getChildPredicate(i);
            for (int j = 0; j < numPivots; j++)
            {
                if (i==j) continue;
                double i_l = queryToPivotDistance[i] - radius;
                double i_u = queryToPivotDistance[i] + radius;

                double j_l = range[0][j];
                double j_u = range[1][j];
                if (i_u < j_l || i_l> j_u) actions[j] = NodeSearchAction.RESULTNONE;
            }
        }
        return actions;
    }
}
