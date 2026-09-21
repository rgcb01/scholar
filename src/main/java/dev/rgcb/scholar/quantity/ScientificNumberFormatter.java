package dev.rgcb.scholar.quantity;

import java.math.BigDecimal;
import java.util.Objects;

public final class ScientificNumberFormatter {
    public String format(QuantityValue value, NumberNotation notation, boolean unicode) {
        Objects.requireNonNull(value); Objects.requireNonNull(notation);
        var nominal = value.nominal();
        var number = formatNumber(nominal.value(), notation, unicode);
        var unit = unicode ? nominal.unit().displaySymbol(UnitRegistry.builtIn()) : nominal.unit().asciiSymbol(UnitRegistry.builtIn());
        if (value instanceof MeasuredQuantity measured) {
            return number + " ± " + formatNumber(measured.absoluteUncertainty(), notation, unicode) + " " + unit;
        }
        return number + " " + unit;
    }

    public String formatNumber(BigDecimal value, NumberNotation notation, boolean unicode) {
        if (notation == NumberNotation.DECIMAL || value.signum() == 0) return plain(value);
        var stripped = value.stripTrailingZeros();
        var exponent = stripped.precision() - stripped.scale() - 1;
        if (notation == NumberNotation.ENGINEERING) exponent = Math.floorDiv(exponent, 3) * 3;
        if (exponent == 0) return plain(value);
        var mantissa = stripped.movePointLeft(exponent).stripTrailingZeros();
        return plain(mantissa) + (unicode ? " × 10" + superscript(exponent) : "e" + exponent);
    }

    private static String plain(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
    private static String superscript(int value) {
        var text = Integer.toString(value); var result = new StringBuilder();
        for (var c : text.toCharArray()) result.append(c == '-' ? '⁻' : "⁰¹²³⁴⁵⁶⁷⁸⁹".charAt(c - '0'));
        return result.toString();
    }
}
