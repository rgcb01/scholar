package dev.rgcb.scholar.plot.layout;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Deterministic axis-number formatting tied to the generated tick step. */
public final class PlotNumberFormatter {
    private PlotNumberFormatter() {
    }

    public static String format(double value, double step) {
        if (!Double.isFinite(value) || !Double.isFinite(step) || step <= 0.0) {
            throw new IllegalArgumentException("Plot tick formatting requires finite value and positive step.");
        }
        var normalized = Math.abs(value) < Math.abs(step) * 1.0e-10 ? 0.0 : value;
        var abs = Math.abs(normalized);
        if (abs != 0.0 && (abs >= 1.0e9 || abs < 1.0e-6)) {
            return scientific(normalized);
        }
        var scale = decimalScale(step);
        return BigDecimal.valueOf(normalized)
                .setScale(scale, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    /** Formats a tick that was generated as an integer multiple of a nice step.
     *
     * <p>Using the decimal representation of the step and integer index avoids
     * exposing binary floating-point multiplication noise such as
     * {@code 3.0000000000000004E-8} in axis labels.</p>
     */
    static String formatGeneratedTick(long index, double step) {
        if (!Double.isFinite(step) || step <= 0.0) {
            throw new IllegalArgumentException("Plot tick formatting requires a finite positive step.");
        }

        var exact = BigDecimal.valueOf(step).multiply(BigDecimal.valueOf(index));
        if (exact.signum() == 0) {
            return "0";
        }

        var approximateAbs = Math.abs(exact.doubleValue());
        if (approximateAbs >= 1.0e9 || approximateAbs < 1.0e-6) {
            return scientific(exact);
        }

        return exact
                .setScale(decimalScale(step), RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static int decimalScale(double step) {
        var exponent = (int) Math.floor(Math.log10(Math.abs(step)));
        return Math.max(0, -exponent);
    }

    private static String scientific(double value) {
        if (value == 0.0) {
            return "0";
        }
        // BigDecimal.valueOf(double) starts from Double.toString's canonical
        // decimal representation, so it remains well-defined even for
        // subnormal values where Math.pow(10, -324) underflows to zero.
        return scientific(BigDecimal.valueOf(value));
    }

    private static String scientific(BigDecimal value) {
        return value
                .stripTrailingZeros()
                .toString()
                .replace("E+", "E");
    }
}
