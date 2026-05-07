package metric;

import db.type.IndexObject;

/**
 * 带记录距离计算次数的距离函数。
 * <p>
 * 包裹了一个基本的{@link Metric}，它可以记录调用{@link Metric#getDistance(IndexObject, IndexObject)}的次数，
 * 并提供额外的获取和清除计数的函数。
 */
public class CountedMetric implements Metric
{
    private static final long serialVersionUID = 5436226220280070858L;

    final private Metric baseMetric;

    private int counter;

    /**
     * 构造函数
     * <p>
     * 使用给定的{@link Metric}创建一个{@code CountedMetric}
     *
     * @param baseMetric 给定的{@link Metric}
     */
    public CountedMetric(Metric baseMetric)
    {
        if (baseMetric == null) throw new NullPointerException("object baseMetric cannot be null");
        this.baseMetric = baseMetric;
        this.counter    = 0;
    }

    /**
     * 计算距离
     *
     * @param one 第一个待计算距离的{@link IndexObject}对象。
     * @param two 第二个待计算距离的{@link IndexObject}对象。
     * @return 返回距离值
     */
    final public double getDistance(IndexObject one, IndexObject two)
    {
        ++counter;
        return baseMetric.getDistance(one, two);
    }

    /**
     * 返回距离计算函数的调用次数
     *
     * @return 距离计算函数的调用次数
     */
    final public int getCounter()
    {
        return counter;
    }

    /**
     * 设置距离计数值为0
     */
    final public void clear()
    {
        counter = 0;
    }

}
