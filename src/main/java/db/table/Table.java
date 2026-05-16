package db.table;


import algorithms.datapartition.PCTPartitionMethods;
import algorithms.datapartition.PartitionMethod;
import algorithms.pivotselection.PivotSelectionMethod;
import db.type.DoubleIndexObjectPair;
import db.type.DoubleVector;
import db.type.IndexObject;
import index.search.Query;
import index.search.RangeQuery;
import index.structure.*;
import index.type.HierarchicalPivotSelectionMode;
import metric.CountedMetric;
import metric.Metric;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 数据库中的数据点集合的实体类，包含它对应的距离函数和在它上面构建的索引。
 */
public abstract class Table implements Serializable
{
    private static final long   serialVersionUID = 8791814013616626066L;
    public               int[]  originalRowIDs;
    /**
     * 记录支撑点
     */
    transient public IndexObject[] pivotData;
    /**
     * 要读入的数据文件的路径（精确到文件名）
     */
    protected            String sourceFileName;
    /**
     * 数据集的大小
     */
    protected            int    dataSize;
    /**
     * 存储到内存上的数据库的前缀名
     */
    protected String                      indexPrefix;
    protected Metric                      metric;
    protected Index                       index;
    /**
     * 管理他的tableManager的名字
     */
    protected String                      tableManagerName;
    transient List<? extends IndexObject> data;
    /**
     * 表示该Table在对应的TableManager中的下标索引
     */
    private int tableLocation = -1;

    public Table()
    {
    }

    /**
     * 构造函数
     *
     * @param fileName    构建索引的文件名
     * @param indexPrefix 输出索引文件的前缀
     * @param dataSize    要读入的数据集的大小
     * @param metric      用于构建索引的距离函数
     */
    Table(String fileName, String indexPrefix, int dataSize, Metric metric)
    {
        if (fileName == null)
            throw new IllegalArgumentException("fileName cannot be null!");
        if (dataSize <= 0)
            throw new IllegalArgumentException("maxDataSize must be greater than zero!");
        if (metric == null)
            throw new IllegalArgumentException("Metric cannot be null!");
        this.sourceFileName = fileName;
        this.indexPrefix    = indexPrefix;
        this.dataSize       = dataSize;
        this.metric         = metric;
    }

    /**
     * 加载部分数据入内存
     *
     * @param reader {@link BufferedReader}缓冲流
     * @param size   要加载进的大小
     */
    public abstract void loadData(BufferedReader reader, int size);

    /**
     * 获取数据表的数据点的数目
     *
     * @return 返回数据表的数据点的数目
     */
    public int size()
    {
        return dataSize;
    }

    /**
     * 给定压缩后table中数据的行号获取数据在未压缩table中的行号
     *
     * @param rowID 数据在压缩后table中的行号
     * @return 数据在未压缩table中的行号
     */
    public int getOriginalRowID(int rowID)
    {
        return originalRowIDs[rowID];
    }

    /**
     * 获取构建该数据表的索引
     *
     * @return 返回构建该数据表的索引
     */
    public Index getIndex()
    {
        return index;
    }

    /**
     * 获取该表中用于计算数据间距离的距离函数
     *
     * @return 返回该表中用于计算数据间距离的距离函数
     */
    public Metric getMetric()
    {
        return metric;
    }

    /**
     * 设置该表中用于计算元素间距离的距离函数
     *
     * @param metric 设置该表中用于计算元素间距离的距离函数
     */
    public void setMetric(Metric metric)
    {
        this.metric = metric;
    }

    /**
     * 利用table中的数据建立VPT索引
     *
     * @param psm           支撑点选择方法
     * @param numPivots     支撑点数目
     * @param pm            数据划分方法
     * @param numPartition  单个支撑点的扇出
     * @param maxLeafSize   叶子节点的最大数目
     * @param mode          层次化支撑点选取方法{@link HierarchicalPivotSelectionMode}
     * @param specifyPivots 为全局建树提供的支撑点集合 不需要使用时直接置为null
     */
    public void buildVPIndex(PivotSelectionMethod psm, int numPivots, PartitionMethod pm, int numPartition, int maxLeafSize, HierarchicalPivotSelectionMode mode, IndexObject[] specifyPivots)
    {
        double startTime, endTime, buildTime;

        //开始计时
        startTime = System.currentTimeMillis();

        //压缩数据
        compressData();
        if (metric instanceof CountedMetric)
            ((CountedMetric) metric).clear();


        index = new VPIndex(indexPrefix, data, metric, maxLeafSize, numPivots, numPartition, mode, psm, pm, specifyPivots);

        // 设置候选集大小
        if (mode == HierarchicalPivotSelectionMode.MIX)
        {
        }
        //建树
        index.buildTree();

        //MIX建树模式需要再次处理完成建树
        if (mode == HierarchicalPivotSelectionMode.MIX)
        {
            index.MixLeafNodeClear();
        }
        //记录建树使用的支撑点
        this.pivotData = index.getAllPivots();
        //存储支撑点数据 存储位置 根目录 名字 indexPrefix+PivotData.txt
        //        savePivotData(this.pivotData);

        //结束计时
        endTime   = System.currentTimeMillis();
        buildTime = (endTime - startTime) / 1000.00;

        //如果metric是统计型metric，则输出构建索引时进行的距离计算次数
        if (this.metric instanceof CountedMetric)
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + ", " + "distantCount = " + ((CountedMetric) metric).getCounter());
            System.out.println("pivotNum = " + index.getPivotNum());
        } else
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime);
        }

    }


    /**
     * 利用table中的数据建立CPT索引
     *
     * @param psm               支撑点选择方法
     * @param numPivots         支撑点数目
     * @param pm                数据划分方法
     * @param numPartition       单个支撑点的扇出
     * @param maxLeafSize       叶子节点的最大数目
     * @param mode              层次化支撑点选取方法{@link HierarchicalPivotSelectionMode}
     * @param specifyPivots     为全局建树提供的支撑点集合 不需要使用时直接置为null
     */
    public void buildCPIndex(PivotSelectionMethod psm, int numPivots, PartitionMethod pm, int numPartition, int maxLeafSize, HierarchicalPivotSelectionMode mode, IndexObject[] specifyPivots){
        double startTime, endTime, buildTime;

        //开始计时
        startTime = System.currentTimeMillis();

        //压缩数据
        compressData();
        if (metric instanceof CountedMetric) ((CountedMetric) metric).clear();


        index = new CPIndex(indexPrefix, data, metric, maxLeafSize, numPivots, numPartition, mode, psm, pm, specifyPivots);

        // 设置候选集大小
        if(mode == HierarchicalPivotSelectionMode.MIX)
        {
        }
        //建树
        index.buildTree();

        //MIX建树模式需要再次处理完成建树
        if(mode == HierarchicalPivotSelectionMode.MIX)
        {
            index.MixLeafNodeClear();
        }
        //记录建树使用的支撑点
        this.pivotData = index.getAllPivots();
        //存储支撑点数据 存储位置 根目录 名字 indexPrefix+PivotData.txt
//        savePivotData(this.pivotData);

        //结束计时
        endTime   = System.currentTimeMillis();
        buildTime = (endTime - startTime) / 1000.00;

        //如果metric是统计型metric，则输出构建索引时进行的距离计算次数
        if (this.metric instanceof CountedMetric)
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + ", " + "distantCount = " + ((CountedMetric) metric).getCounter());
            System.out.println("pivotNum = " + index.getPivotNum());
        } else
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime);
        }

    }


    /**
     * 将table内数据压缩，压缩原理如下：
     * 例如向量数据文件如下：
     * 行号      数据
     * 1     1.0 2.0 3.0
     * 2     1.0 2.0 3.0
     * 3     1.0 2.2 4.0
     * ......
     * 则当对数据进行压缩的时候，IndexObject={(1.0 2.0 3.0),rowIDStart=1,rowIDLength=2}
     */
    protected void compressData()
    {
        // first sort the list according to the data points.
        Collections.sort(data);

        // then, make a list of the unique dataPoints.
        final int              dataSize       = data.size();
        ArrayList<IndexObject> compressedData = new ArrayList<>(dataSize);
        int[]                  rowIDs2        = new int[dataSize];

        IndexObject dataPoint1 = data.get(0);
        int         tempSize   = 1;

        IndexObject dataPoint2;
        for (int i = 1; i < dataSize; i++)
        {
            dataPoint2 = data.get(i);
            if (dataPoint1.equals(dataPoint2))
            {
                tempSize++;
            } else
            {
                if (tempSize > 1)
                {
                    for (int j = i - tempSize; j < i; j++)
                    {
                        int rowID = data.get(j).getRowID();
                        rowIDs2[j] = originalRowIDs[rowID];
                    }
                    dataPoint1.setRowID(i - tempSize);
                    dataPoint1.setRowIDLength(tempSize);
                } else
                {
                    int rowID = data.get(i - 1).getRowID();
                    rowIDs2[i - 1] = originalRowIDs[rowID];
                    dataPoint1.setRowID(i - 1);
                }
                compressedData.add(dataPoint1);
                dataPoint1 = dataPoint2;
                tempSize   = 1;
            }
        }

        if (tempSize > 1)
        {
            for (int i = dataSize - tempSize; i < dataSize; i++)
            {
                int rowID = data.get(i).getRowID();
                rowIDs2[i] = originalRowIDs[rowID];
            }
            dataPoint1.setRowID(dataSize - tempSize);
            dataPoint1.setRowIDLength(tempSize);
        } else
        {
            int rowID = data.get(dataSize - 1).getRowID();
            rowIDs2[dataSize - 1] = originalRowIDs[rowID];
            dataPoint1.setRowID(dataSize - 1);
        }
        compressedData.add(dataPoint1);

        compressedData.trimToSize();
        //System.out.println("original size: " + dataSize + " compressed data size: " + compressedData.size());
        data           = compressedData;
        originalRowIDs = rowIDs2;
    }

    /**
     * 利用table中的数据建立GHT索引
     *
     * @param psm         支撑点选择方法
     * @param pm          数据划分方法
     * @param maxLeafSize 叶子节点的最大数目
     * @param mode        层次化支撑点选取方法{@link HierarchicalPivotSelectionMode}
     */
    public void buildGHIndex(PivotSelectionMethod psm, PartitionMethod pm, int maxLeafSize, HierarchicalPivotSelectionMode mode)
    {
        double startTime, endTime, buildTime;

        //开始计时
        startTime = System.currentTimeMillis();

        //压缩数据
        compressData();
        if (metric instanceof CountedMetric)
            ((CountedMetric) metric).clear();


        index = new GHIndex(indexPrefix, data, metric, maxLeafSize, 2, 2, mode, psm, pm);

        //建树
        index.buildTree();

        //结束计时
        endTime   = System.currentTimeMillis();
        buildTime = (endTime - startTime) / 1000.00;

        //如果metric是统计型metric，则输出构建索引时进行的距离计算次数
        if (this.metric instanceof CountedMetric)
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + ", " + "distantCount = " + ((CountedMetric) metric).getCounter());
        } else
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime);
        }

    }

    /**
     * 利用table中的数据建立GNAT索引
     *
     * @param psm               支撑点选择方法
     * @param numPivot          支撑点数目
     * @param pm                数据划分方法
     * @param maxLeafSize       叶子节点的最大数目
     * @param mode              层次化支撑点选取方法{@link HierarchicalPivotSelectionMode}
     */
    public void buildGNATIndex(PivotSelectionMethod psm,int numPivot, PartitionMethod pm, int maxLeafSize,
                               HierarchicalPivotSelectionMode mode, IndexObject[] pivotSet)
    {
        double startTime, endTime, buildTime;

        //开始计时
        startTime = System.currentTimeMillis();

        //压缩数据
        compressData();
        if (metric instanceof CountedMetric) ((CountedMetric) metric).clear();

        if (psm==null&&pivotSet!=null){
            //指定支撑点构造
            index = new GNATIndex(indexPrefix, data, metric, maxLeafSize, numPivot, mode,  psm, pm, pivotSet);
        }else{
            index = new GNATIndex(indexPrefix, data, metric, maxLeafSize, numPivot, mode,  psm, pm);
        }

        //建树
        index.buildTree();

        //结束计时
        endTime   = System.currentTimeMillis();
        buildTime = (endTime - startTime) / 1000.00;

        //如果metric是统计型metric，则输出构建索引时进行的距离计算次数
        if (this.metric instanceof CountedMetric)
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + ", " + "distantCount = " + ((CountedMetric) metric).getCounter());
        } else
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime);
        }

    }

    /**
     * 利用table中的数据建立PCT索引
     *
     * @param psm           支撑点选择方法
     * @param numPivots     支撑点数目
     * @param pm            划分方法，参考{@link PCTPartitionMethods}
     * @param numPartitions 划分块数
     * @param maxLeafSize   叶子节点的最大数目
     * @param mode          层次化支撑点选取方法{@link HierarchicalPivotSelectionMode}
     * @param specifyPivots 为全局建树提供的支撑点集合 不需要使用时直接置为null
     * @see PCTIndex
     */
    public void buildPCTIndex(PivotSelectionMethod psm, int numPivots,PartitionMethod pm, int numPartitions, int maxLeafSize, HierarchicalPivotSelectionMode mode, IndexObject[] specifyPivots)
    {
        double startTime, endTime, buildTime;

        //开始计时
        startTime = System.currentTimeMillis();

        //PCT必须要压缩数据
        compressData();
        if (metric instanceof CountedMetric)
            ((CountedMetric) metric).clear();

        index = new PCTIndex(indexPrefix, this.data, metric, maxLeafSize, numPivots, numPartitions, mode, psm, pm, specifyPivots);

        //建树
        index.buildTree();

        //结束计时
        endTime   = System.currentTimeMillis();
        buildTime = (endTime - startTime) / 1000.00;

        //如果metric是统计型metric，则输出构建索引时进行的距离计算次数
        if (this.metric instanceof CountedMetric)
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + "s, " + "distantCount = " + ((CountedMetric) metric).getCounter());
        } else
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + "s");
        }

    }

    /**
     * 利用table中的数据建立Apollonian索引
     *
     * @param psm           支撑点选择方法（必须选择返回2个支撑点的方法）
     * @param pm            划分方法
     * @param maxLeafSize   叶子节点的最大数目
     * @param mode          层次化支撑点选取方法{@link HierarchicalPivotSelectionMode}
     * @see ApollonianIndex
     */
    public void buildApollonianIndex(PivotSelectionMethod psm, PartitionMethod pm, int maxLeafSize,
                                     HierarchicalPivotSelectionMode mode)
    {
        double startTime, endTime, buildTime;

        //开始计时
        startTime = System.currentTimeMillis();

        //压缩数据
        compressData();
        if (metric instanceof CountedMetric)
            ((CountedMetric) metric).clear();

        // Apollonian Tree 固定使用 2 个 pivot 和 3 个分区
        int numPivots = 2;
        int numPartitions = 3;

        index = new ApollonianIndex(indexPrefix, this.data, metric, maxLeafSize, numPivots, numPartitions, mode, psm, pm);

        //建树
        index.buildTree();

        //结束计时
        endTime   = System.currentTimeMillis();
        buildTime = (endTime - startTime) / 1000.00;

        //如果metric是统计型metric，则输出构建索引时进行的距离计算次数
        if (this.metric instanceof CountedMetric)
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + "s, " + "distantCount = " + ((CountedMetric) metric).getCounter());
        } else
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + "s");
        }

    }

    /**
     * 构建 RGH-Tree 索引
     *
     * @param psm           支撑点选择方法（必须选择返回2个支撑点的方法）
     * @param pm            划分方法
     * @param maxLeafSize   叶子节点的最大数目
     * @param mode          层次化支撑点选取方法{@link HierarchicalPivotSelectionMode}
     * @see RGHIndex
     */
    /**
     * Build an IAT index.
     */
    public void buildIATIndex(PivotSelectionMethod psm, PartitionMethod pm, int maxLeafSize,
                              HierarchicalPivotSelectionMode mode)
    {
        double startTime, endTime, buildTime;

        startTime = System.currentTimeMillis();

        compressData();
        if (metric instanceof CountedMetric)
            ((CountedMetric) metric).clear();

        int numPivots = 2;
        int numPartitions = 3;

        index = new IATIndex(indexPrefix, this.data, metric, maxLeafSize, numPivots, numPartitions, mode, psm, pm);

        index.buildTree();

        endTime   = System.currentTimeMillis();
        buildTime = (endTime - startTime) / 1000.00;

        if (this.metric instanceof CountedMetric)
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + "s, " + "distantCount = " + ((CountedMetric) metric).getCounter());
        } else
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + "s");
        }
    }

    public void buildRGHIndex(PivotSelectionMethod psm, PartitionMethod pm, int maxLeafSize,
                           HierarchicalPivotSelectionMode mode)
    {
        double startTime, endTime, buildTime;

        startTime = System.currentTimeMillis();

        compressData();
        if (metric instanceof CountedMetric)
            ((CountedMetric) metric).clear();

        int numPivots = 2;
        int numPartitions = 2;

        index = new RGHIndex(indexPrefix, this.data, metric, maxLeafSize, numPivots, numPartitions, mode, psm, pm);

        index.buildTree();

        endTime   = System.currentTimeMillis();
        buildTime = (endTime - startTime) / 1000.00;

        if (this.metric instanceof CountedMetric)
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + "s, " + "distantCount = " + ((CountedMetric) metric).getCounter());
        } else
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + "s");
        }

    }

    /**
     * Build a Power-Distance pivot-space index.
     *
     * <p>The first version is a binary tree: every internal node uses two pivots
     * and one linear boundary in power-distance pivot space.</p>
     */
    public void buildPowerDistanceIndex(PivotSelectionMethod psm, PartitionMethod pm, int maxLeafSize,
                                        HierarchicalPivotSelectionMode mode, IndexObject[] specifyPivots)
    {
        buildPowerDistanceIndex(psm, pm, 2, maxLeafSize, mode, specifyPivots);
    }

    public void buildPowerDistanceIndex(PivotSelectionMethod psm, PartitionMethod pm, int numPartitions,
                                        int maxLeafSize, HierarchicalPivotSelectionMode mode,
                                        IndexObject[] specifyPivots)
    {
        double startTime, endTime, buildTime;

        startTime = System.currentTimeMillis();

        compressData();
        if (metric instanceof CountedMetric)
            ((CountedMetric) metric).clear();

        index = new PowerDistanceIndex(indexPrefix, this.data, metric, maxLeafSize,
                numPartitions,
                mode, psm, pm, specifyPivots);

        index.buildTree();

        endTime = System.currentTimeMillis();
        buildTime = (endTime - startTime) / 1000.00;

        this.pivotData = index.getAllPivots();

        if (this.metric instanceof CountedMetric)
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + "s, " + "distantCount = " + ((CountedMetric) metric).getCounter());
            System.out.println("pivotNum = " + index.getPivotNum());
        } else
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + "s");
        }
    }

    /**
     * Build a Log-Distance pivot-space index.
     *
     * <p>The first version is a binary tree: every internal node uses two pivots
     * and one linear boundary in log-distance pivot space.</p>
     */
    public void buildLogDistanceIndex(PivotSelectionMethod psm, PartitionMethod pm, int maxLeafSize,
                                      HierarchicalPivotSelectionMode mode, IndexObject[] specifyPivots)
    {
        double startTime, endTime, buildTime;

        startTime = System.currentTimeMillis();

        compressData();
        if (metric instanceof CountedMetric)
            ((CountedMetric) metric).clear();

        index = new LogDistanceIndex(indexPrefix, this.data, metric, maxLeafSize,
                mode, psm, pm, specifyPivots);

        index.buildTree();

        endTime = System.currentTimeMillis();
        buildTime = (endTime - startTime) / 1000.00;

        this.pivotData = index.getAllPivots();

        if (this.metric instanceof CountedMetric)
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + "s, " + "distantCount = " + ((CountedMetric) metric).getCounter());
            System.out.println("pivotNum = " + index.getPivotNum());
        } else
        {
            System.out.println("dataSize = " + dataSize + ", " + "buildTime = " + buildTime + "s");
        }
    }

    /**
     * 通过线性扫描数据data搜索Query，返回搜索到的对象。
     *
     * @param query 要搜索的对象
     * @return 搜索结果的Pair列表, Pair的第一个值是该对象与搜索对象的距离，第二个值是结果对象。
     */
    public List<DoubleIndexObjectPair> searchByLinear(Query query)
    {
        this.data = getData();

        //如果传入的是可计数的metric，就清空计数
        //        if (this.metric instanceof CountedMetric) ((CountedMetric) this.metric).clear();

        //判断搜索对象的类型
        List<DoubleIndexObjectPair> result = new LinkedList<>();
        IndexObject[]               expendedData;
        if (query instanceof RangeQuery)
        {
            //传入的对象是范围搜索
            RangeQuery  rangeQuery = (RangeQuery) query;
            IndexObject q          = rangeQuery.getQueryObject();
            double      radius     = rangeQuery.getRadius();
            //用来存储距离
            double dis;
            for (var d : data)
            {
                dis = metric.getDistance(q, d);
                if (dis <= radius)
                {
                    expendedData = d.expand();
                    for (var ed : expendedData)
                    {
                        result.add(new DoubleIndexObjectPair(dis, ed));
                    }
                }
            }
        } else
            throw new UnsupportedOperationException("不支持的搜索类型！");
        //        if (Debug.debug) {
        //            if (metric instanceof CountedMetric) System.out.println("本次搜索的距离计算次数是：" + ((CountedMetric) this.metric).getCounter());
        //        }
        return result;
    }

    /**
     * 获取数据表的全部数据点
     *
     * @return 返回数据表的全部数据点
     */
    public List<? extends IndexObject> getData()
    {
        if(data == null)
        {
            this.data = index.getAllPoints();
            return this.data;
        }
        //如果现在的数据集大小小于应有的大小，证明跑过建树函数，数据集已经被改变了，则重新从索引里读取数据集
        if (data.size() < dataSize)
        {

            this.data = index.getAllPoints();
        }
        return this.data;
    }


    /**
     * 获取该Table在对应的TableManager中的位置。
     *
     * @return 返回该Table在对应的TableManager中的位置。
     */
    public int getTableLocation()
    {
        return tableLocation;
    }

    /**
     * 设置table在其Manager中的索引下标
     *
     * @param tableLocation table在其Manager中的下标
     */
    public void setTableLocation(int tableLocation)
    {
        this.tableLocation = tableLocation;
    }

    /**
     * 获取当前table的Manager的名称
     *
     * @return 返回当前table的Manager的名称
     */
    public String getTableManagerName()
    {
        return tableManagerName;
    }

    /**
     * 设置当前table的Manger的名称
     *
     * @param tableManagerName table的Manger的名称
     */
    public void setTableManagerName(String tableManagerName)
    {
        this.tableManagerName = tableManagerName;
    }

    /**
     * 获取输出索引文件的前缀
     *
     * @return 返回输出索引文件的前缀
     */
    public String getTableIndexPrefix()
    {
        return indexPrefix;
    }

    /**
     * 获取使用的支撑点集合
     *
     * @return 使用的支撑点集合
     */
    public IndexObject[] getPivotData()
    {
        return this.pivotData;
    }

    /**
     * 存储支撑点数据
     * 第一行 维数 数据量
     * 其后  数据坐标
     * <p>
     * 注意使用不同类型数据要添加相应的存储方式
     *
     * @param pivotData 支撑点数据集合
     */
    private void savePivotData(IndexObject[] pivotData) throws IOException
    {
        BufferedWriter bw           = new BufferedWriter(new FileWriter("./" + this.indexPrefix + "PivotData.txt"));
        StringBuffer   stringBuffer = new StringBuffer();
        StringBuilder  onePivot     = new StringBuilder();
        //写第一行
        stringBuffer.append(pivotData[0].size() + " " + pivotData.length);
        //注意相应按数据集的toString方法
        if (indexPrefix.contains("vector"))
        {
            for (int i = 0; i < pivotData.length; i++)
            {
                stringBuffer.append("\n");
                var temp = ((DoubleVector) pivotData[i]).getData();
                stringBuffer.append(temp[0]);
                for (int j = 1; j < temp.length; j++)
                {
                    stringBuffer.append("\t").append(temp[j]);
                }
            }
        }
        bw.write(stringBuffer.toString());
        bw.close();
    }

    //    /**
    //     * 在MIX模式搜索时读取pivotData
    //     *
    //     * 注意使用不同类型数据要添加相应的存储方式
    //     */
    //    public void loadPivotData() throws IOException {
    //        if(indexPrefix.contains("vector")) {
    //
    //            Table table = new DoubleVectorTable("./"+this.indexPrefix+"PivotData.txt", indexPrefix, 9999999, 999999);
    //            DoubleVector[] pivot_read = table.data.toArray(new DoubleVector[0]);
    //            this.pivotData = pivot_read;
    //        }
    //
    //    }


    @Override
    public String toString()
    {
        return "Table{" + "dataSize=" + dataSize + ", indexPrefix='" + indexPrefix + '\'' + ", tableLocation=" + tableLocation + ", metric=" + metric + '}';
    }

}
