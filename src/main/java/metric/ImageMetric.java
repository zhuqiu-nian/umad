package metric;

import db.type.Image;
import db.type.IndexObject;

/**
 * 计算图片间的距离。专为{@code UMAD}图片数据库设计，可能对其他的数据库不适用。
 */
public class ImageMetric implements Metric
{

    private static final long serialVersionUID = 3442536718895037166L;

    /**
     * Feature number for each image. We have three features for each image: structure, texture,
     * histogram.
     */
    final private int feaNum = 3;

    /**
     * Number of floats that can represent each feature.
     */
    final private int[] feaLength = {3, 48, 15};

    /**
     * Distance function selection for each feature.
     */
    final private boolean[] minBool = {false, false, false};

    /**
     * The weights of each feature in the computation of distance.
     */
    final private double[] weights = {0.333333, 0.333333, 0.333333};

    /**
     * The max distance for each feature.
     */
    final private double[] maxDist = {1.0, 60.0, 1.0};

    /**
     * 计算距离
     *
     * @param one 第一个待计算距离的{@link IndexObject}对象。
     * @param two 第二个待计算距离的{@link IndexObject}对象。
     * @return 两者距离值
     */
    public double getDistance(IndexObject one, IndexObject two)
    {
        return getDistance((Image) one, (Image) two);
    }

    /**
     * 计算距离
     *
     * @param one 第一个待计算距离的{@link Image}对象
     * @param two 第二个待计算距离的{@link Image}对象
     * @return 两者距离值
     */
    public double getDistance(Image one, Image two)
    {
        double dist = 0.0;
        for (int i = 0; i < feaNum; i++)
        {
            dist += (getDistance_Fea(one, two, i) / maxDist[i] * weights[i]);
            // metric += ( 2.0*getDistance_Fea(one, two, i) / (one.getMaxDist(i)+two.getMaxDist(i)) *
            // weights[i] ) ;
        }
        return dist;
    }

    /**
     * 计算距离
     *
     * @param one      第一个待计算距离的{@link Image}对象
     * @param two      第二个待计算距离的{@link Image}对象
     * @param FeaIndex 待计算的特征索引值
     * @return 两者在 {@code FeaIndex} 上的距离
     */
    public double getDistance_Fea(Image one, Image two, int FeaIndex)
    {
        int    StartIndex = 0, EndIndex = 0, cnt;
        double dist       = 0.0, tempval = 0.0;

        for (int i = 0; i < FeaIndex; i++)
             StartIndex += feaLength[i];
        EndIndex = StartIndex + feaLength[FeaIndex] - 1;

        // The first method for computing image object distance.
        if (minBool[FeaIndex])
        {
            for (cnt = StartIndex; cnt <= EndIndex; cnt++)
            {
                dist += Math.min(one.getFeature(cnt), two.getFeature(cnt));
                tempval += one.getFeature(cnt);
                // to make it a symmetric Metric space, add the following line
                // tempval += two.m_Feas[cnt] ;
            }
            dist = Math.abs(1.0 - (dist / tempval));
        } else
        { // The second method for computing image object distance.
            for (cnt = StartIndex; cnt <= EndIndex; cnt++)
            {
                tempval = (one.getFeature(cnt) - two.getFeature(cnt));
                dist += (tempval * tempval);
            }
            dist = Math.sqrt(dist);
        }

        return dist;
    }

}
