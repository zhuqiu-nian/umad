package app;

import algorithms.datapartition.*;
import algorithms.pivotselection.PivotSelectionMethod;
import algorithms.pivotselection.PivotSelectionMethods;
import db.TableManager;
import db.table.*;
import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.search.Cursor;
import index.type.HierarchicalPivotSelectionMode;
import metric.LMetric;
import metric.Metric;
import util.Embed;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.exit;
import static util.Util.listEnumNames;

/**
 * 整个软件包的入口类，负责输出面向用户友好的提示，以及将用户的命令转换调用系统响应的组件。
 */
public class Application
{
    // 法向量候选集，
    // 在完全线性划分树中，
    // 需要从各种不同的来源获得法向量候选集，
    // 默认该变量的默认值为"1111"，
    // 变量的四个字符分别代表法向量候选集来源为PCA+VP+CGH+球面上均匀向量，
    // 若字符为'1'，则说明把该来源的法向量放入候选集，若字符为'0'，则不放入候选集，
    private static String normalVectorCandidateSource = "1111";
    public static String getNormalVectorCandidateSource(){                   //例如"1111"，表明法向量候选集都用上了
        return normalVectorCandidateSource;
    }

    // 是否采用基于正交程度的法向量候选集筛选算法,
    // 在完全线性划分树中,
    // 需要对法向量候选集中的法向量进行重新组合，将会获得大量的法向量组，
    // 利用这些法向量组分别对数据进行完全线性划分获得划分结果，从中选出使得划分结果划分排除率最大的法向量组作为最优完全线性划分，
    // 在这个过程中，利用大量的法向量组对数据进行完全线性划分是十分消耗构建开销的，
    // 故本文提出了基于正交程度的法向量候选集筛选算法以降低索引的构建开销，
    // 默认该变量为"true"，即采用该筛选算法进行建树
    private static String isFilterNormalVectorSet = "true";
    public static String getIsFilterNormalVectorSet() {
        return isFilterNormalVectorSet;
    }

    // 是否采用基于查询集r-邻域扩展的精准排除率算法，
    // 在完全线性划分树中，
    // 选择划分排除率最大的法向量组作为最终用于划分结果，
    // 在求划分排除率的过程中，一般认为查询集与数据集一致，而在实际情况中，查询集的大小应该是数据集往外再扩展半径r的范围，
    // 默认该变量为"true"，即采用该r-邻域扩展的精准排除率算法，
    private static String isQuerySetExtensionR = "true";

    public static String getIsQuerySetExtensionR() {
        return isQuerySetExtensionR;
    }


    // 是否利用包含规则进行查询优化,
    // 在VP划分中，除了根据范围查询区域是否跟结点有交集来判断是否继续向下进行搜索外，
    // 还可能有范围查询区域包含了结点的可能，此时可以直接将该结点作为结果返回,
    // 同理，该方式也可能会出现在任意完全线性划分中，该变量就是用来表示是否采用该优化,
    // 默认该变量为"true",即利用该包含规则进行查询优化,
    private static String isSearchOptimization = "true";
    public static String getIsSearchOptimization(){return isSearchOptimization;}

    private static String tableManagerName;
    private static String dataFileName;
    private static int maxDataSize;
    private static int dim = 2;
    private static String indexType = "vp";
    private static PivotSelectionMethod pivotSelectionMethod = PivotSelectionMethods.FFT;
    private static int pivotNum = 3;

    public static String getDataType() {
        return dataType;
    }

    private static String dataType = "vector";
    private static PartitionMethod partitionMethod;
    private static int partitionBlockNum = 3;
    private static int maxLeafSize = 10;
    private static HierarchicalPivotSelectionMode mode = HierarchicalPivotSelectionMode.LOCAL;
    private static String pivotFileName;
    private static String tableName;
    private static String queryFileName;
    private static String queryMode = "single";
    private static int queryDataSize = 1;
    private static Double queryRadius = 0.03;
    private static String clusterMethod = "kmeans";
    private static Integer kCluster = 3;
    private static Integer maxIter = 300;
    private static Double tol = 1e-4;
    private static Double eps = 0.5;
    private static Integer minSamples = 5;
    private static String metric = "euclidean";
    private static Double threshold = 0.5;
    private static Integer factor = 50;

    public static transient List<? extends IndexObject> globalData;             //全局数据集

    /**
     * 整个程序的入口函数
     * -bi (buildIndex) 以构建索引模式启动
     * -qi (queryIndex) 以搜索索引模式启动
     * -c  (cluster) 以聚类分析模式启动
     * -t  (tool) 以度量空间分析工具类模式启动
     * @param args
     */
    public static void main(String[] args) {


        long startTime = System.currentTimeMillis();
        printWelcome();
        if (args.length<1) {
            System.out.println("输入参数有误！输入-h查看帮助！");
            System.exit(-1);
        }
        String mode = args[0];
        String line = null;
        String[] subArgs = null;

        // 支持命令行直接传递参数: -bi "params" 或 -qi "params"
        if (args.length >= 2 && !args[1].startsWith("-")) {
            line = args[1];
            subArgs = line.split(" ");
        } else {
            Scanner scanner = new Scanner(System.in);
            switch (mode.toLowerCase()){
                case "-bi":
                    System.out.println("当前位于构建索引模式，可用参数为：");
                    promptForBuildIndex();
                    System.out.println("请按照要求输入参数(空格分隔，回车键提交)：\n");
                    line = scanner.nextLine();
                    subArgs = line.split(" ");
                    break;
                case "-qi":
                    System.out.println("当前位于查询索引模式，可用参数为：");
                    promptForIndexQuery();
                    System.out.println("请按照要求输入参数(空格分隔，回车键提交)：\n");
                    line = scanner.nextLine();
                    subArgs = line.split(" ");
                    break;
                case "-c":
                    System.out.println("当前位于聚类分析模式，可用参数为：");
                    promptForCluster();
                    System.out.println("请按照要求输入参数(空格分隔，回车键提交)：\n");
                    line = scanner.nextLine();
                    subArgs = line.split(" ");
                    break;
                case "-t":
                    System.out.println("通过该工具可以将数据映射到支撑点空间之中！");
                    promptForTool();
                    System.out.println("请按照要求输入参数(空格分隔，回车键提交)：\n");
                    line = scanner.nextLine();
                    subArgs = line.split(" ");
                    break;
                case "-h":
                    modePrompt();
                    break;
                default:
                    System.out.println("输入参数有误！");
                    modePrompt();
                    System.exit(-1);
            }
        }

        switch (mode.toLowerCase()){
            case "-bi":
                parseBuildIndexArgs(subArgs);
                executeIndexBuilder();
                break;
            case "-qi":
                parseIndexQueryArgs(subArgs);
                executeIndexQuery();
                break;
            case "-c":
                parseClusterArgs(subArgs);
                executeCluster();
                break;
            case "-t":
                parseToolArgs(subArgs);
                executeTool();
                break;
            case "-h":
                modePrompt();
                break;
            default:
                System.out.println("输入参数有误！");
                modePrompt();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("耗时共： " + (endTime - startTime) + "ms");
    }

    private static void executeCluster()
    {
        String psm = ((PivotSelectionMethods)pivotSelectionMethod).name();
        //组合输出目录
        var paths = dataFileName.split("\\\\");
        StringBuffer stringBuffer = new StringBuffer(paths[0]);
        for (int i=1; i<paths.length-1; i++) {
            stringBuffer.append("\\" + paths[i]);
        }
        String[] split = paths[paths.length - 1].split("\\.");
        stringBuffer.append("\\" + split[0] + "_" + pivotNum + psm + "_" + clusterMethod + ".txt");
        String outFilePath = new String(stringBuffer);

        //判断参数是否合法，并初始化相关实例
        if (maxDataSize <=0 ){
            System.out.println("n值不能小于0！");
            exit(-1);
        }
        if (maxDataSize <= kCluster) {
            System.out.println("n值不能小于聚类数k！");
            exit(-1);
        }
        if (dim <= 0) {
            System.out.println("数据维度必须为正数！");
            exit(-1);
        }
        if (pivotNum <= 0) {
            System.out.println("支撑点个数必须为正数！");
            exit(-1);
        }
        Table table = null;
        try
        {
            switch (dataType) {
                case "protein":
                    table = new PeptideTable(dataFileName,"cluster",maxDataSize,dim);
                    break;
                case "dna":
                    table = new DNATable(dataFileName, "cluster", maxDataSize, dim);
                    break;
                case "vector":
                    table = new DoubleVectorTable(dataFileName, "cluster", maxDataSize, dim);
                    break;
                case "image":
                    table = new ImageTable(dataFileName, "cluster", maxDataSize);
                    break;
                case "mass":
                    table = new SpectraTable(dataFileName, "cluster", maxDataSize, null);
                    break;
                default:
                    System.out.println("数据类型参数错误，必须是[protein、dna、vector、image、mass]中的一个！");
            }
            if (kCluster <= 0) {
                System.out.println("类别k必须为正数！");
                exit(-1);
            }
            if (maxIter < 300) {
                System.out.println("最大迭代次数至少为300！");
                exit(-1);
            }
            if (tol > 1e-4) {
                System.out.println("聚类的最小移动距离必须小于1e-4");
                exit(-1);
            }
            if (eps <= 0) {
                System.out.println("eps必须是一个大于0的数！");
                exit(-1);
            }
            if (minSamples <= 0) {
                System.out.println("minSamples必须是一个大于0的整数！");
                exit(-1);
            }
            if (threshold <= 0) {
                System.out.println("threshold必须是一个大于0的数！");
                exit(-1);
            }
            if (factor <= 0) {
                System.out.println("factor必须是一个大于0的整数！");
                exit(-1);
            }
            Metric _metric = null;
            switch (metric) {
                case "manhattan":
                    _metric = LMetric.ManhattanDistanceMetric;
                    break;
                case "euclidean":
                    _metric = LMetric.EuclideanDistanceMetric;
                    break;
                case "infinity":
                    _metric = LMetric.LInfinityDistanceMetric;
                    break;
                default:
                    System.out.println("距离函数输入错误，可选值有manhattan、euclidean、infinity！");
                    exit(-1);
            }

            //执行相应的聚类算法
            Embed embed = new Embed();
            embed.embedToPivotSpace(table,
                    pivotSelectionMethod.selectPivots(table.getMetric(), table.getData(), pivotNum));
            Clustering clustering = new Clustering(embed.getCoordinates());
            switch (clusterMethod) {
                case "kmeans":
                    clustering.executeKMeans(kCluster, maxIter, tol);
                    break;
                case "dbscan":
                    clustering.executeDBSCAN(eps, minSamples, _metric);
                    break;
                case "birch":
                    clustering.executeBIRCH(threshold, factor, kCluster);
                    break;
                default:
                    System.out.println("暂时聚类方法只支持：kmeans、dbscan、birch中的一种！");
                    exit(-1);
            }
            clustering.writeToTxt(outFilePath);
            System.out.println("聚类完成，输出文件为：" + outFilePath);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void parseClusterArgs(String[] args)
    {
        //开始解析参数
        for (int i=0; i<args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "-f":
                    dataFileName = args[++i];
                    break;
                case "-n":
                    maxDataSize = Integer.valueOf(args[++i]);
                    break;
                case "-d":
                    dim = Integer.valueOf(args[++i]);
                    break;
                case "-psm":
                    pivotSelectionMethod = PivotSelectionMethods.valueOf(args[++i].toUpperCase());
                    break;
                case "-pn":
                    pivotNum = Integer.valueOf(args[++i]);
                    break;
                case "-t":
                    dataType = args[++i];
                    break;
                case "-m":
                    clusterMethod = args[++i];
                    break;
                case "-k":
                    kCluster = Integer.valueOf(args[++i]);
                    break;
                case "-maxiter":
                    maxIter = Integer.valueOf(args[++i]);
                    break;
                case "-tol":
                    tol = Double.valueOf(args[++i]);
                    break;
                case "-eps":
                    eps = Double.valueOf(args[++i]);
                    break;
                case "-minsamples":
                    minSamples = Integer.valueOf(args[++i]);
                    break;
                case "-metric":
                    metric = args[++i];
                    break;
                case "-threshold":
                    threshold = Double.valueOf(args[++i]);
                    break;
                case "-factor":
                    factor = Integer.valueOf(args[++i]);
                case "-h":
                    System.out.println("帮助文档：");
                    promptForCluster();
                    exit(-1);
                default:
                    System.out.println("参数出现错误，请参考以下设定：");
                    promptForCluster();
                    exit(-1);
            }
        }
    }

    private static void executeTool()
    {
        TableManager tableManager = TableManager.getTableManager(tableManagerName);
        String outputFilePath = null;
        try
        {
            outputFilePath = UMADTool.mappingToPivotSpace(tableManager, dataType, dataFileName, dim, maxDataSize, pivotSelectionMethod, pivotNum);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        System.out.println("映射成功，结果保存在" + outputFilePath);
        tableManager.close();
    }

    private static void parseToolArgs(String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            switch (args[i].toLowerCase()){
                case "-tm":
                    tableManagerName = args[++i];
                    break;
                case "-t":
                    dataType = args[++i];
                    break;
                case "-f":
                    dataFileName = args[++i];
                    break;
                case "-n":
                    maxDataSize = Integer.valueOf(args[++i]);
                    break;
                case "-d":
                    dim = Integer.valueOf(args[++i]);
                    break;
                case "-psm":
                    pivotSelectionMethod = PivotSelectionMethods.valueOf(args[++i].toUpperCase());
                    break;
                case "-pn":
                    pivotNum = Integer.valueOf(args[++i]);
                    break;
                case "-h":
                    promptForTool();
                    break;
                default:
                    System.out.println("输入参数有误！");
                    promptForTool();
                    System.exit(-1);
            }
        }
    }

    private static void promptForTool()
    {
        System.out.println(
                "     * -tm [tableManager名称]\n" +
                        "     * -t (type)[数据类型：ms、msms、dns、rna、protein、vector、image，默认t=vector]\n" +
                        "     * -f (file)[数据文件的路径]\n" +
                        "     * -n (numbers)[数据文件中数据的个数]\n" +
                        "     * -d (dim)[数据的维度]\n" +
                        "     * -psm (pivot selection method)[支撑点选择方法：" + listEnumNames(PivotSelectionMethods.class) + ",默认psm=fft]\n" +
                        "     * -pn (pivot number)[支撑点个数，默认pn=3]\n" +
                        "     * -h (help)[帮助文档]"
        );
    }
    public static double distance;
    public static double averageExclusionRate;
    private static void executeIndexQuery()
    {
        TableManager tableManager = TableManager.getTableManager(tableManagerName);
        try
        {
            switch (queryMode.toLowerCase()){
                case "single":
                    Cursor cursor = IndexQuery.processRangeQuery(tableManager, tableName, queryFileName, queryRadius);
                    int disCal = cursor.getDisCounter();
                    double averageExclusionRate = cursor.averageExclusionRate;
                    int numberOfInternalNodeSearches = cursor.numberOfInternalNodeSearches;
                    int sizeOfTheResult = cursor.remainingSizeOfTheResult();
                    System.out.println("本次搜索共有" + sizeOfTheResult + "条结果；进行了"+ disCal +"次距离计算；叶子节点搜索了"
                            + numberOfInternalNodeSearches + "次；总得平均排除率为"+averageExclusionRate+ "\n查询结果如下：");
                    while (cursor.hasNext()){
                        System.out.println(cursor.next());
                    }
                    break;
                case "batch":
                    List<DoubleIndexObjectPair> doubleIndexObjectPairs = IndexQuery.batchProcessRangeQuery(tableManager, tableName, queryFileName, queryDataSize, queryRadius);
                    System.out.println("本次搜索一共" + doubleIndexObjectPairs.size() + "条结果：");
                    //doubleIndexObjectPairs.forEach(System.out::println);
                    System.out.println();
                    System.out.println("总距离计算次数:" + distance);
                    System.out.println("平均距离计算次数:" + distance / queryDataSize);
                    System.out.println("平均划分排除率：" + Application.averageExclusionRate / queryDataSize);
                    writeData(doubleIndexObjectPairs.size(), distance, tableName, queryRadius, Application.averageExclusionRate);
                    break;
                default:
                    System.out.println("查询模式有误！");
                    System.exit(-1);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }finally
        {
            tableManager.close();
        }
    }

    private static void parseIndexQueryArgs(String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            switch (args[i].toLowerCase()){
                case "-tm":
                    tableManagerName = args[++i];
                    break;
                case "-tbn":
                    tableName = args[++i];
                    break;
                case "-qf":
                    queryFileName = args[++i];
                    break;
                case "-m":
                    queryMode = args[++i];
                    break;
                case "-qn":
                    queryDataSize = Integer.valueOf(args[++i]);
                    break;
                case "-r":
                    queryRadius = Double.valueOf(args[++i].toUpperCase());
                    break;
                case "-h":
                    promptForIndexQuery();
                    break;
                case "-issearchoptimization":
                    isSearchOptimization = String.valueOf(args[++i]);
                    break;
                default:
                    System.out.println(args[i].toLowerCase());
                    System.out.println("输入参数有误！");
                    promptForIndexQuery();
                    System.exit(-1);
            }
        }
    }

    private static void promptForIndexQuery()
    {
        System.out.println(
                        "     * -tm (table manager)[tableManager名称]\n" +
                        "     * -tbn (table name)[索引名称]\n" +
                        "     * -qf (query file)[查询文件的路径]\n" +
                        "     * -m (mode)[查询模式：single、batch，默认single]\n" +
                        "     * -qn (query number)[查询文件读入的数据量]\n" +
                        "     * -r (radius)[查询半径，默认r=0.1]\n" +
                        "     * -h (help)[帮助文档]\n" +
                        "     * -issearchmod [是否进行范围查询优化，默认为true]"
        );
    }

    private static void printWelcome()
    {
        System.out.println("  _    _   __  __              _____  ");
        System.out.println(" | |  | | |  \\/  |     /\\     |  __ \\ ");
        System.out.println(" | |  | | | \\  / |    /  \\    | |  | |");
        System.out.println(" | |  | | | |\\/| |   / /\\ \\   | |  | |");
        System.out.println(" | |__| | | |  | |  / ____ \\  | |__| |");
        System.out.println("  \\____/  |_|  |_| /_/    \\_\\ |_____/ ");
        System.out.println("                                      ");
    }

    private static void executeIndexBuilder()
    {
        TableManager tableManager = TableManager.getTableManager(tableManagerName);
        String indexName = "";
        try
        {
            switch (indexType.toLowerCase()){
                case "vp":
                    switch (dataType.toLowerCase()){
                        case "ms":
                            indexName = IndexBuilder.buildVPIndexOnMS(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,
                                    pivotNum, (VPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "msms":
                            indexName = IndexBuilder.buildVPIndexOnMsMs(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,
                                    pivotNum, (VPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "dna":
                            indexName = IndexBuilder.buildVPIndexOnDNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,
                                    pivotNum, (VPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "rna":
                            indexName =  IndexBuilder.buildVPIndexOnRNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,
                                    pivotNum, (VPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "protein":
                            indexName = IndexBuilder.buildVPIndexOnProtein(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,
                                    pivotNum, (VPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "vector":
                            indexName = IndexBuilder.buildVPIndexOnVector(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,
                                    pivotNum, (VPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "image":
                            indexName = IndexBuilder.buildVPIndexOnImage(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,
                                    pivotNum, (VPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "english":
                            indexName = IndexBuilder.buildVPIndexOnEnglish(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,
                                    pivotNum, (VPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        default:
                            System.out.println("不支持的数据类型！\n");
                            System.exit(-1);
                    }
                    break;
                case "cp":
                    switch (dataType.toLowerCase()){
                        case "ms":
                            indexName = IndexBuilder.buildCPIndexOnMS(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,
                                    pivotNum, (CPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "msms":
                            indexName = IndexBuilder.buildCPIndexOnMsMs(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,
                                    pivotNum, (CPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "dna":
                            indexName = IndexBuilder.buildCPIndexOnDNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,
                                    pivotNum, (CPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "rna":
                            indexName =  IndexBuilder.buildCPIndexOnRNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,
                                    pivotNum, (CPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "protein":
                            indexName = IndexBuilder.buildCPIndexOnProtein(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,
                                    pivotNum, (CPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "vector":
                            indexName = IndexBuilder.buildCPIndexOnVector(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,
                                    pivotNum, (CPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "image":
                            indexName = IndexBuilder.buildCPIndexOnImage(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,
                                    pivotNum, (CPPartitionMethods)partitionMethod, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        default:
                            System.out.println("不支持的数据类型！\n");
                            System.exit(-1);
                    }
                    break;
                case "gh":
                    switch (dataType.toLowerCase()){
                        case "ms":
                            indexName = IndexBuilder.buildGHIndexOnMS(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,
                                   (GHPartitionMethods)partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "msms":
                            indexName = IndexBuilder.buildGHIndexOnMsMs(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,
                                    (GHPartitionMethods)partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "dna":
                            indexName = IndexBuilder.buildGHIndexOnDNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,
                                    (GHPartitionMethods)partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "rna":
                            indexName = IndexBuilder.buildGHIndexOnRNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,
                                    (GHPartitionMethods)partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "protein":
                            indexName =  IndexBuilder.buildGHIndexOnProtein(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,
                                    (GHPartitionMethods)partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "vector":
                            indexName = IndexBuilder.buildGHIndexOnVector(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,
                                    (GHPartitionMethods)partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "image":
                            indexName = IndexBuilder.buildGHIndexOnImage(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,
                                    (GHPartitionMethods)partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "english":
                            indexName = IndexBuilder.buildGHIndexOnEnglish(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,
                                    (GHPartitionMethods)partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        default:
                            System.out.println("不支持的数据类型！\n");
                            System.exit(-1);
                    }
                    break;
                case "pct":
                    switch (dataType.toLowerCase()){
                        case "ms":
                            indexName = IndexBuilder.buildPCTIndexOnMS(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,(PCTPartitionMethods) partitionMethod,
                                    pivotNum, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "msms":
                            indexName = IndexBuilder.buildPCTIndexOnMsMs(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,(PCTPartitionMethods) partitionMethod,
                                    pivotNum, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "dna":
                            indexName = IndexBuilder.buildPCTIndexOnDNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(PCTPartitionMethods) partitionMethod,
                                    pivotNum, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "rna":
                            indexName = IndexBuilder.buildPCTIndexOnRNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(PCTPartitionMethods) partitionMethod,
                                    pivotNum, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "protein":
                            indexName = IndexBuilder.buildPCTIndexOnProtein(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(PCTPartitionMethods) partitionMethod,
                                    pivotNum, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "vector":
                            indexName = IndexBuilder.buildPCTIndexOnVector(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(PCTPartitionMethods) partitionMethod,
                                    pivotNum, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "image":
                            indexName = IndexBuilder.buildPCTIndexOnImage(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,(PCTPartitionMethods) partitionMethod,
                                    pivotNum, partitionBlockNum,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        default:
                            System.out.println("不支持的数据类型！\n");
                            System.exit(-1);
                    }
                    break;
                case "gnat":
                    switch (dataType.toLowerCase()){
                        case "ms":
                            indexName = IndexBuilder.buildGNATIndexOnMS(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,pivotNum,(GNATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "msms":
                            indexName = IndexBuilder.buildGNATIndexOnMsMs(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,pivotNum,(GNATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "dna":
                            indexName = IndexBuilder.buildGNATIndexOnDNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,pivotNum,(GNATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "rna":
                            indexName = IndexBuilder.buildGNATIndexOnRNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,pivotNum,(GNATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "protein":
                            indexName = IndexBuilder.buildGNATIndexOnProtein(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,pivotNum,(GNATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "vector":
                            indexName = IndexBuilder.buildGNATIndexOnVector(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,pivotNum,(GNATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        case "image":
                            indexName = IndexBuilder.buildGNATIndexOnImage(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,pivotNum,(GNATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode, pivotFileName);
                            break;
                        default:
                            System.out.println(dataType + "不支持的数据类型！\n");
                            System.exit(-1);
                    }
                    break;
                case "at":
                    switch (dataType.toLowerCase()){
                        case "ms":
                            indexName = IndexBuilder.buildApollonianIndexOnMS(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,(ApollonianPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "msms":
                            indexName = IndexBuilder.buildApollonianIndexOnMsMs(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,(ApollonianPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "dna":
                            indexName = IndexBuilder.buildApollonianIndexOnDNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(ApollonianPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "rna":
                            indexName = IndexBuilder.buildApollonianIndexOnRNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(ApollonianPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "protein":
                            indexName = IndexBuilder.buildApollonianIndexOnProtein(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(ApollonianPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "vector":
                            indexName = IndexBuilder.buildApollonianIndexOnVector(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(ApollonianPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "image":
                            indexName = IndexBuilder.buildApollonianIndexOnImage(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,(ApollonianPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "english":
                            indexName = IndexBuilder.buildApollonianIndexOnEnglish(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,(ApollonianPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        default:
                            System.out.println(dataType + "不支持的数据类型！\n");
                            System.exit(-1);
                    }
                    break;
                case "iat":
                    switch (dataType.toLowerCase()){
                        case "ms":
                            indexName = IndexBuilder.buildIATIndexOnMS(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,(IATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "msms":
                            indexName = IndexBuilder.buildIATIndexOnMsMs(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,(IATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "dna":
                            indexName = IndexBuilder.buildIATIndexOnDNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(IATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "rna":
                            indexName = IndexBuilder.buildIATIndexOnRNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(IATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "protein":
                            indexName = IndexBuilder.buildIATIndexOnProtein(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(IATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "vector":
                            indexName = IndexBuilder.buildIATIndexOnVector(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(IATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "image":
                            indexName = IndexBuilder.buildIATIndexOnImage(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,(IATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "english":
                            indexName = IndexBuilder.buildIATIndexOnEnglish(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,(IATPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        default:
                            System.out.println(dataType + "涓嶆敮鎸佺殑鏁版嵁绫诲瀷锛乗n");
                            System.exit(-1);
                    }
                    break;
                case "rgh":
                    switch (dataType.toLowerCase()){
                        case "vector":
                            indexName = IndexBuilder.buildRGHIndexOnVector(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(RGHPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "dna":
                            indexName = IndexBuilder.buildRGHIndexOnDNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(RGHPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "rna":
                            indexName = IndexBuilder.buildRGHIndexOnRNA(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(RGHPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "protein":
                            indexName = IndexBuilder.buildRGHIndexOnProtein(tableManager,dataFileName,
                                    dim,maxDataSize, pivotSelectionMethod,(RGHPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "image":
                            indexName = IndexBuilder.buildRGHIndexOnImage(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,(RGHPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        case "english":
                            indexName = IndexBuilder.buildRGHIndexOnEnglish(tableManager,dataFileName,
                                    maxDataSize, pivotSelectionMethod,(RGHPartitionMethods) partitionMethod,
                                    maxLeafSize, mode);
                            break;
                        default:
                            System.out.println(dataType + "不支持的数据类型！\n");
                            System.exit(-1);
                    }
                    break;
                default:
                    System.out.println(indexType + "不支持的索引类型！\n");
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }finally
        {
            tableManager.close();
        }
        System.out.println("索引构建完成，索引名字为：" + indexName);
    }

    private static void promptForBuildIndex()
    {
        System.out.println(
                        "     * -tm (tableManager)[tableManager名称]\n" +
                        "     * -f (file)[数据文件的路径]\n" +
                        "     * -n (numbers)[数据文件中数据的个数]\n" +
                        "     * -d (dim)[数据的维度]\n" +
                        "     * -i (index)[索引的类型：vp、gh、gnat、pct、cp、at、iat、rgh,默认i=vp]\n" +
                        "     * -psm (pivot select method)[支撑点选择方法：" + listEnumNames(PivotSelectionMethods.class) + ",默认psm=fft]\n" +
                        "     * -pn (pivot numbers)[支撑点个数，默认pn=3，对于GH树等支撑点个数固定的索引结构，该参数无效，对于AT和RGH索引固定为2]\n" +
                        "     * -t (type)[数据类型：ms、msms、dna、protein、vector、image、english，默认t=vector]\n" +
                        "     * -pm (partition method)[划分方法：\n" +
                        "     *                        vp索引树："+ listEnumNames(VPPartitionMethods.class) + "\n" +
                        "     *                        gh索引树："+ listEnumNames(GHPartitionMethods.class) + "\n" +
                        "     *                        gnat索引树："+ listEnumNames(GNATPartitionMethods.class) + "\n" +
                        "     *                        pct索引树："+ listEnumNames(PCTPartitionMethods.class) + "\n" +
                        "     *                        cp索引树："+ listEnumNames(CPPartitionMethods.class) + "\n" +
                        "     *                        at索引树："+ listEnumNames(ApollonianPartitionMethods.class) + "\n" +
                        "     *                        iat索引树："+ listEnumNames(IATPartitionMethods.class) + "\n" +
                        "     *                        rgh索引树："+ listEnumNames(RGHPartitionMethods.class) + "]\n" +
                        "     * -pbn (partition block number)[划分的块数，默认为3,对于GH树等划分块数固定的索引结构，该参数无效，对于AT索引固定为3，RGH索引固定为2]\n" +
                        "     * -maxLeaf [最大叶子大小，默认maxLeaf=10]\n" +
                        "     * -m (mode)[建树的模式：local、global、mix，默认local]\n" +
                        "     * -pf (pivot file)[支撑点文件名，该参数可以指定建树时使用的支撑点。参数只在global建树模式下生效，如果该参数不为空，则传入的支撑点选择方法无效]\n" +
                        "     * -h (help)[帮助文档]\n" +
                        "     * -normalVectorCandidateSource [表示在完全线性算法框架中，法向量组候选集来源。默认为\"1111\"," +
                                "其中每个字符'1'分别代表采用的法向量候选集来源为PCA+VP+CGH+球面上均匀向量，若为字符'0'，则表示不采用某个法向量候选集来源]\n" +
                        "     * -isfilternormalvectorset [表示在完全线性划分树中，是否采用基于正交程度的法向量候选集筛选算法，降低索引的构建开销，默认为\"true\"]\n" +
                        "     * -isquerysetextensionr [表示在完全线性划分树中，是否采用基于查询集r-邻域扩展的精准排除率算法，默认为true]"
        );
    }

    private static void parseBuildIndexArgs(String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            switch (args[i].toLowerCase()){
                case "-tm":
                    tableManagerName = args[++i];
                    break;
                case "-f":
                    dataFileName = args[++i];
                    break;
                case "-n":
                    maxDataSize = Integer.valueOf(args[++i]);
                    break;
                case "-d":
                    dim = Integer.valueOf(args[++i]);
                    break;
                case "-i":
                    indexType = args[++i];
                    break;
                case "-psm":
                    pivotSelectionMethod = PivotSelectionMethods.valueOf(args[++i].toUpperCase());
                    break;
                case "-pn":
                    pivotNum = Integer.valueOf(args[++i]);
                    break;
                case "-t":
                    dataType = args[++i];
                    NormalVector.setqr(dataType);
                    break;
                case "-pm":
                    if(indexType.equalsIgnoreCase("vp"))
                        partitionMethod = VPPartitionMethods.valueOf(args[++i].toUpperCase());
                    if(indexType.equalsIgnoreCase("cp"))
                        partitionMethod = CPPartitionMethods.valueOf(args[++i].toUpperCase());
                    if(indexType.equalsIgnoreCase("gh"))
                        partitionMethod = GHPartitionMethods.valueOf(args[++i].toUpperCase());
                    if (indexType.equalsIgnoreCase("pct"))
                        partitionMethod = PCTPartitionMethods.valueOf(args[++i].toUpperCase());
                    if (indexType.equalsIgnoreCase("gnat"))
                        partitionMethod = GNATPartitionMethods.valueOf(args[++i].toUpperCase());
                    if (indexType.equalsIgnoreCase("at"))
                        partitionMethod = ApollonianPartitionMethods.valueOf(args[++i].toUpperCase());
                    if (indexType.equalsIgnoreCase("iat"))
                        partitionMethod = IATPartitionMethods.valueOf(args[++i].toUpperCase());
                    if (indexType.equalsIgnoreCase("rgh"))
                        partitionMethod = RGHPartitionMethods.valueOf(args[++i].toUpperCase());
                    break;
                case "-pbn":
                    partitionBlockNum = Integer.valueOf(args[++i]);
                    break;
                case "-maxleaf":
                    maxLeafSize = Integer.valueOf(args[++i]);
                    break;
                case "-m":
                    mode = HierarchicalPivotSelectionMode.valueOf(args[++i].toUpperCase());
                    break;
                case "-pf":
                    pivotFileName = args[++i];
                    break;
                case "-h":
                    promptForBuildIndex();
                    break;
                case "-normalvectorcandidatesource":
                    normalVectorCandidateSource = String.valueOf(args[++i]);
                    break;
                case "-isfilternormalvectorset":
                    isFilterNormalVectorSet = String.valueOf(args[++i]);
                    break;
                case "-isquerysetextensionr":
                    isQuerySetExtensionR = String.valueOf(args[++i]);
                    break;
                default:
                    System.out.println("输入参数有误！args[i] = " + args[i]);
                    promptForBuildIndex();
                    System.exit(-1);
            }
        }
        if (partitionMethod==null){
            if (indexType.equalsIgnoreCase("vp"))
                partitionMethod = VPPartitionMethods.BALANCED;
            if (indexType.equalsIgnoreCase("cp"))
                partitionMethod = CPPartitionMethods.BALANCED;
            if (indexType.equalsIgnoreCase("gh"))
                partitionMethod = GHPartitionMethods.GH;
            if (indexType.equalsIgnoreCase("pct"))
                partitionMethod = PCTPartitionMethods.KMEANS;
            if (indexType.equalsIgnoreCase("gnat"))
                partitionMethod = GNATPartitionMethods.gnat;
            if (indexType.equalsIgnoreCase("at"))
                partitionMethod = ApollonianPartitionMethods.APOLLONIAN;
            if (indexType.equalsIgnoreCase("iat"))
                partitionMethod = IATPartitionMethods.SPATIAL_BALANCED;
            if (indexType.equalsIgnoreCase("rgh"))
                partitionMethod = RGHPartitionMethods.RGH;
        }
        if (tableManagerName==null){
            System.out.println("+++++++++++++输入参数有误！\n");
            System.exit(-1);
        }
    }


    private static void modePrompt()
    {
        System.out.println(
                "     * -bi (buildIndex) [以构建索引模式启动]\n" +
                        "     * -qi (queryIndex) [以搜索索引模式启动]\n" +
                        "     * -c  (cluster) [以聚类分析模式启动]\n" +
                        "     * -t  (tool) [以度量空间分析工具类模式启动]\n" +
                        "     * -od  (outlierDetection) [以异常点检测分析模式启动]\n" +
                        "     * -h [帮助文档]"
        );
    }

    private static void promptForCluster(){
        System.out.println(
                "     * -f [数据文件的路径]\n" +
                "     * -n [数据文件中样本的个数]\n" +
                "     * -d [数据的维度]\n" +
                "     * -psm [支撑点选择方法：" + listEnumNames(PivotSelectionMethods.class) + ",默认psm=fft]\n" +
                "     * -pn [支撑点个数，默认pn=3]\n" +
                "     * -t [数据类型：protein、dna、vector、image、mass，默认t=vector]\n" +
                "     * -m [聚类方法：kmeans、dbscan、birch,默认m=kmeans]\n" +
                "     * -k [聚类的类别数，默认k=3,当聚类方法是dbscan时，该参数无效，类别由聚类算法自动确定]\n" +
                "     * -maxIter [最大迭代次数，只有当聚类方法选择kmeans时才有效，默认maxIter=300]\n" +
                "     * -tol [聚类中心的最小移动距离，只有当聚类方法选择kmeans时才有效，默认tol=1e-4]\n" +
                "     * -eps [判断核心样本的范围搜索距离，只有当聚类方法选择dbscan时才有效，默认eps=0.5]\n" +
                "     * -minSamples [判断样本点是否是核心样本的最少的样本个数，只有当聚类方法选择dbscan时才有效，默认minSamples=5]\n" +
                "     * -metric [执行dbscan时使用的距离函数，可选值有manhattan、euclidean、infinity.只有当聚类方法选择dbscan时才有效，默认metric=euclidean]\n" +
                "     * -threshold [判断同属一个类簇的类簇半径，只有当聚类方法选择birch时才有效，默认threshold=0.5]\n" +
                "     * -factor [分支因子，限制CF树的叶子节点的孩子数目，只有当聚类方法选择birch时才有效，默认factor=50]\n" +
                "     * -h [帮助文档]"
        );
    }

    /**
     * 将数据写入文件
     * @param size
     * @param distance
     * @param tableName
     * @param radius
     * @param averageExclusionRate
     * @throws IOException
     */
    public static void writeData(int size, double distance, String tableName, double radius, double averageExclusionRate) throws IOException
    {
//        System.out.println("本次搜索一共" + size + "条结果：");
//        //doubleIndexObjectPairs.forEach(System.out::println);
//        System.out.println();
//        System.out.println("总距离计算次数:" + distance);
//        System.out.println("平均距离计算次数:" + distance / queryDataSize);

        File file = new File(tableName + "_" + radius + ".txt");  //存放数组数据的文件

        FileWriter out = new FileWriter(file);  //文件写入流

        out.write("本次搜索一共: " + size + " 条结果：");
        out.write("\r\n");

        out.write("本次搜索平均: " + (double)size/queryDataSize + "条结果：");
        out.write("\r\n");

        out.write("本次搜索平均排除率: " +  averageExclusionRate/queryDataSize);
        out.write("\r\n");

        out.write("总距离计算次数: " + distance);
        out.write("\r\n");

        out.write("平均距离计算次数: " + distance / queryDataSize);
        out.write("\r\n");

        out.close();
    }

    /**
     * 将存储下来的list写入文件中
     * @param list 包含有法向量组，以及该法向量组的正交程度以及划分排除率的值
     * @param size 查询集的大小，用于求划分排除率的
     * @throws IOException
     */
    public static void write(List<QueueElement> list, int size) throws IOException {

        Map<Vector<Double>, Integer> mapVP = new HashMap();
        Map<Vector<Double>, Integer> mapCGH = new HashMap();
        Map<Vector<Double>, Integer> mapBALL = new HashMap();
        Map<Vector<Double>, Integer> mapOther = new HashMap();



        List<Vector<Double>> VP = NormalVector.getVPNormalVectors(Application.pivotNum);

        for (int i = 0; i < VP.size(); i++)
        {
            if(!mapVP.containsKey(VP.get(i)))
            {
                mapVP.put(VP.get(i), 0);
            }
        }

        List<Vector<Double>> CGH = NormalVector.getCGHNormalVectors(Application.pivotNum);

        for (int i = 0; i < CGH.size(); i++)
        {
            if(!mapCGH.containsKey(CGH.get(i)))
            {
                mapCGH.put(CGH.get(i), 0);
            }
        }

        List<Vector<Double>> BALL = NormalVector.getBallPlaneNormalVectors(6, 0.2);
        for (int i = 0; i < BALL.size(); i++)
        {
            if(!mapBALL.containsKey(BALL.get(i)))
            {
                mapBALL.put(BALL.get(i), 0);
            }
        }


        File file = new File(Application.getDataType() + ".txt");  //存放数组数据的文件

        FileWriter out = new FileWriter(file, true);  //文件写入流

        File fileAnalysis = new File(Application.getDataType() + "_Analysis.txt");  //存放数据分析后的文件

        FileWriter outAnalysis = new FileWriter(fileAnalysis, true);  //文件写入流

        File fileRate = new File(Application.getDataType() + "_Rate.txt");  //存放数据分析后的文件

        FileWriter outRate = new FileWriter(fileRate, true);  //文件写入流

        File fileExtent = new File(Application.getDataType() + "_Extent.txt");  //存放数据分析后的文件

        FileWriter outExtent = new FileWriter(fileExtent, true);  //文件写入流

        int count = 0;
        while(list.size() > 0)
        {

            QueueElement tem = list.remove(0);

            Double data = tem.getExc();                      //获得排除率

            Double rate = data/size;
            out.write("排除率： " + rate + " : ");         //写入
            outRate.write(rate + "\t");

            Double extent = tem.getExtent();
            outExtent.write(extent + "\t");

            List<Vector<Double>> vectorList = tem.getVectorList();  //获得法向量组


            //写入
            for (int i = 0; i < vectorList.size(); i++)
            {
                Vector<Double> vector = vectorList.get(i);
//                System.out.println("[" + vector.get(0) + ", " + vector.get(1) + ", " + vector.get(2) + "]");

                if(count < 50)
                {
                    if(mapVP.containsKey(vector))
                        mapVP.put(vector, mapVP.get(vector) + 1);

                    else if(mapCGH.containsKey(vector))
                        mapCGH.put(vector, mapCGH.get(vector) + 1);

                    else if(mapBALL.containsKey(vector))
                        mapBALL.put(vector, mapBALL.get(vector) + 1);
                    else
                    {
                        if(mapOther.containsKey(vector))
                            mapOther.put(vector, mapOther.get(vector) + 1);
                        else
                            mapOther.put(vector, 1);
                    }
                }


                out.write("\t");

                for (int j = 0; j < vector.size(); j++)
                {
                    if(j == 0)
                        out.write("[ " + vector.get(j));
                    else
                    out.write(", " + vector.get(j));
                }
                out.write(" ], ");
                out.write("\t");
            }
            count++;
        }

        int VPsize = 0;
        for (int i = 0; i < VP.size(); i++)
        {
            if(mapVP.containsKey(VP.get(i)))
                VPsize += mapVP.get(VP.get(i));
        }
        int CGHsize = 0;
        for (int i = 0; i < CGH.size(); i++)
        {
            if(mapCGH.containsKey((CGH.get(i))))
                CGHsize += mapCGH.get(CGH.get(i));
        }

        int BALLsize = 0;
        for (int i = 0; i < BALL.size(); i++)
        {
            if(mapBALL.containsKey((BALL.get(i))))
                BALLsize += mapBALL.get(BALL.get(i));
        }



        count = 50 * pivotNum;
//        outAnalysis.write("count: " + count + ", VPsize: " + VPsize + ", CGHsize: " + CGHsize + ", BALLsize: "
//                + BALLsize + ", OtherSize: " + (count - VPsize - CGHsize - BALLsize));
        outAnalysis.write("total, VPsize, CGHsize, ballSize, otherSize: \t" + count + "\t" + VPsize + "\t"
                + CGHsize + "\t" + BALLsize + "\t" + (count - VPsize - CGHsize - BALLsize));

        out.write("\n");
        outAnalysis.write("\n");
        outRate.write("\n");
        outExtent.write("\n");

        out.close();
        outAnalysis.close();
        outRate.close();
        outExtent.close();
    }
}
