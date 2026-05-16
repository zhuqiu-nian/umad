package metric;

import db.type.IndexObject;
import db.type.StringObject;

public class EditDistanceMetric implements Metric {
    private static final long serialVersionUID = 4224280787519101840L;

    @Override
    public double getDistance(IndexObject one, IndexObject two) {
        if (!(one instanceof StringObject) || !(two instanceof StringObject)) {
            throw new IllegalArgumentException("EditDistanceMetric only supports StringObject");
        }
        return editDistance(((StringObject) one).getValue(), ((StringObject) two).getValue());
    }

    private int editDistance(String left, String right) {
        int leftLength = left.length();
        int rightLength = right.length();
        if (leftLength == 0) {
            return rightLength;
        }
        if (rightLength == 0) {
            return leftLength;
        }

        int[] previous = new int[rightLength + 1];
        int[] current = new int[rightLength + 1];
        for (int j = 0; j <= rightLength; j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= leftLength; i++) {
            current[0] = i;
            char leftChar = left.charAt(i - 1);
            for (int j = 1; j <= rightLength; j++) {
                int substitutionCost = leftChar == right.charAt(j - 1) ? 0 : 1;
                int deletion = previous[j] + 1;
                int insertion = current[j - 1] + 1;
                int substitution = previous[j - 1] + substitutionCost;
                current[j] = Math.min(Math.min(deletion, insertion), substitution);
            }

            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[rightLength];
    }
}
