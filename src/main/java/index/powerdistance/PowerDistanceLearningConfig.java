package index.powerdistance;

import java.io.Serializable;
import java.util.Arrays;

public class PowerDistanceLearningConfig implements Serializable
{
    private static final long serialVersionUID = 8748261323398462341L;

    private final double[] rhoGrid;
    private final int angleCount;
    private final double[] tauQuantiles;
    private final double minBalance;
    private final int trainingQuerySampleSize;
    private final int medoidCandidateCount;
    private final int medoidIterations;
    private final double epsilonDistance;
    private final double comparisonEpsilon;

    public PowerDistanceLearningConfig()
    {
        this(new double[]{-4.0, -2.0, -1.0, -0.5, 0.5, 1.0, 2.0, 4.0},
                16,
                new double[]{0.35, 0.40, 0.45, 0.50, 0.55, 0.60, 0.65},
                0.25,
                512,
                8,
                2,
                PowerDistanceTransform.DEFAULT_EPSILON_DISTANCE,
                PowerDistanceTransform.DEFAULT_COMPARISON_EPSILON);
    }

    public PowerDistanceLearningConfig(double[] rhoGrid, int angleCount,
                                       double[] tauQuantiles, double minBalance,
                                       int trainingQuerySampleSize,
                                       int medoidCandidateCount,
                                       int medoidIterations,
                                       double epsilonDistance,
                                       double comparisonEpsilon)
    {
        if (rhoGrid == null || rhoGrid.length == 0)
        {
            throw new IllegalArgumentException("rhoGrid must not be empty");
        }
        for (double rho : rhoGrid)
        {
            if (rho == 0.0)
            {
                throw new IllegalArgumentException("rhoGrid must not contain zero");
            }
        }
        if (angleCount <= 0)
        {
            throw new IllegalArgumentException("angleCount must be positive");
        }
        if (tauQuantiles == null || tauQuantiles.length == 0)
        {
            throw new IllegalArgumentException("tauQuantiles must not be empty");
        }
        for (double q : tauQuantiles)
        {
            if (q < 0.0 || q > 1.0)
            {
                throw new IllegalArgumentException("tau quantiles must be in [0,1]");
            }
        }
        if (minBalance < 0.0 || minBalance > 0.5)
        {
            throw new IllegalArgumentException("minBalance must be in [0,0.5]");
        }
        if (trainingQuerySampleSize < 0)
        {
            throw new IllegalArgumentException("trainingQuerySampleSize must be non-negative");
        }
        if (medoidCandidateCount < 2)
        {
            throw new IllegalArgumentException("medoidCandidateCount must be at least 2");
        }
        if (medoidIterations < 0)
        {
            throw new IllegalArgumentException("medoidIterations must be non-negative");
        }
        if (!(epsilonDistance > 0.0))
        {
            throw new IllegalArgumentException("epsilonDistance must be positive");
        }
        this.rhoGrid = rhoGrid.clone();
        this.angleCount = angleCount;
        this.tauQuantiles = tauQuantiles.clone();
        this.minBalance = minBalance;
        this.trainingQuerySampleSize = trainingQuerySampleSize;
        this.medoidCandidateCount = medoidCandidateCount;
        this.medoidIterations = medoidIterations;
        this.epsilonDistance = epsilonDistance;
        this.comparisonEpsilon = comparisonEpsilon;
    }

    public double[] getRhoGrid()
    {
        return rhoGrid.clone();
    }

    public int getAngleCount()
    {
        return angleCount;
    }

    public double[] getTauQuantiles()
    {
        return tauQuantiles.clone();
    }

    public double getMinBalance()
    {
        return minBalance;
    }

    public int getTrainingQuerySampleSize()
    {
        return trainingQuerySampleSize;
    }

    public int getMedoidCandidateCount()
    {
        return medoidCandidateCount;
    }

    public int getMedoidIterations()
    {
        return medoidIterations;
    }

    public double getEpsilonDistance()
    {
        return epsilonDistance;
    }

    public double getComparisonEpsilon()
    {
        return comparisonEpsilon;
    }

    @Override
    public String toString()
    {
        return "PowerDistanceLearningConfig{" +
                "rhoGrid=" + Arrays.toString(rhoGrid) +
                ", angleCount=" + angleCount +
                ", tauQuantiles=" + Arrays.toString(tauQuantiles) +
                ", minBalance=" + minBalance +
                ", trainingQuerySampleSize=" + trainingQuerySampleSize +
                ", medoidCandidateCount=" + medoidCandidateCount +
                ", medoidIterations=" + medoidIterations +
                ", epsilonDistance=" + epsilonDistance +
                ", comparisonEpsilon=" + comparisonEpsilon +
                '}';
    }
}
