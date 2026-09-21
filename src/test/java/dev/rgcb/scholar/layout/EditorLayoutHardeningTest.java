package dev.rgcb.scholar.layout;

import dev.rgcb.scholar.client.DevelopmentStressDocument;
import dev.rgcb.scholar.client.DevelopmentStressDocument.Profile;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.editor.TextBoundary;
import dev.rgcb.scholar.editor.DocumentPosition;
import dev.rgcb.scholar.editor.DocumentRange;
import dev.rgcb.scholar.editor.DocumentHitTester;
import dev.rgcb.scholar.editor.CaretGeometryResolver;
import dev.rgcb.scholar.editor.SelectionGeometryResolver;
import dev.rgcb.scholar.data.*;
import dev.rgcb.scholar.plot.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class EditorLayoutHardeningTest {
    private final DocumentLayoutEngine engine = new DocumentLayoutEngine();

    @Test
    void tableUnicodeWrappingPreservesLogicalSourceBoundaries() {
        var value = "\uD835\uDC65e\u0301\uD835\uDC66 y";
        var table = new TableBlock(List.of(new TableRow(List.of(new TableCell(
                new TableCellContent(new InlineContent(List.of(new Text(value, Set.of())))))))), 0);
        var result = new TableLayoutEngine().layout(table, 0, 0, 0, 18, EditorStressMeasurementTest.TEXT);
        var runs = result.rows().getFirst().cells().getFirst().lines().stream().flatMap(l -> l.textRuns().stream()).toList();
        for (var run : runs) {
            assertEquals(run.text(), TextBoundary.substring(value, run.sourceStart(), run.sourceEnd()));
        }
    }

    @Test
    void longMarkedUnicodeParagraphAndBoundedNestedMathHaveCompleteFiniteLayout() {
        var nodes = new java.util.ArrayList<InlineNode>();
        for (var i = 0; i < 800; i++) {
            nodes.add(new Text("e\u0301\uD835\uDC65", i % 2 == 0 ? Set.of(TextMark.BOLD) : Set.of(TextMark.ITALIC)));
        }
        dev.rgcb.scholar.math.MathExpression expression = new dev.rgcb.scholar.math.MathIdentifier("x".repeat(128));
        for (var i = 0; i < 12; i++) {
            expression = new dev.rgcb.scholar.math.MathRoot(new dev.rgcb.scholar.math.MathSequence(List.of(expression)), java.util.Optional.empty());
        }
        var document = new Document(List.of(new Paragraph(new InlineContent(nodes)), new EquationBlock(expression)));
        var layout = engine.layout(document, 24, EditorStressMeasurementTest.TEXT, EditorStressMeasurementTest.MATH);
        assertEquals(2, layout.blocks().size());
        assertTrue(layout.blocks().get(1).y() >= layout.blocks().getFirst().height());
        assertTrue(layout.height() > 0);
        assertEquals(1600, layout.blocks().getFirst().lines().stream().flatMap(l -> l.textRuns().stream())
                .mapToInt(r -> r.sourceEnd() - r.sourceStart()).sum());
    }

    @Test
    void viewportCullingIncludesEdgesAndUsesOverflowSafeBounds() {
        var block = new LaidOutBlock(LaidOutBlockKind.PARAGRAPH, 0, 0, 100, 40, 20, List.of());
        assertFalse(block.intersectsVerticalViewport(0, 99));
        assertTrue(block.intersectsVerticalViewport(0, 100));
        assertTrue(block.intersectsVerticalViewport(120, 10));
        assertFalse(block.intersectsVerticalViewport(121, 10));
        assertFalse(block.intersectsVerticalViewport(100, 0));
        var distant = new LaidOutBlock(LaidOutBlockKind.PARAGRAPH, 0, 0, Integer.MAX_VALUE - 10, 40, 20, List.of());
        assertTrue(distant.intersectsVerticalViewport(Integer.MAX_VALUE - 5, 10));
    }

    @Test
    void atomicReferenceHitCaretAndSelectionUseOnlySemanticEndpoints() {
        var document = new Document(List.of(new Paragraph(new InlineContent(List.of(
                new CrossReference(CrossReferenceTargetKind.SECTION, "s")))),
                new Heading("s", 1, new InlineContent(List.of(new Text("Section", Set.of()))))));
        for (var width : List.of(480, 18)) {
            var layout = engine.layout(document, width, EditorStressMeasurementTest.TEXT);
            var runs = layout.blocks().getFirst().lines().stream().flatMap(line -> line.textRuns().stream()).toList();
            var hitTester = new DocumentHitTester();
            for (var run : runs) {
                assertTrue(run.atomic());
                for (var x = run.x(); x <= run.x() + run.width(); x++) {
                    var hit = hitTester.hitTestInBlock(layout, 0, x, run.y() + 1, EditorStressMeasurementTest.TEXT).orElseThrow();
                    assertTrue(hit.characterOffset() == 0 || hit.characterOffset() == 1);
                }
            }
            var caret = new CaretGeometryResolver().resolve(new DocumentPosition(0, 1), layout, EditorStressMeasurementTest.TEXT);
            assertEquals(runs.getLast().x() + runs.getLast().width(), caret.x());
            assertEquals(runs.getLast().y(), caret.y());
            var rects = new SelectionGeometryResolver().resolve(new DocumentRange(new DocumentPosition(0, 0), new DocumentPosition(0, 1)), layout, EditorStressMeasurementTest.TEXT);
            assertEquals(runs.stream().mapToInt(LaidOutText::width).sum(), rects.stream().mapToInt(rect -> rect.width()).sum());
        }
    }

    @Test
    void unicodeWrappingNeverSplitsLogicalCharactersOrCorruptsSourceOffsets() {
        var value = "\uD835\uDC65e\u0301\uD835\uDC66";
        var document = new Document(List.of(new Paragraph(new InlineContent(List.of(new Text(value, Set.of()))))));
        var layout = engine.layout(document, 6, EditorStressMeasurementTest.TEXT);
        var runs = layout.blocks().getFirst().lines().stream().flatMap(line -> line.textRuns().stream()).toList();
        assertEquals(List.of("\uD835\uDC65", "e\u0301", "\uD835\uDC66"), runs.stream().map(LaidOutText::text).toList());
        for (var run : runs) {
            assertEquals(1, run.sourceEnd() - run.sourceStart());
            assertEquals(run.text(), TextBoundary.substring(value, run.sourceStart(), run.sourceEnd()));
        }
    }

    @Test
    void unicodeTokensAfterWhitespaceUseLogicalSourceOffsets() {
        var value = "\uD835\uDC65e\u0301 y";
        var document = new Document(List.of(new Paragraph(new InlineContent(List.of(new Text(value, Set.of()))))));
        var layout = engine.layout(document, 480, EditorStressMeasurementTest.TEXT);
        for (var run : layout.blocks().getFirst().lines().getFirst().textRuns()) {
            assertEquals(run.text(), TextBoundary.substring(value, run.sourceStart(), run.sourceEnd()));
        }
    }

    @Test
    void veryNarrowTocHasNonnegativeUsableEntryGeometry() {
        var document = new Document(List.of(new TableOfContentsBlock(),
                new Heading("deep", 6, new InlineContent(List.of(new Text("Deep", Set.of()))))));
        var layout = engine.layout(document, 1, EditorStressMeasurementTest.TEXT);
        var entry = layout.blocks().getFirst().tableOfContents().orElseThrow().entries().getFirst();
        assertTrue(entry.x() >= 0 && entry.x() < layout.width());
        assertTrue(entry.width() > 0);
    }

    @Test
    void deepTocIndentRemainsCompactAtReadableWidth() {
        var document = new Document(List.of(new TableOfContentsBlock(),
                new Heading("deep", 6, new InlineContent(List.of(new Text("Deep", Set.of()))))));
        var layout = engine.layout(document, 360, EditorStressMeasurementTest.TEXT);
        var entry = layout.blocks().getFirst().tableOfContents().orElseThrow().entries().getFirst();

        assertEquals(40, entry.x());
        assertEquals(320, entry.width());
    }

    @Test
    void figurePlotUsesDatasetResolutionWithoutChangingSemanticContent() {
        var dataset = new ScientificDataset("d", "Measurements",
                List.of(new DatasetColumn("x", "x", DatasetColumnType.NUMBER),
                        new DatasetColumn("y", "y", DatasetColumnType.NUMBER)),
                List.of(new DatasetRow(List.of(DatasetValue.number("1"), DatasetValue.number("2")))));
        var plot = new PlotBlock(PlotDefinition.of("Bound", AxisDefinition.linear("x"), AxisDefinition.linear("y"),
                List.of(new PlotSeries("s", PlotSeriesKind.SCATTER, new DatasetPlotBinding("d", "x", "y")))));
        var figure = new FigureBlock("f", plot, new InlineContent(List.of()));
        var document = new Document(List.of(figure), List.of(dataset));
        var result = engine.layout(document, 480, EditorStressMeasurementTest.TEXT);
        assertEquals(1, result.blocks().getFirst().plot().orElseThrow().series().getFirst().points().size());
        assertTrue(plot.definition().series().getFirst().points().isEmpty());
        assertSame(figure, document.blocks().getFirst());
        var enormous = dataset.withCell(0, "y", DatasetValue.number("1e400"));
        var degraded = new Document(List.of(figure), List.of(enormous));
        var validation = dev.rgcb.scholar.validation.DocumentValidator.validate(degraded);
        assertTrue(validation.isValid());
        assertTrue(validation.warnings().stream().anyMatch(d -> d.code() == dev.rgcb.scholar.validation.DocumentDiagnosticCode.UNREPRESENTABLE_PLOT_VALUE));
        var degradedLayout = engine.layout(degraded, 480, EditorStressMeasurementTest.TEXT);
        assertTrue(degradedLayout.blocks().getFirst().plot().orElseThrow().series().getFirst().points().isEmpty());
        assertEquals(DatasetValue.number("1e400"), enormous.rows().getFirst().values().get(1));
    }

    @ParameterizedTest
    @EnumSource(Profile.class)
    void mixedFixtureReflowIsDeterministicOrderedAndComplete(Profile profile) {
        var document = DevelopmentStressDocument.create(profile);
        var initial = engine.layout(document, 480, EditorStressMeasurementTest.TEXT, EditorStressMeasurementTest.MATH);
        for (var width : List.of(180, 24, 480)) {
            var layout = engine.layout(document, width, EditorStressMeasurementTest.TEXT, EditorStressMeasurementTest.MATH);
            assertEquals(document.blocks().size(), layout.blocks().size());
            var bottom = 0;
            for (var block : layout.blocks()) {
                assertTrue(block.y() >= bottom);
                assertTrue(block.width() >= 0 && block.height() > 0);
                bottom = block.y() + block.height();
            }
            for (var i = 0; i < layout.blocks().size(); i++) {
                var block = layout.blocks().get(i);
                if (block.kind() != LaidOutBlockKind.PARAGRAPH && block.kind() != LaidOutBlockKind.HEADING) {
                    assertEquals(dev.rgcb.scholar.editor.DocumentHit.block(i), new DocumentHitTester().hit(layout,
                            block.x() + block.width() / 2, block.y() + block.height() / 2, EditorStressMeasurementTest.TEXT));
                }
            }
            assertEquals(bottom, layout.height());
            if (width == 480) { assertEquals(initial, layout); }
        }
    }
}
