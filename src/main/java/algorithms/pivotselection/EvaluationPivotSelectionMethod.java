package algorithms.pivotselection;

import db.type.IndexObject;
import metric.Metric;

import java.util.List;

/**
 * Optional extension for pivot selectors that need a separate candidate set and
 * evaluation set, mainly for MIX hierarchical pivot selection.
 */
public interface EvaluationPivotSelectionMethod extends PivotSelectionMethod
{
    default int[] selectPivots(Metric metric, List<? extends IndexObject> candidateSet,
                               List<? extends IndexObject> evaluationSet, int numPivots)
    {
        return selectPivots(metric, candidateSet, numPivots);
    }
}
