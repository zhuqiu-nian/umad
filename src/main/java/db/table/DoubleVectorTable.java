package db.table;

import app.Application;
import db.type.DoubleVector;
import metric.LMetric;
import metric.Metric;
import util.Debug;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * 向量文件的数据表
 *
 * @author willard
 */
public class DoubleVectorTable extends Table
{

    private static final long serialVersionUID = 7630078213101669086L;

    /**
     * 用于构建索引的距离函数
     */
    private static final Metric DEFAULT_METRIC = LMetric.EuclideanDistanceMetric;

    /**
     * 输入向量的维数
     */
    private int dim;

    /**
     * 返回向量维数
     * @return
     */
    public int getDim()
    {
        return dim;
    }

    /**
     * 构造函数，默认采用欧几里得距离构建索引
     *
     * @param fileName    要构建索引的数据文件
     * @param indexPrefix 输出索引文件的前缀
     * @param size        待读入点的数目
     * @param dimNum      待读入数据的维度
     * @throws IOException IO异常
     */
    public DoubleVectorTable(String fileName, String indexPrefix, int size, int dimNum) throws IOException
    {
        this(fileName, indexPrefix, size, dimNum, DEFAULT_METRIC);
    }

    /**
     * 构造函数
     *
     * @param fileName    要构建索引的数据文件
     * @param indexPrefix 输出索引文件的前缀
     * @param size        待读入点的数目
     * @param dimNum      待读入数据的维度
     * @param metric      用于构建索引的距离函数
     * @throws IOException IO异常
     */
    public DoubleVectorTable(String fileName, String indexPrefix, int size, int dimNum, Metric metric) throws IOException
    {
        super(fileName, indexPrefix, size, metric);
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        this.dim = dimNum;
        loadData(reader, size, dimNum);
        //如果实际读入的数据量小于期待读入的数据量，则将数据集大小设置为实际读入的数据量
        if (data.size() < this.dataSize)
            this.dataSize = data.size();
    }

    /**
     * 加载数据入内存
     *
     * @param reader  文件读写流
     * @param maxSize 最大向量数目
     * @param dimNum  要读取的向量的维度
     * @throws IOException IOException
     */
    public void loadData(BufferedReader reader, int maxSize, int dimNum) throws IOException
    {
        //默认数据集的第一行有两个元素的，第一个代表维数，第二个代表数据量
        String                  line;
        ArrayList<DoubleVector> doubleVectors           = new ArrayList<DoubleVector>();
        ArrayList<Integer>      originalRowIDsArrayList = new ArrayList<Integer>();
        // read vector values from file
        line = reader.readLine(); // read the first line
        if (line != null)
            line = line.trim();

        String[] metaData = line.split("[ \t]+");

        if (metaData.length != 2)
        {
            System.out.println("Error: Cannot parse the data file.");
            System.exit(-1);
        }

        final int dim = Integer.parseInt(metaData[0]); // dimension
        if (dimNum > dim)
        {
            dimNum = dim;
            //保存输入向量的维数
            this.dim = dimNum;
        }

        /*
         * int size = java.lang.Integer.parseInt(metaData[1]); // total number of
         */        // data

        if (Debug.debug)
        {
            Logger.getLogger("index").info("dim: " + dim);
            Logger.getLogger("index").info("size: " + maxSize);
        }

        int      numData = 0;
        double[] data    = new double[dimNum];

        line = reader.readLine();
        if (line != null)
            line = line.trim();

        while (line != null && numData < maxSize)
        {
            String[] row = line.split("[ \t]+");

            if (Debug.debug)
            {
                for (int i = 0; i < row.length; i++)
                     Logger.getLogger("index").finer("row[" + i + "]: " + row[i]);
            }

            for (int i = 0; i < dimNum; i++)
                 data[i] = Double.parseDouble(row[i]);

            // System.out.println(new DoubleVector(new Integer(numData), data));

            originalRowIDsArrayList.add(numData, numData);
            doubleVectors.add(new DoubleVector(this, numData, data));

            line = reader.readLine();
            if (line != null)
                line = line.trim();

            numData++;
        }
        doubleVectors.trimToSize();
        this.data = doubleVectors;
        Application.globalData = doubleVectors;
        originalRowIDs = new int[originalRowIDsArrayList.size()];
        for (int i = 0, e = originalRowIDsArrayList.size(); i < e; i++)
        {
            originalRowIDs[i] = originalRowIDsArrayList.get(i);
        }
    }

    /**
     * 加载部分数据入内存
     *
     * @param reader {@link BufferedReader}缓冲流
     * @param size   要加载进的大小
     */
    @Override
    public void loadData(BufferedReader reader, int size)
    {
        throw new UnsupportedOperationException();
    }
}
