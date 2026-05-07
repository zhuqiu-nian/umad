package algorithms.datapartition;

import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.structure.PartitionResults;
import index.structure.VPPartitionResults;
import metric.Metric;
import util.Debug;

import java.util.*;
import java.util.logging.Logger;


/**
 * 该类为VPIndex的划分方法。
 * <p>
 * Balanced：所有的分区都有相似的大小，划分迅速，但是性能表现最差。
 * ClusteringKMeans：根据内在的集群划分数据，针对每个维度采用k-means算法。
 */
public enum VPPartitionMethods implements PartitionMethod
{
    /**
     * 所有的分区都有相似的大小，划分迅速，但是性能表现最差。
     */
    BALANCED
            {
                /**
                 * 平衡划分是不需要设置划分半径的，此参数可以省略
                 * @param R 可省略
                 */
                public void setMaxRadius(double R)
                {
                }

                /**
                 * @param metric 距离函数
                 * @param pivots 支撑点集合
                 * @param data 数据
                 * @param numPartitions 分区数目
                 * @return 分区结果对象 {@link PartitionResults}
                 */
                public PartitionResults partition(final Metric metric, final IndexObject[] pivots, List<? extends IndexObject> data, final int numPartitions, int maxLS)
                {
                    return partition(metric, pivots, data, 0, data.size(), numPartitions, maxLS);
                }

                /**
                 * @param metric 距离函数
                 * @param pivots 支撑点集合
                 * @param data 数据
                 * @param first 第一个元素的偏移量
                 * @param size 数据大小
                 * @param numPartitions 分区数目
                 * @return 分区结果对象 {@link PartitionResults}
                 */
                public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int first, int size, int numPartitions, int maxLS)
                {
                    return partition(metric, pivots, data, first, size, numPartitions, maxLS, Logger.getLogger("index"));
                }

                /**
                 * @param metric 距离函数
                 * @param pivots 支撑点集合
                 * @param data 数据
                 * @param first 第一个元素的偏移量
                 * @param size 数据大小
                 * @param numPartitions 分区数目
                 * @param logger 日志
                 * @return 分区结果对象 {@link PartitionResults}
                 */
                public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int first, int size, int numPartitions, int maxLS, Logger logger)
                {

                    /*计算整个VPT的划分块数=每个支撑点的扇出*支撑点个数*/
                    final int numPivots = pivots.length;
                    final int fanout    = (int) Math.pow(numPartitions, numPivots);

                    if (Debug.debug) logger.finer("Start of splitData(), data size= " + size + ", VPNumber= " + numPivots + ", fanout= " + fanout);

                    // the lower and upper bound of distances from each child to each
                    // vantage point
                    /*定义每个孩子到支撑点的上界和下界*/
                    double[][] lower = new double[fanout][numPivots];
                    double[][] upper = new double[fanout][numPivots];

                    /*初始化每个数据的封装值为0*/
                    DoubleIndexObjectPair[] wrapper = new DoubleIndexObjectPair[size];
                    for (int i = first; i < size; i++)
                         wrapper[i] = new DoubleIndexObjectPair(0, data.get(i));

                    // split data.
                    int clusterNumber = 1; /*所有划分的总子块数*/
                    // total cluster number when partition based
                    // on each vp, SVF ^ i
                    int clusterCardinality = fanout; // number of final cluster in each
                    // of current cluster

                    int[] clusterOffset = new int[2];
                    clusterOffset[0] = 0;   /*只有一个子块时，开始就是0，结束就是全部数据的最后一个*/
                    clusterOffset[1] = size;

                    /*一个支撑点一划分*/
                    for (int i = 0; i < numPivots; i++)
                    {
                        if (Debug.debug) logger.finer("\nStart spliting vp:" + i + ", cluster number:" + clusterNumber + ", clusterCardinality =" + clusterCardinality + ", computing distances to the vp...");

                        //计算数据到当前支撑点的距离，数据进行封装，封装到wrapper中
                        /*
                         *	 计算的是数据到支撑点的距离，到时候就可能从这里开始修改，修改成超平面的截距
                         **/
                        for (int j = 0; j < size; j++)
                             wrapper[j].setDouble(metric.getDistance(pivots[i], wrapper[j].getObject()));

                        if (Debug.debug) logger.finer("Sorting the new computed distances...:");

                        //对封装好数据到当前支撑点距离的每个子块进行块内的排序
                        for (int j = 0; j < clusterNumber; j++)
                        {
                            if (Debug.debug) logger.finer("[" + j + ": " + clusterOffset[j] + ", " + clusterOffset[j + 1] + "], ");

                            /*
                             *	 对支撑点的距离进行排序，到时候可以对截距进行排序
                             **/
                            Arrays.sort(wrapper, clusterOffset[j], clusterOffset[j + 1], DoubleIndexObjectPair.DoubleComparator);
                        }

                        /*所有划分的总子块数X每个支撑点的块数=下次划分后的总子块数*/
                        final int nextClusterNumber = clusterNumber * numPartitions;
                        int[]     nextClusterOffset = new int[nextClusterNumber + 1];   /*下一次划分的总子块数，每一块都有一个界限*/

                        /*整体界限设置*/
                        nextClusterOffset[0]                 = 0;
                        nextClusterOffset[nextClusterNumber] = size;

                        /*下一次划分后每块的大小*/
                        int nextClusterCardinality = clusterCardinality / numPartitions;

                        // split each current cluster into SVF sub-clusters based on the
                        // distance to current VP
                        /*根据各个数据到支撑点的距离，将已经划分好的每个块进行进一步的划分*/
                        for (int j = 0; j < clusterNumber; j++)  /*对每个当前子块进行的操作*/
                        {
                            /*每个子块的大小，由子块的界限限定*/
                            final int clusterSize = clusterOffset[j + 1] - clusterOffset[j];

                            //空子块的所有儿孙子块都为空
                            if (clusterSize == 0)
                            {
                                for (int k = 0; k < numPartitions; k++)
                                     nextClusterOffset[j * numPartitions + k + 1] = clusterOffset[j + 1];
                                continue;/*跳出循环，直接处理下一个兄弟子块*/
                            }

                            /*
                             *	 以下就是对于非空子块的处理过程了
                             **/
                            if (Debug.debug)
                            {
                                logger.finer("Partitioning the " + j + "th cluster, size=" + clusterSize + ", Distances: ");
                                for (int temp = clusterOffset[j]; temp < clusterOffset[j + 1]; temp++)
                                     logger.finer(wrapper[temp].getDouble() + ", ");
                                logger.finer("");
                            }

                            // find the last indices of each distinct distance value in
                            // wrapper, which is already sorted
                            /*在已经存储好的wrapper值中找到每个划分值的最后一个下标*/
                            ArrayList<Integer> tempIndex = new ArrayList<>();
                            ArrayList<Double>  tempValue = new ArrayList<>();

                            // the distinct distance value in check, and the number of
                            // points with this distance
                            /*第j个划分值，当前划分值*/
                            double currentDistance = wrapper[clusterOffset[j]].getDouble();
                            int    sum             = 0;

                            /*从当前子块中最小的值到当前子块中最大的值，也就是说是整个子块进进行查找*/
                            for (int k = clusterOffset[j]; k < clusterOffset[j + 1]; k++)
                            {
                                final double nextDistance = wrapper[k].getDouble();  /*逐个不断取新的值*/

                                if (nextDistance != currentDistance) // find next
                                // distinct
                                // distance value
                                {
                                    tempIndex.add(sum);  /*没出现过的距离就加进到字典中*/
                                    tempValue.add(currentDistance);
                                    currentDistance = nextDistance;  /*因为wrapper本来就是有序的，只要邻居不相同，那就是不相同了*/
                                }

                                sum++;  /*计算当前块的数据个数*/
                            }
                            // put the last distinct value into the list
                            tempIndex.add(sum);
                            tempValue.add(currentDistance);

                            final int distinctSize = tempIndex.size();  /*当前块一共有这么多个不相同的距离信息*/

                            // index of first point with current distinct distance
                            // value,
                            // this is the offset in current cluster, not the index in
                            // wrapper
                            // distinct distance values

                            /*----------计算当前不重复数据集中的第一个值的索引和值，并以简易字典的形式记录----------------*/
                            int[]    firstPointWithDistinctDistance = new int[distinctSize + 1];
                            double[] distinctDistance               = new double[distinctSize];
                            firstPointWithDistinctDistance[0]            = 0;
                            firstPointWithDistinctDistance[distinctSize] = clusterSize;  /*子块大小*/
                            distinctDistance[0]                          = wrapper[clusterOffset[j]].getDouble();  /*在不重复数据集中的第一个数据*/
                            for (int k = 1; k < distinctSize; k++)
                            {
                                firstPointWithDistinctDistance[k] = tempIndex.get(k - 1).intValue();
                                distinctDistance[k]               = tempValue.get(k).doubleValue();
                            }
                            /*----------计算当前不重复数据集中的第一个值的索引和值，并以简易字典的形式记录----------------*/

                            if (Debug.debug)
                            {
                                logger.finer("distinct distances(" + distinctSize + "): ");
                                for (int temp = 0; temp < distinctSize; temp++)
                                     logger.finer("[" + temp + ": " + distinctDistance[temp] + ", " + firstPointWithDistinctDistance[temp] + "], ");
                                logger.finer("");
                            }

                            // assign the total distinctSize set of points with
                            // identical distance value
                            // to at most SVF sub-clusters, which is actually split
                            // current cluster

                            // number of distinct set that are already been assigned
                            int startingDistinctSet = 0;   /*已经分配的无重复距离值集合*/

                            // if distince set number is greater than SVF, assign them,
                            // otherwise,
                            // just assign one set to each sub-cluster, remain
                            // sub-clusters are all empty
                            /*如果距离集合数过多，就分配掉，否则就进行子集分配，直到所有子集都空*/
                            /*K，当前子集分配到非重复集合*/
                            int k = 0; // k is the current sub-cluster to assign distinct set to
                            while ((k < numPartitions - 1) && (distinctSize - startingDistinctSet > numPartitions - k))
                            {
                                // assign sets based on their cardinality, prefer
                                // balance sub-cluster
                                /*根据基数分配集合，计算中位数*/
                                final int median = (clusterSize - firstPointWithDistinctDistance[startingDistinctSet]) / (numPartitions - k);  /*（当前块大小-当前块的第一个数据）/（每个支撑点的扇出数 - 已分配集合数）*/


                                // 找到包含中位数的集合
                                int t = startingDistinctSet;
                                while (firstPointWithDistinctDistance[t + 1] < median + firstPointWithDistinctDistance[startingDistinctSet]) t++;
                                if (t != startingDistinctSet) t = (firstPointWithDistinctDistance[t + 1] - median - firstPointWithDistinctDistance[startingDistinctSet] >= median + firstPointWithDistinctDistance[startingDistinctSet] - firstPointWithDistinctDistance[t]) ? t - 1 : t;

                                /*startingDistinctSet是第一个非重合集合用来分配当前子集的第一个下标，t是最后一个标下*/
                                /*开始分配子集的上限和下限lower, upper*/
                                nextClusterOffset[j * numPartitions + k + 1] = clusterOffset[j] /*当前集合划分值索引*/ + firstPointWithDistinctDistance[t + 1];

                                final int firstChild = j * clusterCardinality /*第j个子块*每块的数量+第k个集合*儿子块的大小，即第k个儿子集合的第一个下标*/ + k * nextClusterCardinality;   /*nextClusterCardinality = clusterCardinality / numPartitions*/

                                /*
                                 * 	在这个lower和upper两个数组的计算中可能需要再琢磨一下如何修改
                                 * */
                                for (int temp = firstChild; temp < firstChild + nextClusterCardinality; temp++)
                                {
                                    lower[temp][i] = distinctDistance[startingDistinctSet];
                                    upper[temp][i] = distinctDistance[t];  /*第t个元素到到第i个支撑点的距离*/
                                }/*计算当前子集中到支撑点的最大、最小距离*/

                                if (Debug.debug)
                                {
                                    logger.finer("computing " + k + "th sub-cluster, median=" + median + ", assigned distinct set:" + startingDistinctSet + ", last set:" + t + ", first child =" + firstChild + ", i=" + i + ", j=" + j + ", k=" + k);
                                    logger.finer("next cluster offset:");
                                    for (int temp = 0; temp < nextClusterOffset.length; temp++)
                                         logger.finer("[" + temp + ":" + nextClusterOffset[temp] + "],");

                                    logger.finer("\nlower, upper:");
                                    for (int temp = 0; temp < fanout; temp++)
                                         logger.finer("[" + temp + ": " + lower[temp][i] + ", " + upper[temp][i] + "], ");
                                    logger.finer("");
                                }

                                startingDistinctSet = t + 1;   /*跑到包含中位数的集合的开始处*/
                                k++;  /*下一个儿子*/
                            }

                            // if reaches the last sub-cluster, assign all remain set to
                            // it
                            if (k == numPartitions - 1)  /*最后一个子块*/
                            {
                                //j * numPartitions + k + 1=（j+1） * numPartitions，
                                nextClusterOffset[j * numPartitions + k + 1] = clusterOffset[j + 1];

                                final int firstChild = j * clusterCardinality + k * nextClusterCardinality;
                                for (int temp = firstChild; temp < firstChild + nextClusterCardinality; temp++)
                                {
                                    lower[temp][i] = distinctDistance[startingDistinctSet];
                                    upper[temp][i] = distinctDistance[distinctSize - 1];
                                }/*计算当前子集中到支撑点的最大、最小距离*/
                            }

                            // remain set number is not greater than remain sub-cluster
                            // number,
                            // assign one set to each sub-cluster
                            else
                            {
                                if (Debug.debug)
                                {
                                    logger.finer("less distinct set:" + (distinctSize - startingDistinctSet) + ", remain sub-cluster:" + (numPartitions - k));
                                }

                                for (int t = startingDistinctSet; t < distinctSize; t++)
                                {
                                    nextClusterOffset[j * numPartitions + k + 1] = clusterOffset[j] + firstPointWithDistinctDistance[t + 1];

                                    final int firstChild = j * clusterCardinality + k * nextClusterCardinality;
                                    for (int temp = firstChild; temp < firstChild + nextClusterCardinality; temp++)
                                    {
                                        lower[temp][i] = distinctDistance[t];
                                        upper[temp][i] = distinctDistance[t];
                                    }

                                    k++;
                                }

                                /*还有子块，则置空*/
                                if (k < numPartitions)
                                {
                                    for (; k < numPartitions; k++)
                                         nextClusterOffset[j * numPartitions + k + 1] = clusterOffset[j + 1];
                                }
                            }
                        } /*对每个当前子块进行的操作*/ // end of a loop for each cluster

                        /*更新界限信息*/
                        clusterOffset      = nextClusterOffset;
                        clusterCardinality = nextClusterCardinality;
                        clusterNumber      = nextClusterNumber;

                    } /*所有支撑点计算完*/

                    //计算非空子块数，即当前结点的孩子数
                    int childrenNumber = 0;
                    for (int i = 0; i < fanout; i++)
                    {
                        if (clusterOffset[i] < clusterOffset[i + 1]) childrenNumber++;
                    }

                    if (Debug.debug) logger.finer("final children number: " + childrenNumber + ", fanout=" + fanout);

                    /*如果存在空子集，则删除掉它们*/
                    if (childrenNumber < fanout)
                    {
                        double[][] newLower  = new double[childrenNumber][];
                        double[][] newUpper  = new double[childrenNumber][];
                        int[]      newOffset = new int[childrenNumber + 1];
                        newOffset[childrenNumber] = size;

                        int j = 0;
                        for (int i = 0; i < fanout; i++)
                        {
                            if (clusterOffset[i] < clusterOffset[i + 1])
                            {
                                newLower[j]  = lower[i];
                                newUpper[j]  = upper[i];
                                newOffset[j] = clusterOffset[i];
                                j++;
                            }
                        }

                        lower         = newLower;
                        upper         = newUpper;
                        clusterOffset = newOffset;
                    }

                    //安排所有孩子结点到一个集合中去
                    List<List<? extends IndexObject>> subDataList = new ArrayList<>(childrenNumber);
                    for (int i = 0; i < childrenNumber; i++)
                    {
                        ArrayList<IndexObject> subList = new ArrayList<>(clusterOffset[i + 1] - clusterOffset[i]);

                        for (int j = clusterOffset[i]; j < clusterOffset[i + 1]; j++)
                             subList.add(wrapper[j].getObject());

                        if (subList.size() == 0) System.out.println("sub list :" + i + " is empty!");

                        subDataList.add(subList);
                    }

                    return new VPPartitionResults(subDataList, pivots, lower, upper);
                }
            },
    /**
     * 根据内在的集群划分数据，针对每个维度采用k-means算法。
     */
    CLUSTERINGKMEANS
            {
                private int GHTDegree = 0; // -1: ght, -2:cght

                /**
                 * @author Rui Mao
                 */
                class ClusteringKMeansTask
                {

                    private final int first;

                    private final int last;

                    private final double[] lower;

                    private final double[] upper;

                    private final boolean[] toUse;

                    public ClusteringKMeansTask(int first, int last, double[] lower, double[] upper, boolean[] toUse)
                    {
                        this.first = first;
                        this.last  = last;
                        this.lower = lower;
                        this.upper = upper;
                        this.toUse = toUse;
                    }

                }

                /**
                 * @param metric 距离函数
                 * @param pivots 支撑点集合
                 * @param data 数据
                 * @param numPartitions 分区数目
                 * @return 分区结果对象 {@link PartitionResults}
                 */
                public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int numPartitions, int maxLS)
                {
                    return partition(metric, pivots, data, 0, data.size(), numPartitions, maxLS);
                }

                /**
                 * @param metric 距离函数
                 * @param pivots 支撑点集合
                 * @param data 数据
                 * @param first 第一个元素的偏移量
                 * @param size 数据大小
                 * @param numPartitions 分区数目
                 * @return 分区结果对象 {@link PartitionResults}
                 */
                public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int first, int size, int numPartitions, int maxLS)
                {
                    return partition(metric, pivots, data, first, size, numPartitions, maxLS, Logger.getLogger("index"));
                }

                //        /**
                //         * given vantage points, this method partition the dataset based on its
                //         * intrinsic clustering.
                //         *
                //         * @param METRIC
                //         *            the {@link Metric} to compute distance with
                //         * @param data
                //         *            the source data list to split, each element is a
                //         *            {@link RecordObject}
                //         * @param VP
                //         *            the vantage points array, each element can be computed
                //         *            distance on
                //         * @param SVF
                //         *            partition number induced by each vantage point
                //         * @param maxLS
                //         *            max leaf size, if a cluster has less size, don't partition
                //         *            further
                //         * @return a list, the first element is a List [], which contains lists
                //         *         of data of each child, the second element is of db double
                //         *         [][], which is the lowerRange, the min distance from each
                //         *         child to each vantage point, child*VP, the third element is
                //         *         of db double [][], which is the upperRange, the max
                //         *         distance from each child to each vantage point, child*vp
                //         */
                PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int first, int size, final int SVF, final int maxLS, Logger logger)
                {
                    if (Debug.debug) logger.finer("clusteringPartition");

                    // compute all the distance
                    final int numPivots = pivots.length;

                    double[][] distance = new double[numPivots][size];

                    for (int i = 0; i < size; i++)
                        for (int j = 0; j < numPivots; j++)
                             distance[j][i] = metric.getDistance(data.get(i), pivots[j]);

                    // hack: if this is ght,cght partition, transform distances to d1+d2
                    // an d1-d2
                    if (this.GHTDegree < 0)
                    {
                        // for now, must be only two pivots
                        if (distance.length != 2)
                            throw new IllegalArgumentException("for CGHT partition, there should be only two pivots!");

                        // transformation to d1+d2, d1-d2
                        for (int i = 0; i < size; i++)
                        {
                            distance[0][i] = distance[0][i] + distance[1][i];
                            distance[1][i] = distance[0][i] - distance[1][i] * 2;
                        }
                    }

                    // matain a list of clusters to be partitioned. each list item
                    // contains:
                    // 1. the first last offset of the cluster in the data array, two
                    // Integers
                    // 2. the distance ranges to all vps, two 1-d double array the first
                    // is the lower bound,
                    // then the upper bound
                    // 3. a boolean array corresponding to all vps, true means this vp
                    // is to partition on.
                    // if all the list item's boolean array are all false, the partition
                    // is done.
                    // therefore, when add a list item to the list, if its boolean array
                    // is all false, add
                    // it to the end, otherwise to the begining
                    // thus if the first list item's boolean array is all false, then
                    // the partition is done.

                    if (this.GHTDegree != -1)
                    {
                        LinkedList<ClusteringKMeansTask> taskList = new LinkedList<>();
                        boolean[]                        toUse    = new boolean[numPivots];
                        for (int i = 0; i < numPivots; i++)
                        {
                            toUse[i] = true;
                        }
                        ClusteringKMeansTask ckmTask = new ClusteringKMeansTask(0, size - 1, new double[numPivots], new double[numPivots], toUse);
                        taskList.addFirst(ckmTask);

                        boolean done = false;
                        // the loop to partition each cluster
                        while (true)
                        {
                            ckmTask = taskList.getFirst();
                            done    = true;
                            for (int i = 0; i < numPivots; i++)
                                if (ckmTask.toUse[i])
                                {
                                    done = false;
                                    break;
                                }

                            // if done, the first list item's boolean is all false, the
                            // partition is done, ready
                            // to return
                            if (done) break;

                            // otherwise, partition the current cluster, select a best
                            // vp,
                            // partition based on
                            // this vp, put new sub-clusters into task list, arrange
                            // data
                            // list, distance array.
                            partitionACluster(data, distance, taskList, SVF, maxLS, logger);
                        }

                        // now partition is done, return result's in required format.
                        final int childrenNumber = taskList.size(); // may need to check
                        // whether cluster
                        // number
                        // ==1
                        // if (childrenNumber ==1)
                        // System.out.println("cluster can not be partitioned!");

                        List<List<? extends IndexObject>> subDataList = new ArrayList<>(childrenNumber);
                        double[][] allLower     = new double[childrenNumber][numPivots];
                        double[][] allUpper     = new double[childrenNumber][numPivots];
                        final int  taskListSize = taskList.size();
                        List<IndexObject> sub;
                        for (int i = 0; i < taskListSize; i++)
                        {
                            ckmTask = taskList.get(i);
                            sub = new ArrayList<>();
                            for (int j=ckmTask.first; j<ckmTask.last + 1; j++){
                                sub.add(data.get(j));
                            }
                            subDataList.add(sub);
                            for (int j = 0; j < numPivots; j++)
                            {
                                allLower[i][j] = ckmTask.lower[j];
                                allUpper[i][j] = ckmTask.upper[j];
                            }
                        }

                        return new VPPartitionResults(subDataList, pivots, allLower, allUpper);
                    }
                    //else ght partition
                    {
                        final int childrenNumber = 2;
                        List<List<? extends IndexObject>> subDataList = new ArrayList<>(childrenNumber);
                        double[][] allLower = new double[childrenNumber][numPivots];
                        double[][] allUpper = new double[childrenNumber][numPivots];

                        int    head = 0, tail = size - 1;
                        double temp;
                        while (head <= tail)
                        {
                            while ((head < size) && (distance[1][head] <= 0)) head++;
                            while ((tail >= 0) && (distance[1][tail] > 0)) tail--;
                            if (head <= tail)
                            {
                                Collections.swap(data, head, tail);
                                temp              = distance[1][head];
                                distance[1][head] = distance[1][tail];
                                distance[1][tail] = temp;
                            }
                        }
                        subDataList.add((data.subList(0, head)));
                        subDataList.add(data.subList(head, size));

                        return new VPPartitionResults(subDataList, pivots, allLower, allUpper);
                    }
                }

                //        /**
                //         * partition the first cluster in the task list, select a best vp,
                //         * partition based on this vp, put new sub-clusters back into task list,
                //         * if the sub-clusters don't have further partition, append them to the
                //         * end, otherwise insert to the head arrange data list, distance array,
                //         * put data and distances belongs to the same sub-clusters together.
                //         *
                //         * @param data
                //         *            list of data set
                //         * @param distance
                //         *            distance values from each data point to each vantage point
                //         * @param taskList
                //         *            a {@link LinkedList} of all the clusters to be
                //         *            partitioned.
                //         * @param SVF
                //         *            single fanout
                //         * @param maxLS
                //         *            max leaf size, if a cluster has less size, don't partition
                //         *            further
                //         * @param logger
                //         */

                /**
                 * 对任务列表中的第一个集群进行分区，选择一个最佳vp，
                 * 基于这个vp的划分，将新的子集群放回任务列表中，
                 * 如果子集群没有进一步的分区，则将它们附加到最后,
                 * 否则插入到头部排列数据列表，距离数组，
                 * 把数据和距离放在同一个子簇中。
                 * @param data 数据集合
                 * @param distance 从每个数据点到每个有利点的距离值
                 * @param taskList  所有要分区集群的{@link LinkedList}
                 * @param SVF 单扇出
                 * @param maxLS 最大叶大小，如果集群的大小较小，就不要进一步分区
                 * @param logger 日志
                 */
                private void partitionACluster(List<? extends IndexObject> data, double[][] distance, LinkedList<ClusteringKMeansTask> taskList, final int SVF, final int maxLS, Logger logger)
                {
                    ClusteringKMeansTask task  = taskList.removeFirst();
                    final int            first = task.first;
                    final int            last  = task.last;

                    boolean[] toUse = task.toUse;
                    // if current cluster can be fit in a leaf node, don't partition
                    // further
//                    if (last - first + 1 <= maxLS)
//                    {
//                        double min = Double.POSITIVE_INFINITY;
//                        double max = Double.NEGATIVE_INFINITY;
//
//                        double[] lower = task.lower;
//                        double[] upper = task.upper;
//
//                        // set distance range for unused vps
//                        for (int i = 0; i < toUse.length; i++)
//                            if (toUse[i])
//                            {
//                                for (int j = first; j <= last; j++)
//                                {
//                                    if (min > distance[i][j]) min = distance[i][j];
//
//                                    if (max < distance[i][j]) max = distance[i][j];
//                                }
//
//                                lower[i] = min;
//                                upper[i] = max;
//                                toUse[i] = false;
//                            }
//
//                        taskList.addLast(task);
//                        return;
//                    }

                    // if the cluster can not be fit in a leaf node, go on to partition

                    int      childrenNumber = 1;
                    int      minVP          = 0; // the index of the vp with the min variance
                    double   minVar         = Double.POSITIVE_INFINITY; // min value of variance
                    double   var            = 0; // temp variable for variance
                    double[] means          = null; // means of k-means
                    double[] split          = null; // split values
                    double[] minSplit       = null; // the set of split values with the min
                    // variance
                    int[] bucketSize    = null;
                    int[] minBucketSize = null; // the bucketsize array of the vp with
                    // min variance
                    double[] lower    = null;
                    double[] minLower = null;
                    double[] upper    = null;
                    double[] minUpper = null; // lower, upper bound of distances of
                    // each sub-cluster
                    for (int i = 0; i < toUse.length; i++)
                    {
                        // if the vp is already used, go to the next one
                        if (!toUse[i]) continue;

                        // find the initial clustering to run k-means, each item in
                        // means should be a
                        // different real distance value that exists
                        // thus, if the length of means is less than SVF, all the
                        // distinct distance values
                        // have been returned, no need to run k-means
                        means = bucketInitialClustering(distance[i], first, last, SVF);

                        if (means.length < SVF)
                        {
                            if (childrenNumber > means.length) // some other vp can
                                // partition the cluster
                                // into more sub-clusters, ignore current vp
                                continue;
                        } else
                        // run the k-means with means as the initial clustering
                        {
                            kMeans(distance[i], first, last, means, logger);
                        }

                        split = new double[means.length - 1];
                        Arrays.sort(means);
                        for (int j = 0; j < split.length; j++)
                             split[j] = (means[j] + means[j + 1]) / 2;

                        // split values are available, compute the variance
                        bucketSize = new int[split.length + 1];
                        for (int j = 0; j < bucketSize.length; j++)
                             bucketSize[j] = 0;

                        lower = new double[split.length + 1];
                        upper = new double[split.length + 1];
                        for (int j = 0; j < lower.length; j++)
                        {
                            lower[j] = Double.POSITIVE_INFINITY;
                            upper[j] = Double.NEGATIVE_INFINITY;
                        }
                        for (int j = first; j <= last; j++) // for each point, find
                        // which bucket it belongs
                        // to. left bound inclusive, right bound
                        // exclusive
                        {
                            int k = 0;
                            while ((k < split.length) && (distance[i][j] >= split[k])) k++;

                            bucketSize[k]++;
                            if (lower[k] > distance[i][j]) lower[k] = distance[i][j];

                            if (upper[k] < distance[i][j]) upper[k] = distance[i][j];
                        }

                        var = 0;
                        // varx = E(x^2) - (Ex)^2, since Ex is fixed, we dont compute
                        // it.
                        for (int j = 0; j < bucketSize.length; j++)
                             var += bucketSize[j] * bucketSize[j];

                        // compare with currnet min variance
                        if ((bucketSize.length > childrenNumber) || ((bucketSize.length == childrenNumber) && (minVar > var)))
                        {
                            minVar        = var;
                            minSplit      = split;
                            minVP         = i;
                            minBucketSize = bucketSize;
                            minLower      = lower;
                            minUpper      = upper;
                        }
                        if (bucketSize.length > childrenNumber)
                        {
                            childrenNumber = bucketSize.length;
                        }

                    }// end of loop for each vp

                    // the best vp is found , split and bucketsize array are ready,
                    // partition the cluster
                    // now
                    // if childrenNumber ==1, the cluster can not be partitioned by any
                    // vp, add a finished
                    // task to task list
                    if (childrenNumber == 1)
                    {
                        lower = task.lower;
                        upper = task.upper;
                        for (int i = 0; i < toUse.length; i++)
                            if (toUse[i])
                            {
                                lower[i] = distance[i][first];
                                upper[i] = distance[i][first];
                                toUse[i] = false;
                            }

                        ClusteringKMeansTask newCKMTask = new ClusteringKMeansTask(first, last, lower, upper, toUse);
                        taskList.addLast(newCKMTask);
                        return;
                    }

                    // if childrennumber != 1, continue further process
                    // set bucketFirst array, bucketFirst[i] is the offset of the first
                    // element of bucket i
                    int[] bucketFirst = new int[childrenNumber + 1];
                    System.arraycopy(minBucketSize, 0, bucketFirst, 0, childrenNumber);
                    for (int i = 1; i < childrenNumber; i++)
                         bucketFirst[i] += bucketFirst[i - 1];
                    System.arraycopy(bucketFirst, 0, bucketFirst, 1, childrenNumber);
                    bucketFirst[0] = 0;
                    for (int i = 0; i <= childrenNumber; i++)
                         bucketFirst[i] += first;

                    // bucketPointer[i] points to the first place of bucket i to be
                    // sorted
                    int[] bucketPointer = new int[childrenNumber];
                    System.arraycopy(bucketFirst, 0, bucketPointer, 0, childrenNumber);

                    // ready to arrange, for each bucket, for each value, if it doesn't
                    // belong to the
                    // bucket,
                    // exchange it with an element in the correct bucket
                    double tempDouble;
                    for (int i = 0; i < childrenNumber; i++)
                    {
                        for (int j = bucketPointer[i]; j < bucketFirst[i + 1]; j++)
                        {
                            while (true)
                            {
                                // compute the bucket id of current point
                                int k = 0; // k is the correct id
                                while ((k < minSplit.length) && (distance[minVP][j] >= minSplit[k])) k++;

                                // if current point belongs to current bucket, go to
                                // next point
                                if (k == i) break;

                                // exchange the point object
                                Collections.swap(data, j, bucketPointer[k]);

                                // exchange the distance values to all vps
                                for (int t = 0; t < distance.length; t++)
                                {
                                    tempDouble                    = distance[t][j];
                                    distance[t][j]                = distance[t][bucketPointer[k]];
                                    distance[t][bucketPointer[k]] = tempDouble;
                                }

                                bucketPointer[k]++;
                            } // end of while
                        }
                    }

                    // the data list and distance array have been re-arranged, now
                    // create sub-clusters, and
                    // put them into task list
                    toUse[minVP] = false;
                    boolean done = true;
                    // check whether the sub-clusters need further partition
                    for (int i = 0; i < toUse.length; i++)
                        if (toUse[i])
                        {
                            done = false;
                            break;
                        }

                    if (done) // if no further partition (all vp are used), add to the
                    // end of the task
                    // list
                    {
                        for (int i = 0; i < childrenNumber; i++)
                        {
                            lower        = task.lower.clone();
                            lower[minVP] = minLower[i];
                            upper        = task.upper.clone();
                            upper[minVP] = minUpper[i];

                            ClusteringKMeansTask newCKMTask = new ClusteringKMeansTask(bucketFirst[i], bucketFirst[i + 1] - 1, lower, upper, toUse.clone());
                            taskList.addLast(newCKMTask);
                        }
                    } else
                    // further partition is needed, add to the head of the task list
                    {
                        for (int i = childrenNumber - 1; i >= 0; i--)
                        {
                            lower        = task.lower.clone();
                            lower[minVP] = minLower[i];
                            upper        = task.upper.clone();
                            upper[minVP] = minUpper[i];

                            ClusteringKMeansTask newCKMTask = new ClusteringKMeansTask(bucketFirst[i], bucketFirst[i + 1] - 1, lower, upper, toUse.clone());
                            taskList.addFirst(newCKMTask);
                        }
                    }

                }

                //        /**
                //         * find the initial clustering to run k-means, each item in retuned
                //         * array should be a real distinct distance value that exists, thus, if
                //         * the length of returned array is less than SVF, all the distinct
                //         * distance values have been returned
                //         *
                //         * @param distance
                //         *            the array containing all the double value to find means on
                //         * @param first
                //         *            offset of the first element
                //         * @param last
                //         *            offset of the last element
                //         * @param SVF
                //         *            number of means/distinct values to find
                //         * @return an array of means, each element is different from others, if
                //         *         its length is shorter than SVF, all distinct values are in it
                //         */

                /**
                 * 找到初始聚类，运行k-means，每项retuned
                 * 数组应该是一个真实存在的不同距离值，因此，如果
                 * 返回数组的长度小于SVF，完全不同
                 * 距离值已经返回
                 * @param distance 包含查找方法的所有双精度值的数组
                 * @param first 第一个元素的偏移量
                 * @param last 最后一个元素的偏移量
                 * @param SVF 要查找的平均值/不同值的数目
                 * @return 一个表示数组，每个元素不同于其他元素，如果其长度小于SVF，则所有不同的值都在其中
                 */
                private double[] bucketInitialClustering(double[] distance, final int first, final int last, final int SVF)
                {
                    final int bucketNumber = Math.max(Math.min((last - first + 1) / 10, 50 * SVF), SVF);

                    double min, max; // min , max distance to a vp
                    min = Double.POSITIVE_INFINITY;
                    max = Double.NEGATIVE_INFINITY;
                    for (int j = first; j <= last; j++)
                    {
                        if (distance[j] > max) max = distance[j];

                        if (distance[j] < min) min = distance[j];
                    }

                    // if min == max, can not partition by current vp.
                    if (max == min)
                    {
                        double[] result = new double[1];
                        result[0] = distance[first];
                        return result;
                    }

                    // compute the bucket size
                    int[] bucketSize = new int[bucketNumber];
                    for (int i = 0; i < bucketNumber; i++)
                         bucketSize[i] = 0;

                    final double bucketWidth = (max - min) / bucketNumber;
                    for (int i = first; i <= last; i++)
                    {
                        int temp = (int) ((distance[i] - min) / bucketWidth);
                        if (temp >= bucketNumber) temp = bucketNumber - 1;
                        bucketSize[temp]++;
                    }

                    // find the buckets whose size is local max
                    boolean[] isLocalMax = new boolean[bucketNumber];
                    isLocalMax[0]                = bucketSize[0] >= bucketSize[1];
                    isLocalMax[bucketNumber - 1] = bucketSize[bucketNumber - 1] >= bucketSize[bucketNumber - 2];
                    for (int i = 1; i <= bucketNumber - 2; i++)
                         isLocalMax[i] = (bucketSize[i] >= bucketSize[i - 1]) && (bucketSize[i] >= bucketSize[i + 1]);

                    // remove consecutive local max bucket
                    int loop = 0;
                    while (loop < bucketNumber)
                    {
                        if (!isLocalMax[loop]) loop++;
                        else
                        {
                            int lastMax = loop + 1;
                            while ((lastMax < bucketNumber) && isLocalMax[lastMax]) lastMax++;
                            for (int i = loop; i < lastMax; i++)
                                 isLocalMax[i] = false;
                            isLocalMax[(loop + lastMax - 1) / 2] = true;
                            loop                                 = lastMax + 1;
                        }
                    }

                    int localMaxBucketNumber = 0; // number of positive-size local max
                    // bucket
                    for (int i = 0; i < bucketNumber; i++)
                        if (isLocalMax[i] && (bucketSize[i] > 0)) localMaxBucketNumber++;

                    if (localMaxBucketNumber >= SVF) // there are enough bins, find
                    // svf largest ones,
                    // return the middle point of them
                    {
                        boolean[] isLargest = new boolean[bucketNumber];
                        for (int i = 0; i < bucketNumber; i++)
                             isLargest[i] = false;

                        for (int i = 0; i < SVF; i++)
                        {
                            int maxSize = 0;
                            int maxId   = 0;
                            for (int j = 0; j < bucketNumber; j++)
                            {
                                if (isLocalMax[j] && !isLargest[j] && (bucketSize[j] > maxSize))
                                {
                                    maxSize = bucketSize[j];
                                    maxId   = j;
                                }
                            }
                            isLargest[maxId] = true;
                        }
                        double[] result  = new double[SVF];
                        int      counter = 0;
                        for (int i = 0; i < bucketNumber; i++)
                        {
                            if (isLargest[i])
                            {
                                result[counter] = min + (i + 0.5) * bucketWidth;
                                counter++;
                            }
                        }

                        return result;
                    } else
                    // no enough local max bucket, for each local max bin, find a value
                    // in it, then
                    // find metric
                    {
                        double[] result  = new double[SVF];
                        int      counter = 0;
                        // for each local max bucket, find a value in it.
                        for (int i = first; i <= last; i++)
                        {
                            int temp = (int) ((distance[i] - min) / bucketWidth);
                            if (temp >= bucketNumber) temp = bucketNumber - 1;
                            if (isLocalMax[temp])
                            {
                                result[counter]  = distance[i];
                                isLocalMax[temp] = false;
                                counter++;
                                if (counter >= localMaxBucketNumber) break;
                            }
                        }

                        // find distinct values
                        for (int i = first; i <= last; i++)
                        {
                            boolean isDistinct = true;
                            for (int j = 0; j < counter; j++)
                                if (distance[i] == result[j])
                                {
                                    isDistinct = false;
                                    break;
                                }

                            if (isDistinct)
                            {
                                result[counter] = distance[i];
                                counter++;
                                if (counter >= SVF) break;
                            }
                        }

                        if (counter < SVF) // no enough distinct values
                        {
                            double[] finalResult = new double[counter];
                            System.arraycopy(result, 0, finalResult, 0, counter);
                            return finalResult;
                        } else return result;
                    }

                }

                //        /**
                //         * run the k-means given the initial clustering. after running, the
                //         * results are stored in the argument, means
                //         *
                //         * @param distance
                //         *            the array containing all the double value to run on
                //         * @param first
                //         *            offset of the first element
                //         * @param last
                //         *            offset of the last element
                //         * @param means
                //         *            double array of initial values of means, after the method
                //         *            runs, its values are the final means
                //         * @param logger
                //         */

                /**
                 * 运行给定初始聚类的k-means。运行后,
                 * 结果存储在参数means中
                 * @param distance 包含要运行的所有双精度值的数组
                 * @param first 第一个元素的偏移量
                 * @param last 最后一个元素的偏移量
                 * @param means 方法初始值的双数组，在方法运行后，其值是最终的方法
                 * @param logger 日志
                 */
                private void kMeans(double[] distance, final int first, final int last, double[] means, Logger logger)
                {
                    final double stop          = 0.1;
                    final int    size          = last - first + 1;
                    final int    clusterNumber = means.length;
                    short[]      clusterId     = new short[size];
                    double[]     split         = new double[clusterNumber - 1];

                    double sum = 0, newSum = 0;

                    double[] clusterSum  = new double[clusterNumber];
                    int[]    clusterSize = new int[clusterNumber];
                    int      counter     = 0;
                    while ((counter < 2) || Math.abs(newSum - sum) / sum > stop)
                    {
                        sum = newSum;

                        // set the cluster id of each value
                        Arrays.sort(means);
                        for (int j = 0; j < split.length; j++)
                             split[j] = (means[j] + means[j + 1]) / 2;

                        for (int i = 0; i < size; i++)
                        {
                            clusterId[i] = 0; // k is the correct id
                            while ((clusterId[i] < split.length) && (distance[first + i] >= split[clusterId[i]])) clusterId[i]++;
                        }

                        // compute new mean and new sum
                        for (int i = 0; i < clusterNumber; i++)
                        {
                            clusterSum[i]  = 0;
                            clusterSize[i] = 0;
                        }
                        for (int i = 0; i < size; i++)
                        {
                            clusterSum[clusterId[i]] += distance[first + i];
                            clusterSize[clusterId[i]]++;
                        }

                        newSum = 0;
                        for (int i = 0; i < clusterNumber; i++)
                        {
                            means[i] = clusterSum[i] / clusterSize[i];
                            newSum += means[i];
                        }

                        counter++;
                        if ((counter > 100) && (counter % 100 == 0)) System.out.println("counter= " + counter + ", too large!");

                        if (Debug.debug) logger.finer("counter= " + counter + ",  sum= " + sum + ",  new sum= " + newSum);
                    }

                }
            },
    //EXCLUDEDMIDDLE

    //CGHT

    //GHT

}
