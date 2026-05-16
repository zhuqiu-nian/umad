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
import db.table.Table;
import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.powerdistance.PowerDistanceBoundaryOptimizer;
import index.powerdistance.PowerDistanceLearningConfig;
import index.search.Cursor;
import index.search.RangeQuery;
import index.type.HierarchicalPivotSelectionMode;
import metric.CountedMetric;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.Random;
import java.util.Set;

/**
 * Generates clustered vector datasets and compares PowerDistanceIndex with
 * classic metric indexes on exact range search.
 */
public class ClusteredPowerDistanceExperiment
{
    private static final String CSV_HEADER =
            "scenario,dim,dataSize,querySize,clusterCount,clusterStd,radius,"
                    + "indexName,rho,w1,w2,maxLeafSize,buildMs,avgDistanceCount,"
                    + "avgQueryMs,avgExclusionRate,avgPruneRate,avgInternalNodes,"
                    + "avgLeafNodes,avgResultCount,learnedRho,learnedDirectionSummary,"
                    + "correctnessChecked,correct";

    public static void main(String[] args) throws Exception
    {
        Map<String, String> options = parseArgs(args);
        int dim = Integer.parseInt(options.getOrDefault("dim", "2"));
        int dataSize = Integer.parseInt(options.getOrDefault("dataSize", "3000"));
        int querySize = Integer.parseInt(options.getOrDefault("querySize", "3000"));
        int maxLeafSize = Integer.parseInt(options.getOrDefault("maxLeafSize", "20"));
        long seed = Long.parseLong(options.getOrDefault("seed", "20260515"));
        double centerSpread = Double.parseDouble(options.getOrDefault("centerSpread", "3.0"));
        double[] clusterStds = parseDoubles(options.getOrDefault("clusterStds", "0.04,0.12,0.24"));
        int[] clusterCounts = parseInts(options.getOrDefault("clusterCounts", "3,6,12"));
        double[] radii = parseDoubles(options.getOrDefault("radii", "0.15"));
        double[] rhos = parseDoubles(options.getOrDefault("rhoList", "-4,-2,-1,1,2,4"));
        double w1 = Double.parseDouble(options.getOrDefault("w1", "1.0"));
        double w2 = Double.parseDouble(options.getOrDefault("w2", "-1.0"));
        int correctnessQueries = Integer.parseInt(options.getOrDefault("correctnessQueries", "20"));
        PowerDistanceLearningConfig learningConfig = learningConfig(options);
        PivotSelectionMethod pivotSelectionMethod =
                PivotSelectionMethods.valueOf(options.getOrDefault("pivotSelection", "FFT"));
        HierarchicalPivotSelectionMode mode =
                HierarchicalPivotSelectionMode.valueOf(options.getOrDefault("pivotMode", "LOCAL"));

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        Path runDir = Paths.get(options.getOrDefault("outDir",
                "target/power-distance-clustered/run_" + timestamp));
        Path dataDir = runDir.resolve("data");
        Path indexDir = runDir.resolve("indexes");
        Files.createDirectories(dataDir);
        Files.createDirectories(indexDir);

        Path summaryPath = runDir.resolve("clustered_summary.csv");
        Path bestPowerPath = runDir.resolve("clustered_best_power.csv");
        TableManager tableManager = TableManager.getTableManager(indexDir.resolve("manager").toString());

        List<ResultRow> rows = new ArrayList<>();
        try (BufferedWriter summary = Files.newBufferedWriter(summaryPath))
        {
            summary.write(CSV_HEADER);
            summary.newLine();

            int scenarioId = 0;
            for (int clusterCount : clusterCounts)
            {
                for (double clusterStd : clusterStds)
                {
                    scenarioId++;
                    Scenario scenario = generateScenario(dim, dataSize, querySize, clusterCount,
                            clusterStd, centerSpread, seed + scenarioId * 7919L);
                    String scenarioName = "c" + clusterCount + "_std" + safeNumber(clusterStd);
                    Path dataFile = dataDir.resolve(scenarioName + "_data.txt");
                    Path queryFile = dataDir.resolve(scenarioName + "_query.txt");
                    writeVectorFile(dataFile, scenario.data);
                    writeVectorFile(queryFile, scenario.queries);

                    Table queryTable = new DoubleVectorTable(queryFile.toString(),
                            "query_" + scenarioName, querySize, dim);
                    List<? extends IndexObject> queryObjects = queryTable.getData();

                    for (double radius : radii)
                    {
                        List<? extends IndexObject> trainingQueries =
                                trainingQueries(queryObjects, learningConfig);
                        List<IndexSpec> specs = buildIndexSpecs(rhos, w1, w2,
                                trainingQueries, radius, learningConfig);
                        for (IndexSpec spec : specs)
                        {
                            String indexPrefix = indexDir.resolve(scenarioName + "_"
                                    + spec.safeName() + "_r" + safeNumber(radius)
                                    + "_" + System.nanoTime()).toString();
                            ResultRow row = runOne(tableManager, dataFile.toString(), scenarioName,
                                    dim, dataSize, querySize, clusterCount, clusterStd, radius,
                                    maxLeafSize, pivotSelectionMethod, mode, spec, indexPrefix,
                                    queryObjects, correctnessQueries);
                            rows.add(row);
                            summary.write(row.toCsv());
                            summary.newLine();
                            summary.flush();
                            System.out.println(row.shortLine());
                        }
                    }
                }
            }
        }
        finally
        {
            tableManager.close();
        }

        writeBestPowerSummary(bestPowerPath, rows);
        printHumanSummary(rows, summaryPath, bestPowerPath);
    }

    private static ResultRow runOne(TableManager tableManager,
                                    String dataFile,
                                    String scenarioName,
                                    int dim,
                                    int dataSize,
                                    int querySize,
                                    int clusterCount,
                                    double clusterStd,
                                    double radius,
                                    int maxLeafSize,
                                    PivotSelectionMethod pivotSelectionMethod,
                                    HierarchicalPivotSelectionMode mode,
                                    IndexSpec spec,
                                    String indexPrefix,
                                    List<? extends IndexObject> queries,
                                    int correctnessQueries)
            throws IOException
    {
        Table dataTable = new DoubleVectorTable(dataFile, indexPrefix, dataSize, dim);
        CountedMetric countedMetric = new CountedMetric(dataTable.getMetric());
        dataTable.setMetric(countedMetric);
        tableManager.putTable(dataTable);

        long buildStart = System.nanoTime();
        spec.build(dataTable, pivotSelectionMethod, mode, maxLeafSize);
        long buildMs = (System.nanoTime() - buildStart) / 1_000_000L;

        Stats stats = new Stats();
        boolean correct = true;
        int checked = Math.min(correctnessQueries, queries.size());
        for (int i = 0; i < queries.size(); i++)
        {
            IndexObject query = queries.get(i);
            countedMetric.clear();
            Cursor cursor = dataTable.getIndex().search(new RangeQuery(query, radius));
            Set<Integer> indexRows = i < checked ? new HashSet<>() : null;
            int resultCount = drain(cursor, indexRows);
            stats.add(cursor, countedMetric.getCounter(), resultCount);

            if (i < checked)
            {
                Set<Integer> linearRows = linearSearchRows(dataTable, query, radius);
                if (!indexRows.equals(linearRows))
                {
                    correct = false;
                }
            }
        }

        return new ResultRow(scenarioName, dim, dataSize, querySize, clusterCount,
                clusterStd, radius, spec.indexName, spec.rho, spec.w1, spec.w2,
                maxLeafSize, buildMs, stats, spec.learnedRho(),
                spec.learnedDirectionSummary(), checked, correct);
    }

    private static int drain(Cursor cursor, Set<Integer> rows)
    {
        int count = 0;
        while (cursor.hasNext())
        {
            DoubleIndexObjectPair pair = cursor.next();
            if (rows != null)
            {
                rows.add(pair.getObject().getRowID());
            }
            count++;
        }
        return count;
    }

    private static Set<Integer> linearSearchRows(Table table, IndexObject query, double radius)
    {
        Set<Integer> rows = new HashSet<>();
        for (DoubleIndexObjectPair pair : table.searchByLinear(new RangeQuery(query, radius)))
        {
            rows.add(pair.getObject().getRowID());
        }
        return rows;
    }

    private static List<IndexSpec> buildIndexSpecs(double[] rhos, double w1, double w2,
                                                   List<? extends IndexObject> trainingQueries,
                                                   double radius,
                                                   PowerDistanceLearningConfig learningConfig)
    {
        List<IndexSpec> specs = new ArrayList<>();
        specs.add(IndexSpec.gh());
        specs.add(IndexSpec.vp(1));
        specs.add(IndexSpec.vp(2));
        specs.add(IndexSpec.rgh());
        for (double rho : rhos)
        {
            specs.add(IndexSpec.fixedPower(rho, w1, w2));
        }
        specs.add(IndexSpec.learnedPower(trainingQueries, radius, learningConfig));
        return specs;
    }

    private static List<? extends IndexObject> trainingQueries(
            List<? extends IndexObject> queries, PowerDistanceLearningConfig config)
    {
        int limit = Math.min(queries.size(), config.getTrainingQuerySampleSize());
        List<IndexObject> sample = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++)
        {
            sample.add(queries.get(i));
        }
        return sample;
    }

    private static Scenario generateScenario(int dim,
                                             int dataSize,
                                             int querySize,
                                             int clusterCount,
                                             double clusterStd,
                                             double centerSpread,
                                             long seed)
    {
        Random random = new Random(seed);
        double[][] centers = new double[clusterCount][dim];
        for (int i = 0; i < clusterCount; i++)
        {
            for (int j = 0; j < dim; j++)
            {
                centers[i][j] = (random.nextDouble() * 2.0 - 1.0) * centerSpread;
            }
        }
        return new Scenario(sampleClustered(random, centers, dataSize, clusterStd),
                sampleClustered(random, centers, querySize, clusterStd));
    }

    private static List<double[]> sampleClustered(Random random,
                                                  double[][] centers,
                                                  int size,
                                                  double clusterStd)
    {
        List<double[]> rows = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            double[] center = centers[i % centers.length];
            double[] row = new double[center.length];
            for (int j = 0; j < row.length; j++)
            {
                row[j] = center[j] + random.nextGaussian() * clusterStd;
            }
            rows.add(row);
        }
        return rows;
    }

    private static void writeVectorFile(Path file, List<double[]> rows) throws IOException
    {
        Files.createDirectories(file.getParent());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile())))
        {
            int dim = rows.get(0).length;
            writer.write(dim + " " + rows.size());
            writer.newLine();
            for (double[] row : rows)
            {
                for (int i = 0; i < row.length; i++)
                {
                    if (i > 0)
                    {
                        writer.write(' ');
                    }
                    writer.write(Double.toString(row[i]));
                }
                writer.newLine();
            }
        }
    }

    private static void writeBestPowerSummary(Path bestPowerPath, List<ResultRow> rows)
            throws IOException
    {
        try (BufferedWriter writer = Files.newBufferedWriter(bestPowerPath))
        {
            writer.write("scenario,radius,bestPowerIndex,bestPowerRho,bestPowerAvgDistance,"
                    + "bestTraditionalIndex,bestTraditionalAvgDistance,powerDistanceRatio");
            writer.newLine();
            for (String key : scenarioRadiusKeys(rows))
            {
                ResultRow bestPower = null;
                ResultRow bestTraditional = null;
                for (ResultRow row : rows)
                {
                    if (!row.scenarioRadiusKey().equals(key))
                    {
                        continue;
                    }
                    if (row.indexName.startsWith("POWER"))
                    {
                        bestPower = betterDistance(bestPower, row);
                    }
                    else
                    {
                        bestTraditional = betterDistance(bestTraditional, row);
                    }
                }
                if (bestPower != null && bestTraditional != null)
                {
                    double ratio = bestPower.avgDistanceCount / bestTraditional.avgDistanceCount;
                    writer.write(bestPower.scenario + "," + bestPower.radius + ","
                            + bestPower.indexName + "," + bestPower.effectivePowerRho()
                            + "," + bestPower.avgDistanceCount + ","
                            + bestTraditional.indexName + ","
                            + bestTraditional.avgDistanceCount + "," + ratio);
                    writer.newLine();
                }
            }
        }
    }

    private static List<String> scenarioRadiusKeys(List<ResultRow> rows)
    {
        List<String> keys = new ArrayList<>();
        for (ResultRow row : rows)
        {
            String key = row.scenarioRadiusKey();
            if (!keys.contains(key))
            {
                keys.add(key);
            }
        }
        return keys;
    }

    private static ResultRow betterDistance(ResultRow current, ResultRow candidate)
    {
        if (current == null || candidate.avgDistanceCount < current.avgDistanceCount)
        {
            return candidate;
        }
        return current;
    }

    private static void printHumanSummary(List<ResultRow> rows,
                                          Path summaryPath,
                                          Path bestPowerPath)
    {
        System.out.println();
        System.out.println("Summary CSV: " + summaryPath.toAbsolutePath());
        System.out.println("Best power CSV: " + bestPowerPath.toAbsolutePath());
        System.out.println();
        System.out.println("Best rows by scenario/radius (avg distance count):");
        for (String key : scenarioRadiusKeys(rows))
        {
            ResultRow best = null;
            for (ResultRow row : rows)
            {
                if (row.scenarioRadiusKey().equals(key))
                {
                    best = betterDistance(best, row);
                }
            }
            if (best != null)
            {
                System.out.println(best.shortLine());
            }
        }
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
        double[] learningRhoGrid = parseDoubles(options.getOrDefault("learningRhoGrid",
                "-4,-2,-1,-0.5,0.5,1,2,4"));
        int angleCount = Integer.parseInt(options.getOrDefault("angleCount", "16"));
        double[] tauQuantiles = parseDoubles(options.getOrDefault("tauQuantiles",
                "0.35,0.40,0.45,0.50,0.55,0.60,0.65"));
        double minBalance = Double.parseDouble(options.getOrDefault("minBalance", "0.25"));
        int trainingQuerySampleSize = Integer.parseInt(options.getOrDefault("trainingQuerySampleSize", "512"));
        int medoidCandidateCount = Integer.parseInt(options.getOrDefault("medoidCandidateCount", "8"));
        int medoidIterations = Integer.parseInt(options.getOrDefault("medoidIterations", "2"));
        return new PowerDistanceLearningConfig(learningRhoGrid, angleCount, tauQuantiles,
                minBalance, trainingQuerySampleSize, medoidCandidateCount,
                medoidIterations,
                index.powerdistance.PowerDistanceTransform.DEFAULT_EPSILON_DISTANCE,
                index.powerdistance.PowerDistanceTransform.DEFAULT_COMPARISON_EPSILON);
    }

    private static int[] parseInts(String raw)
    {
        String[] parts = raw.split(",");
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++)
        {
            values[i] = Integer.parseInt(parts[i].trim());
        }
        return values;
    }

    private static String safeNumber(double value)
    {
        return String.format(Locale.US, "%.6f", value)
                .replace("-", "m")
                .replace(".", "p")
                .replaceAll("0+$", "")
                .replaceAll("p$", "");
    }

    private static class Scenario
    {
        private final List<double[]> data;
        private final List<double[]> queries;

        private Scenario(List<double[]> data, List<double[]> queries)
        {
            this.data = data;
            this.queries = queries;
        }
    }

    private static class IndexSpec
    {
        private final String indexName;
        private final double rho;
        private final double w1;
        private final double w2;
        private final int vpPivots;
        private final PowerDistanceLearningConfig learningConfig;
        private transient List<? extends IndexObject> trainingQueries;
        private final double queryRadius;
        private transient PowerDistanceLearnedPartitionMethod learnedPartitionMethod;

        private IndexSpec(String indexName, double rho, double w1, double w2, int vpPivots,
                          PowerDistanceLearningConfig learningConfig,
                          List<? extends IndexObject> trainingQueries,
                          double queryRadius)
        {
            this.indexName = indexName;
            this.rho = rho;
            this.w1 = w1;
            this.w2 = w2;
            this.vpPivots = vpPivots;
            this.learningConfig = learningConfig;
            this.trainingQueries = trainingQueries;
            this.queryRadius = queryRadius;
        }

        static IndexSpec gh()
        {
            return new IndexSpec("GH", Double.NaN, Double.NaN, Double.NaN,
                    0, null, null, Double.NaN);
        }

        static IndexSpec vp(int pivots)
        {
            return new IndexSpec("VP" + pivots, Double.NaN, Double.NaN, Double.NaN,
                    pivots, null, null, Double.NaN);
        }

        static IndexSpec rgh()
        {
            return new IndexSpec("RGH", Double.NaN, Double.NaN, Double.NaN,
                    0, null, null, Double.NaN);
        }

        static IndexSpec fixedPower(double rho, double w1, double w2)
        {
            return new IndexSpec("POWER_FIXED_rho_" + rho, rho, w1, w2,
                    0, null, null, Double.NaN);
        }

        static IndexSpec learnedPower(List<? extends IndexObject> trainingQueries,
                                      double queryRadius,
                                      PowerDistanceLearningConfig learningConfig)
        {
            return new IndexSpec("POWER_LEARNED_MEDOID", Double.NaN,
                    Double.NaN, Double.NaN, 0, learningConfig,
                    trainingQueries, queryRadius);
        }

        void build(Table table,
                   PivotSelectionMethod pivotSelectionMethod,
                   HierarchicalPivotSelectionMode mode,
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
                        VPPartitionMethods.BALANCED, 2, maxLeafSize, mode, null);
            }
            else if (indexName.startsWith("POWER"))
            {
                if ("POWER_LEARNED_MEDOID".equals(indexName))
                {
                    learnedPartitionMethod = new PowerDistanceLearnedPartitionMethod(
                            learningConfig, trainingQueries, queryRadius);
                    table.buildPowerDistanceIndex(
                            new PowerDistanceMedoidPairPivotSelectionMethod(
                                    learningConfig, trainingQueries, queryRadius),
                            learnedPartitionMethod, maxLeafSize,
                            HierarchicalPivotSelectionMode.LOCAL, null);
                }
                else
                {
                    table.buildPowerDistanceIndex(pivotSelectionMethod,
                            new PowerDistanceLinearPartitionMethod(rho, w1, w2),
                            maxLeafSize, mode, null);
                }
            }
            else
            {
                throw new IllegalStateException("unknown index spec: " + indexName);
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
            if (model == null)
            {
                return "";
            }
            return model.getW1() + ";" + model.getW2();
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
        private final String scenario;
        private final int dim;
        private final int dataSize;
        private final int querySize;
        private final int clusterCount;
        private final double clusterStd;
        private final double radius;
        private final String indexName;
        private final double rho;
        private final double w1;
        private final double w2;
        private final int maxLeafSize;
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
        private final int correctnessChecked;
        private final boolean correct;

        private ResultRow(String scenario,
                          int dim,
                          int dataSize,
                          int querySize,
                          int clusterCount,
                          double clusterStd,
                          double radius,
                          String indexName,
                          double rho,
                          double w1,
                          double w2,
                          int maxLeafSize,
                          long buildMs,
                          Stats stats,
                          double learnedRho,
                          String learnedDirectionSummary,
                          int correctnessChecked,
                          boolean correct)
        {
            this.scenario = scenario;
            this.dim = dim;
            this.dataSize = dataSize;
            this.querySize = querySize;
            this.clusterCount = clusterCount;
            this.clusterStd = clusterStd;
            this.radius = radius;
            this.indexName = indexName;
            this.rho = rho;
            this.w1 = w1;
            this.w2 = w2;
            this.maxLeafSize = maxLeafSize;
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
            this.correctnessChecked = correctnessChecked;
            this.correct = correct;
        }

        String scenarioRadiusKey()
        {
            return scenario + "|r=" + radius;
        }

        String toCsv()
        {
            return scenario + "," + dim + "," + dataSize + "," + querySize + ","
                    + clusterCount + "," + clusterStd + "," + radius + ","
                    + indexName + "," + rho + "," + w1 + "," + w2 + ","
                    + maxLeafSize + "," + buildMs + "," + avgDistanceCount + ","
                    + avgQueryMs + "," + avgExclusionRate + "," + avgPruneRate + ","
                    + avgInternalNodes + "," + avgLeafNodes + "," + avgResultCount
                    + "," + learnedRho + "," + learnedDirectionSummary
                    + "," + correctnessChecked + "," + correct;
        }

        String shortLine()
        {
            return scenario + " r=" + radius + " " + indexName
                    + " avgDist=" + String.format(Locale.US, "%.2f", avgDistanceCount)
                    + " avgMs=" + String.format(Locale.US, "%.4f", avgQueryMs)
                    + " prune=" + String.format(Locale.US, "%.4f", avgPruneRate)
                    + " correct=" + correct;
        }

        double effectivePowerRho()
        {
            return Double.isNaN(rho) ? learnedRho : rho;
        }
    }
}
