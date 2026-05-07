package app;

import algorithms.datapartition.GNATPartitionMethods;
import algorithms.datapartition.KMPartitionMethods;
import algorithms.datapartition.VPPartitionMethods;
import algorithms.pivotselection.PivotSelectionMethods;
import db.TableManager;
import db.table.Table;
import db.type.DoubleIndexObjectPair;
import db.type.IndexObject;
import index.search.Cursor;
import index.search.RangeQuery;
import index.type.HierarchicalPivotSelectionMode;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainTest extends TestCase
{
    TableManager tableManager;
    @Before
    public void setUp()
    {
        tableManager = TableManager.getTableManager("liu");
    }

    @Test
    public void testKmp() throws IOException
    {
        String vpTableName = IndexBuilder.buildVPIndexOnVector(tableManager, "dataset/vector/randomvector-5-1m",
                5, 1000000, PivotSelectionMethods.FFT, 2, VPPartitionMethods.BALANCED,
                2,20, HierarchicalPivotSelectionMode.LOCAL,null);
        String kmpTableName = IndexBuilder.buildKMPIndexOnVector(tableManager, "dataset/vector/randomvector-5-1m",
                2,1000000,PivotSelectionMethods.FFT, KMPartitionMethods.KMeans,2,
                4,40, HierarchicalPivotSelectionMode.LOCAL,null);
        String gnatTableName = IndexBuilder.buildGNATIndexOnVector(tableManager, "dataset/vector/randomvector-5-1m",
                2,1000000,PivotSelectionMethods.FFT,
                4, GNATPartitionMethods.gnat,40, HierarchicalPivotSelectionMode.LOCAL,null);
//        batchProcessRangeQuery(tableManager, vpTableName, "dataset/vector/hawii.txt",
//                2000, 0.2);
//        batchProcessRangeQuery(tableManager, kmpTableName, "dataset/vector/hawii.txt",
//                2000, 0.2);
//        batchProcessRangeQuery(tableManager, gnatTableName, "dataset/vector/hawii.txt",
//                2000, 0.2);
    }

    public static List<DoubleIndexObjectPair> batchProcessRangeQuery(TableManager tableManager, String tableName, String queriedDataFileName, int queriedDataSetSize, double r) throws IOException
    {
        Table dataTable = tableManager.getTable(tableName);
        if (dataTable == null)
            throw new RuntimeException("tableName不存在！");

        //开始搜索
        //1. 获取待搜索的数据列表
        List<? extends IndexObject> queriedData = IndexQuery.getQueriedData(tableManager, tableName, queriedDataFileName, queriedDataSetSize);
        System.out.println("搜索数据加载成功，共载入" + queriedData.size() + "条数据。");
        //2. 新建结果列表
        List<DoubleIndexObjectPair> result      = new ArrayList<>();
        int                         avgResultNo = 0;
        double                      avgDisNo    = 0;
        double                      avgPrnRt    = 0;
        //3. 搜索
        for (var d : queriedData)
        {
            RangeQuery query = new RangeQuery(d, r);
//            System.out.println("开始查找" + query);
            Cursor cursor = dataTable.getIndex().search(query);
//            System.out.println("查找完毕，共找到" + cursor.remainingSizeOfTheResult() + "条结果，进行了"
//                    + cursor.getDisCounter() + "次距离计算，平均排除率是" + cursor.averageExclusionRate);
            avgResultNo += cursor.remainingSizeOfTheResult();
            avgDisNo += cursor.getDisCounter();
            avgPrnRt += cursor.averageExclusionRate;
//            System.out.println("结果如下：");
            while (cursor.hasNext())
            {
                DoubleIndexObjectPair next = cursor.next();
//                System.out.println(next);
                result.add(next);
            }
        }
        System.out.println("查找完毕，平均每次查找的结果数是" + avgResultNo / queriedData.size() + ";\n平均每次查找的距离计算次数是"
                + avgDisNo / queriedData.size() + ";\n平均每次查找的排除率是"
                + avgPrnRt / queriedData.size());
        //4. 返回结果列表
        return result;
    }

    @After
    public void tearDown() throws Exception
    {
        tableManager.close();
    }
}