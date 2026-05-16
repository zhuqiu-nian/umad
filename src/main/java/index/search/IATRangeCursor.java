package index.search;

import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.structure.IATInternalNode;
import index.structure.LeafNode;
import index.structure.Node;
import index.type.NodeSearchAction;
import manager.ObjectIOManager;
import metric.Metric;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * Range cursor for IAT.
 */
public class IATRangeCursor extends RangeCursor
{
    private static final double EPS = 1e-12;

    public IATRangeCursor(ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(oiom, metric, rootAddress);
    }

    public IATRangeCursor(RangeQuery query, ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(query, oiom, metric, rootAddress);
    }

    @Override
    public NodeSearchAction[] willTheSubTreeFurtherSearch(Node node, Metric metric, IndexObject query,
                                                          double radius, double[] queryToPivotDistance,
                                                          Hashtable<IndexObject, Double> memoryHashTable,
                                                          LinkedList<DoubleIndexObjectPair> currentResult)
    {
        IATInternalNode aNode = (IATInternalNode) node;
        NodeSearchAction[] actions = new NodeSearchAction[3];
        Arrays.fill(actions, NodeSearchAction.RESULTUNKNOWN);

        double d1 = queryToPivotDistance[0];
        double d2 = queryToPivotDistance[1];
        double c1Ratio = aNode.getC1Ratio();
        double c2Ratio = aNode.getC2Ratio();

        boolean fullLeft = d1 + aNode.getM1() <= radius + EPS;
        boolean fullRight = d2 + aNode.getM2() <= radius + EPS;
        boolean pruneLeft = d1 - c1Ratio * d2 > (c1Ratio + 1) * radius + EPS;
        boolean pruneRight = c2Ratio * d2 - d1 > (c2Ratio + 1) * radius + EPS;
        boolean onlyLeft = c1Ratio * d2 - d1 >= (c1Ratio + 1) * radius - EPS;
        boolean onlyRight = d1 - c2Ratio * d2 >= (c2Ratio + 1) * radius - EPS;

        if (fullLeft)
        {
            actions[0] = NodeSearchAction.RESULTALL;
        }
        else if (pruneLeft || onlyRight)
        {
            actions[0] = NodeSearchAction.RESULTNONE;
        }

        actions[1] = (!onlyLeft && !onlyRight) ? NodeSearchAction.RESULTUNKNOWN : NodeSearchAction.RESULTNONE;

        if (fullRight)
        {
            actions[2] = NodeSearchAction.RESULTALL;
        }
        else if (pruneRight || onlyLeft)
        {
            actions[2] = NodeSearchAction.RESULTNONE;
        }

        for (int i = 0; i < actions.length; i++)
        {
            if (actions[i] == NodeSearchAction.RESULTUNKNOWN)
            {
                actions[i] = applyCurrentPivotCover(aNode, i, radius, queryToPivotDistance);
            }
            if (actions[i] == NodeSearchAction.RESULTUNKNOWN)
            {
                actions[i] = applyChildPivotPrecheck(aNode, i, metric, query, radius,
                        queryToPivotDistance, memoryHashTable, currentResult);
            }
        }

        return actions;
    }

    private NodeSearchAction applyCurrentPivotCover(IATInternalNode node, int childIndex,
                                                    double radius, double[] queryToPivotDistance)
    {
        double[] cover = node.getChildCoverRadii(childIndex);
        if (queryToPivotDistance[0] + cover[0] <= radius + EPS ||
            queryToPivotDistance[1] + cover[1] <= radius + EPS)
        {
            return NodeSearchAction.RESULTALL;
        }
        if (queryToPivotDistance[0] - cover[0] > radius + EPS ||
            queryToPivotDistance[1] - cover[1] > radius + EPS)
        {
            return NodeSearchAction.RESULTNONE;
        }
        return NodeSearchAction.RESULTUNKNOWN;
    }

    private NodeSearchAction applyChildPivotPrecheck(IATInternalNode node, int childIndex,
                                                     Metric metric, IndexObject query, double radius,
                                                     double[] queryToPivotDistance,
                                                     Hashtable<IndexObject, Double> memoryHashTable,
                                                     LinkedList<DoubleIndexObjectPair> currentResult)
    {
        double[][] childDistances = node.getChildPivotDistances(childIndex);
        double[] childRadii = node.getChildSubtreeRadii(childIndex);
        if (!hasMetadata(childDistances, childRadii))
        {
            return NodeSearchAction.RESULTUNKNOWN;
        }

        for (int childPivot = 0; childPivot < 2; childPivot++)
        {
            double lowerBound = Math.max(
                    Math.abs(queryToPivotDistance[0] - childDistances[0][childPivot]),
                    Math.abs(queryToPivotDistance[1] - childDistances[1][childPivot]));
            if (lowerBound > childRadii[childPivot] + radius + EPS)
            {
                return NodeSearchAction.RESULTNONE;
            }
        }

        Node childNode = readChild(node, childIndex);
        if (childNode == null || childNode.getNumPivots() < 2)
        {
            return NodeSearchAction.RESULTUNKNOWN;
        }
        if (childNode instanceof LeafNode && childNode.getPivotOf(0).equals(node.getC1())
                && childNode.getPivotOf(1).equals(node.getC2()))
        {
            return NodeSearchAction.RESULTUNKNOWN;
        }

        boolean full = false;
        for (int childPivot = 0; childPivot < 2; childPivot++)
        {
            double exact = getOrComputePivotDistance(metric, query, radius,
                    childNode.getPivotOf(childPivot), memoryHashTable, currentResult);
            if (exact > childRadii[childPivot] + radius + EPS)
            {
                return NodeSearchAction.RESULTNONE;
            }
            if (exact + childRadii[childPivot] <= radius + EPS)
            {
                full = true;
            }
        }

        return full ? NodeSearchAction.RESULTALL : NodeSearchAction.RESULTUNKNOWN;
    }

    private boolean hasMetadata(double[][] childDistances, double[] childRadii)
    {
        return childDistances != null && childDistances.length >= 2 &&
                childDistances[0].length >= 2 && childDistances[1].length >= 2 &&
                childRadii != null && childRadii.length >= 2 &&
                childDistances[0][0] >= 0.0 && childDistances[0][1] >= 0.0 &&
                childDistances[1][0] >= 0.0 && childDistances[1][1] >= 0.0 &&
                childRadii[0] >= 0.0 && childRadii[1] >= 0.0;
    }

    private Node readChild(IATInternalNode node, int childIndex)
    {
        try
        {
            return (Node) oiom.readObject(node.getChildOf(childIndex));
        }
        catch (Exception e)
        {
            return null;
        }
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
