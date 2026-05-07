package db.table;

import db.type.Peptide;
import metric.SequenceFragmentMetric;
import metric.WHDGlobalSequenceFragmentMetric;
import metric.WeightMatrix;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PeptideTable extends SequenceTable
{
    /**
     *
     */
    private static final long serialVersionUID = 5685357333502524772L;

    /**
     *
     */
    public static final WeightMatrix DEFAULT_WEIGHT_MATRIX = Peptide.mPAM250aExtendedWeightMatrix;

    /**
     *
     */
    public static final SequenceFragmentMetric DEFAULT_METRIC = new WHDGlobalSequenceFragmentMetric(DEFAULT_WEIGHT_MATRIX);

    /**
     * 构造函数
     *
     * @param fileName       构建索引的文件名
     * @param indexPrefix    索引前缀名
     * @param maxDataSize    要读入的数据集的大小
     * @param fragmentLength 片段长度
     * @throws IOException IO异常
     */
    public PeptideTable(String fileName, String indexPrefix, int maxDataSize, int fragmentLength) throws IOException
    {
        this(fileName, indexPrefix, maxDataSize, DEFAULT_METRIC, fragmentLength);
    }

    /**
     * 构造函数
     *
     * @param fileName       构建索引的文件名
     * @param indexPrefix    索引前缀名
     * @param maxDataSize    要读入的数据集的大小
     * @param metric         距离函数
     * @param fragmentLength 片段长度
     * @throws IOException IO异常
     */
    public PeptideTable(String fileName, String indexPrefix, int maxDataSize, SequenceFragmentMetric metric, int fragmentLength) throws IOException
    {
        super(fileName, indexPrefix, maxDataSize, metric, fragmentLength);
    }

    /**
     * 加载部分数据入内存
     *
     * @param reader  {@link BufferedReader}缓冲流
     * @param maxSize 要加载进的大小
     */
    @Override
    public void loadData(BufferedReader reader, int maxSize)
    {
        String        ident                 = "";
        List<Peptide> seqs                  = new ArrayList<Peptide>();
        int           counter               = 0;
        int           sequenceLengthCounter = 0;
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
                            seqs.add(new Peptide(ident, currentSequence.toString()));
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

            if (currentSequence.length() != 0) seqs.add(new Peptide(ident, currentSequence.toString()));
        } catch (IOException e)
        {
            throw new IllegalStateException("Error occured when reading FASTA sequence file: " + reader + " error message returned: " + e.getMessage());
        }
        sequences = new Peptide[seqs.size()];
        seqs.toArray(sequences);
    }
}
