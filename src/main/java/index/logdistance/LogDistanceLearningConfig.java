package index.logdistance;

import java.io.Serializable;
import java.util.Arrays;

public class LogDistanceLearningConfig implements Serializable
{
    private static final long serialVersionUID = -6628963519007187421L;

    private final int angleCount;
    private final double[] tauQuantiles;
    private final double minBalance;
    private final int trainingQuerySampleSize;
    private final int medoidCandidateCount;
    private final int medoidIterations;
    private final double epsilonDistance;
    private final double comparisonEpsilon;
    private final double validationFraction;
    private final int topCandidates;
    private final double boxPenaltyWeight;
    private final double childHitPenaltyWeight;

    public LogDistanceLearningConfig()
    {
        this(16,
                new double[]{0.35, 0.40, 0.45, 0.50, 0.55, 0.60, 0.65},
                0.25,
                512,
                8,
                2,
                LogDistanceTransform.DEFAULT_EPSILON_DISTANCE,
                LogDistanceTransform.DEFAULT_COMPARISON_EPSILON,
                0.30,
                16,
                1.0e-4,
                1.0e-4);
    }

    public LogDistanceLearningConfig(int angleCount,
                                     double[] tauQuantiles,
                                     double minBalance,
                                     int trainingQuerySampleSize,
                                     int medoidCandidateCount,
                                     int medoidIterations,
                                     double epsilonDistance,
                                     double comparisonEpsilon,
                                     double validationFraction,
                                     int topCandidates,
                                     double boxPenaltyWeight,
                                     double childHitPenaltyWeight)
    {
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
        if (validationFraction < 0.0 || validationFraction >= 1.0)
        {
            throw new IllegalArgumentException("validationFraction must be in [0,1)");
        }
        if (topCandidates <= 0)
        {
            throw new IllegalArgumentException("topCandidates must be positive");
        }
        if (boxPenaltyWeight < 0.0 || childHitPenaltyWeight < 0.0)
        {
            throw new IllegalArgumentException("penalty weights must be non-negative");
        }
        this.angleCount = angleCount;
        this.tauQuantiles = tauQuantiles.clone();
        this.minBalance = minBalance;
        this.trainingQuerySampleSize = trainingQuerySampleSize;
        this.medoidCandidateCount = medoidCandidateCount;
        this.medoidIterations = medoidIterations;
        this.epsilonDistance = epsilonDistance;
        this.comparisonEpsilon = comparisonEpsilon;
        this.validationFraction = validationFraction;
        this.topCandidates = topCandidates;
        this.boxPenaltyWeight = boxPenaltyWeight;
        this.childHitPenaltyWeight = childHitPenaltyWeight;
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

    public double getValidationFraction()
    {
        return validationFraction;
    }

    public int getTopCandidates()
    {
        return topCandidates;
    }

    public double getBoxPenaltyWeight()
    {
        return boxPenaltyWeight;
    }

    public double getChildHitPenaltyWeight()
    {
        return childHitPenaltyWeight;
    }

    @Override
    public String toString()
    {
        return "LogDistanceLearningConfig{" +
                "angleCount=" + angleCount +
                ", tauQuantiles=" + Arrays.toString(tauQuantiles) +
                ", minBalance=" + minBalance +
                ", trainingQuerySampleSize=" + trainingQuerySampleSize +
                ", medoidCandidateCount=" + medoidCandidateCount +
                ", medoidIterations=" + medoidIterations +
                ", epsilonDistance=" + epsilonDistance +
                ", comparisonEpsilon=" + comparisonEpsilon +
                ", validationFraction=" + validationFraction +
                ", topCandidates=" + topCandidates +
                ", boxPenaltyWeight=" + boxPenaltyWeight +
                ", childHitPenaltyWeight=" + childHitPenaltyWeight +
                '}';
    }
}
