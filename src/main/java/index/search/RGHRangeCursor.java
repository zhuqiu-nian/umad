package index.search;

import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.structure.RGHInternalNode;
import index.structure.Node;
import index.structure.LeafNode;
import index.type.NodeSearchAction;
import manager.ObjectIOManager;
import metric.Metric;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * RGH-Tree 范围搜索光标
 * 严格实现C++版本的剪枝逻辑 (RGHInternalNode.cpp)
 *
 * 剪枝逻辑 (三层IF结构):
 * 1. 左剪枝: (d(Q,Pl)-r)/(d(Q,Pr)+r) >= R => 只搜索右子树
 *    - IF1: 基本剪枝条件已满足
 *    - IF2: 预检查 (利用三角不等式估算下界)
 *    - IF3: 精确检查 (计算查询到右子节点pivots的距离)
 * 2. 右剪枝: d(Q,Pr) > r && (d(Q,Pl)+r)/(d(Q,Pr)-r) <= R => 只搜索左子树
 *    - 同样的三层检查
 * 3. 兜底: 两边都搜索
 */
public class RGHRangeCursor extends RangeCursor
{
    private static final double EPS = 1e-12;

    /**
     * 范围查询构造函数
     */
    public RGHRangeCursor(ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(oiom, metric, rootAddress);
    }

    /**
     * 范围查询构造函数
     */
    public RGHRangeCursor(RangeQuery query, ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(query, oiom, metric, rootAddress);
    }

    /**
     * RGH-Tree 的剪枝判断逻辑 (严格遵循C++版本 - 三层IF结构)
     */
    @Override
    public NodeSearchAction[] willTheSubTreeFurtherSearch(Node node, Metric metric, IndexObject query, double radius, double[] queryToPivotDistance, Hashtable<IndexObject, Double> memoryHashTable, LinkedList<DoubleIndexObjectPair> currentResult)
    {
        RGHInternalNode aNode = (RGHInternalNode) node;

        // 初始化标记数组 [left, right]
        NodeSearchAction[] nodeSearchActions = new NodeSearchAction[2];
        Arrays.fill(nodeSearchActions, NodeSearchAction.RESULTUNKNOWN);

        double d_q_pl = queryToPivotDistance[0];  // d(q, Pl)
        double d_q_pr = queryToPivotDistance[1];  // d(q, Pr)

        double splitRatio = aNode.getSplitRatio();   // R
        double[] childDistances = aNode.getChildDistances();

        // ===== 尝试剪枝左子树 (若成功则只搜右) =====
        // 逻辑: (d(Q,Pl)-r)/(d(Q,Pr)+r) > R
        // C++ 使用 >=，但其建树会把 ratio == R 的 E 集合平衡分到左右两侧；
        // 若查询正落在边界且 r=0，使用 >= 会漏掉被放在左侧的 E 点。
        // 因此这里在边界上保守搜索两边，只在明确偏右时剪掉左子树。
        if ((d_q_pl - radius) / (d_q_pr + radius) > splitRatio + EPS)
        {
            //左子树已经剪掉，看搜不搜右子树
            boolean searchRight = false;

            // 加载右子节点
            long rightChildAddress = aNode.getRightChild();
            Node rightChild = null;
            try {
                rightChild = (Node) oiom.readObject(rightChildAddress);
            } catch (Exception e) {
                // 如果读取失败，保守搜索
                searchRight = true;
            }

            if (rightChild != null && rightChild instanceof RGHInternalNode)
            {
                RGHInternalNode rightIntNode = (RGHInternalNode) rightChild;

                // 获取右子节点B的子节点C和D的覆盖半径
                // B的leftSubtreeRadius = C到B.Pl的最大距离
                // B的rightSubtreeRadius = D到B.Pr的最大距离
                double right_child_radius_l = rightIntNode.getLeftSubtreeRadius();
                double right_child_radius_r = rightIntNode.getRightSubtreeRadius();

                // childDistances[2] = d(Pr, V.right.Pl)
                // childDistances[3] = d(Pr, V.right.Pr)
                double checkValL = Math.abs(d_q_pr - childDistances[2]);
                double checkValR = Math.abs(d_q_pr - childDistances[3]);


                // ===== 第二层IF: 预检查 =====
                if (checkValL <= right_child_radius_l + radius + EPS ||
                    checkValR <= right_child_radius_r + radius + EPS)
                {

                    // ===== 第三层IF: 精确检查 =====
                    // 计算查询到右子节点pivots的距离。这里使用 memoryHashTable：
                    // 如果父层已经算过，则不重复调用 metric，也不重复添加结果。
                    double d_q_right_pl = getOrComputePivotDistance(metric, query, radius,
                            rightIntNode.getLeftPivot(), memoryHashTable, currentResult);
                    double d_q_right_pr = getOrComputePivotDistance(metric, query, radius,
                            rightIntNode.getRightPivot(), memoryHashTable, currentResult);

                    // 用子节点半径进行精确检查
                    if (d_q_right_pl <= right_child_radius_l + radius + EPS ||
                        d_q_right_pr <= right_child_radius_r + radius + EPS)
                    {
                        searchRight = true;
                    }
                }
            }
            else if (rightChild instanceof LeafNode)
            {
                // 如果是叶子节点，保守起见必须搜索
                searchRight = true;
            }
            else if (rightChildAddress > 0)
            {
                // 无法判断节点类型，保守搜索
                searchRight = true;
            }

            if (searchRight)
            {
                // 只搜索右子树
                nodeSearchActions[0] = NodeSearchAction.RESULTNONE;
                nodeSearchActions[1] = NodeSearchAction.RESULTUNKNOWN;
                return nodeSearchActions;
            }

            nodeSearchActions[0] = NodeSearchAction.RESULTNONE;
            nodeSearchActions[1] = NodeSearchAction.RESULTNONE;
            return nodeSearchActions;
        }

        // ===== 尝试剪枝右子树 (若成功则只搜左) =====
        // 逻辑: d(Q,Pr) > r && (d(Q,Pl)+r)/(d(Q,Pr)-r) < R
        // 与上面的边界处理对称：落在 splitRatio 边界时不能只搜左侧。
        else if (d_q_pr > radius && (d_q_pl + radius) / (d_q_pr - radius) < splitRatio - EPS)
        {
            boolean searchLeft = false;

            // 加载左子节点
            long leftChildAddress = aNode.getLeftChild();
            Node leftChild = null;
            try {
                leftChild = (Node) oiom.readObject(leftChildAddress);
            } catch (Exception e) {
                // 如果读取失败，保守搜索
                searchLeft = true;
            }

            if (leftChild != null && leftChild instanceof RGHInternalNode)
            {
                RGHInternalNode leftIntNode = (RGHInternalNode) leftChild;

                // 获取左子节点B的子节点C和D的覆盖半径
                // B的leftSubtreeRadius = C到B.Pl的最大距离
                // B的rightSubtreeRadius = D到B.Pr的最大距离
                double left_child_radius_l = leftIntNode.getLeftSubtreeRadius();
                double left_child_radius_r = leftIntNode.getRightSubtreeRadius();

                // childDistances[0] = d(Pl, V.left.Pl)
                // childDistances[1] = d(Pl, V.left.Pr)
                double checkValL = Math.abs(d_q_pl - childDistances[0]);
                double checkValR = Math.abs(d_q_pl - childDistances[1]);

                // ===== 第二层IF: 预检查 =====
                if (checkValL <= left_child_radius_l + radius + EPS ||
                    checkValR <= left_child_radius_r + radius + EPS)
                {

                    // ===== 第三层IF: 精确检查 =====
                    // 计算查询到左子节点pivots的距离，并缓存，避免递归到子节点后重复计数。
                    double d_q_left_pl = getOrComputePivotDistance(metric, query, radius,
                            leftIntNode.getLeftPivot(), memoryHashTable, currentResult);
                    double d_q_left_pr = getOrComputePivotDistance(metric, query, radius,
                            leftIntNode.getRightPivot(), memoryHashTable, currentResult);

                    // 用子节点半径进行精确检查
                    if (d_q_left_pl <= left_child_radius_l + radius + EPS ||
                        d_q_left_pr <= left_child_radius_r + radius + EPS)
                    {
                        searchLeft = true;
                    }
                }
            }
            else if (leftChild instanceof LeafNode)
            {
                // 如果是叶子节点，保守起见必须搜索
                searchLeft = true;
            }
            else if (leftChildAddress > 0)
            {
                // 无法判断节点类型，保守搜索
                searchLeft = true;
            }

            if (searchLeft)
            {
                // 只搜索左子树
                nodeSearchActions[0] = NodeSearchAction.RESULTUNKNOWN;
                nodeSearchActions[1] = NodeSearchAction.RESULTNONE;
                return nodeSearchActions;
            }

            nodeSearchActions[0] = NodeSearchAction.RESULTNONE;
            nodeSearchActions[1] = NodeSearchAction.RESULTNONE;
            return nodeSearchActions;
        }

        // ===== 兜底: 如果都不满足剪枝���件，两边都搜索 =====
        nodeSearchActions[0] = NodeSearchAction.RESULTUNKNOWN;
        nodeSearchActions[1] = NodeSearchAction.RESULTUNKNOWN;

        return nodeSearchActions;
    }

    private double getOrComputePivotDistance(Metric metric, IndexObject query, double radius,
                                             IndexObject pivot,
                                             Hashtable<IndexObject, Double> memoryHashTable,
                                             LinkedList<DoubleIndexObjectPair> currentResult)
    {
        if (memoryHashTable.containsKey(pivot))
        {
            return memoryHashTable.get(pivot);
        }

        double distance = metric.getDistance(query, pivot);
        memoryHashTable.put(pivot, distance);
        if (distance <= radius)
        {
            currentResult.add(new DoubleIndexObjectPair(distance, pivot));
        }
        return distance;
    }
}
