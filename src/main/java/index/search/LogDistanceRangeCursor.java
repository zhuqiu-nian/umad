package index.search;

import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.logdistance.LogDistanceTransform;
import index.logdistance.LogDistanceTransform.Interval;
import index.structure.LogDistanceInternalNode;
import index.structure.Node;
import index.type.NodeSearchAction;
import manager.ObjectIOManager;
import metric.Metric;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;

public class LogDistanceRangeCursor extends RangeCursor
{
    public LogDistanceRangeCursor(ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(oiom, metric, rootAddress);
    }

    public LogDistanceRangeCursor(RangeQuery query, ObjectIOManager oiom, Metric metric, long rootAddress)
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
        if (!(node instanceof LogDistanceInternalNode))
        {
            throw new RuntimeException("node must be LogDistanceInternalNode");
        }

        LogDistanceInternalNode ldNode = (LogDistanceInternalNode) node;
        NodeSearchAction[] actions = new NodeSearchAction[2];
        Arrays.fill(actions, NodeSearchAction.RESULTUNKNOWN);

        LogDistanceTransform transform = ldNode.getTransform();
        Interval[] intervals = new Interval[2];
        for (int i = 0; i < 2; i++)
        {
            Interval raw = LogDistanceTransform.queryDistanceInterval(queryToPivotDistance[i], radius);
            intervals[i] = transform.transformQueryInterval(raw.getLow(), raw.getHigh());
        }

        Interval scoreBounds = LogDistanceTransform.linearScoreBounds(ldNode.getWeights(), intervals);
        double tau = ldNode.getTau();
        double eps = ldNode.getComparisonEpsilon();

        if (scoreBounds.getHigh() < tau - eps)
        {
            actions[1] = NodeSearchAction.RESULTNONE;
        }
        else if (scoreBounds.getLow() > tau + eps)
        {
            actions[0] = NodeSearchAction.RESULTNONE;
        }

        return actions;
    }
}
