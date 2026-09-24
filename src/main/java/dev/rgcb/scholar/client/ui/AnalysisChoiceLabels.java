package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.document.Document;

/** Readable choice text; an analysis ID remains the identity used when applying the choice. */
public final class AnalysisChoiceLabels {
    private AnalysisChoiceLabels() { }

    public static String fit(Document document, DatasetAnalysisBlock fit) {
        var dataset = document.datasets().stream().filter(value -> value.id().equals(fit.datasetId()))
                .findFirst();
        var datasetName = dataset.map(ScientificDataset::displayLabel).orElse("Missing dataset");
        var xName = dataset.flatMap(value -> value.columns().stream()
                .filter(column -> fit.xColumnId().filter(column.id()::equals).isPresent())
                .findFirst()).map(DatasetColumn::displayName).orElse("Missing X column");
        var yName = dataset.flatMap(value -> value.columns().stream()
                .filter(column -> column.id().equals(fit.yColumnId())).findFirst())
                .map(DatasetColumn::displayName).orElse("Missing Y column");
        var kind = switch (fit.kind()) {
            case DESCRIPTIVE -> "Descriptive statistics";
            case LINEAR_REGRESSION -> "Linear regression";
            case QUADRATIC_FIT -> "Quadratic fit";
            case CUBIC_FIT -> "Cubic fit";
        };
        return kind + " - " + datasetName + " (" + yName + " vs " + xName + ")";
    }
}
