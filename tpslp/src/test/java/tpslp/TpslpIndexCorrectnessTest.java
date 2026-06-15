package tpslp;

import db.type.DoubleIndexObjectPair;
import db.type.DoubleVector;
import db.type.IndexObject;
import metric.CountedMetric;
import metric.LMetric;
import metric.Metric;
import org.junit.Test;
import tpslp.coordinate.LogDistanceMap;
import tpslp.coordinate.PowerDistanceMap;
import tpslp.coordinate.PivotSpaceMap;
import tpslp.index.TpslpIndex;
import tpslp.index.TpslpSearchStats;
import tpslp.partition.DensityEnvelopePartitionLearner;
import tpslp.partition.ThresholdStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TpslpIndexCorrectnessTest
{
    @Test
    public void vpGhCghtAndRghMatchLinearScan()
    {
        Metric metric = LMetric.EuclideanDistanceMetric;
        List<IndexObject> data = sampleData();
        IndexObject[] pivots = new IndexObject[]{data.get(0), data.get(1)};
        List<TpslpIndex> indexes = List.of(
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new PivotSpaceMap())
                        .partitionLearner(TpslpConfigurations.vp(2, 0, 2))
                        .maxLeafSize(8)
                        .build(),
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new PivotSpaceMap())
                        .partitionLearner(TpslpConfigurations.gh(2))
                        .maxLeafSize(8)
                        .build(),
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new PivotSpaceMap())
                        .partitionLearner(TpslpConfigurations.cghtDifferenceAndSum(2))
                        .maxLeafSize(8)
                        .build(),
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new PivotSpaceMap())
                        .partitionLearner(TpslpConfigurations.rgh(0.75, 2))
                        .maxLeafSize(8)
                        .build()
        );

        assertMatchesLinearScan(metric, data, indexes);
    }

    @Test
    public void transformedSpacesMatchLinearScan()
    {
        Metric metric = LMetric.EuclideanDistanceMetric;
        List<IndexObject> data = sampleData();
        IndexObject[] pivots = new IndexObject[]{data.get(0), data.get(1)};
        List<TpslpIndex> indexes = List.of(
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new LogDistanceMap())
                        .partitionLearner(TpslpConfigurations.cghtSum(2))
                        .maxLeafSize(8)
                        .build(),
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new PowerDistanceMap(2.0))
                        .partitionLearner(TpslpConfigurations.freeLine(new double[]{1.0, 1.0}, 3))
                        .maxLeafSize(8)
                        .build()
        );

        assertMatchesLinearScan(metric, data, indexes);
    }

    @Test
    public void learnedTransformedSpacesMatchLinearScan()
    {
        Metric metric = LMetric.EuclideanDistanceMetric;
        List<IndexObject> data = sampleData();
        IndexObject[] pivots = new IndexObject[]{data.get(0), data.get(1)};
        List<IndexObject> trainingQueries = sampleQueries();
        List<TpslpIndex> indexes = List.of(
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new LogDistanceMap())
                        .partitionLearner(TpslpConfigurations.learnedLog(trainingQueries, 0.35))
                        .maxLeafSize(8)
                        .build(),
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new PowerDistanceMap(2.0))
                        .partitionLearner(TpslpConfigurations.learnedPower(
                                trainingQueries, 0.35))
                        .maxLeafSize(8)
                        .build()
        );

        assertMatchesLinearScan(metric, data, indexes);
    }

    @Test
    public void learnedPartitionModulesMatchLinearScan()
    {
        Metric metric = LMetric.EuclideanDistanceMetric;
        List<IndexObject> data = sampleData();
        IndexObject[] pivots = new IndexObject[]{data.get(0), data.get(1)};
        List<TpslpIndex> indexes = List.of(
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new PivotSpaceMap())
                        .partitionLearner(TpslpConfigurations.pca(ThresholdStrategy.MAX_GAP))
                        .maxLeafSize(8)
                        .build(),
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new PivotSpaceMap())
                        .partitionLearner(TpslpConfigurations.queryAdjustedPca(
                                data, 0.25, ThresholdStrategy.OTSU))
                        .maxLeafSize(8)
                        .build(),
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new PivotSpaceMap())
                        .partitionLearner(TpslpConfigurations.densityValley())
                        .maxLeafSize(8)
                        .build(),
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new PivotSpaceMap())
                        .partitionLearner(TpslpConfigurations.densityEnvelope(
                                4, 0.65,
                                DensityEnvelopePartitionLearner.Envelope.MBR,
                                0.02, 4))
                        .maxLeafSize(8)
                        .build(),
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new PivotSpaceMap())
                        .partitionLearner(TpslpConfigurations.densityEnvelope(
                                4, 0.65,
                                DensityEnvelopePartitionLearner.Envelope.PCA_SLAB,
                                0.02, 4))
                        .maxLeafSize(8)
                        .build()
        );

        assertMatchesLinearScan(metric, data, indexes);
    }

    @Test
    public void expectedExclusionThresholdsMatchLinearScan()
    {
        Metric metric = LMetric.EuclideanDistanceMetric;
        List<IndexObject> data = sampleData();
        IndexObject[] pivots = new IndexObject[]{data.get(0), data.get(1)};
        List<IndexObject> trainingQueries = sampleQueries();
        List<TpslpIndex> indexes = List.of(
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new PivotSpaceMap())
                        .partitionLearner(TpslpConfigurations.expectedExclusion(
                                new double[]{1.0, -1.0}, trainingQueries, 0.35, 1))
                        .maxLeafSize(8)
                        .build(),
                TpslpIndex.builder(data, metric)
                        .pivots(pivots)
                        .coordinateMap(new PivotSpaceMap())
                        .partitionLearner(TpslpConfigurations.expectedExclusion(
                                new double[]{1.0, -1.0}, trainingQueries, 0.35, 2))
                        .maxLeafSize(8)
                        .build()
        );

        assertMatchesLinearScan(metric, data, indexes);
    }

    @Test
    public void statsSearchMatchesRegularSearch()
    {
        CountedMetric metric = new CountedMetric(LMetric.EuclideanDistanceMetric);
        List<IndexObject> data = sampleData();
        TpslpIndex index = TpslpIndex.builder(data, metric)
                .pivots(data.get(0), data.get(1))
                .coordinateMap(new PivotSpaceMap())
                .partitionLearner(TpslpConfigurations.densityValley())
                .maxLeafSize(8)
                .build();

        IndexObject query = new DoubleVector(null, 2000, new double[]{0.4, -0.1});
        double radius = 0.45;
        metric.clear();
        TpslpSearchStats stats = index.rangeSearchWithStats(query, radius);

        assertEquals(stats.getMetricDistanceComputations(), metric.getCounter());
        assertEquals(stats.getDistanceComputations(), stats.getLeafDistanceComputations());
        assertEquals(stats.getMetricDistanceComputations(),
                stats.getLeafDistanceComputations()
                        + stats.getQueryPivotDistanceComputations());
        assertEquals(indexed(index, query, radius), rowIds(stats.getResults()));
    }

    private void assertMatchesLinearScan(Metric metric, List<IndexObject> data,
                                         List<TpslpIndex> indexes)
    {
        List<IndexObject> queries = List.of(
                new DoubleVector(null, 1000, new double[]{0.0, 0.0}),
                new DoubleVector(null, 1001, new double[]{0.5, 0.5}),
                new DoubleVector(null, 1002, new double[]{1.5, -0.2}),
                new DoubleVector(null, 1003, new double[]{-0.5, 1.1})
        );
        double[] radii = new double[]{0.0, 0.15, 0.35, 0.8};

        for (TpslpIndex index : indexes)
        {
            for (IndexObject query : queries)
            {
                for (double radius : radii)
                {
                    assertEquals(linear(metric, data, query, radius),
                            indexed(index, query, radius));
                }
            }
        }
    }

    private Set<Integer> indexed(TpslpIndex index, IndexObject query, double radius)
    {
        Set<Integer> result = new HashSet<>();
        for (DoubleIndexObjectPair pair : index.rangeSearch(query, radius))
        {
            result.add(pair.getObject().getRowID());
        }
        return result;
    }

    private Set<Integer> rowIds(List<DoubleIndexObjectPair> pairs)
    {
        Set<Integer> result = new HashSet<>();
        for (DoubleIndexObjectPair pair : pairs)
        {
            result.add(pair.getObject().getRowID());
        }
        return result;
    }

    private Set<Integer> linear(Metric metric, List<IndexObject> data,
                                IndexObject query, double radius)
    {
        Set<Integer> result = new HashSet<>();
        for (IndexObject point : data)
        {
            if (metric.getDistance(query, point) <= radius)
            {
                result.add(point.getRowID());
            }
        }
        return result;
    }

    private List<IndexObject> sampleData()
    {
        Random random = new Random(23L);
        List<IndexObject> data = new ArrayList<>();
        data.add(new DoubleVector(null, 0, new double[]{-1.0, 0.0}));
        data.add(new DoubleVector(null, 1, new double[]{1.0, 0.0}));
        for (int i = 2; i < 82; i++)
        {
            double x = random.nextDouble() * 4.0 - 2.0;
            double y = random.nextDouble() * 3.0 - 1.5;
            data.add(new DoubleVector(null, i, new double[]{x, y}));
        }
        return data;
    }

    private List<IndexObject> sampleQueries()
    {
        return List.of(
                new DoubleVector(null, 3000, new double[]{0.2, 0.1}),
                new DoubleVector(null, 3001, new double[]{1.3, 0.4}),
                new DoubleVector(null, 3002, new double[]{-1.0, -0.7}),
                new DoubleVector(null, 3003, new double[]{0.8, -1.1})
        );
    }
}
