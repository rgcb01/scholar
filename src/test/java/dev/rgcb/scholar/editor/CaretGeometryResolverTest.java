package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.ColumnLayout;
import dev.rgcb.scholar.document.DocumentSettings;
import dev.rgcb.scholar.document.DocumentTemplateId;
import dev.rgcb.scholar.document.PageOrientation;
import dev.rgcb.scholar.document.PageMargins;
import dev.rgcb.scholar.document.PaperSize;
import dev.rgcb.scholar.document.PhysicalLength;
import dev.rgcb.scholar.document.PageDecoration;
import dev.rgcb.scholar.document.VariableDefinition;
import dev.rgcb.scholar.document.ComputedResult;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.analysis.AnalysisKind;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.compute.ScientificValue;
import dev.rgcb.scholar.compute.ExpressionParser;
import dev.rgcb.scholar.quantity.Quantity;
import dev.rgcb.scholar.quantity.UnitParser;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.LaidOutText;
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.layout.LaidOutBlock;
import dev.rgcb.scholar.layout.LaidOutBlockKind;
import dev.rgcb.scholar.layout.LaidOutLine;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.layout.MathTextMeasurer;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CaretGeometryResolverTest {
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();
    private final CaretGeometryResolver resolver = new CaretGeometryResolver();

    @Test
    void layoutTextRunsCarrySourceRanges() {
        var layout = layoutEngine.layout(document("alpha beta gamma"), 60, textMeasurer);

        var ranges = layout.blocks().get(0).lines().stream()
                .flatMap(line -> line.textRuns().stream())
                .map(run -> List.of(run.sourceBlockIndex(), run.sourceStart(), run.sourceEnd()))
                .toList();

        assertTrue(ranges.contains(List.of(0, 0, 5)));
        assertTrue(ranges.stream().anyMatch(range -> range.get(0) == 0 && range.get(1) < range.get(2)));
    }

    @Test
    void resolvesCaretAtParagraphStartMiddleAndEnd() {
        var layout = layoutEngine.layout(document("alpha beta"), 80, textMeasurer);

        var start = resolver.resolve(new DocumentPosition(0, 0), layout, textMeasurer);
        var middle = resolver.resolve(new DocumentPosition(0, 3), layout, textMeasurer);
        var end = resolver.resolve(new DocumentPosition(0, 10), layout, textMeasurer);

        assertEquals(0, start.x());
        assertEquals(15, middle.x());
        assertEquals(50, end.x());
    }

    @Test
    void resolvesCaretAcrossWrappedLines() {
        var layout = layoutEngine.layout(document("alpha beta gamma"), 50, textMeasurer);

        var firstLineEnd = resolver.resolve(new DocumentPosition(0, 10), layout, textMeasurer);
        var secondLineMiddle = resolver.resolve(new DocumentPosition(0, 13), layout, textMeasurer);

        assertEquals(0, firstLineEnd.y());
        assertTrue(secondLineMiddle.y() > firstLineEnd.y());
    }

    @Test
    void relayoutAfterLongerTextChangesCaretGeometry() {
        var shortLayout = layoutEngine.layout(document("short"), 60, textMeasurer);
        var longLayout = layoutEngine.layout(document("short alpha beta gamma"), 60, textMeasurer);

        var shortCaret = resolver.resolve(new DocumentPosition(0, 5), shortLayout, textMeasurer);
        var longCaret = resolver.resolve(new DocumentPosition(0, 22), longLayout, textMeasurer);

        assertTrue(longCaret.y() >= shortCaret.y());
    }

    @Test
    void resolvesCaretAfterTrailingSpace() {
        var layout = layoutEngine.layout(document("abc "), 80, textMeasurer);

        var caret = resolver.resolve(new DocumentPosition(0, 4), layout, textMeasurer);

        assertEquals(20, caret.x());
    }

    @Test
    void resolvesCaretInEmptyParagraphFromEmptyInlineContent() {
        var layout = layoutEngine.layout(
                new Document(List.of(new Paragraph(new InlineContent(List.of())))),
                80,
                textMeasurer);

        var caret = resolver.resolve(new DocumentPosition(0, 0), layout, textMeasurer);

        assertEquals(0, caret.x());
        assertEquals(0, caret.y());
        assertEquals(10, caret.height());
    }

    @Test
    void resolvesCaretInEmptyHeadingUsingHeadingGeometry() {
        var layout = layoutEngine.layout(
                new Document(List.of(new Heading(2, new InlineContent(List.of())))),
                80,
                textMeasurer);

        var caret = resolver.resolve(new DocumentPosition(0, 0), layout, textMeasurer);

        assertEquals(20, caret.x());
        assertEquals(layout.blocks().get(0).y(), caret.y());
        assertEquals(10, caret.height());
    }

    @Test
    void resolvesCaretInsideHeading() {
        var layout = layoutEngine.layout(new Document(List.of(new Heading(2, new InlineContent(List.of((InlineNode) new Text("alpha beta", Set.of())))))), 80, textMeasurer);

        var start = resolver.resolve(new DocumentPosition(0, 0), layout, textMeasurer);
        var middle = resolver.resolve(new DocumentPosition(0, 5), layout, textMeasurer);
        var end = resolver.resolve(new DocumentPosition(0, 10), layout, textMeasurer);

        assertEquals(20, start.x());
        assertEquals(45, middle.x());
        assertEquals(70, end.x());
    }

    @Test void paginatedTextCoordinatesAreNotAddedTwice() {
        var settings = DocumentSettings.blank();
        var layout = layoutEngine.layoutPaginated(new Document(List.of(
                paragraph("first"), paragraph(""), paragraph("alpha")), List.of(), settings), textMeasurer, null);
        var origin = settings.margins().left().logicalUnits();
        assertEquals(origin, resolver.resolve(new DocumentPosition(1, 0), layout, textMeasurer).x());
        assertEquals(origin, resolver.resolve(new DocumentPosition(2, 0), layout, textMeasurer).x());
        assertEquals(origin + 10, resolver.resolve(new DocumentPosition(2, 2), layout, textMeasurer).x());
        assertEquals(origin + 25, resolver.resolve(new DocumentPosition(2, 5), layout, textMeasurer).x());
    }

    @Test void computedAndVariableBlocksCannotShiftFollowingParagraphCaret() {
        var variable = new VariableDefinition("mass-id", "m", new ScientificValue.Physical(
                new Quantity("2.5", new UnitParser().parseRequired("kg"))));
        var source = new Document(List.of(variable));
        var result = new ComputedResult(new ExpressionParser().parse("m*9.81 m/s²*2 m", source).expression(),
                "m*9.81 m/s²*2 m");
        var settings = DocumentSettings.blank();
        var layout = layoutEngine.layoutPaginated(new Document(List.of(variable, paragraph(""), result,
                paragraph("after")), List.of(), settings), textMeasurer, null);
        var origin = settings.margins().left().logicalUnits();
        assertEquals(origin, resolver.resolve(new DocumentPosition(1, 0), layout, textMeasurer).x());
        assertEquals(origin, resolver.resolve(new DocumentPosition(3, 0), layout, textMeasurer).x());
        assertEquals(origin + 25, resolver.resolve(new DocumentPosition(3, 5), layout, textMeasurer).x());
    }

    @Test void paragraphCaretUsesOwnPlacedLineEvenWhenPreviousBlockHasDifferentOrigin() {
        var previous = new LaidOutBlock(LaidOutBlockKind.PARAGRAPH, 0, 210, 0, 90, 20,
                List.of(new LaidOutLine(210, 0, 0, 10, List.of())));
        var line = new LaidOutLine(18, 30, 0, 10, List.of());
        var paragraph = new LaidOutBlock(LaidOutBlockKind.PARAGRAPH, 0, 18, 30, 100, 10, List.of(line));
        var laidOut = new LaidOutDocument(320, 40, List.of(previous, paragraph));
        assertEquals(18, resolver.resolve(new DocumentPosition(1, 0), laidOut, textMeasurer).x());
    }

    @Test void twoColumnCaretUsesItsColumnOriginAndNotPreviousBlockWidth() {
        var settings = new DocumentSettings(DocumentTemplateId.BLANK,
                PaperSize.custom(PhysicalLength.millimetres(80), PhysicalLength.millimetres(60)),
                PageOrientation.PORTRAIT,
                new PageMargins(PhysicalLength.millimetres(5), PhysicalLength.millimetres(5),
                        PhysicalLength.millimetres(5), PhysicalLength.millimetres(5)),
                ColumnLayout.two(), PageDecoration.none());
        var blocks = new java.util.ArrayList<dev.rgcb.scholar.document.BlockNode>();
        for (var i = 0; i < 20; i++) blocks.add(paragraph("x"));
        var layout = layoutEngine.layoutPaginated(new Document(blocks, List.of(), settings), textMeasurer, null);
        var origins = layout.blocks().stream().map(block -> block.lines().getFirst().x()).distinct().toList();
        assertTrue(origins.size() >= 2);
        for (var i = 0; i < blocks.size(); i++) {
            assertEquals(layout.blocks().get(i).lines().getFirst().x(),
                    resolver.resolve(new DocumentPosition(i, 0), layout, textMeasurer).x());
        }
    }

    @Test void actualEquationAndTableLayoutDoNotShiftFollowingParagraph() {
        var blocks = List.<dev.rgcb.scholar.document.BlockNode>of(
                new EquationBlock(new MathIdentifier("energy")), paragraph(""),
                new TableBlock(List.of(new TableRow(List.of(TableCell.empty()))), 0), paragraph("next"));
        var settings = DocumentSettings.blank();
        MathTextMeasurer math = (content, kind) -> new MathTextMetrics(content.length() * 5, 7, 3);
        var layout = layoutEngine.layoutPaginated(new Document(blocks, List.of(), settings), textMeasurer, math);
        var origin = settings.margins().left().logicalUnits();
        assertEquals(origin, resolver.resolve(new DocumentPosition(1, 0), layout, textMeasurer).x());
        assertEquals(origin, resolver.resolve(new DocumentPosition(3, 0), layout, textMeasurer).x());
        assertEquals(origin + 20, resolver.resolve(new DocumentPosition(3, 4), layout, textMeasurer).x());
    }

    @Test void datasetAnalysisDoesNotShiftFollowingParagraphCaret() {
        var dataset = new ScientificDataset("sample", "Sample", List.of(
                new DatasetColumn("value", "Value", DatasetColumnType.NUMBER)),
                List.of(new DatasetRow(List.of(DatasetValue.number("2")))));
        var analysis = new DatasetAnalysisBlock("analysis", dataset.id(), AnalysisKind.DESCRIPTIVE,
                Optional.empty(), "value", Optional.empty(), NumberNotation.DECIMAL);
        var settings = DocumentSettings.blank();
        var layout = layoutEngine.layoutPaginated(new Document(List.of(analysis, paragraph("after")),
                List.of(dataset), settings), textMeasurer, null);
        var origin = settings.margins().left().logicalUnits();
        assertEquals(origin, resolver.resolve(new DocumentPosition(1, 0), layout, textMeasurer).x());
        assertEquals(origin + 25, resolver.resolve(new DocumentPosition(1, 5), layout, textMeasurer).x());
    }

    @Test void figurePlotAndDiagramLayoutDoNotShiftFollowingParagraph() {
        var settings = DocumentSettings.blank();
        for (var atomic : List.<dev.rgcb.scholar.document.BlockNode>of(
                EditorFoundationFixture.plot("Sample"),
                EditorFoundationFixture.genericDiagram(),
                new FigureBlock("figure-id", EditorFoundationFixture.plot("Figure"),
                        new InlineContent(List.of())))) {
            var layout = layoutEngine.layoutPaginated(new Document(List.of(atomic, paragraph("tail")),
                    List.of(), settings), textMeasurer, null);
            assertEquals(settings.margins().left().logicalUnits(),
                    resolver.resolve(new DocumentPosition(1, 0), layout, textMeasurer).x(),
                    atomic.getClass().getSimpleName());
        }
    }

    @Test void enterAndHistoryRestorePlacedCaretGeometry() {
        var settings = DocumentSettings.blank();
        var session = new EditorSession(new Document(List.of(paragraph("")), List.of(), settings), 0);
        assertTrue(session.typeText("abc"));
        assertTrue(session.enter());
        assertEquals(settings.margins().left().logicalUnits(), sessionCaretX(session));
        assertTrue(session.typeText("xy"));
        assertEquals(settings.margins().left().logicalUnits() + 10, sessionCaretX(session));
        assertTrue(session.undo());
        assertEquals(settings.margins().left().logicalUnits(), sessionCaretX(session));
        assertTrue(session.redo());
        assertEquals(settings.margins().left().logicalUnits() + 10, sessionCaretX(session));
    }

    private int sessionCaretX(EditorSession session) {
        var layout = layoutEngine.layoutPaginated(session.current().document(), textMeasurer, null);
        return resolver.resolve(session.current().caret(), layout, textMeasurer).x();
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of(new Text(text, Set.of()))));
    }

    private static Document document(String text) {
        return new Document(List.of(new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))))));
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            return TextBoundary.characterCount(text) * 5;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return 10;
        }
    }
}
