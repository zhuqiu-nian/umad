package index.structure;

import db.type.IndexObject;
import index.search.Cursor;
import index.search.Query;
import metric.Metric;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;


/**
 * 基于距离索引的最主要的接口。
 *
 * <p>
 * 通过index，用户可以构建索引数据库或者从文件中读入一个预先构建好的索引数据库文件，然后进行搜索操作。
 * </p>
 */
public interface Index extends Serializable
{
    /**
     * 获取当前索引的距离函数
     *
     * @return 返回用于构建索引的距离函数
     */
    Metric getMetric();

    /**
     * 获取当前索引的数据大小
     *
     * @return 返回索引数据库的数据对象的总数目
     */
    int size();

    /**
     * 执行范围搜索
     *
     * <p>
     * 对于范围搜索{@code R(q,r)}，返回的搜索结果应该是数据库中所有满足{@code d(q,x)<=r}的点。
     * </p>
     *
     * @param query {@link Query} 对象
     * @return 结果集迭代器 {@link Cursor}
     */
    Cursor search(Query query);

    /**
     * 获取当前索引中的所有数据点
     *
     * @return 返回索引列表中的所有数据点
     */
    List<? extends IndexObject> getAllPoints();

    /**
     * 获取建树使用的支撑点数目
     * @return 建树中使用的支撑点数目
     */
    int getPivotNum();

    /**
     * 获取被存为支撑点的数据
     * @return 支撑点数据集合
     */
    IndexObject[] getAllPivots();

    /**
     * 在MIX建树模式时 初步建树完成后 层次化遍历整棵树
     * 读取所有的支撑点 同时存储 所有叶子节点指针
     * 完成后 对每个叶子节点读出 进而将 数据分为被选为过支撑点的数据 和 普通数据
     * 完成树的清洗 目的 ： 完美解决重复计算和重复添加的问题
     */
    void MixLeafNodeClear();

    /**
     * 关闭索引内部使用的读写对象
     * Closes anything used internally that needs to be closed. It is a good idea to close it when
     * it is no longer in use.
     *
     * @throws IOException IOException
     */
    void close() throws IOException;

    /**
     * 将索引文件删除，并释放内存。该方法仅在当前索引不再需要时使用。
     * Deletes the index from the file, and also release it from memory. This method should only be
     * called when the index will never be used again!
     *
     */
    void destroy();

    /**
     * 构建索引树。
     */
    void buildTree();

    /**
     * 遍历该索引树并输出到磁盘上
     *
     * @param dir 输出文件的保存目录
     */
    void outputIndexTree(String dir);

    /**
     * 获取根节点的划分的r-Neighborhood中的点的数目
     * @param radius r-Neighborhood的大小
     * @return 根节点的划分的r-Neighborhood中的点的数目
     */
    int getNumberOfPointInrNeighborhoodForRootLevel(double radius);
}
