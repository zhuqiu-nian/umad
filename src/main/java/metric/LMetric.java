package metric;

import db.type.DoubleVector;
import db.type.IndexObject;

/**
 * 这个类用来计算两个向量的 {@code L}家族的距离。
 *
 * <pre>
 *     给定两个向量x(x1,x2,...,xk), y(y1,y2,...,yk)，它们之间的距离定义为：
 *      Ls(x,y) = sum( |xi-yi|^s )^(1/s), i=1,..,k where s=1,2,...,infinity.
 *      对于infinite：L(x,y) = max( |xi-yi| ), i = 1,...,k.
 *      L1被称为Manhattan距离。L2被称为Euclidean距离。本类中用L0表示infinite距离。
 * </pre>
 */
public class LMetric implements Metric
{
    private static final long serialVersionUID = -4658067077491099474L;

    /**
     * L1 distance (Manhattan distance) metric for two non-null double arrays of the same length.
     * Given two vectors x(x1,x2,...,xk), y(y1,y2,...,yk), the distance between x,y: L1(x,y) =
     * sum(|xi-yi| ), i= 1,..,k
     */
    public final static LMetric ManhattanDistanceMetric = new LMetric(1);

    /**
     * L2 distance (Euclidean distance) metric for two non-null double arrays of the same length.
     * Given two vectors x(x1,x2,...,xk), y(y1,y2,...,yk), the distance between x,y: L2(x,y) =
     * sum(|xi-yi|^2 )^(1/2), i= 1,..,k
     */
    public final static LMetric EuclideanDistanceMetric = new LMetric(2);

    /**
     * L infinity distance metric for two non-null double arrays of the same length. Given two
     * vectors x(x1,x2,...,xk), y(y1,y2,...,yk), the distance between x,y: L infinity (x,y) = max(
     * |xi-yi| ), i= 1,..,k
     */
    public final static LMetric LInfinityDistanceMetric = new LMetric(0);

    /**
     * Dimension of the metric
     */
    private final int dim;

    /**
     * 构造函数。传入的是L距离的维度，用0表示infinity距离。
     *
     * @param dim dim(the s, but not the dimension of the vectors to compute distance)
     */
    public LMetric(int dim)
    {
        if (dim < 0) throw new IllegalArgumentException("dimension of LMetric is negative:" + dim);

        this.dim = dim;
    }

    /**
     * 计算两个对象之间的L距离。这两个对象应该是两个double型的数组，或者是相同长度的{@link DoubleVector}
     *
     * @param o1 o1
     * @param o2 o2
     * @return 返回两个对象的L距离。
     */
    public double getDistance(IndexObject o1, IndexObject o2)
    {
        if (o1 instanceof DoubleVector && o2 instanceof DoubleVector)
            return getDistance((DoubleVector) o1, (DoubleVector) o2);
        else
            throw new IllegalArgumentException("LMetric cannot compute distance on " + o1.getClass() + " and " + o2.getClass());
    }

    /**
     * 计算两个{@link DoubleVector}之间的L距离。
     *
     * @param dv1 dv1
     * @param dv2 dv2
     * @return 返回两个{@link DoubleVector}的L距离
     */
    public double getDistance(DoubleVector dv1, DoubleVector dv2)
    {
        return getDistance(dv1.getData(), dv2.getData());
    }

    /**
     * /**
     * 计算两个相同维度对的向量之间的L距离。这两个向量应该是两个double型的数组。
     *
     * @param a1 a1
     * @param a2 a2
     * @return 返回两个对象的L距离。
     */
    public double getDistance(double[] a1, double[] a2)
    {
        // check arguments
        if (a1 == null)
            throw new IllegalArgumentException("the first argument is null calling getDistance() of LMetric");

        if (a2 == null)
            throw new IllegalArgumentException("the second argument is null calling getDistance() of LMetric");

        if (a1.length != a2.length)
            throw new IllegalArgumentException("the two arraies are of different lengths (" + a1.length + ", " + a2.length + ") calling getDistance() of LMetric");

        final int length   = a1.length;
        double    distance = 0;

        // infinite distance
        if (dim == 0)
        {
            for (int i = 0; i < length; i++)
                 distance = Math.max(distance, Math.abs(a1[i] - a2[i]));
        }

        // else finite distance
        else
        {
            for (int i = 0; i < length; i++)
                 distance += Math.pow(Math.abs(a1[i] - a2[i]), dim);

            distance = Math.pow(distance, 1 / (double) dim);
        }

        return distance;
    }

    /**
     * 返回L距离的维度
     *
     * @return 返回L距离的维度
     */
    public int getDimension()
    {
        return dim;
    }
}
