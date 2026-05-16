package app;

import algorithms.datapartition.LogDistanceLinearPartitionMethod;
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
import metric.CountedMetric;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimal vector-data batch runner for LogDistanceIndex.
 */
public class LogDistanceBatchExperiment
{
    public static void main(String[] args) throws Exception
    {
        Map<String, String> options = parseArgs(args);
        if (!options.containsKey("dataset") || !options.containsKey("querySet")
                || !options.containsKey("dim") || !options.containsKey("dataSize")
                || !options.containsKey("querySize") || !options.containsKey("radii"))
        {
            printUsage();
            return;
        }

        String dataset = options.get("dataset");
        String querySet = options.get("querySet");
        int dim = Integer.parseInt(options.get("dim"));
        int dataSize = Integer.parseInt(options.get("dataSize"));
        int querySize = Integer.parseInt(options.get("querySize"));
        int maxLeafSize = Integer.parseInt(options.getOrDefault("maxLeafSize", "20"));
        double w1 = Double.parseDouble(options.getOrDefault("w1", "1.0"));
        double w2 = Double.parseDouble(options.getOrDefault("w2", "-1.0"));
        double[] radii = parseDoubles(options.get("radii"));
        HierarchicalPivotSelectionMode mode = HierarchicalPivotSelectionMode.valueOf(options.getOrDefault("pivotMode", "LOCAL"));
        PivotSelectionMethod pivotSelectionMethod = PivotSelectionMethods.valueOf(options.getOrDefault("pivotSelection", "FFT"));
        String pivotFile = options.get("pivotFile");
        double epsilonDistance = Double.parseDouble(options.getOrDefault("epsilonDistance", "1e-12"));

        TableManager tableManager = TableManager.getTableManager("LD_BATCH_" + System.nanoTime());
        Table queryTable = new DoubleVectorTable(querySet, "ld_query", querySize, dim);
        List<? extends IndexObject> queries = queryTable.getData();

        System.out.println("radius,queryIndex,resultCount,linearResultCount,correct,buildMs,queryNs,indexDistanceCount,linearDistanceCount,internalNodes,leafNodes,pruneRate");

        String indexPrefix = "LD_vector_" + System.nanoTime();
        Table dataTable = new DoubleVectorTable(dataset, indexPrefix, dataSize, dim);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);

        IndexObject[] fixedPivots = null;
        HierarchicalPivotSelectionMode buildMode = mode;
        if (pivotFile != null)
        {
            Table pivotTable = new DoubleVectorTable(pivotFile, "ld_pivot", 2, dim);
            tableManager.putTable(pivotTable);
            fixedPivots = pivotTable.getData().toArray(new IndexObject[0]);
            buildMode = HierarchicalPivotSelectionMode.GLOBAL;
        }

        long buildStart = System.nanoTime();
        LogDistanceLinearPartitionMethod partitionMethod =
                new LogDistanceLinearPartitionMethod(w1, w2, epsilonDistance, 1e-12);
        IndexBuilder.bulkLoadLogDistanceIndex(dataTable, pivotSelectionMethod,
                partitionMethod, maxLeafSize, buildMode, fixedPivots);
        long buildMs = (System.nanoTime() - buildStart) / 1_000_000L;

        CountedMetric countedMetric = (CountedMetric) dataTable.getMetric();
        for (double radius : radii)
        {
            for (int queryIndex = 0; queryIndex < queries.size(); queryIndex++)
            {
                IndexObject query = queries.get(queryIndex);
                countedMetric.clear();
                Cursor cursor = dataTable.getIndex().search(new RangeQuery(query, radius));
                Set<Integer> indexResult = drain(cursor);
                int indexDistanceCount = countedMetric.getCounter();

                countedMetric.clear();
                Set<Integer> linearResult = linearSearch(dataTable, query, radius);
                int linearDistanceCount = countedMetric.getCounter();

                System.out.println(radius + "," + queryIndex + ","
                        + indexResult.size() + "," + linearResult.size() + ","
                        + indexResult.equals(linearResult) + ","
                        + buildMs + "," + cursor.nsOfSearchTime + ","
                        + indexDistanceCount + "," + linearDistanceCount + ","
                        + cursor.numberOfInternalNodeSearches + ","
                        + cursor.numberOfLeafNodeSearches + ","
                        + cursor.averagePruneRate);
            }
        }
    }

    private static Set<Integer> drain(Cursor cursor)
    {
        Set<Integer> result = new HashSet<>();
        while (cursor.hasNext())
        {
            result.add(cursor.next().getObject().getRowID());
        }
        return result;
    }

    private static Set<Integer> linearSearch(Table table, IndexObject query, double radius)
    {
        Set<Integer> result = new HashSet<>();
        for (DoubleIndexObjectPair pair : table.searchByLinear(new RangeQuery(query, radius)))
        {
            result.add(pair.getObject().getRowID());
        }
        return result;
    }

    private static Map<String, String> parseArgs(String[] args)
    {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.startsWith("--"))
            {
                String key = arg.substring(2);
                if (i + 1 >= args.length)
                {
                    throw new IllegalArgumentException("missing value for " + arg);
                }
                options.put(key, args[++i]);
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

    private static void printUsage()
    {
        System.out.println("Usage: java app.LogDistanceBatchExperiment "
                + "--dataset data.txt --querySet query.txt --dim 2 "
                + "--dataSize 1000 --querySize 100 --radii 0.05,0.1 "
                + "[--w1 1.0 --w2 -1.0 --pivotMode LOCAL --pivotSelection FFT "
                + "--pivotFile pivots.txt --maxLeafSize 20 --epsilonDistance 1e-12]");
    }
}
