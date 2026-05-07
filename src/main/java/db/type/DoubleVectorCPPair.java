/**
 * edu.utexas.util.DoubleObjectPair 2003.07.19
 *
 * Copyright Information:
 *
 * Change Log:
 * 2003.07.19: Modified from the original mobios package, by Rui Mao
 * 2004.10.31: add toString(), by Rui Mao
 */
package db.type;
/*添加了一个coordinate成员变量*/
import java.util.Comparator;

///**
// *  * Wraps a <code>double</code> and an {@link IndexObject}.
// *  * It also contains two {@link Comparator}s to compare the double or the {@link IndexObject}.
// */

/**
 * 包裹一个{@code double}类型和{@link IndexObject}类型，还包含{@link Double}类型和{@link IndexObject}类型比较器。
 *
 * @author Rui Mao, Willard
 * @version 2004.10.31
 */
//专门用于完全线性划分的
public class DoubleVectorCPPair
{
    private double _double;
    public double[] cordinate; //添加数据在支撑点空间的坐标

    private IndexObject object;

    public DoubleVectorCPPair(double dd, IndexObject o)
    {
        this._double = dd;
        this.object = o;
    }

    public DoubleVectorCPPair()
    {
        this._double = 0;
        this.object = null;
    }

    public double getDouble()
    {
        return _double;
    }

    public IndexObject getObject()
    {
        return object;
    }

    public void setDouble( double d)
    {
        this._double = d;
    }

    public void setObject(IndexObject o)
    {
        this.object = o;
    }

    public String toString()
    {
        return "double =" + _double + ", object= " + object;
    }

    /**
     * {@link Double}类型比较器
     */
    public static final Comparator<DoubleVectorCPPair> DoubleComparator = new Comparator<DoubleVectorCPPair>()
    {
        public int compare(DoubleVectorCPPair first, DoubleVectorCPPair second)
        {
            final double firstDouble  = first.getDouble();
            final double secondDouble = second.getDouble();
            return firstDouble < secondDouble ? -1 : firstDouble > secondDouble ? 1 :0;
        }
    };

    /**
     * {@link IndexObject}类型比较器
     */
    public static final Comparator<DoubleVectorCPPair> ObjectComparator = new Comparator<DoubleVectorCPPair>() {
        public int compare(DoubleVectorCPPair first, DoubleVectorCPPair second)
        {
            return first.getObject().compareTo(second.getObject());
        }
    };
}
