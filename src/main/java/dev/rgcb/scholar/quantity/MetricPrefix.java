package dev.rgcb.scholar.quantity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public enum MetricPrefix {
    QUECTO("q", -30), RONTO("r", -27), YOCTO("y", -24), ZEPTO("z", -21), ATTO("a", -18),
    FEMTO("f", -15), PICO("p", -12), NANO("n", -9), MICRO("µ", -6), MILLI("m", -3),
    CENTI("c", -2), DECI("d", -1), DECA("da", 1), HECTO("h", 2), KILO("k", 3),
    MEGA("M", 6), GIGA("G", 9), TERA("T", 12), PETA("P", 15), EXA("E", 18),
    ZETTA("Z", 21), YOTTA("Y", 24), RONNA("R", 27), QUETTA("Q", 30);

    private final String symbol;
    private final int exponent;

    MetricPrefix(String symbol, int exponent) { this.symbol = symbol; this.exponent = exponent; }
    public String symbol() { return symbol; }
    public int exponent() { return exponent; }
    public BigDecimal factor() { return BigDecimal.ONE.scaleByPowerOfTen(exponent); }

    public static Optional<MetricPrefix> fromLeadingSymbol(String value) {
        var normalized = value.replace('μ', 'µ');
        return Arrays.stream(values()).sorted(Comparator.comparingInt((MetricPrefix p) -> p.symbol.length()).reversed())
                .filter(prefix -> normalized.startsWith(prefix.symbol)).findFirst();
    }
}
