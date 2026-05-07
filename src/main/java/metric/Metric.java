package metric;

import db.type.IndexObject;

import java.io.Serializable;


/**
 * 距离函数对象。
 *
 * <p>{@code Metric}是一个距离函数对象。它给出了计算度量空间中两数据点间距离的函数。这个距离函数应该符合度量空间特性
 * 即三角不等性、非负性和对称性。
 * </p>
 */
public interface Metric extends Serializable
{

    /**
     * 计算两个数据点之间的距离
     *
     * @param one 第一个待计算距离的{@link IndexObject}对象。
     * @param two 第二个待计算距离的{@link IndexObject}对象。
     * @return 两个 {@link IndexObject}对象的距离。
     */
    double getDistance(IndexObject one, IndexObject two);
}
