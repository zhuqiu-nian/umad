package index.structure;

import db.type.IndexObject;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Internal node for IAT.
 */
public class IATInternalNode extends InternalNode
{
    private static final long serialVersionUID = 8108240423051548571L;

    private double c1Ratio;
    private double c2Ratio;
    private double m1;
    private double m2;
    private double[][] childCoverRadii;
    private double[][][] childPivotDistances;
    private double[][] childSubtreeRadii;

    public IATInternalNode()
    {
        super();
        childCoverRadii = new double[3][2];
        childPivotDistances = filled3D(3, 2, 2, -1.0);
        childSubtreeRadii = filled2D(3, 2, -1.0);
    }

    public IATInternalNode(IndexObject[] pivots, int size, long[] childAddresses,
                           double c1Ratio, double c2Ratio, double m1, double m2,
                           double[][] childCoverRadii,
                           double[][][] childPivotDistances,
                           double[][] childSubtreeRadii)
    {
        super(pivots, size, childAddresses);
        this.c1Ratio = c1Ratio;
        this.c2Ratio = c2Ratio;
        this.m1 = m1;
        this.m2 = m2;
        this.childCoverRadii = copy2D(childCoverRadii, 3, 2, 0.0);
        this.childPivotDistances = copy3D(childPivotDistances, 3, 2, 2, -1.0);
        this.childSubtreeRadii = copy2D(childSubtreeRadii, 3, 2, -1.0);
    }

    public IndexObject getC1()
    {
        return pivotSet[0];
    }

    public IndexObject getC2()
    {
        return pivotSet[1];
    }

    public double getC1Ratio()
    {
        return c1Ratio;
    }

    public double getC2Ratio()
    {
        return c2Ratio;
    }

    public double getM1()
    {
        return m1;
    }

    public double getM2()
    {
        return m2;
    }

    public long getLeftChild()
    {
        return childAddresses[0];
    }

    public long getMidChild()
    {
        return childAddresses[1];
    }

    public long getRightChild()
    {
        return childAddresses[2];
    }

    public double[][] getChildCoverRadii()
    {
        return copy2D(childCoverRadii, 3, 2, 0.0);
    }

    public double[] getChildCoverRadii(int childIndex)
    {
        return childCoverRadii[childIndex].clone();
    }

    public double[][] getChildPivotDistances(int childIndex)
    {
        return copy2D(childPivotDistances[childIndex], 2, 2, -1.0);
    }

    public double[] getChildSubtreeRadii(int childIndex)
    {
        return childSubtreeRadii[childIndex].clone();
    }

    public double[] getSubtreeCoverRadii()
    {
        double[] radii = new double[2];
        for (int i = 0; i < childCoverRadii.length; i++)
        {
            radii[0] = Math.max(radii[0], childCoverRadii[i][0]);
            radii[1] = Math.max(radii[1], childCoverRadii[i][1]);
        }
        return radii;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeDouble(c1Ratio);
        out.writeDouble(c2Ratio);
        out.writeDouble(m1);
        out.writeDouble(m2);
        write2D(out, childCoverRadii);
        write3D(out, childPivotDistances);
        write2D(out, childSubtreeRadii);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        c1Ratio = in.readDouble();
        c2Ratio = in.readDouble();
        m1 = in.readDouble();
        m2 = in.readDouble();
        childCoverRadii = read2D(in);
        childPivotDistances = read3D(in);
        childSubtreeRadii = read2D(in);
    }

    private static void write2D(ObjectOutput out, double[][] data) throws IOException
    {
        out.writeInt(data.length);
        out.writeInt(data.length == 0 ? 0 : data[0].length);
        for (double[] row : data)
        {
            for (double value : row)
            {
                out.writeDouble(value);
            }
        }
    }

    private static double[][] read2D(ObjectInput in) throws IOException
    {
        int rows = in.readInt();
        int cols = in.readInt();
        double[][] data = new double[rows][cols];
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                data[i][j] = in.readDouble();
            }
        }
        return data;
    }

    private static void write3D(ObjectOutput out, double[][][] data) throws IOException
    {
        out.writeInt(data.length);
        out.writeInt(data.length == 0 ? 0 : data[0].length);
        out.writeInt(data.length == 0 || data[0].length == 0 ? 0 : data[0][0].length);
        for (double[][] matrix : data)
        {
            for (double[] row : matrix)
            {
                for (double value : row)
                {
                    out.writeDouble(value);
                }
            }
        }
    }

    private static double[][][] read3D(ObjectInput in) throws IOException
    {
        int x = in.readInt();
        int y = in.readInt();
        int z = in.readInt();
        double[][][] data = new double[x][y][z];
        for (int i = 0; i < x; i++)
        {
            for (int j = 0; j < y; j++)
            {
                for (int k = 0; k < z; k++)
                {
                    data[i][j][k] = in.readDouble();
                }
            }
        }
        return data;
    }

    private static double[][] filled2D(int rows, int cols, double value)
    {
        double[][] data = new double[rows][cols];
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                data[i][j] = value;
            }
        }
        return data;
    }

    private static double[][][] filled3D(int x, int y, int z, double value)
    {
        double[][][] data = new double[x][y][z];
        for (int i = 0; i < x; i++)
        {
            data[i] = filled2D(y, z, value);
        }
        return data;
    }

    private static double[][] copy2D(double[][] source, int rows, int cols, double defaultValue)
    {
        double[][] copy = filled2D(rows, cols, defaultValue);
        if (source == null)
        {
            return copy;
        }
        for (int i = 0; i < Math.min(rows, source.length); i++)
        {
            if (source[i] == null)
            {
                continue;
            }
            for (int j = 0; j < Math.min(cols, source[i].length); j++)
            {
                copy[i][j] = source[i][j];
            }
        }
        return copy;
    }

    private static double[][][] copy3D(double[][][] source, int x, int y, int z, double defaultValue)
    {
        double[][][] copy = filled3D(x, y, z, defaultValue);
        if (source == null)
        {
            return copy;
        }
        for (int i = 0; i < Math.min(x, source.length); i++)
        {
            if (source[i] == null)
            {
                continue;
            }
            for (int j = 0; j < Math.min(y, source[i].length); j++)
            {
                if (source[i][j] == null)
                {
                    continue;
                }
                for (int k = 0; k < Math.min(z, source[i][j].length); k++)
                {
                    copy[i][j][k] = source[i][j][k];
                }
            }
        }
        return copy;
    }
}
