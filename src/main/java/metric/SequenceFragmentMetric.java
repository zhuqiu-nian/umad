package metric;

import db.type.Fragment;
import db.type.IndexObject;

/**
 * 计算两个 {@link Fragment}片段之间的距离。
 */
public abstract class SequenceFragmentMetric implements Metric
{

    protected WeightMatrix weightMatrix;

    /**
     * 构造函数
     *
     * @param weightMatrix 加权矩阵
     */
    public SequenceFragmentMetric(WeightMatrix weightMatrix)
    {
        this.weightMatrix = weightMatrix;
    }

    /**
     * @return 返回加权矩阵 {@link WeightMatrix}
     */
    public WeightMatrix getWeightMatrix()
    {
        return weightMatrix;
    }

    /**
     * 计算两个 {@link IndexObject} 之间的距离。
     *
     * @param one 第一个待计算距离的{@link IndexObject}对象。
     * @param two 第二个待计算距离的{@link IndexObject}对象。
     * @return 两个 {@link IndexObject}之间的距离。
     */
    public double getDistance(IndexObject one, IndexObject two)
    {
        return getDistance((Fragment) one, (Fragment) two);
    }

    /**
     * 计算两个 {@link Fragment}之间的距离。
     *
     * @param one 第一个待计算距离的{@link Fragment}对象。
     * @param two 第二个待计算距离的{@link Fragment}对象。
     * @return 两个 {@link Fragment}之间的距离。
     */
    public abstract double getDistance(Fragment one, Fragment two);
}
