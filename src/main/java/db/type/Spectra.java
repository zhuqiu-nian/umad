package db.type;

import db.table.Table;
import util.Debug;

import java.util.Arrays;
import java.util.logging.Logger;


public class Spectra extends DoubleVector
{
    private static final long serialVersionUID = -8300375927493085758L;

    /**
     *
     */
    public Spectra()
    {
    }

    /**
     * @param table   存储数据的table对象
     * @param rowID   行号
     * @param spectra spectra
     */
    public Spectra(Table table, int rowID, String spectra)
    {
        super(table, rowID, spectra);
        // make sure query is sorted
        sortAsc();
    }

    /**
     * @param table   存储数据的table对象
     * @param rowID   行号
     * @param spectra spectra
     */
    public Spectra(Table table, int rowID, double[] spectra)
    {
        super(table, rowID, spectra);
        // make sure query is sorted
        sortAsc();
    }

    /**
     *
     */
    public void sortAsc()
    {
        try
        {
            Arrays.sort(data);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        if (Debug.debug)
        {
            Logger.getLogger("index").finer("Sorted Spectra = " + toString());
        }
    }

    /**
     * 获取数排列好的据最小值
     *
     * @return 返回最小值
     */
    public double getMin()
    {
        return data[0];
    }

    /**
     * 获取排列好的数据最大值
     *
     * @return 返回最大值
     */
    public double getMax()
    {
        return data[data.length - 1];
    }


    public IndexObject[] expand()
    {
        IndexObject[] dbO = new IndexObject[rowIDLength];
        for (int i = 0; i < rowIDLength; i++)
        {
            dbO[i] = new Spectra(table, rowIDStart + i, data);
        }
        return dbO;
    }
}
