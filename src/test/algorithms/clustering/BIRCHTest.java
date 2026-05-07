package algorithms.clustering;

import app.Clustering;
import db.TableManager;
import db.table.DoubleVectorTable;
import db.table.Table;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;

public class BIRCHTest extends TestCase
{
    TableManager tableManager = TableManager.getTableManager("birch");

    @Test
    public static void testExecuteCluster() throws IOException
    {
        String dataFileName = "D:\\WorkSpace\\IDEA\\UMAD\\dataset\\clustering\\simulationData\\vector_2dim_4cluster_10000.txt";
        int    maxDataSize  = 10000;
        int    dim          = 2;
        //组合输出目录
        var          paths        = dataFileName.split("\\\\");
        StringBuffer stringBuffer = new StringBuffer(paths[0]);
        for (int i = 1; i < paths.length - 1; i++)
        {
            stringBuffer.append("\\" + paths[i]);
        }
        String[] split = paths[paths.length - 1].split("\\.");
        stringBuffer.append("\\" + split[0] + "_ger.txt");
        String outFilePath = new String(stringBuffer);

        Table      table      = new DoubleVectorTable(dataFileName, "cluster", maxDataSize, dim);
        Clustering clustering = new Clustering(table.getData());
        clustering.executeBIRCH(0.5, 50, 4);
        clustering.writeToTxt(outFilePath);
        System.out.println("聚类完成，输出文件为：" + outFilePath);
    }

    @After
    public void tearDown() throws Exception
    {
        tableManager.close();
    }

}