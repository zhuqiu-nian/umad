package tpslp.partition;

import algorithms.datapartition.NormalVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public final class BoundaryFactories
{
    private BoundaryFactories()
    {
    }

    public static LinearBoundary vp(int dimension, int pivotIndex)
    {
        double[] weights = new double[dimension];
        weights[pivotIndex] = 1.0;
        return new LinearBoundary(weights);
    }

    public static List<LinearBoundary> mvp(int dimension)
    {
        List<LinearBoundary> boundaries = new ArrayList<>();
        for (int i = 0; i < dimension; i++)
        {
            boundaries.add(vp(dimension, i));
        }
        return boundaries;
    }

    public static LinearBoundary gh()
    {
        return new LinearBoundary(1.0, -1.0);
    }

    public static LinearBoundary cghtSum()
    {
        return new LinearBoundary(1.0, 1.0);
    }

    public static List<LinearBoundary> cght()
    {
        return List.of(gh(), cghtSum());
    }

    public static LinearBoundary rgh(double lambda)
    {
        return new LinearBoundary(1.0, -lambda);
    }

    public static List<LinearBoundary> cpLike(int dimension)
    {
        List<LinearBoundary> boundaries = new ArrayList<>();
        addNormalVectors(boundaries, NormalVector.getVPNormalVectors(dimension));
        addNormalVectors(boundaries, NormalVector.getCGHNormalVectors(dimension));
        if (boundaries.isEmpty())
        {
            boundaries.addAll(mvp(dimension));
            if (dimension == 2)
            {
                boundaries.addAll(cght());
            }
        }
        return boundaries;
    }

    private static void addNormalVectors(List<LinearBoundary> boundaries,
                                         List<Vector<Double>> vectors)
    {
        for (Vector<Double> vector : vectors)
        {
            double[] weights = new double[vector.size()];
            for (int i = 0; i < vector.size(); i++)
            {
                weights[i] = vector.get(i);
            }
            boundaries.add(new LinearBoundary(weights));
        }
    }
}
