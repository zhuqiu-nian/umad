package app;

import algorithms.datapartition.CPPartitionMethods;
import algorithms.datapartition.GHPartitionMethods;
import algorithms.datapartition.PartitionMethod;
import algorithms.datapartition.VPPartitionMethods;
import algorithms.datapartition.*;
import algorithms.datapartition.ApollonianPartitionMethods;
import algorithms.datapartition.RGHPartitionMethods;
import algorithms.datapartition.PCTPartitionMethods;
import algorithms.pivotselection.PivotSelectionMethod;
import algorithms.pivotselection.PivotSelectionMethods;
import db.TableManager;
import db.table.*;
import db.type.IndexObject;
import index.type.HierarchicalPivotSelectionMode;
import metric.CountedMetric;

import java.io.IOException;

/**
 * 索引构建的主要的操作类，通过该类可以执行各种索引的构建操作。
 */
public class IndexBuilder
{
    public static void bulkLoadCPIndex(Table dataTable, PivotSelectionMethod pivotSelectionMethod,
                                       int numPivots, CPPartitionMethods cpPartitionMethods, int cpPartitionNum,
                                       int cpMaxLeafSize, HierarchicalPivotSelectionMode mode, IndexObject[] pivotSet)
    {
        if (pivotSet != null)
        {
            //已经指定了支撑点
            dataTable.buildCPIndex(null, numPivots, cpPartitionMethods, cpPartitionNum, cpMaxLeafSize, mode, pivotSet);
        } else
        {
            //未指定支撑点
            dataTable.buildCPIndex(pivotSelectionMethod, numPivots, cpPartitionMethods, cpPartitionNum, cpMaxLeafSize, mode, null);
        }
    }

    //在ms上构建CP索引，返回索引在磁盘上存储的名称
    public static String buildCPIndexOnMS(TableManager tableManager, String dataFileName, int maxDataSize,
                                          PivotSelectionMethod pivotSelectionMethod,
                                          int numPivots, CPPartitionMethods cpPartitionMethods, int cpPartitionNum,
                                          int cpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                          String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("cp", "ms",
                maxDataSize, pivotSelectionMethod, numPivots,
                cpPartitionMethods.name(), cpPartitionNum, cpMaxLeafSize, mode);
        Table dataTable = new SpectraTable(dataFileName, indexPrefix, maxDataSize, null);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new SpectraTable(pivotsFileName, "pivot", maxDataSize, null);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadCPIndex(dataTable, pivotSelectionMethod, numPivots,
                cpPartitionMethods, cpPartitionNum, cpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    //在vector上构建CP索引，返回索引在磁盘上存储的名称
    public static String buildCPIndexOnVector(TableManager tableManager, String dataFileName, int dim, int maxDataSize,
                                              PivotSelectionMethod pivotSelectionMethod,
                                              int numPivots, CPPartitionMethods cpPartitionMethods, int cpPartitionNum,
                                              int cpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                              String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("cp", "vector",
                maxDataSize, pivotSelectionMethod, numPivots,
                cpPartitionMethods.name(), cpPartitionNum, cpMaxLeafSize, mode);
        Table dataTable = new DoubleVectorTable(dataFileName, indexPrefix, maxDataSize, dim);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new DoubleVectorTable(pivotsFileName, "pivot", maxDataSize, dim);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadCPIndex(dataTable, pivotSelectionMethod, numPivots,
                cpPartitionMethods, cpPartitionNum, cpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }





    //在DNA上构建CP索引，返回索引在磁盘上存储的名称
    public static String buildCPIndexOnDNA(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                           PivotSelectionMethod pivotSelectionMethod,
                                           int numPivots, CPPartitionMethods cpPartitionMethods, int cpPartitionNum,
                                           int cpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                           String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("cp", "dna",
                maxDataSize, pivotSelectionMethod, numPivots,
                cpPartitionMethods.name(), cpPartitionNum, cpMaxLeafSize, mode);
        Table dataTable = new DNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new DNATable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadCPIndex(dataTable, pivotSelectionMethod, numPivots,
                cpPartitionMethods, cpPartitionNum, cpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    //在RNA上构建CP索引，返回索引在磁盘上存储的名称
    public static String buildCPIndexOnRNA(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                           PivotSelectionMethod pivotSelectionMethod,
                                           int numPivots, CPPartitionMethods cpPartitionMethods, int cpPartitionNum,
                                           int cpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                           String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("cp", "rna",
                maxDataSize, pivotSelectionMethod, numPivots,
                cpPartitionMethods.name(), cpPartitionNum, cpMaxLeafSize, mode);
        Table dataTable = new RNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new RNATable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadCPIndex(dataTable, pivotSelectionMethod, numPivots,
                cpPartitionMethods, cpPartitionNum, cpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    //在protein上构建CP索引，返回索引在磁盘上存储的名称
    public static String buildCPIndexOnProtein(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                               PivotSelectionMethod pivotSelectionMethod,
                                               int numPivots, CPPartitionMethods cpPartitionMethods, int cpPartitionNum,
                                               int cpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                               String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("cp", "protein",
                maxDataSize, pivotSelectionMethod, numPivots,
                cpPartitionMethods.name(), cpPartitionNum, cpMaxLeafSize, mode);
        Table dataTable = new PeptideTable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new PeptideTable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadCPIndex(dataTable, pivotSelectionMethod, numPivots,
                cpPartitionMethods, cpPartitionNum, cpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    //在msms上构建CP索引，返回索引在磁盘上存储的名称
    public static String buildCPIndexOnMsMs(TableManager tableManager, String dataFileName, int maxDataSize,
                                            PivotSelectionMethod pivotSelectionMethod,
                                            int numPivots, CPPartitionMethods cpPartitionMethods, int cpPartitionNum,
                                            int cpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                            String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("cp", "msms",
                maxDataSize, pivotSelectionMethod, numPivots,
                cpPartitionMethods.name(), cpPartitionNum, cpMaxLeafSize, mode);
        Table dataTable = new SpectraWithPrecursorMassTable(dataFileName, indexPrefix, maxDataSize);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new SpectraWithPrecursorMassTable(pivotsFileName, "pivot", maxDataSize);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadCPIndex(dataTable, pivotSelectionMethod, numPivots,
                cpPartitionMethods, cpPartitionNum, cpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    //在image上构建CP索引，返回索引在磁盘上存储的名称
    public static String buildCPIndexOnImage(TableManager tableManager, String dataFileName, int maxDataSize,
                                             PivotSelectionMethod pivotSelectionMethod,
                                             int numPivots, CPPartitionMethods cpPartitionMethods, int cpPartitionNum,
                                             int cpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                             String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("cp", "image",
                maxDataSize, pivotSelectionMethod, numPivots,
                cpPartitionMethods.name(), cpPartitionNum, cpMaxLeafSize, mode);
        Table dataTable = new ImageTable(dataFileName, indexPrefix, maxDataSize);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new ImageTable(pivotsFileName, "pivot", maxDataSize);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadCPIndex(dataTable, pivotSelectionMethod, numPivots,
                cpPartitionMethods, cpPartitionNum, cpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }




    //在ms上构建VP索引，返回索引在磁盘上存储的名称
    public static String buildVPIndexOnMS(TableManager tableManager, String dataFileName, int maxDataSize,
                                          PivotSelectionMethod pivotSelectionMethod,
                                          int numPivots, VPPartitionMethods vpPartitionMethods, int vpPartitionNum,
                                          int vpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                          String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("vp", "ms",
                maxDataSize, pivotSelectionMethod, numPivots,
                vpPartitionMethods.name(), vpPartitionNum, vpMaxLeafSize, mode);
        Table dataTable = new SpectraTable(dataFileName, indexPrefix, maxDataSize, null);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new SpectraTable(pivotsFileName, "pivot", maxDataSize, null);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadVPIndex(dataTable, pivotSelectionMethod, numPivots,
                vpPartitionMethods, vpPartitionNum, vpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    /**
     * 根据传入的参数生成数据库保存到磁盘的前缀
     *
     * @param indexType            索引的类型，例如vp，gh等，不需要可传入null
     * @param dataType             数据的类型，例如ms，vector等，不需要可传入null
     * @param maxDataSize          数据集的大小，不需要可传入null
     * @param pivotSelectionMethod 支撑点选择方法，不需要可传入null
     * @param numPivots            支撑点数目，不需要可传入null
     * @param partitionMethod      划分方法，不需要可传入null
     * @param partitionNum         划分块数，不需要可传入null
     * @param maxLeafSize          最大叶子节点大小，不需要可传入null
     * @param mode                 构建索引采用的模式，不需要可传入null
     * @return 存储到磁盘的索引文件的前缀
     */
    private static String getIndexPrefix(String indexType, String dataType, Integer maxDataSize,
                                         PivotSelectionMethod pivotSelectionMethod,
                                         Integer numPivots, String partitionMethod, Integer partitionNum,
                                         Integer maxLeafSize, HierarchicalPivotSelectionMode mode)
    {
        StringBuilder prefix = new StringBuilder();
        if (indexType != null)
        {
            prefix.append(dataType + "-");
        }
        if (mode != null)
        {
            prefix.append(mode.name() + "-");
        }
        if (maxDataSize != null)
        {
            prefix.append(maxDataSize + "-");
        }
        if (indexType != null)
        {
            prefix.append(indexType + "-");
        }
        if (pivotSelectionMethod != null)
        {
            prefix.append(((PivotSelectionMethods) pivotSelectionMethod).name() + "-");
        }
        if (numPivots != null)
        {
            prefix.append("numPivots" + numPivots + "-");
        }
        if (partitionMethod != null)
        {
            prefix.append(partitionMethod + "-");
        }
        prefix.append(
                "numPartition" + partitionNum + "-"
                        + "maxLeafSize" + maxLeafSize
        );
        if (prefix.length() == 0)
        {
            prefix.append(System.currentTimeMillis());
        }
        return prefix.toString();
    }

    public static void bulkLoadVPIndex(Table dataTable, PivotSelectionMethod pivotSelectionMethod,
                                       int numPivots, VPPartitionMethods vpPartitionMethods, int vpPartitionNum,
                                       int vpMaxLeafSize, HierarchicalPivotSelectionMode mode, IndexObject[] pivotSet)
    {
        if (pivotSet != null)
        {
            //已经指定了支撑点
            dataTable.buildVPIndex(null, numPivots, vpPartitionMethods, vpPartitionNum, vpMaxLeafSize, mode, pivotSet);
        } else
        {
            //未指定支撑点
            dataTable.buildVPIndex(pivotSelectionMethod, numPivots, vpPartitionMethods, vpPartitionNum, vpMaxLeafSize, mode, null);
        }
    }

    public static String buildPCTIndexOnMS(TableManager tableManager, String dataFileName, int maxDataSize,
                                           PivotSelectionMethod pivotSelectionMethod, PCTPartitionMethods partitionMethod,
                                           int numPivots, int partitionNum,
                                           int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                           String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("PCT", "ms",
                maxDataSize, pivotSelectionMethod, numPivots,
                partitionMethod.name(), partitionNum, maxLeafSize, mode);
        Table dataTable = new SpectraTable(dataFileName, indexPrefix, maxDataSize, null);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new SpectraTable(pivotsFileName, "pivot", maxDataSize, null);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadPCTIndex(dataTable, pivotSelectionMethod, numPivots,partitionMethod,
                partitionNum, maxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildGNATIndexOnMS(TableManager tableManager, String dataFileName, int maxDataSize,
                                           PivotSelectionMethod pivotSelectionMethod,
                                           int numPivots, PartitionMethod partitionMethod,int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                           String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gnat", "ms",
                maxDataSize, pivotSelectionMethod, numPivots,
                null, numPivots, maxLeafSize, mode);
        Table dataTable = new SpectraTable(dataFileName, indexPrefix, maxDataSize, null);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new SpectraTable(pivotsFileName, "pivot", maxDataSize, null);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGNATIndex(dataTable, pivotSelectionMethod, numPivots, partitionMethod,maxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    /**
     * 构建PCT索引
     */
    public static void bulkLoadPCTIndex(Table dataTable, PivotSelectionMethod pivotSelectionMethod, int numPivots,
                                        PCTPartitionMethods partitionMethod, int numPartition, int maxLeafSize,
                                        HierarchicalPivotSelectionMode mode, IndexObject[] pivotSet)
    {
        if (pivotSet != null)
        {
            dataTable.buildPCTIndex(null, numPivots,partitionMethod, numPartition, maxLeafSize, mode, pivotSet);
        } else
        {
            dataTable.buildPCTIndex(pivotSelectionMethod, numPivots, partitionMethod, numPartition, maxLeafSize,
                     mode, null);
        }
    }

    public static String buildGHIndexOnMS(TableManager tableManager, String dataFileName, int maxDataSize,
                                          PivotSelectionMethod pivotSelectionMethod,
                                          GHPartitionMethods partitionMethod,
                                          int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                          String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gh", "ms",
                maxDataSize, pivotSelectionMethod, null,
                partitionMethod.name(), null, maxLeafSize, mode);
        Table dataTable = new SpectraTable(dataFileName, indexPrefix, maxDataSize, null);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new SpectraTable(pivotsFileName, "pivot", maxDataSize, null);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    /**
     * 构建GH索引
     *
     * @param dataTable            数据Table
     * @param pivotSelectionMethod 支撑点选择方法
     * @param partitionMethod      划分方法
     * @param maxLeafSize          叶子节点的最大大小
     * @param mode                 构建模式
     */
    public static void bulkLoadGHIndex(Table dataTable, PivotSelectionMethod pivotSelectionMethod,
                                       PartitionMethod partitionMethod, int maxLeafSize, HierarchicalPivotSelectionMode mode)
    {
        dataTable.buildGHIndex(pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
    }

    /**
     * 构建GNAT索引
     *
     * @param dataTable            数据Table
     * @param pivotSelectionMethod 支撑点选择方法
     * @param numPivot             支撑点数目
     * @param partitionMethod      划分方法
     * @param maxLeafSize          叶子节点的最大大小
     * @param mode                 构建模式
     */
    public static void bulkLoadGNATIndex(Table dataTable, PivotSelectionMethod pivotSelectionMethod,int numPivot,
                                       PartitionMethod partitionMethod, int maxLeafSize, HierarchicalPivotSelectionMode mode,IndexObject[] pivotSet)
    {
        if (pivotSet != null)
        {
            //已经指定了支撑点
            dataTable.buildGNATIndex(null, numPivot, partitionMethod, maxLeafSize, mode, pivotSet);

        } else
        {
            //未指定支撑点
            dataTable.buildGNATIndex(pivotSelectionMethod, numPivot, partitionMethod, maxLeafSize, mode, null);
        }
    }

    public static String buildGNATIndexOnVector(TableManager tableManager, String dataFileName, int dim, int maxDataSize,
                                              PivotSelectionMethod pivotSelectionMethod,
                                              int numPivots, GNATPartitionMethods gnatPartitionMethods,
                                              int gnatMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                              String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gnat", "vector",
                maxDataSize, pivotSelectionMethod, numPivots,
                gnatPartitionMethods.name(), numPivots, gnatMaxLeafSize, mode);
        Table dataTable = new DoubleVectorTable(dataFileName, indexPrefix, maxDataSize, dim);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new DoubleVectorTable(pivotsFileName, "pivot", maxDataSize, dim);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGNATIndex(dataTable, pivotSelectionMethod, numPivots,
                gnatPartitionMethods, gnatMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildVPIndexOnVector(TableManager tableManager, String dataFileName, int dim, int maxDataSize,
                                              PivotSelectionMethod pivotSelectionMethod,
                                              int numPivots, VPPartitionMethods vpPartitionMethods, int vpPartitionNum,
                                              int vpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                              String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("vp", "vector",
                maxDataSize, pivotSelectionMethod, numPivots,
                vpPartitionMethods.name(), vpPartitionNum, vpMaxLeafSize, mode);
        Table dataTable = new DoubleVectorTable(dataFileName, indexPrefix, maxDataSize, dim);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new DoubleVectorTable(pivotsFileName, "pivot", maxDataSize, dim);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadVPIndex(dataTable, pivotSelectionMethod, numPivots,
                vpPartitionMethods, vpPartitionNum, vpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildPCTIndexOnVector(TableManager tableManager, String dataFileName, int dim, int maxDataSize,
                                               PivotSelectionMethod pivotSelectionMethod, PCTPartitionMethods partitionMethod,
                                               int numPivots, int partitionNum,
                                               int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                               String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("PCT", "vector",
                maxDataSize, pivotSelectionMethod, numPivots,
                partitionMethod.name(), partitionNum, maxLeafSize, mode);
        Table dataTable = new DoubleVectorTable(dataFileName, indexPrefix, maxDataSize, dim);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new DoubleVectorTable(pivotsFileName, "pivot", maxDataSize, dim);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadPCTIndex(dataTable, pivotSelectionMethod, numPivots,partitionMethod,
                partitionNum, maxLeafSize, mode, pivotSet);
        return indexPrefix;
    }


    public static String buildGHIndexOnVector(TableManager tableManager, String dataFileName, int dim, int maxDataSize,
                                              PivotSelectionMethod pivotSelectionMethod,
                                              GHPartitionMethods partitionMethod,
                                              int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                              String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gh", "vector",
                maxDataSize, pivotSelectionMethod, null,
                partitionMethod.name(), null, maxLeafSize, mode);
        Table dataTable = new DoubleVectorTable(dataFileName, indexPrefix, maxDataSize, dim);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new DoubleVectorTable(pivotsFileName, "pivot", maxDataSize, dim);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildVPIndexOnDNA(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                           PivotSelectionMethod pivotSelectionMethod,
                                           int numPivots, VPPartitionMethods vpPartitionMethods, int vpPartitionNum,
                                           int vpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                           String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("vp", "dna",
                maxDataSize, pivotSelectionMethod, numPivots,
                vpPartitionMethods.name(), vpPartitionNum, vpMaxLeafSize, mode);
        Table dataTable = new DNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new DNATable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadVPIndex(dataTable, pivotSelectionMethod, numPivots,
                vpPartitionMethods, vpPartitionNum, vpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildPCTIndexOnDNA(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                            PivotSelectionMethod pivotSelectionMethod, PCTPartitionMethods partitionMethod,
                                            int numPivots, int partitionNum,
                                            int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                            String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("PCT", "dna",
                maxDataSize, pivotSelectionMethod, numPivots,
                partitionMethod.name(), partitionNum, maxLeafSize, mode);
        Table dataTable = new DNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new DNATable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadPCTIndex(dataTable, pivotSelectionMethod, numPivots,partitionMethod,
                partitionNum, maxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildGNATIndexOnDNA(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                            PivotSelectionMethod pivotSelectionMethod,
                                            int numPivots, PartitionMethod partitionMethod,int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                            String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gnat", "dna",
                maxDataSize, pivotSelectionMethod, numPivots,
                null, numPivots, maxLeafSize, mode);
        Table dataTable = new DNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new DNATable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGNATIndex(dataTable, pivotSelectionMethod, numPivots, partitionMethod,maxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildGHIndexOnDNA(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                           PivotSelectionMethod pivotSelectionMethod,
                                           GHPartitionMethods partitionMethod,
                                           int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                           String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gh", "dna",
                maxDataSize, pivotSelectionMethod, null,
                partitionMethod.name(), null, maxLeafSize, mode);
        Table dataTable = new DNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new DNATable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildVPIndexOnRNA(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                           PivotSelectionMethod pivotSelectionMethod,
                                           int numPivots, VPPartitionMethods vpPartitionMethods, int vpPartitionNum,
                                           int vpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                           String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("vp", "rna",
                maxDataSize, pivotSelectionMethod, numPivots,
                vpPartitionMethods.name(), vpPartitionNum, vpMaxLeafSize, mode);
        Table dataTable = new RNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new RNATable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadVPIndex(dataTable, pivotSelectionMethod, numPivots,
                vpPartitionMethods, vpPartitionNum, vpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildPCTIndexOnRNA(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                            PivotSelectionMethod pivotSelectionMethod, PCTPartitionMethods partitionMethod,
                                            int numPivots, int partitionNum,
                                            int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                            String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("PCT", "rna",
                maxDataSize, pivotSelectionMethod, numPivots,
                partitionMethod.name(), partitionNum, maxLeafSize, mode);
        Table dataTable = new RNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new RNATable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadPCTIndex(dataTable, pivotSelectionMethod, numPivots,partitionMethod,
                partitionNum, maxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildGNATIndexOnRNA(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                            PivotSelectionMethod pivotSelectionMethod,
                                            int numPivots, PartitionMethod partitionMethod,int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                            String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gnat", "rna",
                maxDataSize, pivotSelectionMethod, numPivots,
                null, numPivots, maxLeafSize, mode);
        Table dataTable = new RNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new RNATable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGNATIndex(dataTable, pivotSelectionMethod, numPivots, partitionMethod,maxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildGHIndexOnRNA(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                           PivotSelectionMethod pivotSelectionMethod,
                                           GHPartitionMethods partitionMethod,
                                           int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                           String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gh", "rna",
                maxDataSize, pivotSelectionMethod, null,
                partitionMethod.name(), null, maxLeafSize, mode);
        Table dataTable = new RNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new RNATable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildVPIndexOnProtein(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                               PivotSelectionMethod pivotSelectionMethod,
                                               int numPivots, VPPartitionMethods vpPartitionMethods, int vpPartitionNum,
                                               int vpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                               String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("vp", "protein",
                maxDataSize, pivotSelectionMethod, numPivots,
                vpPartitionMethods.name(), vpPartitionNum, vpMaxLeafSize, mode);
        Table dataTable = new PeptideTable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new PeptideTable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadVPIndex(dataTable, pivotSelectionMethod, numPivots,
                vpPartitionMethods, vpPartitionNum, vpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildPCTIndexOnProtein(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                                PivotSelectionMethod pivotSelectionMethod, PCTPartitionMethods partitionMethod,
                                                int numPivots, int partitionNum,
                                                int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                                String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("PCT", "protein",
                maxDataSize, pivotSelectionMethod, numPivots,
                partitionMethod.name(), partitionNum, maxLeafSize, mode);
        Table dataTable = new PeptideTable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new PeptideTable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadPCTIndex(dataTable, pivotSelectionMethod, numPivots,partitionMethod,
                partitionNum, maxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildGNATIndexOnProtein(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                                PivotSelectionMethod pivotSelectionMethod,
                                                int numPivots, PartitionMethod partitionMethod,int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                                String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gnat", "protein",
                maxDataSize, pivotSelectionMethod, numPivots,
                null, numPivots, maxLeafSize, mode);
        Table dataTable = new PeptideTable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new PeptideTable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGNATIndex(dataTable, pivotSelectionMethod, numPivots, partitionMethod, maxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildGHIndexOnProtein(TableManager tableManager, String dataFileName, int fragLength, int maxDataSize,
                                               PivotSelectionMethod pivotSelectionMethod,
                                               GHPartitionMethods partitionMethod,
                                               int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                               String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gh", "protein",
                maxDataSize, pivotSelectionMethod, null,
                partitionMethod.name(), null, maxLeafSize, mode);
        Table dataTable = new PeptideTable(dataFileName, indexPrefix, maxDataSize, fragLength);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new PeptideTable(pivotsFileName, "pivot", maxDataSize, fragLength);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildVPIndexOnMsMs(TableManager tableManager, String dataFileName, int maxDataSize,
                                            PivotSelectionMethod pivotSelectionMethod,
                                            int numPivots, VPPartitionMethods vpPartitionMethods, int vpPartitionNum,
                                            int vpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                            String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("vp", "msms",
                maxDataSize, pivotSelectionMethod, numPivots,
                vpPartitionMethods.name(), vpPartitionNum, vpMaxLeafSize, mode);
        Table dataTable = new SpectraWithPrecursorMassTable(dataFileName, indexPrefix, maxDataSize);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new SpectraWithPrecursorMassTable(pivotsFileName, "pivot", maxDataSize);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadVPIndex(dataTable, pivotSelectionMethod, numPivots,
                vpPartitionMethods, vpPartitionNum, vpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildPCTIndexOnMsMs(TableManager tableManager, String dataFileName, int maxDataSize,
                                             PivotSelectionMethod pivotSelectionMethod, PCTPartitionMethods partitionMethod,
                                             int numPivots, int partitionNum,
                                             int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                             String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("PCT", "msms",
                maxDataSize, pivotSelectionMethod, numPivots,
                partitionMethod.name(), partitionNum, maxLeafSize, mode);
        Table dataTable = new SpectraWithPrecursorMassTable(dataFileName, indexPrefix, maxDataSize);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new SpectraWithPrecursorMassTable(pivotsFileName, "pivot", maxDataSize);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadPCTIndex(dataTable, pivotSelectionMethod, numPivots,partitionMethod,
                partitionNum, maxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildGNATIndexOnMsMs(TableManager tableManager, String dataFileName, int maxDataSize,
                                             PivotSelectionMethod pivotSelectionMethod,
                                             int numPivots, PartitionMethod partitionMethod, int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                             String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gnat", "msms",
                maxDataSize, pivotSelectionMethod, numPivots,
                null, numPivots, maxLeafSize, mode);
        Table dataTable = new SpectraWithPrecursorMassTable(dataFileName, indexPrefix, maxDataSize);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new SpectraWithPrecursorMassTable(pivotsFileName, "pivot", maxDataSize);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGNATIndex(dataTable, pivotSelectionMethod, numPivots, partitionMethod,maxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildGHIndexOnMsMs(TableManager tableManager, String dataFileName, int maxDataSize,
                                            PivotSelectionMethod pivotSelectionMethod,
                                            GHPartitionMethods partitionMethod,
                                            int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                            String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gh", "msms",
                maxDataSize, pivotSelectionMethod, null,
                partitionMethod.name(), null, maxLeafSize, mode);
        Table dataTable = new SpectraWithPrecursorMassTable(dataFileName, indexPrefix, maxDataSize);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new SpectraWithPrecursorMassTable(pivotsFileName, "pivot", maxDataSize);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildVPIndexOnImage(TableManager tableManager, String dataFileName, int maxDataSize,
                                             PivotSelectionMethod pivotSelectionMethod,
                                             int numPivots, VPPartitionMethods vpPartitionMethods, int vpPartitionNum,
                                             int vpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                             String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("vp", "image",
                maxDataSize, pivotSelectionMethod, numPivots,
                vpPartitionMethods.name(), vpPartitionNum, vpMaxLeafSize, mode);
        Table dataTable = new ImageTable(dataFileName, indexPrefix, maxDataSize);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new ImageTable(pivotsFileName, "pivot", maxDataSize);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadVPIndex(dataTable, pivotSelectionMethod, numPivots,
                vpPartitionMethods, vpPartitionNum, vpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildPCTIndexOnImage(TableManager tableManager, String dataFileName, int maxDataSize,
                                              PivotSelectionMethod pivotSelectionMethod, PCTPartitionMethods partitionMethod,
                                              int numPivots, int partitionNum,
                                              int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                              String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("PCT", "image",
                maxDataSize, pivotSelectionMethod, numPivots,
                partitionMethod.name(), partitionNum, maxLeafSize, mode);
        Table dataTable = new ImageTable(dataFileName, indexPrefix, maxDataSize);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new ImageTable(pivotsFileName, "pivot", maxDataSize);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadPCTIndex(dataTable, pivotSelectionMethod, numPivots,partitionMethod,
                partitionNum, maxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildGNATIndexOnImage(TableManager tableManager, String dataFileName, int maxDataSize,
                                              PivotSelectionMethod pivotSelectionMethod,
                                              int numPivots, PartitionMethod partitionMethod, int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                              String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gnat", "image",
                maxDataSize, pivotSelectionMethod, numPivots,
                null, numPivots, maxLeafSize, mode);
        Table dataTable = new ImageTable(dataFileName, indexPrefix, maxDataSize);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new ImageTable(pivotsFileName, "pivot", maxDataSize);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGNATIndex(dataTable, pivotSelectionMethod, numPivots, partitionMethod, maxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildGHIndexOnImage(TableManager tableManager, String dataFileName, int maxDataSize,
                                             PivotSelectionMethod pivotSelectionMethod,
                                             GHPartitionMethods partitionMethod,
                                             int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                             String pivotsFileName) throws IOException
    {
        Table         pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gh", "image",
                maxDataSize, pivotSelectionMethod, null,
                partitionMethod.name(), null, maxLeafSize, mode);
        Table dataTable = new ImageTable(dataFileName, indexPrefix, maxDataSize);
        if (pivotsFileName != null)
        {
            //使用了指定支撑点集合的方式，获取支撑点集合
            pivotTable = new ImageTable(pivotsFileName, "pivot", maxDataSize);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    // ========== Apollonian Index 构建方法 ==========

    public static void bulkLoadApollonianIndex(Table dataTable, PivotSelectionMethod pivotSelectionMethod,
                                                ApollonianPartitionMethods partitionMethod, int maxLeafSize,
                                                HierarchicalPivotSelectionMode mode)
    {
        dataTable.buildApollonianIndex(pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
    }

    // 在 vector 上构建 AT 索引
    public static String buildApollonianIndexOnVector(TableManager tableManager, String dataFileName,
                                                       int dim, int maxDataSize,
                                                       PivotSelectionMethod pivotSelectionMethod,
                                                       ApollonianPartitionMethods partitionMethod,
                                                       int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("at", "vector", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new DoubleVectorTable(dataFileName, indexPrefix, maxDataSize, dim);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadApollonianIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    // 在 DNA 上构建 AT 索引
    public static String buildApollonianIndexOnDNA(TableManager tableManager, String dataFileName,
                                                   int fragLength, int maxDataSize,
                                                   PivotSelectionMethod pivotSelectionMethod,
                                                   ApollonianPartitionMethods partitionMethod,
                                                   int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("at", "dna", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new DNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadApollonianIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    // 在 RNA 上构建 AT 索引
    public static String buildApollonianIndexOnRNA(TableManager tableManager, String dataFileName,
                                                   int fragLength, int maxDataSize,
                                                   PivotSelectionMethod pivotSelectionMethod,
                                                   ApollonianPartitionMethods partitionMethod,
                                                   int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("at", "rna", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new RNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadApollonianIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    // 在 protein 上构建 AT 索引
    public static String buildApollonianIndexOnProtein(TableManager tableManager, String dataFileName,
                                                        int fragLength, int maxDataSize,
                                                        PivotSelectionMethod pivotSelectionMethod,
                                                        ApollonianPartitionMethods partitionMethod,
                                                        int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("at", "protein", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new PeptideTable(dataFileName, indexPrefix, maxDataSize, fragLength);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadApollonianIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    // 在 MS 上构建 AT 索引
    public static String buildApollonianIndexOnMS(TableManager tableManager, String dataFileName, int maxDataSize,
                                                   PivotSelectionMethod pivotSelectionMethod,
                                                   ApollonianPartitionMethods partitionMethod,
                                                   int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("at", "ms", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new SpectraTable(dataFileName, indexPrefix, maxDataSize, null);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadApollonianIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    // 在 MSMS 上构建 AT 索引
    public static String buildApollonianIndexOnMsMs(TableManager tableManager, String dataFileName, int maxDataSize,
                                                    PivotSelectionMethod pivotSelectionMethod,
                                                    ApollonianPartitionMethods partitionMethod,
                                                    int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("at", "msms", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new SpectraWithPrecursorMassTable(dataFileName, indexPrefix, maxDataSize);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadApollonianIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    // 在 image 上构建 AT 索引
    public static String buildApollonianIndexOnImage(TableManager tableManager, String dataFileName, int maxDataSize,
                                                     PivotSelectionMethod pivotSelectionMethod,
                                                     ApollonianPartitionMethods partitionMethod,
                                                     int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("at", "image", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new ImageTable(dataFileName, indexPrefix, maxDataSize);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadApollonianIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildApollonianIndexOnEnglish(TableManager tableManager, String dataFileName, int maxDataSize,
                                                       PivotSelectionMethod pivotSelectionMethod,
                                                       ApollonianPartitionMethods partitionMethod,
                                                       int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("at", "english", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new StringTable(dataFileName, indexPrefix, maxDataSize);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadApollonianIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    // ========== IAT Index build methods ==========

    public static void bulkLoadIATIndex(Table dataTable, PivotSelectionMethod pivotSelectionMethod,
                                        IATPartitionMethods partitionMethod, int maxLeafSize,
                                        HierarchicalPivotSelectionMode mode)
    {
        dataTable.buildIATIndex(pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
    }

    public static String buildIATIndexOnVector(TableManager tableManager, String dataFileName,
                                               int dim, int maxDataSize,
                                               PivotSelectionMethod pivotSelectionMethod,
                                               IATPartitionMethods partitionMethod,
                                               int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("iat", "vector", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new DoubleVectorTable(dataFileName, indexPrefix, maxDataSize, dim);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadIATIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildIATIndexOnDNA(TableManager tableManager, String dataFileName,
                                            int fragLength, int maxDataSize,
                                            PivotSelectionMethod pivotSelectionMethod,
                                            IATPartitionMethods partitionMethod,
                                            int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("iat", "dna", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new DNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadIATIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildIATIndexOnRNA(TableManager tableManager, String dataFileName,
                                            int fragLength, int maxDataSize,
                                            PivotSelectionMethod pivotSelectionMethod,
                                            IATPartitionMethods partitionMethod,
                                            int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("iat", "rna", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new RNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadIATIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildIATIndexOnProtein(TableManager tableManager, String dataFileName,
                                                int fragLength, int maxDataSize,
                                                PivotSelectionMethod pivotSelectionMethod,
                                                IATPartitionMethods partitionMethod,
                                                int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("iat", "protein", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new PeptideTable(dataFileName, indexPrefix, maxDataSize, fragLength);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadIATIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildIATIndexOnMS(TableManager tableManager, String dataFileName, int maxDataSize,
                                           PivotSelectionMethod pivotSelectionMethod,
                                           IATPartitionMethods partitionMethod,
                                           int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("iat", "ms", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new SpectraTable(dataFileName, indexPrefix, maxDataSize, null);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadIATIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildIATIndexOnMsMs(TableManager tableManager, String dataFileName, int maxDataSize,
                                             PivotSelectionMethod pivotSelectionMethod,
                                             IATPartitionMethods partitionMethod,
                                             int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("iat", "msms", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new SpectraWithPrecursorMassTable(dataFileName, indexPrefix, maxDataSize);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadIATIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildIATIndexOnImage(TableManager tableManager, String dataFileName, int maxDataSize,
                                              PivotSelectionMethod pivotSelectionMethod,
                                              IATPartitionMethods partitionMethod,
                                              int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("iat", "image", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new ImageTable(dataFileName, indexPrefix, maxDataSize);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadIATIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildIATIndexOnEnglish(TableManager tableManager, String dataFileName, int maxDataSize,
                                                PivotSelectionMethod pivotSelectionMethod,
                                                IATPartitionMethods partitionMethod,
                                                int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("iat", "english", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 3, maxLeafSize, mode);
        Table dataTable = new StringTable(dataFileName, indexPrefix, maxDataSize);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadIATIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    // ========== RGH Index 构建方法 ==========

    public static void bulkLoadRGHIndex(Table dataTable, PivotSelectionMethod pivotSelectionMethod,
                                      RGHPartitionMethods partitionMethod, int maxLeafSize,
                                      HierarchicalPivotSelectionMode mode)
    {
        dataTable.buildRGHIndex(pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
    }

    /**
     * Build a Power-Distance pivot-space index on an already-created table.
     */
    public static void bulkLoadPowerDistanceIndex(Table dataTable,
                                                  PivotSelectionMethod pivotSelectionMethod,
                                                  PartitionMethod partitionMethod,
                                                  int maxLeafSize,
                                                  HierarchicalPivotSelectionMode mode,
                                                  IndexObject[] pivotSet)
    {
        dataTable.buildPowerDistanceIndex(pivotSelectionMethod, partitionMethod, maxLeafSize, mode, pivotSet);
    }

    public static void bulkLoadPowerDistanceIndex(Table dataTable,
                                                  PivotSelectionMethod pivotSelectionMethod,
                                                  PartitionMethod partitionMethod,
                                                  int numPartitions,
                                                  int maxLeafSize,
                                                  HierarchicalPivotSelectionMode mode,
                                                  IndexObject[] pivotSet)
    {
        dataTable.buildPowerDistanceIndex(pivotSelectionMethod, partitionMethod,
                numPartitions, maxLeafSize, mode, pivotSet);
    }

    /**
     * Build a Log-Distance pivot-space index on an already-created table.
     */
    public static void bulkLoadLogDistanceIndex(Table dataTable,
                                                PivotSelectionMethod pivotSelectionMethod,
                                                PartitionMethod partitionMethod,
                                                int maxLeafSize,
                                                HierarchicalPivotSelectionMode mode,
                                                IndexObject[] pivotSet)
    {
        dataTable.buildLogDistanceIndex(pivotSelectionMethod, partitionMethod, maxLeafSize, mode, pivotSet);
    }

    // 在 vector 上构建 RGH 索引
    public static String buildRGHIndexOnVector(TableManager tableManager, String dataFileName,
                                              int dim, int maxDataSize,
                                              PivotSelectionMethod pivotSelectionMethod,
                                              RGHPartitionMethods partitionMethod,
                                              int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("rgh", "vector", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 2, maxLeafSize, mode);
        Table dataTable = new DoubleVectorTable(dataFileName, indexPrefix, maxDataSize, dim);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadRGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    // 在 DNA 上构建 RGH 索引
    public static String buildRGHIndexOnDNA(TableManager tableManager, String dataFileName,
                                            int fragLength, int maxDataSize,
                                            PivotSelectionMethod pivotSelectionMethod,
                                            RGHPartitionMethods partitionMethod,
                                            int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("rgh", "dna", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 2, maxLeafSize, mode);
        Table dataTable = new DNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadRGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    // 在 RNA 上构建 RGH 索引
    public static String buildRGHIndexOnRNA(TableManager tableManager, String dataFileName,
                                             int fragLength, int maxDataSize,
                                             PivotSelectionMethod pivotSelectionMethod,
                                             RGHPartitionMethods partitionMethod,
                                             int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("rgh", "rna", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 2, maxLeafSize, mode);
        Table dataTable = new RNATable(dataFileName, indexPrefix, maxDataSize, fragLength);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadRGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    // 在 protein 上构建 RGH 索引
    public static String buildRGHIndexOnProtein(TableManager tableManager, String dataFileName,
                                               int fragLength, int maxDataSize,
                                               PivotSelectionMethod pivotSelectionMethod,
                                               RGHPartitionMethods partitionMethod,
                                               int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("rgh", "protein", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 2, maxLeafSize, mode);
        Table dataTable = new PeptideTable(dataFileName, indexPrefix, maxDataSize, fragLength);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadRGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildRGHIndexOnImage(TableManager tableManager, String dataFileName, int maxDataSize,
                                             PivotSelectionMethod pivotSelectionMethod,
                                             RGHPartitionMethods partitionMethod,
                                             int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("rgh", "image", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 2, maxLeafSize, mode);
        Table dataTable = new ImageTable(dataFileName, indexPrefix, maxDataSize);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadRGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildRGHIndexOnEnglish(TableManager tableManager, String dataFileName, int maxDataSize,
                                               PivotSelectionMethod pivotSelectionMethod,
                                               RGHPartitionMethods partitionMethod,
                                               int maxLeafSize, HierarchicalPivotSelectionMode mode) throws IOException
    {
        String indexPrefix = getIndexPrefix("rgh", "english", maxDataSize, pivotSelectionMethod, 2,
                partitionMethod.name(), 2, maxLeafSize, mode);
        Table dataTable = new StringTable(dataFileName, indexPrefix, maxDataSize);
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadRGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }

    public static String buildVPIndexOnEnglish(TableManager tableManager, String dataFileName, int maxDataSize,
                                              PivotSelectionMethod pivotSelectionMethod,
                                              int numPivots, VPPartitionMethods vpPartitionMethods, int vpPartitionNum,
                                              int vpMaxLeafSize, HierarchicalPivotSelectionMode mode,
                                              String pivotsFileName) throws IOException
    {
        Table pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("vp", "english",
                maxDataSize, pivotSelectionMethod, numPivots,
                vpPartitionMethods.name(), vpPartitionNum, vpMaxLeafSize, mode);
        Table dataTable = new StringTable(dataFileName, indexPrefix, maxDataSize);
        if (pivotsFileName != null)
        {
            pivotTable = new StringTable(pivotsFileName, "pivot", maxDataSize);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadVPIndex(dataTable, pivotSelectionMethod, numPivots,
                vpPartitionMethods, vpPartitionNum, vpMaxLeafSize, mode, pivotSet);
        return indexPrefix;
    }

    public static String buildGHIndexOnEnglish(TableManager tableManager, String dataFileName, int maxDataSize,
                                              PivotSelectionMethod pivotSelectionMethod,
                                              GHPartitionMethods partitionMethod,
                                              int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                              String pivotsFileName) throws IOException
    {
        Table pivotTable;
        IndexObject[] pivotSet = null;
        String indexPrefix = getIndexPrefix("gh", "english",
                maxDataSize, pivotSelectionMethod, null,
                partitionMethod.name(), null, maxLeafSize, mode);
        Table dataTable = new StringTable(dataFileName, indexPrefix, maxDataSize);
        if (pivotsFileName != null)
        {
            pivotTable = new StringTable(pivotsFileName, "pivot", maxDataSize);
            tableManager.putTable(pivotTable);
            pivotSet = pivotTable.getData().toArray(new IndexObject[0]);
        }
        dataTable.setMetric(new CountedMetric(dataTable.getMetric()));
        tableManager.putTable(dataTable);
        bulkLoadGHIndex(dataTable, pivotSelectionMethod, partitionMethod, maxLeafSize, mode);
        return indexPrefix;
    }
}
