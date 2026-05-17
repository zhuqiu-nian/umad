package db.table;

import app.Application;
import db.type.DoubleVector;
import metric.LMetric;
import metric.Metric;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class DimacsVectorTable extends Table
{
    private static final long serialVersionUID = -7121291423608712201L;

    private static final Metric DEFAULT_METRIC = LMetric.EuclideanDistanceMetric;

    private final int skipVectors;
    private int dim;

    public DimacsVectorTable(String fileName, String indexPrefix, int size,
                             int skipVectors) throws IOException
    {
        this(fileName, indexPrefix, size, skipVectors, DEFAULT_METRIC);
    }

    public DimacsVectorTable(String fileName, String indexPrefix, int size,
                             int skipVectors, Metric metric) throws IOException
    {
        super(fileName, indexPrefix, size, metric);
        if (skipVectors < 0)
        {
            throw new IllegalArgumentException("skipVectors must be non-negative");
        }
        this.skipVectors = skipVectors;
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        loadData(reader, size);
        if (data.size() < this.dataSize)
        {
            this.dataSize = data.size();
        }
    }

    public int getDim()
    {
        return dim;
    }

    @Override
    public void loadData(BufferedReader reader, int size)
    {
        try
        {
            String line = reader.readLine();
            if (line == null)
            {
                throw new IllegalStateException("empty DIMACS vector file");
            }
            String[] header = line.trim().split("[, \\t]+");
            if (header.length < 3)
            {
                throw new IllegalStateException("cannot parse DIMACS vector header: " + line);
            }
            int baseCount = Integer.parseInt(header[0]);
            int queryCount = Integer.parseInt(header[1]);
            dim = Integer.parseInt(header[2]);
            int available = baseCount + queryCount;
            int actualSkip = Math.min(skipVectors, available);
            for (int i = 0; i < actualSkip; i++)
            {
                if (reader.readLine() == null)
                {
                    break;
                }
            }

            ArrayList<DoubleVector> vectors = new ArrayList<DoubleVector>(size);
            ArrayList<Integer> originalRowIDsArrayList = new ArrayList<Integer>(size);
            int numData = 0;
            line = reader.readLine();
            while (line != null && numData < size)
            {
                line = line.trim();
                if (!line.isEmpty())
                {
                    String[] row = line.split("[, \\t]+");
                    if (row.length < dim)
                    {
                        throw new IllegalStateException("vector row has fewer dimensions than expected: " + line);
                    }
                    double[] values = new double[dim];
                    for (int i = 0; i < dim; i++)
                    {
                        values[i] = Double.parseDouble(row[i]);
                    }
                    int originalRowID = actualSkip + numData;
                    originalRowIDsArrayList.add(originalRowID);
                    vectors.add(new DoubleVector(this, numData, values));
                    numData++;
                }
                line = reader.readLine();
            }
            vectors.trimToSize();
            data = vectors;
            Application.globalData = vectors;
            originalRowIDs = new int[originalRowIDsArrayList.size()];
            for (int i = 0; i < originalRowIDs.length; i++)
            {
                originalRowIDs[i] = originalRowIDsArrayList.get(i);
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Error reading DIMACS vector file: " + e.getMessage(), e);
        }
    }
}
