package xin.vanilla.narcissus.service.cost;

import org.junit.Test;
import xin.vanilla.narcissus.api.cost.CostParameters;
import xin.vanilla.narcissus.data.cost.CostCalculation;
import xin.vanilla.narcissus.enums.EnumCostType;

import static org.junit.Assert.*;

public class CostCalculatorTest {
    private final CostCalculator calculator = new CostCalculator();

    private CostParameters parameters(double fixed, double rate, int min, int max) {
        return new CostParameters(EnumCostType.EXP_POINT, fixed, rate, min, max, "", "", "");
    }

    @Test
    public void fixedAndDistanceFeesAreCombined() {
        assertEquals(5, calculator.calculate(parameters(2, .002, 0, 20), 1500).amount());
    }

    @Test
    public void defaultBoundsLimitTheDistanceFee() {
        assertEquals(20, calculator.calculate(parameters(0, .002, 0, 20), 10000).amount());
    }

    @Test
    public void unlimitedUpperBoundStillRoundsUp() {
        assertEquals(21, calculator.calculate(parameters(20.01, 0, 0, -1), 0).amount());
    }

    @Test
    public void zeroUpperBoundMeansZeroRatherThanUnlimited() {
        assertEquals(0, calculator.calculate(parameters(2, 1, 0, 0), 10).amount());
    }

    @Test
    public void lowerBoundIsAppliedBeforeRounding() {
        assertEquals(3, calculator.calculate(parameters(.01, 0, 3, 8), 0).amount());
    }

    @Test
    public void rejectsInvalidDistanceAndOverflow() {
        for (double distance : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1}) {
            assertFalse(calculator.calculate(parameters(0, 1, 0, -1), distance).isSuccess());
        }
        assertFalse(calculator.calculate(parameters(Integer.MAX_VALUE + .1, 0, 0, -1), 0).isSuccess());
        assertFalse(calculator.calculate(parameters(Double.MAX_VALUE, Double.MAX_VALUE, 0, 20), 2).isSuccess());
    }

    @Test
    public void failedCalculationCannotBeReadAsAnAmount() {
        CostCalculation failed = calculator.calculate(parameters(0, 1, 0, -1), Double.NaN);
        try {
            failed.amount();
            fail("A failed result must not encode a payable amount");
        } catch (IllegalStateException expected) {
            assertFalse(failed.isSuccess());
        }
    }

    @Test
    public void freeCostDoesNotRequireDistanceOrFormula() {
        assertEquals(0, calculator.calculate(CostParameters.free(), Double.NaN).amount());
        assertEquals(0, calculator.calculate(CostParameters.free(), null, null).amount());
    }

    @Test
    public void invalidConfigurationIsRejectedBeforeEvaluation() {
        for (double value : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            try {
                parameters(value, 0, 0, 20);
                fail("Invalid fixed amount");
            } catch (IllegalArgumentException expected) { /* Expected validation boundary. */ }
            try {
                parameters(0, value, 0, 20);
                fail("Invalid distance rate");
            } catch (IllegalArgumentException expected) { /* Expected validation boundary. */ }
        }
        try {
            parameters(0, 1, 3, 2);
            fail("Reversed limits");
        } catch (IllegalArgumentException expected) { /* Expected validation boundary. */ }
    }

    @Test
    public void customFormulaKeepsRawValidationBeforeClampingAndIntegerCeiling() {
        CostParameters bounded = parameters(0, 0, 3, 8);
        xin.vanilla.narcissus.api.cost.CostContext context = new CostPaymentServiceTest.Fixture().context(
                xin.vanilla.narcissus.api.cost.CostPhase.CHECK);
        for (double raw : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1}) {
            assertEquals(xin.vanilla.narcissus.enums.EnumCostFailure.INVALID_RESULT,
                    calculator.calculate(bounded, context, c -> raw).failure());
        }
        assertEquals(3, calculator.calculate(bounded, context, c -> .01).amount());
        assertEquals(4, calculator.calculate(bounded, context, c -> 3.01).amount());
        assertEquals(8, calculator.calculate(bounded, context, c -> Double.MAX_VALUE).amount());
        CostParameters unlimited = parameters(0, 0, 0, -1);
        assertEquals(Integer.MAX_VALUE, calculator.calculate(unlimited, context, c -> Integer.MAX_VALUE).amount());
        assertEquals(xin.vanilla.narcissus.enums.EnumCostFailure.AMOUNT_OVERFLOW,
                calculator.calculate(unlimited, context, c -> Integer.MAX_VALUE + .01).failure());
        assertEquals(xin.vanilla.narcissus.enums.EnumCostFailure.FORMULA_FAILED,
                calculator.calculate(unlimited, context, c -> { throw new IllegalStateException("Formula failed"); }).failure());
    }

    @Test
    public void textLimitsAreMeasuredInCharacters() {
        new CostParameters(EnumCostType.COMMAND, 0, 0, 0, 20, "", repeat(8192), "");
        try {
            new CostParameters(EnumCostType.COMMAND, 0, 0, 0, 20, "", repeat(8193), "");
            fail("Too long");
        } catch (IllegalArgumentException expected) { /* Expected validation boundary. */ }
    }

    private static String repeat(int length) {
        char[] value = new char[length];
        java.util.Arrays.fill(value, 'a');
        return new String(value);
    }
}
