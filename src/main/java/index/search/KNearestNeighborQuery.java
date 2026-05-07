package index.search;


import db.type.IndexObject;

//该代码为老版本代码，还未进行修改

/**
 * 给定查询对象q和k，返回数据中距离查询对象q最近的k个节点
 * Given a query object and k, return the k points in the database with the smallest disances to the query.
 */
public class KNearestNeighborQuery implements Query
{
    /**
     * 初始化查询对象
     * Initializes the range query object
     *
     * @param center the {@link Object} that serves as the query object
     * @param k      the search.txt radius of the range query.
     **/
    public KNearestNeighborQuery(IndexObject center, int k)
    {
        if (k < 1) throw new IllegalArgumentException("k < 1: " + k);

        this.k      = k;
        this.center = center;
    }

    /**
     * 返回查询对象的引用
     * Return a reference to the query object
     *
     * @return a reference to the query object
     */
    public IndexObject getQueryObject()
    {
        return center;
    }

    /**
     * 返回最近邻的数量限制k
     *
     * @return 最近邻的数量限制k
     */
    final public int getK()
    {
        return k;
    }

    private final int         k;
    private final IndexObject center;

}
