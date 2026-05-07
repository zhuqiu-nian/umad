package db.type;

/**
 * 一个Pair，用来包裹向量和他所属的类别。
 *
 * @author liulinfeng
 * @version 2021/2/28
 */
public class DoubleVectorClusterPair
{
    private DoubleVector doubleVector;
    private int cluster;

    public DoubleVectorClusterPair(DoubleVector doubleVector, int cluster) {
        this.doubleVector = doubleVector;
        this.cluster = cluster;
    }

    public DoubleVector getDoubleVector() {
        return doubleVector;
    }

    public void setDoubleVector(DoubleVector doubleVector) {
        this.doubleVector = doubleVector;
    }

    public int getCluster() {
        return cluster;
    }

    public void setCluster(int cluster) {
        this.cluster = cluster;
    }

    @Override
    public String toString() {
        return "DoubleVectorClusterPair{" +
                "doubleVector=" + doubleVector +
                ", cluster=" + cluster +
                '}';
    }
}
