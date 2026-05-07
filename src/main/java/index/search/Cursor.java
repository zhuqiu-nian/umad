package index.search;

import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.structure.InternalNode;
import index.structure.LeafNode;
import index.structure.Node;
import index.structure.PivotTable;
import manager.MckoiObjectIOManager;
import manager.ObjectIOManager;
import metric.CountedMetric;
import metric.Metric;

import java.io.*;
import java.util.*;

/**
 * 实现了{@link Iterator}，可以在结果集中迭代。
 *
 * <p>
 * 该类是所有搜索操作的超类。对于不同类型的索引树的搜索，只需要实现方法{@link  Cursor#inflateResult()}。在该方法中填充结果集{@code result}。
 * </P>
 */
public abstract class Cursor implements Iterator
{
    //层次遍历的全局计数变量
    private static int counter;
    //搜索过程的距离计算计数
    protected      int disCounter;
    ObjectIOManager                   oiom;
    Metric                            metric;
    long                              rootAddress;
    LinkedList<DoubleIndexObjectPair> result;
    //标记result结果集是否被填充过
    private boolean inflated = false;

    //用到的统计量
    /**
     * 本次搜索的叶子节点中的线性扫描次数
     */
    public int leaf_time;
    /**
     * 本次搜索的平均排除率等于单次内部节点搜索排除率P=本次内部节点搜索可以排除的点的数目/该内部节点存储的总的数据量，累加取平均值
     */
    public double averageExclusionRate = 0;
    /**
     * 本次搜索的内部节点搜索次数
     */
    public int numberOfInternalNodeSearches = 0;
    /**
     * 本次搜索的叶子节点的搜索次数
     */
    public int numberOfLeafNodeSearches = 0;
    /**
     * 本次搜索的所有类型节点的搜索次数= 本次搜索的内部节点搜索次数 + 本次搜索的叶子节点的搜索次数
     */
    public int    numberOfNodeSearches = 0;
    /**
     * 本次搜索的时间，单位ns
     */
    public long   nsOfSearchTime       = 0;
    /**
     * 本次搜索的平均剪枝率等于单次内部节点搜索的剪枝率P=本次内部节点搜索可以排除的分支数/该内部节点总的分支数，累加取平均值
     */
    public double averagePruneRate     = 0;

    /**
     * 查询类{@link Cursor}的构造函数
     *
     * @param oiom        io读写对象
     * @param metric      搜索使用的距离函数
     * @param rootAddress 要搜索的索引树的根节点
     */
    public Cursor(ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        this.oiom        = oiom;
        this.metric      = metric;
        this.rootAddress = rootAddress;
        result           = new LinkedList<DoubleIndexObjectPair>();
    }

    /**
     * 返回以地址为根的子树中所有数据点的列表,该方法不会自动解压数据，如果数据被压缩过那么返回之后要调用{@link Cursor#unzipTheData(List)}解压返回的数据。
     * Return a list of all the data points in the subtree rooted at the address
     *
     * @param oiom            数据对象
     * @param address         要查询的子树的根节点
     * @param memoryHashTable 记忆已经在结果集中的支撑点
     * @return each data point is an IndexObject, consist of a row ID and an index key
     *         每个数据点都是一个{@link IndexObject}，由一个行ID和一个索引键组成
     */
    public static List<IndexObject> getAllCompressedPoints(ObjectIOManager oiom, long address, Hashtable<IndexObject, Double> memoryHashTable)
    {
        Node node = null;
        try
        {
            node = (Node) oiom.readObject(address);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        LinkedList<IndexObject> result = new LinkedList<>();
        for (int pivot = 0; pivot < node.getNumPivots(); pivot++)
        {
            IndexObject p = node.getPivotOf(pivot);
            //如果哈希表里没有这个点，证明这个点没有被添加过
            if (!(memoryHashTable.containsKey(p)))
            {
                memoryHashTable.put(p, -1.0);
                result.add(p);
            }
        }

        if (node instanceof InternalNode)
        {
            InternalNode iNode = (InternalNode) node;
            for (int child = 0; child < iNode.getNumChildren(); child++)
                 result.addAll(getAllCompressedPoints(oiom, iNode.getChildOf(child), memoryHashTable));
        } else if (node instanceof LeafNode)
        {
            LeafNode lNode = (LeafNode) node;
            for (int child = 0; child < lNode.getDataSize(); child++)
            {
                result.add(lNode.getDataOf(child));
            }
        } else
        {
            throw new UnsupportedOperationException("Node db: " + node.getClass() + " is not supported!");
        }
        return result;
    }

    /**
     * 返回以地址为根的子树中的节点总数
     * Return the total number of nodes in the subtree rooted at the address
     *
     * @param oiom    数据对象
     * @param address 要查询的子树的根节点
     * @return 返回以地址为根的子树中的节点总数
     */
    public static int getNodeNumber(ObjectIOManager oiom, long address)
    {
        int  result = 1;
        Node node   = null;
        try
        {
            node = (Node) oiom.readObject(address);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        if (node instanceof InternalNode)
        {
            InternalNode iNode = (InternalNode) node;
            for (int child = 0; child < iNode.getNumChildren(); child++)
                 result += getNodeNumber(oiom, iNode.getChildOf(child));
        }

        return result;
    }

    /**
     * 返回以地址为根的子树中叶索引节点的总数
     * Return the total number of leaf index node in the subtree rooted at the address
     *
     * @param oiom    数据对象
     * @param address 要查询的子树的根节点
     * @return 返回以地址为根的子树中叶索引节点的总数
     */
    public static int getLeafNodeNumber(ObjectIOManager oiom, long address)
    {
        int  result = 0;
        Node node   = null;
        try
        {
            node = (Node) oiom.readObject(address);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        if (node instanceof InternalNode)
        {
            InternalNode iNode = (InternalNode) node;
            for (int child = 0; child < iNode.getNumChildren(); child++)
                 result += getLeafNodeNumber(oiom, iNode.getChildOf(child));
        } else if (node instanceof LeafNode)
        {
            return 1;
        } else
        {
            throw new UnsupportedOperationException("Node db: " + node.getClass() + " is not supported!");
        }
        return result;
    }

    /**
     * 在MIX建树模式时 初步建树完成后 层次化遍历整棵树
     * 读取所有的支撑点 同时存储 所有叶子节点指针
     * 完成后 对每个叶子节点读出 进而将 数据分为被选为过支撑点的数据 和 普通数据
     * 完成树的清洗 目的 ： 完美解决重复计算和重复添加的问题
     * @param filePrefix 输出文件的前缀
     * @param oiom       读写io对象
     * @param address    索引树的根节点
     */
    public static void hierarchyTraverseAndClear(String filePrefix, ObjectIOManager oiom, long address)
    {
        //层次遍历需要的队列
        ArrayDeque<Long> deque       = new ArrayDeque<>();

        //存储所有叶子节点需要的队列
        ArrayDeque<Long> dequeLeaf       = new ArrayDeque<>();

        //存储所有的支撑点
        Set pivotData = new HashSet();

        int              numChildren = 0;
        long             point;
        //把根节点压入队列
        deque.addLast(address);

        //开始遍历 获取所有的支撑点 以及 存储所有的叶子节点指针
        while (!(deque.isEmpty()))
        {
            point = deque.pop();
            try {
                Object o = oiom.readObject(point);
                if (o instanceof InternalNode)
                {
                    numChildren = ((InternalNode) o).getNumChildren();
                    pivotData.addAll(Arrays.asList(((InternalNode) o).getAllPivots()));
                    for (int i = 0; i < numChildren; i++)
                    {
                        deque.addLast(((InternalNode) o).getChildOf(i));
                    }

                } else if (o instanceof LeafNode)
                {
                    pivotData.addAll(Arrays.asList(((LeafNode) o).getAllPivots()));
                    dequeLeaf.add(point);
                } else
                {
                    throw new RuntimeException("层次遍历时传入的不是节点类型");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        //使用可写的oiom对象覆写
        ObjectIOManager tempOiom = new MckoiObjectIOManager(filePrefix, "000", 1024 * 1024 * 1024, "Java IO", 4, 128 * 1024, false);
        tempOiom.open();

        //根据支撑点 将每个叶子节点数据做分类
        while (!(dequeLeaf.isEmpty())) {

            //普通数据距离 同时作为支撑点的数据距离

            int dataPoint;
            point = dequeLeaf.pop();
            boolean isPivot[];
            try {
                Object o = oiom.readObject(point);
                isPivot = new boolean[((PivotTable)o).getDataSize()];
                var data = ((PivotTable)o).getAllData();
                dataPoint = 0;
                //对一组节点内数据划分 以及对距离数据
                for (var temp:data)
                {
                    if (pivotData.contains(temp))
                    {
                        isPivot[dataPoint] = true;
                    }
                    dataPoint++;
                }

                ((PivotTable)o).setIsPivotData(isPivot);
                tempOiom.writeObject(o, point);


            } catch (IOException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        tempOiom.close();


    }

    /**
     * 层次遍历索引树，并把每个内部节点的划分结果和支撑点输出，同时输出数据在支撑点空间中的坐标。
     *
     * @param filePrefix 输出文件的前缀
     * @param oiom       读写io对象
     * @param address    要层次遍历的索引树的根节点
     * @param metric     建树时使用的距离函数
     */
    public static void hierarchyTraverseAndSave(String filePrefix, ObjectIOManager oiom, long address, Metric metric)
    {
        //层次遍历需要的队列
        ArrayDeque<Long> deque       = new ArrayDeque<>();
        int              numChildren = 0;
        long             point;
        //把根节点压入队列
        deque.addLast(address);

        //开始遍历
        while (!(deque.isEmpty()))
        {
            point = deque.pop();
            try
            {
                Object o = oiom.readObject(point);
                if (o instanceof InternalNode)
                {
                    visitInternalNode(filePrefix, oiom, (InternalNode) o, metric);
                    numChildren = ((InternalNode) o).getNumChildren();
                    for (int i = 0; i < numChildren; i++)
                        deque.addLast(((InternalNode) o).getChildOf(i));
                } else if (o instanceof LeafNode)
                {
                    visitLeafNode(filePrefix, (LeafNode) o, metric);
                } else
                {
                    throw new RuntimeException("层次遍历时传入的不是节点类型");
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            } catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            } catch (InstantiationException e)
            {
                e.printStackTrace();
            } catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }

        }

        //计数辅助置0
        counter = 0;
    }

    /**
     * 遍历叶子节点并输出其中的支撑点，源度量空间数据，使用支撑点映射了之后的向量空间数据
     *
     * @param filePrefix 输出文件的前缀
     * @param node       要遍历的节点
     * @param metric     建树时使用的距离函数
     */
    private static void visitLeafNode(String filePrefix, LeafNode node, Metric metric)
    {
        if (node.getNumPivots() == 0) return;

        //构建输出文件
        try (FileOutputStream pivotOutputStream = new FileOutputStream(new File(filePrefix + "_" + counter + "pivot" + ".txt"));
             OutputStreamWriter pivotOutputWriter = new OutputStreamWriter(pivotOutputStream);
             FileOutputStream psDataOutputStream = new FileOutputStream(new File(filePrefix + "_" + counter + "psData" + ".txt"));
             OutputStreamWriter psDataOutputWriter = new OutputStreamWriter(psDataOutputStream);
             FileOutputStream msDataOutputStream = new FileOutputStream(new File(filePrefix + "_" + counter + "msData" + ".txt"));
             OutputStreamWriter msDataOutputWriter = new OutputStreamWriter(msDataOutputStream))
        {
            int numPivots = node.getNumPivots();

            //输出支撑点
            pivotOutputWriter.write(numPivots + "\n");
            for (int i = 0; i < numPivots; i++)
                 pivotOutputWriter.write(node.getPivotOf(i).toString() + "\n");

            //输出度量空间数据和向量空间数据
            //遍历度量空间数据输出
            msDataOutputWriter.write(node.getPivotOf(0).size() + "\t" + node.getDataSize() + "\n");
            psDataOutputWriter.write(node.getNumPivots() + "\t" + node.getDataSize() + "\n");
            for (int dataIndex = 0; dataIndex < node.getDataSize(); dataIndex++)
            {
                msDataOutputWriter.write(node.getDataOf(dataIndex).toString() + "\n");
                for (int pivotIndex = 0; pivotIndex < numPivots; pivotIndex++)
                {
                    psDataOutputWriter.write(metric.getDistance(node.getDataOf(dataIndex), node.getPivotOf(pivotIndex)) + "\t");
                }
                psDataOutputWriter.write("\n");
            }
            //计数器加1
            counter++;
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 遍历内部节点并输出其中的支撑点，源度量空间数据，使用支撑点映射了之后的向量空间数据
     *
     * @param filePrefix 输出文件的前缀
     * @param oiom       读写IO对象
     * @param node       要遍历的节点
     * @param metric     建树时使用的距离函数
     */
    private static void visitInternalNode(String filePrefix, ObjectIOManager oiom, InternalNode node, Metric metric)
    {
        if (node.getNumPivots() == 0) return;

        //构建输出文件
        try (FileOutputStream pivotOutputStream = new FileOutputStream(new File(filePrefix + "_" + counter + "pivot" + ".txt"));
             OutputStreamWriter pivotOutputWriter = new OutputStreamWriter(pivotOutputStream);
             FileOutputStream psDataOutputStream = new FileOutputStream(new File(filePrefix + "_" + counter + "psData" + ".txt"));
             OutputStreamWriter psDataOutputWriter = new OutputStreamWriter(psDataOutputStream);
             FileOutputStream msDataOutputStream = new FileOutputStream(new File(filePrefix + "_" + counter + "msData" + ".txt"));
             OutputStreamWriter msDataOutputWriter = new OutputStreamWriter(msDataOutputStream);)
        {
            int numPivots   = node.getNumPivots();
            int numChildren = node.getNumChildren();

            //输出支撑点
            pivotOutputWriter.write(numPivots + "\n");
            for (int i = 0; i < numPivots; i++)
                 pivotOutputWriter.write(node.getPivotOf(i).toString() + "\n");

            //输出度量空间数据和向量空间数据
            //遍历每个孩子
            long                        point;
            List<? extends IndexObject> data;
            msDataOutputWriter.write(node.getPivotOf(0).size() + "\t" + node.getDataSize() + "\n");
            psDataOutputWriter.write(node.getNumPivots() + "\t" + node.getDataSize() + "\n");
            for (int childIndex = 0; childIndex < numChildren; childIndex++)
            {
                point = node.getChildOf(childIndex);
                data  = getAllCompressedPoints(oiom, point, new Hashtable<>());
                //遍历数据输出
                for (var d : data)
                {
                    msDataOutputWriter.write(d.toString() + "\t" + childIndex + "\n");
                    for (int pivotIndex = 0; pivotIndex < numPivots; pivotIndex++)
                    {
                        psDataOutputWriter.write(metric.getDistance(d, node.getPivotOf(pivotIndex)) + "\t");
                    }
                    psDataOutputWriter.write(childIndex + "\n");
                }
            }
            //计数器加1
            counter++;
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 测试是否还有更多封装过的数据对象
     *
     * @return 如果{@code Cursor}有更多元素，则返回true。
     */
    public boolean hasNext()
    {
        //搜索并填充结果集
        if (!inflated)
        {
            inflateResult();
            inflated = true;
        }

        return result.size() > 0;
    }

    /**
     * 获取下一个封装过的数据对象
     *
     * @return 返回下一个封装过的数据对象
     */
    public DoubleIndexObjectPair next()
    {
        if (result.size() > 0)
            return result.remove();
        else
        {
            throw new NoSuchElementException("The result was already returned!");
        }
    }

    /**
     * 解压传入的{@link DoubleIndexObjectPair}结果列表中的数据。
     * @param data 待解压的数据列表
     * @return 解压后的{@link DoubleIndexObjectPair}结果列表
     */
    public static List<DoubleIndexObjectPair> unzipTheResult(List<DoubleIndexObjectPair> data){
        List<DoubleIndexObjectPair> unzipList = new LinkedList<>();
        for (var dioPair : data){
            IndexObject           iObject = dioPair.getObject();
            if (iObject instanceof IndexObject)
            {
                double        _double  = dioPair.getDouble();
                IndexObject[] cioArray = (dioPair.getObject()).expand();
                for (int i = 0; i < cioArray.length; i++)
                {
                    unzipList.add(new DoubleIndexObjectPair(_double, cioArray[i]));
                }
            }
        }
        return unzipList;
    }

    /**
     * 解压传入的{@link IndexObject}列表中的数据。
     * @param data 待解压的数据列表
     * @return 解压后的{@link IndexObject}列表中的数据。
     */
    public static List<IndexObject> unzipTheData(List<IndexObject> data){
        List<IndexObject> unzipList = new LinkedList<>();
        for (var iObject : data){
            if (iObject instanceof IndexObject)
            {
                IndexObject[] cioArray = iObject.expand();
                for (int i = 0; i < cioArray.length; i++)
                {
                    unzipList.add(cioArray[i]);
                }
            }
        }
        return unzipList;
    }

    /**
     * 该函数用于填充result结果集。所有的索引树类型要实现自己的搜索都要实现该抽象函数。注意返回的结果要处理解压缩问题
     *
     * <p>
     * 在该抽象函数中，填充结果集result。
     * </P>
     */
    protected abstract void inflateResult();

    /**
     * 获取搜索结果中剩余的结果数目
     *
     * @return 返回搜索结果中剩余的结果数目
     */
    public int remainingSizeOfTheResult()
    {
        //搜索并填充结果集
        if (!inflated)
        {
            inflateResult();
            inflated = true;
        }
        return result.size();
    }

    /**
     * 返回搜索过程的距离计算次数
     *
     * @return 搜索过程距离计算次数
     */
    public int getDisCounter()
    {
        if (this.metric instanceof CountedMetric)
        {
            if (this.disCounter == 0){
                inflateResult();
                inflated=true;
            }
            return this.disCounter;
        } else
            throw new IllegalArgumentException("请将距离函数改成可以计数的距离函数");
    }

    /**
     * 获取根节点的划分的r-Neighborhood中的点的数目
     * @param radius r-Neighborhood的大小
     * @return 根节点的划分的r-Neighborhood中的点的数目
     */
    public abstract int getNumberOfPointInrNeighborhoodForRootLevel(double radius);
}
