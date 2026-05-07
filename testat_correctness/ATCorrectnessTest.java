package app;

import algorithms.datapartition.ApollonianPartitionMethods;
import algorithms.pivotselection.PivotSelectionMethods;
import db.TableManager;
import db.table.Table;
import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.search.Cursor;
import index.search.RangeQuery;
import index.type.HierarchicalPivotSelectionMode;
import metric.LMetric;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * AT (Apollonian Tree) Index Correctness Test
 *
 * This test verifies that the AT index returns the same results as brute-force search.
 * It compares the search results of both methods for multiple query points and radii.
 *
 * Run: java -cp "target/classes:target/lib/*" app.ATCorrectnessTest
 */
public class ATCorrectnessTest {

    // Test parameters
    private static final String TEST_DATA_FILE = "testat_correctness/test_vector_100.txt";
    private static final int DIMENSION = 2;
    private static final int MAX_DATA_SIZE = 100;
    private static final int MAX_LEAF_SIZE = 10;
    private static final String METRIC_NAME = "L2"; // Euclidean distance

    // Test query radii - will test multiple radii
    private static final double[] TEST_RADII = {0.05, 0.1, 0.2, 0.5, 1.0};

    // Number of query points to test
    private static final int NUM_QUERY_POINTS = 10;

    private TableManager tableManager;
    private Table dataTable;

    public static void main(String[] args) throws IOException {
        System.out.println("===========================================");
        System.out.println("AT (Apollonian Tree) Index Correctness Test");
        System.out.println("===========================================\n");

        ATCorrectnessTest test = new ATCorrectnessTest();
        test.run();
    }

    public ATCorrectnessTest() {
        this.tableManager = TableManager.getTableManager("at_test");
    }

    public void run() throws IOException {
        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;

        try {
            // Build AT index
            System.out.println("Building AT index...");
            buildATIndex();
            System.out.println("AT index built successfully.\n");

            // Load test query points (use first NUM_QUERY_POINTS from the data file)
            List<IndexObject> queryPoints = loadQueryPoints();
            System.out.println("Loaded " + queryPoints.size() + " query points.\n");

            // Run tests for each query point and radius
            for (IndexObject queryPoint : queryPoints) {
                for (double radius : TEST_RADII) {
                    totalTests++;
                    TestResult result = runTest(queryPoint, radius);

                    if (result.passed) {
                        passedTests++;
                        System.out.printf("  [PASS] Query: %s, Radius: %.2f -> AT: %d results (dist:%d), BruteForce: %d results%n",
                                queryPoint.toString().substring(0, Math.min(30, queryPoint.toString().length())),
                                radius, result.atResultCount, result.atDistanceCount, result.bruteForceResultCount);
                    } else {
                        failedTests++;
                        System.out.printf("  [FAIL] Query: %s, Radius: %.2f -> AT: %d results (dist:%d), BruteForce: %d results%n",
                                queryPoint.toString().substring(0, Math.min(30, queryPoint.toString().length())),
                                radius, result.atResultCount, result.atDistanceCount, result.bruteForceResultCount);
                        System.out.println("        Missing from AT: " + result.missingResults);
                        System.out.println("        Extra in AT: " + result.extraResults);
                    }
                }
                System.out.println();
            }

        } finally {
            tableManager.close();
        }

        // Print summary
        System.out.println("===========================================");
        System.out.println("Test Summary");
        System.out.println("===========================================");
        System.out.printf("Total Tests: %d%n", totalTests);
        System.out.printf("Passed:     %d%n", passedTests);
        System.out.printf("Failed:     %d%n", failedTests);
        System.out.printf("Pass Rate:  %.2f%%%n", (double) passedTests / totalTests * 100);

        if (failedTests > 0) {
            System.exit(1);
        }
    }

    /**
     * Build the AT index using the system interface
     */
    private void buildATIndex() throws IOException {
        String tableName = IndexBuilder.buildApollonianIndexOnVector(
                tableManager,
                TEST_DATA_FILE,
                DIMENSION,
                MAX_DATA_SIZE,
                (algorithms.pivotselection.PivotSelectionMethod) PivotSelectionMethods.FFT,
                ApollonianPartitionMethods.APOLLONIAN,
                MAX_LEAF_SIZE,
                HierarchicalPivotSelectionMode.LOCAL
        );
        this.dataTable = tableManager.getTable(tableName);
        System.out.println("Table name: " + tableName);
        System.out.println("Table size: " + dataTable.getData().size());
    }

    /**
     * Load query points from the test data file
     */
    private List<IndexObject> loadQueryPoints() throws IOException {
        List<IndexObject> queryPoints = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader(TEST_DATA_FILE));
        // Skip header line
        String line = reader.readLine();
        int count = 0;

        while ((line = reader.readLine()) != null && count < NUM_QUERY_POINTS) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length < DIMENSION) continue;

            double[] data = new double[DIMENSION];
            for (int i = 0; i < DIMENSION; i++) {
                data[i] = Double.parseDouble(parts[i]);
            }
            queryPoints.add(new db.type.DoubleVector(data));
            count++;
        }
        reader.close();

        return queryPoints;
    }

    /**
     * Run a single test for a query point and radius
     */
    private TestResult runTest(IndexObject queryPoint, double radius) {
        TestResult result = new TestResult();

        // AT search
        RangeQuery query = new RangeQuery(queryPoint, radius);
        Cursor cursor = dataTable.getIndex().search(query);

        Set<Integer> atResultIds = new HashSet<>();
        int atDistanceCount = cursor.getDisCounter();
        result.atDistanceCount = atDistanceCount;
        while (cursor.hasNext()) {
            DoubleIndexObjectPair pair = cursor.next();
            atResultIds.add(pair.getObject().getRowID());
            result.atResultCount++;
        }

        // Brute-force search
        LMetric metric = new LMetric(2);
        List<? extends IndexObject> allData = dataTable.getData();

        for (IndexObject obj : allData) {
            double distance = metric.getDistance(queryPoint, obj);
            if (distance <= radius) {
                result.bruteForceResultCount++;
                if (!atResultIds.contains(obj.getRowID())) {
                    result.missingResults.add(obj.getRowID());
                }
            }
        }

        // Find extra results in AT that are not in brute-force
        // (This should not happen if the index is correct)
        // We don't check for this as it would require re-running the search

        // Check if results match
        result.passed = (result.atResultCount == result.bruteForceResultCount && result.missingResults.isEmpty());

        return result;
    }

    /**
     * Test result container
     */
    private static class TestResult {
        boolean passed;
        int atResultCount;
        int atDistanceCount;
        int bruteForceResultCount;
        Set<Integer> missingResults = new HashSet<>();
        List<Integer> extraResults = new ArrayList<>();
    }
}