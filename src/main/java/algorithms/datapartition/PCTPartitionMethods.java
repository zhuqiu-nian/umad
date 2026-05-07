package algorithms.datapartition;

import db.type.IndexObject;
import index.structure.PCTPartitionResults;
import index.structure.PartitionResults;
import metric.LMetric;
import metric.Metric;

import java.util.*;

/**
 * PCT索引树支持的划分方法的枚举类。
 */
public enum PCTPartitionMethods implements PartitionMethod
{
    /**
     * 构建索引时的K-Means的最大迭代此处maxIter=500，聚类中心的最小移动距离是10^-4.
     */
    KMEANS
            {

                //K-Means聚类划分的最大迭代次数,默认最大迭代500次
                private int maxIter = 500;;
                //K-Means聚类划分的聚类中心的最小移动距离，默认最小移动距离为10^-4
                private double tol = 1e-4;

                /**
                 * 执行划分操作
                 *
                 * @param metric        划分使用的距离函数
                 * @param pivotSet        划分使用的支撑点集合
                 * @param data          待划分的数据集
                 * @param numPartitions 划分的块数
                 * @param maxLS         建树的叶子结点的最大大小
                 * @return 划分结果
                 */
                @Override
                public PCTPartitionResults partition(Metric metric, IndexObject[] pivotSet, List<? extends IndexObject> data, int numPartitions, int maxLS)
                {
                    //由于K-Means聚类采用的距离函数是L2距离，所以传入的metric被忽略，聚类时不使用这个参数。
                    //先将所有数据点映射到支撑点之中
                    int numPivot = pivotSet.length;
                    double[][] coordinates = new double[data.size()][numPivot];  //存储映射后的坐标
                    for (int i = 0; i < data.size(); i++)   //遍历数据集
                    {
                        for (int j = 0; j < pivotSet.length; j++) //遍历支撑点
                        {
                            coordinates[i][j] = metric.getDistance(data.get(i), pivotSet[j]);
                        }
                    }

                    //在映射后的支撑点空间执行聚类操作
                    KMeans kMeans = new KMeans(coordinates, numPartitions, maxIter, tol);
                    kMeans.execute();
                    //拿到聚类结果和聚类中心
                    int[]      tag       = kMeans.getTag();
                    double[][] centroids = kMeans.getCentroids();
                    // 存储该块内的点的坐标，在每个维度的最大值和最小值
                    double[][] maxCoordinationSingleDim = new double[numPartitions][numPivot];
                    double[][] minCoordinationSingleDim = new double[numPartitions][numPivot];
                    for (int i = 0; i < numPartitions; i++)
                    {
                        Arrays.fill(maxCoordinationSingleDim[i], Double.MIN_VALUE);
                        Arrays.fill(minCoordinationSingleDim[i], Double.MAX_VALUE);
                    }
                    // 存储最大半径
                    double[]   radius    = new double[numPartitions];
                    //初始化划分结果列表
                    List<List<? extends IndexObject>> subDataList = new LinkedList<>();
                    //按照聚类结果划分数据
                    List<List<IndexObject>> indexObjectList = new LinkedList<>();
                    for (int i = 0; i < numPartitions; i++)
                         indexObjectList.add(new ArrayList<>());
                    //添加到对应的结果集
                    for (int i = 0; i < tag.length; i++)
                    {
                        IndexObject indexObject = data.get(i);
                        int         cluster     = tag[i];
                        indexObjectList.get(cluster).add(indexObject);
                        //计算该点和其聚类中心的欧式距离
                        double dis = LMetric.EuclideanDistanceMetric.getDistance(coordinates[i], centroids[cluster]);
                        //更新每个维度的最大值和最小值
                        for (int j = 0; j < numPivot; j++)
                        {
                            maxCoordinationSingleDim[cluster][j] = Math.max(maxCoordinationSingleDim[cluster][j], coordinates[i][j]);
                            minCoordinationSingleDim[cluster][j] = Math.min(minCoordinationSingleDim[cluster][j], coordinates[i][j]);
                        }
                        // 更新最大半径
                        radius[cluster] = Math.max(radius[cluster], dis);
                        //判断该点到其他的聚类中心的欧式距离，如果出现相等的，则该点还要被划到其他的块中
                        for (int j = 0; j < numPartitions; j++)
                        {
                            if (j == cluster)
                                continue;
                            if (LMetric.EuclideanDistanceMetric.getDistance(coordinates[i], centroids[j]) == dis)
                            {
                                indexObjectList.get(j).add(indexObject);
                            }
                        }
                    }
                    //转移结果块
                    for (int i = 0; i < numPartitions; i++)
                    {
                        subDataList.add(indexObjectList.get(i));
                    }

                    return new PCTPartitionResults(subDataList, pivotSet, centroids, maxCoordinationSingleDim,
                            minCoordinationSingleDim, radius);
                }

                /**
                 * 执行划分操作
                 *
                 * @param metric        划分使用的距离函数
                 * @param pivots        划分使用的支撑点集合
                 * @param data          待划分的数据集
                 * @param first         数据的起始位置
                 * @param size          数据大小
                 * @param numPartitions 划分的块数
                 * @param maxLS         建树的叶子结点的最大大小
                 * @return 划分结果
                 */
                @Override
                public PCTPartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int first, int size, int numPartitions, int maxLS)
                {
                    throw new UnsupportedOperationException("不支持的操作！");
                }
            },

    /**
     * PAM(Partitioning Around Medoids)是K-Medoids的具体实现。其对比K-Means算法每次选择聚类中心的时候从已有的数据点中选择，这样可以降低
     * 对离群点的敏感性。每次迭代的时候每个中心点都使用其他所有的点代替一次，然后进行聚类计算损失。取损失小的结果作为迭代之后的中心。PAM算法利用了
     * 贪心策略，不一定保证最优解，但是比穷举搜索要快。
     */
    PAM
            {
                //PAM聚类划分的最大迭代次数,默认最大迭代500次
                private int maxIter = 500;;
                /**
                 * 执行划分操作
                 *
                 * @param metric        划分使用的距离函数
                 * @param pivotSet        划分使用的支撑点集合
                 * @param data          待划分的数据集
                 * @param numPartitions 划分的块数
                 * @param maxLS         建树的叶子结点的最大大小
                 * @return 划分结果
                 */
                @Override
                public PartitionResults partition(Metric metric, IndexObject[] pivotSet, List<? extends IndexObject> data, int numPartitions, int maxLS)
                {
                    //由于PAM聚类采用的距离函数是L2距离，所以传入的metric被忽略，聚类时不使用这个参数。
                    //先将所有数据点映射到支撑点之中
                    int numPivot = pivotSet.length;
                    double[][] coordinates = new double[data.size()][numPivot];  //存储映射后的坐标
                    for (int i = 0; i < data.size(); i++)   //遍历数据集
                    {
                        for (int j = 0; j < pivotSet.length; j++) //遍历支撑点
                        {
                            coordinates[i][j] = metric.getDistance(data.get(i), pivotSet[j]);
                        }
                    }

                    //在映射后的支撑点空间执行聚类操作
                    PAM pam = new PAM(coordinates, numPartitions, maxIter);
                    pam.execute();
                    //拿到聚类结果和聚类中心
                    int[]      tag       = pam.getTag();
                    double[][] centroids = pam.getCentroids();
                    // 存储该块内的点的坐标，在每个维度的最大值和最小值
                    double[][] maxCoordinationSingleDim = new double[numPartitions][numPivot];
                    double[][] minCoordinationSingleDim = new double[numPartitions][numPivot];
                    for (int i = 0; i < numPartitions; i++)
                    {
                        Arrays.fill(maxCoordinationSingleDim[i], Double.MIN_VALUE);
                        Arrays.fill(minCoordinationSingleDim[i], Double.MAX_VALUE);
                    }
                    // 存储最大半径
                    double[]   radius    = new double[numPartitions];
                    //初始化划分结果列表
                    List<List<? extends IndexObject>> subDataList = new LinkedList<>();
                    //按照聚类结果划分数据
                    List<List<IndexObject>> indexObjectList = new LinkedList<>();
                    for (int i = 0; i < numPartitions; i++)
                         indexObjectList.add(new ArrayList<>());
                    //添加到对应的结果集
                    for (int i = 0; i < tag.length; i++)
                    {
                        IndexObject indexObject = data.get(i);
                        int         cluster     = tag[i];
                        indexObjectList.get(cluster).add(indexObject);
                        //计算该点和其聚类中心的欧式距离
                        double dis = LMetric.EuclideanDistanceMetric.getDistance(coordinates[i], centroids[cluster]);
                        //更新每个维度的最大值和最小值
                        for (int j = 0; j < numPivot; j++)
                        {
                            maxCoordinationSingleDim[cluster][j] = Math.max(maxCoordinationSingleDim[cluster][j], coordinates[i][j]);
                            minCoordinationSingleDim[cluster][j] = Math.min(minCoordinationSingleDim[cluster][j], coordinates[i][j]);
                        }
                        // 更新最大半径
                        radius[cluster] = Math.max(radius[cluster], dis);
                        //判断该点到其他的聚类中心的欧式距离，如果出现相等的，则该点还要被划到其他的块中
                        for (int j = 0; j < numPartitions; j++)
                        {
                            if (j == cluster)
                                continue;
                            if (LMetric.EuclideanDistanceMetric.getDistance(coordinates[i], centroids[j]) == dis)
                            {
                                indexObjectList.get(j).add(indexObject);
                            }
                        }
                    }
                    //转移结果块
                    for (int i = 0; i < numPartitions; i++)
                    {
                        subDataList.add(indexObjectList.get(i));
                    }

                    return new PCTPartitionResults(subDataList, pivotSet, centroids, maxCoordinationSingleDim,
                            minCoordinationSingleDim, radius);
                }

                /**
                 * 执行划分操作
                 *
                 * @param metric        划分使用的距离函数
                 * @param pivots        划分使用的支撑点集合
                 * @param data          待划分的数据集
                 * @param first         数据的起始位置
                 * @param size          数据大小
                 * @param numPartitions 划分的块数
                 * @param maxLS         建树的叶子结点的最大大小
                 * @return 划分结果
                 */
                @Override
                public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int first, int size, int numPartitions, int maxLS)
                {
                    throw new UnsupportedOperationException("不支持的操作！");
                }
            };

    /**
     * 抽象KMeans算法
     */
    private abstract class AbstractKMeans
    {
        /* 类别数目 */
        private int        k;
        /* 待聚类的数据 */
        private double[][] data;
        /* 聚类结果 */
        private int[]      tag;

        /* 聚类中心 */
        private double[][] centroids;

        /**
         * 构造函数
         * Constructor
         *
         * @param data 样本集
         * @param k    簇数
         */
        public AbstractKMeans(double[][] data, int k)
        {
            if (data == null || k <= 0)
                throw new IllegalArgumentException("参数值异常！");
            this.data = data;
            this.k    = k;
        }

        //执行聚类算法
        public final void execute()
        {
            //如果要初始化的聚类中心个数大于数据数目，则全体数据作为数据中心返回
            if (k >= data.length){
                this.centroids = data.clone();
                return;
            }

            //初始化聚类中心
            double[][] centroid = initCentroid(data, k);


            //开始迭代
            int iterNum = 0;
            do
            {
                this.tag = updateClassification(data, centroid);
                double[][] newCentroid = updateCentroid(data, tag, k);
                iterNum++;
                if (isStop(iterNum, centroid, newCentroid))
                {
                    break;
                } else
                {
                    centroid = newCentroid;
                }
            } while (true);
            this.centroids = centroid;
        }

        /**
         * 初始化聚类中心的方法，默认是随机初始化的方式。
         *
         * @param data 待初始化的数据集
         * @param k    要初始化的中心的个数
         * @return
         */
        public double[][] initCentroid(double[][] data, int k)
        {
            Random     random       = new Random();
            double[][] initCentroid = new double[k][];
            //初始化一个下标数组，一会使用产生的随机数从这个数组中取下标，然后将使用过的下标移到数组的最后，边界减少1，接着取随机数
            //该方法是避免产生的随机数与之前的重复
            int[] num = new int[data.length];
            for (int i = 0; i < data.length; i++)
                 num[i] = i;
            //随机数边界
            int edge = data.length;
            //存储初始化的聚类中心的下标
            int[] initIndex = new int[k];
            for (int i = 0; i < k; i++)
            {
                //产生一个随机数下标
                int index = random.nextInt(edge);
                //用随机下标取一个整数
                int intRandom = num[index];
                initCentroid[i] = data[intRandom];
                initIndex[i]    = intRandom;
                num[index]      = num[edge - 1];
                edge--;
            }
            //调用初始化后函数
            afterInitCentroid(initIndex);
            return initCentroid;
        }

        /**
         * 按照聚类中心更新每个向量的类别
         *
         * @param data
         * @param centroid
         */
        public int[] updateClassification(double[][] data, double[][] centroid)
        {
            int[] tag = new int[data.length];
            //初始化数据类别
            Arrays.fill(tag, -1);
            //遍历每个数据
            for (int i = 0; i < data.length; i++)
            {
                double dis = Double.POSITIVE_INFINITY;
                //遍历每个聚类中心
                for (int j = 0; j < centroid.length; j++)
                {
                    double distanceFromCentroid = LMetric.EuclideanDistanceMetric.getDistance(data[i], centroid[j]);
                    if (dis > distanceFromCentroid)
                    {
                        //记录最小的距离
                        dis = distanceFromCentroid;
                        //分类到j
                        tag[i] = j;
                    }
                }
            }
            return tag;
        }

        /**
         * 更新聚类中心
         */
        public abstract double[][] updateCentroid(double[][] data, int[] tag, int k);

        /**
         * 判断迭代是否结束
         *
         * @param iterNum     已经迭代的次数
         * @param centroid    旧的聚类中心
         * @param newCentroid 新的聚类中心
         * @return true 表示迭代结束 false 表示迭代未结束
         */
        public abstract boolean isStop(int iterNum, double[][] centroid, double[][] newCentroid);

        /**
         * 初始化聚类中心之后调用该函数
         *
         * @param initIndex 初始化的聚类中心的下标
         */
        public void afterInitCentroid(int[] initIndex)
        {
            return;
        }

        /**
         * 返回聚类标签，类别从0开始
         *
         * @return 聚类标签
         */
        public final int[] getTag()
        {
            return tag;
        }

        /**
         * 返回聚类结束时的聚类中心
         *
         * @return 聚类结束时的聚类中心
         */
        public final double[][] getCentroids()
        {
            return centroids;
        }

    }

    /**
     * KMeans的最原始的迭代实现版本
     */
    private class KMeans extends AbstractKMeans
    {
        /* 最大迭代次数 */
        private int    maxIter;
        /* 迭代中心的偏移量的阈值*/
        private double tol;

        /**
         * 构造函数
         * Constructor
         *
         * @param data 样本集
         * @param k    簇数
         */
        public KMeans(double[][] data, int k, int maxIter, double tol)
        {
            super(data, k);
            this.maxIter = maxIter;
            this.tol     = tol;
        }

        /**
         * 更新聚类中心
         *
         * @param data
         * @param tag
         * @param k
         * @return
         */
        @Override
        public double[][] updateCentroid(double[][] data, int[] tag, int k)
        {
            //每个类簇里点的数目，Java会自动初始化为0
            int[] numbers = new int[k];

            //样本向量的维度
            int dim = data[0].length;

            //存储每个类簇中样本向量的和,java会自动初始化为0
            double[][] distances = new double[k][dim];

            //遍历样本集，求每个类簇中的样本的和
            for (int i = 0; i < data.length; i++)
            {
                //获取样本的类别
                int cluster = tag[i];
                numbers[cluster]++;
                //求向量和
                for (int j = 0; j < dim; j++)
                {
                    distances[cluster][j] += data[i][j];
                }
            }

            //使用每个类簇样本的和除以其中的样本数目，得到新的聚类中心
            for (int i = 0; i < k; i++)
            {
                int num = numbers[i];
                //每个向量维度都要取平均值
                for (int j = 0; j < dim; j++)
                {
                    distances[i][j] /= num;
                }
            }
            return distances;
        }

        /**
         * 判断迭代是否结束
         *
         * @param iterNum     已经迭代的次数
         * @param centroid    旧的聚类中心
         * @param newCentroid 新的聚类中心
         * @return true 表示迭代结束 false 表示迭代未结束
         */
        @Override
        public boolean isStop(int iterNum, double[][] centroid, double[][] newCentroid)
        {
            if (maxIter == iterNum)
                return true;
            for (int i = 0; i < centroid.length; i++)
            {
                if (LMetric.EuclideanDistanceMetric.getDistance(centroid[i], newCentroid[i]) > tol)
                {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * K-Medoids的PAM版本
     */
    private class PAM extends AbstractKMeans
    {
        /* 最大迭代次数 */
        private int   maxIter;
        /* 存储聚类中心在data中的下标 */
        private int[] centroidIndex;

        /**
         * 构造函数
         * Constructor
         *
         * @param data 样本集
         * @param k    簇数
         */
        public PAM(double[][] data, int k, int maxIter)
        {
            super(data, k);
            this.maxIter = maxIter;
        }

        /**
         * 初始化聚类中心之后调用该函数
         *
         * @param initIndex 初始化的聚类中心的下标
         */
        @Override
        public void afterInitCentroid(int[] initIndex)
        {
            this.centroidIndex = initIndex;
        }

        /**
         * 更新聚类中心
         *
         * @param data
         * @param tag
         * @param k
         */
        @Override
        public double[][] updateCentroid(double[][] data, int[] tag, int k)
        {
            double[][] newCentroid = new double[k][];
            for (int i = 0; i < k; i++)
            {
                int curCenterIndex = centroidIndex[i];
                //存储最小代价和最小代价对应的聚类中心的下标
                double minCost = 0;
                int minCostIndex = curCenterIndex;
                for (int j = 0; j < data.length; j++)
                {
                    //用每一个非代表点去试着替换代表点
                    //1. 先判断j是不是代表点，如果是则继续下次循环
                    boolean isCenter = false;
                    for (int l = 0; l < k; l++)
                    {
                        if (centroidIndex[l]==j) {
                            isCenter = true;
                            break;
                        }
                    }
                    //2. 如果j是代表点，则继续下次循环
                    if (isCenter) continue;
                    //3. j不是代表点，可以用j来代替curCenterIndex了, 开始计算替换之后的代价
                    int cost = 0;
                    for (int l = 0; l < data.length; l++)
                    {
                        //1. 计算数据点l到新聚类中心j和其到原来聚类中心的距离
                        double disOfj = LMetric.EuclideanDistanceMetric.getDistance(data[l],data[j]);
                        double disOld = LMetric.EuclideanDistanceMetric.getDistance(data[l], data[tag[l]]);
                        //2. 代价等于d(x,新的聚类中心)-d(x,旧的聚类中心)求和
                        cost += disOfj - disOld;
                    }
                    //5. 替换之后的代价计算完成，如果小于最小代价，则存储这个结果
                    if (cost<minCost){
                        minCost = cost;
                        minCostIndex = j;
                    }
                }
                //存储最小的代价的下标
                curCenterIndex = minCostIndex;
                centroidIndex[i] = curCenterIndex;
                newCentroid[i] = data[curCenterIndex].clone();
            }
            return newCentroid;
        }

        /**
         * 判断迭代是否结束
         *
         * @param iterNum     已经迭代的次数
         * @param centroid    旧的聚类中心
         * @param newCentroid 新的聚类中心
         * @return true 表示迭代结束 false 表示迭代未结束
         */
        @Override
        public boolean isStop(int iterNum, double[][] centroid, double[][] newCentroid)
        {
            //达到最大迭代次数，则迭代终止
            if (iterNum == maxIter)
                return true;
            //聚类中心不再移动，则迭代终止
            for (int i = 0; i < centroid.length; i++)
            {
                for (int j = 0; j < centroid[0].length; j++)
                {
                    if (centroid[i][j] != newCentroid[i][j])
                        return false;
                }
            }
            return true;
        }
    }
}
