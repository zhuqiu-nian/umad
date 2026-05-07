package db.type;

import java.util.Comparator;
import java.util.Objects;


/**
 * 包裹一个{@code double}类型和{@link IndexObject}类型，还包含{@link Double}类型和{@link IndexObject}类型比较器。
 */
public class DoubleIndexObjectPair
{
    private double      _double;
    private IndexObject object;

    public DoubleIndexObjectPair(double dd, IndexObject o)
    {
        this._double = dd;
        this.object  = o;
    }

    public DoubleIndexObjectPair()
    {
        this._double = 0;
        this.object  = null;
    }

    public double getDouble()
    {
        return _double;
    }

    public IndexObject getObject()
    {
        return object;
    }

    public void setDouble(double d)
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
    public static final Comparator<DoubleIndexObjectPair> DoubleComparator = new Comparator<DoubleIndexObjectPair>()
    {
        public int compare(DoubleIndexObjectPair first, DoubleIndexObjectPair second)
        {
            final double firstDouble  = first.getDouble();
            final double secondDouble = second.getDouble();
            return firstDouble < secondDouble ? -1 : firstDouble > secondDouble ? 1 : 0;
        }
    };

    /**
     * {@link IndexObject}类型比较器
     */
    public static final Comparator<DoubleIndexObjectPair> ObjectComparator = new Comparator<DoubleIndexObjectPair>()
    {
        public int compare(DoubleIndexObjectPair first, DoubleIndexObjectPair second)
        {
            return first.getObject().compareTo(second.getObject());
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoubleIndexObjectPair that = (DoubleIndexObjectPair) o;
        return Objects.equals(object, that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_double, object);
    }
}
