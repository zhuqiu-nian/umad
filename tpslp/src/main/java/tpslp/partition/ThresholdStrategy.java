package tpslp.partition;

import java.io.Serializable;
import java.util.Arrays;

public enum ThresholdStrategy implements Serializable
{
    MEDIAN
            {
                @Override
                public double threshold(double[] scores)
                {
                    double[] sorted = sorted(scores);
                    return sorted[sorted.length / 2];
                }
            },

    MAX_GAP
            {
                @Override
                public double threshold(double[] scores)
                {
                    double[] sorted = sorted(scores);
                    if (sorted.length < 2)
                    {
                        return sorted[0];
                    }
                    int best = 0;
                    double bestGap = Double.NEGATIVE_INFINITY;
                    for (int i = 0; i < sorted.length - 1; i++)
                    {
                        double gap = sorted[i + 1] - sorted[i];
                        if (gap > bestGap)
                        {
                            bestGap = gap;
                            best = i;
                        }
                    }
                    return (sorted[best] + sorted[best + 1]) * 0.5;
                }
            },

    OTSU
            {
                @Override
                public double threshold(double[] scores)
                {
                    double[] sorted = sorted(scores);
                    if (sorted.length < 3)
                    {
                        return MAX_GAP.threshold(sorted);
                    }
                    double total = 0.0;
                    for (double score : sorted)
                    {
                        total += score;
                    }
                    double prefix = 0.0;
                    double best = Double.NEGATIVE_INFINITY;
                    int bestIndex = 0;
                    for (int i = 0; i < sorted.length - 1; i++)
                    {
                        prefix += sorted[i];
                        int left = i + 1;
                        int right = sorted.length - left;
                        if (right == 0)
                        {
                            break;
                        }
                        double leftMean = prefix / left;
                        double rightMean = (total - prefix) / right;
                        double separation = (double) left * (double) right
                                * Math.pow(leftMean - rightMean, 2.0);
                        if (separation > best)
                        {
                            best = separation;
                            bestIndex = i;
                        }
                    }
                    return (sorted[bestIndex] + sorted[bestIndex + 1]) * 0.5;
                }
            };

    public abstract double threshold(double[] scores);

    static double[] sorted(double[] scores)
    {
        if (scores == null || scores.length == 0)
        {
            throw new IllegalArgumentException("scores must not be empty");
        }
        double[] sorted = scores.clone();
        Arrays.sort(sorted);
        return sorted;
    }
}
