package algorithms.datapartition;

import db.type.IndexObject;
import index.structure.IATPartitionResults;
import index.structure.PartitionResults;
import metric.Metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Partition methods for IAT (Improved Apollonian Tree).
 *
 * QUANTILE keeps the original AT count-balanced split.
 * SPATIAL_BALANCED chooses thresholds that trade count balance for tighter
 * child covering balls, which is more useful when query distance calls are
 * expensive.
 */
public enum IATPartitionMethods implements PartitionMethod
{
    QUANTILE
            {
                public PartitionResults partition(Metric metric, IndexObject[] pivots,
                                                  List<? extends IndexObject> data,
                                                  int numPartitions, int maxLS)
                {
                    return partition(metric, pivots, data, 0, data.size(), numPartitions, maxLS);
                }

                public PartitionResults partition(Metric metric, IndexObject[] pivots,
                                                  List<? extends IndexObject> data,
                                                  int first, int size, int numPartitions, int maxLS)
                {
                    return partitionInternal(metric, pivots, data, first, size, numPartitions, false);
                }
            },

    SPATIAL_BALANCED
            {
                public PartitionResults partition(Metric metric, IndexObject[] pivots,
                                                  List<? extends IndexObject> data,
                                                  int numPartitions, int maxLS)
                {
                    return partition(metric, pivots, data, 0, data.size(), numPartitions, maxLS);
                }

                public PartitionResults partition(Metric metric, IndexObject[] pivots,
                                                  List<? extends IndexObject> data,
                                                  int first, int size, int numPartitions, int maxLS)
                {
                    return partitionInternal(metric, pivots, data, first, size, numPartitions, true);
                }
            };

    private static PartitionResults partitionInternal(Metric metric, IndexObject[] pivots,
                                                      List<? extends IndexObject> data,
                                                      int first, int size, int numPartitions,
                                                      boolean spatialBalanced)
    {
        if (pivots.length != 2)
        {
            throw new IllegalArgumentException("IAT partition requires exactly 2 pivots, but got " + pivots.length);
        }
        if (numPartitions != 3)
        {
            throw new IllegalArgumentException("IAT partition requires exactly 3 partitions, but got " + numPartitions);
        }

        IndexObject c1 = pivots[0];
        IndexObject c2 = pivots[1];
        Entry[] entries = new Entry[size];
        double[] finiteRatios = new double[size];
        int finiteCount = 0;

        for (int i = 0; i < size; i++)
        {
            IndexObject x = data.get(first + i);
            double d1 = metric.getDistance(c1, x);
            double d2 = metric.getDistance(c2, x);
            double ratio = d2 == 0.0 ? Double.MAX_VALUE : d1 / d2;
            entries[i] = new Entry(x, ratio, d1, d2);
            if (ratio != Double.MAX_VALUE && !Double.isNaN(ratio))
            {
                finiteRatios[finiteCount++] = ratio;
            }
        }

        double[] thresholds = spatialBalanced
                ? chooseSpatialThresholds(entries, finiteRatios, finiteCount, metric.getDistance(c1, c2))
                : chooseQuantileThresholds(entries);

        return buildResult(entries, pivots, thresholds[0], thresholds[1]);
    }

    private static double[] chooseQuantileThresholds(Entry[] entries)
    {
        if (entries.length == 0)
        {
            return new double[]{0.5, 2.0};
        }

        double[] sorted = new double[entries.length];
        for (int i = 0; i < entries.length; i++)
        {
            sorted[i] = entries[i].ratio;
        }
        Arrays.sort(sorted);

        if (entries.length == 1)
        {
            double r = sorted[0];
            if (r <= 0.0 || r == Double.MAX_VALUE)
            {
                return new double[]{0.5, 2.0};
            }
            return new double[]{r * 0.9, r * 1.1};
        }

        double c1Ratio = sorted[entries.length / 3];
        double c2Ratio = sorted[(2 * entries.length) / 3];
        if (c1Ratio <= 0.0)
        {
            c1Ratio = Double.MIN_VALUE;
        }
        if (c2Ratio <= c1Ratio)
        {
            c2Ratio = c1Ratio < 1.0 ? c1Ratio * 2.0 : c1Ratio + 1.0;
        }
        return new double[]{c1Ratio, c2Ratio};
    }

    private static double[] chooseSpatialThresholds(Entry[] entries, double[] finiteRatios,
                                                   int finiteCount, double pivotDistance)
    {
        if (finiteCount < 3)
        {
            return chooseQuantileThresholds(entries);
        }

        double[] sorted = Arrays.copyOf(finiteRatios, finiteCount);
        Arrays.sort(sorted);
        int[] candidateIndexes = candidateIndexes(finiteCount);

        double bestScore = Double.POSITIVE_INFINITY;
        double bestC1 = sorted[finiteCount / 3];
        double bestC2 = sorted[(2 * finiteCount) / 3];

        for (int leftIndex : candidateIndexes)
        {
            for (int rightIndex : candidateIndexes)
            {
                if (rightIndex <= leftIndex)
                {
                    continue;
                }
                double c1 = sorted[leftIndex];
                double c2 = sorted[rightIndex];
                if (c2 <= c1)
                {
                    continue;
                }
                Score score = score(entries, c1, c2, pivotDistance);
                if (score.valid && score.value < bestScore)
                {
                    bestScore = score.value;
                    bestC1 = c1;
                    bestC2 = c2;
                }
            }
        }

        return sanitizeThresholds(bestC1, bestC2);
    }

    private static int[] candidateIndexes(int n)
    {
        List<Integer> indexes = new ArrayList<>();
        int step = Math.max(1, n / 24);
        for (int i = Math.max(0, n / 10); i < Math.min(n, (9 * n) / 10); i += step)
        {
            indexes.add(i);
        }
        addIndex(indexes, n / 3, n);
        addIndex(indexes, (2 * n) / 3, n);
        addIndex(indexes, n / 4, n);
        addIndex(indexes, (3 * n) / 4, n);
        return indexes.stream().mapToInt(Integer::intValue).distinct().sorted().toArray();
    }

    private static void addIndex(List<Integer> indexes, int index, int n)
    {
        if (index >= 0 && index < n)
        {
            indexes.add(index);
        }
    }

    private static Score score(Entry[] entries, double c1, double c2, double pivotDistance)
    {
        int[] counts = new int[3];
        double[][] covers = new double[3][2];

        for (Entry entry : entries)
        {
            int child = childOf(entry.ratio, c1, c2);
            counts[child]++;
            covers[child][0] = Math.max(covers[child][0], entry.d1);
            covers[child][1] = Math.max(covers[child][1], entry.d2);
        }

        for (int count : counts)
        {
            if (count == 0)
            {
                return Score.invalid();
            }
        }

        double maxCompactCover = 0.0;
        double averageCompactCover = 0.0;
        for (int i = 0; i < 3; i++)
        {
            double compact = Math.min(covers[i][0], covers[i][1]);
            maxCompactCover = Math.max(maxCompactCover, compact);
            averageCompactCover += compact * counts[i] / entries.length;
        }

        double ideal = entries.length / 3.0;
        double maxCountDeviation = 0.0;
        for (int count : counts)
        {
            maxCountDeviation = Math.max(maxCountDeviation, Math.abs(count - ideal) / entries.length);
        }

        double scale = pivotDistance > 0.0 ? pivotDistance : Math.max(maxCompactCover, 1.0);
        return Score.valid(maxCompactCover + 0.25 * averageCompactCover + scale * maxCountDeviation);
    }

    private static PartitionResults buildResult(Entry[] entries, IndexObject[] pivots,
                                                double c1Ratio, double c2Ratio)
    {
        List<IndexObject> left = new ArrayList<>();
        List<IndexObject> mid = new ArrayList<>();
        List<IndexObject> right = new ArrayList<>();
        double[][] childCoverRadii = new double[3][2];

        for (Entry entry : entries)
        {
            int child = childOf(entry.ratio, c1Ratio, c2Ratio);
            if (child == 0)
            {
                left.add(entry.object);
            }
            else if (child == 1)
            {
                mid.add(entry.object);
            }
            else
            {
                right.add(entry.object);
            }
            childCoverRadii[child][0] = Math.max(childCoverRadii[child][0], entry.d1);
            childCoverRadii[child][1] = Math.max(childCoverRadii[child][1], entry.d2);
        }

        List<List<? extends IndexObject>> subDataList = new ArrayList<>(3);
        subDataList.add(left);
        subDataList.add(mid);
        subDataList.add(right);

        return new IATPartitionResults(subDataList, pivots, c1Ratio, c2Ratio,
                childCoverRadii[0][0], childCoverRadii[2][1], childCoverRadii);
    }

    private static int childOf(double ratio, double c1Ratio, double c2Ratio)
    {
        if (ratio < c1Ratio)
        {
            return 0;
        }
        if (ratio > c2Ratio)
        {
            return 2;
        }
        return 1;
    }

    private static double[] sanitizeThresholds(double c1, double c2)
    {
        if (Double.isNaN(c1) || c1 <= 0.0)
        {
            c1 = Double.MIN_VALUE;
        }
        if (Double.isNaN(c2) || c2 <= c1)
        {
            c2 = c1 < 1.0 ? c1 * 2.0 : c1 + 1.0;
        }
        return new double[]{c1, c2};
    }

    private static class Entry
    {
        final IndexObject object;
        final double ratio;
        final double d1;
        final double d2;

        Entry(IndexObject object, double ratio, double d1, double d2)
        {
            this.object = object;
            this.ratio = ratio;
            this.d1 = d1;
            this.d2 = d2;
        }
    }

    private static class Score
    {
        final boolean valid;
        final double value;

        private Score(boolean valid, double value)
        {
            this.valid = valid;
            this.value = value;
        }

        static Score invalid()
        {
            return new Score(false, Double.POSITIVE_INFINITY);
        }

        static Score valid(double value)
        {
            return new Score(true, value);
        }
    }
}
