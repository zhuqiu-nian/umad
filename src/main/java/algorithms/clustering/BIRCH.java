package algorithms.clustering;

import db.type.DoubleVector;
import db.type.DoubleVectorClusterPair;
import metric.LMetric;
import util.Debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * BIRCH聚类算法。
 *
 * The BIRCH builds a tree called the Clustering Feature Tree (CFT) for the given data. The data is essentially lossy
 * compressed to a set of Clustering Feature nodes (CF Nodes). The CF Nodes have a number of subclusters called
 * Clustering Feature subclusters (CF Subclusters) and these CF Subclusters located in the non-terminal CF Nodes can
 * have CF Nodes as children.
 *
 */
public class BIRCH {
    /* 样本数据集 */
    private DoubleVector[] data = null;
    /* 类别数目 default = 3 */
    private int            k    = -1;
    /* 最小聚类子簇的半径 default  = 0.5 */
    private double threshold = 0.5;
    /* 每个结点的类簇数 ,default = 50 */
    private int branchingFactor = 50;
    /* 样本数据集的大小 */
    private int dataSize = -1;
    /* 样本向量维度 */
    private int dim = -1;
    /* 存储构建好的树的所有的叶子节点 */
    private ArrayList<LeafNode> leafNodes = null;
    /* 存储构建好的树的所有MinCluster节点 */
    private ArrayList<MinCluster> minClusters = null;
    /* 数据聚类后的标签 */
    private int[] labels = null;

    /**
     * BIRCH算法的构造函数
     * @param data 数据集 DataSet
     * @param threshold
     *         The radius of the subcluster obtained by merging a new sample and the
     *         closest subcluster should be lesser than the threshold. Otherwise a new
     *         subcluster is started. Setting this value to be very low promotes
     *         splitting and vice-versa.
     * @param branchingFactor
     *          Maximum number of CF subclusters in each node. If a new samples enters
     *         such that the number of subclusters exceed the branching_factor then
     *         that node is split into two nodes with the subclusters redistributed
     *         in each. The parent subcluster of that node is removed and two new
     *         subclusters are added as parents of the 2 split nodes.
     * @param k the number of cluster
     *          类别数目
     */
    public BIRCH(DoubleVector[] data, double threshold, int branchingFactor, int k) {
        if (data==null || k <0 || threshold<0 || branchingFactor<=1) throw new IllegalArgumentException("参数不正确！");
        this.data = data;
        this.k = k;
        this.threshold = threshold;
        this.branchingFactor = branchingFactor;
        this.dataSize = data.length;
        this.dim = data[0].size();
        this.labels = new int[this.dataSize];
        Arrays.fill(this.labels, -1);
    }

    /**
     * 执行BIRCH算法
     * Execute BIRCH.
     * @return DoubleVectorClusterPair[]包裹着向量和类别
     */
    public DoubleVectorClusterPair[] execute(){
        //开始构建树
        NonLeafNode root = new NonLeafNode(new CF(), null);
        root.addChildren(new LeafNode(new CF(), root));
        for (int i=0; i<dataSize; i++) root.add(i);

        //取构建好的树的所有叶子节点和MinCluster节点
        this.leafNodes = new ArrayList<>();
        this.minClusters = new ArrayList<>();
        findAndSetLeafNodesMinClustersList(root);
        //判断是在leafNodes上进行AGNES聚类，还是在minClusters上进行AGNES聚类
        if (leafNodes.size()==this.k) {
            //叶子节点数等于聚类数目，则直接按照叶子节点对应的类簇标记类别
            for (int i=0; i<this.k;i++){
                ArrayList<MinCluster> children = leafNodes.get(i).getMinClusterChildren();
                for (int j=0; i<children.size(); j++){
                    ArrayList<Integer> dataIndex = children.get(j).getDataIndex();
                    for (var k: dataIndex){
                        labels[k] = i;
                    }
                }
            }
            //标记完毕
        } else if (leafNodes.size() > this.k){
            //叶子节点数目大于聚类数目，在叶子节点集合上执行AGNES聚类算法
            //基本思路是用一个List<CF>存储所有的CF，然后在这个上面进行聚类。对应的用一个List<List<Integer>>存储聚类结果，然后根据结果打
            //标签
            ArrayList<CF> cfs = new ArrayList<>();
            ArrayList<ArrayList<Integer>> dataIndexCluster = new ArrayList<>();
            //遍历所有叶子节点，初始化List<CF>和List<List<Integer>>
            for (int i=0; i<leafNodes.size(); i++){
                cfs.add(i ,leafNodes.get(i).getCf().clone());
                dataIndexCluster.add(i, new ArrayList<Integer>());
                for (int j=0; j<leafNodes.get(i).getMinClusterChildren().size(); j++){
                    dataIndexCluster.get(i).addAll(leafNodes.get(i).getMinClusterChildren().get(j).getDataIndex());
                }
            }
            //所有要用的列表已经初始化好，剩下的就是在cfs上执行聚类，聚类的同时，对应的dataIndexCluster也要修改
            while (cfs.size() > this.k){
                //找到距离最近的两个类簇
                double minDis = Double.POSITIVE_INFINITY;
                int minIndexRight = 1;
                int minIndexLeft = 0;
                for (int i=0; i<cfs.size()-1; i++){
                    for (int j=i+1; j<cfs.size(); j++){
                        var dis = cfs.get(i).getEuclideanDistanceWith(cfs.get(j));
                        if (minDis > dis){
                            minDis = dis;
                            minIndexLeft = i;
                            minIndexRight = j;
                        }
                    }
                }
                //已经拿到最近的两个集群的下标
                //合并最近的两个集群
                cfs.get(minIndexLeft).add(cfs.get(minIndexRight));
                cfs.remove(minIndexRight);
                dataIndexCluster.get(minIndexLeft).addAll(dataIndexCluster.get(minIndexRight));
                dataIndexCluster.remove(minIndexRight);
            }
            //聚类完成，dataIndexCluster内就是聚类结果，按照聚类结果给数据打标签
            for (int i=0 ;i<dataIndexCluster.size(); i++){
                for (var d : dataIndexCluster.get(i)){
                    labels[d] = i;
                }
            }
        } else {
            //叶子节点数目小于聚类数目，尝试在minCluster上执行AGNES
            if (minClusters.size() == this.k){
                //最小类簇单元数目正好等于类别数目，则直接按照对应的最小类簇单元标记类别
                for (int i=0; i<this.k; i++){
                    ArrayList<Integer> dataIndex = minClusters.get(i).getDataIndex();
                    for (var k : dataIndex){
                        labels[k] = i;
                    }
                }
            } else if (minClusters.size() > this.k){
                //最小类簇单元数目大于类别数目，则在最小类簇单元上执行AGNES算法
                //Todo
                //基本思路同上，首先初始化List<CF>和List<List<integer>>
                ArrayList<CF> cfs = new ArrayList<>();
                ArrayList<ArrayList<Integer>> dataIndexCluster = new ArrayList<>();
                //遍历所有MinCluster节点，初始化List<CF>和List<List<Integer>>
                for (int i=0; i<minClusters.size(); i++){
                    cfs.add(i ,minClusters.get(i).getCf().clone());
                    dataIndexCluster.add(i, new ArrayList<Integer>());
                    dataIndexCluster.get(i).addAll(minClusters.get(i).getDataIndex());
                }
                //所有要用的列表已经初始化好，剩下的就是在cfs上执行聚类，聚类的同时，对应的dataIndexCluster也要修改
                while (cfs.size() > this.k){
                    //找到距离最近的两个类簇
                    double minDis = Double.POSITIVE_INFINITY;
                    int minIndexRight = 1;
                    int minIndexLeft = 0;
                    for (int i=0; i<cfs.size()-1; i++){
                        for (int j=i+1; j<cfs.size(); j++){
                            var dis = cfs.get(i).getEuclideanDistanceWith(cfs.get(j));
                            if (minDis > dis){
                                minDis = dis;
                                minIndexLeft = i;
                                minIndexRight = j;
                            }
                        }
                    }
                    //已经拿到最近的两个集群的下标
                    //合并最近的两个集群
                    cfs.get(minIndexLeft).add(cfs.get(minIndexRight));
                    cfs.remove(minIndexRight);
                    dataIndexCluster.get(minIndexLeft).addAll(dataIndexCluster.get(minIndexRight));
                    dataIndexCluster.remove(minIndexRight);
                }
                //聚类完成，dataIndexCluster内就是聚类结果，按照聚类结果给数据打标签
                for (int i=0 ;i<dataIndexCluster.size(); i++){
                    for (var d : dataIndexCluster.get(i)){
                        labels[d] = i;
                    }
                }
            } else {
                throw new IllegalStateException("最终的类簇数目少于k值，请减小k值或减小threshold和branchingFactor！");
            }
        }
        if (Debug.debug) Logger.getLogger("umad").info(Arrays.toString(labels));
        //将标签和数据包装到一起返回
        DoubleVectorClusterPair[] result = new DoubleVectorClusterPair[dataSize];
        for (int i=0; i<dataSize; i++){
            result[i] = new DoubleVectorClusterPair(data[i],labels[i]);
        }
        return result;
    }

    // 遍历聚类特征树，将LeafNode和minCluster加入到相应的列表中
    private void findAndSetLeafNodesMinClustersList(Node root) {
        if (root!=null){
//            if (Debug.debug) System.out.println(root);
            if (root instanceof NonLeafNode){
                //访问非叶子节点
                var node = (NonLeafNode)root;
                var children = node.getChildren();
                for (var c : children){
                    findAndSetLeafNodesMinClustersList(c);
                }
            } else if (root instanceof LeafNode){
                //访问叶子节点
                var node = (LeafNode)root;
                this.leafNodes.add(node);
                var children = node.getMinClusterChildren();
                for (var c : children){
                    findAndSetLeafNodesMinClustersList(c);
                }
            } else {
                //访问类簇节点
                var node = (MinCluster)root;
                this.minClusters.add(node);
                var children = node.getDataIndex();
                if (Debug.debug) Arrays.toString(children.toArray());
            }
        }
    }

    /**
     * 聚类特征基本属性
     *
     */
    private class CF implements Cloneable{
        // data point instance
        private int N;
        // N个节点的线性和
        private DoubleVector LS;
        // N个节点的平方和
        private double SS;

        /**
         * CF的构造函数
         * @param n 数据样本点的个数
         * @param LS 数据样本点的线性和
         * @param SS 数据样本点的平方和
         */
        public CF(int n, DoubleVector LS, double SS) {
            this.N = n;
            this.LS = new DoubleVector(LS.getData().clone());
            this.SS = SS;
        }

        /**
         * 使用样本集自动计算生成CF
         * @param dataIndex 要计算CF的节点数据下标构成的数组
         */
        public CF(int[] dataIndex) {
            this.N = dataIndex.length;
            this.LS = computeLS(dataIndex);
            this.SS = computeSS(dataIndex);
        }

        /**
         * 使用样本点自动计算生成CF
         * @param recordIndex 样本点的下标
         */
        public CF(int recordIndex) {
            var record = data[recordIndex];
            this.N = 1;
            this.LS = new DoubleVector(record.getData().clone());
            double sumOfSS = 0;
            for (int i=0; i<record.getData().length; i++){
                sumOfSS += record.getData()[i] * record.getData()[i];
            }
            this.SS = sumOfSS;

        }

        /**
         * 使用Node自动生成CF
         * @param node node
         */
        public CF(Node node) {
            this.N = node.getCf().getN();
            this.LS = new DoubleVector(node.getCf().getLS().getData().clone());
            this.SS = node.getCf().getSS();
        }

        /**
         * 生成一个全0的特征
         */
        public CF() {
            this.N = 0;
            double[] db = new double[dim];
            Arrays.fill(db, 0);
            this.LS = new DoubleVector(db);
            this.SS = 0;
        }

        public int getN() {
            return N;
        }

        public void setN(int n) {
            N = n;
        }

        public DoubleVector getLS() {
            return new DoubleVector(LS.getData().clone());
        }

        public void setLS(DoubleVector lS) {
            LS = lS;
        }

        public double getSS() {
            return SS;
        }

        public void setSS(double sS) {
            SS = sS;
        }

        /**
         * 根据节点数据计算线性和
         *
         * @param dataIndex
         *            要计算CF的节点数据下标构成的数组
         */
        private DoubleVector computeLS(int[] dataIndex) {
            //数据集大小
            int num = dataIndex.length;
            //初始化求和数组
            var sum = new double[dim];
            Arrays.fill(sum, 0);

            //求线性和
            for (int i = 0; i < num; i++) {
                DoubleVector record = data[dataIndex[i]];
                for (int j = 0; j < dim; j++) {
                    sum[j] += record.getData()[j];
                }
            }
            return new DoubleVector(sum);
        }

        /**
         * 根据节点数据计算平方和
         *
         * @param dataIndex
         *            要计算CF的节点数据下标构成的数组
         */
        private double computeSS(int[] dataIndex) {
            int num = dataIndex.length;
            //初始化平方和变量
            double sumOfSquare = 0;

            for (int i = 0; i < num; i++) {
                double[] record = data[dataIndex[i]].getData();
                for (int j = 0; j < dim; j++) {
                    sumOfSquare += record[j] * record[j];
                }
            }

            return sumOfSquare;
        }

        /**
         * CF加otherCF,该方法会直接修改CF的值，同时把修改后的CF返回
         *
         * @param otherCF 要加的另一个CF
         */
        public CF add(CF otherCF) {
            double[] otherLS = otherCF.getLS().getData().clone();
            var sumOfLS = LS.getData().clone();

            // 3个值在数量上进行叠加
            for (int i = 0; i < otherLS.length; i++) {
                sumOfLS[i] += otherLS[i];
            }
            this.N += otherCF.getN();
            this.SS += otherCF.getSS();
            this.LS = new DoubleVector(sumOfLS);
            return this;
        }

        /**
         * CF加样本向量，该方法会直接修改CF的值，同时把修改后的CF返回
         * @param recordIndex 要加的样本向量的下标
         * @return
         */
        public CF add(int recordIndex) {
            this.add(new CF(recordIndex));
            return this;
        }

        /**
         * 计算这个CF特征的质心
         *      C = LS/N
         * @return
         */
        public  DoubleVector getCentroid(){
            var c = LS.getData().clone();
            for (int i=0; i< c.length; i++){
                c[i] /= this.N;
            }
            return new DoubleVector(c);
        }

        /**
         * 计算这个CF特征的簇半径
         *  R = sqrt((N*SS-LS^2)/N^2)
         */
        public double getRadius(){
            double squareOfLS = 0;
            var ls = this.LS.getData().clone();
            for (int i=0; i<ls.length; i++){
                squareOfLS += ls[i] * ls[i];
            }
            return Math.sqrt((this.N * this.SS - squareOfLS) / (this.N * this.N));
        }

        /**
         * 计算这个CF与另一个CF的欧几里得距离
         * D0 = sqrt((LS1/N1 - LS2/N2)^2)
         * @param otherCf 另一个CF特征值
         * @return double 欧几里得距离
         */
        public double getEuclideanDistanceWith(CF otherCf){
            return LMetric.EuclideanDistanceMetric.getDistance(this.getCentroid(), otherCf.getCentroid());
        }

        /**
         * 计算这个CF与另一个样本点的CF特征值之间的欧几里得距离
         * @param recordIndex 数据样本点的下标
         * @return double 欧几里得距离
         */
        public double getEuclideanDistanceWith(int recordIndex){
            return getEuclideanDistanceWith(new CF(recordIndex));
        }

        @Override
        protected CF clone(){
            return new CF(this.N, new DoubleVector(this.LS.getData().clone()),this.SS);
        }

        @Override
        public String toString() {
            return "CF{" +
                    "N=" + N +
                    ", LS=" + LS +
                    ", SS=" + SS +
                    '}';
        }
    }

    /**
     * CF特征树所有的节点的抽象父类
     */
    private abstract class Node {
        /* 节点的CF属性 */
        private CF cf = null;

        public Node(CF cf) {
            this.cf = cf.clone();
        }

        /**
         * 返回CF值的引用，注意对该值进行修改会影响到原值。
         * @return
         */
        public CF getCf() {
            return cf;
        }

        public void setCf(CF cf) {
            this.cf = cf.clone();
        }

        abstract public void add(int recordIndex);

    }

    /**
     * 非叶子节点
     *
     */
    private class NonLeafNode extends Node {
        // 非叶子节点的孩子节点可能为非叶子节点，也可能为叶子节点
        private ArrayList<Node> children = new ArrayList<>();
        // 父亲节点
        private NonLeafNode parentNode;

        /**
         * 创建一个非叶子节点
         * @param cf
         * @param parentNode
         */
        public NonLeafNode(CF cf, NonLeafNode parentNode) {
            super(cf);
            this.parentNode = parentNode;
        }

        /**
         * 将一个数据样本添加到以该节点为跟的CF树中
         * @param recordIndex 要添加的样本点的索引下表
         */
        @Override
        public void add(int recordIndex) {
            //更新聚类特征值
            this.getCf().add(recordIndex);
            //寻找要插入的位置
            int indexOfMinDistance = 0;
            double minDistance = Double.POSITIVE_INFINITY;
            for (int i =0; i<children.size(); i++){
                double dis = children.get(i).getCf().getEuclideanDistanceWith(recordIndex);
                if (minDistance > dis){
                    minDistance = dis;
                    indexOfMinDistance = i;
                }
            }
            //找到插入的位置之后插入
            children.get(indexOfMinDistance).add(recordIndex);
        }

        /**
         * 添加一个孩子节点，并检查添加的合法性。
         * @param child 要添加的孩子节点
         */
        public void addChildren(Node child){
            children.add(child);
            checkTheLegalityAndDealWithIt();
        }

        private void checkTheLegalityAndDealWithIt() {
            if (children.size() <= k) return;
            //节点孩子数不合法，需要分裂节点
            NonLeafNode[] nonLeafNodes = new NonLeafNode[2];
            //存储孩子节点之间的距离
            double[][] distanceInChildren = new double[children.size()][children.size()];
            /* 计算所有孩子节点之间的距离，并找出距离最大的两个孩子节点 */
            double maxDis = Double.NEGATIVE_INFINITY;
            //存储两个最远的类簇的索引
            int[] index = new int[2];
            for (int i=0; i<children.size()-1; i++){
                for (int j=i+1; j<children.size(); j++){
                    CF CFi = children.get(i).getCf();
                    CF CFj = children.get(j).getCf();
                    double dis = CFi.getEuclideanDistanceWith(CFj);
                    if (maxDis < dis){
                        maxDis = dis;
                        index[0] = i;
                        index[1] = j;
                    }
                    distanceInChildren[i][j] = dis;
                    distanceInChildren[j][i] = dis;
                }
            }
            //已经拿到了所有的孩子之间的距离，也拿到了最远的两个孩子的索引下标
            nonLeafNodes[0] = new NonLeafNode(new CF(children.get(index[0])),this.parentNode);
            nonLeafNodes[0].addChildren(children.get(index[0]));
            nonLeafNodes[1] = new NonLeafNode(new CF(children.get(index[1])), this.parentNode);
            nonLeafNodes[1].addChildren(children.get(index[1]));
            //根据远近划分所有孩子
            for (int i=0; i<children.size(); i++){
                if (i==index[0] || i ==index[1]) continue;
                if (distanceInChildren[index[0]][i] < distanceInChildren[index[1]][i]){
                    nonLeafNodes[0].addChildren(children.get(i));
                    nonLeafNodes[0].getCf().add(children.get(i).getCf());
                } else {
                    nonLeafNodes[1].addChildren(children.get(i));
                    nonLeafNodes[1].getCf().add(children.get(i).getCf());
                }
            }
            //判断该节点是否是根节点，如果是根节点则向下增长，如果不是根节点，则创建一个兄弟节点，然后连接到父节点的位置
            if (this.parentNode == null){
                //该节点是根节点
                this.children = new ArrayList<>();
                this.children.add(nonLeafNodes[0]);
                this.children.add(nonLeafNodes[1]);
                nonLeafNodes[0].parentNode = this;
                nonLeafNodes[1].parentNode = this;
            }else{
                //该节点不是根节点
                this.children = nonLeafNodes[0].children;
                this.setCf(nonLeafNodes[0].getCf());
                this.parentNode.addChildren(nonLeafNodes[1]);
            }
        }

        @Override
        public String toString() {
            return "NonLeafNode{ ChildrenNum:" + children.size() +
                    '\t' + this.getCf() + '}';
        }

        /**
         * 返回孩子列表
         * @return
         */
        public ArrayList<Node> getChildren() {
            return (ArrayList<Node>) children.clone();
        }
    }

    /**
     * CF树叶子节点
     *
     */
    private class LeafNode extends Node {
        // 孩子集群
        private ArrayList<MinCluster> minClusterChildren = new ArrayList<>();
        // 父亲节点
        private NonLeafNode parentNode = null;

        /**
         * 叶子节点的构造函数。
         * @param cf cf特征
         * @param parentNode 指向非叶子父节点的指针
         */
        public LeafNode(CF cf, NonLeafNode parentNode) {
            super(cf);
            this.parentNode = parentNode;
        }

        /**
         * 在当前叶节点下添加一个孩子节点,并检查添加是否合法。
         * @param minCluster
         */
        public void addMinClusterChildren(MinCluster minCluster) {
            this.minClusterChildren.add(minCluster);
            checkTheLegalityAndDealWithIt();
        }

        /**
         * 将叶子节点划分出2个,此方法应该在一个新的数据点已经插入到孩子集群中以后调用。
         *
         * @return 新划分出的叶子节点
         */
        private LeafNode divideLeafNode() {
            LeafNode[] leafNodeArray = new LeafNode[2];
            //存储孩子节点之间的距离
            double[][] distanceInChildren = new double[minClusterChildren.size()][minClusterChildren.size()];
            /* 计算所有孩子节点之间的距离，并找出距离最大的两个孩子节点 */
            double maxDis = Double.NEGATIVE_INFINITY;
            //存储两个最远的类簇的索引
            int[] index = new int[2];
            for (int i=0; i<minClusterChildren.size()-1; i++){
                for (int j=i+1; j<minClusterChildren.size(); j++){
                    CF CFi = minClusterChildren.get(i).getCf();
                    CF CFj = minClusterChildren.get(j).getCf();
                    double dis = CFi.getEuclideanDistanceWith(CFj);
                    if (maxDis < dis){
                        maxDis = dis;
                        index[0] = i;
                        index[1] = j;
                    }
                    distanceInChildren[i][j] = dis;
                    distanceInChildren[j][i] = dis;
                }
            }
            //已经拿到了所有的孩子之间的距离，也拿到了最远的两个孩子的索引下标
            leafNodeArray[0] = new LeafNode(new CF(minClusterChildren.get(index[0])),this.parentNode);
            leafNodeArray[0].addMinClusterChildren(minClusterChildren.get(index[0]));
            leafNodeArray[1] = new LeafNode(new CF(minClusterChildren.get(index[1])), this.parentNode);
            leafNodeArray[1].addMinClusterChildren(minClusterChildren.get(index[1]));
            //根据远近划分所有孩子
            for (int i=0; i<minClusterChildren.size(); i++){
                if (i==index[0] || i ==index[1]) continue;
                if (distanceInChildren[index[0]][i] < distanceInChildren[index[1]][i]){
                    leafNodeArray[0].addMinClusterChildren(minClusterChildren.get(i));
                    leafNodeArray[0].getCf().add(minClusterChildren.get(i).getCf());
                } else {
                    leafNodeArray[1].addMinClusterChildren(minClusterChildren.get(i));
                    leafNodeArray[1].getCf().add(minClusterChildren.get(i).getCf());
                }
            }
            //用其中一个leafNode刷新原本的leafNode的值
            this.minClusterChildren = leafNodeArray[0].minClusterChildren;
            this.setCf(leafNodeArray[0].getCf());
            return leafNodeArray[1];
        }

        /**
         * 检查叶子节点的分支因子的合法性，如果不合法就创建一个兄弟节点，并将兄弟节点挂到父节点下面。
         */
        private void checkTheLegalityAndDealWithIt(){
            if (this.minClusterChildren.size() <= branchingFactor) return;
            var newLeafNode = divideLeafNode();
            this.parentNode.addChildren(newLeafNode);
        }
        /**
         * 将一个数据点添加到以该节点为根的CF树中
         * @param recordIndex 要添加的数据点
         */
        @Override
        public void add(int recordIndex) {
            //更新聚类特征值
            this.getCf().add(recordIndex);
            //寻找应该插入的位置,孩子不为空的时候
            if (!minClusterChildren.isEmpty()) {
                int indexOfMinDistance = 0;
                double minDistance = minClusterChildren.get(0).getCf().getEuclideanDistanceWith(recordIndex);
                for (int i=1; i<minClusterChildren.size(); i++) {
                    double distance = minClusterChildren.get(i).getCf().getEuclideanDistanceWith(recordIndex);
                    if (minDistance > distance){
                        minDistance = distance;
                        indexOfMinDistance = i;
                    }
                }
                //找到之后将数据插入该位置,具体的能否插入是minCluster的问题
                minClusterChildren.get(indexOfMinDistance).add(recordIndex);
            } else {
                //如果孩子数组为空，则新建minCluster，并将数据插入.
                MinCluster minCluster = new MinCluster(new CF(recordIndex), recordIndex, this);
                minClusterChildren.add(minCluster);
            }
        }

        @Override
        public String toString() {
            return "LeafNode{ ChildrenNum:" + minClusterChildren.size() +
                    '\t' + this.getCf() + "}";
        }

        public ArrayList<MinCluster> getMinClusterChildren() {
            return (ArrayList<MinCluster>) minClusterChildren.clone();
        }
    }

    /**
     * 叶子节点中的小集群
     *
     */
    private class MinCluster extends Node{
        //集群中的数据点
        private ArrayList<Integer> dataIndex = new ArrayList<>();
        //父亲节点
        private LeafNode parentNode;

        /**
         * @param cf CF特征值
         * @param recordIndex 数据索引的下标
         * @param parentNode 父节点指针
         */
        public MinCluster(CF cf, int recordIndex, LeafNode parentNode) {
            super(cf);
            this.dataIndex.add(recordIndex);
            this.parentNode = parentNode;

        }


        public ArrayList<Integer> getDataIndex() {
            return dataIndex;
        }

        @Override
        public void add(int recordIndex) {
            //执行插入数据操作
            //首先判断插入后的半径是否超过threshold
            CF cf = new CF(this);
            cf.add(recordIndex);
            double r = cf.getRadius();
            if (r < threshold) {
                //执行插入操作
                this.dataIndex.add(recordIndex);
                //更新CF值
                this.getCf().add(recordIndex);
            } else {
                //新建一个minCluster，和this连接到同一个叶子节点下
                MinCluster minCluster = new MinCluster(new CF(recordIndex), recordIndex, this.parentNode);
                this.parentNode.addMinClusterChildren(minCluster);
            }
        }

        @Override
        public String toString() {
            return "MinCluster{ ClusterSize:" + dataIndex.size() + '\t' +
                    this.getCf() + Arrays.toString(dataIndex.toArray()) +"}";
        }
    }


    // 单元测试
    public static void main(String[] args){
        DoubleVector[] data = new DoubleVector[10];
        for (int i=0; i<10;i++){
            var dd = new double[3];
            Arrays.fill(dd, 2);
            data[i] = new DoubleVector(dd);
        }
        BIRCH birch = new BIRCH(data, 0.5, 50, 3);
    }
}

