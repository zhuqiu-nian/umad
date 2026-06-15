package tpslp.partition;

import tpslp.geometry.Interval;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Discrete expected-exclusion threshold selector based on the model in
 * "On the Expected Exclusion Power of Binary Partitions for Metric Search".
 *
 * <p>The selector does not choose a partition direction. It assumes scores
 * s=f(x) and query score intervals I_q are already known, estimates EP(tau),
 * and returns the strongest one or two peak thresholds.</p>
 */
public final class ExpectedExclusionThresholdSelector implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final int maxThresholds;
    private final double minBalance;
    private final double minPeakRatio;
    private final double minSeparationFraction;
    private final double[] quantiles;

    public ExpectedExclusionThresholdSelector()
    {
        this(1);
    }

    public ExpectedExclusionThresholdSelector(int maxThresholds)
    {
        this(maxThresholds, 0.05, 0.85, 0.08,
                new double[]{0.05, 0.10, 0.15, 0.20, 0.25,
                        0.30, 0.35, 0.40, 0.45, 0.50,
                        0.55, 0.60, 0.65, 0.70, 0.75,
                        0.80, 0.85, 0.90, 0.95});
    }

    public ExpectedExclusionThresholdSelector(int maxThresholds, double minBalance,
                                              double minPeakRatio,
                                              double minSeparationFraction,
                                              double[] quantiles)
    {
        if (maxThresholds < 1)
        {
            throw new IllegalArgumentException("maxThresholds must be positive");
        }
        if (minBalance < 0.0 || minBalance >= 0.5)
        {
            throw new IllegalArgumentException("minBalance must be in [0,0.5)");
        }
        if (minPeakRatio < 0.0 || minPeakRatio > 1.0)
        {
            throw new IllegalArgumentException("minPeakRatio must be in [0,1]");
        }
        if (minSeparationFraction < 0.0 || minSeparationFraction > 1.0)
        {
            throw new IllegalArgumentException("minSeparationFraction must be in [0,1]");
        }
        if (quantiles == null || quantiles.length == 0)
        {
            throw new IllegalArgumentException("quantiles must not be empty");
        }
        this.maxThresholds = maxThresholds;
        this.minBalance = minBalance;
        this.minPeakRatio = minPeakRatio;
        this.minSeparationFraction = minSeparationFraction;
        this.quantiles = quantiles.clone();
    }

    public double[] select(double[] scores, Interval[] queryIntervals)
    {
        if (scores == null || scores.length == 0)
        {
            return new double[]{0.0};
        }
        double[] candidates = candidates(scores);
        List<ScoredThreshold> evaluated = new ArrayList<>();
        for (double tau : candidates)
        {
            double leftFraction = fractionLessOrEqual(scores, tau);
            double rightFraction = 1.0 - leftFraction;
            if (Math.min(leftFraction, rightFraction) < minBalance)
            {
                continue;
            }
            double ep = expectedPower(leftFraction, rightFraction, tau, queryIntervals);
            evaluated.add(new ScoredThreshold(tau, ep));
        }
        if (evaluated.isEmpty())
        {
            return new double[]{ThresholdStrategy.MEDIAN.threshold(scores)};
        }

        double max = evaluated.stream().mapToDouble(t -> t.power).max().orElse(0.0);
        List<ScoredThreshold> peaks = localPeaks(evaluated);
        if (peaks.isEmpty())
        {
            peaks = evaluated;
        }
        peaks.sort(Comparator.comparingDouble((ScoredThreshold t) -> t.power).reversed());

        double range = scoreRange(scores);
        double minSeparation = range * minSeparationFraction;
        List<Double> selected = new ArrayList<>();
        for (ScoredThreshold peak : peaks)
        {
            if (peak.power < max * minPeakRatio)
            {
                continue;
            }
            boolean tooClose = false;
            for (double tau : selected)
            {
                if (Math.abs(tau - peak.tau) < minSeparation)
                {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose)
            {
                selected.add(peak.tau);
                if (selected.size() >= maxThresholds)
                {
                    break;
                }
            }
        }
        if (selected.isEmpty())
        {
            selected.add(peaks.get(0).tau);
        }
        double[] thresholds = new double[selected.size()];
        for (int i = 0; i < selected.size(); i++)
        {
            thresholds[i] = selected.get(i);
        }
        Arrays.sort(thresholds);
        return thresholds;
    }

    public List<ScoredThreshold> curve(double[] scores, Interval[] queryIntervals)
    {
        double[] candidates = candidates(scores);
        List<ScoredThreshold> curve = new ArrayList<>();
        for (double tau : candidates)
        {
            double leftFraction = fractionLessOrEqual(scores, tau);
            double rightFraction = 1.0 - leftFraction;
            curve.add(new ScoredThreshold(tau,
                    expectedPower(leftFraction, rightFraction, tau, queryIntervals)));
        }
        return curve;
    }

    private double expectedPower(double leftFraction, double rightFraction,
                                 double tau, Interval[] queryIntervals)
    {
        if (queryIntervals == null || queryIntervals.length == 0)
        {
            return Math.min(leftFraction, rightFraction);
        }
        int queryLeft = 0;
        int queryRight = 0;
        for (Interval interval : queryIntervals)
        {
            if (interval.getHigh() <= tau)
            {
                queryLeft++;
            }
            if (interval.getLow() > tau)
            {
                queryRight++;
            }
        }
        double pQueryLeft = (double) queryLeft / (double) queryIntervals.length;
        double pQueryRight = (double) queryRight / (double) queryIntervals.length;
        return rightFraction * pQueryLeft + leftFraction * pQueryRight;
    }

    private List<ScoredThreshold> localPeaks(List<ScoredThreshold> evaluated)
    {
        List<ScoredThreshold> peaks = new ArrayList<>();
        if (evaluated.size() == 1)
        {
            peaks.add(evaluated.get(0));
            return peaks;
        }
        for (int i = 0; i < evaluated.size(); i++)
        {
            double previous = i == 0 ? Double.NEGATIVE_INFINITY : evaluated.get(i - 1).power;
            double current = evaluated.get(i).power;
            double next = i == evaluated.size() - 1 ? Double.NEGATIVE_INFINITY
                    : evaluated.get(i + 1).power;
            if (current >= previous && current >= next)
            {
                peaks.add(evaluated.get(i));
            }
        }
        return peaks;
    }

    private double[] candidates(double[] scores)
    {
        double[] sorted = scores.clone();
        Arrays.sort(sorted);
        double[] candidates = new double[quantiles.length];
        int count = 0;
        for (double quantile : quantiles)
        {
            if (quantile <= 0.0 || quantile >= 1.0)
            {
                continue;
            }
            int index = (int) Math.round(quantile * (sorted.length - 1));
            index = Math.max(0, Math.min(sorted.length - 1, index));
            double value = sorted[index];
            if (count == 0 || value != candidates[count - 1])
            {
                candidates[count++] = value;
            }
        }
        return Arrays.copyOf(candidates, count);
    }

    private double fractionLessOrEqual(double[] scores, double tau)
    {
        int left = 0;
        for (double score : scores)
        {
            if (score <= tau)
            {
                left++;
            }
        }
        return (double) left / (double) scores.length;
    }

    private double scoreRange(double[] scores)
    {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double score : scores)
        {
            min = Math.min(min, score);
            max = Math.max(max, score);
        }
        return Math.max(0.0, max - min);
    }

    public static final class ScoredThreshold implements Serializable
    {
        private static final long serialVersionUID = 1L;

        private final double tau;
        private final double power;

        private ScoredThreshold(double tau, double power)
        {
            this.tau = tau;
            this.power = power;
        }

        public double getTau()
        {
            return tau;
        }

        public double getPower()
        {
            return power;
        }
    }
}
