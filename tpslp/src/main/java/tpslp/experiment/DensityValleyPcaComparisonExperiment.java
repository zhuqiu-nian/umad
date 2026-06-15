package tpslp.experiment;

import algorithms.pivotselection.PivotSelectionMethods;
import db.table.ImageTable;
import db.table.PeptideTable;
import db.table.StringTable;
import db.table.Table;
import db.type.DoubleVector;
import db.type.IndexObject;
import metric.LMetric;
import metric.Metric;
import tpslp.TpslpConfigurations;
import tpslp.coordinate.CoordinateMap;
import tpslp.coordinate.LogDistanceMap;
import tpslp.coordinate.PivotSpaceMap;
import tpslp.coordinate.PowerDistanceMap;
import tpslp.index.TpslpIndex;
import tpslp.index.TpslpSearchStats;
import tpslp.partition.PartitionLearner;
import tpslp.partition.LinearBoundary;
import tpslp.partition.LinearSlabPartitionLearner;
import tpslp.partition.ThresholdStrategy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

public final class DensityValleyPcaComparisonExperiment
{
    private static final Path ROOT = Path.of("C:/Users/zjy/Desktop/研究生/umad/umad");
    private static final int INDEX_SIZE = 1000;
    private static final int TRAIN_QUERY_SIZE = 120;
    private static final int TEST_QUERY_SIZE = 180;
    private static final int MAX_LEAF_SIZE = 20;
    private static final int NUM_PIVOTS = 3;
    private static final double RADIUS_QUANTILE = 0.03;
    private static final Random RANDOM = new Random(20260615L);

    private DensityValleyPcaComparisonExperiment()
    {
    }

    public static void main(String[] args) throws Exception
    {
        Locale.setDefault(Locale.US);
        List<DataSetSpec> dataSets = List.of(
                new DataSetSpec("vector20", "vector", () -> loadNumeric(
                        ROOT.resolve("data/vectors-20.dat"), 20, INDEX_SIZE + TRAIN_QUERY_SIZE + TEST_QUERY_SIZE)),
                new DataSetSpec("allfeas-vector", "vector", () -> loadNumeric(
                        ROOT.resolve("data/allfeas.dat"), 66, INDEX_SIZE + TRAIN_QUERY_SIZE + TEST_QUERY_SIZE)),
                new DataSetSpec("image", "image", () -> loadTable(new ImageTable(
                        ROOT.resolve("data").toString(), "tpslp-image-exp",
                        INDEX_SIZE + TRAIN_QUERY_SIZE + TEST_QUERY_SIZE))),
                new DataSetSpec("english", "string", () -> loadTable(new StringTable(
                        ROOT.resolve("data/English.dic").toString(), "tpslp-english-exp",
                        INDEX_SIZE + TRAIN_QUERY_SIZE + TEST_QUERY_SIZE))),
                new DataSetSpec("yeast-protein", "protein", () -> loadTable(new PeptideTable(
                        ROOT.resolve("data/yeast.aa").toString(), "tpslp-protein-exp",
                        INDEX_SIZE + TRAIN_QUERY_SIZE + TEST_QUERY_SIZE, 12)))
        );

        List<MapSpec> maps = List.of(
                new MapSpec("pivot", PivotSpaceMap::new),
                new MapSpec("log", LogDistanceMap::new),
                new MapSpec("power0.5", () -> new PowerDistanceMap(0.5)),
                new MapSpec("power2", () -> new PowerDistanceMap(2.0))
        );

        List<String> lines = new ArrayList<>();
        lines.add("dataset,type,map,learner,radius,avgMetricCalls,avgLeafDistanceComputations,avgInternalNodes,avgLeafNodes,avgResults,buildMillis,queryMillis");

        for (DataSetSpec dataSet : dataSets)
        {
            LoadedData loaded;
            try
            {
                loaded = dataSet.loader.get();
            } catch (Throwable throwable)
            {
                System.out.println("SKIP " + dataSet.name + ": " + throwable.getMessage());
                continue;
            }
            if (loaded.data.size() < INDEX_SIZE + TRAIN_QUERY_SIZE + TEST_QUERY_SIZE)
            {
                System.out.println("SKIP " + dataSet.name + ": only " + loaded.data.size() + " objects loaded");
                continue;
            }

            List<IndexObject> indexData = new ArrayList<>(loaded.data.subList(0, INDEX_SIZE));
            List<IndexObject> trainingQueries = new ArrayList<>(
                    loaded.data.subList(INDEX_SIZE, INDEX_SIZE + TRAIN_QUERY_SIZE));
            List<IndexObject> testQueries = new ArrayList<>(
                    loaded.data.subList(INDEX_SIZE + TRAIN_QUERY_SIZE,
                            INDEX_SIZE + TRAIN_QUERY_SIZE + TEST_QUERY_SIZE));
            double radius = estimateRadius(loaded.metric, indexData, testQueries);

            for (MapSpec mapSpec : maps)
            {
                List<LearnerSpec> learners = List.of(
                        new LearnerSpec("baseline-VP1", () ->
                                TpslpConfigurations.vp(NUM_PIVOTS, 0, 2)),
                        new LearnerSpec("baseline-MVP", () ->
                                TpslpConfigurations.mvp(NUM_PIVOTS, 2)),
                        new LearnerSpec("baseline-GH12", () ->
                                TpslpConfigurations.freeLine(new double[]{1.0, -1.0, 0.0}, 2)),
                        new LearnerSpec("baseline-CGHT-sum12", () ->
                                TpslpConfigurations.freeLine(new double[]{1.0, 1.0, 0.0}, 2)),
                        new LearnerSpec("baseline-CGHT-diff-sum12", () ->
                                new LinearSlabPartitionLearner(List.of(
                                        new LinearBoundary(1.0, -1.0, 0.0),
                                        new LinearBoundary(1.0, 1.0, 0.0)), 2)),
                        new LearnerSpec("baseline-RGH12-0.75", () ->
                                TpslpConfigurations.freeLine(new double[]{1.0, -0.75, 0.0}, 2)),
                        new LearnerSpec("baseline-CP-like", () ->
                                TpslpConfigurations.cpLike(NUM_PIVOTS, 2)),
                        new LearnerSpec("PCA-MaxGap", () ->
                                TpslpConfigurations.pca(ThresholdStrategy.MAX_GAP)),
                        new LearnerSpec("QueryAdjustedPCA", () ->
                                TpslpConfigurations.queryAdjustedPca(trainingQueries, radius,
                                        ThresholdStrategy.MAX_GAP)),
                        new LearnerSpec("DensityValley", TpslpConfigurations::densityValley),
                        new LearnerSpec("DensityValleyQ", () ->
                                TpslpConfigurations.densityValley(0.1, trainingQueries, radius))
                );

                for (LearnerSpec learnerSpec : learners)
                {
                    runOne(lines, dataSet, loaded.metric, indexData, trainingQueries, testQueries,
                            radius, mapSpec, learnerSpec);
                }
            }
        }

        Path out = ROOT.resolve("target/density_valley_pca_comparison_counted.csv");
        Files.createDirectories(out.getParent());
        Files.write(out, lines);
        System.out.println("WROTE " + out);
    }

    private static void runOne(List<String> lines,
                               DataSetSpec dataSet,
                               Metric metric,
                               List<IndexObject> indexData,
                               List<IndexObject> trainingQueries,
                               List<IndexObject> testQueries,
                               double radius,
                               MapSpec mapSpec,
                               LearnerSpec learnerSpec)
    {
        long buildStart = System.currentTimeMillis();
        TpslpIndex index = TpslpIndex.builder(indexData, metric)
                .pivotSelection(PivotSelectionMethods.FFT, NUM_PIVOTS)
                .coordinateMap(mapSpec.factory.get())
                .partitionLearner(learnerSpec.factory.get())
                .maxLeafSize(MAX_LEAF_SIZE)
                .build();
        long buildMillis = System.currentTimeMillis() - buildStart;

        long queryStart = System.currentTimeMillis();
        long metricCalls = 0;
        long leafDistances = 0;
        long internalNodes = 0;
        long leafNodes = 0;
        long results = 0;
        for (IndexObject query : testQueries)
        {
            TpslpSearchStats stats = index.rangeSearchWithStats(query, radius);
            metricCalls += stats.getMetricDistanceComputations();
            leafDistances += stats.getLeafDistanceComputations();
            internalNodes += stats.getVisitedInternalNodes();
            leafNodes += stats.getVisitedLeafNodes();
            results += stats.getResults().size();
        }
        long queryMillis = System.currentTimeMillis() - queryStart;
        double n = testQueries.size();
        String line = String.format(Locale.US, "%s,%s,%s,%s,%.8f,%.4f,%.4f,%.4f,%.4f,%.4f,%d,%d",
                dataSet.name, dataSet.type, mapSpec.name, learnerSpec.name, radius,
                metricCalls / n, leafDistances / n, internalNodes / n, leafNodes / n, results / n,
                buildMillis, queryMillis);
        lines.add(line);
        System.out.println(line);
    }

    private static double estimateRadius(Metric metric, List<IndexObject> data, List<IndexObject> queries)
    {
        int sampleSize = Math.min(5000, data.size() * Math.min(queries.size(), 20));
        double[] distances = new double[sampleSize];
        for (int i = 0; i < sampleSize; i++)
        {
            IndexObject query = queries.get(RANDOM.nextInt(queries.size()));
            IndexObject point = data.get(RANDOM.nextInt(data.size()));
            distances[i] = metric.getDistance(query, point);
        }
        Arrays.sort(distances);
        int index = Math.max(0, Math.min(distances.length - 1,
                (int) Math.round(RADIUS_QUANTILE * (distances.length - 1))));
        double radius = distances[index];
        if (radius <= 0.0)
        {
            for (double distance : distances)
            {
                if (distance > 0.0)
                {
                    return distance;
                }
            }
            return 0.0;
        }
        return radius;
    }

    private static LoadedData loadTable(Table table)
    {
        return new LoadedData(new ArrayList<>(table.getData()), table.getMetric());
    }

    private static LoadedData loadNumeric(Path path, int dimension, int limit) throws IOException
    {
        List<IndexObject> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile())))
        {
            String line;
            int rowId = 0;
            while ((line = reader.readLine()) != null && data.size() < limit)
            {
                line = line.trim();
                if (line.isEmpty())
                {
                    continue;
                }
                String[] tokens = line.split("[ \t]+");
                double[] values = parseNumericRow(tokens, dimension);
                if (values == null)
                {
                    continue;
                }
                data.add(new DoubleVector(null, rowId++, values));
            }
        }
        return new LoadedData(data, LMetric.EuclideanDistanceMetric);
    }

    private static double[] parseNumericRow(String[] tokens, int dimension)
    {
        if (tokens.length == dimension)
        {
            return parse(tokens, 0, dimension);
        }
        if (tokens.length > dimension)
        {
            return parse(tokens, tokens.length - dimension, dimension);
        }
        return null;
    }

    private static double[] parse(String[] tokens, int offset, int dimension)
    {
        double[] values = new double[dimension];
        try
        {
            for (int i = 0; i < dimension; i++)
            {
                values[i] = Double.parseDouble(tokens[offset + i]);
            }
            return values;
        } catch (NumberFormatException ex)
        {
            return null;
        }
    }

    private static final class LoadedData
    {
        private final List<IndexObject> data;
        private final Metric metric;

        private LoadedData(List<IndexObject> data, Metric metric)
        {
            this.data = data;
            this.metric = metric;
        }
    }

    private static final class DataSetSpec
    {
        private final String name;
        private final String type;
        private final SupplierWithException<LoadedData> loader;

        private DataSetSpec(String name, String type, SupplierWithException<LoadedData> loader)
        {
            this.name = name;
            this.type = type;
            this.loader = loader;
        }
    }

    private static final class MapSpec
    {
        private final String name;
        private final Supplier<CoordinateMap> factory;

        private MapSpec(String name, Supplier<CoordinateMap> factory)
        {
            this.name = name;
            this.factory = factory;
        }
    }

    private static final class LearnerSpec
    {
        private final String name;
        private final Supplier<PartitionLearner> factory;

        private LearnerSpec(String name, Supplier<PartitionLearner> factory)
        {
            this.name = name;
            this.factory = factory;
        }
    }

    @FunctionalInterface
    private interface SupplierWithException<T>
    {
        T get() throws Exception;
    }
}
