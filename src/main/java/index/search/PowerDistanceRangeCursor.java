package index.search;

import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.powerdistance.PowerDistanceTransform;
import index.powerdistance.PowerDistanceTransform.Interval;
import index.structure.Node;
import index.structure.PowerDistanceInternalNode;
import index.type.NodeSearchAction;
import manager.ObjectIOManager;
import metric.Metric;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;

public class PowerDistanceRangeCursor extends RangeCursor
{
    public PowerDistanceRangeCursor(ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(oiom, metric, rootAddress);
    }

    public PowerDistanceRangeCursor(RangeQuery query, ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(query, oiom, metric, rootAddress);
    }

    @Override
    public NodeSearchAction[] willTheSubTreeFurtherSearch(Node node, Metric metric,
                                                          IndexObject query, double radius,
                                                          double[] queryToPivotDistance,
                                                          Hashtable<IndexObject, Double> memoryHashTable,
                                                          LinkedList<DoubleIndexObjectPair> currentResult)
    {
        if (!(node instanceof PowerDistanceInternalNode))
        {
            throw new RuntimeException("node must be PowerDistanceInternalNode");
        }

        PowerDistanceInternalNode pdNode = (PowerDistanceInternalNode) node;
        NodeSearchAction[] actions = new NodeSearchAction[pdNode.getNumChildren()];
        Arrays.fill(actions, NodeSearchAction.RESULTUNKNOWN);

        if (!pdNode.isPowerPruningEnabled())
        {
            return actions;
        }

        PowerDistanceTransform transform = pdNode.getTransform();
        Interval[] intervals = new Interval[2];
        for (int i = 0; i < 2; i++)
        {
            Interval raw = PowerDistanceTransform.queryDistanceInterval(queryToPivotDistance[i], radius);
            intervals[i] = transform.transformQueryInterval(raw.getLow(), raw.getHigh());
        }

        Interval scoreBounds = PowerDistanceTransform.linearScoreBounds(pdNode.getWeights(), intervals);
        double[] thresholds = pdNode.getThresholds();
        double eps = pdNode.getComparisonEpsilon();

        for (int i = 0; i < actions.length; i++)
        {
            double childLow = i == 0 ? Double.NEGATIVE_INFINITY : thresholds[i - 1];
            double childHigh = i == actions.length - 1 ? Double.POSITIVE_INFINITY : thresholds[i];
            boolean strictlyBelowChild = scoreBounds.getHigh() < childLow - eps;
            boolean strictlyAboveChild = scoreBounds.getLow() > childHigh + eps;
            if (strictlyBelowChild || strictlyAboveChild)
            {
                actions[i] = NodeSearchAction.RESULTNONE;
            }
        }

        return actions;
    }
}
