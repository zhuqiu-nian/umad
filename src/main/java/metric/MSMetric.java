/* Smriti Ramakrishnan, 2004.03.21
 * This computed modified cosine distance (elements of the vectors are equal - within a particular tolerance)
 * vectors are CCS form of binary vectors - stored in MSDataKeyObjects
 * this ideally should extend BinaryVectorMetric, but the getCosine implementation seems faster here.
 * If this is wrong, just extend from it and calls remain the same, but need to add
 * 1) DataVector.
 * 2) Change InnerProduct to use 'Modified' contains(..)
 * 3) Add min, max,step to constructor
 * 4) Does a metric have to have a equals method ?
 *
 * Requires to implement java.io.Serializable - since index.Index needs to be serializable
 */

package metric;

import db.type.IndexObject;
import db.type.Spectra;

import static metric.MSMSConstants.MS_TOLERANCE;

/**
 * {@code MSDataMetric}是{@link Spectra}的距离函数，并实现了{@link Metric}接口。
 **/

public class MSMetric implements Metric
{
    static final long serialVersionUID = 179878801379011989L;

    /**
     * 构造函数。
     *
     * @param min  min
     * @param max  max
     * @param step step
     * @param tol  tol
     */
    public MSMetric(int min, int max, double step, double tol)
    {
        //super(min, max, step);
        this.min  = min;
        this.max  = max;
        this.step = step;
        this.tol  = tol;
    }

    /**
     * 默认的构造函数。所有的参数(min max step tol)都是临时的。
     */
    public MSMetric()
    {
        this.min  = 0;
        this.max  = 0;
        this.step = 0;
        this.tol  = MS_TOLERANCE;
    }

    /**
     * 计算两个{@link IndexObject}之间的距离。
     *
     * @param v1 v1
     * @param v2 v2
     * @return 返回 {@link IndexObject}之间的距离。
     */
    public double getDistance(IndexObject v1, IndexObject v2)
    {
        return getDistance((Spectra) v1, (Spectra) v2);
    }

    /**
     * 计算两个 {@link Spectra}之间的距离
     *
     * @param v1 v1
     * @param v2 v2
     * @return 返回 {@link Spectra}之间的距离
     */
    public double getDistance(Spectra v1, Spectra v2)
    {
        // now compute cosine distance
        double cos = getCosine(v1, v2);


        if (Math.abs(Math.abs(cos) - 1) < COS_THRESHOLD)
        { // precision
            if (cos > 0) cos = 1; // very similar
            else cos = -1; // very unsimilar
        } else if (Math.abs(cos) > 1)
        {
            System.out.println("COS_THRESHOLD = " + COS_THRESHOLD + ", cos = " + cos);
            System.out.println("got cosine > 1, cosine=" + cos + ", :" + v1.toString() + ", v2:" + v2.toString() + "Quitting.");
            System.exit(0);
        }


        return Math.acos(cos);


    }

    private double getCosine(Spectra one, Spectra two)
    {
        double[] v1 = one.getData();
        double[] v2 = two.getData();

        return getInnerProduct(v1, v2) / (getMagnitude(v1) * getMagnitude(v2));

    }

    private int getInnerProduct(double[] v1, double[] v2)
    {
        int    dist = 0;
        int    i    = 0, j = 0;
        double val1;
        double val2;

        //System.out.println("TOL = " + tol);
        while (i < v1.length && j < v2.length)
        {
            val1 = v1[i];
            val2 = v2[j];
            //new version -- gets rid of Math.abs()
            if (val1 <= val2)
            {
                if (val2 - val1 <= tol)
                {
                    dist++;
                    i++;
                    j++;
                } else
                {
                    i++;
                }
            } else
            {
                if (val1 - val2 <= tol)
                {
                    dist++;
                    i++;
                    j++;
                } else
                {
                    j++;
                }
            }
        }

        return dist;
    }

    private double getMagnitude(double[] d)
    {
        return Math.sqrt(d.length);
    }

    int min, max; // min, max are mass ranges
    double step; // Same as Tolerance ?
    //end-legacy

    double tol;
    private final double COS_THRESHOLD = 0.00005;
}
