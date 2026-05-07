package algorithms.datapartition;

import db.type.IndexObject;
import index.structure.RGHPartitionResults;
import index.structure.PartitionResults;
import metric.Metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RGH-Tree 的分区方法
 * 使用两个 pivot (Pl, Pr) 和单一比率进行二分划分：
 * left: ratio < splitRatio
 * right: ratio >= splitRatio
 */
public enum RGHPartitionMethods implements PartitionMethod
{
    /**
     * RGH 二分划分方法
     */
    RGH
            {
                /**
                 * @param metric 距离函数
                 * @param pivots 支撑点集合 [Pl, Pr]
                 * @param data 数据
                 * @param numPartitions 分区数目（必须是2）
                 * @return 分区结果对象 {@link PartitionResults}
                 */
                public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int numPartitions, int maxLS)
                {
                    return partition(metric, pivots, data, 0, data.size(), numPartitions, maxLS);
                }

                /**
                 * @param metric 距离函数
                 * @param pivots 支撑点集合 [Pl, Pr]
                 * @param data 数据
                 * @param first 第一个元素的偏移量
                 * @param size 数据大小
                 * @param numPartitions 分区数目（必须是2）
                 * @return 分区结果对象 {@link PartitionResults}
                 */
                public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int first, int size, int numPartitions, int maxLS)
                {
                    if (pivots.length != 2)
                    {
                        throw new IllegalArgumentException("RGH partition requires exactly 2 pivots, but got " + pivots.length);
                    }

                    if (numPartitions != 2)
                    {
                        throw new IllegalArgumentException("RGH partition requires exactly 2 partitions, but got " + numPartitions);
                    }

                    IndexObject Pl = pivots[0];
                    IndexObject Pr = pivots[1];

                    List<IndexObject> left = new ArrayList<>();
                    List<IndexObject> right = new ArrayList<>();

                    // 计算比率阈值 (使用中位数)
                    double[] ratios = new double[size];
                    for (int i = 0; i < size; i++)
                    {
                        IndexObject x = data.get(first + i);
                        double dPl = metric.getDistance(Pl, x);
                        double dPr = metric.getDistance(Pr, x);

                        if (dPr == 0.0)
                        {
                            // 如果 dPr 为 0，设置为最大值（在右侧）
                            ratios[i] = Double.MAX_VALUE;
                        }
                        else
                        {
                            ratios[i] = dPl / dPr;
                        }
                    }

                    // 排序找中位数作为 splitRatio
                    double[] sortedRatios = ratios.clone();
                    Arrays.sort(sortedRatios);

                    double splitRatio;
                    if (size == 1)
                    {
                        splitRatio = sortedRatios[0];
                    }
                    else
                    {
                        // 使用中位数
                        splitRatio = sortedRatios[size / 2];
                    }

                    // 处理边界情况
                    if (splitRatio <= 0 || splitRatio == Double.MAX_VALUE)
                    {
                        splitRatio = 1.0;
                    }

                    // 划分数据并计算最大距离
                    double leftMaxDist = 0.0;   // Left 分区到 Pl 的最大距离
                    double rightMaxDist = 0.0;  // Right 分区到 Pr 的最大距离

                    for (int i = 0; i < size; i++)
                    {
                        IndexObject x = data.get(first + i);
                        double ratio = ratios[i];

                        if (ratio < splitRatio)
                        {
                            left.add(x);
                            double d = metric.getDistance(Pl, x);
                            if (d > leftMaxDist) leftMaxDist = d;
                        }
                        else
                        {
                            right.add(x);
                            double d = metric.getDistance(Pr, x);
                            if (d > rightMaxDist) rightMaxDist = d;
                        }
                    }

                    List<List<? extends IndexObject>> subDataList = new ArrayList<>(2);
                    subDataList.add(left);
                    subDataList.add(right);

                    return new RGHPartitionResults(subDataList, pivots, splitRatio,
                            leftMaxDist, rightMaxDist,
                            leftMaxDist, leftMaxDist,  // 子节点使用自己的半径
                            rightMaxDist, rightMaxDist);
                }
            }
}