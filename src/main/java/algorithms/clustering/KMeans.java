package algorithms.clustering;

import db.type.DoubleVector;
import db.type.DoubleVectorClusterPair;
import metric.LMetric;
import util.Debug;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * KMeans算法
 */
public class KMeans{
    /* 类别数目 */
    private int k = -1;
    /* 最大迭代次数 */
    private int maxIter = -1;
    /* 迭代中心的偏移量的阈值*/
    private double         tol  = -1;
    /* 待聚类的数据 */
    private DoubleVector[] data = null;

    /**
     * 构造函数
     * Constructor
     * @param data 样本集 DataSet
     * @param k 簇数 the number of cluster
     * @param maxIter 最大迭代次数  the max number of iterations
     * @param tol 聚类中心偏移量阈值 cluster Center Offset Threshold
     */
    public KMeans(DoubleVector[] data, int k, int maxIter, double tol) {
        if (data == null || k <=0 || maxIter <=0 || tol <=0) throw new IllegalArgumentException("参数值异常！");
        this.data = data;
        this.k = k;
        this.maxIter = maxIter;
        this.tol = tol;
    }


    /**
     * 执行聚类算法
     * Execute K-Means.
     * @return {@link DoubleVectorClusterPair} 二维数组，第一维是聚类结果，第二维是聚类中心
     *          Two-dimensional array, the first dimension is the clustering result, the second dimension
     *          is the cluster center
     */
    public DoubleVectorClusterPair[][] execute(){
        //初始化聚类中心
        DoubleVectorClusterPair[] centroid = initCentroid(data, k);
        //存储聚类后结果的“对”
        DoubleVectorClusterPair[] dataClusters = new DoubleVectorClusterPair[data.length];

        //初始化数据类别“对”数组
        for(int i = 0; i < dataClusters.length; i++){
            dataClusters[i] = new DoubleVectorClusterPair(data[i], -1);
        }

        //开始迭代
        do {
            updateClassification(dataClusters,centroid);
            DoubleVectorClusterPair[] newCentroid = updateCentroid(dataClusters,k);
            maxIter--;
            if (isStop(maxIter,tol,centroid, newCentroid)){
                centroid = newCentroid;
                break;
            } else {
                centroid = newCentroid;
            }
        }while(true);

        var result = new DoubleVectorClusterPair[2][];
        result[0] = dataClusters;
        result[1] = centroid;
        return result;
    }

    //判断迭代是否结束
    private boolean isStop(int maxIter, double tol, DoubleVectorClusterPair[] centroid, DoubleVectorClusterPair[] newCentroid) {
        if (maxIter==0) return true;
        boolean flag = true;
        for (int i=0; i<centroid.length; i++){
            if (LMetric.EuclideanDistanceMetric.getDistance(centroid[i].getDoubleVector(),
                    newCentroid[i].getDoubleVector()) > tol) flag = false;
        }
        return flag;
    }

    //更新聚类中心
    private DoubleVectorClusterPair[] updateCentroid(DoubleVectorClusterPair[] dataClusters, int n) {
        //每个类簇里点的数目
        int[] numbers = new int[n];
        Arrays.fill(numbers, 0);

        //样本向量的维度
        int dim = dataClusters[0].getDoubleVector().size();

        //存储每个类簇中样本向量的和
        double[][] distances =  new double[n][dim];
        for (int i=0;i<n;i++) Arrays.fill(distances[i],0);

        for (int i=0; i<dataClusters.length; i++){
            int cluster = dataClusters[i].getCluster();
            numbers[cluster]++;
            for (int j=0; j<dim; j++) {
                distances[cluster][j] += dataClusters[i].getDoubleVector().getData()[j];
            }
        }

        for (int i=0; i<n; i++){
            int num = numbers[i];
            for (int j=0; j<dim; j++){
                distances[i][j] /= num;
            }
        }

        DoubleVectorClusterPair[] newCentroid = new DoubleVectorClusterPair[n];

        for (int i=0; i<n; i++) {
            newCentroid[i] = new DoubleVectorClusterPair(new DoubleVector(distances[i]), i);
        }

        if (Debug.debug){
            Logger.getLogger("umad").info("类簇中点的数目：" + Arrays.toString(numbers));
        }

        return newCentroid;
    }

    //更新每个向量的聚类
    private void updateClassification(DoubleVectorClusterPair[] dataClusters, DoubleVectorClusterPair[] centroid) {
        for (int i=0; i<dataClusters.length; i++){
            double dis = Double.POSITIVE_INFINITY;
            for (int j=0; j<centroid.length; j++){
                double distanceFromCentroid = LMetric.EuclideanDistanceMetric.getDistance(
                        dataClusters[i].getDoubleVector(),centroid[j].getDoubleVector());
                if (dis > distanceFromCentroid){
                    dis = distanceFromCentroid;
                    dataClusters[i].setCluster(centroid[j].getCluster());
                }
            }
        }
    }

    //初始化聚类中心
    private DoubleVectorClusterPair[] initCentroid(DoubleVector[] data, int n) {
        Random random = new Random();
        DoubleVectorClusterPair[] initCentroid = new DoubleVectorClusterPair[n];
        int[] num = new int[data.length];
        for (int i=0; i<data.length; i++) num[i]=i;
        int edge = num.length;
        for (int i=0; i<n; i++){
            //产生一个随机数下标
            int index = random.nextInt(edge);
            //用随机下标取一个整数
            int intRandom = num[index];
            initCentroid[i] = new DoubleVectorClusterPair(data[intRandom], i);
            num[index] = edge-1;
            edge--;
        }
        return initCentroid;
    }
}
