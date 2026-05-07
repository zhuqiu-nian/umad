package algorithms.datapartition;

import app.Application;

import java.util.*;

/**
 *
 * 属于完全线性划分算法框架，用于存放法向量组相关数据
 * 包括有：
 * 1. 法向量组来源VPT，CGHT，PCA以及指向球面上的均匀单位法向量
 * 2. 筛选法向量组
 */
public class NormalVector
{
    //存储数据的查询半径，在建树时，需要根据范围查询半径求划分排除率，该参数用于建树时候的查询半径
    public static double qr = 0;

    /**
     * 对于不同的数据集采用不同的查询半径
     * @param dataType
     */
    public static void setqr(String dataType)
    {
        if(dataType.equals("dna"))
        {
            NormalVector.qr = 1.0;
            return;
        }

        if(dataType.equals("vector"))
        {
            NormalVector.qr = 0.04;
            return;
        }

        if(dataType.equals("image"))
        {
            NormalVector.qr = 0.1;
            return;
        }

        if(dataType.equals("protein"))
        {
            NormalVector.qr = 26;
            return;
        }

    }

    /**
     * 获得VPNormalVectors法向量（0,1）（1,0）
     * @param dim  支撑点数据空间维度
     * @return
     */
    public static List<Vector<Double>> getVPNormalVectors(int dim)
    {
        if(Application.getNormalVectorCandidateSource().charAt(1) == '0'){
            return new ArrayList<>();
        }
        //1，生成候选集、
        List<Vector<Double>> VPNormalVector = new ArrayList<Vector<Double>>();

        //把PCA的结果、VP、GH的法向量都放进normalVector
        //将VP法向量放入其中

        for(int i = 0; i < dim; i++)
        {
            Vector<Double> tmpVector = new Vector<Double>();
            for(int j = 0; j < dim; j++)
            {
                if(j == i)
                    tmpVector.add(new Double(1));
                else
                    tmpVector.add(new Double(0));
            }
            VPNormalVector.add(tmpVector);
        }
        return VPNormalVector;
    }

    /**
     * 获得GHNormalVectors法向量（1,1）（1，-1）
     * @param dim
     * @return
     */
    public static List<Vector<Double>> getCGHNormalVectors(int dim)
    {
        if(Application.getNormalVectorCandidateSource().charAt(2) == '0'){
            return new ArrayList<>();
        }
        //1，生成候选集、
        List<Vector<Double>> CGHNormalVector = new ArrayList<Vector<Double>>();

        //将GH法向量加入进来

        for(int i = 0; i < dim - 1; i++)
        {
            Vector<Double> tmpVector = new Vector<Double>();
            for(int j = 0; j < dim; j++)
            {
                if(j == i)
                    tmpVector.add(new Double(1));
                else if(j == i + 1)
                    tmpVector.add(new Double(-1));
                else
                    tmpVector.add(new Double(0));
            }
            CGHNormalVector.add(tmpVector);
        }
        //将最后一个GH法向量放入normalVector中
        Vector<Double> tmpVector = new Vector<Double>();
        tmpVector.add(new Double(1));
        for (int i = 1; i < dim - 1; i++)
            tmpVector.add(new Double(0));
        tmpVector.add(new Double(1));
        CGHNormalVector.add(tmpVector);

        List<Vector<Double>> result = new ArrayList<Vector<Double>>();

        for (int i = 0; i < CGHNormalVector.size(); i++)
        {
            double total = 0;
            for (int j = 0; j < CGHNormalVector.get(i).size(); j++)
            {
                total += Math.pow(CGHNormalVector.get(i).get(j), 2);
            }
            total = Math.sqrt(total);

            Vector<Double> tem = new Vector<>();
            for (int j = 0; j < CGHNormalVector.get(i).size(); j++)
            {
                tem.add(CGHNormalVector.get(i).get(j)/total);
            }

            result.add(tem);
        }

        return  result;

    }

    /**
     * 获得PCANormalVectors法向量,与matlab结果一致
     * @param data
     * @return
     */
    public static List<Vector<Double>> getPCANormalVectors(double[][] data)
    {
        if(Application.getNormalVectorCandidateSource().charAt(0) == '0'){
            return new ArrayList<>();
        }
        PCANormalVectors pcaNormalVectors = new PCANormalVectors();
        return pcaNormalVectors.getVectors(data);
    }

    /**
     * 指向三维球面上的均匀法向量 Marsaglia方法，只有在三维支撑点空间中使用
     * https://blog.csdn.net/xuejinglingai/article/details/113267713
     * @param num
     * @param inteval
     * @return 返回球面上法向量候选集
     */
    public static List<Vector<Double>> getBallPlaneNormalVectors(int num , double inteval)
    {
        /**
         * step1：
         * 　　　　随机产生一对均匀分布的随机数 u ，v   ；这里u，v 在[-1,1] 范围内
         * 　step2 ：
         * 　　　　计算  r^2 = u^2+v^2;
         * 　　　　如果 r^2 > 1 则重新抽样，直到满足   r^2 < 1  .
         *   step3 ：
         * 　　　　计算
         * 　　　　x=2*u*sqrt(1-r2);
         * 　　　　y=2*v*sqrt(1-r2);
         * 　　　　z=1-2*r2;　　
         *
         * https://blog.csdn.net/xuejinglingai/article/details/113267713
         */
        if(Application.getNormalVectorCandidateSource().charAt(3) == '0'){
            return new ArrayList<>();
        }
        List<Vector<Double>> ballPlaneNormalVector = new ArrayList<>();
        double[] x = new double[num];
        double[] y = new double[num];

        x[0] = 0;
        y[0] = 0;

        for (int i = 1; i < x.length; i++)
        {
            x[i] = x[i - 1] + inteval;
            y[i] = y[i - 1] + inteval;
        }

        for (int i = 0; i < x.length; i++)
        {
            for (int j = 0; j < y.length; j++)
            {
                Double r2 = x[i] * x[i] + y[j] * y[j];
                if(r2 <= 1)
                {
                    Vector<Double> tem = new Vector<>();
                    double a = 2 * x[i] * Math.sqrt(1 - r2);
                    double b = 2 * y[j] * Math.sqrt(1 - r2);
                    double c = 1 - 2*r2;
                    if(c >= 0)
                    {
                        tem.add(a);
                        tem.add(b);
                        tem.add(c);

                        ballPlaneNormalVector.add(tem);
                    }

//                    System.out.println(tem.get(0) * tem.get(0) + tem.get(1) * tem.get(1) + tem.get(2) * tem.get(2));
                }
            }
        }
        return ballPlaneNormalVector;
    }

    /*存储法向量集合*/
    public static List<Integer[]> permutationResult;

    /**
     * map按value升序，获得最优若干个法向量组
     * @param mapNormalVectorSets
     * @return
     */
    public static Integer[] valueUpSort(Map<Integer, Double> mapNormalVectorSets) {

        // 升序比较器
        Comparator<Map.Entry<Integer, Double>> valueComparator = new Comparator<Map.Entry<Integer,Double>>() {
            @Override
            public int compare(Map.Entry<Integer, Double> o1,
                               Map.Entry<Integer, Double> o2) {
                // TODO Auto-generated method stub
                if(o1.getValue()-o2.getValue() > 0)
                    return 1;
                else if(o1.getValue()-o2.getValue() < 0)
                    return -1;

                return 0;

            }
        };
        // map转换成list进行排序
        List<Map.Entry<Integer, Double>> list = new ArrayList<Map.Entry<Integer,Double>>(mapNormalVectorSets.entrySet());
        // 排序
        Collections.sort(list,valueComparator);
        // 默认情况下，TreeMap对key进行升序排序
//        System.out.println("------------map按照value降序排序--------------------");
        Integer[] integer = new Integer[NormalVector.NormalVectorSetMaxSize];
        int count = 0;
        for (Map.Entry<Integer, Double> entry : list)
        {
//            System.out.println(entry.getKey() + ":" + entry.getValue());
            if(count < integer.length)
                integer[count++] = entry.getKey();
            else
                break;
        }
        return integer;

    }

    /**
     * 不筛选得到法向量候选集
     * @param NormalVectors   法向量候选集
     * @return    法向量组候选集
     */
    public static List<Integer[]> getAllNormalVectorSet(List<Vector<Double>> NormalVectors){
        getPermutationResult(NormalVectors);
        return permutationResult;
    }

    //最大法向量候选集个数
    public final static int NormalVectorSetMaxSize = 100;
    /**
     * 根据正交程度进行排序，计算较优的前num个法向量组
     * @param NormalVectors 法向量候选集
     * @return 不同法向量组的下标，并存入permutationResult 法向量组候选集
     */
    public static List<Integer[]> getAllNormalVectorSetRefind(List<Vector<Double>> NormalVectors)
    {
        getPermutationResult(NormalVectors);

        if(permutationResult.size() < NormalVector.NormalVectorSetMaxSize)
            return permutationResult;

        Map<Integer, Double> map = new HashMap<>();


        for (int i = 0; i < permutationResult.size(); i++)
        {
            int num = permutationResult.get(0).length;
            double value = 0;                            //给不同的法向量组定义一个值，用来判断谁优谁劣，优胜劣汰

            /*计算不同法向量组的value，放入map中*/
            for (int j = 0; j < num - 1; j++)
            {
                for (int k = j + 1; k < num; k++)
                {
                    Vector<Double> tmp1 = NormalVectors.get(permutationResult.get(i)[j]);
                    Vector<Double> tmp2 = NormalVectors.get(permutationResult.get(i)[k]);
                    for (int l = 0; l < tmp1.size(); l++)
                    {
                        double tem = Math.abs(tmp1.get(l) * tmp2.get(l));
                        value += tem;
                    }
                }
            }
            map.put(i, value);
        }

        Integer[] key = valueUpSort(map);             /*map按value值进行升序*/
        List<Integer[]> result = new LinkedList<>();

        for (int i = 0; i < key.length; i++)
        {
            result.add(permutationResult.get(key[i]));
        }

        permutationResult = result;

        return result;
    }


    //存储当前的临时组合的下标
    static Integer[] tmpcombin;

    /**
     *   获得全组合结果存在list中
     * @param vectorSize
     * @param dim
     * @param list
     */
    public static void allCombin(int vectorSize, int dim, List<Integer[]> list)
    {

        for(int i=dim; i<=vectorSize; i++) {
            tmpcombin[dim-1] = i-1;
            if(dim>1)
                allCombin(i-1,dim-1, list);
            else
            {
                Integer[] tem = new Integer[tmpcombin.length];
                for (int j = 0; j < tem.length; j++) {
                    tem[j] = tmpcombin[j];
                }
                list.add(tem);
            }
        }
    }


    /**
     * 求全组合结果，并存入permutationResult中
     * @param NormalVectors  法向量候选集
     */
    public static void getPermutationResult(List<Vector<Double>> NormalVectors)
    {
        int vectorSize = NormalVectors.size();
        int dim = NormalVectors.get(0).size();

        permutationResult = new ArrayList<>();
        tmpcombin = new Integer[dim];
        allCombin(vectorSize, dim, permutationResult);
    }



    /**
     * 计算法向量组的正交程度，将法向量候选集中的normalVector互相点乘并加和
     * @param normalVectors 法向量候选集
     * @return
     */
    public static double getExtent(List<Vector<Double>> normalVectors) {
        double extent = 0;

        for (int i = 0; i < normalVectors.size() - 1; i++) {
            for (int j = i + 1; j < normalVectors.size(); j++) {
                Vector<Double> tmp1 = normalVectors.get(i);
                Vector<Double> tmp2 = normalVectors.get(j);
                for (int l = 0; l < tmp1.size(); l++)
                {
                    double tem = Math.abs(tmp1.get(l) * tmp2.get(l));
                    extent += tem;
                }
            }
        }
        return extent;
    }
}