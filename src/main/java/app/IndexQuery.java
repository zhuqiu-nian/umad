package app;

import db.TableManager;
import db.table.*;
import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.search.Cursor;
import index.search.RangeQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 在索引中进行搜索的操作类。
 */
public class IndexQuery
{
    /**
     * 执行范围查找，将会读取输入文件的全部数据作为查找对象
     *
     * @param tableManager        tableManager名称
     * @param tableName           table名称
     * @param queriedDataFileName 查找对象的文件
     * @param r                   查找半径
     * @return
     * @throws IOException
     */
    public static List<DoubleIndexObjectPair> batchProcessRangeQuery(TableManager tableManager, String tableName, String queriedDataFileName, int queriedDataSetSize, double r) throws IOException
    {
        Table dataTable = tableManager.getTable(tableName);
        if (dataTable == null)
            throw new RuntimeException("tableName不存在！");

        //开始搜索
        //1. 获取待搜索的数据列表
        List<? extends IndexObject> queriedData = getQueriedData(tableManager, tableName, queriedDataFileName, queriedDataSetSize);
        System.out.println("搜索数据加载成功，共载入" + queriedData.size() + "条数据。");
        //2. 新建结果列表
        List<DoubleIndexObjectPair> result      = new ArrayList<>();
        int                         avgResultNo = 0;
        double                      avgDisNo    = 0;
        double                      avgPrnRt    = 0;
        //3. 搜索
        Application.distance = 0;
        Application.averageExclusionRate = 0;
        for (var d : queriedData){
            RangeQuery query = new RangeQuery(d, r);
            Cursor     cursor = dataTable.getIndex().search(query);
            Application.distance += cursor.getDisCounter();
            Application.averageExclusionRate += cursor.averageExclusionRate;
            while (cursor.hasNext()){
                result.add(cursor.next());
            }
        }
        System.out.println("查找完毕，平均每次查找的结果数是" + avgResultNo / queriedData.size() + ";\n平均每次查找的距离计算次数是"
                + avgDisNo / queriedData.size() + ";\n平均每次查找的排除率是"
                + avgPrnRt / queriedData.size());
        //4. 返回结果列表
        return result;
    }

    /**
     * 执行单个查询，返回查询结果列表
     * @param tableManager
     * @param tableName
     * @param q
     * @param r
     * @return
     */
    public static List<DoubleIndexObjectPair> processSingleQuery(TableManager tableManager,String tableName,
                                                        IndexObject q, double r){
        Table dataTable = tableManager.getTable(tableName);
        if (dataTable==null) throw new RuntimeException("tableName不存在！");

        //执行搜索
        RangeQuery query = new RangeQuery(q, r);
        Cursor     cursor = dataTable.getIndex().search(query);
        List<DoubleIndexObjectPair> retList = new ArrayList<>();
        while (cursor.hasNext()){
            retList.add(cursor.next());
        }
        return retList;
    }

    /**
     * 执行批量搜索，并返回平均距离计算次数
     * @param tableManager
     * @param tableName
     * @param queriedDataFileName
     * @param r
     * @throws IOException
     */
    public static double batchProcessRangeQueryAndGetAvgDisCount(TableManager tableManager,String tableName, String queriedDataFileName, int queryDataSize,double r) throws IOException{
        Table dataTable = tableManager.getTable(tableName);
        if (dataTable==null) throw new RuntimeException("tableName不存在！");

        //开始搜索
        //1. 获取待搜索的数据列表
        List<? extends IndexObject> queriedData = getQueriedData(tableManager,tableName,queriedDataFileName,queryDataSize);
        //2. 执行搜索
        int disCount = queriedData.stream().map(x -> new RangeQuery(x, r)).map(query -> dataTable.getIndex().search(query)).mapToInt(Cursor::getDisCounter).sum();
        return disCount/(float)queryDataSize;
    }

    /**
     * 获取指定文件中的数据，作为查询列表返回
     * @param tableManager
     * @param tableName 跟queriedDataFileName同类型的table
     * @param queriedDataFileName
     * @param queriedDataSetSize
     * @return
     * @throws IOException
     */
    public static List<? extends IndexObject> getQueriedData(TableManager tableManager,String tableName, String queriedDataFileName, int queriedDataSetSize) throws IOException
    {
        Table dataTable = tableManager.getTable(tableName);
        if (dataTable == null)
            throw new RuntimeException("tableName不存在！");
        Table queriedTable = null;
        //判断，创建对应的搜索数据table
        if (dataTable instanceof SpectraTable)
        {
            queriedTable = new SpectraTable(queriedDataFileName, "query", queriedDataSetSize, null);
        } else if (dataTable instanceof SpectraWithPrecursorMassTable)
        {
            queriedTable = new SpectraWithPrecursorMassTable(queriedDataFileName, "query", queriedDataSetSize);
        } else if (dataTable instanceof DNATable)
        {
            int fragLength = ((DNATable) dataTable).getFragmentLength();
            queriedTable = new DNATable(queriedDataFileName, "query", queriedDataSetSize, fragLength);
        } else if (dataTable instanceof RNATable)
        {
            int fragLength = ((RNATable) dataTable).getFragmentLength();
            queriedTable = new DNATable(queriedDataFileName, "query", queriedDataSetSize, fragLength);
        } else if (dataTable instanceof PeptideTable)
        {
            int fragLength = ((PeptideTable) dataTable).getFragmentLength();
            queriedTable = new PeptideTable(queriedDataFileName, "query", queriedDataSetSize, fragLength);
        } else if (dataTable instanceof DoubleVectorTable)
        {
            int dim = ((DoubleVectorTable) dataTable).getDim();
            queriedTable = new DoubleVectorTable(queriedDataFileName, "query", queriedDataSetSize, dim);
        } else if (dataTable instanceof ImageTable)
        {
            queriedTable = new ImageTable(queriedDataFileName, "query", queriedDataSetSize);
        } else
        {
            throw new RuntimeException("不支持的类型！");
        }
        if (queriedTable == null)
            throw new RuntimeException("查询数据读入错误！");
        List<? extends IndexObject> queriedData = queriedTable.getData();
        return queriedData;
    }

    public static Cursor processRangeQuery(TableManager tableManager, String tableName, String queryFileName, Double queryRadius) throws IOException
    {
        Table dataTable = tableManager.getTable(tableName);
        if (dataTable==null) throw new RuntimeException("tableName不存在！");

        //开始搜索
        //1. 获取待搜索的数据列表
        List<? extends IndexObject> queriedData = getQueriedData(tableManager,tableName,queryFileName,1);
        //2. 执行搜索
        return dataTable.getIndex().search(new RangeQuery(queriedData.get(0), queryRadius));
    }
}
