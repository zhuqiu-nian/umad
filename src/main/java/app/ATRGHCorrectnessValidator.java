package app;

import algorithms.datapartition.ApollonianPartitionMethods;
import algorithms.datapartition.IATPartitionMethods;
import algorithms.datapartition.RGHPartitionMethods;
import algorithms.pivotselection.PivotSelectionMethod;
import algorithms.pivotselection.PivotSelectionMethods;
import db.TableManager;
import db.table.DoubleVectorTable;
import db.table.Table;
import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.search.Cursor;
import index.search.RangeQuery;
import index.type.HierarchicalPivotSelectionMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates AT, IAT, and RGH range-query correctness by comparing index results with
 * brute-force linear scan results through the project's own Table interfaces.
 *
 * Example:
 * java -cp "target/classes;libs/*" app.ATRGHCorrectnessValidator
 *
 * Example with experiment data:
 * java -cp "target/classes;libs/*" app.ATRGHCorrectnessValidator ^
 *   -f data/vector/clusteredvector-2d-100k-100c.txt -n 5000 -d 2 -qn 100 -radii 0.05
 */
public class ATRGHCorrectnessValidator {
    private static final String DEFAULT_DATA_FILE = "testat_correctness/test_vector_100.txt";
    private static final int DEFAULT_DATA_SIZE = 100;
    private static final int DEFAULT_DIMENSION = 2;
    private static final int DEFAULT_QUERY_SIZE = 10;
    private static final int DEFAULT_MAX_LEAF_SIZE = 10;
    private static final double[] DEFAULT_RADII = {0.05, 0.1, 0.2, 0.5, 1.0};
    private static int lastIndexDistanceCount = 0;

    private enum IndexKind {
        AT, IAT, RGH
    }

    private static class Config {
        String dataFile = DEFAULT_DATA_FILE;
        String queryFile = null;
        int dataSize = DEFAULT_DATA_SIZE;
        int dimension = DEFAULT_DIMENSION;
        int querySize = DEFAULT_QUERY_SIZE;
        int maxLeafSize = DEFAULT_MAX_LEAF_SIZE;
        double[] radii = DEFAULT_RADII;
        List<IndexKind> indexes = Arrays.asList(IndexKind.AT, IndexKind.IAT, IndexKind.RGH);
        IATPartitionMethods iatPartitionMethod = IATPartitionMethods.SPATIAL_BALANCED;

        String queryFile() {
            return queryFile == null ? dataFile : queryFile;
        }
    }

    private static class ValidationResult {
        int total;
        int passed;
        int failed;
        long distanceCount;
    }

    public static void main(String[] args) throws IOException {
        Config config = parseArgs(args);
        printConfig(config);

        int totalFailures = 0;
        for (IndexKind indexKind : config.indexes) {
            ValidationResult result = validateIndex(indexKind, config);
            totalFailures += result.failed;
        }

        if (totalFailures > 0) {
            System.exit(1);
        }
    }

    private static ValidationResult validateIndex(IndexKind indexKind, Config config) throws IOException {
        String managerName = "correctness_" + indexKind.name().toLowerCase() + "_" + System.currentTimeMillis();
        TableManager tableManager = TableManager.getTableManager(managerName);

        try {
            System.out.println();
            System.out.println("===========================================");
            System.out.println(indexKind + " correctness validation");
            System.out.println("===========================================");

            Table dataTable = buildIndex(indexKind, tableManager, config);
            List<? extends IndexObject> queryObjects = loadQueryObjects(config);
            System.out.println("Query objects loaded: " + queryObjects.size());

            ValidationResult summary = new ValidationResult();
            for (int queryIndex = 0; queryIndex < queryObjects.size(); queryIndex++) {
                IndexObject queryObject = queryObjects.get(queryIndex);
                for (double radius : config.radii) {
                    summary.total++;
                    boolean passed = validateOneQuery(indexKind, dataTable, queryObject, queryIndex, radius);
                    summary.distanceCount += lastIndexDistanceCount;
                    if (passed) {
                        summary.passed++;
                    } else {
                        summary.failed++;
                    }
                }
            }

            System.out.println("-------------------------------------------");
            System.out.printf("%s summary: total=%d, passed=%d, failed=%d, passRate=%.2f%%, avgDistanceCount=%.2f%n",
                    indexKind, summary.total, summary.passed, summary.failed,
                    summary.total == 0 ? 0.0 : summary.passed * 100.0 / summary.total,
                    summary.total == 0 ? 0.0 : summary.distanceCount / (double) summary.total);
            return summary;
        } finally {
            tableManager.close();
        }
    }

    private static Table buildIndex(IndexKind indexKind, TableManager tableManager, Config config) throws IOException {
        PivotSelectionMethod pivotSelectionMethod = PivotSelectionMethods.FFT;
        String tableName;

        if (indexKind == IndexKind.AT) {
            tableName = IndexBuilder.buildApollonianIndexOnVector(
                    tableManager,
                    config.dataFile,
                    config.dimension,
                    config.dataSize,
                    pivotSelectionMethod,
                    ApollonianPartitionMethods.APOLLONIAN,
                    config.maxLeafSize,
                    HierarchicalPivotSelectionMode.LOCAL
            );
        } else if (indexKind == IndexKind.IAT) {
            tableName = IndexBuilder.buildIATIndexOnVector(
                    tableManager,
                    config.dataFile,
                    config.dimension,
                    config.dataSize,
                    pivotSelectionMethod,
                    config.iatPartitionMethod,
                    config.maxLeafSize,
                    HierarchicalPivotSelectionMode.LOCAL
            );
        } else {
            tableName = IndexBuilder.buildRGHIndexOnVector(
                    tableManager,
                    config.dataFile,
                    config.dimension,
                    config.dataSize,
                    pivotSelectionMethod,
                    RGHPartitionMethods.RGH,
                    config.maxLeafSize,
                    HierarchicalPivotSelectionMode.LOCAL
            );
        }

        Table dataTable = tableManager.getTable(tableName);
        if (dataTable == null) {
            throw new IllegalStateException("Cannot load table after building index: " + tableName);
        }

        System.out.println("Built table: " + tableName);
        System.out.println("Indexed objects: " + dataTable.getData().size());
        return dataTable;
    }

    private static List<? extends IndexObject> loadQueryObjects(Config config) throws IOException {
        int querySize = Math.min(config.querySize, config.dataSize);
        DoubleVectorTable queryTable = new DoubleVectorTable(
                config.queryFile(),
                "correctness_query",
                querySize,
                config.dimension
        );
        return queryTable.getData();
    }

    private static boolean validateOneQuery(IndexKind indexKind, Table dataTable, IndexObject queryObject, int queryIndex, double radius) {
        RangeQuery query = new RangeQuery(queryObject, radius);

        List<Integer> indexRowIds = collectIndexRowIds(dataTable, query);
        List<Integer> bruteForceRowIds = collectBruteForceRowIds(dataTable, query);

        boolean passed = indexRowIds.equals(bruteForceRowIds);
        if (passed) {
            System.out.printf("[PASS] %s query=%d radius=%.6f results=%d%n",
                    indexKind, queryIndex, radius, indexRowIds.size());
        } else {
            List<Integer> missing = difference(bruteForceRowIds, indexRowIds);
            List<Integer> extra = difference(indexRowIds, bruteForceRowIds);
            System.out.printf("[FAIL] %s query=%d radius=%.6f index=%d bruteForce=%d missing=%d extra=%d%n",
                    indexKind, queryIndex, radius, indexRowIds.size(), bruteForceRowIds.size(),
                    missing.size(), extra.size());
            System.out.println("       missing rowIDs: " + preview(missing));
            System.out.println("       extra rowIDs:   " + preview(extra));
        }
        return passed;
    }

    private static List<Integer> collectIndexRowIds(Table dataTable, RangeQuery query) {
        Cursor cursor = dataTable.getIndex().search(query);
        List<Integer> rowIds = new ArrayList<>();
        while (cursor.hasNext()) {
            rowIds.add(cursor.next().getObject().getRowID());
        }
        lastIndexDistanceCount = cursor.getDisCounter();
        Collections.sort(rowIds);
        return rowIds;
    }

    private static List<Integer> collectBruteForceRowIds(Table dataTable, RangeQuery query) {
        List<DoubleIndexObjectPair> bruteForceResults = dataTable.searchByLinear(query);
        List<Integer> rowIds = new ArrayList<>();
        for (DoubleIndexObjectPair pair : bruteForceResults) {
            rowIds.add(pair.getObject().getRowID());
        }
        Collections.sort(rowIds);
        return rowIds;
    }

    private static List<Integer> difference(List<Integer> left, List<Integer> right) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (Integer value : right) {
            counts.put(value, counts.getOrDefault(value, 0) + 1);
        }

        List<Integer> result = new ArrayList<>();
        for (Integer value : left) {
            int count = counts.getOrDefault(value, 0);
            if (count == 0) {
                result.add(value);
            } else {
                counts.put(value, count - 1);
            }
        }
        return result;
    }

    private static String preview(List<Integer> values) {
        int limit = Math.min(values.size(), 20);
        String text = values.subList(0, limit).toString();
        if (values.size() > limit) {
            return text + " ...";
        }
        return text;
    }

    private static Config parseArgs(String[] args) {
        Config config = new Config();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            switch (arg) {
                case "-f":
                    config.dataFile = args[++i];
                    break;
                case "-qf":
                    config.queryFile = args[++i];
                    break;
                case "-n":
                    config.dataSize = Integer.parseInt(args[++i]);
                    break;
                case "-d":
                    config.dimension = Integer.parseInt(args[++i]);
                    break;
                case "-qn":
                    config.querySize = Integer.parseInt(args[++i]);
                    break;
                case "-maxleaf":
                    config.maxLeafSize = Integer.parseInt(args[++i]);
                    break;
                case "-r":
                    config.radii = new double[]{Double.parseDouble(args[++i])};
                    break;
                case "-radii":
                    config.radii = parseRadii(args[++i]);
                    break;
                case "-i":
                    config.indexes = parseIndexes(args[++i]);
                    break;
                case "-iatpm":
                    config.iatPartitionMethod = IATPartitionMethods.valueOf(args[++i].toUpperCase());
                    break;
                case "-h":
                case "--help":
                    printHelpAndExit();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        return config;
    }

    private static double[] parseRadii(String value) {
        String[] parts = value.split(",");
        double[] radii = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            radii[i] = Double.parseDouble(parts[i].trim());
        }
        return radii;
    }

    private static List<IndexKind> parseIndexes(String value) {
        String normalized = value.trim().toLowerCase();
        if ("both".equals(normalized)) {
            return Arrays.asList(IndexKind.AT, IndexKind.IAT, IndexKind.RGH);
        }
        if ("at".equals(normalized)) {
            return Collections.singletonList(IndexKind.AT);
        }
        if ("iat".equals(normalized)) {
            return Collections.singletonList(IndexKind.IAT);
        }
        if ("rgh".equals(normalized)) {
            return Collections.singletonList(IndexKind.RGH);
        }
        throw new IllegalArgumentException("Index must be one of: at, iat, rgh, both");
    }

    private static void printConfig(Config config) {
        System.out.println("AT/IAT/RGH correctness validator");
        System.out.println("dataFile=" + config.dataFile);
        System.out.println("queryFile=" + config.queryFile());
        System.out.println("dataSize=" + config.dataSize);
        System.out.println("dimension=" + config.dimension);
        System.out.println("querySize=" + config.querySize);
        System.out.println("maxLeafSize=" + config.maxLeafSize);
        System.out.println("radii=" + Arrays.toString(config.radii));
        System.out.println("indexes=" + config.indexes);
        System.out.println("iatPartitionMethod=" + config.iatPartitionMethod);
    }

    private static void printHelpAndExit() {
        System.out.println("Usage: java -cp \"target/classes;libs/*\" app.ATRGHCorrectnessValidator [options]");
        System.out.println("Options:");
        System.out.println("  -f <file>           data file, default " + DEFAULT_DATA_FILE);
        System.out.println("  -qf <file>          query file, default same as data file");
        System.out.println("  -n <number>         indexed data size, default " + DEFAULT_DATA_SIZE);
        System.out.println("  -d <dimension>      vector dimension, default " + DEFAULT_DIMENSION);
        System.out.println("  -qn <number>        query object count, default " + DEFAULT_QUERY_SIZE);
        System.out.println("  -r <radius>         single query radius");
        System.out.println("  -radii <a,b,c>      comma-separated query radii");
        System.out.println("  -i <at|iat|rgh|both> index to validate, default both");
        System.out.println("  -iatpm <method>      IAT partition method, default SPATIAL_BALANCED");
        System.out.println("  -maxLeaf <number>   max leaf size, default " + DEFAULT_MAX_LEAF_SIZE);
        System.exit(0);
    }
}
