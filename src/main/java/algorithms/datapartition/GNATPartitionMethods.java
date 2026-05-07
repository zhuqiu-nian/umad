package algorithms.datapartition;

import db.type.IndexObject;
import index.structure.GNATPartitionResults;
import index.structure.PartitionResults;
import metric.Metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public enum GNATPartitionMethods implements PartitionMethod
{
    gnat
            {
                @Override
                public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int numPartitions, int maxLS)
                {
                    return partition(metric, pivots, data, 0, data.size(), numPartitions, maxLS);
                }

                @Override
                public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data, int first, int size, int numPartitions, int maxLS)
                {
                    int numPivots = pivots.length;
                    ArrayList<? super IndexObject>[] subArray = new ArrayList[numPivots];
                    double[][] lowRange = new double[numPivots][numPivots];
                    double[][] upRange = new double[numPivots][numPivots];
                    double[] radius = new double[numPivots];
                    //初始化相关值
                    for (int i = 0; i < numPivots; i++)
                    {
                        subArray[i] = new ArrayList<>();
                        double[] t = new double[numPivots];
                        Arrays.fill(t , Double.MAX_VALUE);
                        lowRange[i] = t;
                        t = new double[numPivots];
                        Arrays.fill(t , Double.MIN_VALUE);
                        upRange[i] = t;
                        radius[i] = Double.MIN_VALUE;
                    }
                    //开始划分
                    data.forEach((Consumer<IndexObject>) indexObject ->
                    {
                        int divHeapIndex = 0;  //存储x最近的支撑点的下标
                        double[] disArr = new double[numPivots];  //存储x到每个支撑点的距离
                        for (int i = 0; i < numPivots; i++)
                        {
                            disArr[i] = metric.getDistance(indexObject, pivots[i]);
                            if (disArr[i] < disArr[divHeapIndex]) divHeapIndex = i;
                        }
                        //更新上下界
                        for (int i = 0; i < numPivots; i++)
                        {
                            if (i==divHeapIndex){
                                lowRange[i][divHeapIndex] = 0;
                                upRange[i][divHeapIndex] = 0;
                            }else{
                                lowRange[i][divHeapIndex] = Math.min(lowRange[i][divHeapIndex], disArr[i]);
                                upRange[i][divHeapIndex] = Math.max(upRange[i][divHeapIndex], disArr[i]);
                            }

                        }
                        //划分到对应的结果集中
                        subArray[divHeapIndex].add(indexObject);
                        radius[divHeapIndex] = Math.max(radius[divHeapIndex], disArr[divHeapIndex]);
                    });
                    List<List<? extends IndexObject>> subDataList = new ArrayList<>();
                    for (int i = 0; i < numPivots; i++)
                    {
                        subDataList.add(subArray[i].stream().map(x-> (IndexObject)x).collect(Collectors.toList()));
                    }
                    return new GNATPartitionResults(subDataList,pivots, lowRange, upRange, radius);
                }
            }
}
