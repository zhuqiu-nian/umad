package index.structure;

import db.type.IndexObject;

import java.util.List;

/**
 * Partition result for IAT.
 */
public class IATPartitionResults extends PartitionResults
{
    private final double c1Ratio;
    private final double c2Ratio;
    private final double m1;
    private final double m2;
    private final double[][] childCoverRadii;

    public IATPartitionResults(List<List<? extends IndexObject>> subDataList,
                               IndexObject[] pivotSet,
                               double c1Ratio, double c2Ratio,
                               double m1, double m2,
                               double[][] childCoverRadii)
    {
        super(subDataList, pivotSet);
        this.c1Ratio = c1Ratio;
        this.c2Ratio = c2Ratio;
        this.m1 = m1;
        this.m2 = m2;
        this.childCoverRadii = copy2D(childCoverRadii, 3, 2, 0.0);
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

    public double[][] getChildCoverRadii()
    {
        return copy2D(childCoverRadii, 3, 2, 0.0);
    }

    @Override
    public InternalNode getInstanceOfInternalNode(IndexObject[] pivotSet, long[] childAddress)
    {
        return getInstanceOfInternalNode(pivotSet, childAddress, null, null);
    }

    public InternalNode getInstanceOfInternalNode(IndexObject[] pivotSet, long[] childAddress,
                                                  double[][][] childPivotDistances,
                                                  double[][] childSubtreeRadii)
    {
        return new IATInternalNode(pivotSet, getDataSize(), childAddress,
                c1Ratio, c2Ratio, m1, m2, childCoverRadii,
                childPivotDistances, childSubtreeRadii);
    }

    private static double[][] copy2D(double[][] source, int rows, int cols, double defaultValue)
    {
        double[][] copy = new double[rows][cols];
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                copy[i][j] = source != null && i < source.length && source[i] != null && j < source[i].length
                        ? source[i][j] : defaultValue;
            }
        }
        return copy;
    }
}
