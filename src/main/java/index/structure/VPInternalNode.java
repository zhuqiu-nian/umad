package index.structure;

import db.type.IndexObject;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

/**
 * 该类为VPIndex的内部节点类，在{@link InternalNode}类的基础上额外存储了
 * 每个扇出中的数据到该内部节点的每个支撑点的距离的上下界。
 * 例如：{@code lowerRange[i][j]}代表了第i个扇出中的数据到第j个支撑点的距离的下界。
 */
public class VPInternalNode extends InternalNode
{

    private static final long       serialVersionUID = 4783659135501605975L;
    public               double[][] lowerRange;
    public               double[][] upperRange;

    /**
     * 无参数构造方法(兼容序列话，尽量不要调用)
     */
    public VPInternalNode()
    {
    }

    /**
     * {@link VPInternalNode}类的构造函数
     *
     * @param pivots         支撑点
     * @param size           数据大小
     * @param childAddresses 孩子指针数组
     * @param lowerRange     当前节点扇出到支撑点距离的下界
     * @param upperRange     当前节点扇出到支撑点距离的上界
     */
    public VPInternalNode(IndexObject[] pivots, int size, long[] childAddresses, double[][] lowerRange, double[][] upperRange)
    {
        super(pivots, size, childAddresses);
        this.lowerRange = lowerRange;
        this.upperRange = upperRange;
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

    /**
     * 以字符串的形式返回当前VPInternalNode的信息
     *
     * @return 当前VPInternalNode的信息
     */
    @Override
    public String toString()
    {
        StringBuilder ranggString = new StringBuilder();
        for (int i = 0; i < lowerRange.length; i++)
        {
            ranggString.append("第" + i + "个孩子的范围：\n");
            ranggString.append("下界：" + Arrays.toString(lowerRange[i]) + '\n');
            ranggString.append("上界：" + Arrays.toString(upperRange[i]) + '\n');

        }
        StringBuilder upperString = new StringBuilder();
        for (var c : upperRange)
        {
            upperString.append(Arrays.toString(c) + '\n');
        }
        return "VPInternalNode{\n" + ranggString + "childAddresses=\n" + Arrays.toString(childAddresses) + ", \npivotSet=\n" + Arrays.toString(pivotSet) + ", \ndataSize=\n" + dataSize + '}';
    }

    /**
     * 对VPInternalNode进行写入
     *
     * @param out 输出对象
     * @throws IOException 写入对象时发生IO异常
     */
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
    }

    /**
     * 读取VPInternalNode
     *
     * @param in 输入对象
     * @throws IOException            读当前VPInternalNode时发生IO异常
     * @throws ClassNotFoundException 对象类型错误
     */
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
    }
}
