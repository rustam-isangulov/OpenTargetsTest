package TargetDiseaseScore;

import java.math.BigDecimal;
import java.util.Arrays;

public class Calculator {
    private Calculator() {}

    static double median(double[] l) {
        if (l == null)
            throw new IllegalArgumentException("Attempt to calculate median value on a null list");
        if (l.length == 0)
            throw new IllegalArgumentException("Attempt to calculate median value on an empty list");

        // sort first
        Arrays.sort(l);

        if (l.length % 2 == 0) {
            // this is to avoid double precision errors for simple score computation
            return BigDecimal.valueOf(l[l.length/2])
                    .add(BigDecimal.valueOf(l[l.length/2 - 1]))
                    .divide(BigDecimal.valueOf(2.0)).doubleValue();
        } else {
            return l[l.length/2];
        }
    }
}
