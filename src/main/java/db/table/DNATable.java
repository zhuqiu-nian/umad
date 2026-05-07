package db.table;


import db.type.DNA;
import metric.SequenceFragmentMetric;
import metric.WHDGlobalSequenceFragmentMetric;
import metric.WeightMatrix;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class DNATable extends SequenceTable
{

    private static final long serialVersionUID = 4633926830904730890L;


    public static final WeightMatrix DEFAULT_WEIGHT_MATRIX = DNA.EditDistanceWeightMatrix;

    public static final SequenceFragmentMetric DEFAULT_METRIC = new WHDGlobalSequenceFragmentMetric(DEFAULT_WEIGHT_MATRIX);

    /**
     * 构造函数
     *
     * @param fileName       要读入的数据文件的名字
     * @param indexPrefix    索引前缀名
     * @param dataSize       数据集大小
     * @param fragmentLength 片段长度
     * @throws IOException IO异常
     */
    public DNATable(String fileName, String indexPrefix, int dataSize, int fragmentLength) throws IOException
    {
        this(fileName, indexPrefix, dataSize, DEFAULT_METRIC, fragmentLength);
    }

    /**
     * 构造函数
     *
     * @param fileName       要读入的数据文件的名字
     * @param indexPrefix    索引前缀名
     * @param dataSize       数据集大小
     * @param metric         使用的距离函数
     * @param fragmentLength 片段长度
     * @throws IOException IO异常
     */
    public DNATable(String fileName, String indexPrefix, int dataSize, SequenceFragmentMetric metric, int fragmentLength) throws IOException
    {
        super(fileName, indexPrefix, dataSize, metric, fragmentLength);
    }

    /**
     * 加载部分数据入内存
     *
     * @param reader  {@link BufferedReader}缓冲流
     * @param maxSize 要加载进的大小
     */
    public void loadData(BufferedReader reader, int maxSize)
    {
        String    ident                 = "";
        List<DNA> seqs                  = new ArrayList<DNA>();
        int       counter               = 0;
        int       sequenceLengthCounter = 0;
        try
        {
            // read sequences from file
            StringBuffer currentSequence = new StringBuffer();
            String       line            = reader.readLine();
            if (line != null) line = line.trim();

            while (line != null && counter < maxSize && sequenceLengthCounter < maxSize)
            {
                if (line.length() >= 1)
                {
                    if (line.charAt(0) == '>') // beginning of a sequence
                    {
                        if (currentSequence.length() != 0)
                        {
                            seqs.add(new DNA(ident, currentSequence.toString()));
                            counter += currentSequence.length();
                            currentSequence.setLength(0);
                        }
                        ident = line;
                    } else
                    // begin of a new line of current sequence
                    {
                        currentSequence.append(line);
                        sequenceLengthCounter = currentSequence.length();
                    }
                }
                line = reader.readLine();
                if (line != null) line = line.trim();
            }

            if (currentSequence.length() != 0) seqs.add(new DNA(ident, currentSequence.toString()));
        } catch (IOException e)
        {
            throw new IllegalStateException("Error occured when reading FASTA sequence file: " + reader + " error message returned: " + e.getMessage());
        }
        sequences = new DNA[seqs.size()];
        seqs.toArray(sequences);
    }
}
