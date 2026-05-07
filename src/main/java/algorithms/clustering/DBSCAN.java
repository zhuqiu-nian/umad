package algorithms.clustering;

import db.type.DoubleVector;
import db.type.DoubleVectorClusterPair;
import metric.Metric;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * DBSCAN聚类算法。
 *
 * Perform DBSCAN clustering from vector array or distance matrix.
 *
 * DBSCAN - Density-Based Spatial Clustering of Applications with Noise.
 * Finds core samples of high density and expands clusters from them.
 * Good for data which contains clusters of similar density.
 *
 */
public class DBSCAN {
    /* eps—邻域的判断阈值 */
    private double eps = Double.NEGATIVE_INFINITY;
    /* 核心对象的判断阈值 */
    private int            min_samples = Integer.MIN_VALUE;
    /* 聚类样本 */
    private DoubleVector[] data        = null;
    /* 距离函数 */
    private Metric         metric      = null;
    /* 类簇数目 */
    private int            clusterNum = -1;

    /**
     * 构造一个DBSCAN算法的实体类
     * @param data 样本数据集
     *             DataSet
     * @param eps default=0.5
     *         The maximum distance between two samples for one to be considered
     *         as in the neighborhood of the other. This is not a maximum bound
     *         on the distances of points within a cluster. This is the most
     *         important DBSCAN parameter to choose appropriately for your data set
     *         and distance function.
     * @param min_samples default=5
     *         The number of samples (or total weight) in a neighborhood for a point
     *         to be considered as a core point. This includes the point itself.
     * @param metric 计算特征向量之间距离用的距离函数
     */
    public DBSCAN(DoubleVector[] data,double eps, int min_samples, Metric metric) {
        if (data == null) throw new IllegalArgumentException("数据不能为空！");
        if (eps < 0) throw new IllegalArgumentException("eps必须为正数！");
        if (min_samples <= 0) throw new IllegalArgumentException("min_samples必须为正整数！");
        if (metric == null) throw new IllegalArgumentException("距离函数不能为空");
        this.data = data;
        this.eps = eps;
        this.min_samples = min_samples;
        this.metric = metric;
    }

    /**
     * 执行聚类算法。
     * Execute DBSCAN.
     * @return DoubleVectorClusterPair[]
     */
    public DoubleVectorClusterPair[] execute(){
        //获取样本点集的每个点的eps邻域内所有点的下标，neighborhods是一个二维整数型数组，第i行表示第i个样本点的邻居点的下标
        int[][] neighborhods = nerestNeighbors(data, eps, metric);
        //获取每个样本点的邻居数目
        int[] n_neighbors = new int[data.length];
        for (int i = 0; i< data.length; i++){
            n_neighbors[i] = neighborhods[i].length;
        }
        //获取核心节点的下标
        ArrayList<Integer> coreSamples = new ArrayList<Integer>();
        for (int i = 0; i< data.length; i++){
            if (n_neighbors[i] >= min_samples) coreSamples.add(i);
        }
        //初始化聚类标签，所有的样本点都标记成噪声-1
        int[] labels = new int[data.length];
        Arrays.fill(labels, -1);
        //调用内部DBSCAN算法，算法直接更新labels的值
        innerDbscan(coreSamples,neighborhods,labels);
        //将结果包装成DoubleVectorClusterPair返回
        DoubleVectorClusterPair[] result = new DoubleVectorClusterPair[data.length];
        for (int i = 0; i< data.length; i++){
            result[i] = new DoubleVectorClusterPair(data[i], labels[i]);
        }
        //返回结果
        return result;
    }

    /**
     * 执行DBSCAN算法
     * @param coreSamples 存储了核心样本下标索引的集合
     * @param neighborhods int型二维数组，第i行代表第i个样本点的eps-邻域内的点的下标
     * @param labels int型数组，第i个整数代表第i个样本点的类别，噪声点已经被初始化成了-1
     */
    private void innerDbscan(ArrayList<Integer> coreSamples, int[][] neighborhods, int[] labels) {
        int k = -1; //初始化聚类簇数
        //初始化未访问的集合
        ArrayList<Integer> notVisited = new ArrayList<>(data.length);
        for (int i = 0; i< data.length; i++) notVisited.add(i);
        while (!coreSamples.isEmpty()){
            //记录当前未访问样本集合
            ArrayList<Integer> notVisitedCopy = new ArrayList<>(notVisited);
            //随机获取一个核心样本
            int o = coreSamples.remove(coreSamples.size()-1);
            //初始化一个队列
            ArrayDeque<Integer> Q = new ArrayDeque<>();
            Q.addLast(o);
            notVisited.remove(Integer.valueOf(o));
            while (!Q.isEmpty()){
                int q = Q.removeFirst();
                if (neighborhods[q].length >= min_samples){
                    ArrayList<Integer> d = interSection(neighborhods[q], notVisited);
                    Q.addAll(d);
                    notVisited.removeAll(d);
                }
            }
            k = k + 1;
            notVisitedCopy.removeAll(notVisited);
            for (int i : notVisitedCopy){
                labels[i] = k;
            }
            coreSamples.removeAll(notVisitedCopy);
        }
        this.clusterNum = k + 1;
    }

    //求数组和集合的交集，返回同时在数组和集合中出现的元素构成的集合
    private ArrayList<Integer> interSection(int[] neighborhod, ArrayList<Integer> notVisited) {
        ArrayList<Integer> result = new ArrayList<>();
        for (int i=0; i<neighborhod.length; i++){
            if (notVisited.contains(neighborhod[i])) result.add(neighborhod[i]);
        }
        return result;
    }

    //暴力搜索返回eps-邻域情况
    private int[][] nerestNeighbors(DoubleVector[] data, double eps, Metric metric){
        int[][] result = new int[data.length][];

        int[][] neighborhodsFlag = new int[data.length][data.length];
        for (int i=0; i<data.length; i++){
            for (int j=i+1; j<data.length; j++){
                if (metric.getDistance(data[i],data[j]) <= eps){
                    neighborhodsFlag[i][j] = 1;
                    neighborhodsFlag[j][i] = 1;
                }
            }
        }

        for (int i=0; i<data.length; i++){
            ArrayList<Integer> list = new ArrayList<>();
            for (int j=0; j<data.length; j++){
                if (neighborhodsFlag[i][j] == 1)
                    list.add(j);
            }
            result[i] = new int[list.size()];
            int listSize = list.size();
            for (int j=0; j<listSize; j++){
                result[i][j] = list.remove(list.size()-1);
            }
        }
        return result;
    }

    public int getClusterNum() {
        return clusterNum;
    }
}
