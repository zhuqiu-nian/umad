package index.search;

import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.structure.InternalNode;
import index.structure.LeafNode;
import index.structure.Node;
import index.structure.PivotTable;
import index.type.NodeSearchAction;
import manager.ObjectIOManager;
import metric.CountedMetric;
import metric.Metric;
import util.Debug;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

/**
 * 范围搜索的抽象类，对于每一种索引类型要实现搜索操作都要实现该类。
 *
 * <p>
 * 对于每一种索引树类型都要有对应的{@link RangeCursor}子类，在子类中实现
 * {@link RangeCursor#willTheSubTreeFurtherSearch(Node, Metric, IndexObject, double, double[])}
 * 方法，该方法返回{@code NodeSearchAction}数组，该数组标记了对应数组下标的每个扇出的处理方式，向搜索过程提供待
 * 搜索节点的每个扇出的处理方式。
 * 例如VPRangeCursor该方法的实现如下：
 * </p>
 * <pre>
 *     public NodeSearchAction[] willTheSubTreeFurtherSearch(Node node, Metric metric, IndexObject query, double radius, double[] queryToPivotDistance, Hashtable&lt;IndexObject, Double&gt; memoryHashTable)
 *     {
 *         VPInternalNode aNode = (VPInternalNode) node;
 *         //初始化标记数组
 *         NodeSearchAction[] nodeSearchActions = new NodeSearchAction[aNode.getNumChildren()];
 *         Arrays.fill(nodeSearchActions, NodeSearchAction.RESULTUNKNOWN);
 *
 *         //逐个判断每个扇出的处理方法
 *         for (int i = 0; i &lt; aNode.getNumChildren(); i++)
 *         {
 *             //拿到该孩子内存储的数据到每个支撑点距离的上下界限
 *             double[][] range = aNode.getChildPredicate(i);
 *
 *             //遍历每个支撑点，根据查询对象到支撑点的距离、搜索半径以及孩子的上下界标记孩子的状态
 *             for(int j = 0; j &lt; aNode.getNumPivots(); j++)
 *             {
 *                 //当一个支撑点满足 dis(q, p_j) + 第i个扇出的数据到第j个支撑点的上界 &lt;= 查询半径， 那么第i个扇出的所有数据都在查询范围内
 *                 if(range[1][j] + queryToPivotDistance[j] &lt;= radius)
 *                 {
 *                     nodeSearchActions[i] = NodeSearchAction.RESULTALL;
 *                     break;
 *                 }
 *
 *                 //当一个支撑点满足 dis(q, p_j) + 查询半径 &lt; 第i个扇出的数据到第j个支撑点的下界， 那么第i个扇出的所有数据都不在查询范围内
 *                 //当一个支撑点满足 dis(q, p_j) - 查询半径 &gt; 第i个扇出的数据到第j个支撑点的上界， 那么第i个扇出的所有数据都不在查询范围内
 *                 if(queryToPivotDistance[j] + radius &lt; range[0][j] || queryToPivotDistance[j] - radius &gt; range[1][j])
 *                 {
 *                     nodeSearchActions[i] = NodeSearchAction.RESULTNONE;
 *                     break;
 *                 }
 *             }
 *         }
 *         return nodeSearchActions;
 *     }
 *     </pre>
 */
public abstract class RangeCursor extends Cursor
{

    IndexObject q = null; //范围搜索对象
    double      radius; // 搜索半径

    // RGH索引专用：缓存子节点pivot距离 (key: IndexObject, value: 距离)
    // 使用Hashtable以兼容其他索引类型（如果不用则为null）
    protected java.util.Hashtable<IndexObject, Double> distanceCache = null;

    /**
     * 获取距离缓存（供RGH索引使用）
     */
    protected java.util.Hashtable<IndexObject, Double> getDistanceCache()
    {
        return distanceCache;
    }

    /**
     * 设置距离缓存（供RGH索引使用）
     */
    protected void setDistanceCache(java.util.Hashtable<IndexObject, Double> cache)
    {
        this.distanceCache = cache;
    }

    /**
     * 范围查询构造函数
     *
     * @param oiom        io读写对象
     * @param metric      搜索使用的距离函数
     * @param rootAddress 要搜索的索引树的根节点
     */
    public RangeCursor(ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(oiom, metric, rootAddress);
    }

    /**
     * 范围查询构造函数
     *
     * @param query       查询对象
     * @param oiom        io读写对象
     * @param metric      搜索使用的距离函数
     * @param rootAddress 要搜索的索引树的根节点
     */
    public RangeCursor(RangeQuery query, ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(oiom, metric, rootAddress);
        this.q      = query.getQueryObject();
        this.radius = query.getRadius();
        this.leaf_time = 0;
    }

    /**
     * 当对内部节点进行搜索时使用该方法。
     *
     * <p>
     * 该方法输入{@link Node}类型的内部节点，返回以该结果为根的在查询范围内的结果，返回的结果为{@link DoubleIndexObjectPair}类型的LinkedList。
     * 具体查询流程如下：
     * step 1. 计算查询对象与每个支撑点的距离，并将在查询范围内的支撑点加入到结果集中；
     * step 2. 调用{@link RangeCursor#willTheSubTreeFurtherSearch(Node, Metric, IndexObject, double, double[])}判断该内部节点的孩子节点
     * 是否在查询范围内，若在在查询范围内，则递归搜索以该节点为根的索引树，并将搜索结果返回。
     * </p>
     *
     * @param node 需要搜索的节点
     * @param memoryHashTable 记忆已经在结果集中的支撑点,同时记忆查询点到支撑点的距离，避免每层支撑点重复的时候重复添加计算
     * @return 在查询范围内的结果
     */
    public LinkedList<DoubleIndexObjectPair> internalSearch(Node node, Hashtable<IndexObject, Double> memoryHashTable)
    {
        if (node instanceof LeafNode)
        {
            //在叶子节点中执行范围搜索
            return leafSearch(node, memoryHashTable);
        }
        InternalNode aNode = (InternalNode) node;
        this.numberOfInternalNodeSearches++;

        //计算排除率的辅助变量
        int dataVolumeOfInternalNodes = aNode.getDataSize();

        //定义结果集
        LinkedList<DoubleIndexObjectPair> currentResult = new LinkedList<>();

        //判断支撑点是否在查询范围内
        double[] queryToPivotDistance = new double[aNode.getNumPivots()];

        for (int i = 0; i < aNode.getNumPivots(); i++)
        {
            //拿到支撑点
            IndexObject pivot = aNode.getPivotOf(i);
            //如果这个点的距离已经计算过则直接拿，而且是结果就已经添加过了，所以不用重新判断添加了
            //但是注意！如果willTheSubTreeFurtherSearch中有计算操作的话，此处会忽视！需要在willTheSubTreeFurtherSearch中进行判断是否已经计算过并加入结果集！ --- IGNORE ---
            if (memoryHashTable.containsKey(pivot)) {
                queryToPivotDistance[i] = memoryHashTable.get(pivot);
            } else {
                //没有计算过，则要重新计算，并添加到记忆哈希表中
                queryToPivotDistance[i] = metric.getDistance(q, pivot);
                memoryHashTable.put(pivot, queryToPivotDistance[i]);
                if (queryToPivotDistance[i] <= radius)
                {
                    //如果支撑点的距离也在范围查找内，则添加到结果集中
                    currentResult.add(new DoubleIndexObjectPair(queryToPivotDistance[i], pivot));
                }
            }
        }

        //递归搜索在查询范围内的孩子节点，并返回结果集
        NodeSearchAction[] processingMethods = willTheSubTreeFurtherSearch(aNode, metric, q, radius, queryToPivotDistance, memoryHashTable, currentResult);

        //计算排除率的辅助变量
        int numberOfPointsThatCannotBeExcluded = 0;
        int childToSearchNum = 0;
        for (int i = 0; i < processingMethods.length; i++)
        {
            try
            {
                if (processingMethods[i] == NodeSearchAction.RESULTUNKNOWN)
                {
                    childToSearchNum++;
                    Object toSearchNode = oiom.readObject(aNode.getChildOf(i));
                    numberOfPointsThatCannotBeExcluded += ((Node)toSearchNode).getDataSize();
                    currentResult.addAll(internalSearch((Node) toSearchNode, memoryHashTable));
                } else if (processingMethods[i] == NodeSearchAction.RESULTALL)
                {
                    List<IndexObject> allResult = getAllCompressedPoints(this.oiom, aNode.getChildOf(i), memoryHashTable);
                    allResult.forEach(indexObject -> currentResult.add(new DoubleIndexObjectPair(-1, indexObject)));
                }else if(processingMethods[i] == NodeSearchAction.RESULTNEEDLINERSCAN){
                    //进行线性扫描一遍，然后扫描到的结果添加到结果集中
                    List<IndexObject> allDataList = getAllCompressedPoints(this.oiom, aNode.getChildOf(i), memoryHashTable);
                    allDataList.forEach(indexObject -> {
                        double dis;
                        if (!memoryHashTable.containsKey(indexObject) || memoryHashTable.get(indexObject)==-1){
                            dis = metric.getDistance(q, indexObject);
                        }else{
                            dis = memoryHashTable.get(indexObject);
                        }
                        if(dis <= radius){
                            currentResult.add(new DoubleIndexObjectPair(dis, indexObject));
                        }
                    });
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        //排除率累加
        this.averageExclusionRate += 1 - numberOfPointsThatCannotBeExcluded / (double)dataVolumeOfInternalNodes;

        this.averagePruneRate += 1 - childToSearchNum /(double)((InternalNode) node).getNumChildren();

        return currentResult;
    }

    /**
     * 当对叶子节点进行搜索时调用该方法。
     *
     * <p>
     * 该方法输入{@link Node}类型的叶子节点，返回在查询范围内的节点，返回的结果为{@link DoubleIndexObjectPair}类型的LinkedList。
     * 具体查询流程如下：
     * setp 1. 计算查询对象与每个支撑点的距离，并将在查询范围内的支撑点加入到结果集中；
     * step 2. 使用三角不等式对叶子节点中的数据点进行排除。
     * </p>
     *
     * @param node 需要搜索的节点
     * @param memoryHashTable 记忆已经在结果集中的支撑点,同时记忆查询点到支撑点的距离，避免每层支撑点重复的时候重复添加计算
     * @return 在查询范围内的结果
     */
    public LinkedList<DoubleIndexObjectPair> leafSearch(Node node, Hashtable<IndexObject, Double> memoryHashTable)
    {
        //判断当前节点是否是叶子节点，如果不是便抛出异常
        if (!(node instanceof LeafNode))
        {
            throw new RuntimeException("leafSearch函数传入的节点不是叶子节点");
        }

        this.numberOfLeafNodeSearches++;

        PivotTable aNode = (PivotTable) node;

        LinkedList<DoubleIndexObjectPair> currentResult = new LinkedList<DoubleIndexObjectPair>();

        //判断支撑点是否在查询范围内，如果在便加入到结果集中
        double[] dis = new double[aNode.getNumPivots()];
        for (int i = 0; i < aNode.getNumPivots(); i++)
        {
            //拿到支撑点
            IndexObject pivot = aNode.getPivotOf(i);
            //如果这个点的距离已经计算过则直接拿，而且是结果就已经添加过了，所以不用重新判断添加了
            if (memoryHashTable.containsKey(pivot)) {
                dis[i] = memoryHashTable.get(pivot);
            } else {
                //没有计算过，则要重新计算，并添加到记忆哈希表中
                dis[i] = metric.getDistance(q, pivot);
                memoryHashTable.put(pivot, dis[i]);
                if (dis[i] <= radius)
                {
                    //如果支撑点的距离也在范围查找内，则添加到结果集中
                    currentResult.add(new DoubleIndexObjectPair(dis[i], pivot));
                }
            }
        }
        //使用tempResultFlag数组标记其对应下标的数据是否在结果集中
        boolean[] tempResultFlag = new boolean[aNode.getDataSize()];
        Arrays.fill(tempResultFlag, true);
        //在PivotTable中通过三角不等式排除不在查询范围内的节点
        for (int i = 0; i < aNode.getDataSize(); i++)
        {
            double[] dataToPivotDis = aNode.getDataPoint2PivotDistance(i);
            for (int j = 0; j < aNode.getNumPivots(); j++)
            {

                //|d(p_j,q)-d(p_j,x_i)|>r，则从结果集中移除，即标记为false
                if (Math.abs(dis[j] - dataToPivotDis[j]) > radius)
                {
                    tempResultFlag[i] = false;
                    break;
                }
            }
        }
        var isPivot = aNode.getIsPivotData();
        //根据tempResultFlag数组的标记情况组成结果集,同时对标记为true的数据进行了线性扫描，将在查询范围内的数据添加到结果集中
        double d = 0;
        for (int i = 0; i < aNode.getDataSize(); i++)
        {
            if (tempResultFlag[i])
            {
                this.leaf_time++;
                //如果是同时被选为支撑点的数据
                if (isPivot[i]){
                    //如果已经计算过则已经判断结果并处理了 则直接跳过
                    if (memoryHashTable.containsKey(aNode.getDataOf(i))) {
                        continue;
                    } else {
                        //否则加入记录 并进一步处理
                        d = metric.getDistance(q, aNode.getDataOf(i));
                        memoryHashTable.put(aNode.getDataOf(i), d);
                    }
                }else {
                    //若是普通数据则正常操作即可
                    d = metric.getDistance(q, aNode.getDataOf(i));
                }
                if ((d <= radius))
                {
                    currentResult.add(new DoubleIndexObjectPair(d, aNode.getDataOf(i)));
                }
            }
        }
        return currentResult;
    }

    /**
     * 该函数用于填充result结果集。
     *
     * <p>
     * 在该抽象函数中，填充结果集result。
     * </P>
     */
    @Override
    public void inflateResult()
    {
        double startTime, endTime, searchTime;
        //开始计时
//        startTime = System.currentTimeMillis();
        startTime = System.nanoTime();
        Object node = null;

        //如果传入的是可计数的metric，就清空计数
        if (this.metric instanceof CountedMetric) ((CountedMetric) this.metric).clear();

        try
        {
            node = oiom.readObject(rootAddress);
            //数据默认压缩，所以在解压之前不可能有数据是重复的，为了解决各种支撑点重复的问题，在解压前进行去重
            if (node instanceof InternalNode)
                this.result = new LinkedList<>(Cursor.unzipTheResult(internalSearch((Node) node, new Hashtable<>())));
            else if (node instanceof LeafNode)
                this.result = new LinkedList<>(Cursor.unzipTheResult(leafSearch((Node) node, new Hashtable<>())));
            else
                throw new RuntimeException("inflateResult()时传入的节点为空");
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        //计算统计量
        this.averageExclusionRate /= this.numberOfInternalNodeSearches;
        this.averagePruneRate /= this.numberOfInternalNodeSearches;
        this.numberOfNodeSearches = this.numberOfInternalNodeSearches + this.numberOfLeafNodeSearches;
        //结束计时
        endTime   = System.nanoTime();
        this.nsOfSearchTime += (endTime-startTime);
        searchTime = (endTime - startTime) / 1000.00;
        if (this.metric instanceof CountedMetric) this.disCounter = ((CountedMetric) this.metric).getCounter();
        if (Debug.debug) {
            if (metric instanceof CountedMetric) System.out.println("本次搜索的距离计算次数是：" + this.disCounter);
            System.out.println("本次搜索操作耗时：" + searchTime +"s");
        }
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
        //基本思路是使用willTheSubTreeFurtherSearch方法判断索引中存储的全部的点，如果返回的处理动作中只对一个动作进行搜索，那么其就不在r邻域中
        var data = getAllCompressedPoints(this.oiom, this.rootAddress, new Hashtable<>());
        int counter = 0;
        try
        {
            Node node = (Node) this.oiom.readObject(this.rootAddress);
            NodeSearchAction[] actions;
            double[] qTop = new double[node.getNumPivots()];
            int flag=0;
            for (var d : data){
                //计算d到支撑点的距离
                for (int i=0; i<node.getNumPivots(); i++) qTop[i] = this.metric.getDistance(d,node.getPivotOf(i));
                actions = willTheSubTreeFurtherSearch(node, this.metric, d, radius, qTop, new Hashtable<>(), new LinkedList<>());
                flag=0;
                for (var a : actions){
                    if (a == NodeSearchAction.RESULTUNKNOWN) flag++;
                }
                if (flag > 1) counter++;
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
        return counter;
    }

    /**
     * {@link RangeCursor#internalSearch(Node, Hashtable)}调用该方法获取对每个孩子的处理方法。
     *
     * <p>
     * 不同种类的索引树实现该方法，通过枚举类{@link NodeSearchAction}标记{@link Node}的每个子节点的处理方法。
     * 如果{@code NodeSearchAction[i] == RESULTNONE}表示第i个孩子节点被剪枝，稍后的搜索不进入该孩子。
     * 如果{@code NodeSearchAction[i] == RESULTUNKNOWN}表示第i个孩子节点不能排除，稍后的搜索进入该孩子。
     * 如果{@code NodeSearchAction[i] == RESULTALL}表示第i个孩子节点的所有数据都应该被添加到结果集中。
     * </p>
     *
     * <pre>
     *      public NodeSearchAction[] willTheSubTreeFurtherSearch(Node node, Metric metric, IndexObject query, double radius, double[] queryToPivotDistance, Hashtable<IndexObject, Double> memoryHashTable)
     *      {
     *         VPInternalNode aNode = (VPInternalNode) node;
     *         //初始化标记数组
     *         NodeSearchAction[] nodeSearchActions = new NodeSearchAction[aNode.getNumChildren()];
     *         Arrays.fill(nodeSearchActions, NodeSearchAction.RESULTUNKNOWN);
     *
     *         //逐个判断每个扇出的处理方法
     *         for (int i = 0; i &lt; aNode.getNumChildren(); i++)
     *         {
     *             //拿到该孩子内存储的数据到每个支撑点距离的上下界限
     *             double[][] range = aNode.getChildPredicate(i);
     *
     *             //遍历每个支撑点，根据查询对象到支撑点的距离、搜索半径以及孩子的上下界标记孩子的状态
     *             for(int j = 0; j &lt; aNode.getNumPivots(); j++)
     *             {
     *                 //当一个支撑点满足 dis(q, p_j) + 第i个扇出的数据到第j个支撑点的上界 &lt;= 查询半径， 那么第i个扇出的所有数据都在查询范围内
     *                 if(range[1][j] + queryToPivotDistance[j] &lt;= radius)
     *                 {
     *                     nodeSearchActions[i] = NodeSearchAction.RESULTALL;
     *                     break;
     *                 }
     *
     *                 //当一个支撑点满足 dis(q, p_j) + 查询半径 &lt; 第i个扇出的数据到第j个支撑点的下界， 那么第i个扇出的所有数据都不在查询范围内
     *                 //当一个支撑点满足 dis(q, p_j) - 查询半径 &gt; 第i个扇出的数据到第j个支撑点的上界， 那么第i个扇出的所有数据都不在查询范围内
     *                 if(queryToPivotDistance[j] + radius &lt; range[0][j] || queryToPivotDistance[j] - radius &gt; range[1][j])
     *                 {
     *                     nodeSearchActions[i] = NodeSearchAction.RESULTNONE;
     *                     break;
     *                 }
     *             }
     *         }
     *         return nodeSearchActions;
     *      }
     * </pre>
     *
     * @param node                 需要搜索的节点
     * @param metric               距离函数
     * @param query                查询对象
     * @param radius               查询半径
     * @param queryToPivotDistance 查询对象到该节点的各个支撑点的距离
     * @param memoryHashTable 记忆表，用于缓存计算过的pivot距离
     * @return 标记孩子状态的数组
     * @see NodeSearchAction
     */
    public abstract NodeSearchAction[] willTheSubTreeFurtherSearch(Node node, Metric metric, IndexObject query, double radius, double[] queryToPivotDistance, Hashtable<IndexObject, Double> memoryHashTable, LinkedList<DoubleIndexObjectPair> currentResult);
}
