package metric;

import db.type.DoubleVector;
import db.type.IndexObject;
import db.type.SpectraWithPrecursorMass;

import static metric.MSMSConstants.MS_PRECURSOR_TOLERANCE;
import static metric.MSMSConstants.MS_TOLERANCE;

/**
 * {@code MSMSMetric} 是对于 {@code comparing tandem spectra signatures} 的{@code fuzzy cosine distance}距离的实现。
 * <p>
 * MSMSMetric is an implementation of a fuzzy cosine distance metric for comparing tandem spectra
 * signatures. Elements of the vectors are equal, within a given tolerance.
 *
 * @author Smriti Ramakrishnan, Willard
 * @version 2004.11.29
 */
public class MSMSMetric implements Metric
{

    public MSMSMetric(int min, int max, double step, double tol)
    {
        // super(min, max, step);
        this.min  = min;
        this.max  = max;
        this.step = step;
        this.tol  = tol;

        mscosdist    = 0.0;
        absMassDiff  = 0.0;
        massDiffTerm = 0.0;
    }

    /**
     * 默认的构造函数，所有的参数都是以默认值初始化。
     * <pre>
     *      min = 0;
     *      max = 0;
     *      step = 0;
     *      tol = MSMSConstants.MS_TOLERANCE
     * </pre>
     */
    public MSMSMetric()
    {
        this.min  = 0;
        this.max  = 0;
        this.step = 0;
        this.tol  = MS_TOLERANCE;

        mscosdist    = 0.0;
        absMassDiff  = 0.0;
        massDiffTerm = 0.0;
    }

    /**
     * 计算两个{@link IndexObject}之间的距离。
     *
     * @param v1 v1
     * @param v2 v2
     * @return 两个 {@link IndexObject}之间的距离。
     */
    public double getDistance(IndexObject v1, IndexObject v2)
    {
        return getDistance((SpectraWithPrecursorMass) v1, (SpectraWithPrecursorMass) v2);
    }

    /**
     * 计算两个{@link SpectraWithPrecursorMass}之间的距离。
     *
     * @param v1 v1
     * @param v2 v2
     * @return 两个 {@link SpectraWithPrecursorMass}之间的距离。
     */
    public double getDistance(SpectraWithPrecursorMass v1, SpectraWithPrecursorMass v2)
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

        mscosdist    = Math.acos(cos);
        massDiffTerm = getAbsPrecursorMassDiff(v1, v2);
        double dist = massDiffTerm + mscosdist;

        return dist;

    }

    private double getAbsPrecursorMassDiff(SpectraWithPrecursorMass v1, SpectraWithPrecursorMass v2)
    {
        double m1 = v1.getPrecursorMass();
        double m2 = v2.getPrecursorMass();

        absMassDiff = Math.abs(m1 - m2);

        if (absMassDiff < COS_THRESHOLD) absMassDiff = 0.0;

        if (absMassDiff <= MS_PRECURSOR_TOLERANCE) return 0.0;
        else return (absMassDiff);
    }


    private double getCosine(DoubleVector one, DoubleVector two)
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

        // System.out.println("TOL = " + tol);
        while (i < v1.length && j < v2.length)
        {
            val1 = v1[i];
            val2 = v2[j];
            // new version -- gets rid of Math.abs()
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
        // return d.length;
    }

    /**
     * 计算{@link SpectraWithPrecursorMass}之间的距离，并以字符串形式返回。
     *
     * @param k1 k1
     * @param k2 k2
     * @return 返回的字符串，包含维度等信息。
     */
    public String printDistance(SpectraWithPrecursorMass k1, SpectraWithPrecursorMass k2)
    {
        // set values of mscosdist and massDiff
        getDistance(k1, k2);
        java.text.DecimalFormat frm = new java.text.DecimalFormat("####.########");

        StringBuffer outStr = new StringBuffer(20);
        outStr.append("MSCOSDIST = " + frm.format(mscosdist) + ", MASS_DIFF_TERM = " + frm.format(massDiffTerm) + " (abs mass diff = " + frm.format(absMassDiff) + ")\n");

        return outStr.toString();
    }

    int min, max;                               // min, max are mass ranges
    double step;                                   // Same as Tolerance ?

    double mscosdist;
    double absMassDiff;
    double massDiffTerm;

    double tol;
    private final double COS_THRESHOLD    = 0.00005;
    static final  long   serialVersionUID = 8368326281379099335L;
}
