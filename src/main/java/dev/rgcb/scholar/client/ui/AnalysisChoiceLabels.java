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
        var datasetName = dataset.map(ScientificDataset::displayLabel)
                .orElseGet(() -> ScholarTranslations.get("scholar.dataset.missing"));
        var xName = dataset.flatMap(value -> value.columns().stream()
                .filter(column -> fit.xColumnId().filter(column.id()::equals).isPresent())
                .findFirst()).map(DatasetColumn::displayName)
                .orElseGet(() -> ScholarTranslations.get("scholar.dataset.missing_x_column"));
        var yName = dataset.flatMap(value -> value.columns().stream()
                .filter(column -> column.id().equals(fit.yColumnId())).findFirst())
                .map(DatasetColumn::displayName)
                .orElseGet(() -> ScholarTranslations.get("scholar.dataset.missing_y_column"));
        var kind = switch (fit.kind()) {
            case DESCRIPTIVE -> ScholarTranslations.get("scholar.dataset.analysis.descriptive");
            case LINEAR_REGRESSION -> ScholarTranslations.get("scholar.dataset.analysis.linear_regression");
            case QUADRATIC_FIT -> ScholarTranslations.get("scholar.dataset.analysis.quadratic_fit");
            case CUBIC_FIT -> ScholarTranslations.get("scholar.dataset.analysis.cubic_fit");
        };
        return ScholarTranslations.get("scholar.dataset.analysis.choice", kind, datasetName, yName, xName);
    }
}
