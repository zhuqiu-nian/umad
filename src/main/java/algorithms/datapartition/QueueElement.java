package algorithms.datapartition;

import java.util.List;
import java.util.Vector;

/**
 * 存储中间变量
 */
public class QueueElement
{
    private List<Vector<Double>> vectorList;

    private double exc;

    private double extent;

    /**
     * 存储中间数据
     * @param vectorList 划分法向量组
     * @param exc 划分排除率
     * @param extent 法向量正交程度
     */
    public QueueElement(List<Vector<Double>> vectorList, double exc, double extent) {
        this.vectorList = vectorList;
        this.exc = exc;
        this.extent = extent;
    }



    public QueueElement(List<Vector<Double>> vector, double exc) {
        this.vectorList = vector;
        this.exc = exc;
    }

    public List<Vector<Double>> getVectorList() {
        return vectorList;
    }

    public double getExtent() {
        return extent;
    }

    public void setExtent(double extent) {
        this.extent = extent;
    }

    public void setVectorList(List<Vector<Double>> vectorList) {

        this.vectorList = vectorList;
    }

    public double getExc() {

        return exc;
    }

    public void setExc(double exc) {

        this.exc = exc;
    }
}
