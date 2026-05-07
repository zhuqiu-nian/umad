package index.structure;

import db.type.IndexObject;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

/**
<<<<<<< HEAD
 * GNAT的内部节点。参考论文Near Neighbor Search in Large Metric Spaces(http://www.vldb.org/conf/1995/P574.PDF)进行实现
=======
 * GNAT的内部节点。GNAT是一种维诺图划分。其以每个支撑点为聚类中心，将数据划分到离它最近的支撑点所代表的聚类块中。同时，在划分的时候GNAT会存储每个
 * 聚类块到其他聚类块的最近距离和最远距离。搜索的时候可以利用最近距离和最远距离进行排除操作。
>>>>>>> dev_liu
 */
public class GNATInternalNode extends InternalNode
{

    private static final long serialVersionUID = 7215498814858257648L;
    private               double[][] lowerRange;
    private               double[][] upperRange;
    private               double[]   radius;

    /**
     * 无参数构造方法(兼容序列化，尽量不要调用)
     */
    public GNATInternalNode()
    {
    }

    /**
     * {@link GNATInternalNode}类的构造函数
     *
     * @param pivots         支撑点
     * @param size           数据大小
     * @param childAddresses 孩子指针数组
     * @param lowerRange     当前节点扇出到支撑点距离的下界
     * @param upperRange     当前节点扇出到支撑点距离的上界
     */
    public GNATInternalNode(IndexObject[] pivots, int size, long[] childAddresses, double[][] lowerRange, double[][] upperRange,
                            double[] radius)
    {
        super(pivots, size, childAddresses);
        this.lowerRange = lowerRange;
        this.upperRange = upperRange;
        this.radius = radius;
    }

    /**
     * 返回该扇出存储的数据到每个支撑点距离的上下界
     *
     * @param childIndex 孩子节点(扇出)的索引
     * @return 一个二维数组。第一维度表示这个扇出的数据到每个支撑点距离的下界，第二维表示这个扇出的数据到没个支撑点的上界
     *         a 2-d array of the lower ranges (first row) and the upper ranges
     *         (second row) of the child to each pivot.
     */
    public double[][] getChildPredicate(int childIndex)
    {
        double[][] result = new double[2][];
        result[0] = lowerRange[childIndex];
        result[1] = upperRange[childIndex];

        return result;
    }

    public double getRadius(int childIndex){
        return radius[childIndex];
    }

    @Override
    public String toString()
    {
        StringBuilder ranggString = new StringBuilder();
        for (int i = 0; i < lowerRange.length; i++)
        {
            ranggString.append("第" + i + "个孩子的半径：\n");
            ranggString.append(radius[i] + "，该孩子到其他孩子的范围为，");
            ranggString.append("下界：" + Arrays.toString(lowerRange[i]) + '\n');
            ranggString.append("上界：" + Arrays.toString(upperRange[i]) + '\n');

        }
        return "GNATInternalNode{\n" + ranggString + "childAddresses=\n" + Arrays.toString(childAddresses) + ", \npivotSet=\n" + Arrays.toString(pivotSet) + ", \ndataSize=\n" + dataSize + '}';
    }



    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeInt(lowerRange.length);
        for (int i = 0; i < lowerRange.length; i++)
        {
            out.writeInt(lowerRange[i].length);
            for (int j = 0; j < lowerRange[i].length; j++)
            {
                out.writeDouble(lowerRange[i][j]);
            }
        }
        out.writeInt(upperRange.length);
        for (int i = 0; i < upperRange.length; i++)
        {
            out.writeInt(upperRange[i].length);
            for (int j = 0; j < upperRange[i].length; j++)
            {
                out.writeDouble(upperRange[i][j]);
            }
        }
        out.writeInt(radius.length);
        for (int i = 0; i < radius.length; i++)
        {
            out.writeDouble(radius[i]);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        lowerRange = new double[in.readInt()][];
        for (int i = 0; i < lowerRange.length; i++)
        {
            lowerRange[i] = new double[in.readInt()];
            for (int j = 0; j < lowerRange[i].length; j++)
            {
                lowerRange[i][j] = in.readDouble();
            }
        }
        upperRange = new double[in.readInt()][];
        for (int i = 0; i < upperRange.length; i++)
        {
            upperRange[i] = new double[in.readInt()];
            for (int j = 0; j < upperRange[i].length; j++)
            {
                upperRange[i][j] = in.readDouble();
            }
        }
        radius = new double[in.readInt()];
        for (int i = 0; i < radius.length; i++)
        {
            radius[i] = in.readDouble();
        }
    }
}
