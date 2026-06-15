package tpslp.partition;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import db.type.IndexObject;
import metric.Metric;
import tpslp.coordinate.CoordinateMap;

import java.util.ArrayList;
import java.util.List;

final class LinearLearningSupport
{
    private static final double EPSILON = 1e-12;

    private LinearLearningSupport()
    {
    }

    static double[][] coordinates(Metric metric, IndexObject[] pivots, CoordinateMap coordinateMap,
                                  List<? extends IndexObject> data)
    {
        double[][] coordinates = new double[data.size()][];
        for (int i = 0; i < data.size(); i++)
        {
            coordinates[i] = coordinateMap.mapPoint(metric, pivots, data.get(i));
        }
        return coordinates;
    }

    static PartitionPlan leafPlan(List<? extends IndexObject> data, int dimension)
    {
        double[] weights = new double[Math.max(1, dimension)];
        weights[0] = 1.0;
        LinearSlabRegion region = new LinearSlabRegion(List.of(
                new LinearSlab(new LinearBoundary(weights),
                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)));
        return new PartitionPlan(List.of(new PartitionPlan.ChildPartition(copy(data), region)));
    }

    static PartitionPlan binaryPlan(List<? extends IndexObject> data, double[] direction,
                                    double threshold, double[] scores)
    {
        LinearBoundary boundary = new LinearBoundary(direction);
        List<IndexObject> left = new ArrayList<>();
        List<IndexObject> right = new ArrayList<>();
        for (int i = 0; i < data.size(); i++)
        {
            if (scores[i] <= threshold)
            {
                left.add(data.get(i));
            }
            else
            {
                right.add(data.get(i));
            }
        }
        if (left.isEmpty() || right.isEmpty())
        {
            return leafPlan(data, direction.length);
        }

        List<PartitionPlan.ChildPartition> children = new ArrayList<>();
        children.add(new PartitionPlan.ChildPartition(left,
                new LinearSlabRegion(List.of(new LinearSlab(boundary,
                        Double.NEGATIVE_INFINITY, threshold)))));
        children.add(new PartitionPlan.ChildPartition(right,
                new LinearSlabRegion(List.of(new LinearSlab(boundary,
                        threshold, Double.POSITIVE_INFINITY)))));
        return new PartitionPlan(children);
    }

    static PartitionPlan multiThresholdPlan(List<? extends IndexObject> data, double[] direction,
                                            double[] thresholds, double[] scores)
    {
        if (thresholds == null || thresholds.length == 0)
        {
            return leafPlan(data, direction.length);
        }
        double[] sortedThresholds = thresholds.clone();
        java.util.Arrays.sort(sortedThresholds);

        LinearBoundary boundary = new LinearBoundary(direction);
        List<List<IndexObject>> partitions = new ArrayList<>();
        for (int i = 0; i <= sortedThresholds.length; i++)
        {
            partitions.add(new ArrayList<>());
        }
        for (int i = 0; i < scores.length; i++)
        {
            partitions.get(partitionIndex(scores[i], sortedThresholds)).add(data.get(i));
        }

        List<PartitionPlan.ChildPartition> children = new ArrayList<>();
        for (int i = 0; i < partitions.size(); i++)
        {
            if (partitions.get(i).isEmpty())
            {
                continue;
            }
            double low = i == 0 ? Double.NEGATIVE_INFINITY : sortedThresholds[i - 1];
            double high = i == sortedThresholds.length
                    ? Double.POSITIVE_INFINITY : sortedThresholds[i];
            children.add(new PartitionPlan.ChildPartition(partitions.get(i),
                    new LinearSlabRegion(List.of(new LinearSlab(boundary, low, high)))));
        }
        if (children.size() <= 1)
        {
            return leafPlan(data, direction.length);
        }
        return new PartitionPlan(children);
    }

    static double[] scores(double[][] coordinates, double[] direction)
    {
        double[] scores = new double[coordinates.length];
        LinearBoundary boundary = new LinearBoundary(direction);
        for (int i = 0; i < coordinates.length; i++)
        {
            scores[i] = boundary.score(coordinates[i]);
        }
        return scores;
    }

    static double[][] covariance(double[][] data)
    {
        int n = data.length;
        int dim = data[0].length;
        double[] mean = new double[dim];
        for (double[] row : data)
        {
            for (int j = 0; j < dim; j++)
            {
                mean[j] += row[j];
            }
        }
        for (int j = 0; j < dim; j++)
        {
            mean[j] /= n;
        }

        double[][] covariance = new double[dim][dim];
        if (n < 2)
        {
            return covariance;
        }
        for (double[] row : data)
        {
            for (int i = 0; i < dim; i++)
            {
                double di = row[i] - mean[i];
                for (int j = 0; j < dim; j++)
                {
                    covariance[i][j] += di * (row[j] - mean[j]);
                }
            }
        }
        for (int i = 0; i < dim; i++)
        {
            for (int j = 0; j < dim; j++)
            {
                covariance[i][j] /= n - 1;
            }
        }
        return covariance;
    }

    static double[] firstPrincipalDirection(double[][] data)
    {
        return leadingEigenvector(covariance(data));
    }

    static double[] diagonalGeneralizedPrincipalDirection(double[][] numerator,
                                                          double[] diagonalPenalty)
    {
        double[][] adjusted = new double[numerator.length][numerator.length];
        for (int i = 0; i < numerator.length; i++)
        {
            double rowScale = 1.0 / Math.sqrt(Math.max(diagonalPenalty[i], EPSILON));
            for (int j = 0; j < numerator.length; j++)
            {
                double colScale = 1.0 / Math.sqrt(Math.max(diagonalPenalty[j], EPSILON));
                adjusted[i][j] = numerator[i][j] * rowScale * colScale;
            }
        }
        double[] vector = leadingEigenvector(adjusted);
        for (int i = 0; i < vector.length; i++)
        {
            vector[i] /= Math.sqrt(Math.max(diagonalPenalty[i], EPSILON));
        }
        return vector;
    }

    static double[] leadingEigenvector(double[][] matrix)
    {
        if (matrix.length == 1)
        {
            return new double[]{1.0};
        }
        EigenvalueDecomposition decomposition = new Matrix(matrix).eig();
        double[] eigenvalues = new double[matrix.length];
        double[][] d = decomposition.getD().getArray();
        for (int i = 0; i < eigenvalues.length; i++)
        {
            eigenvalues[i] = d[i][i];
        }
        double[][] vectors = decomposition.getV().getArray();
        int best = 0;
        for (int i = 1; i < eigenvalues.length; i++)
        {
            if (eigenvalues[i] > eigenvalues[best])
            {
                best = i;
            }
        }
        double[] vector = new double[matrix.length];
        for (int i = 0; i < vector.length; i++)
        {
            vector[i] = vectors[i][best];
        }
        return vector;
    }

    static double[] normalizeOrFallback(double[] direction, int dimension)
    {
        if (direction == null || direction.length != dimension)
        {
            return axisFallback(dimension);
        }
        double norm = 0.0;
        for (double value : direction)
        {
            if (Double.isNaN(value) || Double.isInfinite(value))
            {
                return axisFallback(dimension);
            }
            norm += value * value;
        }
        if (norm <= EPSILON)
        {
            return axisFallback(dimension);
        }
        norm = Math.sqrt(norm);
        double[] normalized = direction.clone();
        for (int i = 0; i < normalized.length; i++)
        {
            normalized[i] /= norm;
        }
        return normalized;
    }

    static double distance(double[] left, double[] right)
    {
        double sum = 0.0;
        for (int i = 0; i < left.length; i++)
        {
            double d = left[i] - right[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    static double queryVisitCost(Metric metric, IndexObject[] pivots, CoordinateMap coordinateMap,
                                 List<? extends IndexObject> data,
                                 List<? extends IndexObject> trainingQueries,
                                 double queryRadius,
                                 double[] direction,
                                 double[] thresholds,
                                 double[] scores)
    {
        if (trainingQueries == null || trainingQueries.isEmpty())
        {
            return Double.POSITIVE_INFINITY;
        }
        LinearBoundary boundary = new LinearBoundary(direction);
        int[] partitionSizes = new int[thresholds.length + 1];
        for (double score : scores)
        {
            partitionSizes[partitionIndex(score, thresholds)]++;
        }

        double cost = 0.0;
        for (IndexObject query : trainingQueries)
        {
            tpslp.geometry.Interval projected =
                    boundary.project(coordinateMap.mapQuery(metric, pivots, query, queryRadius));
            for (int partition = 0; partition < partitionSizes.length; partition++)
            {
                double low = partition == 0 ? Double.NEGATIVE_INFINITY : thresholds[partition - 1];
                double high = partition == thresholds.length
                        ? Double.POSITIVE_INFINITY : thresholds[partition];
                tpslp.geometry.Interval slab = new tpslp.geometry.Interval(low, high);
                if (projected.intersects(slab))
                {
                    cost += partitionSizes[partition];
                }
            }
        }
        return cost / trainingQueries.size();
    }

    static tpslp.geometry.Interval[] projectedQueryIntervals(
            Metric metric, IndexObject[] pivots, CoordinateMap coordinateMap,
            List<? extends IndexObject> trainingQueries, double queryRadius,
            double[] direction)
    {
        if (trainingQueries == null)
        {
            return new tpslp.geometry.Interval[0];
        }
        LinearBoundary boundary = new LinearBoundary(direction);
        tpslp.geometry.Interval[] intervals = new tpslp.geometry.Interval[trainingQueries.size()];
        for (int i = 0; i < trainingQueries.size(); i++)
        {
            intervals[i] = boundary.project(coordinateMap.mapQuery(metric, pivots,
                    trainingQueries.get(i), queryRadius));
        }
        return intervals;
    }

    static int partitionIndex(double score, double[] thresholds)
    {
        for (int i = 0; i < thresholds.length; i++)
        {
            if (score <= thresholds[i])
            {
                return i;
            }
        }
        return thresholds.length;
    }

    static double[] thresholdCandidates(double[] scores)
    {
        double[] candidates = new double[]{
                ThresholdStrategy.MEDIAN.threshold(scores),
                ThresholdStrategy.MAX_GAP.threshold(scores),
                ThresholdStrategy.OTSU.threshold(scores),
                quantile(scores, 0.20),
                quantile(scores, 0.30),
                quantile(scores, 0.40),
                quantile(scores, 0.60),
                quantile(scores, 0.70),
                quantile(scores, 0.80)
        };
        java.util.Arrays.sort(candidates);
        int count = 0;
        for (double candidate : candidates)
        {
            if (count == 0 || candidate != candidates[count - 1])
            {
                candidates[count++] = candidate;
            }
        }
        return java.util.Arrays.copyOf(candidates, count);
    }

    static double quantile(double[] scores, double quantile)
    {
        double[] sorted = scores.clone();
        java.util.Arrays.sort(sorted);
        int index = (int) Math.round(quantile * (sorted.length - 1));
        index = Math.max(0, Math.min(sorted.length - 1, index));
        return sorted[index];
    }

    static List<IndexObject> copy(List<? extends IndexObject> data)
    {
        return new ArrayList<>(data);
    }

    private static double[] axisFallback(int dimension)
    {
        double[] fallback = new double[Math.max(1, dimension)];
        fallback[0] = 1.0;
        return fallback;
    }
}
