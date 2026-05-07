package util;

import db.table.*;
import db.type.DoubleVector;
import db.type.Image;
import db.type.IndexObject;
import db.type.Spectra;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * 进行数据映射的工具类，该类将原始数据映射到支撑点空间之中。该类输入存有数据的table
 *
 * @author JiaYing Chen 2020.9.11
 */

public class Embed
{

    private double[][] coordinates; //映射后的坐标，二维数组

    private float[][] coordinates_float;
    private int pivotNum;   //记录支撑点数目

    private int dataNum;    //记录数据的个数 等于coodinates.length

    public Embed() {
    }

    /**
     * 返回指定数据映射后的坐标
     *
     * @param rowID 要返回的数据位于的行数
     * @return 指定数据映射后的向量坐标
     */
    public double[] getCoordinate(int rowID) {
        double[] cd = coordinates[rowID];
        return cd;
    }

    /**
     * 返回指定数据映射后的坐标的指定维
     *
     * @param rowID 要返回的数据所在的行数
     * @param colID 要返回该数据的向量坐标的第colID维
     * @return double型的坐标值
     */
    public double getCoordinate(int rowID, int colID) {
        return coordinates_float[rowID][colID];
    }

    /**
     * 获取支撑点的数目
     * @return int 支撑点的数目
     */
    public int getPivotNum() {
        return pivotNum;
    }

    /**
     * 获取数据集的总大小
     * @return int 数据集的大小
     */
    public int getDataNum() {
        return dataNum;
    }

    /**
     * 得到coordinates（double）的数据
     *
     * @return 存储坐标的二维数组
     */
    public double[][] getCoordinates() {
        return coordinates;
    }


    /**
     * 将映射后的文件写入到txt文档中
     * <p>
     * 格式为” /t“为向量内部的分隔符，最后一个向量后面没有分隔符，只有”/n“
     *
     * @param filePath
     * @throws IOException
     */
    public void writeToTxt(String filePath) throws IOException {
        File f = new File(filePath);
        FileOutputStream out = new FileOutputStream(f);
        OutputStreamWriter writer = new OutputStreamWriter(out);
        writer.write(pivotNum + " \t" + dataNum + "\n");
        for (int i = 0; i < dataNum; i++) {
            for (int j = 0; j < pivotNum; j++) {
                if (j == pivotNum - 1) {
                    writer.write(coordinates_float[i][j] + "\n");
                    break;
                }
                writer.write(coordinates_float[i][j] + " \t");
            }
        }
        writer.close(); //等待写完
    }

    /**
     * 将int数据从大端转化为小端模式，返回的是byte
     *
     * @param a 要转换的int型数据
     * @return byte[]
     */
    private static byte[] intToBytes_Little(int a) {
        return new byte[]{
                (byte) (a & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 24) & 0xFF)
        };
    }

    /**
     * 将double数据从大端转化为小端模式，返回的是byte
     *
     * @param d 要转换的double型数据
     * @return byte[]
     */
    private static byte[] doubleToBytes_Little(double d) {
        long l = Double.doubleToLongBits(d);
        byte b[] = new byte[8];
        b[7] = (byte) (0xff & (l >> 56));
        b[6] = (byte) (0xff & (l >> 48));
        b[5] = (byte) (0xff & (l >> 40));
        b[4] = (byte) (0xff & (l >> 32));
        b[3] = (byte) (0xff & (l >> 24));
        b[2] = (byte) (0xff & (l >> 16));
        b[1] = (byte) (0xff & (l >> 8));
        b[0] = (byte) (0xff & l);
        return b;
    }


    /**
     * 将float数据从大端转化为小端模式，返回的是byte
     *
     * @param d 要转换的float型数据
     * @return byte[]
     */
    private static byte[] floatToBytes_Little(float d) {
        int l = Float.floatToIntBits(d);
        byte b[] = new byte[4];
        b[3] = (byte) (0xff & (l >> 24));
        b[2] = (byte) (0xff & (l >> 16));
        b[1] = (byte) (0xff & (l >> 8));
        b[0] = (byte) (0xff & l);
        return b;
    }


    /**
     * 将coordinates（double）数据的数据以小端模式写入到文件中
     * <p>
     * 格式为（数据的维度d+double*d）*n
     *
     * @param filepath 文件路径
     */
    public void writeToBinary_Little_Double(String filepath) {
        try {
            //对大量数据的写入，使用缓冲流BufferedOutputStream类可以提高效率
            DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(filepath)));
            for (int i = 0; i < dataNum; i++) {
                out.write(intToBytes_Little(pivotNum));   //先写入数据的维度,int转化为二进制小端模式
                for (int j = 0; j < pivotNum; j++) {
                    out.write(doubleToBytes_Little(coordinates[i][j]));   //再写入数据，double转化成二进制小端模式
                }
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 将coordinates_float数据的数据以小端模式写入到文件中
     * <p>
     * 格式为（数据的维度d+float*d）*n
     *
     * @param filepath
     */
    public void writeToBinary_Little_Float(String filepath) {
        try {
            //对大量数据的写入，使用缓冲流BufferedOutputStream类可以提高效率
            DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(filepath)));
            for (int i = 0; i < dataNum; i++) {
                out.write(intToBytes_Little(pivotNum));   //先写入数据的维度,int转化为二进制小端模式
                for (int j = 0; j < pivotNum; j++) {

                    out.write(floatToBytes_Little(coordinates_float[i][j]));   //再写入数据，double转化成二进制小端模式
                }
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * toString方法，输出坐标值
     *
     * @return
     */
    @Override
    public String toString() {
        for (int i = 0; i < coordinates_float.length; i++)     //输出embed的信息作调试
        {
            for (int j = 0; j < pivotNum; j++) {
                System.out.print(coordinates_float[i][j] + " ");
            }
            System.out.println("");
        }
        for (int i = 0; i < coordinates.length; i++)     //输出embed的信息作调试
        {
            for (int j = 0; j < pivotNum; j++) {
                System.out.print(coordinates[i][j] + " ");
            }
            System.out.println("");
        }
        return "Embed{" +
                "coordinates=" + Arrays.toString(coordinates) + Arrays.toString(coordinates_float) +
                '}';
    }


    /**
     * 将数据映射到支撑点空间
     * <p>
     * 传输数据和支撑点，计算每个点在支撑点空间的坐标，存储在coordinates_float/coordinates里面
     *
     * @param dt     含有源度量空间数据的table
     * @param pivots 选择的支撑点数组
     * @throws IOException
     */
    public void embedToPivotSpace(Table dt, int[] pivots){
        if (dt instanceof DoubleVectorTable) {
            if (Debug.debug) System.out.println("is DoubleVectorTable getclass: " + dt.getData().get(1).getClass());
            // DoubleVectorTable dt=new DoubleVectorTable(table.getSourceFileName(), table.getIndexPrefix(), 500,2,metric);
            List<DoubleVector> data = (List<DoubleVector>) dt.getData();
            coordinates_float = new float[data.size()][pivots.length];
            pivotNum = pivots.length;//记录支撑点的数目和数据的大小
            dataNum = data.size();
            System.out.println("dataSize:" + dataNum);
            for (int i = 0; i < pivots.length; i++)   //按支撑点映射
            {
                DoubleVector pivot = data.get(pivots[i]);   //记录第i个支撑点的内容
                for (int j = 0; j < data.size(); j++) //遍历数据集
                {
                    // System.out.println("datasize:"+data.size()+"pivotnum"+pivots.length);
                    coordinates_float[j][i] = (float) dt.getMetric().getDistance(pivot, data.get(j));//计算第i个支撑点映射后的坐标
                }
            }
        } else if (dt instanceof ImageTable)  //把数据类型改成Image即可
        {
            System.out.println("getclass2: " + dt.getData().get(1).getClass());
            // DoubleVectorTable dt=new DoubleVectorTable(table.getSourceFileName(), table.getIndexPrefix(), 500,2,metric);
            List<Image> data = (List<Image>) dt.getData();
            coordinates_float = new float[data.size()][pivots.length];
            pivotNum = pivots.length;//记录支撑点的数目和数据的大小
            dataNum = data.size();
            //System.out.println("dataSize:"+dataNum);
            for (int i = 0; i < pivots.length; i++)   //按支撑点映射
            {
                Image pivot = data.get(pivots[i]);   //记录第i个支撑点的内容
                for (int j = 0; j < data.size(); j++) //遍历数据集
                {
                    // System.out.println(data.get(j));
                    coordinates_float[j][i] = (float) dt.getMetric().getDistance(pivot, data.get(j));//计算第i个支撑点映射后的坐标
                }
            }

        } else if (dt instanceof DNATable) {
            System.out.println("getclass2: " + dt.getData().get(1).getClass());
            // DoubleVectorTable dt=new DoubleVectorTable(table.getSourceFileName(), table.getIndexPrefix(), 500,2,metric);
            List<IndexObject> data = (List<IndexObject>) dt.getData();
            coordinates_float = new float[data.size()][pivots.length];
            pivotNum = pivots.length;//记录支撑点的数目和数据的大小
            dataNum = data.size();
            //System.out.println("dataSize:"+dataNum);
            for (int i = 0; i < pivots.length; i++)   //按支撑点映射
            {
                IndexObject pivot = data.get(pivots[i]);   //记录第i个支撑点的内容
                for (int j = 0; j < data.size(); j++) //遍历数据集
                {
                    // System.out.println(data.get(j));
                    coordinates_float[j][i] = (float) dt.getMetric().getDistance(pivot, data.get(j));//计算第i个支撑点映射后的坐标
                }
            }
        } else if (dt instanceof SpectraTable) {
            System.out.println("getclass2: " + dt.getData().get(1).getClass());
            // DoubleVectorTable dt=new DoubleVectorTable(table.getSourceFileName(), table.getIndexPrefix(), 500,2,metric);
            List<Spectra> data = (List<Spectra>) dt.getData();
            coordinates_float = new float[data.size()][pivots.length];
            pivotNum = pivots.length;//记录支撑点的数目和数据的大小
            dataNum = data.size();
            //System.out.println("dataSize:"+dataNum);
            for (int i = 0; i < pivots.length; i++)   //按支撑点映射
            {
                Spectra pivot = data.get(pivots[i]);   //记录第i个支撑点的内容
                for (int j = 0; j < data.size(); j++) //遍历数据集
                {
                    // System.out.println(data.get(j));
                    coordinates_float[j][i] = (float) dt.getMetric().getDistance(pivot, data.get(j));//计算第i个支撑点映射后的坐标
                }
            }
        } else {
            System.out.println("This is not a right input!!!");
            System.out.println("getclass: " + dt.getData().get(1).getClass());
        }
        int dataSize = coordinates_float.length;
        int dataDim = coordinates_float[0].length;
        coordinates = new double[dataSize][dataDim];
        for (int i=0; i<dataSize; i++){
            for (int j=0; j<dataDim; j++){
                coordinates[i][j] = coordinates_float[i][j];
            }
        }
    }


}
