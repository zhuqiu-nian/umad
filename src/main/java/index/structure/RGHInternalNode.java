package index.structure;

import db.type.IndexObject;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * RGH-Tree 的内部节点
 * 使用两个 pivot (Pl, Pr) 和单一比率 splitRatio 进行二分划分
 * left: ratio < splitRatio
 * right: ratio >= splitRatio
 *
 * 关键字段:
 * - splitRatio: 分割比率 R
 * - leftSubtreeRadius (dl): 左子树的最大覆盖半径
 * - rightSubtreeRadius (dr): 右子树的最大覆盖半径
 * - childDistances[4]: 子节点参考点的距离
 *   [0] = d(Pl, V.left.Pl), [1] = d(Pl, V.left.Pr)
 *   [2] = d(Pr, V.right.Pl), [3] = d(Pr, V.right.Pr)
 * - childLeftRadius: 左子节点自己的左右半径 [leftPl, leftPr]
 * - childRightRadius: 右子节点自己的左右半径 [rightPl, rightPr]
 */
public class RGHInternalNode extends InternalNode
{
    private static final long serialVersionUID = 9008240423051548571L;

    private double splitRatio;      // 分割比率 R
    private double leftSubtreeRadius;    // dl: 左子树的覆盖半径 (父节点视角)
    private double rightSubtreeRadius;   // dr: 右子树的覆盖半径 (父节点视角)
    private double[] childDistances;   // 子节点参考点距离 [4]

    // 新增: 子节点自己的半径 (用于精确剪枝)
    private double[] childLeftRadius;   // [0] = leftPl到左子树最远点, [1] = leftPr到左子树最远点
    private double[] childRightRadius;  // [0] = rightPl到右子树最远点, [1] = rightPr到右子树最远点

    // 新增: q到子节点pivots的距离缓存 (用于避免重复计算)
    // 在搜索过程中由父节点填充，子节点使用后清空
    private double cachedDistToLeftPivot = Double.NaN;
    private double cachedDistToRightPivot = Double.NaN;

    public RGHInternalNode()
    {
        super();
        childDistances = new double[4];
        childLeftRadius = new double[2];
        childRightRadius = new double[2];
    }

    /**
     * 构造函数 (最完整版本)
     *
     * @param pivots 支撑点集合 [Pl, Pr]
     * @param size 数据大小
     * @param childAddresses 孩子节点地址 [left, right]
     * @param splitRatio 分割比率
     * @param leftSubtreeRadius 左子树覆盖半径 dl
     * @param rightSubtreeRadius 右子树覆盖半径 dr
     * @param childDistances 子节点距离数组 [4]
     * @param childLeftRadius 左子节点自己的半径 [leftPl, leftPr]
     * @param childRightRadius 右子节点自己的半径 [rightPl, rightPr]
     */
    public RGHInternalNode(IndexObject[] pivots, int size, long[] childAddresses,
                     double splitRatio, double leftSubtreeRadius, double rightSubtreeRadius,
                     double[] childDistances, double[] childLeftRadius, double[] childRightRadius)
    {
        super(pivots, size, childAddresses);
        this.splitRatio = splitRatio;
        this.leftSubtreeRadius = leftSubtreeRadius;
        this.rightSubtreeRadius = rightSubtreeRadius;
        this.childDistances = childDistances != null ? childDistances.clone() : new double[4];
        this.childLeftRadius = childLeftRadius != null ? childLeftRadius.clone() : new double[2];
        this.childRightRadius = childRightRadius != null ? childRightRadius.clone() : new double[2];
    }

    /**
     * 简化的构造函数 (兼容旧代码)
     */
    public RGHInternalNode(IndexObject[] pivots, int size, long[] childAddresses,
                     double splitRatio, double leftSubtreeRadius, double rightSubtreeRadius,
                     double[] childDistances)
    {
        this(pivots, size, childAddresses, splitRatio, leftSubtreeRadius, rightSubtreeRadius,
             childDistances, new double[]{leftSubtreeRadius, leftSubtreeRadius},
             new double[]{rightSubtreeRadius, rightSubtreeRadius});
    }

    /**
     * 获取左 pivot (Pl)
     */
    public IndexObject getLeftPivot()
    {
        return pivotSet[0];
    }

    /**
     * 获取右 pivot (Pr)
     */
    public IndexObject getRightPivot()
    {
        return pivotSet[1];
    }

    /**
     * 获取分割比率
     */
    public double getSplitRatio()
    {
        return splitRatio;
    }

    /**
     * 获取左子树覆盖半径 (dl)
     */
    public double getLeftSubtreeRadius()
    {
        return leftSubtreeRadius;
    }

    /**
     * 获取右子树覆盖半径 (dr)
     */
    public double getRightSubtreeRadius()
    {
        return rightSubtreeRadius;
    }

    /**
     * 获取子节点距离数组
     */
    public double[] getChildDistances()
    {
        return childDistances;
    }

    /**
     * 获取左子节点自己的半径 [leftPl, leftPr]
     */
    public double[] getChildLeftRadius()
    {
        return childLeftRadius;
    }

    /**
     * 获取左子节点到leftPivot的距离 (radius l)
     */
    public double getChildLeftRadiusL()
    {
        return childLeftRadius[0];
    }

    /**
     * 获取左子节点到rightPivot的距离 (radius r)
     */
    public double getChildLeftRadiusR()
    {
        return childLeftRadius[1];
    }

    /**
     * 获取右子节点自己的半径 [rightPl, rightPr]
     */
    public double[] getChildRightRadius()
    {
        return childRightRadius;
    }

    /**
     * 获取右子节点到leftPivot的距离 (radius l)
     */
    public double getChildRightRadiusL()
    {
        return childRightRadius[0];
    }

    /**
     * 获取右子节点到rightPivot的距离 (radius r)
     */
    public double getChildRightRadiusR()
    {
        return childRightRadius[1];
    }

    /**
     * 获取左孩子地址
     */
    public long getLeftChild()
    {
        return childAddresses[0];
    }

    /**
     * 获取右孩子地址
     */
    public long getRightChild()
    {
        return childAddresses[1];
    }

    /**
     * 获取缓存的到左Pivot的距离 (用于避免重复计算)
     */
    public double getCachedDistToLeftPivot()
    {
        return cachedDistToLeftPivot;
    }

    /**
     * 获取缓存的到右Pivot的距离 (用于避免重复计算)
     */
    public double getCachedDistToRightPivot()
    {
        return cachedDistToRightPivot;
    }

    /**
     * 设置缓存的到左Pivot的距离
     */
    public void setCachedDistToLeftPivot(double dist)
    {
        this.cachedDistToLeftPivot = dist;
    }

    /**
     * 设置缓存的到右Pivot的距离
     */
    public void setCachedDistToRightPivot(double dist)
    {
        this.cachedDistToRightPivot = dist;
    }

    /**
     * 清空缓存的距离
     */
    public void clearCachedDistances()
    {
        this.cachedDistToLeftPivot = Double.NaN;
        this.cachedDistToRightPivot = Double.NaN;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeDouble(splitRatio);
        out.writeDouble(leftSubtreeRadius);
        out.writeDouble(rightSubtreeRadius);
        out.writeInt(childDistances.length);
        for (double d : childDistances)
        {
            out.writeDouble(d);
        }
        // 写入子节点半径
        out.writeInt(childLeftRadius.length);
        for (double d : childLeftRadius)
        {
            out.writeDouble(d);
        }
        out.writeInt(childRightRadius.length);
        for (double d : childRightRadius)
        {
            out.writeDouble(d);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        splitRatio = in.readDouble();
        leftSubtreeRadius = in.readDouble();
        rightSubtreeRadius = in.readDouble();
        int len = in.readInt();
        childDistances = new double[len];
        for (int i = 0; i < len; i++)
        {
            childDistances[i] = in.readDouble();
        }
        // 读取子节点半径
        int lenL = in.readInt();
        childLeftRadius = new double[lenL];
        for (int i = 0; i < lenL; i++)
        {
            childLeftRadius[i] = in.readDouble();
        }
        int lenR = in.readInt();
        childRightRadius = new double[lenR];
        for (int i = 0; i < lenR; i++)
        {
            childRightRadius[i] = in.readDouble();
        }
    }

    @Override
    public String toString()
    {
        return "RGHInternalNode{" +
                "Pl=" + (pivotSet.length > 0 ? pivotSet[0] : "null") +
                ", Pr=" + (pivotSet.length > 1 ? pivotSet[1] : "null") +
                ", splitRatio=" + splitRatio +
                ", dl=" + leftSubtreeRadius +
                ", dr=" + rightSubtreeRadius +
                ", childLeftRadius=[" + childLeftRadius[0] + "," + childLeftRadius[1] + "]" +
                ", childRightRadius=[" + childRightRadius[0] + "," + childRightRadius[1] + "]" +
                ", dataSize=" + dataSize +
                '}';
    }
}