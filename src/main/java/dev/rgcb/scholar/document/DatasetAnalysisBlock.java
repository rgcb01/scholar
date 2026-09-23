package dev.rgcb.scholar.document;

import dev.rgcb.scholar.analysis.AnalysisKind;
import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.quantity.UnitExpression;
import java.util.Objects;
import java.util.Optional;

/** Authored dataset analysis; computed statistics and fitted samples are never stored here. */
public record DatasetAnalysisBlock(String id, String datasetId, AnalysisKind kind, Optional<String> xColumnId,
                                   String yColumnId, Optional<UnitExpression> displayUnit,
                                   NumberNotation notation) implements BlockNode {
    public DatasetAnalysisBlock {
        id = Objects.requireNonNull(id, "id").trim();
        datasetId = Objects.requireNonNull(datasetId, "datasetId").trim();
        kind = Objects.requireNonNull(kind, "kind");
        xColumnId = Objects.requireNonNull(xColumnId, "xColumnId");
        yColumnId = Objects.requireNonNull(yColumnId, "yColumnId").trim();
        displayUnit = Objects.requireNonNull(displayUnit, "displayUnit");
        notation = Objects.requireNonNull(notation, "notation");
        if (id.isEmpty() || datasetId.isEmpty() || yColumnId.isEmpty()
                || kind.isFit() != xColumnId.isPresent()
                || xColumnId.filter(String::isBlank).isPresent()) {
            throw new IllegalArgumentException("Analysis requires stable identity and columns appropriate to its kind.");
        }
    }
}
