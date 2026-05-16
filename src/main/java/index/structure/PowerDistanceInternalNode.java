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
    }

    public PowerDistanceInternalNode(IndexObject[] pivots, int size, long[] childAddresses,
                                     double rho, double epsilonDistance,
                                     double w1, double w2, double[] thresholds,
                                     double comparisonEpsilon)
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
    }
}
