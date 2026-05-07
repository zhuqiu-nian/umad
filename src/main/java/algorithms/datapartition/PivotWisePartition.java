package algorithms.datapartition;

import db.type.IndexObject;
import index.structure.PartitionResults;
import index.structure.VPPartitionResults;
import metric.Metric;
import util.Debug;
import util.Histogram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is a utility class of data partition algorithm.  It partitions data pivot by pivot.
 * Which pivot to partition with depends on the result of partition based on that pivot
 *
 * @author Rui Mao
 * @version 2006.06.28
 */
class PivotWisePartition implements PartitionMethod
{
    int        MaxLS          = 0;
    Logger     logger         = null;
    int        SVF            = 0;
    double[][] distance       = null;
    double     MaxRadius      = 0;
    double     HistogramScale = 10;

    /**
     * @param R 最大半径
     */
    public void setMaxRadius(double R)
    {
        this.MaxRadius = R;
    }

    /**
     * @param metric        距离函数
     * @param pivots        支撑点集合
     * @param data          数据集合
     * @param numPartitions 分区数目
     * @param maxLS         最大叶大小，如果集群的大小较小，就不要进一步分区
     * @return 分区结果对象{@link PartitionResults}
     */
    public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int numPartitions, int maxLS)
    {
        return partition(metric, pivots, data, 0, data.size(), numPartitions, maxLS);
    }

    /**
     * @param metric        距离函数
     * @param pivots        支撑点集合
     * @param data          数据集合
     * @param first         第一个元素的偏移量
     * @param size          数据大小
     * @param numPartitions 分区数目
     * @param maxLS         最大叶大小，如果集群的大小较小，就不要进一步分区
     * @return 分区结果对象{@link PartitionResults}
     */
    public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int first, int size, int numPartitions, int maxLS)
    {
        double[][] distance = new double[pivots.length][size];

        for (int i = first; i < first + size; i++)
            for (int j = 0; j < pivots.length; j++)
                 distance[j][i - first] = metric.getDistance(data.get(i), pivots[j]);

        return partition(distance, pivots, data.subList(first, first + size), numPartitions, maxLS, Logger.getLogger("index"), this.MaxRadius);
    }
    //    /**
    //     * given pivots, this method partition the dataset pivot by pivot.
    //     *
    //     * @param distance distances from each data point (column) to each piovt(row)
    //     * @param pivot the pivots array, each element can be computed distance on
    //     * @param data the source data list to split, each element is a {@link RecordObject}
    //     * @param SVF partition number induced by each vantage point
    //     * @param maxLS max leaf size, if a cluster has less size, don't partition further
    //     * @return a list, the first element is a List [], which contains lists of data of each child,
    //     * the second element is of db double [][], which is the lowerRange, the min distance from each child
    //     * to each vantage point, child*VP, the third element is of db double [][], which is the upperRange,
    //     * the max distance from each child to each vantage point, child*vp
    //     */

    /**
     * 该方法在给定数据透视的基础上，对数据集进行透视划分。
     *
     * @param distance 每个数据点(列)到每个piovt(行)的距离
     * @param pivot    支撑点集合，可以对每个元素计算距离
     * @param data     要拆分的源数据列表
     * @param SVF      由每个有利点引出的分区数
     * @param maxLS    最大叶大小，如果集群的大小较小，就不要进一步分区
     * @param logger   日志
     * @param R        最大半径
     * @return 一个列表，第一个元素是list[]，它包含每个子元素的数据列表，
     *         第二个元素的类型是double[][]，它是较低的范围，即到每个子元素的最小距离
     *         对于每个有利位置，child*VP，第三个元素的类型是double[][]，这是上标，
     *         每个孩子到每个有利点的最大距离，child*vp
     */
    public PartitionResults partition(double[][] distance, IndexObject[] pivot, List<? extends IndexObject> data, final int SVF, final int maxLS, Logger logger, double R)
    {
        if (Debug.debug) logger.finer("Pivot-wise Partition");

        this.logger    = logger;
        this.MaxLS     = maxLS;
        this.SVF       = SVF;
        this.distance  = distance;
        this.MaxRadius = R;
        //compute all the distance
        final int numP = pivot.length;


        //maintain a list of partition task, each task contains: 
        // 1. the first last offset of the cluster data in the data array, two Integers
        // 2. the distance ranges to all vps, two 1-d double array the first is the lower bound, then the upper bound
        //    if the upper bound to a pivot is -1, then the pivot is not used

        LinkedList<PartitionTask> taskList = new LinkedList<PartitionTask>();
        taskList.addFirst(new PartitionTask(data, pivot));

        //maintain a list of partitions that are done.  finally, use these completed partitions to create an index node.
        LinkedList<PartitionTask> completedTask = new LinkedList<PartitionTask>();

        // the loop to partition each cluster
        while (!taskList.isEmpty())
        {
            PartitionTask task = taskList.removeFirst();

            //if task is finished or is a leaf, move it to completed task list.
            if (task.isDone() || task.isLeaf(distance, maxLS))
            {
                completedTask.add(task);
                continue;
            }

            //otherwise, process a partition task
            //1. select a best pivot
            //2. partition based on this pivot
            //3. put new tasks into task list
            //4. sort the data list and distance array.
            taskList.addAll(0, processTask(task));
        }

        //now partition is done, return result's in required format.
        final int childrenNumber = completedTask.size();  //may need to check whether cluster number ==1
        //if (childrenNumber ==1)
        //System.out.println("cluster can not be partitioned!");

        List<List<? extends IndexObject>> subDataList = new ArrayList<List<? extends IndexObject>>(childrenNumber);
        double[][]                        allLower    = new double[childrenNumber][numP];
        double[][]                        allUpper    = new double[childrenNumber][numP];

        for (int i = 0; i < childrenNumber; i++)
        {
            PartitionTask task = completedTask.get(i);
            subDataList.add(data.subList(task.first, task.last));
            for (int j = 0; j < numP; j++)
            {
                allLower[i][j] = task.lower[j];
                allUpper[i][j] = task.upper[j];
            }
        }

        VPPartitionResults partitionResult = new VPPartitionResults(subDataList, pivot, allLower, allUpper);

        return partitionResult;
    }


    //    /**
    //     * process a partition task.
    //     * Note 1: the task should be checked whether can be a leaf node before calling this method.
    //     * Note 2: the task should also be checked wheter all the points are identical
    //     * 1. select a best pivot
    //     * 2. partition based on this pivot
    //     * 3. create new tasks and return
    //     * 4. sort the data list and distance array, put data and distances belongs to the same sub-clusters together.
    //     */

    /**
     * 处理一个分区任务。
     * 注意1:在调用此方法之前，应该检查任务是否可以是叶节点。
     * 注意2:如果所有的点都相同，也应该检查任务
     * *1。选择一个最佳支点
     * 2.基于这个枢轴的划分
     * 3.创建新任务并返回
     * 4.对数据列表和距离数组进行排序，将属于同一子集群的数据和距离放在一起。
     *
     * @param task 分区任务
     * @return 新的任务
     */
    private List<PartitionTask> processTask(PartitionTask task)
    {
        final int pivotNum = task.pivot.length;

        double   obj                      = Double.NEGATIVE_INFINITY;  //object function value of partition, the larger the better.
        int      pivot                    = 0;
        double   largestRange             = 0;
        double[] clusterLeftBound         = null;  //inclusive
        double[] clusterRightBound        = null; //inclusive
        double[] clusterFirstOffsetDouble = null;   //inclusive, will cast to integer

        //select pivot to partition with
        double tempR = this.MaxRadius;
        while ((obj == Double.NEGATIVE_INFINITY) && !task.isDone())
        {
            for (int i = 0; i < pivotNum; i++)
            {
                //skip used pivot
                if (task.upper[i] != -1) continue;

                //partition by one pivot, return a 2-d double array.  no empty cluster allowed 
                //0th row: consists of only one element, the objective function value, the larger the better. can be the pruning rate etc.
                //1st row: cluster left bound, left inclusive
                //2nd row: range, left and right
                //3rd row: cluster first offset
                //4th row: cluster right bound, right inclusive
                double[][] result = partitionByOnePivot(i, tempR, task);

                if (obj < result[0][0])
                {
                    obj                      = result[0][0];
                    clusterLeftBound         = result[1];
                    clusterRightBound        = result[4];
                    clusterFirstOffsetDouble = result[3];
                    pivot                    = i;
                }

                if (result[2][0] == result[2][1])
                {
                    task.lower[i] = result[2][0];
                    task.upper[i] = result[2][1];
                } else
                {
                    largestRange = (largestRange > result[2][1] - result[2][0]) ? largestRange : result[2][1] - result[2][0];
                }
            }

            if (obj != Double.NEGATIVE_INFINITY) break;

            tempR = largestRange / 4;

        }

        //partition by the pivot selected
        int[] clusterFirstOffset = new int[clusterFirstOffsetDouble.length];
        for (int i = 0; i < clusterFirstOffset.length; i++)
             clusterFirstOffset[i] = (int) clusterFirstOffsetDouble[i];

        sort(clusterLeftBound, clusterFirstOffset, task, pivot);

        //create partition tasks and then return
        ArrayList<PartitionTask> children = new ArrayList<PartitionTask>(clusterFirstOffset.length);
        for (int i = 0; i < clusterFirstOffset.length; i++)
        {
            //skip empty cluster
            if (((i == clusterFirstOffset.length - 1) && (clusterFirstOffset[i] == task.last)) || ((i < clusterFirstOffset.length - 1) && (clusterFirstOffset[i] == clusterFirstOffset[i + 1])))
                continue;

            double[] l = task.lower.clone();
            double[] u = task.upper.clone();
            l[pivot] = clusterLeftBound[i];
            u[pivot] = clusterRightBound[i];
            if (i == clusterFirstOffset.length - 1) children.add(new PartitionTask(task.data, clusterFirstOffset[i], task.last, task.pivot, l, u));
            else children.add(new PartitionTask(task.data, clusterFirstOffset[i], clusterFirstOffset[i + 1], task.pivot, l, u));
        }

        return children;

    }

    //    /**
    //     * partition by the distances to one pivot, return a double array. no empty cluster allowed
    //     * 0th row consists of only one element, the objective function value, the larger the better. can be the pruning rate etc.
    //     * 1st row: cluster left bound,left inclusive, right exclusive
    //     * 2nd row: range, left and right
    //     * 3rd row: cluster first offset
    //     * 4th row: cluster right bound, left exclusive, right inclusive
    //     *
    //     */

    /**
     * 按到一个主元的距离划分，返回一个双数组。不允许空簇
     * 第0行只包含一个元素，目标函数值越大越好。可以是修剪率等。
     * 第一行:集群左绑定，左包含，右排除
     * 第二行:范围，左边和右边
     * 第三行:集群第一偏移
     * 第4行:集群右界，左排外，右包容
     *
     * @param pivot 支撑点
     * @param R     半径
     * @param task  分区任务
     * @return 分区结果
     */
    double[][] partitionByOnePivot(int pivot, double R, PartitionTask task)
    {
        ArrayList<Histogram.BinInfo> bin = Histogram.completeOneDHistogram(-R / this.HistogramScale / 2, R / this.HistogramScale, this.distance[pivot], task.first, task.last);

        double[][] result = new double[5][];
        result[2] = new double[]{bin.get(0).lower(), bin.get(bin.size() - 1).upper()};

        //return if range is not large enough
        if ((bin.size() < 3) || (bin.get(bin.size() - 2).upper() - bin.get(1).lower()) <= 2 * R)
        {
            partitionSmallRange(result, bin, task);
            return result;
        }

        //range is large enough, find the pruning rate for all the possible 3-partitions.
        //if the size of the 3 clusters are a, b, c, where b has widht 2R, then the pruning rate is:
        //r = 2ac/(a+b+c)^2,  since a+b+c is constant for all partitions, we can just use r=ac for comparison
        int    bestLeftBoundary  = 0;   //the offset of the first bin in the middle part
        int    bestRightBoundary = 0; //the offset of the last bin in the middle part
        int    bestA             = 0, bestB = 0;      //cluster size of the best partition.
        double maxR              = -1;  //the max value of r=ac, for comparison.  the larger the better.

        double a             = 0, b = 0;
        int    rightBoundary = 0;
        for (int leftBoundary = 1; leftBoundary < bin.size() - 1; leftBoundary++)
        {
            if (bin.get(bin.size() - 2).upper() - bin.get(leftBoundary).lower() < 2 * R)//already reach the right ends
                break;

            //compute a
            a = 0;
            for (int i = 0; i < leftBoundary; i++)
                 a += bin.get(i).size();

            //find right boundary
            rightBoundary = leftBoundary;
            b             = bin.get(rightBoundary).size();
            while ((rightBoundary < bin.size() - 2) && ((bin.get(rightBoundary).upper() - bin.get(leftBoundary).lower()) < 2 * R))
            {
                rightBoundary++;
                b += bin.get(rightBoundary).size();
            }

            //already reach the right ends.  already check at the beginning of the loop, just for safety
            if (rightBoundary == bin.size() - 1) break;

            //comparison with the best-so-far
            if (maxR < a * (task.last - task.first - b - a))
            {
                maxR              = a * (task.last - task.first - b - a);
                bestLeftBoundary  = leftBoundary;
                bestRightBoundary = rightBoundary;
                bestA             = (int) a;
                bestB             = (int) b;
            }
        }

        //set the cluster information and return
        result[0] = new double[]{maxR};
        result[1] = new double[3];
        result[3] = new double[3];
        result[4] = new double[3];

        result[1][0] = bin.get(0).lower();
        result[3][0] = task.first;
        result[4][0] = bin.get(bestLeftBoundary - 1).upper();

        result[1][1] = bin.get(bestLeftBoundary).lower();
        result[3][1] = task.first + bestA;
        result[4][1] = bin.get(bestRightBoundary).upper();

        result[1][2] = bin.get(bestRightBoundary + 1).lower();
        result[3][2] = task.first + bestA + bestB;
        result[4][2] = bin.get(bin.size() - 1).upper();

        return result;

    }


    void partitionSmallRange(double[][] result, ArrayList<Histogram.BinInfo> bin, PartitionTask task)
    {
        boolean isDiscrete = true;
        for (Histogram.BinInfo b : bin)
            if (b.upper() != b.lower())
            {
                isDiscrete = false;
                break;
            }

        if (isDiscrete) //if discrete, return each discrete value as a cluster
        {
            result[0] = new double[]{0};
            result[1] = new double[bin.size()];
            for (int i = 0; i < bin.size(); i++)
                 result[1][i] = bin.get(i).lower();
            result[4]    = result[1].clone();
            result[3]    = new double[bin.size()];
            result[3][0] = task.first;
            for (int i = 1; i < bin.size(); i++)
                 result[3][i] = result[3][i - 1] + bin.get(i - 1).size();

        } else
        {
            result[0] = new double[]{Double.NEGATIVE_INFINITY};
            result[1] = new double[]{result[2][0]};
            result[4] = new double[]{result[2][1]};
            result[3] = new double[]{task.first};
        }

    }


    //    /**
    //     * sort the array and list into groups, based on given split values and group sizes
    //     * @param split
    //     * @param count
    //     * @param distance
    //     * @param data
    //     */

    /**
     * 根据给定的分割值和组大小，对数组和列表进行分组
     *
     * @param clusterLeftBound   聚类
     * @param clusterFirstOffset 第一个聚类的便宜
     * @param task               任务
     * @param pivot              支撑点集合
     */
    void sort(double[] clusterLeftBound, int[] clusterFirstOffset, PartitionTask task, int pivot)
    {
        double    temp          = 0;
        int       toCluster     = 0;
        final int clusterNum    = clusterFirstOffset.length;
        int[]     currentOffset = clusterFirstOffset.clone();
        for (int cluster = 0; cluster < clusterNum; cluster++)
        {
            for (; currentOffset[cluster] < ((cluster == clusterNum - 1) ? task.last : clusterFirstOffset[cluster + 1]); currentOffset[cluster]++)
            {
                toCluster = cluster + 1;
                while (toCluster != cluster)
                {
                    //compute tocluster
                    for (toCluster = 0; toCluster < clusterNum - 1; toCluster++)
                    {
                        if (this.distance[pivot][currentOffset[cluster]] < clusterLeftBound[toCluster + 1]) break;
                    }

                    if (toCluster != cluster)  //exchange
                    {
                        Collections.swap(task.data, currentOffset[cluster], currentOffset[toCluster]);

                        for (int i = 0; i < task.pivot.length; i++)
                        {
                            temp                                  = distance[i][currentOffset[cluster]];
                            distance[i][currentOffset[cluster]]   = distance[i][currentOffset[toCluster]];
                            distance[i][currentOffset[toCluster]] = temp;
                        }
                        currentOffset[toCluster]++;
                    }//end of exchange
                }//end of while
            }//end of one cluster
        }

    }


    class PartitionTask
    {
        //Metric metric;
        List<? extends IndexObject> data;  //data to partition
        final int first;  //offset of the first point in the data list, inclusive
        final int last;   //offset of the last point in the data list, exclusive.
        IndexObject[] pivot;  //pivots based on distance to which to partition the data
        double[]      upper;  //upper.length = lower.length = pivot.length.
        double[]      lower;  //upper and lower bounds to used pivots.  computed by previous partition steps.
        //if upper[i] == -1, then pivot[i] is not used yet.

        //        /**
        //         * Constructor of PartitionTask.  Assume no pivots were used
        //         * @param data data to partition, copy by reference
        //         * @param pivot pivots to use, copy by reference
        //         */

        /**
         * PartitionTask的构造函数。假设没有使用支撑点
         *
         * @param data  数据到分区，通过引用复制
         * @param pivot 使用支撑点，复制参考
         */
        public PartitionTask(List<? extends IndexObject> data, IndexObject[] pivot)
        {
            this(data, 0, data.size(), pivot, new double[pivot.length], new double[pivot.length]);
            for (int i = 0; i < pivot.length; i++)
                 upper[i] = -1;
        }

        //        /**
        //         * Constructor of PartitionTask
        //         * @param data data to partition, copy by reference
        //         * @param first offset of the first point in the data list, inclusive
        //         * @param last offset of the last point in the data list, exclusive.
        //         * @param pivot pivots to use, copy by reference
        //         * @param upper upper bounds to used pivots, copy by value
        //         * @param lower lower bounds to used pivots, copy by value
        //         */

        /**
         * 构造函数的PartitionTask
         *
         * @param data  数据到分区，通过引用复制
         * @param first 数据列表中第一个点的第一个偏移量，包括
         * @param last  数据列表中最后一点的偏移量，排他。
         * @param pivot 使用支撑点，复制参考
         * @param lower 使用的数据透视的ower bounds，按值复制
         * @param upper 上上界使用的数据轴，复制值
         */
        public PartitionTask(List<? extends IndexObject> data, int first, int last, IndexObject[] pivot, double[] lower, double[] upper)
        {
            if ((data == null) || (pivot == null) || (upper == null) || (lower == null))
                throw new IllegalArgumentException("Null argument!");

            if (first >= last) throw new IllegalArgumentException("Empty data list to partition!");

            if ((pivot.length != upper.length) || (upper.length != lower.length))
                throw new IllegalArgumentException("Arrays of inconsistent size!");

            //this.metric = metric;
            this.data  = data;
            this.first = first;
            this.last  = last;
            this.pivot = pivot;
            this.upper = upper.clone();
            this.lower = lower.clone();

        }

        //        /**
        //         * check whether there are still pivots to use
        //         * @return true if no pivots to use
        //         */

        /**
         * 检查是否还有可用的枢轴
         *
         * @return 如果没有支点可用，为真
         */
        boolean isDone()
        {
            for (int i = 0; i < upper.length; i++)
                if (upper[i] == -1) return false;
            return true;
        }

        //        /**
        //         * Check whether this partition task is small enough to form a leaf node.
        //         * If yes, compute the range to all unused pivots.
        //         * @param mls maximum leaf node size
        //         * @param distance distances from each data point (column) to each piovt(row)
        //         * @return
        //         */

        /**
         * 检查此分区任务是否小到足以形成叶节点。
         * 如果是，计算所有未使用的枢轴的范围。
         *
         * @param distance 每个数据点(列)到每个piovt(行)的距离
         * @param mls      最大叶节点大小
         * @return 是否是叶子节点
         */
        boolean isLeaf(double[][] distance, int mls)
        {
            if (last - first > mls) return false;

            for (int i = 0; i < pivot.length; i++)
            {
                if (upper[i] != -1) //pivot i is already used
                    continue;

                upper[i] = Double.NEGATIVE_INFINITY;
                lower[i] = Double.POSITIVE_INFINITY;
                for (int j = first; j < last; j++)
                {
                    upper[i] = (upper[i] > distance[i][j]) ? upper[i] : distance[i][j];
                    lower[i] = (lower[i] < distance[i][j]) ? lower[i] : distance[i][j];
                }
            }

            return true;
        }

    }
}
