package index.search;

import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.structure.ApollonianInternalNode;
import index.structure.Node;
import index.type.NodeSearchAction;
import manager.ObjectIOManager;
import metric.Metric;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * Apollonian Tree 范围搜索的实体类
 * 严格实现三个定理的剪枝逻辑
 *
 * 定理1-1 (定位): 使用 >= 判断数据点必然在左/右子树
 * 定理1-2 (剪枝): 使用 > 判断可以剪枝
 * 定理1-3 (全取): 使用 <= 判断可完全获取
 */
public class ApollonianRangeCursor extends RangeCursor
{
    /**
     * 范围查询构造函数
     */
    public ApollonianRangeCursor(ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(oiom, metric, rootAddress);
    }

    /**
     * 范围查询构造函数
     */
    public ApollonianRangeCursor(RangeQuery query, ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(query, oiom, metric, rootAddress);
    }

    /**
     * Apollonian Tree 的剪枝判断逻辑
     *
     * 严格遵循三个定理：
     * 定理1-1: c1*d2-d1 >= (c1+1)r => 必然在左子树
     *         d1-c2*d2 >= (c2+1)r => 必然在右子树
     * 定理1-2: d1-c1*d2 > (c1+1)r => 不在左子树
     *         c2*d2-d1 > (c2+1)r => 不在右子树
     * 定理1-3: d1+M1 <= r => 左子树全取
     *         d2+M2 <= r => 右子树全取
     */
    @Override
    public NodeSearchAction[] willTheSubTreeFurtherSearch(Node node, Metric metric, IndexObject query, double radius, double[] queryToPivotDistance, Hashtable<IndexObject, Double> memoryHashTable, LinkedList<DoubleIndexObjectPair> currentResult)
    {
        ApollonianInternalNode aNode = (ApollonianInternalNode) node;

        // 初始化标记数组 [left, mid, right]
        NodeSearchAction[] nodeSearchActions = new NodeSearchAction[3];
        Arrays.fill(nodeSearchActions, NodeSearchAction.RESULTUNKNOWN);

        double d1 = queryToPivotDistance[0];  // d(q, c1)
        double d2 = queryToPivotDistance[1];  // d(q, c2)

        double c1_ratio = aNode.getC1Ratio();  // c1
        double c2_ratio = aNode.getC2Ratio();  // c2
        double M1 = aNode.getM1();  // 左子树到 c1 的最大距离
        double M2 = aNode.getM2();  // 右子树到 c2 的最大距离

        // ===== 定理1-3: 全取条件 (使用 <=) =====
        boolean fullLeft = (d1 + M1 <= radius);
        boolean fullRight = (d2 + M2 <= radius);

        // ===== 定理1-2: 剪枝条件 (使用 >) =====
        boolean pruneLeft = (d1 - c1_ratio * d2 > (c1_ratio + 1) * radius);
        boolean pruneRight = (c2_ratio * d2 - d1 > (c2_ratio + 1) * radius);

        // ===== 定理1-1: 定位条件 (使用 >=) =====
        boolean onlyLeft = (c1_ratio * d2 - d1 >= (c1_ratio + 1) * radius);   // 必然在左子树
        boolean onlyRight = (d1 - c2_ratio * d2 >= (c2_ratio + 1) * radius);  // 必然在右子树

        // ===== Left 分支 =====
        if (fullLeft) {
            nodeSearchActions[0] = NodeSearchAction.RESULTALL;
        }
        else if (!pruneLeft && (onlyLeft || !onlyRight)) {
            nodeSearchActions[0] = NodeSearchAction.RESULTUNKNOWN;  // 需要搜索
        }
        else {
            nodeSearchActions[0] = NodeSearchAction.RESULTNONE;  // 剪枝
        }

        // ===== Mid 分支 =====
        // 中间分支无法用定理1-1/1-2/1-3直接判断，只有当 not onlyLeft and not onlyRight 时才搜索
        if (!onlyLeft && !onlyRight) {
            nodeSearchActions[1] = NodeSearchAction.RESULTUNKNOWN;  // 需要搜索
        }
        else {
            nodeSearchActions[1] = NodeSearchAction.RESULTNONE;  // 跳过
        }

        // ===== Right 分支 =====
        if (fullRight) {
            nodeSearchActions[2] = NodeSearchAction.RESULTALL;
        }
        else if (!pruneRight && (onlyRight || !onlyLeft)) {
            nodeSearchActions[2] = NodeSearchAction.RESULTUNKNOWN;  // 需要搜索
        }
        else {
            nodeSearchActions[2] = NodeSearchAction.RESULTNONE;  // 剪枝
        }

        return nodeSearchActions;
    }
}