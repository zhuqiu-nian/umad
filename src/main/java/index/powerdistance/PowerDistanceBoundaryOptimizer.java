package index.powerdistance;

import db.type.IndexObject;
import metric.Metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PowerDistanceBoundaryOptimizer
{
    private static final double BETTER_EPS = 1e-12;

    public Result optimize(Metric metric,
                           IndexObject[] pivots,
                           List<? extends IndexObject> data,
                           int first,
                           int size,
                           List<? extends IndexObject> trainingQueries,
                           double queryRadius,
                           PowerDistanceLearningConfig config)
    {
        return optimize(metric, pivots, data, first, size, trainingQueries,
                queryRadius, config, 2);
    }

    public Result optimize(Metric metric,
                           IndexObject[] pivots,
                           List<? extends IndexObject> data,
                           int first,
                           int size,
                           List<? extends IndexObject> trainingQueries,
                           double queryRadius,
                           PowerDistanceLearningConfig config,
                           int numPartitions)
    {
        if (pivots.length != 2)
        {
            throw new IllegalArgumentException("optimizer requires exactly 2 pivots");
        }
        if (numPartitions < 2)
        {
            throw new IllegalArgumentException("numPartitions must be at least 2");
        }
        if (size <= 0)
        {
            return fallback(metric, pivots, data, first, size, config);
        }

        DistanceCache cache = new DistanceCache(metric, pivots, data, first, size,
                trainingQueries, queryRadius, config.getTrainingQuerySampleSize());
        List<double[]> directions = directions(config.getAngleCount());
        Result best = null;

        for (double rho : config.getRhoGrid())
        {
            PowerDistanceTransform transform =
                    new PowerDistanceTransform(rho, config.getEpsilonDistance());
            double[] y1 = transformDistances(cache.dataDistances1, transform);
            double[] y2 = transformDistances(cache.dataDistances2, transform);

            for (double[] direction : directions)
            {
                double w1 = direction[0];
                double w2 = direction[1];
                double[] scores = scores(y1, y2, w1, w2);
                double[] sorted = scores.clone();
                Arrays.sort(sorted);

                for (double[] thresholds : thresholdCandidates(sorted,
                        config.getTauQuantiles(), numPartitions))
                {
                    Counts counts = partitionCounts(scores, thresholds);
                    if (!balanced(counts.partitionSizes, size,
                            config.getMinBalance(), numPartitions))
                    {
                        continue;
                    }

                    Result candidate = evaluateCandidate(metric, pivots, cache,
                            transform, rho, w1, w2, thresholds, counts, scores,
                            config.getComparisonEpsilon());
                    if (isBetter(candidate, best))
                    {
                        best = candidate;
                    }
                }
            }
        }

        if (best == null)
        {
            return fallback(metric, pivots, data, first, size, config);
        }
        return best;
    }

    public Result fallback(Metric metric,
                           IndexObject[] pivots,
                           List<? extends IndexObject> data,
                           int first,
                           int size,
                           PowerDistanceLearningConfig config)
    {
        double rho = 1.0;
        double w1 = 1.0;
        double w2 = -1.0;
        PowerDistanceTransform transform =
                new PowerDistanceTransform(rho, config.getEpsilonDistance());
        double[] scores = new double[Math.max(size, 0)];
        for (int i = 0; i < scores.length; i++)
        {
            IndexObject x = data.get(first + i);
            double y1 = transform.transformPointDistance(metric.getDistance(x, pivots[0]));
            double y2 = transform.transformPointDistance(metric.getDistance(x, pivots[1]));
            scores[i] = safeScore(w1, y1, w2, y2);
        }
        double[] sorted = scores.clone();
        Arrays.sort(sorted);
        double tau = sorted.length == 0 ? 0.0 : sorted[sorted.length / 2];
        Counts counts = partitionCounts(scores, new double[]{tau});
        return new Result(rho, w1, w2, new double[]{tau}, counts.partitionSizes,
                false, 0.0, balanceScore(counts.left, counts.right),
                0.0, true, true);
    }

    public static double score(IndexObject x, Metric metric, IndexObject[] pivots,
                               double rho, double epsilonDistance,
                               double w1, double w2)
    {
        PowerDistanceTransform transform = new PowerDistanceTransform(rho, epsilonDistance);
        double y1 = transform.transformPointDistance(metric.getDistance(x, pivots[0]));
        double y2 = transform.transformPointDistance(metric.getDistance(x, pivots[1]));
        return safeScore(w1, y1, w2, y2);
    }

    public static Counts partitionCounts(double[] scores, double tau)
    {
        return partitionCounts(scores, new double[]{tau});
    }

    public static Counts partitionCounts(double[] scores, double[] thresholds)
    {
        int[] partitionSizes = new int[thresholds.length + 1];
        int equal = 0;
        for (double score : scores)
        {
            for (int i = 0; i < thresholds.length; i++)
            {
                if (score <= thresholds[i])
                {
                    partitionSizes[i]++;
                    if (score == thresholds[i])
                    {
                        equal++;
                    }
                    score = Double.NaN;
                    break;
                }
            }
            if (!Double.isNaN(score))
            {
                partitionSizes[thresholds.length]++;
            }
        }
        int left = partitionSizes.length > 0 ? partitionSizes[0] : 0;
        int right = partitionSizes.length > 1 ? partitionSizes[1] : 0;
        return new Counts(left, right, equal, partitionSizes);
    }

    private Result evaluateCandidate(Metric metric,
                                     IndexObject[] pivots,
                                     DistanceCache cache,
                                     PowerDistanceTransform transform,
                                     double rho,
                                     double w1,
                                     double w2,
                                     double[] thresholds,
                                     Counts counts,
                                     double[] scores,
                                     double comparisonEpsilon)
    {
        boolean queryAware = cache.hasQueries();
        double score = 0.0;
        double margin = 0.0;
        if (queryAware)
        {
            double[] weights = new double[]{w1, w2};
            double[][][] childRanges = childPivotDistanceRanges(cache.dataDistances1,
                    cache.dataDistances2, scores, thresholds, counts.partitionSizes.length);
            for (int i = 0; i < cache.queryCount; i++)
            {
                PowerDistanceTransform.Interval raw1 =
                        PowerDistanceTransform.queryDistanceInterval(cache.queryDistances1[i],
                                cache.queryRadius);
                PowerDistanceTransform.Interval raw2 =
                        PowerDistanceTransform.queryDistanceInterval(cache.queryDistances2[i],
                                cache.queryRadius);
                PowerDistanceTransform.Interval[] intervals =
                        new PowerDistanceTransform.Interval[]{
                                transform.transformQueryInterval(raw1.getLow(), raw1.getHigh()),
                                transform.transformQueryInterval(raw2.getLow(), raw2.getHigh())
                        };
                PowerDistanceTransform.Interval bounds =
                        PowerDistanceTransform.linearScoreBounds(weights, intervals);
                int visited = 0;
                double queryMargin = 0.0;
                for (int partition = 0; partition < counts.partitionSizes.length; partition++)
                {
                    double low = partition == 0 ? Double.NEGATIVE_INFINITY : thresholds[partition - 1];
                    double high = partition == counts.partitionSizes.length - 1
                            ? Double.POSITIVE_INFINITY : thresholds[partition];
                    boolean intersects = !(bounds.getHigh() < low - comparisonEpsilon
                            || bounds.getLow() > high + comparisonEpsilon);
                    intersects = intersects && rawIntersects(childRanges, partition,
                            raw1, raw2, comparisonEpsilon);
                    if (intersects)
                    {
                        visited += counts.partitionSizes[partition];
                    }
                    else
                    {
                        queryMargin += finiteMargin(distanceToInterval(bounds, low, high));
                    }
                }
                score += 1.0 - ((double) visited / (double) scores.length);
                margin += queryMargin;
            }
            score /= cache.queryCount;
            margin /= cache.queryCount;
        }
        else
        {
            score = balanceScore(counts.partitionSizes);
            margin = dataMargin(scores, thresholds);
        }
        return new Result(rho, w1, w2, thresholds, counts.partitionSizes,
                queryAware, score, balanceScore(counts.partitionSizes),
                margin, true, false);
    }

    private static double[][][] childPivotDistanceRanges(double[] distances1,
                                                         double[] distances2,
                                                         double[] scores,
                                                         double[] thresholds,
                                                         int numPartitions)
    {
        double[][][] ranges = new double[numPartitions][2][2];
        for (int partition = 0; partition < numPartitions; partition++)
        {
            for (int pivot = 0; pivot < 2; pivot++)
            {
                ranges[partition][pivot][0] = Double.POSITIVE_INFINITY;
                ranges[partition][pivot][1] = Double.NEGATIVE_INFINITY;
            }
        }
        for (int i = 0; i < scores.length; i++)
        {
            int partition = partitionIndex(scores[i], thresholds);
            updateRange(ranges[partition][0], distances1[i]);
            updateRange(ranges[partition][1], distances2[i]);
        }
        for (int partition = 0; partition < numPartitions; partition++)
        {
            for (int pivot = 0; pivot < 2; pivot++)
            {
                if (ranges[partition][pivot][0] == Double.POSITIVE_INFINITY)
                {
                    ranges[partition][pivot][0] = 0.0;
                    ranges[partition][pivot][1] = Double.POSITIVE_INFINITY;
                }
            }
        }
        return ranges;
    }

    private static int partitionIndex(double score, double[] thresholds)
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

    private static void updateRange(double[] range, double value)
    {
        if (value < range[0])
        {
            range[0] = value;
        }
        if (value > range[1])
        {
            range[1] = value;
        }
    }

    private static boolean rawIntersects(double[][][] childRanges, int partition,
                                         PowerDistanceTransform.Interval raw1,
                                         PowerDistanceTransform.Interval raw2,
                                         double eps)
    {
        return intervalIntersects(raw1, childRanges[partition][0], eps)
                && intervalIntersects(raw2, childRanges[partition][1], eps);
    }

    private static boolean intervalIntersects(PowerDistanceTransform.Interval queryRange,
                                              double[] childRange,
                                              double eps)
    {
        return !(queryRange.getHigh() < childRange[0] - eps
                || queryRange.getLow() > childRange[1] + eps);
    }

    private static boolean balanced(int[] partitionSizes, int size, double minBalance,
                                    int numPartitions)
    {
        double effectiveMinBalance = Math.min(minBalance, 0.5 / numPartitions);
        int min = (int) Math.ceil(size * effectiveMinBalance);
        for (int partitionSize : partitionSizes)
        {
            if (partitionSize < min)
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isBetter(Result candidate, Result best)
    {
        if (best == null)
        {
            return true;
        }
        if (candidate.score > best.score + BETTER_EPS)
        {
            return true;
        }
        if (candidate.score < best.score - BETTER_EPS)
        {
            return false;
        }
        if (candidate.balanceScore > best.balanceScore + BETTER_EPS)
        {
            return true;
        }
        if (candidate.balanceScore < best.balanceScore - BETTER_EPS)
        {
            return false;
        }
        return candidate.marginScore > best.marginScore + BETTER_EPS;
    }

    private static double[] transformDistances(double[] distances,
                                               PowerDistanceTransform transform)
    {
        double[] values = new double[distances.length];
        for (int i = 0; i < distances.length; i++)
        {
            values[i] = transform.transformPointDistance(distances[i]);
        }
        return values;
    }

    private static double[] scores(double[] y1, double[] y2, double w1, double w2)
    {
        double[] scores = new double[y1.length];
        for (int i = 0; i < scores.length; i++)
        {
            scores[i] = safeScore(w1, y1[i], w2, y2[i]);
        }
        return scores;
    }

    private static double safeScore(double w1, double y1, double w2, double y2)
    {
        double score = weightedTerm(w1, y1) + weightedTerm(w2, y2);
        if (Double.isNaN(score))
        {
            double t1 = weightedTerm(w1, y1);
            double t2 = weightedTerm(w2, y2);
            if (t1 == Double.POSITIVE_INFINITY || t2 == Double.POSITIVE_INFINITY)
            {
                return Double.POSITIVE_INFINITY;
            }
            if (t1 == Double.NEGATIVE_INFINITY || t2 == Double.NEGATIVE_INFINITY)
            {
                return Double.NEGATIVE_INFINITY;
            }
            return 0.0;
        }
        return score;
    }

    private static double weightedTerm(double weight, double value)
    {
        if (weight == 0.0)
        {
            return 0.0;
        }
        return weight * value;
    }

    private static double quantile(double[] sorted, double quantile)
    {
        if (sorted.length == 0)
        {
            return 0.0;
        }
        int index = (int) Math.round(quantile * (sorted.length - 1));
        if (index < 0)
        {
            index = 0;
        }
        if (index >= sorted.length)
        {
            index = sorted.length - 1;
        }
        return sorted[index];
    }

    private static List<double[]> thresholdCandidates(double[] sorted,
                                                      double[] configuredQuantiles,
                                                      int numPartitions)
    {
        List<double[]> candidates = new ArrayList<>();
        if (numPartitions == 2)
        {
            for (double quantile : configuredQuantiles)
            {
                candidates.add(new double[]{quantile(sorted, quantile)});
            }
            return candidates;
        }

        double[] balanced = new double[numPartitions - 1];
        for (int i = 1; i < numPartitions; i++)
        {
            balanced[i - 1] = quantile(sorted,
                    (double) i / (double) numPartitions);
        }
        addUniqueThresholds(candidates, balanced);

        int width = numPartitions - 1;
        for (int start = 0; start + width <= configuredQuantiles.length; start++)
        {
            double[] thresholds = new double[width];
            for (int i = 0; i < width; i++)
            {
                thresholds[i] = quantile(sorted, configuredQuantiles[start + i]);
            }
            addUniqueThresholds(candidates, thresholds);
        }
        return candidates;
    }

    private static void addUniqueThresholds(List<double[]> candidates,
                                            double[] thresholds)
    {
        for (int i = 1; i < thresholds.length; i++)
        {
            if (thresholds[i] < thresholds[i - 1])
            {
                return;
            }
        }
        for (double[] candidate : candidates)
        {
            if (Arrays.equals(candidate, thresholds))
            {
                return;
            }
        }
        candidates.add(thresholds.clone());
    }

    private static void combineThresholds(List<double[]> candidates,
                                          double[] current,
                                          int depth,
                                          int start,
                                          double[] sorted,
                                          double[] configuredQuantiles)
    {
        if (depth == current.length)
        {
            addUniqueThresholds(candidates, current);
            return;
        }
        int remaining = current.length - depth;
        for (int i = start; i <= configuredQuantiles.length - remaining; i++)
        {
            current[depth] = quantile(sorted, configuredQuantiles[i]);
            combineThresholds(candidates, current, depth + 1, i + 1,
                    sorted, configuredQuantiles);
        }
    }

    private static double balanceScore(int left, int right)
    {
        int total = left + right;
        if (total == 0)
        {
            return 0.0;
        }
        return (double) Math.min(left, right) / (double) total;
    }

    private static double balanceScore(int[] partitionSizes)
    {
        int total = 0;
        int min = Integer.MAX_VALUE;
        for (int partitionSize : partitionSizes)
        {
            total += partitionSize;
            min = Math.min(min, partitionSize);
        }
        if (total == 0 || min == Integer.MAX_VALUE)
        {
            return 0.0;
        }
        return (double) min / (double) total;
    }

    private static double dataMargin(double[] scores, double[] thresholds)
    {
        double margin = 0.0;
        for (double threshold : thresholds)
        {
            double lower = Double.NEGATIVE_INFINITY;
            double upper = Double.POSITIVE_INFINITY;
            for (double score : scores)
            {
                if (score <= threshold && score > lower)
                {
                    lower = score;
                }
                if (score >= threshold && score < upper)
                {
                    upper = score;
                }
            }
            if (Double.isFinite(lower) && Double.isFinite(upper))
            {
                margin += Math.max(0.0, upper - lower);
            }
        }
        return margin;
    }

    private static double distanceToInterval(PowerDistanceTransform.Interval bounds,
                                             double low, double high)
    {
        if (bounds.getHigh() < low)
        {
            return low - bounds.getHigh();
        }
        if (bounds.getLow() > high)
        {
            return bounds.getLow() - high;
        }
        return 0.0;
    }

    private static double finiteMargin(double value)
    {
        if (!Double.isFinite(value))
        {
            return Double.MAX_VALUE / 4.0;
        }
        return Math.max(0.0, value);
    }

    private static List<double[]> directions(int angleCount)
    {
        List<double[]> directions = new ArrayList<>();
        for (int i = 0; i < angleCount; i++)
        {
            double theta = Math.PI * i / angleCount;
            addDirection(directions, Math.cos(theta), Math.sin(theta));
        }
        addDirection(directions, 1.0, 0.0);
        addDirection(directions, 0.0, 1.0);
        addDirection(directions, 1.0, -1.0);
        addDirection(directions, 1.0, 1.0);
        return directions;
    }

    private static void addDirection(List<double[]> directions, double w1, double w2)
    {
        double norm = Math.sqrt(w1 * w1 + w2 * w2);
        if (norm == 0.0)
        {
            return;
        }
        double nw1 = w1 / norm;
        double nw2 = w2 / norm;
        for (double[] direction : directions)
        {
            if (Math.abs(direction[0] - nw1) < 1e-10
                    && Math.abs(direction[1] - nw2) < 1e-10)
            {
                return;
            }
        }
        directions.add(new double[]{nw1, nw2});
    }

    public static class Counts
    {
        public final int left;
        public final int right;
        public final int equal;
        public final int[] partitionSizes;

        public Counts(int left, int right, int equal, int[] partitionSizes)
        {
            this.left = left;
            this.right = right;
            this.equal = equal;
            this.partitionSizes = partitionSizes.clone();
        }
    }

    public static class Result
    {
        private final double rho;
        private final double w1;
        private final double w2;
        private final double[] thresholds;
        private final int[] partitionSizes;
        private final boolean queryAware;
        private final double score;
        private final double balanceScore;
        private final double marginScore;
        private final boolean valid;
        private final boolean fallback;

        public Result(double rho, double w1, double w2, double[] thresholds,
                      int[] partitionSizes, boolean queryAware,
                      double score, double balanceScore, double marginScore,
                      boolean valid, boolean fallback)
        {
            this.rho = rho;
            this.w1 = w1;
            this.w2 = w2;
            this.thresholds = thresholds.clone();
            this.partitionSizes = partitionSizes.clone();
            this.queryAware = queryAware;
            this.score = score;
            this.balanceScore = balanceScore;
            this.marginScore = marginScore;
            this.valid = valid;
            this.fallback = fallback;
        }

        public double getRho()
        {
            return rho;
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

        public int getLeftSize()
        {
            return partitionSizes.length == 0 ? 0 : partitionSizes[0];
        }

        public int getRightSize()
        {
            return partitionSizes.length < 2 ? 0 : partitionSizes[1];
        }

        public int[] getPartitionSizes()
        {
            return partitionSizes.clone();
        }

        public boolean isQueryAware()
        {
            return queryAware;
        }

        public double getScore()
        {
            return score;
        }

        public double getBalanceScore()
        {
            return balanceScore;
        }

        public double getMarginScore()
        {
            return marginScore;
        }

        public boolean isValid()
        {
            return valid;
        }

        public boolean isFallback()
        {
            return fallback;
        }

        public String directionSummary()
        {
            return "(" + w1 + "," + w2 + ")";
        }
    }

    private static class DistanceCache
    {
        private final double[] dataDistances1;
        private final double[] dataDistances2;
        private final double[] queryDistances1;
        private final double[] queryDistances2;
        private final int queryCount;
        private final double queryRadius;

        private DistanceCache(Metric metric,
                              IndexObject[] pivots,
                              List<? extends IndexObject> data,
                              int first,
                              int size,
                              List<? extends IndexObject> trainingQueries,
                              double queryRadius,
                              int maxQueries)
        {
            dataDistances1 = new double[size];
            dataDistances2 = new double[size];
            for (int i = 0; i < size; i++)
            {
                IndexObject x = data.get(first + i);
                dataDistances1[i] = metric.getDistance(x, pivots[0]);
                dataDistances2[i] = metric.getDistance(x, pivots[1]);
            }

            int requestedQueries = trainingQueries == null ? 0 : trainingQueries.size();
            if (maxQueries > 0)
            {
                requestedQueries = Math.min(requestedQueries, maxQueries);
            }
            queryCount = requestedQueries;
            this.queryRadius = queryRadius;
            queryDistances1 = new double[queryCount];
            queryDistances2 = new double[queryCount];
            for (int i = 0; i < queryCount; i++)
            {
                IndexObject query = trainingQueries.get(i);
                queryDistances1[i] = metric.getDistance(query, pivots[0]);
                queryDistances2[i] = metric.getDistance(query, pivots[1]);
            }
        }

        private boolean hasQueries()
        {
            return queryCount > 0 && queryRadius >= 0.0;
        }
    }
}
