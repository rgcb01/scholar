package dev.rgcb.scholar.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DatasetAnalysisEngineTest {
    private final DatasetAnalysisEngine engine = new DatasetAnalysisEngine();
    private final UnitParser units = new UnitParser();

    @Test void descriptiveStatisticsIgnoreMissingAndKeepUnits() {
        var dataset = oneColumn("cm", "1", "2", null, "3", "4");
        var result = outcome(dataset, AnalysisKind.DESCRIPTIVE).result().orElseThrow();
        assertEquals(4, result.observationCount());
        assertEquals(1, result.missingCount());
        assertDecimal("1", metric(result, "Min"));
        assertDecimal("4", metric(result, "Max"));
        assertDecimal("2.5", metric(result, "Mean"));
        assertDecimal("2.5", metric(result, "Median"));
        assertDecimal("1.666666666666666666666666666666667", metric(result, "Sample variance"));
        assertEquals("cm²", result.metrics().stream().filter(m -> m.name().equals("Sample variance"))
                .findFirst().orElseThrow().value().unit().orElseThrow().displaySymbol(dev.rgcb.scholar.quantity.UnitRegistry.builtIn()));
    }

    @Test void oneAndZeroObservationsHaveHonestDiagnostics() {
        var one = outcome(oneColumn("m", "3"), AnalysisKind.DESCRIPTIVE);
        assertEquals(1, one.result().orElseThrow().observationCount());
        assertFalse(one.result().orElseThrow().metrics().stream().anyMatch(metric -> metric.name().equals("Sample SD")));
        assertEquals(AnalysisDiagnostic.Code.INSUFFICIENT_OBSERVATIONS, one.diagnostics().getFirst().code());
        assertEquals(AnalysisDiagnostic.Code.NO_OBSERVATIONS,
                outcome(oneColumn("m", (String) null), AnalysisKind.DESCRIPTIVE).diagnostics().getFirst().code());
    }

    @Test void absoluteTemperatureMeanAndSpreadHaveDifferentSemantics() {
        var result = outcome(oneColumn("°C", "20", "22"), AnalysisKind.DESCRIPTIVE).result().orElseThrow();
        assertEquals(QuantitySemantics.ABSOLUTE_TEMPERATURE,
                result.metrics().stream().filter(m -> m.name().equals("Mean")).findFirst().orElseThrow().value().semantics());
        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE,
                result.metrics().stream().filter(m -> m.name().equals("Sample SD")).findFirst().orElseThrow().value().semantics());
        assertEquals(QuantitySemantics.LINEAR,
                result.metrics().stream().filter(m -> m.name().equals("Sample variance")).findFirst().orElseThrow().value().semantics());
    }

    @Test void exactLinearQuadraticAndCubicFitsHaveCorrectUnits() {
        for (var degree = 1; degree <= 3; degree++) {
            var kind = switch (degree) {
                case 1 -> AnalysisKind.LINEAR_REGRESSION;
                case 2 -> AnalysisKind.QUADRATIC_FIT;
                default -> AnalysisKind.CUBIC_FIT;
            };
            var rows = new ArrayList<DatasetRow>();
            for (var i = 0; i < 6; i++) {
                var y = 2 + 3 * i + (degree >= 2 ? 4 * i * i : 0) + (degree == 3 ? 5 * i * i * i : 0);
                rows.add(new DatasetRow(List.of(DatasetValue.number(Integer.toString(i)), DatasetValue.number(Integer.toString(y)))));
            }
            var dataset = paired(rows);
            var result = outcome(dataset, kind).result().orElseThrow();
            assertEquals(6, result.observationCount());
            assertEquals(degree + 1, result.coefficients().size());
            assertTrue(result.predict(BigDecimal.valueOf(7)).subtract(BigDecimal.valueOf(
                    2 + 3 * 7 + (degree >= 2 ? 4 * 49 : 0) + (degree == 3 ? 5 * 343 : 0))).abs()
                    .compareTo(new BigDecimal("0.0000001")) < 0);
            assertEquals("m/s", result.coefficients().get(1).unit().orElseThrow()
                    .displaySymbol(dev.rgcb.scholar.quantity.UnitRegistry.builtIn()));
            assertTrue(result.rSquared().orElseThrow().subtract(BigDecimal.ONE).abs()
                    .compareTo(new BigDecimal("0.0000001")) < 0);
        }
    }

    @Test void regressionUsesOnlyCompletePairsAndRejectsConstantX() {
        var dataset = paired(List.of(row("0", "1"), row("1", null), row("2", "5")));
        var result = outcome(dataset, AnalysisKind.LINEAR_REGRESSION).result().orElseThrow();
        assertEquals(2, result.observationCount());
        assertEquals(1, result.missingCount());
        assertDecimal("2", result.coefficients().get(1).value());
        var constant = paired(List.of(row("1", "1"), row("1", "2")));
        assertEquals(AnalysisDiagnostic.Code.CONSTANT_X,
                outcome(constant, AnalysisKind.LINEAR_REGRESSION).diagnostics().getFirst().code());
    }

    @Test void absoluteTemperatureAxesAreExplicitlyRejectedForFitting() {
        var dataset = new ScientificDataset("d", "Temperature", List.of(
                new DatasetColumn("x", "Time", DatasetColumnType.NUMBER, Optional.of(units.parseRequired("s"))),
                new DatasetColumn("y", "Temperature", DatasetColumnType.NUMBER, Optional.of(units.parseRequired("°C")))),
                List.of(row("0", "20"), row("1", "21")));
        assertEquals(AnalysisDiagnostic.Code.UNSUPPORTED_TEMPERATURE_AXIS,
                outcome(dataset, AnalysisKind.LINEAR_REGRESSION).diagnostics().getFirst().code());
    }

    @Test void oddMedianAndNoisyPolynomialAreDeterministic() {
        var odd = outcome(oneColumn("m", "7", "1", "3"), AnalysisKind.DESCRIPTIVE).result().orElseThrow();
        assertDecimal("3", metric(odd, "Median"));
        var noisy = paired(List.of(row("0", "1"), row("1", "4"), row("2", "10"),
                row("3", "17"), row("4", "27"), row("5", "41")));
        var first = outcome(noisy, AnalysisKind.QUADRATIC_FIT).result().orElseThrow();
        var second = outcome(noisy, AnalysisKind.QUADRATIC_FIT).result().orElseThrow();
        assertSame(first, second);
        assertTrue(first.rSquared().orElseThrow().compareTo(new BigDecimal("0.98")) > 0);
        assertEquals("m/s²", first.coefficients().get(2).unit().orElseThrow()
                .displaySymbol(dev.rgcb.scholar.quantity.UnitRegistry.builtIn()));
    }

    @Test void datasetIdentityScopesCacheAndMissingColumnIsDiagnostic() {
        var source = paired(List.of(row("0", "1"), row("1", "3")));
        var definition = new DatasetAnalysisBlock("fit", "d", AnalysisKind.LINEAR_REGRESSION,
                Optional.of("x"), "y", Optional.empty(), NumberNotation.DECIMAL);
        var original = engine.evaluate(new Document(List.of(definition), List.of(source)), definition);
        var unrelated = new ScientificDataset("other", "Other", List.of(new DatasetColumn("a", "A", DatasetColumnType.NUMBER)),
                List.of(new DatasetRow(List.of(DatasetValue.number("2")))));
        assertSame(original, engine.evaluate(new Document(List.of(definition), List.of(source, unrelated)), definition));
        var updated = paired(List.of(row("0", "1"), row("1", "5")));
        assertNotEquals(original.result().orElseThrow().coefficients(),
                engine.evaluate(new Document(List.of(definition), List.of(updated)), definition).result().orElseThrow().coefficients());
        var missingColumn = new DatasetAnalysisBlock("fit", "d", AnalysisKind.DESCRIPTIVE, Optional.empty(), "gone",
                Optional.empty(), NumberNotation.DECIMAL);
        assertEquals(AnalysisDiagnostic.Code.MISSING_COLUMN,
                engine.evaluate(new Document(List.of(missingColumn), List.of(source)), missingColumn).diagnostics().getFirst().code());
    }

    @Test void polynomialWithInsufficientDistinctXIsSingular() {
        var data = paired(List.of(row("0", "1"), row("0", "2"), row("1", "3")));
        assertEquals(AnalysisDiagnostic.Code.SINGULAR_FIT,
                outcome(data, AnalysisKind.QUADRATIC_FIT).diagnostics().getFirst().code());
    }

    private AnalysisOutcome outcome(ScientificDataset dataset, AnalysisKind kind) {
        var block = new DatasetAnalysisBlock("a", dataset.id(), kind,
                kind.isFit() ? Optional.of("x") : Optional.empty(), "y", Optional.empty(), NumberNotation.DECIMAL);
        return engine.evaluate(new Document(List.of(block), List.of(dataset)), block);
    }

    private ScientificDataset oneColumn(String unit, String... values) {
        var rows = new ArrayList<DatasetRow>();
        for (var value : values) rows.add(new DatasetRow(List.of(value == null ? DatasetValue.missing() : DatasetValue.number(value))));
        return new ScientificDataset("d", "Values", List.of(new DatasetColumn("y", "Y", DatasetColumnType.NUMBER,
                Optional.of(units.parseRequired(unit)))), rows);
    }

    private ScientificDataset paired(List<DatasetRow> rows) {
        return new ScientificDataset("d", "Pairs", List.of(
                new DatasetColumn("x", "X", DatasetColumnType.NUMBER, Optional.of(units.parseRequired("s"))),
                new DatasetColumn("y", "Y", DatasetColumnType.NUMBER, Optional.of(units.parseRequired("m")))), rows);
    }

    private static DatasetRow row(String x, String y) {
        return new DatasetRow(List.of(x == null ? DatasetValue.missing() : DatasetValue.number(x),
                y == null ? DatasetValue.missing() : DatasetValue.number(y)));
    }

    private static BigDecimal metric(AnalysisResult result, String name) {
        return result.metrics().stream().filter(metric -> metric.name().equals(name)).findFirst().orElseThrow().value().value();
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
