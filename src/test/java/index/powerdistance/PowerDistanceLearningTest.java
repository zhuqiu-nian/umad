package index.powerdistance;

import algorithms.datapartition.PowerDistanceLearnedPartitionMethod;
import algorithms.pivotselection.PowerDistanceMedoidPairPivotSelectionMethod;
import db.TableManager;
import db.table.DoubleVectorTable;
import db.type.DoubleVector;
import db.type.IndexObject;
import index.search.Cursor;
import index.search.RangeQuery;
import index.structure.PowerDistanceIndex;
import index.type.HierarchicalPivotSelectionMode;
import metric.LMetric;
import metric.Metric;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PowerDistanceLearningTest
{
    @Test
    public void optimizerFindsQueryAwareBoundaryWithPruningPotential()
    {
        Metric metric = LMetric.EuclideanDistanceMetric;
        List<IndexObject> data = twoClusterData();
        IndexObject[] pivots = new IndexObject[]{data.get(0), data.get(20)};
        List<IndexObject> queries = new ArrayList<>();
        queries.add(new DoubleVector(null, 10_000, new double[]{0.0, 0.0}));
        queries.add(new DoubleVector(null, 10_001, new double[]{10.0, 0.0}));

        PowerDistanceLearningConfig config = smallConfig();
        PowerDistanceBoundaryOptimizer.Result result =
                new PowerDistanceBoundaryOptimizer().optimize(metric, pivots,
                        data, 0, data.size(), queries, 0.4, config);

        assertTrue(result.isQueryAware());
        assertFalse(result.isFallback());
        assertTrue(result.getScore() > 0.0);
        assertTrue(Math.min(result.getLeftSize(), result.getRightSize())
                >= data.size() * config.getMinBalance());
    }

    @Test
    public void medoidPivotSelectionReturnsDataPointsNearBothClusters()
    {
        Metric metric = LMetric.EuclideanDistanceMetric;
        List<IndexObject> data = twoClusterData();
        PowerDistanceMedoidPairPivotSelectionMethod selector =
                new PowerDistanceMedoidPairPivotSelectionMethod(smallConfig(),
                        data.subList(0, 4), 0.4);

        int[] pivots = selector.selectPivots(metric, data, 2);

        assertEquals(2, pivots.length);
        assertTrue(pivots[0] != pivots[1]);
        double x0 = ((DoubleVector) data.get(pivots[0])).getData()[0];
        double x1 = ((DoubleVector) data.get(pivots[1])).getData()[0];
        assertTrue((x0 < 5.0 && x1 > 5.0) || (x1 < 5.0 && x0 > 5.0));
    }

    @Test
    public void learnedIndexMatchesLinearScan()
            throws IOException
    {
        Metric metric = LMetric.EuclideanDistanceMetric;
        File dataFile = writeVectorFile(randomRows());
        TableManager tableManager = TableManager.getTableManager(
                "target/test-index/pd_learning_manager_" + System.nanoTime());
        DoubleVectorTable table = new DoubleVectorTable(dataFile.getPath(),
                "pd_learning_table_" + System.nanoTime(), 80, 2);
        tableManager.putTable(table);
        List<? extends IndexObject> tableData = table.getData();
        List<IndexObject> data = new ArrayList<>();
        data.addAll(tableData);
        List<IndexObject> original = new ArrayList<>(data);
        List<IndexObject> queries = randomQueries();
        PowerDistanceLearningConfig config = smallConfig();
        PowerDistanceIndex index = new PowerDistanceIndex(
                "target/test-index/pd_learned_" + System.nanoTime(),
                data, metric, 8,
                HierarchicalPivotSelectionMode.LOCAL,
                new PowerDistanceMedoidPairPivotSelectionMethod(config, queries, 0.25),
                new PowerDistanceLearnedPartitionMethod(config, queries, 0.25),
                null);
        index.buildTree();

        for (IndexObject query : queries)
        {
            assertEquals(linear(metric, original, query, 0.25),
                    indexed(index, query, 0.25));
        }
        index.destroy();
    }

    private PowerDistanceLearningConfig smallConfig()
    {
        return new PowerDistanceLearningConfig(
                new double[]{-2.0, 1.0, 2.0},
                8,
                new double[]{0.4, 0.5, 0.6},
                0.2,
                16,
                4,
                1,
                PowerDistanceTransform.DEFAULT_EPSILON_DISTANCE,
                PowerDistanceTransform.DEFAULT_COMPARISON_EPSILON);
    }

    private List<IndexObject> twoClusterData()
    {
        List<IndexObject> data = new ArrayList<>();
        for (int i = 0; i < 20; i++)
        {
            data.add(new DoubleVector(null, i, new double[]{i * 0.01, 0.0}));
        }
        for (int i = 0; i < 20; i++)
        {
            data.add(new DoubleVector(null, 20 + i, new double[]{10.0 + i * 0.01, 0.0}));
        }
        return data;
    }

    private List<double[]> randomRows()
    {
        Random random = new Random(19L);
        List<double[]> rows = new ArrayList<>();
        for (int i = 0; i < 80; i++)
        {
            double cx = i < 40 ? -1.0 : 1.0;
            double cy = i < 40 ? 0.0 : 0.5;
            rows.add(new double[]{cx + random.nextGaussian() * 0.2,
                    cy + random.nextGaussian() * 0.2});
        }
        return rows;
    }

    private File writeVectorFile(List<double[]> rows) throws IOException
    {
        new File("target/test-index").mkdirs();
        File file = File.createTempFile("pd-learning", ".txt",
                new File("target/test-index"));
        try (FileWriter writer = new FileWriter(file))
        {
            writer.write("2 " + rows.size() + System.lineSeparator());
            for (double[] row : rows)
            {
                writer.write(row[0] + " " + row[1] + System.lineSeparator());
            }
        }
        return file;
    }

    private List<IndexObject> randomQueries()
    {
        List<IndexObject> queries = new ArrayList<>();
        queries.add(new DoubleVector(null, 1000, new double[]{-1.0, 0.0}));
        queries.add(new DoubleVector(null, 1001, new double[]{1.0, 0.5}));
        queries.add(new DoubleVector(null, 1002, new double[]{0.0, 0.0}));
        queries.add(new DoubleVector(null, 1003, new double[]{1.2, 0.2}));
        return queries;
    }

    private Set<Integer> indexed(PowerDistanceIndex index, IndexObject query,
                                 double radius)
    {
        Cursor cursor = index.search(new RangeQuery(query, radius));
        Set<Integer> result = new HashSet<>();
        while (cursor.hasNext())
        {
            result.add(cursor.next().getObject().getRowID());
        }
        return result;
    }

    private Set<Integer> linear(Metric metric, List<IndexObject> data,
                                IndexObject query, double radius)
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
}
