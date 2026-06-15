package tpslp.index;

import db.type.DoubleIndexObjectPair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TpslpSearchStats implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int visitedInternalNodes;
    private int visitedLeafNodes;
    private int leafDistanceComputations;
    private int queryPivotDistanceComputations;
    private final List<DoubleIndexObjectPair> results = new ArrayList<>();

    void internalNodeVisited()
    {
        visitedInternalNodes++;
    }

    void leafNodeVisited()
    {
        visitedLeafNodes++;
    }

    void distanceComputed()
    {
        leafDistanceComputations++;
    }

    void queryPivotDistancesComputed(int count)
    {
        queryPivotDistanceComputations += count;
    }

    void addResult(DoubleIndexObjectPair result)
    {
        results.add(result);
    }

    public int getVisitedInternalNodes()
    {
        return visitedInternalNodes;
    }

    public int getVisitedLeafNodes()
    {
        return visitedLeafNodes;
    }

    public int getVisitedNodes()
    {
        return visitedInternalNodes + visitedLeafNodes;
    }

    public int getDistanceComputations()
    {
        return leafDistanceComputations;
    }

    public int getLeafDistanceComputations()
    {
        return leafDistanceComputations;
    }

    public int getQueryPivotDistanceComputations()
    {
        return queryPivotDistanceComputations;
    }

    public int getMetricDistanceComputations()
    {
        return queryPivotDistanceComputations + leafDistanceComputations;
    }

    public int getResultCount()
    {
        return results.size();
    }

    public List<DoubleIndexObjectPair> getResults()
    {
        return Collections.unmodifiableList(results);
    }
}
