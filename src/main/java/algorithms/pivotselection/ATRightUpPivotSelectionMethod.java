package algorithms.pivotselection;

import db.type.IndexObject;
import metric.Metric;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pivot selector for AT-style ratio partitions.
 *
 * The score favors pivot pairs for which most evaluation objects have a large
 * min(d(x,p1), d(x,p2)) relative to d(p1,p2). In the two-pivot distance map
 * this pushes the data away from the lower-left region where AT ratio
 * boundaries are dense and range-query boxes tend to intersect many cuts.
 */
public class ATRightUpPivotSelectionMethod implements EvaluationPivotSelectionMethod
{
    private static final double EPS = 1e-12;
    private static final int DEFAULT_CANDIDATE_LIMIT = 64;
    private static final int DEFAULT_EVAL_LIMIT = 384;

    private final int candidateLimit;
    private final int evalLimit;

    public ATRightUpPivotSelectionMethod()
    {
        this(DEFAULT_CANDIDATE_LIMIT, DEFAULT_EVAL_LIMIT);
    }

    public ATRightUpPivotSelectionMethod(int candidateLimit, int evalLimit)
    {
        this.candidateLimit = Math.max(2, candidateLimit);
        this.evalLimit = Math.max(2, evalLimit);
    }

    public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int numPivots)
    {
        return selectPivots(metric, data, 0, data.size(), numPivots);
    }

    public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int first, int dataSize, int numPivots)
    {
        return selectPivots(metric, data, data, first, dataSize, numPivots);
    }

    public int[] selectPivots(Metric metric, List<? extends IndexObject> candidateSet,
                              List<? extends IndexObject> evaluationSet, int numPivots)
    {
        return selectPivots(metric, candidateSet, evaluationSet, 0, candidateSet.size(), numPivots);
    }

    private int[] selectPivots(Metric metric, List<? extends IndexObject> candidateSet,
                               List<? extends IndexObject> evaluationSet, int first, int dataSize, int numPivots)
    {
        if (numPivots != 2)
        {
            return PivotSelectionMethods.FFT.selectPivots(metric, candidateSet, first, dataSize, numPivots);
        }
        if (dataSize <= 2)
        {
            int[] result = new int[dataSize];
            for (int i = 0; i < dataSize; i++) result[i] = first + i;
            return result;
        }

        int[] candidates = candidatePool(metric, candidateSet, first, dataSize);
        int[] eval = sampleIndices(0, evaluationSet.size(), evalLimit);

        double bestScore = Double.NEGATIVE_INFINITY;
        int bestA = candidates[0];
        int bestB = candidates[1];

        for (int i = 0; i < candidates.length; i++)
        {
            int a = candidates[i];
            IndexObject pa = candidateSet.get(a);
            for (int j = i + 1; j < candidates.length; j++)
            {
                int b = candidates[j];
                IndexObject pb = candidateSet.get(b);
                double pivotDistance = metric.getDistance(pa, pb);
                if (pivotDistance <= EPS) continue;

                double score = scorePair(metric, evaluationSet, pa, pb, pivotDistance, eval);
                if (score > bestScore)
                {
                    bestScore = score;
                    bestA = a;
                    bestB = b;
                }
            }
        }

        if (bestScore == Double.NEGATIVE_INFINITY)
        {
            return PivotSelectionMethods.FFT.selectPivots(metric, candidateSet, first, dataSize, numPivots);
        }
        return new int[]{bestA, bestB};
    }

    private double scorePair(Metric metric, List<? extends IndexObject> data, IndexObject p1, IndexObject p2,
                             double pivotDistance, int[] eval)
    {
        int count = eval.length;
        if (count < 9) return Double.NEGATIVE_INFINITY;

        double[] d1 = new double[count];
        double[] d2 = new double[count];
        double[] logRatio = new double[count];
        double sumMin = 0.0;
        double sumD1 = 0.0;
        double sumD2 = 0.0;

        for (int i = 0; i < count; i++)
        {
            IndexObject x = data.get(eval[i]);
            d1[i] = metric.getDistance(x, p1);
            d2[i] = metric.getDistance(x, p2);
            logRatio[i] = Math.log((d1[i] + EPS) / (d2[i] + EPS));
            sumMin += Math.min(d1[i], d2[i]);
            sumD1 += d1[i];
            sumD2 += d2[i];
        }

        double[] sorted = logRatio.clone();
        java.util.Arrays.sort(sorted);
        int q1 = count / 3;
        int q2 = (2 * count) / 3;
        double split1 = sorted[q1];
        double split2 = sorted[q2];
        if (split2 <= split1 + EPS) return Double.NEGATIVE_INFINITY;

        int leftCount = 0;
        int midCount = 0;
        int rightCount = 0;
        double leftMaxD1 = 0.0;
        double midMaxMin = 0.0;
        double rightMaxD2 = 0.0;

        for (int i = 0; i < count; i++)
        {
            if (logRatio[i] < split1)
            {
                leftCount++;
                if (d1[i] > leftMaxD1) leftMaxD1 = d1[i];
            }
            else if (logRatio[i] > split2)
            {
                rightCount++;
                if (d2[i] > rightMaxD2) rightMaxD2 = d2[i];
            }
            else
            {
                midCount++;
                double min = Math.min(d1[i], d2[i]);
                if (min > midMaxMin) midMaxMin = min;
            }
        }

        double meanMin = sumMin / count;
        double meanDistance = (sumD1 + sumD2) / (2.0 * count);
        double rightUp = meanMin / (meanDistance + EPS);

        double gap1 = localGap(sorted, q1);
        double gap2 = localGap(sorted, q2);
        double ratioSpread = percentile(sorted, 0.9) - percentile(sorted, 0.1);
        double gapScore = (gap1 + gap2) / (ratioSpread + EPS);

        double target = count / 3.0;
        double balanceError = (Math.abs(leftCount - target) + Math.abs(midCount - target) + Math.abs(rightCount - target)) / count;
        double balanceScore = 1.0 - balanceError;

        double coverScale = meanDistance + EPS;
        double coverPenalty = (leftMaxD1 + midMaxMin + rightMaxD2) / (3.0 * coverScale);
        double pairScalePenalty = Math.abs(pivotDistance / (meanDistance + EPS) - 1.0);

        return 1.8 * gapScore
                + 2.0 * balanceScore
                + 0.7 * rightUp
                + 0.2 * Math.min(ratioSpread, 4.0)
                - 1.6 * coverPenalty
                - 0.5 * pairScalePenalty;
    }

    private double localGap(double[] sorted, int index)
    {
        int left = Math.max(0, index - 2);
        int right = Math.min(sorted.length - 1, index + 2);
        return sorted[right] - sorted[left];
    }

    private double percentile(double[] sorted, double p)
    {
        if (sorted.length == 0) return 0.0;
        double pos = p * (sorted.length - 1);
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        if (lo == hi) return sorted[lo];
        double w = pos - lo;
        return sorted[lo] * (1.0 - w) + sorted[hi] * w;
    }

    private int[] sampleIndices(int first, int dataSize, int limit)
    {
        if (dataSize <= 0) return new int[0];

        int count = Math.min(dataSize, limit);
        Set<Integer> indices = new LinkedHashSet<>(count);
        indices.add(first);

        if (count == dataSize)
        {
            for (int i = 0; i < dataSize; i++) indices.add(first + i);
        }
        else
        {
            for (int i = 0; indices.size() < count && i < count * 2; i++)
            {
                int offset = (int) Math.floor(i * (dataSize - 1.0) / (count - 1.0));
                indices.add(first + offset);
            }
        }

        int[] result = new int[indices.size()];
        int i = 0;
        for (Integer index : indices) result[i++] = index;
        return result;
    }

    private int[] candidatePool(Metric metric, List<? extends IndexObject> candidateSet, int first, int dataSize)
    {
        int count = Math.min(dataSize, candidateLimit);
        if (count >= dataSize)
        {
            int[] result = new int[dataSize];
            for (int i = 0; i < dataSize; i++) result[i] = first + i;
            return result;
        }
        return PivotSelectionMethods.FFT.selectPivots(metric, candidateSet, first, dataSize, count);
    }
}
