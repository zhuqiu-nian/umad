package index.structure;

import algorithms.datapartition.PartitionMethod;
import db.type.IndexObject;
import index.search.Cursor;
import index.search.Query;
import index.search.RangeCursor;
import index.search.RangeQuery;
import index.type.HierarchicalPivotSelectionMode;
import manager.MckoiObjectIOManager;
import manager.ObjectIOManager;
import metric.Metric;
import util.Debug;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

/**
 * 该抽象类是所有索引类型的父类，实现一个新的索引树类型都需要继承该类并实现其中的抽象方法。
 *
 * <pre>
 *     要添加一个新的索引类型需要实现以下四个方法：
 *     1. {@link AbstractIndex#pivotSelection(Metric, List, List, int)}：
 *          通过该方法向建树过程提供支撑点集合。
 *     2. {@link AbstractIndex#partition(Metric, IndexObject[], List, int)}：
 *          通过实现该方法向建树过程提供划分结果。
 *     3. {@link AbstractIndex#search(Query)}：
 *     3. {@link AbstractIndex#search(Query)}：
 *          通过实现该方法返回对应索引树的搜索结果的实体类。
 *     4. {@link AbstractIndex#setPivotCandidateSetSize(int)}：
 *          该方法只有在新建索引对象实例，参数{@link HierarchicalPivotSelectionMode}选择为"MIX"的时候才会调用，
 *          通过该方法向"MIX"建树模式提供候选集。
 * </pre>
 */
public abstract class AbstractIndex implements Index
{

    private static final       long            serialVersionUID      = -5683141537579546207L;
    transient protected static String          nodeFileNameExtension = "000";
    transient protected        ObjectIOManager oiom;
    transient protected        Logger          logger;
    HierarchicalPivotSelectionMode hierarchicalPivotSelectionMode; //构建索引的模式
    protected long root;   //索引树根节点
    int    totalSize;  //数据的总大小
    int    maxLeafSize;  //叶子的最大容量
    Metric metric;
    /**
     * 该索引文件实例化到内存中的时候的前缀名,当索引文件实例化到内存的时候，文件名为“indexPrefix.000"
     */
    String indexPrefix;
    int    numPivot; //支撑点数目
    int    numPartitions;  //划分块数
    transient List<? extends IndexObject> data; //数据集合
    transient List<? extends IndexObject> pivotCandidateSet     = null;   //当使用混合模式构建索引的时候的支撑点候选集
    transient int                         pivotCandidateSetSize = Integer.MIN_VALUE;   //当使用混合模式构建索引树的时候的支撑点候选集的大小
    transient IndexObject[] specifyPivots = null;  //在使用GLOBAL建树时 传入该参数 直接使用传入的数据作为支撑点
    /**
     * 记录支撑点
     */
    transient Set<IndexObject> allPivotSet = null;

    /**
     * 使用局部选点方法构建索引。
     *
     * @param indexPrefix   输出的索引文件的前缀名
     * @param data          用于构建索引的数据
     * @param metric        构建索引使用的距离函数
     * @param maxLeafSize   叶子节点的最大容量
     * @param numPivot      支撑点数目
     * @param numPartitions 每层的划分块数
     */
    public AbstractIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric, int maxLeafSize, int numPivot, int numPartitions)
    {
        this(indexPrefix, data, metric, maxLeafSize, numPivot, numPartitions, HierarchicalPivotSelectionMode.LOCAL, null);
    }
//    /**
//     * 使用GLOBAL建树时 通过该构造函数 可以指定支撑点集合
//     *
//     * @param indexPrefix                    输出的索引文件的前缀名
//     * @param data                           用于构建索引的数据
//     * @param metric                         构建索引使用的距离函数
//     * @param maxLeafSize                    叶子节点的最大容量
//     * @param numPivot                       支撑点数目
//     * @param numPartitions                  每层的划分块数
//     * @param hierarchicalPivotSelectionMode 构建索引的模式，可选值有[LOCAL, GLOBAL, MIX]
//     * @param specifyPivots                  使用GLOBAL建树时 传入该参数 直接使用传入的数据作为支撑点
//     */
//    public AbstractIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric, int maxLeafSize, int numPivot, int numPartitions, HierarchicalPivotSelectionMode hierarchicalPivotSelectionMode, IndexObject[] specifyPivots)
//    {
//        this(indexPrefix, data, metric, maxLeafSize, numPivot, numPartitions, hierarchicalPivotSelectionMode);
//        if (hierarchicalPivotSelectionMode != HierarchicalPivotSelectionMode.GLOBAL)
//        {
//            throw new RuntimeException("该构造方法只为全局建树服务");
//        }
//        this.specifyPivots = specifyPivots;
//
//    }
    /**
     * 使用指定模式构建索引。
     *
     * @param indexPrefix                    输出的索引文件的前缀名
     * @param data                           用于构建索引的数据
     * @param metric                         构建索引使用的距离函数
     * @param maxLeafSize                    叶子节点的最大容量
     * @param numPivot                       支撑点数目
     * @param numPartitions                  每层的划分块数
     * @param hierarchicalPivotSelectionMode 构建索引的模式，可选值有[LOCAL, GLOBAL, MIX]
     * @param specifyPivots                  使用GLOBAL建树时 传入该参数 直接使用传入的数据作为支撑点
     */
    public AbstractIndex(String indexPrefix, List<? extends IndexObject> data, Metric metric, int maxLeafSize, int numPivot, int numPartitions, HierarchicalPivotSelectionMode hierarchicalPivotSelectionMode, IndexObject[] specifyPivots)
    {
        if (indexPrefix == null) throw new IllegalArgumentException("fileName cannot be null!");
        if (metric == null) throw new IllegalArgumentException("metric cannot be null!");
        if (data == null) throw new IllegalArgumentException("data list cannot be null!");

        this.indexPrefix   = indexPrefix;
        this.metric        = metric;
        totalSize          = data.size();
        this.maxLeafSize   = maxLeafSize;
        this.numPivot      = numPivot;
        this.numPartitions = numPartitions;
        this.data          = data;
        this.specifyPivots = specifyPivots; //设定的支撑点集合 只在GLOBAL模式下 使用
        this.allPivotSet = new HashSet();

        //合法性检查
        if (this.totalSize <= this.numPivot) throw new IllegalArgumentException("数据集大小不能小于支撑点的数目！");

        //
        if (specifyPivots != null && hierarchicalPivotSelectionMode != HierarchicalPivotSelectionMode.GLOBAL)
        {
            throw new RuntimeException("只有使用全局选点方式建树的时候才可以指定支撑点！");
        }
        // 1. create the io manager, writeable
        initOIOM(false);
        openOIOM();

        // 2. create the logger
        logger = Logger.getLogger("umad.index");
        // set the logger level;
        logger.setLevel(Debug.LEVEL);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Debug.LEVEL);
        // init the Handler: currently use a console handler, in the future will probably use the
        // FileHandler, or both
        logger.addHandler(consoleHandler);

        switch (hierarchicalPivotSelectionMode)
        {
            case GLOBAL:
                this.hierarchicalPivotSelectionMode = HierarchicalPivotSelectionMode.GLOBAL;
                break;
            case MIX:
                this.hierarchicalPivotSelectionMode = HierarchicalPivotSelectionMode.MIX;
                break;
            default:
                this.hierarchicalPivotSelectionMode = HierarchicalPivotSelectionMode.LOCAL;
        }
    }

    /**
     * 初始化对象IO管理器{@code ObjectIOManager}
     *
     * @param readOnly
     */
    void initOIOM(boolean readOnly)
    {
        oiom = new MckoiObjectIOManager(indexPrefix, nodeFileNameExtension, 1024 * 1024 * 1024, "Java IO", 4, 128 * 1024, readOnly);
    }

    /**
     * 打开对象IO管理器
     */
    void openOIOM()
    {
        String nodeFileName = indexPrefix + "." + nodeFileNameExtension;
        try
        {
            if (!oiom.open())
            {
                throw new Error("Cannot open store for :" + nodeFileName);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 构建索引树。每个index必须在创建{@link AbstractIndex}后，调用该方法来执行索引构建。
     */
    public void buildTree()
    {
        if (hierarchicalPivotSelectionMode == HierarchicalPivotSelectionMode.MIX && pivotCandidateSetSize == Integer.MIN_VALUE)
        {
            //用户未设置支撑点候选集大小，使用默认值赋值
            pivotCandidateSetSize = numPivot * 5000;
        }
        //建树
        this.root = bulkLoad(hierarchicalPivotSelectionMode);


        //关闭IO管理器
        try
        {
            this.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        initOIOM(true);
        openOIOM();
    }

    /**
     * 当{@link HierarchicalPivotSelectionMode}选择MIX模式的时候，使用该方法设置支撑点候选集合的大小。
     *
     * @param pivotCandidateSetSize 支撑点候选集的大小
     */
    public void setPivotCandidateSetSize(int pivotCandidateSetSize)
    {
        if (this.hierarchicalPivotSelectionMode != HierarchicalPivotSelectionMode.MIX)
            throw new IllegalArgumentException("只有当HierarchicalPivotSelectionMode设置为MIX的时候才可以设置支撑点候选集合的大小！");
        this.pivotCandidateSetSize = pivotCandidateSetSize;
    }

    /**
     * 获取距离函数对象
     * <p>
     * {@link Index#getMetric()}
     *
     * @return 返回距离函数对象
     */
    public Metric getMetric()
    {
        return this.metric;
    }

    /**
     * 获取索引内数据的总量
     *
     * @return 索引的数据大小
     * @see Index#size()
     */
    public int size()
    {
        return totalSize;
    }

    /**
     * 该方法需要在具体的索引中进行实现，在索引中查找指定的{@link Query}，在该方法中新建索引类型相应的{@link Cursor}。
     *
     * <p>
     * 需要通过实现该方法，返回索引类型相应的{@link Cursor}。通过对{@code Cursor}的遍历操作，可以得到{@link Query}操作的结果。
     * 以下是VPIndex的search的实现：
     * </p>
     * <pre>
     *      Cursor search.txt(Query q)
     *      {
     *          if (q instanceof RangeQuery)
     *              return new VPRangeCursor((RangeQuery) q, oiom, metric, root);
     *          else if (q instanceof KNNQuery)
     *              return new VPKNNCursor((KNNQuery) q, oiom, metric, root);
     *          else
     *              throw new UnsupportedOperationException("Unsupported query db " + q.getClass());
     *       }
     * </pre>
     *
     * @param q 搜索对象
     * @see Cursor
     */
    public abstract Cursor search(Query q);

    /**
     * 获取Index内的所有的数据点
     *
     * @return 所有的数据点 {@link IndexObject}的列表{@code List<IndexObject>}。
     * @see Index#getAllPoints()
     */
    public List<IndexObject> getAllPoints()
    {
        return Cursor.unzipTheData(Cursor.getAllCompressedPoints(oiom, root, new Hashtable<>()));
    }

    /**
     * 关闭对象IO管理器
     *
     * @throws IOException 关闭oiom时出现IO异常
     * @see Index#close()
     */
    public void close() throws IOException
    {
        oiom.close();
    }

    /**
     * 删除对象IO管理器（注意，此方法会连同生成的数据库索引文件一起删除）
     *
     * @throws IOException 销毁oiom时出现IO异常
     * @see Index#destroy()
     */
    public void destroy()
    {
        oiom.close();
        // delete the oiom
        (new File(indexPrefix + "." + nodeFileNameExtension)).delete();
    }

    /**
     * 该方法可通过"LOCAL"、“GLOBAL”和“MIX”三种模式进行建树，建树完成后返回建好的索引树的根指针。
     *
     * @param hierarchicalPivotSelectionMode 层次支撑点选择模式
     * @return 返回建好的索引树的根指针
     * @see HierarchicalPivotSelectionMode
     */
    public long bulkLoad(HierarchicalPivotSelectionMode hierarchicalPivotSelectionMode)
    {
        int[]            pivots;  //选择的支撑点的下标
        IndexObject[]    pivotSet;  //将下标转成相应的支撑点对象
        PartitionResults partition = null;
        try
        {
            switch (hierarchicalPivotSelectionMode)
            {
                case LOCAL:
                    pivots = pivotSelection(metric, this.data, this.data, numPivot);
                    //从数据集中删除被选择的支撑点
                    pivotSet = new IndexObject[pivots.length];
                    for (int i = 0; i < pivotSet.length; i++) pivotSet[i] = data.get(pivots[i]);
                    this.data.removeAll(Arrays.asList(pivotSet));
                    this.allPivotSet.addAll(Arrays.asList(pivotSet));
                    partition = partition(metric, pivotSet, data, numPartitions);

                    return localBulkLoad(partition, pivotSet, numPartitions);

                case GLOBAL:
                    if (specifyPivots == null)
                    {
                        pivots = pivotSelection(metric, this.data, this.data, numPivot);
//                    从数据集中删除被选择的支撑点
                        pivotSet = new IndexObject[pivots.length];
                        for (int i = 0; i < pivotSet.length; i++) pivotSet[i] = data.get(pivots[i]);

                    }else{ //使用固定的支撑点
                        pivotSet = specifyPivots;

                    }
                    this.allPivotSet.addAll(Arrays.asList(pivotSet));
                    this.data.removeAll(Arrays.asList(pivotSet));
                    partition = partition(metric, pivotSet, data, numPartitions);
                    return globalBulkLoad(partition, pivotSet, numPartitions);

                case MIX:
                    this.pivotCandidateSet = selectPivotCandicateSet(metric, data, pivotCandidateSetSize);
                    //根据评价集从候选集中选出对应的支撑点
                    pivots = pivotSelection(metric, pivotCandidateSet, this.data, numPivot);
                    pivotSet = new IndexObject[pivots.length];
                    for (int i = 0; i < pivotSet.length; i++) pivotSet[i] = pivotCandidateSet.get(pivots[i]);
                    this.allPivotSet.addAll(Arrays.asList(pivotSet));
                    //将选择且存在于评价集里的支撑点删除
                    this.data.removeAll(Arrays.asList(pivotSet));
                    partition = partition(metric, pivotSet, data, numPartitions);
                    return mixBulkLoad(partition, pivotSet, numPartitions);
            }
        } catch (IOException e)
        {
            throw new RuntimeException("节点写入时发生异常");
        }
        throw new RuntimeException("bulkLoad加载失败！");
    }

    /**
     * 生成支撑点候选集。
     *
     * <p>
     * 在{@link HierarchicalPivotSelectionMode}选择为MIX模式的时候，需要重载实现该方法。通过该方法从传入的data中抽样
     * pivotCandidateSetSize({@link AbstractIndex#setPivotCandidateSetSize(int)}数目的支撑点候选集合。在调用
     * {@link AbstractIndex#buildTree()}的时候，会通过该方法获取支撑点候选集，进行后续的建树操作。
     * </p>
     *
     * @param metric                距离函数的实例。
     * @param data                  被抽样的数据集
     * @param pivotCandidateSetSize 需要的支撑点候选集的大小，默认大小为//todo 选择的最优
     * @return 支撑点候选集合
     */
    List<? extends IndexObject> selectPivotCandicateSet(Metric metric, List<? extends IndexObject> data, int pivotCandidateSetSize)
    {
        throw new UnsupportedOperationException("当HierarchicalPivotSelectionMode设置为MIX的时候，必须override该方法，通过该方法返回支撑点候选集合！");
    }

    /**
     * 使用混合层次选点来创建索引树。更多请参考{@link HierarchicalPivotSelectionMode}
     *
     * <p>
     * 当{@link HierarchicalPivotSelectionMode}选择MIX的时候，建树时会自动调用该方法来构建索引树。
     * </p>
     *
     * @param partition     划分结果
     * @param pivotSet      支撑点集合
     * @param numPartitions 划分数目
     * @return 树的根指针
     * @throws IOException 节点写入错误时抛出
     */
    protected long mixBulkLoad(PartitionResults partition, IndexObject[] pivotSet, int numPartitions) throws IOException
    {
        long[] childAddress = new long[partition.getNumPartition()];
        int[]  newPivots;
        long   point;
        //遍历划分结果，递归的建树
        for (int i = 0; i < partition.getNumPartition(); i++)
        {
            List<? extends IndexObject> partitionData = partition.getPartitionOf(i);
            //选择支撑点,partitionData作为评价集 候选集在类中获取
            newPivots = pivotSelection(metric, this.pivotCandidateSet, partitionData, this.numPivot);
            IndexObject[] newPivotsSet = new IndexObject[newPivots.length];
            //从数据集中删除被选择的支撑点
            for (int j = 0; j < newPivotsSet.length; j++) newPivotsSet[j] = this.pivotCandidateSet.get(newPivots[j]);
            this.allPivotSet.addAll(Arrays.asList(newPivotsSet));
            partitionData.removeAll(Arrays.asList(newPivotsSet));

            if (partitionData.size() > this.maxLeafSize)
            {

                //如果数据量比叶子节点容量大，则继续划分建树
                PartitionResults partitionResults = partition(metric, newPivotsSet, partitionData, numPartitions);
                point = mixBulkLoad(partitionResults, newPivotsSet, numPartitions);
            } else
            {
                //数据量比叶子节点容量小了，这个时候创建叶子节点
                point = createAndWriteLeafNode(newPivotsSet, partitionData);
            }
            childAddress[i] = point;
        }
        InternalNode node = partition.getInstanceOfInternalNode(pivotSet, childAddress);

        return writeInternalNode(node);
    }

    /**
     * 使用全局层次选点来创建索引树。更多请参考{@link HierarchicalPivotSelectionMode}
     *
     * <p>
     * 当{@link HierarchicalPivotSelectionMode}选择LOCAL的时候，建树时会自动调用该方法来构建索引树。
     * </p>
     *
     * @param partition     划分结果
     * @param pivotSet      支撑点集合
     * @param numPartitions 划分数目
     * @return 树的根指针
     * @throws IOException 节点写入错误时抛出
     */
    protected long globalBulkLoad(PartitionResults partition, IndexObject[] pivotSet, int numPartitions) throws IOException
    {
        long[] childAddress = new long[partition.getNumPartition()];
        long   point;
        //遍历划分结果，递归的建树
        for (int i = 0; i < partition.getNumPartition(); i++)
        {
            List<? extends IndexObject> partitionData = partition.getPartitionOf(i);
            if (partitionData.size() > this.maxLeafSize)
            {
                //如果数据量比叶子节点容量大，则继续划分建树
                PartitionResults partitionResults = partition(metric, pivotSet, partitionData, numPartitions);
                point = globalBulkLoad(partitionResults, pivotSet, numPartitions);
            } else
            {
                //数据量比叶子节点容量小了，这个时候创建叶子节点
                point = createAndWriteLeafNode(pivotSet, partitionData);
            }
            childAddress[i] = point;
        }
        InternalNode node = partition.getInstanceOfInternalNode(pivotSet, childAddress);

        return writeInternalNode(node);
    }

    /**
     * 使用局部层次选点来创建索引树。更多请参考{@link HierarchicalPivotSelectionMode}
     *
     * <p>
     * 当{@link HierarchicalPivotSelectionMode}选择LOCAL的时候，建树时会自动调用该方法来构建索引树。
     * </p>
     *
     * @param partition     划分结果
     * @param pivots        支撑点集合
     * @param numPartitions 划分数目
     * @return 树的根指针
     * @throws IOException 节点写入内存失败时抛出
     */
    protected long localBulkLoad(PartitionResults partition, IndexObject[] pivots, int numPartitions) throws IOException
    {
        long[] childAddress = new long[partition.getNumPartition()];
        int[]  newPivots;
        long   point;
        //遍历划分结果，递归的建树
        for (int i = 0; i < partition.getNumPartition(); i++)
        {
            List<? extends IndexObject> partitionData = partition.getPartitionOf(i);
            //选择支撑点
            newPivots = pivotSelection(metric, partitionData, partitionData, this.numPivot);

            IndexObject[] newPivotsSet = new IndexObject[newPivots.length];
            //从数据集中删除被选择的支撑点
            for (int j = 0; j < newPivotsSet.length; j++) newPivotsSet[j] = partitionData.get(newPivots[j]);
            partitionData.removeAll(Arrays.asList(newPivotsSet));
            this.allPivotSet.addAll(Arrays.asList(newPivotsSet));

            if (partitionData.size() > this.maxLeafSize)
            {
                //如果数据量比叶子节点容量大，则继续划分建树
                PartitionResults partitionResults = partition(metric, newPivotsSet, partitionData, numPartitions);
                point = localBulkLoad(partitionResults, newPivotsSet, numPartitions);
            } else
            {
                //数据量比叶子节点容量小了，这个时候创建叶子节点
                point = createAndWriteLeafNode(newPivotsSet, partitionData);
            }
            childAddress[i] = point;
        }
        InternalNode node = partition.getInstanceOfInternalNode(pivots, childAddress);

        return writeInternalNode(node);
    }

    /**
     * 将内部节点写入到磁盘上，并返回内部节点的地址。
     *
     * @param node 要写入的内部节点
     * @return 写入的地址
     * @throws IOException 节点写入错误时抛出
     */
    protected long writeInternalNode(InternalNode node) throws IOException
    {
        long x = oiom.writeObject(node);
        return x;
    }

    /**
     * 创建叶子节点并写入到磁盘上，同时将叶子节点的地址返回。
     *
     * @param pivotsSet 叶子节点的支撑点集合
     * @param data      叶子节点存储的数据
     * @return 写入的地址
     * @throws IOException 节点写入错误时抛出
     */
    public long createAndWriteLeafNode(IndexObject[] pivotsSet, List<? extends IndexObject> data) throws IOException
    {
        //计算数据点到每个支撑点的距离，构建距离表
        double[][] distances = new double[data.size()][pivotsSet.length];
        for (int row = 0; row < data.size(); row++)
        {
            for (int col = 0; col < pivotsSet.length; col++)
            {
                distances[row][col] = this.metric.getDistance(data.get(row), pivotsSet[col]);
            }
        }
        PivotTable pivotTable = new PivotTable(pivotsSet, data.toArray(new IndexObject[0]), data.size(), distances);
        long       p          = oiom.writeObject(pivotTable);
        return p;
    }

    /**
     * 获取建树使用的支撑点数目
     * @return 建树中使用的支撑点数目
     */
    public int getPivotNum()
    {
        return this.allPivotSet.size();
    }

    /**
     * 获取建树使用的支撑点集合
     * @return 返回支撑点集合
     */
    public IndexObject[] getAllPivots() {
        IndexObject[] allPivots = new IndexObject[this.allPivotSet.size()];
        Iterator iterator = this.allPivotSet.iterator();
        for (int i = 0;i < this.allPivotSet.size();i++)
        {
            allPivots[i] = (IndexObject) iterator.next();
        }
        return allPivots;
    }

    /**
     * 抽象的支撑点选择方法,子类应通过实现该方法为建树过程的划分提供支撑点集合,
     * 该方法最终返回int型数组，数组存储从支撑点候选集中选择的支撑点下标。
     *
     * <p>
     * 该方法由AbstractIndex的子类实现。
     * 目前，支撑点选择的模式有三种：LOCAL、GLOBAL和MIX。
     * 在建树过程中，支撑点候选集{@param candidateSet}和支撑点评价集{@param evaluationSet}的差异在于：
     * 1.当使用“LOCAL”模式选择支撑点时，候选集和评价集均为当前划分中的数据；
     * 2.当使用“GLOBAL”模式选择支撑点时，候选集和评价集均为全体数据；
     * 3.当使用“MIX”模式选择支撑点时，候选集{@param candidateSet}为全体数据的抽样，评价集{@param evaluationSet}为当前划分的数据，
     * 候选集由{@link AbstractIndex}的该类的pivotCandidateSet属性提供。
     * </p>
     *
     * @param metric        选择支撑点使用的距离函数
     * @param candidateSet  支撑点候选集
     * @param evaluationSet 支撑点评价集
     * @param numPivot      支撑点数目
     * @return 返回被选择的支撑点在原数据列表data中的下标
     */
    abstract int[] pivotSelection(Metric metric, List<? extends IndexObject> candidateSet, List<? extends IndexObject> evaluationSet, int numPivot);

    /**
     * 构建索引树时使用的划分数据方法。子类需要实现该方法，向{@link AbstractIndex#buildTree()}操作提供数据划分方式。
     *
     * <p>
     * 可以使用系统提供的划分方法{@link PartitionMethod}的//todo 补全
     * </p>
     *
     * @param metric       划分采用的距离函数{@link Metric}
     * @param pivotSet     划分使用的支撑点集合
     * @param data         要划分的数据
     * @param numPartitons 要划分的块数
     * @return 划分结果
     * @see PartitionResults
     */
    abstract PartitionResults partition(Metric metric, IndexObject[] pivotSet, List<? extends IndexObject> data, int numPartitons);

    /**
     * 在MIX建树模式时 初步建树完成后 层次化遍历整棵树
     * 读取所有的支撑点 同时存储 所有叶子节点指针
     * 完成后 对每个叶子节点读出 进而将 数据分为被选为过支撑点的数据 和 普通数据
     * 完成树的清洗 目的 ： 完美解决重复计算和重复添加的问题
     */
    public  void MixLeafNodeClear()
    {
        Cursor.hierarchyTraverseAndClear(indexPrefix, oiom, root);
    }

    /**
     * 返回一个字符串来代表这个对象
     *
     * @return 返回一个字符串来代表这个对象
     */
    @Override
    public String toString()
    {
        StringBuilder toString = new StringBuilder("AbstractIndex{" + "hierarchicalPivotSelectionMode=" + hierarchicalPivotSelectionMode + ", root=" + root + ", totalSize=" + totalSize + ", maxLeafSize=" + maxLeafSize + ", metric=" + metric + ", indexPrefix='" + indexPrefix + '\'' + ", numPivot=" + numPivot + ", numPartitions=" + numPartitions);
        if (hierarchicalPivotSelectionMode == HierarchicalPivotSelectionMode.MIX)
        {
            toString.append(", pivotCandidateSetSize=" + pivotCandidateSetSize);
        }
        return toString.append("}").toString();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        initOIOM(true);
        openOIOM();
    }
    /**
     * 遍历该索引树并输出到磁盘上
     *
     * @param dir 输出文件的保存目录
     */
    public void outputIndexTree(String dir)
    {
        if (!(dir.endsWith(File.separator))) dir = dir + "/";
        File file = new File(dir + indexPrefix);
        if (!(file.exists())) {
            file.mkdirs();
        }
        Cursor.hierarchyTraverseAndSave(dir + indexPrefix + "/" + indexPrefix, oiom, root, metric);
    }

    /**
     * 获取根节点的划分的r-Neighborhood中的点的数目
     *
     * @param radius r-Neighborhood的大小
     * @return 根节点的划分的r-Neighborhood中的点的数目
     */
    @Override
    public int getNumberOfPointInrNeighborhoodForRootLevel(double radius)
    {
        return this.getCursor().getNumberOfPointInrNeighborhoodForRootLevel(radius);
    }

    /**
     * 返回一个对应索引树类型的Cursor
     * @return 对应索引树类型的Cursor
     */
    public abstract Cursor getCursor();
}
