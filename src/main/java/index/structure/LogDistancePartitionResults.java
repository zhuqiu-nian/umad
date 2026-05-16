package index.structure;

import db.type.IndexObject;

import java.util.List;

public class LogDistancePartitionResults extends PartitionResults
{
    private static final long serialVersionUID = 9208240423051548582L;

    private final double epsilonDistance;
    private final double w1;
    private final double w2;
    private final double tau;
    private final double comparisonEpsilon;

    public LogDistancePartitionResults(List<List<? extends IndexObject>> subDataList,
                                       IndexObject[] pivotSet,
                                       double epsilonDistance,
                                       double w1, double w2, double tau,
                                       double comparisonEpsilon)
    {
        super(subDataList, pivotSet);
        this.epsilonDistance = epsilonDistance;
        this.w1 = w1;
        this.w2 = w2;
        this.tau = tau;
        this.comparisonEpsilon = comparisonEpsilon;
    }

    public double getTau()
    {
        return tau;
    }

    @Override
    public InternalNode getInstanceOfInternalNode(IndexObject[] pivotSet, long[] childAddress)
    {
        return new LogDistanceInternalNode(pivotSet, getDataSize(), childAddress,
                epsilonDistance, w1, w2, tau, comparisonEpsilon);
    }

    @Override
    public String toString()
    {
        return "LogDistancePartitionResults{" +
                "epsilonDistance=" + epsilonDistance +
                ", w1=" + w1 +
                ", w2=" + w2 +
                ", tau=" + tau +
                ", comparisonEpsilon=" + comparisonEpsilon +
                ", leftSize=" + (listOfPartitions.size() > 0 ? listOfPartitions.get(0).size() : 0) +
                ", rightSize=" + (listOfPartitions.size() > 1 ? listOfPartitions.get(1).size() : 0) +
                '}';
    }
}
