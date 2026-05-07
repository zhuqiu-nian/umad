package index.structure;

import db.type.IndexObject;

import java.util.List;

public class GHPartitionResults extends PartitionResults
{

    /**
     * 划分结果类{@link PartitionResults}的构造函数
     *
     * @param subDataList 数据的划分结果
     * @param pivotSet    采用的支撑点集合
     */
    public GHPartitionResults(List<List<? extends IndexObject>> subDataList, IndexObject[] pivotSet)
    {
        super(subDataList, pivotSet);
    }

    /**
     * 该方法在构建索引树的时候由GHIndex调用。使用该方法获取一个对应的索引树类型的内部节点。
     *
     * @param pivotSet     支撑点集合
     * @param childAddress 内部节点中的孩子节点指针数组。
     * @return 对应索引树类型的内部节点实例
     */
    @Override
    public InternalNode getInstanceOfInternalNode(IndexObject[] pivotSet, long[] childAddress)
    {
        return new GHInternalNode(pivotSet, this.getDataSize(), childAddress);
    }
}
