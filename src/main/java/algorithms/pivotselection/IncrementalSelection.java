package algorithms.pivotselection;


import db.type.IndexObject;
import metric.Metric;

import java.util.List;
import java.util.Random;

/**
 * 支撑点选择算法中增量选择算法
 */
public class IncrementalSelection implements PivotSelectionMethod, java.io.Serializable
{
    private static final long serialVersionUID = 6928847050089693231L;
    private final        int  constantA;
    private final        int  constantN;

    public IncrementalSelection(int a, int n)
    {
        this.constantA = a;
        this.constantN = n;
    }

    /**
     * @param metric    距离函数
     * @param data      数据大小
     * @param numPivots 支撑点数目
     * @return 支撑点ID
     */
    public int[] selectPivots(Metric metric, List<? extends IndexObject> data, final int numPivots)
    {
        return selectPivots(metric, data, 0, data.size(), numPivots);
    }

    /**
     * @param metric    距离函数
     * @param data      数据
     * @param first     第一个支撑点ID
     * @param dataSize  数据大小
     * @param numPivots 支撑点数目
     * @return 支撑点ID
     */
    public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int first, int dataSize, final int numPivots)
    {
        final int NP     = (numPivots > dataSize) ? dataSize : numPivots;
        int[]     pivots = new int[NP];

        if (NP == dataSize)
        {
            for (int i = first; i < dataSize + first; i++)
                 pivots[i - first] = i;

            return removeDuplicate(metric, data, pivots);
        }

        int       m = Math.max(dataSize * dataSize / 100, dataSize);
        final int A = (constantA > m) ? m : constantA;
        final int N = (constantN > dataSize) ? 2 : constantN;
        //System.out.println("data size= " + dataSize + ", constantA = " + constantA + ", constantN = " + constantN + ", A = " + A + ", N= " + N);
        Random r = new Random();

        //generate the A set
        int[][] setA = new int[2][A];
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < A; j++)
                 setA[i][j] = r.nextInt(dataSize) + first;

        double[] D = new double[A];
        for (int i = 0; i < A; i++)
             D[i] = Double.NEGATIVE_INFINITY;

        double[][] ND = new double[N][A];  //store the distances for the candidates in setN to samples in set A

        int[]  setN       = new int[N];
        double sum        = 0;
        double largestSum = -1;

        for (int k = 0; k < NP; k++)
        {
            //generate set N
            //支撑点候选集 目前是随机选择
            for (int i = 0; i < N; i++)
                 setN[i] = r.nextInt(dataSize) + first;

            //compute ND
            //对每个支撑点候选
            for (int i = 0; i < N; i++)
            {
                sum = 0;
                //A评价集大小 计算点对距离差？取最大的距离差作为支撑点
                for (int j = 0; j < A; j++)
                {
                    ND[i][j] = Math.max(D[j], Math.abs(metric.getDistance(data.get(setA[0][j]), data.get(setN[i])) - metric.getDistance(data.get(setA[1][j]), data.get(setN[i]))));
                    sum += ND[i][j];
                }

                if (sum > largestSum)
                {
                    largestSum = sum;
                    pivots[k]  = i;  //stores the largest row of ND, but not the offset of the pivot yet
                }
            }

            System.arraycopy(ND[pivots[k]], 0, D, 0, A);
            pivots[k] = setN[pivots[k]]; //now really stores the offset of the pivot
        }

        return removeDuplicate(metric, data, pivots);


    }

    /**
     * check the array of pivots, remove the duplicate.
     *
     * @param metric 距离函数
     * @param data   数据
     * @param pivots 支撑点
     * @return 分区结果对象 {@link index.structure.PartitionResults}
     */
    public static int[] removeDuplicate(Metric metric, List<? extends IndexObject> data, int[] pivots)
    {
        final int size        = pivots.length;
        boolean[] isDuplicate = new boolean[size];
        for (int i = 0; i < size; i++)
             isDuplicate[i] = false;
        //重复位置 isDuplicate数组置为true
        for (int i = 0; i < size; i++)
        {
            if (isDuplicate[i]) continue;
            for (int j = i; j < size; j++)
            {
                if (isDuplicate[j]) continue;
                if (metric.getDistance(data.get(i), data.get(j)) == 0) isDuplicate[j] = true;
            }
        }

        int counter = 0;
        for (int i = 0; i < size; i++)
            if (isDuplicate[i]) counter++;

        if (counter == size)
        {
            return pivots;
        }
        else
        {
            System.out.println("temp");
            int[] temp = new int[counter];
            counter = 0;
            for (int i = 0; i < size; i++)
            {
                if (isDuplicate[i])
                {
                    System.out.println(pivots[i]);
                    temp[counter++] = pivots[i];
                }

            }
            System.out.println(temp.length);
            return temp;
        }
    }


}

