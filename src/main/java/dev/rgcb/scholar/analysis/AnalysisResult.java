package dev.rgcb.scholar.analysis;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AnalysisResult(int observationCount, int missingCount, List<Metric> metrics,
                             List<AnalysisValue> coefficients, Optional<BigDecimal> rSquared,
                             Optional<BigDecimal> xMinimum, Optional<BigDecimal> xMaximum) {
    public record Metric(String name, AnalysisValue value) {
        public Metric { Objects.requireNonNull(name); Objects.requireNonNull(value); }
    }

    public AnalysisResult {
        if (observationCount < 0 || missingCount < 0) throw new IllegalArgumentException("Negative observation count.");
        metrics = List.copyOf(metrics);
        coefficients = List.copyOf(coefficients);
        Objects.requireNonNull(rSquared);
        Objects.requireNonNull(xMinimum);
        Objects.requireNonNull(xMaximum);
    }

    public BigDecimal predict(BigDecimal x) {
        var value = BigDecimal.ZERO;
        for (var index = coefficients.size() - 1; index >= 0; index--) {
            value = value.multiply(x, dev.rgcb.scholar.quantity.UnitRegistry.MATH_CONTEXT)
                    .add(coefficients.get(index).value(), dev.rgcb.scholar.quantity.UnitRegistry.MATH_CONTEXT);
        }
        return value;
    }
}
