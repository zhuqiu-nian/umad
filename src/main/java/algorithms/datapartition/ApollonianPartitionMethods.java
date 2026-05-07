package algorithms.datapartition;

import db.type.IndexObject;
import index.structure.ApollonianPartitionResults;
import index.structure.PartitionResults;
import metric.Metric;

import java.util.ArrayList;
import java.util.List;

/**
 * Apollonian Tree 的分区方法
 * 使用两个 pivot (c1, c2) 和比率进行三分划分：
 * left: ratio < c1_ratio
 * mid: c1_ratio <= ratio <= c2_ratio
 * right: ratio > c2_ratio
 */
public enum ApollonianPartitionMethods implements PartitionMethod
{
    /**
     * Apollonian 三分划分方法
     */
    APOLLONIAN
            {
                /**
                 * @param metric 距离函数
                 * @param pivots 支撑点集合 [c1, c2]
                 * @param data 数据
                 * @param numPartitions 分区数目（必须是3）
                 * @return 分区结果对象 {@link PartitionResults}
                 */
                public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int numPartitions, int maxLS)
                {
                    return partition(metric, pivots, data, 0, data.size(), numPartitions, maxLS);
                }

                /**
                 * @param metric 距离函数
                 * @param pivots 支撑点集合 [c1, c2]
                 * @param data 数据
                 * @param first 第一个元素的偏移量
                 * @param size 数据大小
                 * @param numPartitions 分区数目（必须是3）
                 * @return 分区结果对象 {@link PartitionResults}
                 */
                public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int first, int size, int numPartitions, int maxLS)
                {
                    if (pivots.length != 2)
                    {
                        throw new IllegalArgumentException("Apollonian partition requires exactly 2 pivots, but got " + pivots.length);
                    }

                    if (numPartitions != 3)
                    {
                        throw new IllegalArgumentException("Apollonian partition requires exactly 3 partitions, but got " + numPartitions);
                    }

                    IndexObject c1 = pivots[0];
                    IndexObject c2 = pivots[1];

                    List<IndexObject> left = new ArrayList<>();
                    List<IndexObject> mid = new ArrayList<>();
                    List<IndexObject> right = new ArrayList<>();

                    // 计算比率阈值
                    double[] ratios = new double[size];
                    for (int i = 0; i < size; i++)
                    {
                        IndexObject x = data.get(first + i);
                        double d1 = metric.getDistance(c1, x);
                        double d2 = metric.getDistance(c2, x);

                        if (d2 == 0)
                        {
                            // 如果 d2 为 0，设置为最大值（归入 right）
                            ratios[i] = Double.MAX_VALUE;
                        }
                        else
                        {
                            ratios[i] = d1 / d2;
                        }
                    }

                    // 排序找阈值
                    double[] sortedRatios = ratios.clone();
                    java.util.Arrays.sort(sortedRatios);

                    int n = sortedRatios.length;
                    double c1_ratio, c2_ratio;

                    if (n == 1)
                    {
                        double r = sortedRatios[0];
                        if (r <= 0 || r == Double.MAX_VALUE)
                        {
                            c1_ratio = 0.5;
                            c2_ratio = 2.0;
                        }
                        else
                        {
                            c1_ratio = r * 0.9;
                            c2_ratio = r * 1.1;
                        }
                    }
                    else
                    {
                        int idx1 = n / 3;
                        int idx2 = (2 * n) / 3;
                        c1_ratio = sortedRatios[idx1];
                        c2_ratio = sortedRatios[idx2];

                        if (c1_ratio <= 0) c1_ratio = Double.MIN_VALUE;
                        if (c2_ratio <= c1_ratio)
                        {
                            c2_ratio = (c1_ratio < 1.0) ? c1_ratio * 2.0 : c1_ratio + 1.0;
                        }
                    }

                    // 划分数据
                    double M1 = 0.0;  // left 分区到 c1 的最大距离
                    double M2 = 0.0;  // right 分区到 c2 的最大距离

                    for (int i = 0; i < size; i++)
                    {
                        IndexObject x = data.get(first + i);
                        double ratio = ratios[i];

                        if (ratio < c1_ratio)
                        {
                            left.add(x);
                            double d = metric.getDistance(c1, x);
                            if (d > M1) M1 = d;
                        }
                        else if (ratio > c2_ratio)
                        {
                            right.add(x);
                            double d = metric.getDistance(c2, x);
                            if (d > M2) M2 = d;
                        }
                        else
                        {
                            mid.add(x);
                        }
                    }

                    List<List<? extends IndexObject>> subDataList = new ArrayList<>(3);
                    subDataList.add(left);
                    subDataList.add(mid);
                    subDataList.add(right);

                    return new ApollonianPartitionResults(subDataList, pivots, c1_ratio, c2_ratio, M1, M2);
                }
            }
}