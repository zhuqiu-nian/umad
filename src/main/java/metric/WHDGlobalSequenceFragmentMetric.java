package metric;

import db.type.Fragment;

/**
 * 计算带加权汉明距离的 {@link Fragment} 的全局比对距离。
 */

public class WHDGlobalSequenceFragmentMetric extends SequenceFragmentMetric
{


    private static final long serialVersionUID = 7847936320149830952L;

    /**
     * 构造函数
     *
     * @param weightMatrix 加权矩阵
     */
    public WHDGlobalSequenceFragmentMetric(WeightMatrix weightMatrix)
    {
        super(weightMatrix);
    }


    /* (non-Javadoc)
     * @see mobios.metric.SequenceFragmentMetric#getDistance(mobios.type.Fragment, mobios.type.Fragment)
     */

    /**
     * 计算距离
     *
     * @param one 第一个输入序列
     * @param two 第二个输入序列
     * @return 两个 {@link Fragment}之间的距离
     */
    public double getDistance(Fragment one, Fragment two)
    {
        int firstSize;
        if ((firstSize = one.size()) != two.size())
        {
            System.out.println("stop! Two fragments must have the same length");
        }
        double distance = 0.0;
        for (int i = 0; i < firstSize; i++)
        {
            distance += weightMatrix.getDistance(one.get(i), two.get(i));
        }
        return distance;
    }

    /* (non-Javadoc)
     * @see mobios.metric.SequenceFragmentMetric#getWeightMatrix()
     */

    /**
     * 获取加权矩阵 {@link WeightMatrix}
     *
     * @return 加权矩阵 {@link WeightMatrix}
     */
    public WeightMatrix getWeightMatrix()
    {
        return weightMatrix;
    }
}
