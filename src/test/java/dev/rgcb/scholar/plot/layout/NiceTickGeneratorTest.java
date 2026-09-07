package dev.rgcb.scholar.plot.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.rgcb.scholar.plot.AxisRange;
import java.util.List;
import org.junit.jupiter.api.Test;

class NiceTickGeneratorTest {
    private final NiceTickGenerator generator = new NiceTickGenerator();

    @Test
    void generatesClassicZeroToTenTicks() {
        assertEquals(
                List.of("0", "2", "4", "6", "8", "10"),
                generator.generate(new AxisRange(0, 10)).stream().map(AxisTick::label).toList());
    }

    @Test
    void generatesSymmetricDecimalTicksWithoutFloatingNoise() {
        var ticks = generator.generate(new AxisRange(-1.1, 1.1));
        assertEquals(List.of("-1", "-0.5", "0", "0.5", "1"), ticks.stream().map(AxisTick::label).toList());
        assertEquals(List.of(-1.0, -0.5, 0.0, 0.5, 1.0), ticks.stream().map(AxisTick::value).toList());
    }

    @Test
    void smallDecimalStepKeepsReadablePrecision() {
        assertEquals(
                List.of("0", "0.02", "0.04", "0.06", "0.08", "0.1"),
                generator.generate(new AxisRange(0, 0.1)).stream().map(AxisTick::label).toList());
    }

    @Test
    void verySmallValuesUseDeterministicScientificNotation() {
        var labels = generator.generate(new AxisRange(1e-8, 6e-8)).stream().map(AxisTick::label).toList();
        assertEquals(List.of("1E-8", "2E-8", "3E-8", "4E-8", "5E-8", "6E-8"), labels);
    }

    @Test
    void niceStepUsesOneTwoFiveDecades() {
        assertEquals(1.0, NiceTickGenerator.niceStep(0.7));
        assertEquals(2.0, NiceTickGenerator.niceStep(1.1));
        assertEquals(5.0, NiceTickGenerator.niceStep(2.1));
        assertEquals(10.0, NiceTickGenerator.niceStep(5.1));
        assertEquals(0.05, NiceTickGenerator.niceStep(0.03), 1e-12);
    }

    @Test
    void requiresAtLeastTwoTargetTicks() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(new AxisRange(0, 1), 1));
    }

    @Test
    void outputIsDeterministicAcrossRepeatedCalls() {
        var range = new AxisRange(-12.3, 48.7);
        assertEquals(generator.generate(range), generator.generate(range));
    }
    @Test
    void subnormalRangeFallsBackToFiniteEndpointTicks() {
        var min = Double.MIN_VALUE;
        var max = Math.nextUp(min);
        var ticks = generator.generate(new AxisRange(min, max));

        assertEquals(2, ticks.size());
        assertEquals(min, ticks.get(0).value());
        assertEquals(max, ticks.get(1).value());
        assertEquals(List.of("4.9E-324", "9.9E-324"), ticks.stream().map(AxisTick::label).toList());
    }

    @Test
    void targetOfTwoFallsBackToEndpointsInsteadOfReturningOneInteriorTick() {
        var range = new AxisRange(-0.05, 1.05);
        var ticks = generator.generate(range, 2);

        assertEquals(2, ticks.size());
        assertEquals(range.min(), ticks.get(0).value());
        assertEquals(range.max(), ticks.get(1).value());
    }

}
