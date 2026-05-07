package db.table;

import app.Application;
import db.type.Image;
import metric.ImageMetric;
import metric.Metric;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片数据库，它包括构建的索引和构建索引用到的距离函数，该类是针对UMAD图片数据集设计的类，可能不适用其他图片对象。
 */
public class ImageTable extends Table
{
    private static final long serialVersionUID = 1574357904350312666L;

    private static final Metric DEFAULT_METRIC = new ImageMetric();

    public ImageTable(String dirName, String indexPrefix, int maxSize) throws FileNotFoundException
    {
        this(dirName, indexPrefix, maxSize, DEFAULT_METRIC);
    }


    private ImageTable(String dirName, String indexPrefix, int size, Metric metric) throws FileNotFoundException
    {
        super(dirName, indexPrefix, size, metric);

        BufferedReader reader, readerMaxInfo = null;
        reader = new BufferedReader(new FileReader(dirName + "/allfeas.dat"));
        try
        {
            readerMaxInfo = new BufferedReader(new FileReader(dirName + "/maxinfo.dat"));
        } catch (FileNotFoundException e1)
        {
            System.err.println("maxinfo.dat not found. loading data without Max information");
            loadData(reader, size);
            //如果实际读入的数据量小于期待读入的数据量，则将数据集大小设置为实际读入的数据量
            if (data.size() < this.dataSize) this.dataSize = data.size();
        }

        loadData(reader, readerMaxInfo, size);
        //如果实际读入的数据量小于期待读入的数据量，则将数据集大小设置为实际读入的数据量
        if (data.size() < this.dataSize) this.dataSize = data.size();
    }

    public void loadData(BufferedReader reader, int size)
    {
        String           line;
        int              count  = 0;
        ArrayList<Image> images = new ArrayList<Image>(imageNum);
        try
        {
            // read values from the text file.
            line = reader.readLine();
            if (line != null) line = line.trim();
            while (line != null && count <= size)
            {
                float[]  aList        = new float[totalFeaLen];
                String[] lineSegments = line.split(" ");
                for (int i = 1; i <= totalFeaLen; i++)
                {
                    aList[i - 1] = Float.parseFloat(lineSegments[i]);
                }
                images.add(new Image(this, count, aList));
                originalRowIDs[count] = count;
                count++;
                line = reader.readLine();
                if (line != null) line = line.trim();
            }
        } catch (java.io.IOException e)
        {
            e.printStackTrace();
            throw new IllegalStateException("Error occured when reading Image features file: " + reader);
        }
        images.trimToSize();
        data = images;
    }

    protected void loadData(BufferedReader reader, BufferedReader readerMaxInfo, int size)
    {
        String             line;
        String             lineMaxInfo;
        int                count                   = 0;
        ArrayList<Image>   images                  = new ArrayList<Image>(imageNum);
        ArrayList<Integer> originalRowIDsArrayList = new ArrayList<Integer>();

        try
        {
            // read values from the text file.
            line = reader.readLine();
            if (line != null) line = line.trim();
            lineMaxInfo = readerMaxInfo.readLine();
            if (lineMaxInfo != null) lineMaxInfo = lineMaxInfo.trim();
            while (line != null && lineMaxInfo != null && count <= size)
            {
                float[]  aList               = new float[totalFeaLen];
                double[] maxDist             = new double[feaLength.length];
                String[] lineSegments        = line.split(" ");
                String[] lineMaxInfoSegments = lineMaxInfo.split(" ");
                for (int i = 1; i <= totalFeaLen; i++)
                {
                    aList[i - 1] = Float.parseFloat(lineSegments[i]);
                }
                for (int i = 0; i < feaLength.length; i++)
                {
                    maxDist[i] = Double.parseDouble(lineMaxInfoSegments[i]);
                }
                images.add(new Image(this, count, aList, maxDist));
                originalRowIDsArrayList.add(count);
                count++;
                line = reader.readLine();
                if (line != null) line = line.trim();
                lineMaxInfo = readerMaxInfo.readLine();
                if (lineMaxInfo != null) lineMaxInfo = lineMaxInfo.trim();
            }
        } catch (java.io.IOException e)
        {
            e.printStackTrace();
            throw new IllegalStateException("Error occured when reading Image features file: " + reader);
        }
        images.trimToSize();
        data = images;
        Application.globalData = images;
        originalRowIDs = new int[originalRowIDsArrayList.size()];
        for (int i = 0, e = originalRowIDsArrayList.size(); i < e; i++)
        {
            originalRowIDs[i] = originalRowIDsArrayList.get(i);
        }

    }

    //    /**
    //     * A utility method to create max distance information for each image.
    //     * Output has a row for each image, which corresponds to the maximum
    //     * distance of this image to the other images.
    //     */

    /**
     * 一个为每张图片创建最大距离信息的工具方法。每张图片输出一行(控制台输出），对应该图到其他图片的最大距离信息。
     *
     * @param r 图像对象列表
     */
    public void createMaxInfo(List r)
    {
        ImageMetric metric = new ImageMetric();
        int         sz     = r.size();
        double      maxDist;
        double      tempVal;
        for (int k1 = 0; k1 < sz; k1++)
        {
            Image ob1 = (Image) r.get(k1);
            for (int FeaIndex = 0; FeaIndex < feaLength.length; FeaIndex++)
            {
                maxDist = 0.0;
                for (int k2 = 0; k2 < sz; k2++)
                {
                    Image ob2 = (Image) r.get(k2);
                    tempVal = metric.getDistance_Fea(ob1, ob2, FeaIndex);
                    if (tempVal > maxDist)
                    {
                        maxDist = tempVal;
                    }
                }
                System.out.print(maxDist);
                System.out.print(" ");
            }
            System.out.println();
        }
    }

    final public int totalFeaLen = 66;

    final public int[] feaLength = {3, 48, 15};

    final public int imageNum = 10221;
}
