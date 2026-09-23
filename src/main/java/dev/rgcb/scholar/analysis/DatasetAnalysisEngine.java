package dev.rgcb.scholar.analysis;

import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetValueKind;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.quantity.PhysicalDimension;
import dev.rgcb.scholar.quantity.Quantity;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitExpression;
import dev.rgcb.scholar.quantity.UnitFactor;
import dev.rgcb.scholar.quantity.UnitRegistry;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Deterministic, derived analysis. Dataset identity scopes cache invalidation. */
public final class DatasetAnalysisEngine {
    private static final MathContext MC = UnitRegistry.MATH_CONTEXT;
    private static final UnitRegistry UNITS = UnitRegistry.builtIn();
    private final IdentityHashMap<ScientificDataset, Map<DatasetAnalysisBlock, AnalysisOutcome>> cache = new IdentityHashMap<>();

    public AnalysisOutcome evaluate(Document document, DatasetAnalysisBlock definition) {
        var dataset = document.datasets().stream().filter(value -> value.id().equals(definition.datasetId())).findFirst();
        if (dataset.isEmpty()) return AnalysisOutcome.failure(AnalysisDiagnostic.Code.MISSING_DATASET, "Dataset is unavailable.");
        var source = dataset.orElseThrow();
        if (cache.size() >= 32 && !cache.containsKey(source)) cache.clear();
        return cache.computeIfAbsent(source, ignored -> new HashMap<>())
                .computeIfAbsent(definition, ignored -> calculate(source, definition));
    }

    private static AnalysisOutcome calculate(ScientificDataset dataset, DatasetAnalysisBlock definition) {
        var yIndex = dataset.columnIndex(definition.yColumnId());
        var xIndex = definition.xColumnId().map(dataset::columnIndex).orElse(-1);
        if (yIndex < 0 || definition.kind().isFit() && xIndex < 0) {
            return AnalysisOutcome.failure(AnalysisDiagnostic.Code.MISSING_COLUMN, "Analysis column is unavailable.");
        }
        var yColumn = dataset.columns().get(yIndex);
        var xColumn = xIndex < 0 ? null : dataset.columns().get(xIndex);
        if (yColumn.type() != DatasetColumnType.NUMBER || xColumn != null && xColumn.type() != DatasetColumnType.NUMBER) {
            return AnalysisOutcome.failure(AnalysisDiagnostic.Code.TEXT_COLUMN, "Analysis requires numeric columns.");
        }
        if (definition.displayUnit().isPresent()) {
            if (yColumn.unit().isEmpty() || !yColumn.unit().orElseThrow().compatibleWith(definition.displayUnit().orElseThrow(), UNITS)) {
                return AnalysisOutcome.failure(AnalysisDiagnostic.Code.INCOMPATIBLE_DISPLAY_UNIT, "Display unit is incompatible with Y.");
            }
        }
        if (definition.kind().isFit() && (yColumn.quantitySemantics().isAbsoluteTemperature()
                || xColumn.quantitySemantics().isAbsoluteTemperature())) {
            return AnalysisOutcome.failure(AnalysisDiagnostic.Code.UNSUPPORTED_TEMPERATURE_AXIS,
                    "V1 fitting does not accept affine absolute-temperature axes.");
        }
        var x = new ArrayList<BigDecimal>();
        var y = new ArrayList<BigDecimal>();
        var missing = 0;
        for (var row : dataset.rows()) {
            var yCell = row.values().get(yIndex);
            var xCell = xIndex < 0 ? null : row.values().get(xIndex);
            if (yCell.kind() == DatasetValueKind.MISSING || xCell != null && xCell.kind() == DatasetValueKind.MISSING) {
                missing++;
                continue;
            }
            if (yCell.kind() != DatasetValueKind.NUMBER || xCell != null && xCell.kind() != DatasetValueKind.NUMBER) {
                return AnalysisOutcome.failure(AnalysisDiagnostic.Code.TEXT_COLUMN, "Numeric column contains text.");
            }
            y.add(convertY(yCell.number().orElseThrow(), yColumn, definition.displayUnit()));
            if (xCell != null) x.add(xCell.number().orElseThrow());
        }
        var yUnit = definition.displayUnit().or(() -> yColumn.unit());
        if (definition.kind() == AnalysisKind.DESCRIPTIVE) {
            return descriptive(y, missing, yUnit, yColumn.quantitySemantics());
        }
        return fit(x, y, missing, xColumn.unit(), yUnit, yColumn.quantitySemantics(), definition.kind().degree());
    }

    private static BigDecimal convertY(BigDecimal value, DatasetColumn column, Optional<UnitExpression> target) {
        if (target.isEmpty()) return value;
        return new Quantity(value, column.unit().orElseThrow(), column.quantitySemantics())
                .convertTo(target.orElseThrow()).value();
    }

    private static AnalysisOutcome descriptive(List<BigDecimal> values, int missing, Optional<UnitExpression> unit,
                                                QuantitySemantics semantics) {
        if (values.isEmpty()) return AnalysisOutcome.failure(AnalysisDiagnostic.Code.NO_OBSERVATIONS, "No valid observations.");
        var sorted = values.stream().sorted().toList();
        var sum = values.stream().reduce(BigDecimal.ZERO, (left, right) -> left.add(right, MC));
        var mean = sum.divide(BigDecimal.valueOf(values.size()), MC);
        var median = values.size() % 2 == 1 ? sorted.get(values.size() / 2)
                : sorted.get(values.size() / 2 - 1).add(sorted.get(values.size() / 2), MC).divide(BigDecimal.valueOf(2), MC);
        var metrics = new ArrayList<AnalysisResult.Metric>();
        metrics.add(metric("Count", BigDecimal.valueOf(values.size()), Optional.empty(), QuantitySemantics.LINEAR));
        metrics.add(metric("Missing", BigDecimal.valueOf(missing), Optional.empty(), QuantitySemantics.LINEAR));
        metrics.add(metric("Min", sorted.getFirst(), unit, semantics));
        metrics.add(metric("Max", sorted.getLast(), unit, semantics));
        metrics.add(metric("Mean", mean, unit, semantics));
        metrics.add(metric("Median", median, unit, semantics));
        if (values.size() > 1) {
            var sumSquares = BigDecimal.ZERO;
            for (var value : values) {
                var delta = value.subtract(mean, MC);
                sumSquares = sumSquares.add(delta.multiply(delta, MC), MC);
            }
            var variance = sumSquares.divide(BigDecimal.valueOf(values.size() - 1), MC);
            metrics.add(metric("Sample variance", variance, power(unit, 2), QuantitySemantics.LINEAR));
            metrics.add(metric("Sample SD", variance.sqrt(MC), unit,
                    semantics.isAbsoluteTemperature() ? QuantitySemantics.TEMPERATURE_DIFFERENCE : semantics));
        }
        var result = new AnalysisResult(values.size(), missing, metrics, List.of(), Optional.empty(), Optional.empty(), Optional.empty());
        return values.size() > 1 ? AnalysisOutcome.success(result) : new AnalysisOutcome(Optional.of(result),
                List.of(new AnalysisDiagnostic(AnalysisDiagnostic.Code.INSUFFICIENT_OBSERVATIONS,
                        "Sample variance and SD require two observations.")));
    }

    private static AnalysisOutcome fit(List<BigDecimal> x, List<BigDecimal> y, int missing,
                                       Optional<UnitExpression> xUnit, Optional<UnitExpression> yUnit,
                                       QuantitySemantics ySemantics, int degree) {
        var n = x.size();
        if (n < degree + 1) return AnalysisOutcome.failure(AnalysisDiagnostic.Code.INSUFFICIENT_OBSERVATIONS,
                "Fit requires at least " + (degree + 1) + " paired observations.");
        var xmin = x.stream().min(BigDecimal::compareTo).orElseThrow();
        var xmax = x.stream().max(BigDecimal::compareTo).orElseThrow();
        if (xmin.compareTo(xmax) == 0) return AnalysisOutcome.failure(AnalysisDiagnostic.Code.CONSTANT_X, "X is constant.");
        var center = xmin.add(xmax, MC).divide(BigDecimal.valueOf(2), MC);
        var halfRange = xmax.subtract(xmin, MC).divide(BigDecimal.valueOf(2), MC);
        var sums = new BigDecimal[degree * 2 + 1];
        var rhs = new BigDecimal[degree + 1];
        java.util.Arrays.fill(sums, BigDecimal.ZERO);
        java.util.Arrays.fill(rhs, BigDecimal.ZERO);
        for (var row = 0; row < n; row++) {
            var z = x.get(row).subtract(center, MC).divide(halfRange, MC);
            var power = BigDecimal.ONE;
            for (var index = 0; index < sums.length; index++) {
                sums[index] = sums[index].add(power, MC);
                if (index < rhs.length) rhs[index] = rhs[index].add(power.multiply(y.get(row), MC), MC);
                power = power.multiply(z, MC);
            }
        }
        var matrix = new BigDecimal[degree + 1][degree + 1];
        for (var row = 0; row <= degree; row++) for (var col = 0; col <= degree; col++) matrix[row][col] = sums[row + col];
        var normalized = solve(matrix, rhs);
        if (normalized.isEmpty()) return AnalysisOutcome.failure(AnalysisDiagnostic.Code.SINGULAR_FIT, "Fit is singular.");
        var coefficients = new BigDecimal[degree + 1];
        java.util.Arrays.fill(coefficients, BigDecimal.ZERO);
        for (var k = 0; k <= degree; k++) {
            var denominator = halfRange.pow(k, MC);
            for (var j = 0; j <= k; j++) {
                var term = normalized.orElseThrow()[k].multiply(BigDecimal.valueOf(binomial(k, j)), MC)
                        .multiply(center.negate().pow(k - j, MC), MC).divide(denominator, MC);
                coefficients[j] = coefficients[j].add(term, MC);
            }
        }
        var average = y.stream().reduce(BigDecimal.ZERO, (left, right) -> left.add(right, MC))
                .divide(BigDecimal.valueOf(n), MC);
        var sse = BigDecimal.ZERO;
        var sst = BigDecimal.ZERO;
        for (var row = 0; row < n; row++) {
            var predicted = polynomial(coefficients, x.get(row));
            var residual = y.get(row).subtract(predicted, MC);
            sse = sse.add(residual.multiply(residual, MC), MC);
            var centered = y.get(row).subtract(average, MC);
            sst = sst.add(centered.multiply(centered, MC), MC);
        }
        var rSquared = sst.signum() == 0 ? (sse.signum() == 0 ? BigDecimal.ONE : BigDecimal.ZERO)
                : BigDecimal.ONE.subtract(sse.divide(sst, MC), MC);
        var measured = new ArrayList<AnalysisValue>();
        for (var index = 0; index <= degree; index++) {
            var coefficientUnit = quotient(yUnit, xUnit, index);
            var coefficientSemantics = coefficientUnit.isPresent()
                    && coefficientUnit.orElseThrow().dimension(UNITS).equals(PhysicalDimension.TEMPERATURE)
                    ? QuantitySemantics.TEMPERATURE_DIFFERENCE : QuantitySemantics.LINEAR;
            measured.add(new AnalysisValue(coefficients[index], coefficientUnit, coefficientSemantics));
        }
        return AnalysisOutcome.success(new AnalysisResult(n, missing, List.of(), measured,
                Optional.of(rSquared), Optional.of(xmin), Optional.of(xmax)));
    }

    private static Optional<BigDecimal[]> solve(BigDecimal[][] matrix, BigDecimal[] rhs) {
        var length = rhs.length;
        for (var pivot = 0; pivot < length; pivot++) {
            var best = pivot;
            for (var row = pivot + 1; row < length; row++) {
                if (matrix[row][pivot].abs().compareTo(matrix[best][pivot].abs()) > 0) best = row;
            }
            if (matrix[best][pivot].signum() == 0) return Optional.empty();
            var temp = matrix[pivot]; matrix[pivot] = matrix[best]; matrix[best] = temp;
            var value = rhs[pivot]; rhs[pivot] = rhs[best]; rhs[best] = value;
            for (var row = pivot + 1; row < length; row++) {
                var factor = matrix[row][pivot].divide(matrix[pivot][pivot], MC);
                for (var col = pivot; col < length; col++) matrix[row][col] = matrix[row][col].subtract(factor.multiply(matrix[pivot][col], MC), MC);
                rhs[row] = rhs[row].subtract(factor.multiply(rhs[pivot], MC), MC);
            }
        }
        var result = new BigDecimal[length];
        for (var row = length - 1; row >= 0; row--) {
            var value = rhs[row];
            for (var col = row + 1; col < length; col++) value = value.subtract(matrix[row][col].multiply(result[col], MC), MC);
            result[row] = value.divide(matrix[row][row], MC);
        }
        return Optional.of(result);
    }

    private static BigDecimal polynomial(BigDecimal[] coefficients, BigDecimal x) {
        var value = BigDecimal.ZERO;
        for (var index = coefficients.length - 1; index >= 0; index--) value = value.multiply(x, MC).add(coefficients[index], MC);
        return value;
    }

    private static int binomial(int n, int k) {
        return switch (n) {
            case 0 -> 1;
            case 1 -> 1;
            case 2 -> k == 1 ? 2 : 1;
            case 3 -> k == 1 || k == 2 ? 3 : 1;
            default -> throw new IllegalArgumentException("Only degree 1-3 fits are supported.");
        };
    }

    private static AnalysisResult.Metric metric(String name, BigDecimal value, Optional<UnitExpression> unit,
                                                QuantitySemantics semantics) {
        return new AnalysisResult.Metric(name, new AnalysisValue(value, unit, semantics));
    }

    private static Optional<UnitExpression> power(Optional<UnitExpression> unit, int exponent) {
        return unit.map(source -> new UnitExpression(source.factors().stream()
                .map(factor -> new UnitFactor(factor.unitId(), factor.prefix(), factor.exponent() * exponent)).toList()));
    }

    private static Optional<UnitExpression> quotient(Optional<UnitExpression> numerator,
                                                     Optional<UnitExpression> denominator, int power) {
        var factors = new LinkedHashMap<String, UnitFactor>();
        var exponents = new LinkedHashMap<String, Integer>();
        numerator.ifPresent(unit -> unit.factors().forEach(factor -> addFactor(factors, exponents, factor, factor.exponent())));
        if (power > 0) denominator.ifPresent(unit -> unit.factors().forEach(factor ->
                addFactor(factors, exponents, factor, -factor.exponent() * power)));
        var combined = new ArrayList<UnitFactor>();
        exponents.forEach((key, exponent) -> { if (exponent != 0) {
            var factor = factors.get(key);
            combined.add(new UnitFactor(factor.unitId(), factor.prefix(), exponent));
        }});
        return combined.isEmpty() ? Optional.empty() : Optional.of(new UnitExpression(combined));
    }

    private static void addFactor(Map<String, UnitFactor> factors, Map<String, Integer> exponents,
                                  UnitFactor factor, int exponent) {
        var key = factor.unitId() + ":" + factor.prefix().map(Enum::name).orElse("");
        factors.putIfAbsent(key, factor);
        exponents.merge(key, exponent, Integer::sum);
    }
}
