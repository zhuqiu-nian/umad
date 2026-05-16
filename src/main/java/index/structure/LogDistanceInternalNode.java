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

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeDouble(epsilonDistance);
        out.writeDouble(w1);
        out.writeDouble(w2);
        out.writeDouble(tau);
        out.writeDouble(comparisonEpsilon);
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
    }
}
