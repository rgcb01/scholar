package dev.rgcb.scholar.layout;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.analysis.AnalysisKind;
import dev.rgcb.scholar.data.*;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.quantity.NumberNotation;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnalysisLayoutTest {
    private static final TextMeasurer MEASURER = new TextMeasurer() {
        @Override public int measureWidth(String text, TextStyle style) { return text.length() * 6; }
        @Override public int lineHeight(TextStyle style) { return 12; }
    };

    @Test void analysisHasMultipleReadableDerivedLinesAndPlainTextWithoutIds() {
        var source = new ScientificDataset("internal-dataset", "Measurements",
                List.of(new DatasetColumn("internal-column", "Length", DatasetColumnType.NUMBER)),
                List.of(new DatasetRow(List.of(DatasetValue.number("1"))),
                        new DatasetRow(List.of(DatasetValue.number("3")))));
        var analysis = new DatasetAnalysisBlock("internal-analysis", source.id(), AnalysisKind.DESCRIPTIVE,
                Optional.empty(), "internal-column", Optional.empty(), NumberNotation.DECIMAL);
        var document = new Document(List.of(analysis), List.of(source));
        var layout = new DocumentLayoutEngine().layout(document, 320, MEASURER);
        assertEquals(LaidOutBlockKind.ANALYSIS, layout.blocks().getFirst().kind());
        assertTrue(layout.blocks().getFirst().lines().size() >= 6);
        var exported = new DocumentPlainTextSerializer().serialize(document);
        assertTrue(exported.contains("Descriptive Statistics - Length"));
        assertTrue(exported.contains("Mean: 2"));
        assertFalse(exported.contains("internal-analysis"));
        assertFalse(exported.contains("internal-dataset"));
    }

    @Test void regressionTextExportContainsEquationAndObservationCountWithoutInternalIds() {
        var dataset = new ScientificDataset("dataset-id", "Motion", List.of(
                new DatasetColumn("time-id", "Time", DatasetColumnType.NUMBER),
                new DatasetColumn("distance-id", "Distance", DatasetColumnType.NUMBER)), List.of(
                new DatasetRow(List.of(DatasetValue.number("0"), DatasetValue.number("1"))),
                new DatasetRow(List.of(DatasetValue.number("1"), DatasetValue.number("3")))));
        var block = new DatasetAnalysisBlock("analysis-id", dataset.id(), AnalysisKind.LINEAR_REGRESSION,
                Optional.of("time-id"), "distance-id", Optional.empty(), NumberNotation.DECIMAL);
        var text = new DocumentPlainTextSerializer().serialize(new Document(List.of(block), List.of(dataset)));
        assertTrue(text.contains("Linear Regression - Distance vs Time"));
        assertTrue(text.contains("R^2 = 1"));
        assertTrue(text.contains("n = 2"));
        assertFalse(text.contains("analysis-id"));
        assertFalse(text.contains("dataset-id"));
    }
}
