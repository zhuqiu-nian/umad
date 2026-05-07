package app;

import algorithms.clustering.BIRCH;
import algorithms.clustering.DBSCAN;
import algorithms.clustering.KMeans;
import db.type.DoubleVector;
import db.type.DoubleVectorClusterPair;
import db.type.IndexObject;
import metric.LMetric;
import metric.Metric;
import util.Debug;
import util.Embed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.System.exit;

/**
 * 聚类算法的执行类。
 * The execution class of the clustering algorithm.
 * 使用该类可以执行聚类算法，导出聚类结果，评价聚类结果。
 * Use this class to perform clustering algorithms, output clustering results, and evaluate clustering results.
 *
 */
public class Clustering
{
    /* 待聚类的数据 */
    private DoubleVector[] data     = null;
    /* 聚类数据集的大小 */
    private int            dataSize = -1;
    /* Davies-Bouldin指标 */
    private Double                    daviesBouldin   = Double.NEGATIVE_INFINITY;
    /* 聚类结果 */
    private DoubleVectorClusterPair[] resultOfCluster = null;
    /* 类簇数目 */
    private int                       clusterNum      = -1;
    /* 每个类簇的质心 */
    private DoubleVectorClusterPair[] centroid = null;

    /**
     * 使用{@link DoubleVector}构造聚类实例
     * Use {@link DoubleVector} to construct a clustering instance
     * @param data {@link DoubleVector}
     */
    public Clustering(DoubleVector[] data) {
        this.data = data;
        this.dataSize = data.length;
    }

    /**
     * 使用{@link List}构造聚类实例
     * Use {@link List} to construct a clustering instance
     * @param data {@link List}
     */
    public Clustering(List<? extends IndexObject> data) {
        this.data = data.toArray(new DoubleVector[0]);
        this.dataSize = data.size();
    }

    /**
     * 使用double[][]数组初始化
     * Use double[][] to construct a clustering instance
     * @param data data
     */
    public Clustering(double[][] data) {
        this.dataSize = data.length;
        this.data = new DoubleVector[this.dataSize];
        for (int i=0; i<dataSize; i++){
            this.data[i] = new DoubleVector(data[i]);
        }
    }

    /**
     * 使用默认参数执行k-means聚类算法，最大迭代次数默认为300,迭代中心偏移量阈值默认为1e-4
     * Use the default parameters to execute the k-means clustering algorithm, the maximum number of
     * iterations defaults to 300, and the iteration center offset threshold defaults to 1e-4
     * @param k the Number Of Categories To Cluster
     */
    public void executeKMeans(int k) {
        executeKMeans(k, 300, 1e-4);
    }

    /**
     * 执行k-means聚类算法
     * execute the k-means clustering algorithm.
     * @param k 要聚类的类别数目
     *          the Number Of Categories To Cluster
     * @param maxIter 最大迭代次数
     *                the maximum number of iterations
     * @param tol 迭代中心的偏移量的阈值
     *            the iteration center offset threshold
     */
    public void executeKMeans(int k, int maxIter, double tol){
        KMeans kMeans = new KMeans(data ,k, maxIter, tol);
        var    result = kMeans.execute();
        this.clusterNum = k;
        this.resultOfCluster = result[0];
        this.centroid = result[1];
        if (Debug.debug){
            Logger logger = Logger.getLogger("umad");
            logger.info(Arrays.toString(result[1]));
        }
    }

    /**
     * 默认参数执行DBSCAN，eps=0.5,min_samples=5
     * The default parameters execute DBSCAN, eps=0.5, min_samples=5
     * @param metric metric
     */
    public void executeDBSCAN(Metric metric){
        DBSCAN dbscan = new DBSCAN(data, 0.5, 5, metric);
        resultOfCluster = dbscan.execute();
        this.clusterNum = dbscan.getClusterNum();
    }

    /**
     * execute DBSCAN Algorithm
     * @param eps default=0.5
     *         The maximum distance between two samples for one to be considered
     *         as in the neighborhood of the other. This is not a maximum bound
     *         on the distances of points within a cluster. This is the most
     *         important DBSCAN parameter to choose appropriately for your data set
     *         and distance function.
     * @param min_samples default=5
     *         The number of samples (or total weight) in a neighborhood for a point
     *         to be considered as a core point. This includes the point itself.
     * @param metric distance Function Used To Calculate The istance Between Feature Vectors
     */
    public void executeDBSCAN(double eps, int min_samples, Metric metric){
        DBSCAN dbscan = new DBSCAN(data, eps, min_samples, metric);
        resultOfCluster = dbscan.execute();
        this.clusterNum = dbscan.getClusterNum();
    }

    /**
     * 使用默认参数执行BIRCH，threshold=0.5，branchingFactor=50
     * Execute BIRCH with default parameters, threshold=0.5, branchingFactor=50
     * @param k k
     */
    public void executeBIRCH(int k){
        BIRCH birch = new BIRCH(data, 0.5, 50, k);
        resultOfCluster = birch.execute();
        this.clusterNum = k;
    }

    /**
     * 执行BIRCH
     * Execute BIRCH
     * @param threshold cluster Radius
     * @param branchingFactor branching Factor
     * @param k number Of Clusters To Be Clustered
     */
    public void executeBIRCH(double threshold, int branchingFactor, int k){
        BIRCH birch = new BIRCH(data, threshold, branchingFactor, k);
        resultOfCluster = birch.execute();
        this.clusterNum = k;
    }

    /**
     * 获取样本集
     * Get the DataSet.
     * @return 样本集 samples dataSet
     */
    public DoubleVector[] getData() {
        return data;
    }

    /**
     * 获取样本集大小
     * Get the Size of DataSet.
     * @return 样本集大小
     */
    public int getDataSize(){
        return dataSize;
    }

    /**
     * 获取类簇数目
     * Get the number of Cluster.
     * @return 类簇数目
     */
    public int getClusterNum(){
        return clusterNum;
    }

    /**
     * 将聚类结果写入到txt文档中
     * Write the clustering results into a txt file
     * <p>
     * 文件的第一行分别是 样本个数 样本维数 样本类簇数
     * The first line of the file is the number of samples, the number of sample dimensions,
     * and the number of sample clusters.
     * 后面的数据格式为” /t“为向量内部的分隔符，最后一个分量是类别，它的后面没有分隔符，只有”/n“
     * The following data format is "/t" is the separator inside the vector, the last component is the category,
     * there is no separator after it, only "/n"
     *
     * @param filePath filePath to save
     * @throws IOException IOException
     */
    public void writeToTxt(String filePath) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(new File(filePath)));
        writer.write(resultOfCluster[0].getDoubleVector().size() + "\t" + dataSize + "\t" +
                clusterNum + "\n");
        for (var r : resultOfCluster) {
            double[] doubleVector = r.getDoubleVector().getData();
            int cluster = r.getCluster();
            for (int j = 0; j < doubleVector.length; j++) {
                writer.write(doubleVector[j] + "\t");
            }
            writer.write(cluster + "\n");
        }
        writer.close(); //等待写完
    }


    /**
     * 获取样本聚类结果的DB指数(Davies-Nouldin Index)。
     * Get the DB index of the sample clustering result (Davies-Nouldin Index)
     *
     * 样本聚类结果的DB指数表示簇内差异与簇间差异的比值，这个比值越大，聚类效果越差，反之则越好。因使用欧式距离所以对于环状分布聚类评测很差。
     * The DB index of the sample clustering result indicates the ratio of the difference between clusters and
     * the difference between clusters. The larger the ratio, the worse the clustering effect, and vice versa.
     * @return Davies-Nouldin Index
     */
    public Double getDaviesBouldin(){
        int k = getClusterNum();
        DoubleVectorClusterPair[] centroid = getCentroid();

        //求第i类样本到其质心的平均距离
        int[] numbers = new int[k];  //存储每类样本的数目
        Arrays.fill(numbers, 0);
        double[] dis = new double[k];  //存储每类样本到其质心的距离和
        for (var c : getClusterResult()){
            int _cluster = c.getCluster();
            if (_cluster == -1) continue;  //噪声点不考虑
            numbers[_cluster]++;
            dis[_cluster] += LMetric.EuclideanDistanceMetric
                    .getDistance(c.getDoubleVector(), centroid[_cluster].getDoubleVector());
        }
        for (int i=0; i<k; i++){
            dis[i] /= numbers[i];
        }

        //计算DB指数
        double dbi = 0;
        for (int i=0; i<k; i++){
            double max = 0;
            for (int j=i+1; j<k; j++){
                double temp = ( dis[i] + dis[j] ) / LMetric.EuclideanDistanceMetric.getDistance(
                        centroid[i].getDoubleVector(), centroid[j].getDoubleVector());
                if (max < temp) max = temp;
            }
            dbi += max;
        }
        dbi /= k;
        if (Debug.debug){
            Logger.getLogger("umad").info("dbi值是：" + dbi);
        }
        return dbi;
    }


    /**
     * 获取聚类结果
     * get the result of clustering
     * @return {@link DoubleVectorClusterPair}
     */
    public DoubleVectorClusterPair[] getClusterResult() {
        if (resultOfCluster == null) throw new IllegalStateException("请先对数据进行聚类！");
        return resultOfCluster;
    }

    /**
     * 获取聚类结果中的每个类簇的质心
     * get the centroids
     * @return {@link DoubleVectorClusterPair}
     */
    public DoubleVectorClusterPair[] getCentroid(){
        if(centroid != null) return centroid;
        if (this.resultOfCluster == null || this.clusterNum == -1)
            throw new IllegalStateException("请先执行一个聚类算法！");
        //每个类簇里点的数目
        int[] numbers = new int[clusterNum];
        Arrays.fill(numbers, 0);

        //样本向量的维度
        int dim = resultOfCluster[0].getDoubleVector().size();

        //存储每个类簇中样本向量的和
        double[][] distances =  new double[clusterNum][dim];
        for (int i=0;i<clusterNum;i++) Arrays.fill(distances[i],0);

        for (int i = 0; i< resultOfCluster.length; i++){
            int cluster = resultOfCluster[i].getCluster();
            if (cluster == -1) continue;
            numbers[cluster]++;
            for (int j=0; j<dim; j++) {
                distances[cluster][j] += resultOfCluster[i].getDoubleVector().getData()[j];
            }
        }

        for (int i=0; i<clusterNum; i++){
            int num = numbers[i];
            for (int j=0; j<dim; j++){
                distances[i][j] /= num;
            }
        }

        DoubleVectorClusterPair[] newCentroid = new DoubleVectorClusterPair[clusterNum];

        for (int i=0; i<clusterNum; i++) {
            newCentroid[i] = new DoubleVectorClusterPair(new DoubleVector(distances[i]), i);
        }

        if (Debug.debug){
            Logger.getLogger("umad").info("类簇中点的数目：" + Arrays.toString(numbers));
        }
        centroid = newCentroid;
        return centroid;
    }

}
