package index.logdistance;

import db.type.IndexObject;
import metric.Metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogDistanceBoundaryOptimizer
{
    private static final double BETTER_EPS = 1e-12;

    public Result optimize(Metric metric,
                           IndexObject[] pivots,
                           List<? extends IndexObject> data,
                           int first,
                           int size,
                           List<? extends IndexObject> trainingQueries,
                           double queryRadius,
                           LogDistanceLearningConfig config)
    {
        if (pivots.length != 2)
        {
            throw new IllegalArgumentException("optimizer requires exactly 2 pivots");
        }
        if (size <= 0)
        {
            return fallback(metric, pivots, data, first, size, config);
        }

        DistanceCache cache = new DistanceCache(metric, pivots, data, first, size,
                trainingQueries, queryRadius, config.getTrainingQuerySampleSize());
        LogDistanceTransform transform = new LogDistanceTransform(config.getEpsilonDistance());
        double[] y1 = transformDistances(cache.dataDistances1, transform);
        double[] y2 = transformDistances(cache.dataDistances2, transform);
        List<double[]> directions = directions(config.getAngleCount());
        List<Candidate> topCandidates = new ArrayList<>();
        Candidate bestDataCandidate = null;
        int trainEnd = trainingEnd(cache, config);
        int validationStart = trainEnd;
        int validationEnd = cache.queryCount;
        if (validationStart >= validationEnd)
        {
            validationStart = 0;
            validationEnd = cache.queryCount;
        }

        for (double[] direction : directions)
        {
            double w1 = direction[0];
            double w2 = direction[1];
            double[] scores = scores(y1, y2, w1, w2);
            double[] sorted = scores.clone();
            Arrays.sort(sorted);
            for (double tau : thresholdCandidates(sorted, config.getTauQuantiles()))
            {
                Counts counts = partitionCounts(scores, tau);
                if (!balanced(counts.left, counts.right, size, config.getMinBalance()))
                {
                    continue;
                }
                Candidate candidate = evaluateCandidate(cache, transform, w1, w2,
                        tau, counts, scores, 0, trainEnd, config);
                if (cache.hasQueries())
                {
                    offerTopCandidate(topCandidates, candidate, config.getTopCandidates());
                }
                else if (isBetter(candidate, bestDataCandidate))
                {
                    bestDataCandidate = candidate;
                }
            }
        }

        if (cache.hasQueries() && !topCandidates.isEmpty())
        {
            Candidate best = null;
            for (Candidate candidate : topCandidates)
            {
                Candidate validationCandidate = evaluateCandidate(cache, transform,
                        candidate.w1, candidate.w2, candidate.tau,
                        candidate.counts, candidate.scores,
                        validationStart, validationEnd, config);
                if (isBetter(validationCandidate, best))
                {
                    best = validationCandidate;
                }
            }
            if (best != null)
            {
                return best.toResult();
            }
        }

        if (bestDataCandidate != null)
        {
            return bestDataCandidate.toResult();
        }
        if (!topCandidates.isEmpty())
        {
            return topCandidates.get(0).toResult();
        }
        return fallback(metric, pivots, data, first, size, config);
    }

    public Result fallback(Metric metric,
                           IndexObject[] pivots,
                           List<? extends IndexObject> data,
                           int first,
                           int size,
                           LogDistanceLearningConfig config)
    {
        double w1 = 1.0;
        double w2 = -1.0;
        LogDistanceTransform transform = new LogDistanceTransform(config.getEpsilonDistance());
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
        Counts counts = partitionCounts(scores, tau);
        return new Result(w1, w2, tau, counts.left, counts.right,
                false, balanceScore(counts.left, counts.right),
                balanceScore(counts.left, counts.right), 0.0,
                Double.POSITIVE_INFINITY, 0.0, 0.0, true);
    }

    public static double score(IndexObject x, Metric metric, IndexObject[] pivots,
                               double epsilonDistance, double w1, double w2)
    {
        LogDistanceTransform transform = new LogDistanceTransform(epsilonDistance);
        double y1 = transform.transformPointDistance(metric.getDistance(x, pivots[0]));
        double y2 = transform.transformPointDistance(metric.getDistance(x, pivots[1]));
        return safeScore(w1, y1, w2, y2);
    }

    public static Counts partitionCounts(double[] scores, double tau)
    {
        int left = 0;
        int right = 0;
        int equal = 0;
        for (double score : scores)
        {
            if (score <= tau)
            {
                left++;
                if (score == tau)
                {
                    equal++;
                }
            }
            else
            {
                right++;
            }
        }
        return new Counts(left, right, equal);
    }

    private Candidate evaluateCandidate(DistanceCache cache,
                                        LogDistanceTransform transform,
                                        double w1,
                                        double w2,
                                        double tau,
                                        Counts counts,
                                        double[] scores,
                                        int queryStart,
                                        int queryEnd,
                                        LogDistanceLearningConfig config)
    {
        boolean queryAware = cache.hasQueries();
        double score;
        double margin;
        double estimatedDistanceCost;
        double boxPenalty = 0.0;
        double childHitPenalty = 0.0;
        if (queryAware)
        {
            double[] weights = new double[]{w1, w2};
            double[][][] childRanges = childPivotDistanceRanges(cache.dataDistances1,
                    cache.dataDistances2, scores, tau);
            int[] partitions = partitionIndexes(scores, tau);
            int effectiveQueryStart = Math.max(0, Math.min(queryStart, cache.queryCount));
            int effectiveQueryEnd = Math.max(effectiveQueryStart,
                    Math.min(queryEnd, cache.queryCount));
            int evaluatedQueries = effectiveQueryEnd - effectiveQueryStart;
            if (evaluatedQueries == 0)
            {
                effectiveQueryStart = 0;
                effectiveQueryEnd = cache.queryCount;
                evaluatedQueries = cache.queryCount;
            }
            double exactCandidates = 0.0;
            double childHits = 0.0;
            double queryMargin = 0.0;
            for (int i = effectiveQueryStart; i < effectiveQueryEnd; i++)
            {
                LogDistanceTransform.Interval raw1 =
                        LogDistanceTransform.queryDistanceInterval(cache.queryDistances1[i],
                                cache.queryRadius);
                LogDistanceTransform.Interval raw2 =
                        LogDistanceTransform.queryDistanceInterval(cache.queryDistances2[i],
                                cache.queryRadius);
                LogDistanceTransform.Interval[] intervals =
                        new LogDistanceTransform.Interval[]{
                                transform.transformQueryInterval(raw1.getLow(), raw1.getHigh()),
                                transform.transformQueryInterval(raw2.getLow(), raw2.getHigh())
                        };
                LogDistanceTransform.Interval bounds =
                        LogDistanceTransform.linearScoreBounds(weights, intervals);
                for (int partition = 0; partition < 2; partition++)
                {
                    double low = partition == 0 ? Double.NEGATIVE_INFINITY : tau;
                    double high = partition == 0 ? tau : Double.POSITIVE_INFINITY;
                    boolean rawDisjoint = !rawIntersects(childRanges, partition,
                            raw1, raw2, config.getComparisonEpsilon());
                    boolean scoreDisjoint = bounds.getHigh() < low - config.getComparisonEpsilon()
                            || bounds.getLow() > high + config.getComparisonEpsilon();
                    if (rawDisjoint || scoreDisjoint)
                    {
                        queryMargin += finiteMargin(distanceToInterval(bounds, low, high));
                        continue;
                    }
                    if (resultAll(cache, childRanges, partition, i,
                            config.getComparisonEpsilon()))
                    {
                        childHits += 1.0;
                        continue;
                    }
                    childHits += 1.0;
                    exactCandidates += leafSurvivorCount(cache, partitions, partition, i);
                }
            }
            estimatedDistanceCost = exactCandidates / evaluatedQueries;
            margin = queryMargin / evaluatedQueries;
            boxPenalty = boxPenalty(childRanges);
            childHitPenalty = childHits / evaluatedQueries;
            score = 1.0 - estimatedDistanceCost / Math.max(1.0, scores.length);
            score -= config.getBoxPenaltyWeight() * boxPenalty;
            score -= config.getChildHitPenaltyWeight() * childHitPenalty;
        }
        else
        {
            score = balanceScore(counts.left, counts.right);
            margin = dataMargin(scores, tau);
            estimatedDistanceCost = scores.length * (1.0 - score);
        }
        return new Candidate(w1, w2, tau, counts, scores, queryAware,
                score, balanceScore(counts.left, counts.right), margin,
                estimatedDistanceCost, boxPenalty, childHitPenalty);
    }

    private static double[][][] childPivotDistanceRanges(double[] distances1,
                                                         double[] distances2,
                                                         double[] scores,
                                                         double tau)
    {
        double[][][] ranges = new double[2][2][2];
        for (int child = 0; child < 2; child++)
        {
            for (int pivot = 0; pivot < 2; pivot++)
            {
                ranges[child][pivot][0] = Double.POSITIVE_INFINITY;
                ranges[child][pivot][1] = Double.NEGATIVE_INFINITY;
            }
        }
        for (int i = 0; i < scores.length; i++)
        {
            int partition = scores[i] <= tau ? 0 : 1;
            updateRange(ranges[partition][0], distances1[i]);
            updateRange(ranges[partition][1], distances2[i]);
        }
        for (int child = 0; child < 2; child++)
        {
            for (int pivot = 0; pivot < 2; pivot++)
            {
                if (ranges[child][pivot][0] == Double.POSITIVE_INFINITY)
                {
                    ranges[child][pivot][0] = 0.0;
                    ranges[child][pivot][1] = Double.POSITIVE_INFINITY;
                }
            }
        }
        return ranges;
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
                                         LogDistanceTransform.Interval raw1,
                                         LogDistanceTransform.Interval raw2,
                                         double eps)
    {
        return intervalIntersects(raw1, childRanges[partition][0], eps)
                && intervalIntersects(raw2, childRanges[partition][1], eps);
    }

    private static boolean intervalIntersects(LogDistanceTransform.Interval queryRange,
                                              double[] childRange,
                                              double eps)
    {
        return !(queryRange.getHigh() < childRange[0] - eps
                || queryRange.getLow() > childRange[1] + eps);
    }

    private static boolean resultAll(DistanceCache cache,
                                     double[][][] childRanges,
                                     int partition,
                                     int queryIndex,
                                     double eps)
    {
        return cache.queryDistances1[queryIndex] + childRanges[partition][0][1]
                <= cache.queryRadius + eps
                || cache.queryDistances2[queryIndex] + childRanges[partition][1][1]
                <= cache.queryRadius + eps;
    }

    private static int leafSurvivorCount(DistanceCache cache,
                                         int[] partitions,
                                         int partition,
                                         int queryIndex)
    {
        int count = 0;
        for (int i = 0; i < partitions.length; i++)
        {
            if (partitions[i] != partition)
            {
                continue;
            }
            if (Math.abs(cache.queryDistances1[queryIndex] - cache.dataDistances1[i])
                    <= cache.queryRadius
                    && Math.abs(cache.queryDistances2[queryIndex] - cache.dataDistances2[i])
                    <= cache.queryRadius)
            {
                count++;
            }
        }
        return count;
    }

    private static int[] partitionIndexes(double[] scores, double tau)
    {
        int[] partitions = new int[scores.length];
        for (int i = 0; i < scores.length; i++)
        {
            partitions[i] = scores[i] <= tau ? 0 : 1;
        }
        return partitions;
    }

    private static boolean balanced(int left, int right, int size, double minBalance)
    {
        int min = (int) Math.ceil(size * minBalance);
        return left >= min && right >= min;
    }

    private static int trainingEnd(DistanceCache cache,
                                   LogDistanceLearningConfig config)
    {
        if (!cache.hasQueries())
        {
            return 0;
        }
        if (cache.queryCount <= 1 || config.getValidationFraction() <= 0.0)
        {
            return cache.queryCount;
        }
        int validationCount = (int) Math.round(cache.queryCount
                * config.getValidationFraction());
        validationCount = Math.max(1, Math.min(cache.queryCount - 1, validationCount));
        return cache.queryCount - validationCount;
    }

    private static void offerTopCandidate(List<Candidate> candidates,
                                          Candidate candidate,
                                          int limit)
    {
        int insertAt = 0;
        while (insertAt < candidates.size()
                && !isBetter(candidate, candidates.get(insertAt)))
        {
            insertAt++;
        }
        candidates.add(insertAt, candidate);
        while (candidates.size() > limit)
        {
            candidates.remove(candidates.size() - 1);
        }
    }

    private static boolean isBetter(Candidate candidate, Candidate best)
    {
        if (best == null)
        {
            return true;
        }
        if (candidate.estimatedDistanceCost < best.estimatedDistanceCost - BETTER_EPS)
        {
            return true;
        }
        if (candidate.estimatedDistanceCost > best.estimatedDistanceCost + BETTER_EPS)
        {
            return false;
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
                                               LogDistanceTransform transform)
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

    private static double[] thresholdCandidates(double[] sorted,
                                                double[] configuredQuantiles)
    {
        double[] candidates = new double[configuredQuantiles.length + 1];
        candidates[0] = quantile(sorted, 0.50);
        for (int i = 0; i < configuredQuantiles.length; i++)
        {
            candidates[i + 1] = quantile(sorted, configuredQuantiles[i]);
        }
        return unique(candidates);
    }

    private static double[] unique(double[] values)
    {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int count = 0;
        for (double value : sorted)
        {
            if (count == 0 || value != sorted[count - 1])
            {
                sorted[count++] = value;
            }
        }
        return Arrays.copyOf(sorted, count);
    }

    private static double quantile(double[] sorted, double quantile)
    {
        if (sorted.length == 0)
        {
            return 0.0;
        }
        int index = (int) Math.round(quantile * (sorted.length - 1));
        index = Math.max(0, Math.min(sorted.length - 1, index));
        return sorted[index];
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

    private static double dataMargin(double[] scores, double tau)
    {
        double lower = Double.NEGATIVE_INFINITY;
        double upper = Double.POSITIVE_INFINITY;
        for (double score : scores)
        {
            if (score <= tau && score > lower)
            {
                lower = score;
            }
            if (score >= tau && score < upper)
            {
                upper = score;
            }
        }
        if (Double.isFinite(lower) && Double.isFinite(upper))
        {
            return Math.max(0.0, upper - lower);
        }
        return 0.0;
    }

    private static double distanceToInterval(LogDistanceTransform.Interval bounds,
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

    private static double boxPenalty(double[][][] childRanges)
    {
        double rootLow1 = Double.POSITIVE_INFINITY;
        double rootHigh1 = Double.NEGATIVE_INFINITY;
        double rootLow2 = Double.POSITIVE_INFINITY;
        double rootHigh2 = Double.NEGATIVE_INFINITY;
        for (double[][] childRange : childRanges)
        {
            rootLow1 = Math.min(rootLow1, childRange[0][0]);
            rootHigh1 = Math.max(rootHigh1, childRange[0][1]);
            rootLow2 = Math.min(rootLow2, childRange[1][0]);
            rootHigh2 = Math.max(rootHigh2, childRange[1][1]);
        }
        double width1 = finiteWidth(new double[]{rootLow1, rootHigh1});
        double width2 = finiteWidth(new double[]{rootLow2, rootHigh2});
        double normalizer = Math.max(1.0, width1 * width2);
        double areaSum = 0.0;
        for (double[][] childRange : childRanges)
        {
            areaSum += finiteWidth(childRange[0]) * finiteWidth(childRange[1]) / normalizer;
        }
        double overlap = overlapWidth(childRanges[0][0], childRanges[1][0])
                * overlapWidth(childRanges[0][1], childRanges[1][1]) / normalizer;
        return areaSum + overlap;
    }

    private static double finiteWidth(double[] range)
    {
        if (!Double.isFinite(range[0]) || !Double.isFinite(range[1]))
        {
            return 0.0;
        }
        return Math.max(0.0, range[1] - range[0]);
    }

    private static double overlapWidth(double[] a, double[] b)
    {
        if (!Double.isFinite(a[0]) || !Double.isFinite(a[1])
                || !Double.isFinite(b[0]) || !Double.isFinite(b[1]))
        {
            return 0.0;
        }
        return Math.max(0.0, Math.min(a[1], b[1]) - Math.max(a[0], b[0]));
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

        public Counts(int left, int right, int equal)
        {
            this.left = left;
            this.right = right;
            this.equal = equal;
        }
    }

    public static class Result
    {
        private final double w1;
        private final double w2;
        private final double tau;
        private final int leftSize;
        private final int rightSize;
        private final boolean queryAware;
        private final double score;
        private final double balanceScore;
        private final double marginScore;
        private final double estimatedDistanceCost;
        private final double boxPenalty;
        private final double childHitPenalty;
        private final boolean fallback;

        public Result(double w1, double w2, double tau, int leftSize,
                      int rightSize, boolean queryAware, double score,
                      double balanceScore, double marginScore,
                      double estimatedDistanceCost, double boxPenalty,
                      double childHitPenalty, boolean fallback)
        {
            this.w1 = w1;
            this.w2 = w2;
            this.tau = tau;
            this.leftSize = leftSize;
            this.rightSize = rightSize;
            this.queryAware = queryAware;
            this.score = score;
            this.balanceScore = balanceScore;
            this.marginScore = marginScore;
            this.estimatedDistanceCost = estimatedDistanceCost;
            this.boxPenalty = boxPenalty;
            this.childHitPenalty = childHitPenalty;
            this.fallback = fallback;
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

        public double getEstimatedDistanceCost()
        {
            return estimatedDistanceCost;
        }

        public String directionSummary()
        {
            return w1 + ";" + w2;
        }
    }

    private static class Candidate
    {
        private final double w1;
        private final double w2;
        private final double tau;
        private final Counts counts;
        private final double[] scores;
        private final boolean queryAware;
        private final double score;
        private final double balanceScore;
        private final double marginScore;
        private final double estimatedDistanceCost;
        private final double boxPenalty;
        private final double childHitPenalty;

        private Candidate(double w1, double w2, double tau, Counts counts,
                          double[] scores, boolean queryAware, double score,
                          double balanceScore, double marginScore,
                          double estimatedDistanceCost, double boxPenalty,
                          double childHitPenalty)
        {
            this.w1 = w1;
            this.w2 = w2;
            this.tau = tau;
            this.counts = counts;
            this.scores = scores;
            this.queryAware = queryAware;
            this.score = score;
            this.balanceScore = balanceScore;
            this.marginScore = marginScore;
            this.estimatedDistanceCost = estimatedDistanceCost;
            this.boxPenalty = boxPenalty;
            this.childHitPenalty = childHitPenalty;
        }

        private Result toResult()
        {
            return new Result(w1, w2, tau, counts.left, counts.right,
                    queryAware, score, balanceScore, marginScore,
                    estimatedDistanceCost, boxPenalty, childHitPenalty, false);
        }
    }

    private static class DistanceCache
    {
        private final double[] dataDistances1;
        private final double[] dataDistances2;
        private final double[] queryDistances1;
        private final double[] queryDistances2;
        private final double queryRadius;
        private final int queryCount;

        private DistanceCache(Metric metric, IndexObject[] pivots,
                              List<? extends IndexObject> data, int first, int size,
                              List<? extends IndexObject> trainingQueries,
                              double queryRadius, int queryLimit)
        {
            dataDistances1 = new double[size];
            dataDistances2 = new double[size];
            for (int i = 0; i < size; i++)
            {
                IndexObject x = data.get(first + i);
                dataDistances1[i] = metric.getDistance(x, pivots[0]);
                dataDistances2[i] = metric.getDistance(x, pivots[1]);
            }
            this.queryRadius = queryRadius;
            int limit = trainingQueries == null ? 0
                    : Math.min(trainingQueries.size(), queryLimit);
            queryDistances1 = new double[limit];
            queryDistances2 = new double[limit];
            for (int i = 0; i < limit; i++)
            {
                IndexObject q = trainingQueries.get(i);
                queryDistances1[i] = metric.getDistance(q, pivots[0]);
                queryDistances2[i] = metric.getDistance(q, pivots[1]);
            }
            queryCount = limit;
        }

        private boolean hasQueries()
        {
            return queryCount > 0;
        }
    }
}
