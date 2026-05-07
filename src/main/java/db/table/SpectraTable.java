package db.table;

import db.type.Spectra;
import metric.Metric;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;


public class SpectraTable extends Table
{

    private static final long serialVersionUID = -6528032331962266589L;

    /**
     * 构造函数
     *
     * @param fileName    要读入的数据文件的名字
     * @param indexPrefix 索引前缀名
     * @param maxSize     数据集大小
     * @param metric      距离函数
     * @throws IOException IO异常
     */
    public SpectraTable(String fileName, String indexPrefix, int maxSize, Metric metric) throws IOException
    {
        super(fileName, indexPrefix, maxSize, metric);

        BufferedReader reader = new BufferedReader(new java.io.FileReader(fileName));
        loadData(reader, maxSize);
        //如果实际读入的数据量小于期待读入的数据量，则将数据集大小设置为实际读入的数据量
        if (data.size() < this.dataSize) this.dataSize = data.size();
    }

    /**
     * 加载部分数据入内存
     *
     * @param reader  {@link BufferedReader}缓冲流
     * @param maxSize 要加载进的大小
     */
    public void loadData(BufferedReader reader, int maxSize)
    {
        String             line;
        ArrayList<Spectra> spectra = null;
        System.out.println("Loading... ");
        try
        {
            // read sequences from file
            line = reader.readLine(); // read the first line
            if (line != null) line = line.trim();
            // get total rows from first line and allocate the ArrayList
            int numSpectra = Integer.parseInt(line);
            spectra        = new ArrayList<Spectra>(numSpectra);
            originalRowIDs = new int[numSpectra];

            // read line byte line
            line = reader.readLine();
            if (line != null) line = line.trim();

            int count = 0;
            while (line != null && count < maxSize)
            {
                if (count % 1000 == 0)
                {
                    System.out.print(count + ".. ");
                }
                String[] row = line.split(" ", 2);
                originalRowIDs[count] = new Integer(row[0]).intValue();
                spectra.add(new Spectra(this, count, row[1]));

                line = reader.readLine();
                if (line != null) line = line.trim();
                count++;
            }
        } catch (IOException e)
        {
            e.printStackTrace();
            throw new IllegalStateException("Error occured when reading ms file: " + reader + " error message returned: " + e.getMessage());
        } catch (NumberFormatException e)
        {
            e.printStackTrace();
            /* Ignore strings with invalid characters. */
        }
        spectra.trimToSize();
        data = spectra;
    }

}
