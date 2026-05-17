package app;

import algorithms.datapartition.GHPartitionMethods;
import algorithms.datapartition.PowerDistanceLearnedPartitionMethod;
import algorithms.datapartition.PowerDistanceLinearPartitionMethod;
import algorithms.datapartition.RGHPartitionMethods;
import algorithms.datapartition.VPPartitionMethods;
import algorithms.pivotselection.PivotSelectionMethod;
import algorithms.pivotselection.PivotSelectionMethods;
import algorithms.pivotselection.PowerDistanceMedoidPairPivotSelectionMethod;
import db.TableManager;
import db.table.DoubleVectorTable;
import db.table.ImageTable;
import db.table.PeptideTable;
import db.table.StringTable;
import db.table.Table;
import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.powerdistance.PowerDistanceBoundaryOptimizer;
import index.powerdistance.PowerDistanceLearningConfig;
import index.powerdistance.PowerDistanceTransform;
import index.search.Cursor;
import index.search.RangeQuery;
import index.type.HierarchicalPivotSelectionMode;
import metric.CountedMetric;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RealDatasetPowerExperiment
{
    private static final String CSV_HEADER =
            "dataset,type,dataSize,querySize,radius,indexName,rho,buildMs,"
                    + "avgDistanceCount,avgQueryMs,avgExclusionRate,avgPruneRate,"
                    + "avgInternalNodes,avgLeafNodes,avgResultCount,learnedRho,"
                    + "learnedDirectionSummary";

    public static void main(String[] args) throws Exception
    {
        Map<String, String> options = parseArgs(args);
        int dataSize = Integer.parseInt(options.getOrDefault("dataSize", "3000"));
        int querySize = Integer.parseInt(options.getOrDefault("querySize", "3000"));
        int maxLeafSize = Integer.parseInt(options.getOrDefault("maxLeafSize", "20"));
        int powerFanout = Integer.parseInt(options.getOrDefault("powerFanout", "2"));
        boolean includeLearned = Boolean.parseBoolean(options.getOrDefault("includeLearned", "false"));
        boolean verifyLinearScan = Boolean.parseBoolean(options.getOrDefault("verifyLinearScan", "false"));
        double[] rhos = parseDoubles(options.getOrDefault("rhoList", "-4,-2,-1,1,2,4"));
        PivotSelectionMethod pivotSelectionMethod =
                PivotSelectionMethods.valueOf(options.getOrDefault("pivotSelection", "FFT"));
        PowerDistanceLearningConfig learningConfig = learningConfig(options);

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        Path runDir = Paths.get(options.getOrDefault("outDir",
                "target/power-distance-real/run_" + timestamp));
        Path indexDir = runDir.resolve("indexes");
        Files.createDirectories(indexDir);

        List<DatasetSpec> datasets = defaultDatasets(options, dataSize, querySize);
        List<ResultRow> rows = new ArrayList<>();
        Path summaryPath = runDir.resolve("real_dataset_summary.csv");
        TableManager tableManager = TableManager.getTableManager(indexDir.resolve("manager").toString());
        try (BufferedWriter writer = Files.newBufferedWriter(summaryPath))
        {
            writer.write(CSV_HEADER);
            writer.newLine();
            for (DatasetSpec dataset : datasets)
            {
                List<? extends IndexObject> queries = dataset.loadQueries(querySize);
                List<IndexSpec> specs = indexSpecs(rhos, includeLearned, learningConfig,
                        trainingQueries(queries, learningConfig), dataset.radius,
                        powerFanout);
                for (IndexSpec spec : specs)
                {
                    String indexPrefix = indexDir.resolve(dataset.name + "_"
                            + spec.safeName() + "_" + System.nanoTime()).toString();
                    ResultRow row = runOne(tableManager, dataset, indexPrefix, dataSize,
                            maxLeafSize, pivotSelectionMethod, spec, queries,
                            verifyLinearScan);
                    rows.add(row);
                    writer.write(row.toCsv());
                    writer.newLine();
                    writer.flush();
                    System.out.println(row.shortLine());
                }
            }
        }
        finally
        {
            tableManager.close();
        }

        Path bestPath = runDir.resolve("real_dataset_best.csv");
        writeBest(bestPath, rows);
        printSummary(summaryPath, bestPath, rows);
    }

    private static ResultRow runOne(TableManager tableManager, DatasetSpec dataset,
                                    String indexPrefix, int dataSize, int maxLeafSize,
                                    PivotSelectionMethod pivotSelectionMethod,
                                    IndexSpec spec,
                                    List<? extends IndexObject> queries)
            throws Exception
    {
        return runOne(tableManager, dataset, indexPrefix, dataSize, maxLeafSize,
                pivotSelectionMethod, spec, queries, false);
    }

    private static ResultRow runOne(TableManager tableManager, DatasetSpec dataset,
                                    String indexPrefix, int dataSize, int maxLeafSize,
                                    PivotSelectionMethod pivotSelectionMethod,
                                    IndexSpec spec,
                                    List<? extends IndexObject> queries,
                                    boolean verifyLinearScan)
            throws Exception
    {
        Table table = dataset.loadTable(indexPrefix, dataSize);
        metric.Metric rawMetric = table.getMetric();
        List<IndexObject> originalData = verifyLinearScan
                ? new ArrayList<>(table.getData()) : null;
        CountedMetric countedMetric = new CountedMetric(rawMetric);
        table.setMetric(countedMetric);
        tableManager.putTable(table);

        long buildStart = System.nanoTime();
        spec.build(table, pivotSelectionMethod, maxLeafSize);
        long buildMs = (System.nanoTime() - buildStart) / 1_000_000L;

        Stats stats = new Stats();
        for (IndexObject query : queries)
        {
            countedMetric.clear();
            Cursor cursor = table.getIndex().search(new RangeQuery(query, dataset.radius));
            Set<Integer> indexedResult = verifyLinearScan ? drainToSet(cursor) : null;
            int resultCount = verifyLinearScan ? indexedResult.size() : drain(cursor);
            if (verifyLinearScan)
            {
                Set<Integer> linearResult = linearScan(rawMetric, originalData,
                        query, dataset.radius);
                if (!indexedResult.equals(linearResult))
                {
                    throw new IllegalStateException("range query mismatch: dataset="
                            + dataset.name + ", index=" + spec.indexName
                            + ", query=" + query + ", indexed="
                            + indexedResult.size() + ", linear=" + linearResult.size());
                }
            }
            stats.add(cursor, countedMetric.getCounter(), resultCount);
        }

        return new ResultRow(dataset.name, dataset.type, table.size(), queries.size(),
                dataset.radius, spec.indexName, spec.rho, buildMs, stats,
                spec.learnedRho(), spec.learnedDirectionSummary());
    }

    private static int drain(Cursor cursor)
    {
        int count = 0;
        while (cursor.hasNext())
        {
            DoubleIndexObjectPair ignored = cursor.next();
            count++;
        }
        return count;
    }

    private static Set<Integer> drainToSet(Cursor cursor)
    {
        Set<Integer> result = new HashSet<>();
        while (cursor.hasNext())
        {
            result.add(cursor.next().getObject().getRowID());
        }
        return result;
    }

    private static Set<Integer> linearScan(metric.Metric metric,
                                           List<? extends IndexObject> data,
                                           IndexObject query,
                                           double radius)
    {
        Set<Integer> result = new HashSet<>();
        for (IndexObject x : data)
        {
            if (metric.getDistance(query, x) <= radius)
            {
                result.add(x.getRowID());
            }
        }
        return result;
    }

    private static List<IndexSpec> indexSpecs(double[] rhos, boolean includeLearned,
                                              PowerDistanceLearningConfig learningConfig,
                                              List<? extends IndexObject> trainingQueries,
                                              double radius, int powerFanout)
    {
        List<IndexSpec> specs = new ArrayList<>();
        specs.add(IndexSpec.gh());
        specs.add(IndexSpec.vp(1));
        specs.add(IndexSpec.vp(2));
        specs.add(IndexSpec.rgh());
        for (double rho : rhos)
        {
            specs.add(IndexSpec.fixedPower(rho, powerFanout));
        }
        if (includeLearned)
        {
            specs.add(IndexSpec.learnedPower(learningConfig, trainingQueries, radius,
                    powerFanout));
        }
        return specs;
    }

    private static List<? extends IndexObject> trainingQueries(List<? extends IndexObject> queries,
                                                               PowerDistanceLearningConfig config)
    {
        int limit = Math.min(queries.size(), config.getTrainingQuerySampleSize());
        List<IndexObject> sample = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++)
        {
            sample.add(queries.get(i));
        }
        return sample;
    }

    private static List<DatasetSpec> defaultDatasets(Map<String, String> options,
                                                     int dataSize,
                                                     int querySize)
            throws Exception
    {
        String only = options.get("datasets");
        Map<String, Double> radiusOverrides = radiusOverrides(options);
        List<DatasetSpec> datasets = new ArrayList<>();
        addIfSelected(datasets, only, withRadiusOverride(DatasetSpec.vector("clusteredvector",
                "data/vector/clusteredvector-2d-100k-100c.txt", 2, 0.15), radiusOverrides));
        addIfSelected(datasets, only, withRadiusOverride(DatasetSpec.vector("hawii",
                "data/vector/hawii.txt", 2, 0.05), radiusOverrides));
        addIfSelected(datasets, only, withRadiusOverride(DatasetSpec.vector("texas",
                "data/vector/texas.txt", 2, 0.05), radiusOverrides));
        addIfSelected(datasets, only, withRadiusOverride(DatasetSpec.vector("uniform5",
                "data/vector/Uniform-5-d-vector.txt", 5, 0.50), radiusOverrides));
        addIfSelected(datasets, only, withRadiusOverride(DatasetSpec.vector("uniform20",
                "data/vector/Uniform-20-d-vector.txt", 20, 1.20), radiusOverrides));
        addIfSelected(datasets, only, withRadiusOverride(DatasetSpec.image("image", "data", 0.05), radiusOverrides));
        addIfSelected(datasets, only, withRadiusOverride(DatasetSpec.english("english",
                "data/English.dic", 2.0), radiusOverrides));
        addIfSelected(datasets, only, withRadiusOverride(DatasetSpec.protein("protein",
                "data/yeast.aa", 5, 2.0), radiusOverrides));

        for (DatasetSpec dataset : datasets)
        {
            dataset.dataSizeHint = dataSize;
            dataset.querySizeHint = querySize;
        }
        return datasets;
    }

    private static DatasetSpec withRadiusOverride(DatasetSpec spec,
                                                  Map<String, Double> radiusOverrides)
    {
        Double radius = radiusOverrides.get(spec.name.toLowerCase(Locale.ROOT));
        if (radius == null)
        {
            radius = radiusOverrides.get("*");
        }
        return radius == null ? spec : spec.withRadius(radius);
    }

    private static Map<String, Double> radiusOverrides(Map<String, String> options)
    {
        Map<String, Double> overrides = new HashMap<>();
        if (options.containsKey("radius"))
        {
            overrides.put("*", Double.parseDouble(options.get("radius")));
        }
        String raw = options.get("radiusOverrides");
        if (raw == null || raw.trim().isEmpty())
        {
            return overrides;
        }
        for (String entry : raw.split(","))
        {
            String[] parts = entry.trim().split(":");
            if (parts.length != 2)
            {
                throw new IllegalArgumentException("radiusOverrides entry must be name:value, got " + entry);
            }
            overrides.put(parts[0].trim().toLowerCase(Locale.ROOT),
                    Double.parseDouble(parts[1].trim()));
        }
        return overrides;
    }

    private static void addIfSelected(List<DatasetSpec> datasets, String only,
                                      DatasetSpec spec)
    {
        if (only == null || containsName(only, spec.name))
        {
            datasets.add(spec);
        }
    }

    private static boolean containsName(String raw, String name)
    {
        String[] parts = raw.split(",");
        for (String part : parts)
        {
            if (part.trim().equalsIgnoreCase(name))
            {
                return true;
            }
        }
        return false;
    }

    private static void writeBest(Path bestPath, List<ResultRow> rows)
            throws Exception
    {
        try (BufferedWriter writer = Files.newBufferedWriter(bestPath))
        {
            writer.write("dataset,bestIndex,bestAvgDistance,bestPowerIndex,bestPowerAvgDistance,bestTraditionalIndex,bestTraditionalAvgDistance,powerVsTraditionalRatio");
            writer.newLine();
            for (String dataset : datasetNames(rows))
            {
                ResultRow best = null;
                ResultRow bestPower = null;
                ResultRow bestTraditional = null;
                for (ResultRow row : rows)
                {
                    if (!row.dataset.equals(dataset))
                    {
                        continue;
                    }
                    best = better(best, row);
                    if (row.indexName.startsWith("POWER"))
                    {
                        bestPower = better(bestPower, row);
                    }
                    else
                    {
                        bestTraditional = better(bestTraditional, row);
                    }
                }
                double ratio = bestPower == null || bestTraditional == null
                        ? Double.NaN
                        : bestPower.avgDistanceCount / bestTraditional.avgDistanceCount;
                writer.write(dataset + "," + best.indexName + "," + best.avgDistanceCount
                        + "," + value(bestPower == null ? null : bestPower.indexName)
                        + "," + value(bestPower == null ? null : bestPower.avgDistanceCount)
                        + "," + value(bestTraditional == null ? null : bestTraditional.indexName)
                        + "," + value(bestTraditional == null ? null : bestTraditional.avgDistanceCount)
                        + "," + ratio);
                writer.newLine();
            }
        }
    }

    private static void printSummary(Path summaryPath, Path bestPath,
                                     List<ResultRow> rows)
    {
        System.out.println();
        System.out.println("Summary CSV: " + summaryPath.toAbsolutePath());
        System.out.println("Best CSV: " + bestPath.toAbsolutePath());
        System.out.println();
        System.out.println("Best rows by dataset (avg distance count):");
        for (String dataset : datasetNames(rows))
        {
            ResultRow best = null;
            for (ResultRow row : rows)
            {
                if (row.dataset.equals(dataset))
                {
                    best = better(best, row);
                }
            }
            if (best != null)
            {
                System.out.println(best.shortLine());
            }
        }
    }

    private static List<String> datasetNames(List<ResultRow> rows)
    {
        List<String> names = new ArrayList<>();
        for (ResultRow row : rows)
        {
            if (!names.contains(row.dataset))
            {
                names.add(row.dataset);
            }
        }
        return names;
    }

    private static ResultRow better(ResultRow current, ResultRow candidate)
    {
        if (current == null || candidate.avgDistanceCount < current.avgDistanceCount)
        {
            return candidate;
        }
        return current;
    }

    private static String value(Object value)
    {
        return value == null ? "" : value.toString();
    }

    private static Map<String, String> parseArgs(String[] args)
    {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.startsWith("--"))
            {
                if (i + 1 >= args.length)
                {
                    throw new IllegalArgumentException("missing value for " + arg);
                }
                options.put(arg.substring(2), args[++i]);
            }
        }
        return options;
    }

    private static double[] parseDoubles(String raw)
    {
        String[] parts = raw.split(",");
        double[] values = new double[parts.length];
        for (int i = 0; i < parts.length; i++)
        {
            values[i] = Double.parseDouble(parts[i].trim());
        }
        return values;
    }

    private static PowerDistanceLearningConfig learningConfig(Map<String, String> options)
    {
        String preset = options.getOrDefault("learningPreset", "medium").toLowerCase(Locale.ROOT);
        String defaultRhoGrid = "-4,-2,-1,1,2,4";
        String defaultTauQuantiles = "0.35,0.40,0.45,0.50,0.55,0.60,0.65";
        String defaultAngleCount = "12";
        String defaultTrainingQuerySampleSize = "128";
        String defaultMedoidCandidateCount = "6";
        String defaultMedoidIterations = "1";
        String defaultValidationFraction = "0.0";
        String defaultTopCandidates = "16";
        String defaultBoxPenaltyWeight = "0.0001";
        String defaultChildHitPenaltyWeight = "0.0001";
        if ("strong".equals(preset) || "costaware".equals(preset))
        {
            defaultRhoGrid = "-4,-2,-1,-0.5,0.5,1,2,4";
            defaultAngleCount = "16";
            defaultTrainingQuerySampleSize = "256";
            defaultMedoidCandidateCount = "8";
            defaultMedoidIterations = "2";
        }
        if ("costaware".equals(preset))
        {
            defaultValidationFraction = "0.30";
            defaultTopCandidates = "16";
        }
        else if (!"medium".equals(preset) && !"fast".equals(preset)
                && !"strong".equals(preset))
        {
            throw new IllegalArgumentException("learningPreset must be fast, medium, strong, or costAware");
        }
        if ("fast".equals(preset))
        {
            defaultRhoGrid = "-4,-2,2,4";
            defaultTauQuantiles = "0.4,0.5,0.6";
            defaultAngleCount = "8";
            defaultTrainingQuerySampleSize = "64";
            defaultMedoidCandidateCount = "4";
        }
        return new PowerDistanceLearningConfig(
                parseDoubles(options.getOrDefault("learningRhoGrid", defaultRhoGrid)),
                Integer.parseInt(options.getOrDefault("angleCount", defaultAngleCount)),
                parseDoubles(options.getOrDefault("tauQuantiles", defaultTauQuantiles)),
                Double.parseDouble(options.getOrDefault("minBalance", "0.25")),
                Integer.parseInt(options.getOrDefault("trainingQuerySampleSize", defaultTrainingQuerySampleSize)),
                Integer.parseInt(options.getOrDefault("medoidCandidateCount", defaultMedoidCandidateCount)),
                Integer.parseInt(options.getOrDefault("medoidIterations", defaultMedoidIterations)),
                PowerDistanceTransform.DEFAULT_EPSILON_DISTANCE,
                PowerDistanceTransform.DEFAULT_COMPARISON_EPSILON,
                Double.parseDouble(options.getOrDefault("validationFraction", defaultValidationFraction)),
                Integer.parseInt(options.getOrDefault("topCandidates", defaultTopCandidates)),
                Double.parseDouble(options.getOrDefault("boxPenaltyWeight", defaultBoxPenaltyWeight)),
                Double.parseDouble(options.getOrDefault("childHitPenaltyWeight", defaultChildHitPenaltyWeight)));
    }

    private static class DatasetSpec
    {
        private final String name;
        private final String type;
        private final String path;
        private final int dimOrFragmentLength;
        private final double radius;
        private int dataSizeHint;
        private int querySizeHint;

        private DatasetSpec(String name, String type, String path,
                            int dimOrFragmentLength, double radius)
        {
            this.name = name;
            this.type = type;
            this.path = path;
            this.dimOrFragmentLength = dimOrFragmentLength;
            this.radius = radius;
        }

        static DatasetSpec vector(String name, String path, int dim, double radius)
        {
            return new DatasetSpec(name, "vector", path, dim, radius);
        }

        static DatasetSpec image(String name, String path, double radius)
        {
            return new DatasetSpec(name, "image", path, 0, radius);
        }

        static DatasetSpec english(String name, String path, double radius)
        {
            return new DatasetSpec(name, "english", path, 0, radius);
        }

        static DatasetSpec protein(String name, String path, int fragmentLength,
                                   double radius)
        {
            return new DatasetSpec(name, "protein", path, fragmentLength, radius);
        }

        DatasetSpec withRadius(double newRadius)
        {
            return new DatasetSpec(name, type, path, dimOrFragmentLength, newRadius);
        }

        Table loadTable(String indexPrefix, int size) throws Exception
        {
            if ("vector".equals(type))
            {
                return new DoubleVectorTable(path, indexPrefix, size, dimOrFragmentLength);
            }
            if ("image".equals(type))
            {
                return new ImageTable(path, indexPrefix, size);
            }
            if ("english".equals(type))
            {
                return new StringTable(path, indexPrefix, size);
            }
            if ("protein".equals(type))
            {
                return new PeptideTable(path, indexPrefix, size, dimOrFragmentLength);
            }
            throw new IllegalStateException("unknown dataset type: " + type);
        }

        List<? extends IndexObject> loadQueries(int requestedSize) throws Exception
        {
            Table table = loadTable("query_" + name + "_" + System.nanoTime(),
                    requestedSize);
            return table.getData();
        }
    }

    private static class IndexSpec
    {
        private final String indexName;
        private final double rho;
        private final int vpPivots;
        private final int powerFanout;
        private final PowerDistanceLearningConfig learningConfig;
        private final List<? extends IndexObject> trainingQueries;
        private final double queryRadius;
        private PowerDistanceLearnedPartitionMethod learnedPartitionMethod;

        private IndexSpec(String indexName, double rho, int vpPivots,
                          int powerFanout,
                          PowerDistanceLearningConfig learningConfig,
                          List<? extends IndexObject> trainingQueries,
                          double queryRadius)
        {
            this.indexName = indexName;
            this.rho = rho;
            this.vpPivots = vpPivots;
            this.powerFanout = powerFanout;
            this.learningConfig = learningConfig;
            this.trainingQueries = trainingQueries;
            this.queryRadius = queryRadius;
        }

        static IndexSpec gh()
        {
            return new IndexSpec("GH", Double.NaN, 0, 0, null, null, Double.NaN);
        }

        static IndexSpec vp(int pivots)
        {
            return new IndexSpec("VP" + pivots, Double.NaN, pivots, 0,
                    null, null, Double.NaN);
        }

        static IndexSpec rgh()
        {
            return new IndexSpec("RGH", Double.NaN, 0, 0, null, null, Double.NaN);
        }

        static IndexSpec fixedPower(double rho, int powerFanout)
        {
            return new IndexSpec("POWER_FIXED_f" + powerFanout + "_rho_" + rho,
                    rho, 0, powerFanout,
                    null, null, Double.NaN);
        }

        static IndexSpec learnedPower(PowerDistanceLearningConfig config,
                                      List<? extends IndexObject> trainingQueries,
                                      double queryRadius, int powerFanout)
        {
            return new IndexSpec("POWER_LEARNED_MEDOID_f" + powerFanout,
                    Double.NaN, 0, powerFanout,
                    config, trainingQueries, queryRadius);
        }

        void build(Table table, PivotSelectionMethod pivotSelectionMethod,
                   int maxLeafSize)
        {
            if ("GH".equals(indexName))
            {
                table.buildGHIndex(pivotSelectionMethod, GHPartitionMethods.GH,
                        maxLeafSize, HierarchicalPivotSelectionMode.LOCAL);
            }
            else if ("RGH".equals(indexName))
            {
                table.buildRGHIndex(pivotSelectionMethod, RGHPartitionMethods.RGH,
                        maxLeafSize, HierarchicalPivotSelectionMode.LOCAL);
            }
            else if (indexName.startsWith("VP"))
            {
                table.buildVPIndex(pivotSelectionMethod, vpPivots,
                        VPPartitionMethods.BALANCED, 2, maxLeafSize,
                        HierarchicalPivotSelectionMode.LOCAL, null);
            }
            else if (indexName.startsWith("POWER_LEARNED_MEDOID"))
            {
                learnedPartitionMethod = new PowerDistanceLearnedPartitionMethod(
                        learningConfig, trainingQueries, queryRadius);
                table.buildPowerDistanceIndex(
                        new PowerDistanceMedoidPairPivotSelectionMethod(
                                learningConfig, trainingQueries, queryRadius,
                                powerFanout),
                        learnedPartitionMethod, powerFanout, maxLeafSize,
                        HierarchicalPivotSelectionMode.LOCAL, null);
            }
            else if (indexName.startsWith("POWER_FIXED"))
            {
                table.buildPowerDistanceIndex(pivotSelectionMethod,
                        new PowerDistanceLinearPartitionMethod(rho, 1.0, -1.0),
                        powerFanout, maxLeafSize, HierarchicalPivotSelectionMode.LOCAL, null);
            }
            else
            {
                throw new IllegalStateException("unknown index: " + indexName);
            }
        }

        String safeName()
        {
            return indexName.replace("-", "m").replace(".", "p");
        }

        double learnedRho()
        {
            PowerDistanceBoundaryOptimizer.Result model =
                    learnedPartitionMethod == null ? null : learnedPartitionMethod.getRootModel();
            return model == null ? Double.NaN : model.getRho();
        }

        String learnedDirectionSummary()
        {
            PowerDistanceBoundaryOptimizer.Result model =
                    learnedPartitionMethod == null ? null : learnedPartitionMethod.getRootModel();
            return model == null ? "" : model.getW1() + ";" + model.getW2();
        }
    }

    private static class Stats
    {
        private double distanceCount;
        private double queryNs;
        private double exclusionRate;
        private double pruneRate;
        private double internalNodes;
        private double leafNodes;
        private double resultCount;
        private int count;

        void add(Cursor cursor, int distances, int results)
        {
            distanceCount += distances;
            queryNs += cursor.nsOfSearchTime;
            exclusionRate += finiteOrZero(cursor.averageExclusionRate);
            pruneRate += finiteOrZero(cursor.averagePruneRate);
            internalNodes += cursor.numberOfInternalNodeSearches;
            leafNodes += cursor.numberOfLeafNodeSearches;
            resultCount += results;
            count++;
        }

        private double finiteOrZero(double value)
        {
            return Double.isFinite(value) ? value : 0.0;
        }
    }

    private static class ResultRow
    {
        private final String dataset;
        private final String type;
        private final long dataSize;
        private final long querySize;
        private final double radius;
        private final String indexName;
        private final double rho;
        private final long buildMs;
        private final double avgDistanceCount;
        private final double avgQueryMs;
        private final double avgExclusionRate;
        private final double avgPruneRate;
        private final double avgInternalNodes;
        private final double avgLeafNodes;
        private final double avgResultCount;
        private final double learnedRho;
        private final String learnedDirectionSummary;

        private ResultRow(String dataset, String type, long dataSize, long querySize,
                          double radius, String indexName, double rho,
                          long buildMs, Stats stats, double learnedRho,
                          String learnedDirectionSummary)
        {
            this.dataset = dataset;
            this.type = type;
            this.dataSize = dataSize;
            this.querySize = querySize;
            this.radius = radius;
            this.indexName = indexName;
            this.rho = rho;
            this.buildMs = buildMs;
            this.avgDistanceCount = stats.distanceCount / stats.count;
            this.avgQueryMs = stats.queryNs / stats.count / 1_000_000.0;
            this.avgExclusionRate = stats.exclusionRate / stats.count;
            this.avgPruneRate = stats.pruneRate / stats.count;
            this.avgInternalNodes = stats.internalNodes / stats.count;
            this.avgLeafNodes = stats.leafNodes / stats.count;
            this.avgResultCount = stats.resultCount / stats.count;
            this.learnedRho = learnedRho;
            this.learnedDirectionSummary = learnedDirectionSummary;
        }

        String toCsv()
        {
            return dataset + "," + type + "," + dataSize + "," + querySize + ","
                    + radius + "," + indexName + "," + rho + "," + buildMs + ","
                    + avgDistanceCount + "," + avgQueryMs + "," + avgExclusionRate + ","
                    + avgPruneRate + "," + avgInternalNodes + "," + avgLeafNodes + ","
                    + avgResultCount + "," + learnedRho + ","
                    + learnedDirectionSummary;
        }

        String shortLine()
        {
            return dataset + " " + indexName
                    + " avgDist=" + String.format(Locale.US, "%.2f", avgDistanceCount)
                    + " avgMs=" + String.format(Locale.US, "%.4f", avgQueryMs)
                    + " prune=" + String.format(Locale.US, "%.4f", avgPruneRate);
        }
    }
}
