package index.type;

/**
 * 该枚举提供三种层次选择支撑点的模式，度量空间索引树的建立基本都可以归纳成一个递归的过程，递归的每次执行建立索引树的一个节点。
 * 局部支撑点选取则是在每次递归构建的时候针对当前数据选择支撑点。
 * 全局支撑点选取不进行递归的支撑点选取，整个树只进行一次支撑点选取，而所有的节点都复用相同的支撑点。
 * 混合支撑点选取在每次递归构建的时候从全体数据集中为节点对应的局部数据集选取支撑点。
 * <p>
 * 层次选择支撑点有三种模式分别为（1）局部选点（LOCAL）；（2）全局选点(GLOBAL)；（3）混合选点(MIX)。
 * <p>
 * 具体解释如下：
 * <p>
 * 1.局部选点（LOCAL）：该种模式在递归建树过程中，针对当前数据选择支撑点。每个节点的建立可以分为两个基本的步骤：支撑点选取和数据划分。
 * 伪代码如下：
 * <pre>
 *              BUILD_TREE_LOCAL(DATA S):
 *                  Partition &lt;- S
 *                  REPEAT:
 *                      Pivot &lt;- SELECT_PIVOT(Partition)
 *                      Partition &lt;- PARTITION_DATA(Partition, Pivot)
 *                      CREATE_NODE(Pivot, Partition)
 *                  UNTIL no more data to partition
 * </pre>
 * 局部选点算法中，每次都是在当前数据集中以优化当前数据集的搜索性能为目标进行支撑点选取。也就是说，支撑点选取的候选集和评价集都是当前
 * 数据集。经过划分以后，不同的数据集之间是完全独立的。
 * <p>
 * 2.全局选点（GLOBAL）：全局支撑点选取不进行递归的支撑点选取，整个树只进行一次支撑点选取，而所有的节点都复用相同的支撑点。简化后的
 * 索引树构建算法伪代码如下：
 * <pre>
 *              BUILD_TREE_GLOBAL(DATA S):
 *                  Pivot &lt;- SELECT_PIVOT(S)
 *                  Partition &lt;- S
 *                  REPEAT:
 *                      Partition &lt;- PARTITION_DATA(Partition, Pivot)
 *                      CREATE_NODE(Pivot, Partition)
 *                  UNTIL no more data to partition
 *          </pre>
 * 全局支撑点选取不进行递归的支撑点选取，整个树只进行一次支撑点选取，而所有的节点都复用相同的支撑点。
 * <p>
 * 3.混合选点（MIX）：该种模式将全体数据作为支撑点选择的候选集，在递归建树过程中，将当前划分中的数据作为评价集，从候选集中选出使得评价集上目标函数最优的支撑点集合。
 * <pre>
 *              BUILD_TREE_MIX(DATA S):
 *                  Partition &lt;- S
 *                  REPEAT:
 *                      Pivot &lt;- SELECT_PIVOT(S, Partition)
 *                      Partition &lt;- PARTITION_DATA(Partition, Pivot)
 *                      CREATE_NODE(Pivot, Partition)
 *                  UNTIL no more data to partition
 *          </pre>
 * 在构建索引树的根节点以下的节点的时候，从全体数据集中为节点对应的局部数据集选取支撑点。为此扩展支撑点选取函数，传入的数据作为支撑点的评价集，支撑点从全体数据中选取。
 */
public enum HierarchicalPivotSelectionMode
{
    /**
     * 该种模式在递归建树过程中，针对当前数据选择支撑点。
     */
    LOCAL,
    /**
     * 全局支撑点选取不进行递归的支撑点选取，整个树只进行一次支撑点选取，而所有的节点都复用相同的支撑点。
     */
    GLOBAL,
    /**
     * 该种模式将全体数据作为支撑点选择的候选集，在递归建树过程中，将当前划分中的数据作为评价集，从候选集中选出使得评价集上目标函数最优的支撑点集合。
     */
    MIX
}
