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

                    // 计算比率阈值 (使用中位数)，严格对齐 C++ calculateSplitRatio：
                    // dPr == 0 的点不参与 splitRatio 计算，但划分时归入右侧。
                    List<Double> validRatios = new ArrayList<>();
                    double[] ratios = new double[size];
                    for (int i = 0; i < size; i++)
                    {
                        IndexObject x = data.get(first + i);
                        double dPl = metric.getDistance(Pl, x);
                        double dPr = metric.getDistance(Pr, x);

                        if (dPr == 0.0)
                        {
                            // 如果 dPr 为 0，划分阶段归入右侧，但不参与中位数阈值计算。
                            ratios[i] = Double.MAX_VALUE;
                        }
                        else
                        {
                            ratios[i] = dPl / dPr;
                            validRatios.add(ratios[i]);
                        }
                    }

                    double splitRatio;
                    if (validRatios.isEmpty())
                    {
                        splitRatio = 1.0;
                    }
                    else
                    {
                        double[] sortedRatios = new double[validRatios.size()];
                        for (int i = 0; i < validRatios.size(); i++)
                        {
                            sortedRatios[i] = validRatios.get(i);
                        }
                        Arrays.sort(sortedRatios);
                        splitRatio = sortedRatios[sortedRatios.length / 2];
                    }

                    List<IndexObject> cl = new ArrayList<>(); // ratio < R
                    List<IndexObject> cr = new ArrayList<>(); // ratio > R 或 dPr == 0
                    List<IndexObject> equal = new ArrayList<>(); // ratio == R

                    for (int i = 0; i < size; i++)
                    {
                        IndexObject x = data.get(first + i);
                        double ratio = ratios[i];

                        if (ratio == Double.MAX_VALUE)
                        {
                            cr.add(x);
                        }
                        else if (ratio < splitRatio)
                        {
                            cl.add(x);
                        }
                        else if (ratio > splitRatio)
                        {
                            cr.add(x);
                        }
                        else
                        {
                            equal.add(x);
                        }
                    }

                    // 对齐 C++ partitionData: ratio == R 的 E 集合按公式分配以保持平衡。
                    int a = cl.size() - cr.size();
                    int b = equal.size();
                    int countEl = (b + a + 1) / 2;
                    int countEr = (b - a) / 2;

                    if (countEl < 0) countEl = 0;
                    if (countEr < 0) countEr = 0;
                    if (countEl + countEr > b) countEl = b - countEr;

                    for (int i = 0; i < b; i++)
                    {
                        if (i < countEl)
                        {
                            left.add(equal.get(i));
                        }
                        else
                        {
                            right.add(equal.get(i));
                        }
                    }
                    left.addAll(cl);
                    right.addAll(cr);

                    double leftMaxDist = 0.0;   // Left 分区到 Pl 的最大距离
                    double rightMaxDist = 0.0;  // Right 分区到 Pr 的最大距离
                    for (IndexObject x : left)
                    {
                        double d = metric.getDistance(Pl, x);
                        if (d > leftMaxDist) leftMaxDist = d;
                    }
                    for (IndexObject x : right)
                    {
                        double d = metric.getDistance(Pr, x);
                        if (d > rightMaxDist) rightMaxDist = d;
                    }

                    List<List<? extends IndexObject>> subDataList = new ArrayList<>(2);
                    subDataList.add(left);
                    subDataList.add(right);

                    return new RGHPartitionResults(subDataList, pivots, splitRatio,
                            leftMaxDist, rightMaxDist);
                }
            }
}
