package algorithms.pivotselection;

import db.type.IndexObject;
import metric.Metric;

import java.util.List;

/**
 * 支撑点选择算法接口
 *
 * @author Willard
 */
public interface PivotSelectionMethod
{
    /**
     * @param metric    距离函数
     * @param data      数据
     * @param numPivots 支撑点数目
     * @return 返回被选择的支撑点在原数据列表data中的下标
     */
    int[] selectPivots(Metric metric, List<? extends IndexObject> data, final int numPivots);

    /**
     * 从数据列表data的部分数据中选择支撑点。
     *
     * @param metric    距离函数
     * @param data      数据
     * @param first     第一个元素的偏移量
     * @param dataSize  数据大小
     * @param numPivots 支撑点数目
     * @return 返回被选择的支撑点在原数据列表data中的下标
     */
    int[] selectPivots(Metric metric, List<? extends IndexObject> data, int first, int dataSize, final int numPivots);
}
