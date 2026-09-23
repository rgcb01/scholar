package dev.rgcb.scholar.analysis;

import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.document.Document;
import java.util.ArrayList;
import java.util.List;
import java.math.MathContext;
import java.math.RoundingMode;

/** Presentation of derived results; no formatted value enters the document model. */
public final class AnalysisFormatter {
    private static final MathContext DISPLAY_PRECISION = new MathContext(6, RoundingMode.HALF_UP);
    private final DatasetAnalysisEngine engine = new DatasetAnalysisEngine();

    public List<String> lines(Document document, DatasetAnalysisBlock definition, boolean unicode) {
        var dataset = document.datasets().stream().filter(value -> value.id().equals(definition.datasetId())).findFirst();
        var title = switch (definition.kind()) {
            case DESCRIPTIVE -> "Descriptive Statistics";
            case LINEAR_REGRESSION -> "Linear Regression";
            case QUADRATIC_FIT -> "Quadratic Fit";
            case CUBIC_FIT -> "Cubic Fit";
        };
        var yName = dataset.flatMap(value -> value.columns().stream()
                .filter(column -> column.id().equals(definition.yColumnId())).findFirst())
                .map(value -> value.displayName()).orElse("[missing column]");
        var xName = definition.xColumnId().flatMap(id -> dataset.flatMap(value -> value.columns().stream()
                .filter(column -> column.id().equals(id)).findFirst())).map(value -> value.displayName()).orElse("[missing column]");
        var lines = new ArrayList<String>();
        lines.add(title + " - " + (definition.kind().isFit() ? yName + " vs " + xName : yName));
        var outcome = engine.evaluate(document, definition);
        if (outcome.result().isEmpty()) {
            lines.add("Analysis unavailable: " + outcome.diagnostics().getFirst().message());
            return List.copyOf(lines);
        }
        var result = outcome.result().orElseThrow();
        if (definition.kind().isFit()) {
            var terms = new ArrayList<String>();
            for (var i = result.coefficients().size() - 1; i >= 0; i--) {
                var suffix = i == 0 ? "" : (unicode ? " · " : " * ") + xName
                        + (i == 1 ? "" : unicode ? (i == 2 ? "²" : "³") : "^" + i);
                terms.add(display(result.coefficients().get(i), definition, unicode) + suffix);
            }
            lines.add(yName + " = " + String.join(" + ", terms));
            lines.add((unicode ? "R²" : "R^2") + " = " + result.rSquared().orElseThrow().round(DISPLAY_PRECISION)
                    .stripTrailingZeros().toPlainString());
            lines.add("n = " + result.observationCount());
        } else {
            for (var metric : result.metrics()) {
                lines.add(metric.name() + ": " + display(metric.value(), definition, unicode));
            }
        }
        for (var diagnostic : outcome.diagnostics()) lines.add(diagnostic.message());
        return List.copyOf(lines);
    }

    public String plainText(Document document, DatasetAnalysisBlock definition) {
        return String.join("\n", lines(document, definition, false));
    }

    private static String display(AnalysisValue value, DatasetAnalysisBlock definition, boolean unicode) {
        return new AnalysisValue(value.value().round(DISPLAY_PRECISION), value.unit(), value.semantics())
                .format(definition.notation(), unicode);
    }
}
