package index.structure;

import db.type.IndexObject;
import index.powerdistance.PowerDistanceTransform;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Internal node for a binary split in power-distance pivot space.
 *
 * <p>The boundary score is w1 * d(x,p1)^rho + w2 * d(x,p2)^rho.
 * One or more thresholds split this score axis into ordered slabs.</p>
 */
public class PowerDistanceInternalNode extends InternalNode
{
    private static final long serialVersionUID = 9208240423051548571L;

    private double rho;
    private double epsilonDistance;
    private double w1;
    private double w2;
    private double[] thresholds;
    private double comparisonEpsilon;
    private double[][][] childPivotDistanceRanges;

    public PowerDistanceInternalNode()
    {
        super();
    }

    public PowerDistanceInternalNode(IndexObject[] pivots, int size, long[] childAddresses,
                                     double rho, double epsilonDistance,
                                     double w1, double w2, double tau,
                                     double comparisonEpsilon)
    {
        super(pivots, size, childAddresses);
        if (pivots.length != 2)
        {
            throw new IllegalArgumentException("PowerDistanceInternalNode requires exactly 2 pivots");
        }
        if (childAddresses.length != 2)
        {
            throw new IllegalArgumentException("PowerDistanceInternalNode requires exactly 2 children");
        }
        this.rho = rho;
        this.epsilonDistance = epsilonDistance;
        this.w1 = w1;
        this.w2 = w2;
        this.thresholds = new double[]{tau};
        this.comparisonEpsilon = comparisonEpsilon;
        this.childPivotDistanceRanges = new double[0][][];
    }

    public PowerDistanceInternalNode(IndexObject[] pivots, int size, long[] childAddresses,
                                     double rho, double epsilonDistance,
                                     double w1, double w2, double[] thresholds,
                                     double comparisonEpsilon)
    {
        this(pivots, size, childAddresses, rho, epsilonDistance, w1, w2,
                thresholds, comparisonEpsilon, null);
    }

    public PowerDistanceInternalNode(IndexObject[] pivots, int size, long[] childAddresses,
                                     double rho, double epsilonDistance,
                                     double w1, double w2, double[] thresholds,
                                     double comparisonEpsilon,
                                     double[][][] childPivotDistanceRanges)
    {
        super(pivots, size, childAddresses);
        if (pivots.length != 2)
        {
            throw new IllegalArgumentException("PowerDistanceInternalNode requires exactly 2 pivots");
        }
        if (childAddresses.length < 2)
        {
            throw new IllegalArgumentException("PowerDistanceInternalNode requires at least 2 children");
        }
        if (thresholds == null)
        {
            throw new IllegalArgumentException("thresholds must not be null");
        }
        if (thresholds.length != 0 && thresholds.length != childAddresses.length - 1)
        {
            throw new IllegalArgumentException("threshold count must be child count - 1, or zero to disable pruning");
        }
        this.rho = rho;
        this.epsilonDistance = epsilonDistance;
        this.w1 = w1;
        this.w2 = w2;
        this.thresholds = thresholds.clone();
        this.comparisonEpsilon = comparisonEpsilon;
        this.childPivotDistanceRanges = cloneRanges(childPivotDistanceRanges);
    }

    public double getRho()
    {
        return rho;
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
        return thresholds.length == 0 ? 0.0 : thresholds[0];
    }

    public double[] getThresholds()
    {
        return thresholds.clone();
    }

    public boolean isPowerPruningEnabled()
    {
        return thresholds.length == getNumChildren() - 1 && (w1 != 0.0 || w2 != 0.0);
    }

    public double getComparisonEpsilon()
    {
        return comparisonEpsilon;
    }

    public double[] getWeights()
    {
        return new double[]{w1, w2};
    }

    public PowerDistanceTransform getTransform()
    {
        return new PowerDistanceTransform(rho, epsilonDistance);
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
        out.writeDouble(rho);
        out.writeDouble(epsilonDistance);
        out.writeDouble(w1);
        out.writeDouble(w2);
        out.writeInt(thresholds.length);
        for (double threshold : thresholds)
        {
            out.writeDouble(threshold);
        }
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
        rho = in.readDouble();
        epsilonDistance = in.readDouble();
        w1 = in.readDouble();
        w2 = in.readDouble();
        thresholds = new double[in.readInt()];
        for (int i = 0; i < thresholds.length; i++)
        {
            thresholds[i] = in.readDouble();
        }
        comparisonEpsilon = in.readDouble();
        int childRangeCount = in.readInt();
        childPivotDistanceRanges = new double[childRangeCount][][];
        for (int child = 0; child < childRangeCount; child++)
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
