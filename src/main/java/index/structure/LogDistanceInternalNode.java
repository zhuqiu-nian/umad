package index.structure;

import db.type.IndexObject;
import index.logdistance.LogDistanceTransform;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Internal node for a binary split in log-distance pivot space.
 *
 * <p>The boundary is w1 * log(d(x,p1)) + w2 * log(d(x,p2)) = tau.</p>
 */
public class LogDistanceInternalNode extends InternalNode
{
    private static final long serialVersionUID = 9208240423051548581L;

    private double epsilonDistance;
    private double w1;
    private double w2;
    private double tau;
    private double comparisonEpsilon;
    private double[][][] childPivotDistanceRanges;

    public LogDistanceInternalNode()
    {
        super();
    }

    public LogDistanceInternalNode(IndexObject[] pivots, int size, long[] childAddresses,
                                   double epsilonDistance, double w1, double w2,
                                   double tau, double comparisonEpsilon)
    {
        super(pivots, size, childAddresses);
        if (pivots.length != 2)
        {
            throw new IllegalArgumentException("LogDistanceInternalNode requires exactly 2 pivots");
        }
        if (childAddresses.length != 2)
        {
            throw new IllegalArgumentException("LogDistanceInternalNode requires exactly 2 children");
        }
        this.epsilonDistance = epsilonDistance;
        this.w1 = w1;
        this.w2 = w2;
        this.tau = tau;
        this.comparisonEpsilon = comparisonEpsilon;
        this.childPivotDistanceRanges = new double[0][][];
    }

    public LogDistanceInternalNode(IndexObject[] pivots, int size, long[] childAddresses,
                                   double epsilonDistance, double w1, double w2,
                                   double tau, double comparisonEpsilon,
                                   double[][][] childPivotDistanceRanges)
    {
        this(pivots, size, childAddresses, epsilonDistance, w1, w2,
                tau, comparisonEpsilon);
        this.childPivotDistanceRanges = cloneRanges(childPivotDistanceRanges);
    }

    public double getEpsilonDistance()
    {
        return epsilonDistance;
    }

    public double getW1()
    {
        return w1;
    }

    public double getW2()
    {
        return w2;
    }

    public double getTau()
    {
        return tau;
    }

    public double getComparisonEpsilon()
    {
        return comparisonEpsilon;
    }

    public double[] getWeights()
    {
        return new double[]{w1, w2};
    }

    public LogDistanceTransform getTransform()
    {
        return new LogDistanceTransform(epsilonDistance);
    }

    public boolean hasChildPivotDistanceRanges()
    {
        return childPivotDistanceRanges.length == getNumChildren();
    }

    public double[] getChildPivotDistanceRange(int childIndex, int pivotIndex)
    {
        return childPivotDistanceRanges[childIndex][pivotIndex].clone();
    }

    private double[][][] cloneRanges(double[][][] ranges)
    {
        if (ranges == null)
        {
            return new double[0][][];
        }
        if (ranges.length != getNumChildren())
        {
            throw new IllegalArgumentException("child range count must match child count");
        }
        double[][][] copy = new double[ranges.length][][];
        for (int child = 0; child < ranges.length; child++)
        {
            if (ranges[child] == null || ranges[child].length != getNumPivots())
            {
                throw new IllegalArgumentException("each child range must include every pivot");
            }
            copy[child] = new double[ranges[child].length][];
            for (int pivot = 0; pivot < ranges[child].length; pivot++)
            {
                if (ranges[child][pivot] == null || ranges[child][pivot].length != 2)
                {
                    throw new IllegalArgumentException("each pivot range must be [low, high]");
                }
                copy[child][pivot] = ranges[child][pivot].clone();
            }
        }
        return copy;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeDouble(epsilonDistance);
        out.writeDouble(w1);
        out.writeDouble(w2);
        out.writeDouble(tau);
        out.writeDouble(comparisonEpsilon);
        out.writeInt(childPivotDistanceRanges.length);
        for (double[][] childRanges : childPivotDistanceRanges)
        {
            out.writeInt(childRanges.length);
            for (double[] pivotRange : childRanges)
            {
                out.writeDouble(pivotRange[0]);
                out.writeDouble(pivotRange[1]);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        epsilonDistance = in.readDouble();
        w1 = in.readDouble();
        w2 = in.readDouble();
        tau = in.readDouble();
        comparisonEpsilon = in.readDouble();
        int childCount = in.readInt();
        childPivotDistanceRanges = new double[childCount][][];
        for (int child = 0; child < childCount; child++)
        {
            int pivotCount = in.readInt();
            childPivotDistanceRanges[child] = new double[pivotCount][2];
            for (int pivot = 0; pivot < pivotCount; pivot++)
            {
                childPivotDistanceRanges[child][pivot][0] = in.readDouble();
                childPivotDistanceRanges[child][pivot][1] = in.readDouble();
            }
        }
    }
}
