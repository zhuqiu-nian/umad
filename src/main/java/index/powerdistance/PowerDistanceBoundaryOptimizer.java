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

                    Candidate candidate = evaluateCandidate(cache,
                            transform, rho, w1, w2, thresholds, counts, scores,
                            0, trainEnd, config);
                    if (cache.hasQueries())
                    {
                        offerTopCandidate(topCandidates, candidate,
                                config.getTopCandidates());
                    }
                    else if (isBetter(candidate, bestDataCandidate))
                    {
                        bestDataCandidate = candidate;
                    }
                }
            }
        }

        if (cache.hasQueries() && !topCandidates.isEmpty())
        {
            Candidate best = null;
            for (Candidate candidate : topCandidates)
            {
                Candidate validationCandidate = evaluateCandidate(cache,
                        candidate.transform, candidate.rho, candidate.w1,
                        candidate.w2, candidate.thresholds, candidate.counts,
                        candidate.scores, validationStart, validationEnd, config);
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

        if (topCandidates.isEmpty())
        {
            return fallback(metric, pivots, data, first, size, config);
        }
        return topCandidates.get(0).toResult();
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
                0.0, Double.POSITIVE_INFINITY, 0.0, 0.0, true, true);
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

    private Candidate evaluateCandidate(DistanceCache cache,
                                     PowerDistanceTransform transform,
                                     double rho,
                                     double w1,
                                     double w2,
                                     double[] thresholds,
                                     Counts counts,
                                     double[] scores,
                                     int queryStart,
                                     int queryEnd,
                                     PowerDistanceLearningConfig config)
    {
        boolean queryAware = cache.hasQueries();
        double score = 0.0;
        double margin = 0.0;
        double estimatedDistanceCost = Double.POSITIVE_INFINITY;
        double boxPenalty = 0.0;
        double childHitPenalty = 0.0;
        if (queryAware)
        {
            double[] weights = new double[]{w1, w2};
            double[][][] childRanges = childPivotDistanceRanges(cache.dataDistances1,
                    cache.dataDistances2, scores, thresholds, counts.partitionSizes.length);
            int[] partitions = partitionIndexes(scores, thresholds);
            int effectiveQueryStart = Math.max(0, Math.min(queryStart, cache.queryCount));
            int effectiveQueryEnd = Math.max(effectiveQueryStart,
                    Math.min(queryEnd, cache.queryCount));
            int evaluatedQueries = effectiveQueryEnd - effectiveQueryStart;
            double exactCandidates = 0.0;
            double childHits = 0.0;
            if (evaluatedQueries == 0)
            {
                effectiveQueryStart = 0;
                effectiveQueryEnd = cache.queryCount;
                evaluatedQueries = cache.queryCount;
            }
            for (int i = effectiveQueryStart; i < effectiveQueryEnd; i++)
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
                    visited += counts.partitionSizes[partition];
                    childHits += 1.0;
                    exactCandidates += leafSurvivorCount(cache, partitions, partition, i);
                }
                score += 1.0 - ((double) visited / (double) scores.length);
                margin += queryMargin;
            }
            score /= evaluatedQueries;
            margin /= evaluatedQueries;
            estimatedDistanceCost = exactCandidates / evaluatedQueries;
            childHitPenalty = childHits / evaluatedQueries;
            boxPenalty = boxPenalty(childRanges);
            score = 1.0 - estimatedDistanceCost / Math.max(1.0, scores.length);
            score -= config.getBoxPenaltyWeight() * boxPenalty;
            score -= config.getChildHitPenaltyWeight() * childHitPenalty;
        }
        else
        {
            score = balanceScore(counts.partitionSizes);
            margin = dataMargin(scores, thresholds);
            estimatedDistanceCost = scores.length * (1.0 - score);
        }
        return new Candidate(transform, rho, w1, w2, thresholds, counts, scores,
                queryAware, score, balanceScore(counts.partitionSizes),
                margin, estimatedDistanceCost, boxPenalty, childHitPenalty,
                false);
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

    private static int trainingEnd(DistanceCache cache,
                                   PowerDistanceLearningConfig config)
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

    private static int[] partitionIndexes(double[] scores, double[] thresholds)
    {
        int[] partitions = new int[scores.length];
        for (int i = 0; i < scores.length; i++)
        {
            partitions[i] = partitionIndex(scores[i], thresholds);
        }
        return partitions;
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

    private static double boxPenalty(double[][][] childRanges)
    {
        if (childRanges.length == 0)
        {
            return 0.0;
        }
        double rootWidth1 = 0.0;
        double rootWidth2 = 0.0;
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
        if (Double.isFinite(rootLow1) && Double.isFinite(rootHigh1))
        {
            rootWidth1 = Math.max(0.0, rootHigh1 - rootLow1);
        }
        if (Double.isFinite(rootLow2) && Double.isFinite(rootHigh2))
        {
            rootWidth2 = Math.max(0.0, rootHigh2 - rootLow2);
        }
        double normalizer = Math.max(1.0, rootWidth1 * rootWidth2);
        double areaSum = 0.0;
        for (double[][] childRange : childRanges)
        {
            areaSum += normalizedArea(childRange, normalizer);
        }
        double overlap = 0.0;
        for (int i = 0; i < childRanges.length; i++)
        {
            for (int j = i + 1; j < childRanges.length; j++)
            {
                overlap += overlapArea(childRanges[i], childRanges[j]) / normalizer;
            }
        }
        return areaSum + overlap;
    }

    private static double normalizedArea(double[][] range, double normalizer)
    {
        double width1 = finiteWidth(range[0]);
        double width2 = finiteWidth(range[1]);
        return (width1 * width2) / normalizer;
    }

    private static double overlapArea(double[][] a, double[][] b)
    {
        double width1 = overlapWidth(a[0], b[0]);
        double width2 = overlapWidth(a[1], b[1]);
        return width1 * width2;
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
            addUniqueThresholds(candidates, new double[]{quantile(sorted, 0.50)});
            for (double quantile : configuredQuantiles)
            {
                addUniqueThresholds(candidates, new double[]{quantile(sorted, quantile)});
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
        private final double estimatedDistanceCost;
        private final double boxPenalty;
        private final double childHitPenalty;
        private final boolean valid;
        private final boolean fallback;

        public Result(double rho, double w1, double w2, double[] thresholds,
                      int[] partitionSizes, boolean queryAware,
                      double score, double balanceScore, double marginScore,
                      double estimatedDistanceCost, double boxPenalty,
                      double childHitPenalty,
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
            this.estimatedDistanceCost = estimatedDistanceCost;
            this.boxPenalty = boxPenalty;
            this.childHitPenalty = childHitPenalty;
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

        public double getEstimatedDistanceCost()
        {
            return estimatedDistanceCost;
        }

        public double getBoxPenalty()
        {
            return boxPenalty;
        }

        public double getChildHitPenalty()
        {
            return childHitPenalty;
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

    private static class Candidate
    {
        private final PowerDistanceTransform transform;
        private final double rho;
        private final double w1;
        private final double w2;
        private final double[] thresholds;
        private final Counts counts;
        private final double[] scores;
        private final boolean queryAware;
        private final double score;
        private final double balanceScore;
        private final double marginScore;
        private final double estimatedDistanceCost;
        private final double boxPenalty;
        private final double childHitPenalty;
        private final boolean fallback;

        private Candidate(PowerDistanceTransform transform,
                          double rho,
                          double w1,
                          double w2,
                          double[] thresholds,
                          Counts counts,
                          double[] scores,
                          boolean queryAware,
                          double score,
                          double balanceScore,
                          double marginScore,
                          double estimatedDistanceCost,
                          double boxPenalty,
                          double childHitPenalty,
                          boolean fallback)
        {
            this.transform = transform;
            this.rho = rho;
            this.w1 = w1;
            this.w2 = w2;
            this.thresholds = thresholds.clone();
            this.counts = counts;
            this.scores = scores;
            this.queryAware = queryAware;
            this.score = score;
            this.balanceScore = balanceScore;
            this.marginScore = marginScore;
            this.estimatedDistanceCost = estimatedDistanceCost;
            this.boxPenalty = boxPenalty;
            this.childHitPenalty = childHitPenalty;
            this.fallback = fallback;
        }

        private Result toResult()
        {
            return new Result(rho, w1, w2, thresholds, counts.partitionSizes,
                    queryAware, score, balanceScore, marginScore,
                    estimatedDistanceCost, boxPenalty, childHitPenalty,
                    true, fallback);
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
