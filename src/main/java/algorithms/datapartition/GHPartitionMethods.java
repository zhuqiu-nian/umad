package algorithms.datapartition;

import db.type.IndexObject;
import index.structure.GHPartitionResults;
import index.structure.PartitionResults;
import metric.Metric;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 该类实现了GH树可以用的划分方法。
 * <p>
 *  GH:默认的GH划分，根据数据到两个支撑点的远近将数据划分到对应的块中。
 */
public enum GHPartitionMethods implements PartitionMethod
{
    /**
     * 默认的GH划分，根据数据到两个支撑点的远近将数据划分到对应的块中。
     */
    GH {
        /**
         * 执行划分操作
         *
         * @param metric        划分使用的距离函数
         * @param pivots        划分使用的支撑点集合
         * @param data          待划分的数据集
         * @param numPartitions 划分的块数
         * @param maxLS         建树的叶子结点的最大大小
         * @return 划分结果
         */
        @Override
        public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data,
                                          int numPartitions, int maxLS)
        {
            return partition(metric, pivots, data, 0, data.size(), numPartitions, maxLS);
        }

        /**
         * 执行划分操作
         *
         * @param metric        划分使用的距离函数
         * @param pivots        划分使用的支撑点集合
         * @param data          待划分的数据集
         * @param first         数据的起始位置
         * @param size          数据大小
         * @param numPartitions 划分的块数
         * @param maxLS         建树的叶子结点的最大大小
         * @return 划分结果
         */
        @Override
        public PartitionResults partition(Metric metric, IndexObject[] pivots, List<? extends IndexObject> data,
                                          int first, int size, int numPartitions, int maxLS)
        {
            if(pivots.length != 2)
                throw new RuntimeException("GHT支撑点个数必须2");
            if(numPartitions != 2)
                throw new RuntimeException("GHT单个节点的扇出必须2");
            //subDataList用于保存GH划分的结果
            List<List<? extends IndexObject>> subDataList = new LinkedList<>();
            //a1用于保存距离支撑点p1近的数据
            ArrayList<IndexObject> a1 = new ArrayList<>();
            //a2用于保存距离支撑点p2近的数据
            ArrayList<IndexObject> a2 = new ArrayList<>();

            double dis1, dis2;
            //遍历数据，距离支撑点p1近的数据加入到a1中，距离支撑点p2近的数据加入a2中
            for(int i = first; i < first + size; i++)
            {
                IndexObject d = data.get(i);
                dis1 = metric.getDistance(d, pivots[0]);
                dis2 = metric.getDistance(d, pivots[1]);
                if(dis1 < dis2)
                {
                    a1.add(d);
                }
                else
                {
                    a2.add(d);
                }
            }
            a1.trimToSize();
            a2.trimToSize();
            subDataList.add(a1);
            subDataList.add(a2);

            return new GHPartitionResults(subDataList, pivots);
        }
    }
}
