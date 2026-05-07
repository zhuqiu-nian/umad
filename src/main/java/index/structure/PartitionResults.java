package index.structure;

import db.type.IndexObject;

import java.util.List;

/**
 * 划分结果的抽象类。
 *
 * <p>
 * 该类作为索引树采用的划分方法所对应的划分结果的抽象类。如果要实现一种新的索引树类型，就必须要实现对应的划分结果类。
 * 建树的过程中，要调用该类的{@code getInstanceOfInternalNode(long[] childAddress)}方法，获取对应的索引树类型的节点实例。
 * 所有的子类要实现该抽象方法，通过该方法向建树过程提供一个内部节点的实例。
 * </p>
 */
public abstract class PartitionResults
{

    List<List<? extends IndexObject>> listOfPartitions;
    IndexObject[]                     pivotSet; //支撑点集合


    /**
     * 划分结果类{@link PartitionResults}的构造函数
     *
     * @param subDataList 数据的划分结果
     * @param pivotSet    采用的支撑点集合
     */
    public PartitionResults(List<List<? extends IndexObject>> subDataList, IndexObject[] pivotSet)
    {
        this.listOfPartitions = subDataList;
        this.pivotSet         = pivotSet;
    }

    /**
     * 获取划分的块数
     *
     * @return 划分的块数
     */
    public int getNumPartition()
    {
        return listOfPartitions.size();
    }


    /**
     * 向划分结果集中添加新的数据块
     *
     * @param partition 待添加的新的数据块
     */
    public void addPartition(List<? extends IndexObject> partition)
    {
        listOfPartitions.add(partition);
    }

    /**
     * 获取划分结果的第index块
     *
     * @param index 要返回的数据块的索引
     * @return 返回划分结果的第index块
     */
    public List<? extends IndexObject> getPartitionOf(int index)
    {
        return listOfPartitions.get(index);
    }

    /**
     * 该方法在构建索引树的时候由AbstractIndex调用。使用该方法获取一个对应的索引树类型的内部节点。
     *
     * @param pivotSet     支撑点集合
     * @param childAddress 内部节点中的孩子节点指针数组。
     * @return 对应索引树类型的内部节点实例
     */
    public abstract InternalNode getInstanceOfInternalNode(IndexObject[] pivotSet, long[] childAddress);

    /**
     * 获取该{@link PartitionResults}对象包含的所有的数据大小
     *
     * @return 返回该对象包含的所有的数据大小
     */
    public int getDataSize()
    {
        int dataSize = 0;
        for (int i = 0; i < this.listOfPartitions.size(); i++)
        {
            dataSize += listOfPartitions.get(i).size();
        }
        dataSize += this.pivotSet.length;
        return dataSize;
    }
}
