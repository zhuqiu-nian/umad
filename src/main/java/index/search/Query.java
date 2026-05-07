package index.search;


import db.type.IndexObject;

/**
 * {@code Query}所有搜索方法必须实现该接口。
 * Base interface for queries, consisting of all the information necessary to search.txt the database.
 */
public interface Query
{
    /**
     * 获取查询对象
     *
     * @return 返回搜索方法对象
     */
    IndexObject getQueryObject();
}
