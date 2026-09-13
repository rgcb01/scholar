package dev.rgcb.scholar.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CrossReferenceResolverTest {
    private final CrossReferenceResolver resolver = new CrossReferenceResolver();

    @Test
    void crossReferenceRequiresKindAndTargetId() {
        assertEquals(CrossReferenceTargetKind.FIGURE, new CrossReference(CrossReferenceTargetKind.FIGURE, " fig ").kind());
        assertEquals("fig", new CrossReference(CrossReferenceTargetKind.FIGURE, " fig ").targetId());
        assertThrows(NullPointerException.class, () -> new CrossReference(null, "fig"));
        assertThrows(IllegalArgumentException.class, () -> new CrossReference(CrossReferenceTargetKind.FIGURE, " "));
    }

    @Test
    void resolvesFigureByStableIdAndDerivedDocumentNumber() {
        var document = document(figure("velocity", "Velocity graph"));

        var resolution = resolver.resolve(document, new CrossReference(CrossReferenceTargetKind.FIGURE, "velocity"));

        assertTrue(resolution.resolved());
        assertEquals("Figure 1", resolution.displayText());
        assertEquals("velocity", resolution.target().targetId());
    }

    @Test
    void figureNumberChangesWhenDocumentOrderChangesButTargetIdStaysStable() {
        var target = figure("velocity", "Velocity graph");
        var before = document(figure("setup", "Setup"), target);
        var after = document(target);

        assertEquals("Figure 2", resolver.resolve(before, ref(CrossReferenceTargetKind.FIGURE, "velocity")).displayText());
        assertEquals("Figure 1", resolver.resolve(after, ref(CrossReferenceTargetKind.FIGURE, "velocity")).displayText());
    }

    @Test
    void deletedTargetLeavesDeterministicBrokenReference() {
        var document = document(paragraph("See ", ref(CrossReferenceTargetKind.FIGURE, "missing"), "."));

        var resolution = resolver.resolve(document, ref(CrossReferenceTargetKind.FIGURE, "missing"));

        assertFalse(resolution.resolved());
        assertEquals("[Missing reference]", resolution.displayText());
    }

    @Test
    void resolvesTableEquationAndHeadingTargetsWhenTheyHaveIds() {
        var document = document(
                new Heading("motion", 1, inline("Motion")),
                new TableBlock("measurements", List.of(row(cell("Data"))), 1),
                new EquationBlock("velocity", new MathIdentifier("v")));

        assertEquals("Section 1", resolver.resolve(document, ref(CrossReferenceTargetKind.SECTION, "motion")).displayText());
        assertEquals("Table 1", resolver.resolve(document, ref(CrossReferenceTargetKind.TABLE, "measurements")).displayText());
        assertEquals("Equation 1", resolver.resolve(document, ref(CrossReferenceTargetKind.EQUATION, "velocity")).displayText());
    }

    @Test
    void resolvesNestedHeadingReferenceWithHierarchicalSectionLabel() {
        var document = document(
                new Heading("methods", 1, inline("Methods")),
                new Heading("setup", 2, inline("Setup")));

        assertEquals("Section 1.1", resolver.resolve(document, ref(CrossReferenceTargetKind.SECTION, "setup")).displayText());
    }

    @Test
    void sectionReferenceRenumbersWhenHeadingOrderChanges() {
        var target = new Heading("setup", 2, inline("Setup"));
        var before = document(new Heading("methods", 1, inline("Methods")), target);
        var after = document(new Heading("intro", 1, inline("Intro")), new Heading("methods", 1, inline("Methods")), target);

        assertEquals("Section 1.1", resolver.resolve(before, ref(CrossReferenceTargetKind.SECTION, "setup")).displayText());
        assertEquals("Section 2.1", resolver.resolve(after, ref(CrossReferenceTargetKind.SECTION, "setup")).displayText());
    }

    @Test
    void sectionReferenceUsesSkippedLevelPolicy() {
        var document = document(
                new Heading("methods", 1, inline("Methods")),
                new Heading("calibration", 3, inline("Calibration")));

        assertEquals("Section 1.0.1", resolver.resolve(document, ref(CrossReferenceTargetKind.SECTION, "calibration")).displayText());
    }

    private static CrossReference ref(CrossReferenceTargetKind kind, String id) {
        return new CrossReference(kind, id);
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static FigureBlock figure(String id, String caption) {
        return new FigureBlock(id, plot(), inline(caption));
    }

    private static PlotBlock plot() {
        return new PlotBlock(PlotDefinition.of(
                "Plot",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("Series", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(1, 1))))));
    }

    private static Paragraph paragraph(InlineNode... nodes) {
        return new Paragraph(new InlineContent(List.of(nodes)));
    }

    private static Paragraph paragraph(String before, InlineNode node, String after) {
        return paragraph(new Text(before, Set.of()), node, new Text(after, Set.of()));
    }

    private static InlineContent inline(String text) {
        return new InlineContent(List.of(new Text(text, Set.of())));
    }

    private static TableRow row(TableCell... cells) {
        return new TableRow(List.of(cells));
    }

    private static TableCell cell(String text) {
        return new TableCell(new TableCellContent(inline(text)));
    }
}
