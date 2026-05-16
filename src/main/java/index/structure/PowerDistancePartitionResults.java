package index.structure;

import db.type.IndexObject;
import index.powerdistance.PowerDistanceTransform;

import java.util.List;

public class PowerDistancePartitionResults extends PartitionResults
{
    private static final long serialVersionUID = 9208240423051548572L;

    private final double rho;
    private final double epsilonDistance;
    private final double w1;
    private final double w2;
    private final double[] thresholds;
    private final double comparisonEpsilon;

    public PowerDistancePartitionResults(List<List<? extends IndexObject>> subDataList,
                                         IndexObject[] pivotSet,
                                         double rho, double epsilonDistance,
                                         double w1, double w2, double tau,
                                         double comparisonEpsilon)
    {
        super(subDataList, pivotSet);
        this.rho = rho;
        this.epsilonDistance = epsilonDistance;
        this.w1 = w1;
        this.w2 = w2;
        this.thresholds = new double[]{tau};
        this.comparisonEpsilon = comparisonEpsilon;
    }

    public PowerDistancePartitionResults(List<List<? extends IndexObject>> subDataList,
                                         IndexObject[] pivotSet,
                                         double rho, double epsilonDistance,
                                         double w1, double w2, double[] thresholds,
                                         double comparisonEpsilon)
    {
        super(subDataList, pivotSet);
        this.rho = rho;
        this.epsilonDistance = epsilonDistance;
        this.w1 = w1;
        this.w2 = w2;
        this.thresholds = thresholds.clone();
        this.comparisonEpsilon = comparisonEpsilon;
    }

    public double getRho()
    {
        return rho;
    }

    public double getTau()
    {
        return thresholds.length == 0 ? 0.0 : thresholds[0];
    }

    public double[] getThresholds()
    {
        return thresholds.clone();
    }

    @Override
    public InternalNode getInstanceOfInternalNode(IndexObject[] pivotSet, long[] childAddress)
    {
        return new PowerDistanceInternalNode(pivotSet, getDataSize(), childAddress,
                rho, epsilonDistance, w1, w2, thresholds, comparisonEpsilon);
    }

    @Override
    public String toString()
    {
        return "PowerDistancePartitionResults{" +
                "rho=" + rho +
                ", epsilonDistance=" + epsilonDistance +
                ", w1=" + w1 +
                ", w2=" + w2 +
                ", thresholds=" + java.util.Arrays.toString(thresholds) +
                ", comparisonEpsilon=" + comparisonEpsilon +
                ", leftSize=" + (listOfPartitions.size() > 0 ? listOfPartitions.get(0).size() : 0) +
                ", rightSize=" + (listOfPartitions.size() > 1 ? listOfPartitions.get(1).size() : 0) +
                '}';
    }
}
