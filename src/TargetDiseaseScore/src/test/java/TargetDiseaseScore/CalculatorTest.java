package TargetDiseaseScore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


public class CalculatorTest {

    @ParameterizedTest
    @MethodSource("medianInputs")
    @DisplayName("Median calculation test \uD83E\uDD20.")
    public void testMedian(double[] input, double output) {
        assertEquals(output, Calculator.median(input), 0);
    }

    public static Stream<Arguments> medianInputs() {
        return Stream.of(
                Arguments.of(new double[] {1, 3, 3, 6, 7, 8, 9}, 6)
                , Arguments.of(new double[] {1, 2, 3, 4, 5, 6, 8, 9}, 4.5)
                , Arguments.of(new double[]{2}, 2)
        );
    }


    @Test
    @DisplayName("Median calculation bad input test.")
    public void testMedianBadInputs() {
        assertAll("Test for illegal array inputs"
                , () -> assertThrows(IllegalArgumentException.class, () -> Calculator.median(null)
                        , () -> "null value should throw IllegalArgumentException")
                , () -> assertThrows(IllegalArgumentException.class, () -> Calculator.median(new double[]{})
                        , () -> "empty array should throw IllegalArgumentException"));

    }
}
