package db.table;


import app.Application;
import db.type.Alphabet;
import db.type.Fragment;
import db.type.IndexObject;
import db.type.Sequence;
import metric.SequenceFragmentMetric;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 序列数据表
 */
public abstract class SequenceTable extends Table
{
    private static final long serialVersionUID = -1519446228237674948L;

    public Alphabet alphabet;

    public int fragmentLength;

    public Sequence[] sequences;

    public int[] fragmentOffsets;

    /**
     * 构造函数
     *
     * @param fileName       构建索引的文件名
     * @param indexPrefix    索引前缀名
     * @param maxSize        要读入的数据集的大小
     * @param metric         距离函数
     * @param fragmentLength 片段长度
     * @throws IOException IO异常
     */
    protected SequenceTable(String fileName, String indexPrefix, int maxSize, SequenceFragmentMetric metric, int fragmentLength) throws IOException
    {
        super(fileName, indexPrefix, maxSize, metric);
        if (fragmentLength <= 0) throw new IllegalArgumentException("fragment length must be greater than zero!");

        this.alphabet       = metric.getWeightMatrix().getAlphabet();
        this.fragmentLength = fragmentLength;

        BufferedReader reader = new BufferedReader(new java.io.FileReader(fileName));
        loadData(reader, maxSize);
        initFragmentList(maxSize);
        //如果实际读入的数据量小于期待读入的数据量，则将数据集大小设置为实际读入的数据量
        if (data.size() < this.dataSize) this.dataSize = data.size();
    }

    /**
     * 初始化 FragmentList
     *
     * @param size 初始化size
     */
    private void initFragmentList(int size)
    {
        int count = 0;
        // first figure out how long arrays are going to be;
        for (int i = 0; i < sequences.length; i++)
        {
            int numFragments = sequences[i].numFragments(fragmentLength);
            for (int j = 0; j < numFragments; j++)
            {
                if (count < size)
                {
                    count++;
                }
            }
        }
        // init rowIDs list;
        ArrayList<Fragment> fragmentList = new ArrayList<Fragment>(count);
        originalRowIDs  = new int[count];
        fragmentOffsets = new int[count];

        // reset count;
        count = 0;
        for (int i = 0; i < sequences.length; i++)
        {
            int numFragments = sequences[i].numFragments(fragmentLength);
            for (int j = 0; j < numFragments; j++)
            {
                if (count < size)
                {
                    this.originalRowIDs[count]  = i;
                    this.fragmentOffsets[count] = j;
                    Fragment frag = new Fragment(this, count);
                    fragmentList.add(frag);
                    count++;
                }
            }
        }

        fragmentList.trimToSize();
        data = fragmentList;
        Application.globalData = fragmentList;
    }

    /**
     * 重复数据压缩
     */
    public void compressData()
    {
        // first sort the list according to the data points.
        Collections.sort(data);

        // then, make a list of the unique dataPoints.
        final int              dataSize       = data.size();
        ArrayList<IndexObject> compressedData = new ArrayList<IndexObject>(dataSize);
        int[]                  rowIDs2        = new int[dataSize];
        int[]                  dataOffset2    = new int[dataSize];

        IndexObject dataPoint1 = data.get(0);
        int         tempSize   = 1;

        IndexObject dataPoint2;
        for (int i = 1; i < dataSize; i++)
        {
            dataPoint2 = data.get(i);
            if (dataPoint1.equals(dataPoint2))
            {
                tempSize++;
            } else
            {
                if (tempSize > 1)
                {
                    for (int j = i - tempSize; j < i; j++)
                    {
                        int rowID = data.get(j).getRowID();
                        rowIDs2[j]     = originalRowIDs[rowID];
                        dataOffset2[j] = fragmentOffsets[rowID];
                    }
                    dataPoint1.setRowID(i - tempSize);
                    dataPoint1.setRowIDLength(tempSize);
                } else
                {
                    int rowID = data.get(i - 1).getRowID();
                    rowIDs2[i - 1]     = originalRowIDs[rowID];
                    dataOffset2[i - 1] = fragmentOffsets[rowID];
                    dataPoint1.setRowID(i - 1);
                }
                compressedData.add(dataPoint1);
                dataPoint1 = dataPoint2;
                tempSize   = 1;
            }
        }

        if (tempSize > 1)
        {
            for (int i = dataSize - tempSize; i < dataSize; i++)
            {
                int rowID = data.get(i).getRowID();
                rowIDs2[i]     = originalRowIDs[rowID];
                dataOffset2[i] = fragmentOffsets[rowID];
            }
            dataPoint1.setRowID(dataSize - tempSize);
            dataPoint1.setRowIDLength(tempSize);
        } else
        {
            int rowID = data.get(dataSize - 1).getRowID();
            rowIDs2[dataSize - 1]     = originalRowIDs[rowID];
            dataOffset2[dataSize - 1] = fragmentOffsets[rowID];
            dataPoint1.setRowID(dataSize - 1);
        }
        compressedData.add(dataPoint1);

        compressedData.trimToSize();
        //System.out.println("original size: " + dataSize + " compressed data size: " + compressedData.size());
        data            = compressedData;
        originalRowIDs  = rowIDs2;
        fragmentOffsets = dataOffset2;
    }

    /**
     * 获取{@code fragmentLength}
     *
     * @return 返回{@code fragmentLength}
     */
    public int getFragmentLength()
    {
        return fragmentLength;
    }

    /**
     * 获取相应位置的 {@code fragmentOffset}
     *
     * @param rowID 想要获取 {@code fragmentOffset} 的行号
     * @return 返回相应位置的 {@code fragmentOffset}
     */
    public int getFragmentOffset(int rowID)
    {
        return fragmentOffsets[rowID];
    }

}
