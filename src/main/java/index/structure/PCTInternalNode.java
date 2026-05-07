package index.structure;

import db.type.IndexObject;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

/**
 * PCT(Pivot Space Cluster Tree)索引树，也是一种基于维诺划分的索引树，其基本思路是将同一个类别的数据划分到同一个数据块中，而这种划分操作是完全在支撑点空间中进行的。
 * 搜索的时候也要先将查询点映射到支撑点空间中，因此每个内部结点都相当于是一个新的支撑点空间，需要保存构建这个空间的支撑点信息。
 * 除此之外，为了搜索的时候可以排除，还需要保存这个支撑点空间的每个子空间（维诺超多面体）的聚类中心（站点），和每个子结点的引用。
 * 因此，一个内部结点应该存储三类信息，支撑点信息、每个子空间的中心和每个子结点的引用。
 * 因为MBR加速查询（{@link index.search.PCTRangeCursor}）过程需要使用到维诺超多面体在每个支撑点维度上的投影，所以我们在划分的时候就需要存储这些信息，
 * 那么PCT内部结点除了存储支撑点信息、每个子空间的中心和每个子结点的引用之外，还需要存储数据在每个支撑点维度的上下界。
 * <pre>
 *     索引PCTInternalNode的数据结构如下：
 *         <pre>
 *             PCTInternalNode{
 * 	           DataElement[]  pivots;   //存储生成该支撑点空间的支撑点集合
 * 	           Double[][]  centroids;   //存储该支撑点空间的每个子空间的聚类中心
 * 	           Node[]  children;      //存储每个子结点的引用
 * 	           Double[][] minValue;
 * 	           Double[][] maxValue;
 *             }
 *         </pre>
 *  </pre>
 * @see index.search.PCTRangeCursor
 * @author liulinfeng 2022/3/12
 */
public class PCTInternalNode extends InternalNode
{
    private static final long       serialVersionUID = 8622530239374877466L;
    //保存每个聚类块的聚类中心
    private              double[][] centroid;
    //保存每个聚类块的半径
    private              double[]   radius;
    //保存该块内的点的坐标在每个维度上的最大值
    private double[][] maxCoordinateSingleDim;
    //保存该块内点的坐标在每个维度上的最小值
    private double[][] minCoordinateSingleDim;

    /**
     * 无参数构造方法(兼容序列化，尽量不要调用)
     */
    public PCTInternalNode()
    {
    }

    /**
     * 内部节点类{@link InternalNode}的构造函数
     *
     * @param pivots         支撑点
     * @param size           当前内部节点所涉及的数据大小
     * @param childAddresses 当前内部节点的孩子指针
     * @param maxCoordinateSingleDim 该块内的点的坐标在每个维度的最大值
     * @param minCoordinateSingleDim 该块内的点在每个维度的最小值
     */
    public PCTInternalNode(IndexObject[] pivots, int size, long[] childAddresses, double[][] centroid,
                           double[][] maxCoordinateSingleDim, double[][] minCoordinateSingleDim, double[] radius)
    {
        super(pivots, size, childAddresses);
        this.centroid = centroid;
        this.maxCoordinateSingleDim = maxCoordinateSingleDim;
        this.minCoordinateSingleDim = minCoordinateSingleDim;
        this.radius = radius;
    }

    /**
     * 获取指定孩子的聚类中心
     *
     * @param childIndex 孩子的下标
     * @return 指定孩子的聚类中心
     */
    public double[] getCentroidOf(int childIndex)
    {
        return centroid[childIndex].clone();
    }

    /**
     * 返回所有孩子的聚类中心
     * @return
     */
    public double[][] getCentroids(){
        return centroid.clone();
    }

    /**
     * 获取指定孩子的聚类半径
     *
     * @param childIndex 孩子的下标
     * @return 指定孩子的聚类中心
     */
    public double getRadiusOf(int childIndex)
    {
        return radius[childIndex];
    }

    /**
     * 返回所有孩子的聚类半径
     * @return
     */
    public double[] getRadius(){
        return radius.clone();
    }


    /**
     * 返回代表KMPInternalNode的字符串
     *
     * @return 代表KMPInternalNode的字符串
     */
    @Override
    public String toString()
    {
        StringBuilder toString = new StringBuilder("PCTInternalNode{ pivotSet=" + Arrays.toString(pivotSet)
                + "\n, dataSize=" + dataSize + '\n');
        for (int i = 0; i < centroid.length; i++)
        {
            toString.append("第" + i + "个孩子：centroid=" + Arrays.toString(centroid[i]) + "radius=" + radius[i]
                    + ", childAddresses=" + childAddresses[i]);
        }
        return toString.append('}').toString();
    }

    /**
     * 对内部节点进行写入
     *
     * @param out 输出对象
     * @throws IOException 写入对象时发生IO异常
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeInt(centroid.length);
        for (int i = 0; i < centroid.length; i++)
        {
            out.writeInt(centroid[i].length);
            for (int j = 0; j < centroid[i].length; j++)
            {
                out.writeDouble(centroid[i][j]);
            }
        }
        out.writeInt(maxCoordinateSingleDim.length);
        for (int i = 0; i < maxCoordinateSingleDim.length; i++)
        {
            out.writeInt(maxCoordinateSingleDim[i].length);
            for (int j = 0; j < maxCoordinateSingleDim[i].length; j++)
            {
                out.writeDouble(maxCoordinateSingleDim[i][j]);
            }
        }
        out.writeInt(minCoordinateSingleDim.length);
        for (int i = 0; i < minCoordinateSingleDim.length; i++)
        {
            out.writeInt(minCoordinateSingleDim[i].length);
            for (int j = 0; j < minCoordinateSingleDim[i].length; j++)
            {
                out.writeDouble(minCoordinateSingleDim[i][j]);
            }
        }
        out.writeInt(radius.length);
        for (int i = 0; i < radius.length; i++)
        {
            out.writeDouble(radius[i]);
        }
    }

    /**
     * 读取内部节点
     *
     * @param in 输入对象
     * @throws IOException            读当前内部节点时发生IO异常
     * @throws ClassNotFoundException 对象类型错误
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        centroid = new double[in.readInt()][];
        for (int i = 0; i < centroid.length; i++)
        {
            centroid[i] = new double[in.readInt()];
            for (int j = 0; j < centroid[i].length; j++)
            {
                centroid[i][j] = in.readDouble();
            }
        }
        int i1 = in.readInt();
        maxCoordinateSingleDim = new double[i1][];
        for (int i = 0; i < i1; i++)
        {
            int i12 = in.readInt();
            maxCoordinateSingleDim[i] = new double[i12];
            for (int j = 0; j <i12 ; j++)
            {
                maxCoordinateSingleDim[i][j] =in.readDouble();
            }
        }
        int i2 = in.readInt();
        minCoordinateSingleDim = new double[i1][];
        for (int i = 0; i < i2; i++)
        {
            int i22 = in.readInt();
            minCoordinateSingleDim[i] = new double[i22];
            for (int j = 0; j <i22 ; j++)
            {
                minCoordinateSingleDim[i][j] =in.readDouble();
            }
        }
        int i3 = in.readInt();
        radius = new double[i3];
        for (int i = 0; i < i3; i++)
        {
            radius[i] = in.readDouble();
        }
    }

    /**
     * 获取该块内的坐标，在每个维度的最小值
     * @return
     */
    public double[][] getMinCoordinateSingleDim()
    {
        return minCoordinateSingleDim;
    }

    /**
     * 获取该块内的坐标，在每个维度的最大值
     * @return
     */
    public double[][] getMaxCoordinateSingleDim()
    {
        return maxCoordinateSingleDim;
    }

    /**
     * 返回聚类块的半径
     * @return
     */
    public double[] getRadii()
    {
        return radius;
    }
}
