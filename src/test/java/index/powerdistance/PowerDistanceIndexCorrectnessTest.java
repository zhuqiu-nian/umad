package index.powerdistance;

import algorithms.datapartition.PowerDistanceLinearPartitionMethod;
import db.TableManager;
import db.table.DoubleVectorTable;
import db.type.DoubleIndexObjectPair;
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

public class PowerDistanceIndexCorrectnessTest
{
    @Test
    public void rangeSearchMatchesLinearScanForPositiveAndNegativeRho()
            throws IOException
    {
        double[] rhos = new double[]{2.0, -2.0};
        int[] fanouts = new int[]{2, 4};
        double[] radii = new double[]{0.0, 0.15, 0.35, 0.75};

        for (double rho : rhos)
        {
            for (int fanout : fanouts)
            {
                String managerPrefix = "target/test-index/pd_manager_" + rho + "_" + fanout + "_" + System.nanoTime();
                TableManager tableManager = TableManager.getTableManager(managerPrefix);
                File dataFile = writeVectorFile(sampleData());
                DoubleVectorTable table = new DoubleVectorTable(dataFile.getPath(),
                        "pd_table_" + rho + "_" + fanout + "_" + System.nanoTime(), 80, 2);
                tableManager.putTable(table);
                List<? extends IndexObject> data = table.getData();
                List<IndexObject> original = new ArrayList<>();
                original.addAll(data);
                IndexObject[] pivots = new IndexObject[]{data.get(0), data.get(1)};
                Metric metric = LMetric.EuclideanDistanceMetric;
                String prefix = "target/test-index/pd_" + rho + "_" + fanout + "_" + System.nanoTime();
                new File("target/test-index").mkdirs();

                PowerDistanceIndex index = new PowerDistanceIndex(prefix, data, metric, 8,
                        fanout, HierarchicalPivotSelectionMode.GLOBAL, null,
                        new PowerDistanceLinearPartitionMethod(rho, 1.0, -1.0), pivots);
                index.buildTree();

                List<IndexObject> queries = queryData();
                for (IndexObject query : queries)
                {
                    for (double radius : radii)
                    {
                        assertEquals(linear(metric, original, query, radius),
                                indexed(index, query, radius));
                    }
                }

                index.destroy();
            }
        }
    }

    @Test
    public void degenerateFallbackSplitStillMatchesLinearScan()
            throws IOException
    {
        File dataFile = writeVectorFile(degenerateData());
        TableManager tableManager = TableManager.getTableManager(
                "target/test-index/pd_degenerate_manager_" + System.nanoTime());
        DoubleVectorTable table = new DoubleVectorTable(dataFile.getPath(),
                "pd_degenerate_table_" + System.nanoTime(), 60, 2);
        tableManager.putTable(table);
        List<? extends IndexObject> data = table.getData();
        List<IndexObject> original = new ArrayList<>();
        original.addAll(data);
        IndexObject[] pivots = new IndexObject[]{data.get(0), data.get(1)};
        Metric metric = LMetric.EuclideanDistanceMetric;

        PowerDistanceIndex index = new PowerDistanceIndex(
                "target/test-index/pd_degenerate_" + System.nanoTime(),
                data, metric, 4,
                HierarchicalPivotSelectionMode.GLOBAL, null,
                new PowerDistanceLinearPartitionMethod(2.0, 1.0, -1.0),
                pivots);
        index.buildTree();

        for (IndexObject query : queryData())
        {
            for (double radius : new double[]{0.0, 0.1, 1.0})
            {
                assertEquals(linear(metric, original, query, radius),
                        indexed(index, query, radius));
            }
        }

        index.destroy();
    }

    private List<double[]> sampleData()
    {
        Random random = new Random(7L);
        List<double[]> data = new ArrayList<>();
        data.add(new double[]{-1.0, 0.0});
        data.add(new double[]{1.0, 0.0});
        for (int i = 2; i < 80; i++)
        {
            double x = random.nextDouble() * 4.0 - 2.0;
            double y = random.nextDouble() * 3.0 - 1.5;
            data.add(new double[]{x, y});
        }
        return data;
    }

    private File writeVectorFile(List<double[]> rows) throws IOException
    {
        new File("target/test-index").mkdirs();
        File file = File.createTempFile("pd-correctness", ".txt", new File("target/test-index"));
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

    private List<double[]> degenerateData()
    {
        List<double[]> data = new ArrayList<>();
        data.add(new double[]{-1.0, 0.0});
        data.add(new double[]{1.0, 0.0});
        for (int i = 2; i < 60; i++)
        {
            data.add(new double[]{0.0, 0.0});
        }
        return data;
    }

    private List<IndexObject> queryData()
    {
        List<IndexObject> queries = new ArrayList<>();
        queries.add(new DoubleVector(null, 1000, new double[]{-1.0, 0.0}));
        queries.add(new DoubleVector(null, 1001, new double[]{0.0, 0.0}));
        queries.add(new DoubleVector(null, 1002, new double[]{1.2, 0.1}));
        queries.add(new DoubleVector(null, 1003, new double[]{-0.3, 0.8}));
        return queries;
    }

    private Set<Integer> indexed(PowerDistanceIndex index, IndexObject query, double radius)
    {
        Cursor cursor = index.search(new RangeQuery(query, radius));
        Set<Integer> result = new HashSet<>();
        while (cursor.hasNext())
        {
            result.add(cursor.next().getObject().getRowID());
        }
        return result;
    }

    private Set<Integer> linear(Metric metric, List<? extends IndexObject> data, IndexObject query, double radius)
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
