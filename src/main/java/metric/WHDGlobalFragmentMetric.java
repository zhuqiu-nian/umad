package metric;


///**
// * This class computes global alignment with weighted Hamming distance
// * @author Weijia Xu, Rui Mao
// * @version 2004.03.02
// */

import db.type.Fragment;

/**
 * 这个类使用加权汉明距离计算全局比对距离。
 */
public class WHDGlobalFragmentMetric extends SequenceFragmentMetric
{

    /**
     * 构造函数
     *
     * @param weightMatrix 加权矩阵
     */
    public WHDGlobalFragmentMetric(WeightMatrix weightMatrix)
    {
        super(weightMatrix);
    }

    private static final long serialVersionUID = 7847936320149830952L;

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
            //XXX remove this check
            //if (weightMatrix.getDistance(one.get(i), two.get(i))!=weightMatrix.getDistance(two.get(i), one.get(i)))
            //    throw new Error("Error in the weightMatrix!");
            distance += weightMatrix.getDistance(one.get(i), two.get(i));
        }
        return distance;
    }

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
