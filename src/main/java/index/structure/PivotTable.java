package index.structure;

import db.type.IndexObject;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

/**
 * Pivot Table 是一种特殊的索引结构，只有一个节点（大叶子节点），节点中保存相应的支撑点和其他数据，
 * 只计算每个数据到每个支撑点的距离并存储在 double[][] distance 中，建立的 Pivot Table 查询时
 * 直接通过支撑点来排除数据，不能排除的，依然线性扫描。
 */
public class PivotTable extends LeafNode
{
    private static final long serialVersionUID = -3627262295106855252L;


    private double[][] distance; //pivotTable存储的距离对，第i行是第i个数据到每个支撑点的距离

    /**
     * 无参数构造方法(兼容序列话，尽量不要调用)
     */
    public PivotTable() {
    }

    /**
     * {@link PivotTable}类的构造函数
     *
     * @param pivots   支撑点
     * @param data     数据
     * @param size     数据大小
     * @param distance 距离矩阵
     */
    public PivotTable(IndexObject[] pivots, IndexObject[] data, int size, double[][] distance) {
        super(pivots, data, size);

        if (distance == null) throw new IllegalArgumentException("distance array cannot be null!");
        this.distance = distance;
    }

    /**
     * 查询第dataIndex个数据到每个支撑点的距离
     *
     * @param dataIndex 要查询的数据下标
     * @return 返回数据到每个支撑点的距离
     * the distances from a child to all the pivots.
     */
    public double[] getDataPoint2PivotDistance(int dataIndex) {
        return distance[dataIndex];
    }

    /**
     * @return 返回pivotTable所保存的距离对
     */
    public double[][] getDistanceTable() {
        return distance;
    }

    /**
     * 在MIX建树模式时 初步建树完成后 层次化遍历整棵树
     * 读取所有的支撑点 同时存储 所有叶子节点指针
     * 完成后 对每个叶子节点读出 进而将 数据分为被选为过支撑点的数据 和 普通数据
     * 完成树的清洗 目的 ： 完美解决重复计算和重复添加的问题
     */
    private static void MixLeafNodeClear() {
    }

    /**
     * 对PivotTable进行写入
     *
     * @param out 输出对象
     * @throws IOException 写入对象时发生IO异常
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(distance.length);
        for (int i = 0; i < distance.length; i++) {
            out.writeInt(distance[i].length);
            for (int j = 0; j < distance[i].length; j++) {
                out.writeDouble(distance[i][j]);
            }
        }


    }

    /**
     * 读取PivotTable
     *
     * @param in 输入对象
     * @throws IOException            读当前PivotTable时发生IO异常
     * @throws ClassNotFoundException 对象类型错误
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        distance = new double[in.readInt()][];
        for (int i = 0; i < distance.length; i++) {
            distance[i] = new double[in.readInt()];
            for (int j = 0; j < distance[i].length; j++) {
                distance[i][j] = in.readDouble();
            }
        }


    }

    @Override
    public String toString() {
        return "PivotTable{" +
                "distance=" + Arrays.toString(distance) +
                '}';
    }



}


