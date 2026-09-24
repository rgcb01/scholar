package dev.rgcb.scholar.client.screen;

import dev.rgcb.scholar.client.ui.AnalysisChoiceLabels;
import dev.rgcb.scholar.analysis.AnalysisKind;
import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.quantity.NumberNotation;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScholarAnalysisDialogTest {
    @Test void fitChoiceShowsHumanReadableDatasetAndColumnsInsteadOfIds() {
        var dataset = new ScientificDataset("dataset-internal", "Free Fall", List.of(
                new DatasetColumn("x-internal", "Time", DatasetColumnType.NUMBER),
                new DatasetColumn("y-internal", "Distance", DatasetColumnType.NUMBER)), List.of());
        var document = new Document(List.of(new Paragraph(new InlineContent(List.of(new Text("Result", Set.of()))))),
                List.of(dataset));
        var analysis = new DatasetAnalysisBlock("fit-internal", dataset.id(), AnalysisKind.QUADRATIC_FIT,
                Optional.of("x-internal"), "y-internal", Optional.empty(), NumberNotation.DECIMAL);

        var label = AnalysisChoiceLabels.fit(document, analysis);
        assertEquals("Quadratic fit - Free Fall (Distance vs Time)", label);
        assertFalse(label.contains("internal"));
    }
}
