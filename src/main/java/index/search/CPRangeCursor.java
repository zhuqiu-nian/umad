package index.search;

import app.Application;
import db.type.IndexObject;
import index.structure.CPInternalNode;
import index.structure.Node;
import index.type.NodeSearchAction;
import manager.ObjectIOManager;
import metric.Metric;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.LinkedList;
import db.type.DoubleIndexObjectPair;

/**
 * 完全线性划分树CP树范围搜索的实体类，该类继承抽象类{@link RangeCursor}实现父类的抽象方法{@link RangeCursor#willTheSubTreeFurtherSearch(Node, Metric, IndexObject, double, double[])}
 * 通过该抽象方法判断搜索的时候对于每一个内部节点的扇出的处理方法。
 */
public class CPRangeCursor extends RangeCursor
{
    /**
     * 范围查询构造函数
     *
     * @param oiom        io读写对象
     * @param metric      搜索使用的距离函数
     * @param rootAddress 要搜索的索引树的根节点
     */
    public CPRangeCursor(ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(oiom, metric, rootAddress);
    }

    /**
     * {@link CPRangeCursor}类的构造函数
     *
     * @param query       查询对象
     * @param oiom        io读写对象
     * @param metric      搜索使用的距离函数
     * @param rootAddress 要搜索的索引树的根节点
     */
    public CPRangeCursor(RangeQuery query, ObjectIOManager oiom, Metric metric, long rootAddress)
    {
        super(query, oiom, metric, rootAddress);
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
     * @param node                 需要搜索的节点
     * @param metric               距离函数
     * @param query                查询对象
     * @param radius               查询半径
     * @param queryToPivotDistance 查询对象到该节点的各个支撑点的距离
     * @return 标记孩子状态的数组
     * @see NodeSearchAction
     */
    @Override
    public NodeSearchAction[] willTheSubTreeFurtherSearch(Node node, Metric metric, IndexObject query, double radius, double[] queryToPivotDistance, Hashtable<IndexObject, Double> memoryHashTable, LinkedList<DoubleIndexObjectPair> currentResult)
    {

        CPInternalNode aNode = (CPInternalNode) node;
        //初始化标记数组
        NodeSearchAction[] nodeSearchActions = new NodeSearchAction[aNode.getNumChildren()];
        Arrays.fill(nodeSearchActions, NodeSearchAction.RESULTUNKNOWN);

        //创建2^（n-1）个n维的查询点的顶点

        double[][] vertex = getCorners(queryToPivotDistance.length, queryToPivotDistance, radius);

        //逐个判断每个扇出的处理方法
        /**
         * 1. 根据查询点确定范围查询区域
         * 2. 根据范围查询区域和数据块是否有交集从而确定是否可以将该数据块排除
         *
         * **/
        for (int i = 0; i < aNode.getNumChildren(); i++)
        {
            //拿到该孩子内存储的数据到每个支撑点距离的上下界限
            double[][] range = aNode.getChildPredicate(i);

            //获得该节点的法向量集合
            List<Vector<Double>> normalVectorGroup = aNode.getNormalVectorGroup();     //获得该扇出的法向量组
            double[][] longestDistanceTopivots = aNode.getLongestDistanceTopivots();   //获得该扇出到该内部节点的每个支撑点的距离的最远值

             //int cuntNew = 0;
            //遍历每个支撑点，根据查询对象到支撑点的距离、搜索半径以及孩子的上下界标记孩子的状态
            for (int j = 0; j < aNode.getNumPivots(); j++)
            {
                /****是否可以利用包含关系判断存在结点范围全部位于查询范围以内，若是，则直接将该结点的所有数据作为结果返回***/
                //当一个支撑点满足 dis(q, p_j) + 第i个扇出的数据到第j个支撑点的上界 <= 查询半径， 那么第i个扇出的所有数据都在查询范围内
                if(longestDistanceTopivots != null && Application.getIsSearchOptimization().equals("true"))    //mod = true,查询优化
                    if(longestDistanceTopivots[i][j] + queryToPivotDistance[j] <= radius)
                    {
                        nodeSearchActions[i] = NodeSearchAction.RESULTALL;
                        break;
                    }

                //2.1，计算过该法这方向上的各顶点所在超平面的截距
                double[] tmp = new double[2];
                tmp[0] = Double.MAX_VALUE;
                tmp[1] = -1;

                Vector<Double> vector = normalVectorGroup.get(j);				 //获得当前方向上的法向量
                tmp = getIntecept(vertex, vector);                           //获得当前范围查询投影到法向量的最小最大截距

                //若查询范围与该区域映射到该法向量的区域无交集，则排除
                if (isExclude(tmp, range[0][j], range[1][j]))
                {
                    nodeSearchActions[i] = NodeSearchAction.RESULTNONE;
                    break;
                }
            }
        }
        return nodeSearchActions;
    }

    /*1. 求角落点坐标*/
    /**
     * 根据查询点确定范围查询区域，
     * 用范围查询的所有顶点来表示范围查询，所以该函数的目的是计算范围查询的所有顶点
     * @param numPivot 支撑点个数
     * @param cordinate 数据点在支撑点空间的坐标
     * @param qr 查询半径
     * @return 返回数据点在支撑点空间中范围查询球的所有顶点坐标
     * 首先，获得一个左下角坐标为原点的超立方体
     * 其次，将该立方体平移到查询点，确定真实的范围查询的立方体的顶点
     */
    public double[][] getCorners(int numPivot, double[] cordinate, double qr)
    {
        //1. 将角存储下来
        int n = (int)Math.pow(2, numPivot);      //查询球的角的个数

        double[][] corners = new double[n][numPivot];      //查询角的坐标

        double[] corners0 = new double[numPivot];    //第一个角
        for (int i = 0; i < numPivot; i++)
            corners0[i] = new Double(0);

        corners[0] = corners0;

        for (int i = 1; i < corners.length; i++)
            corners[i] = plusOne(corners[i - 1]);


        for (int i = 0; i < corners.length; i++)
        {
            for (int j = 0; j < corners[i].length; j++)
            {
                corners[i][j] = corners[i][j] * 2 * qr + cordinate[j] - qr;
            }
        }
        //以上将以2*qr为边长的正方形的顶点都计算出来了

        return corners;
    }

    //二进制加一
    public double[] plusOne(double[] digitsOrigial)
    {
        double[] digits = new double[digitsOrigial.length];
        for (int i = 0; i < digits.length; i++)
        {
            digits[i] = digitsOrigial[i];
        }
        for(int i = digits.length - 1; i >= 0; i--)
        {
            if(digits[i] < 1)
            {
                digits[i] += 1;
                return digits;
            }
            else
                digits[i] = 0;
        }

        System.out.println("二进制加一errors!!!!!!!!!!!!!!!!!!");

        return null;
    }

    /**
     * 求范围查询映射到法向量上的范围
     * @param corners 范围查询角落点
     * @param vector 法向量
     * @return 求范围查询映射到法向量后的最小和最大截距
     */
    public double[] getIntecept(double[][] corners, Vector<Double> vector)
    {
        double min = Double.MAX_VALUE;
        double max = -1;

        for (int i = 0; i < corners.length; i++)
        {
            double intecept = 0;
            for (int j = 0; j < corners[i].length; j++)
            {
                intecept += corners[i][j] * vector.get(j);
            }
            if(intecept < min)
                min = intecept;
            if(intecept > max)
                max = intecept;
        }

        return new double[]{min, max};
    }

    /*3. 判断映射后的区域和给定区域是否有交集，有，则该区域无法排除*/

    /**
     *  判断范围查询和结点有交集
     * @param intecept 给定范围查询球投影到法向量的截距
     * @param lower 该划分区域在该法向量的截距范围的最小值
     * @param upper 该划分区域在该法向量的截距范围的最大值
     * @return 是否范围球和该划分区域有交集
     */
    public boolean isExclude(double[] intecept, double lower, double upper)
    {
        //如果两个区间不相交，那么最大的开始端一定大于最小的结束端
        if(Math.max(intecept[0], lower) > Math.min(intecept[1], upper))
            return true;
        else
            return false;
    }
}
