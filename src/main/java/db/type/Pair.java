package db.type;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;

/**
 * 包裹两个对象的包裹器。
 */
public class Pair implements Serializable, Comparable
{

    private static final long serialVersionUID = 7226016227786370437L;

    private final Object first;
    private final Object second;

    /**
     * 构造函数
     *
     * @param first  第一个对象
     * @param second 第二个对象
     */
    public Pair(Object first, Object second)
    {
        this.first  = first;
        this.second = second;
    }

    /**
     * 获取包裹的第一个对象
     *
     * @return 返回包裹的第一个对象
     */
    public Object first()
    {
        return first;
    }

    /**
     * 获取包裹的第二个对象
     *
     * @return 返回包裹的第二个对象
     */
    public Object second()
    {
        return second;
    }

    public String toString()
    {
        java.text.DecimalFormat format = new java.text.DecimalFormat("#.######");
        format.setMaximumFractionDigits(6);

        String firstString  = (first instanceof Double) ? format.format(((Double) first).doubleValue()) : first.toString();
        String secondString = (second instanceof Double) ? format.format(((Double) second).doubleValue()) : second.toString();


        return "(" + firstString + ", " + secondString + ")";
    }


    private void writeObject(ObjectOutputStream objectStream) throws IOException
    {
        objectStream.defaultWriteObject();
    }


    private void readObject(ObjectInputStream objectStream) throws IOException, ClassNotFoundException
    {
        objectStream.defaultReadObject();
    }

    public static final Comparator<Object> FirstComparator = new Comparator<Object>()
    {
        public int compare(Object first, Object second)
        {
            return ((Comparable) (((Pair) first).first())).compareTo(((Pair) second).first());
        }
    };

    public static final Comparator SecondComparator = new Comparator()
    {
        public int compare(Object first, Object second)
        {
            return ((Comparable) ((Pair) first).second()).compareTo(((Pair) second).second());
        }
    };

    /**
     * 比较该对象与传入对象的大小
     *
     * @param o 要比较的对象
     * @return 比较结果
     */
    public int compareTo(Object o)
    {
        return FirstComparator.compare(this, o);
    }

    public static void main(String[] args)
    {
        int last = Integer.parseInt(args[0]);
        for (int i = last; i > 1; i--)
             System.out.println("1/" + i + " : " + f(1 / (double) i));

        for (int i = 1; i <= last; i++)
             System.out.println(i + " : " + f2(i));


    }

    static double f(double m)
    {
        return (m / 2 + 1) * Math.log(m + 2) + (m / 2 - 1) * Math.log(m);

    }

    static double f1(double m)
    {
        return Math.log((m + 2) * m) / 2 + 1 - 1 / m;
    }

    static double f2(double m)
    {
        return (m + 1.5) * Math.log(1 + 2 / m) - 1;
    }

}
