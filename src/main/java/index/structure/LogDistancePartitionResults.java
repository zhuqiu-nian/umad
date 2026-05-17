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
    private final double[][][] childPivotDistanceRanges;

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
        this.childPivotDistanceRanges = new double[0][][];
    }

    public LogDistancePartitionResults(List<List<? extends IndexObject>> subDataList,
                                       IndexObject[] pivotSet,
                                       double epsilonDistance,
                                       double w1, double w2, double tau,
                                       double comparisonEpsilon,
                                       double[][][] childPivotDistanceRanges)
    {
        super(subDataList, pivotSet);
        this.epsilonDistance = epsilonDistance;
        this.w1 = w1;
        this.w2 = w2;
        this.tau = tau;
        this.comparisonEpsilon = comparisonEpsilon;
        this.childPivotDistanceRanges = cloneRanges(childPivotDistanceRanges);
    }

    public double getTau()
    {
        return tau;
    }

    @Override
    public InternalNode getInstanceOfInternalNode(IndexObject[] pivotSet, long[] childAddress)
    {
        return new LogDistanceInternalNode(pivotSet, getDataSize(), childAddress,
                epsilonDistance, w1, w2, tau, comparisonEpsilon,
                childPivotDistanceRanges);
    }

    private double[][][] cloneRanges(double[][][] ranges)
    {
        if (ranges == null)
        {
            return new double[0][][];
        }
        double[][][] copy = new double[ranges.length][][];
        for (int child = 0; child < ranges.length; child++)
        {
            copy[child] = new double[ranges[child].length][];
            for (int pivot = 0; pivot < ranges[child].length; pivot++)
            {
                copy[child][pivot] = ranges[child][pivot].clone();
            }
        }
        return copy;
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
