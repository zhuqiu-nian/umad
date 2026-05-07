package index.search;


import db.type.IndexObject;

/**
 * 范围搜索的实体类。
 * 该类包装范围搜索时所需要的查询对象的实体。
 */
public class RangeQuery implements Query
{

    private final double      radius;
    private final IndexObject center;

    /**
     * {@link RangeQuery}类的构造函数
     *
     * @param center 搜索中心
     * @param radius 搜索半径
     */
    public RangeQuery(IndexObject center, double radius)
    {
        if (radius < 0.0) throw new IllegalArgumentException("radius < 0: " + radius);

        this.radius = radius;
        this.center = center;
    }

    /**
     * 获取查询对象
     *
     * @return 返回搜索中心点
     */
    public IndexObject getQueryObject()
    {
        return center;
    }

    /**
     * 获取查询半径
     *
     * @return 返回查询半径
     */
    final public double getRadius()
    {
        return radius;
    }

    @Override
    public String toString()
    {
        return "RangeQuery{" + "radius=" + radius + ", center=" + center + '}';
    }
}
