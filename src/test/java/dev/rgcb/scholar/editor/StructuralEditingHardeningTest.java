package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetPlotBinding;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetTableBinding;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.editor.MathCaretSelection;
import dev.rgcb.scholar.math.editor.MathPath;
import dev.rgcb.scholar.math.editor.MathSequencePosition;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StructuralEditingHardeningTest {
    private final EditorSelectionValidator selectionValidator = new EditorSelectionValidator();

    @Test
    void deletingAtomicBlocksChoosesDeterministicValidSelection() {
        var middle = new EditorSession(document(paragraph("A"), equation("eq", "x"), paragraph("B")), 0);
        middle.setCurrent(new EditorState(middle.current().document(), new BlockSelection(1), Optional.empty()));
        assertTrue(middle.deleteForward());
        assertValid(middle);
        assertTextCaret(middle, 0, 1);

        var first = new EditorSession(document(equation("eq", "x"), paragraph("B")), 1);
        first.setCurrent(new EditorState(first.current().document(), new BlockSelection(0), Optional.empty()));
        assertTrue(first.deleteBackward());
        assertValid(first);
        assertTextCaret(first, 0, 0);

        var last = new EditorSession(document(paragraph("A"), equation("eq", "x")), 0);
        last.setCurrent(new EditorState(last.current().document(), new BlockSelection(1), Optional.empty()));
        assertTrue(last.deleteForward());
        assertValid(last);
        assertTextCaret(last, 0, 1);

        var only = sessionAt(document(equation("eq", "x")), new BlockSelection(0));
        assertTrue(only.deleteBackward());
        assertValid(only);
        assertEquals(1, only.current().document().blocks().size());
        assertInstanceOf(Paragraph.class, only.current().document().blocks().get(0));
        assertTextCaret(only, 0, 0);
    }

    @Test
    void paragraphSplitPreservesMarksAndCrossReferenceBoundaries() {
        var document = document(new Paragraph(new InlineContent(List.of(
                new Text("He", Set.of(TextMark.BOLD)),
                new Text("llo", Set.of(TextMark.ITALIC)),
                new Text("World", Set.of())))));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 5)));

        assertTrue(session.enter());
        assertValid(session);

        assertEquals(List.of(Paragraph.class, Paragraph.class), blockTypes(session));
        assertEquals("Hello", textOf(session.current().document().blocks().get(0)));
        assertEquals("World", textOf(session.current().document().blocks().get(1)));
        assertTextCaret(session, 1, 0);
    }

    @Test
    void headingEnterSemanticsDoNotDuplicateStableIds() {
        var start = new EditorSession(document(heading("motion", 2, "Motion")), 0);
        start.setCurrent(new EditorState(start.current().document(), new DocumentPosition(0, 0)));
        assertTrue(start.enter());
        assertValid(start);
        assertInstanceOf(Paragraph.class, start.current().document().blocks().get(0));
        assertEquals(Optional.of("motion"), ((Heading) start.current().document().blocks().get(1)).id());

        var middle = new EditorSession(document(heading("motion", 2, "Motion")), 0);
        middle.setCurrent(new EditorState(middle.current().document(), new DocumentPosition(0, 2)));
        assertTrue(middle.enter());
        assertValid(middle);
        var left = (Heading) middle.current().document().blocks().get(0);
        var right = (Heading) middle.current().document().blocks().get(1);
        assertEquals(Optional.of("motion"), left.id());
        assertTrue(right.id().isEmpty());

        var end = new EditorSession(document(heading("motion", 2, "Motion")), 0);
        end.setCurrent(new EditorState(end.current().document(), new DocumentPosition(0, 6)));
        assertTrue(end.enter());
        assertValid(end);
        assertEquals(Optional.of("motion"), ((Heading) end.current().document().blocks().get(0)).id());
        assertInstanceOf(Paragraph.class, end.current().document().blocks().get(1));

        var empty = new EditorSession(document(heading("motion", 2, "")), 0);
        empty.setCurrent(new EditorState(empty.current().document(), new DocumentPosition(0, 0)));
        assertTrue(empty.enter());
        assertValid(empty);
        assertInstanceOf(Paragraph.class, empty.current().document().blocks().get(0));
    }

    @Test
    void textMergingKeepsLeftBlockIdentityAndStopsAtAtomicBoundary() {
        var paragraphs = new EditorSession(document(paragraph("A"), paragraph("B")), 0);
        paragraphs.setCurrent(new EditorState(paragraphs.current().document(), new DocumentPosition(1, 0)));
        assertTrue(paragraphs.deleteBackward());
        assertValid(paragraphs);
        assertEquals(1, paragraphs.current().document().blocks().size());
        assertEquals("AB", textOf(paragraphs.current().document().blocks().get(0)));

        var headings = new EditorSession(document(heading("left", 2, "A"), heading("right", 3, "B")), 0);
        headings.setCurrent(new EditorState(headings.current().document(), new DocumentPosition(1, 0)));
        assertTrue(headings.deleteBackward());
        assertValid(headings);
        var merged = (Heading) headings.current().document().blocks().get(0);
        assertEquals(2, merged.level());
        assertEquals(Optional.of("left"), merged.id());
        assertEquals("AB", textOf(merged));

        var boundary = new EditorSession(document(paragraph("A"), equation("eq", "x"), paragraph("B")), 0);
        boundary.setCurrent(new EditorState(boundary.current().document(), new DocumentPosition(2, 0)));
        assertFalse(boundary.deleteBackward());
        assertValid(boundary);
        assertEquals(new BlockSelection(1), boundary.current().selection());
        assertTrue(boundary.deleteForward());
        assertValid(boundary);
        assertEquals(List.of(Paragraph.class, Paragraph.class), blockTypes(boundary));
    }

    @Test
    void figureWrapAndUnwrapPreserveContainedBlockAndStableSelection() {
        var session = new EditorSession(document(paragraph("A"), plotBlock(), diagramBlock(), paragraph("B")), 0);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(1), Optional.empty()));

        assertTrue(session.wrapSelectedPlotInFigure());
        assertValid(session);
        var figure = assertInstanceOf(FigureBlock.class, session.current().document().blocks().get(1));
        assertInstanceOf(PlotBlock.class, figure.content());
        assertEquals(new BlockSelection(1), session.current().selection());

        assertTrue(session.unwrapFigure());
        assertValid(session);
        assertInstanceOf(PlotBlock.class, session.current().document().blocks().get(1));
        assertEquals(new BlockSelection(1), session.current().selection());

        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(2), Optional.empty()));
        assertTrue(session.wrapSelectedDiagramInFigure());
        assertValid(session);
        assertTrue(session.unwrapFigure());
        assertValid(session);
        assertInstanceOf(DiagramBlock.class, session.current().document().blocks().get(2));
    }

    @Test
    void tableStructuralCommandsPreserveShapeAndDatasetBackedViewsAreNotReshaped() {
        var session = sessionAt(document(TableBlock.empty(2, 2)),
                new TableEditingSelection(0, TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 0)),
                Optional.empty());

        assertTrue(session.insertTableRowBelow());
        assertValid(session);
        assertEquals(3, table(session, 0).rows().size());
        assertTrue(session.insertTableColumnRight());
        assertValid(session);
        assertEquals(3, table(session, 0).columnCount());
        assertTrue(session.deleteTableRow());
        assertValid(session);
        assertTrue(session.deleteTableColumn());
        assertValid(session);
        assertEquals(2, table(session, 0).rows().size());
        assertEquals(2, table(session, 0).columnCount());

        var datasetDocument = new Document(List.of(new TableBlock(new DatasetTableBinding("data"))), List.of(dataset("data")));
        var backed = sessionAt(datasetDocument,
                new TableEditingSelection(0, TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 0)),
                Optional.empty());
        assertFalse(backed.supportsInsertTableRow());
        assertFalse(backed.supportsDeleteTableColumn());
        assertFalse(backed.insertTableRowBelow());
        assertValid(backed);
        assertEquals(1, backed.current().document().datasets().size());
    }

    @Test
    void plotAndDiagramStructuralEditsPreserveDocumentValidity() {
        var plotSession = sessionAt(document(plotBlock()),
                new PlotEditingSelection(0, new PlotSeriesTarget(0)),
                Optional.empty());
        assertTrue(plotSession.addPlotPoint());
        assertValid(plotSession);
        assertTrue(plotSession.deletePlotPoint());
        assertValid(plotSession);
        assertTrue(plotSession.addPlotSeries(PlotSeriesKind.SCATTER));
        assertValid(plotSession);
        assertTrue(plotSession.deletePlotSeries());
        assertValid(plotSession);

        var diagramSession = sessionAt(document(diagramBlock()),
                new DiagramEditingSelection(0, new DiagramPropertyTarget(DiagramProperty.CANVAS)),
                Optional.empty());
        assertTrue(diagramSession.addDiagramNode());
        assertValid(diagramSession);
        var target = diagramSession.current().diagramEditingSelection().target();
        assertInstanceOf(DiagramElementTarget.class, target);
        assertTrue(diagramSession.deleteDiagramNode());
        assertValid(diagramSession);
    }

    @Test
    void structuralCutAndPasteKeepDocumentAndSelectionValidAndRemapIds() {
        var session = new EditorSession(document(
                heading("motion", 2, "Motion"),
                equation("eq-1", "x"),
                table("table-1"),
                new TableOfContentsBlock(),
                paragraph("End")), 4);
        var clipboard = new FakeClipboard();
        var context = new EditorActionContext(session, clipboard);

        copyPasteBlock(session, context, 0);
        assertValid(session);
        assertEquals("motion-2", ((Heading) session.current().document().blocks().get(1)).id().orElseThrow());

        copyPasteBlock(session, context, 2);
        assertValid(session);
        assertEquals("eq-1-2", ((EquationBlock) session.current().document().blocks().get(3)).id().orElseThrow());

        copyPasteTableIntoTextCaret(session, context, 4);
        assertValid(session);
        assertEquals("table-1-2", ((TableBlock) session.current().document().blocks().get(6)).id().orElseThrow());

        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(5), Optional.empty()));
        assertTrue(BuiltInEditorActions.cut().execute(context).documentChanged());
        assertValid(session);
        assertTrue(session.current().document().blocks().stream().noneMatch(TableOfContentsBlock.class::isInstance));
    }

    @Test
    void deletingDatasetBackedViewsDoesNotDeleteDatasetResource() {
        var dataset = dataset("data");
        var plot = new PlotBlock(new PlotDefinition(
                "Dataset Plot",
                AxisDefinition.linear("time"),
                AxisDefinition.linear("height"),
                List.of(new PlotSeries("data", PlotSeriesKind.LINE, new DatasetPlotBinding("data", "time", "height"))),
                true,
                true,
                PlotDefinition.DEFAULT_HEIGHT));
        var document = new Document(List.of(new TableBlock(new DatasetTableBinding("data")), plot, paragraph("End")), List.of(dataset));
        var session = new EditorSession(document, 2);

        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(0), Optional.empty()));
        assertTrue(session.deleteForward());
        assertValid(session);
        assertEquals(1, session.current().document().datasets().size());

        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(0), Optional.empty()));
        assertTrue(session.deleteForward());
        assertValid(session);
        assertEquals(1, session.current().document().datasets().size());
    }

    @Test
    void contextMenuDeleteBlockUsesSharedDeleteActionPath() {
        var session = new EditorSession(document(paragraph("A"), equation("eq", "x"), paragraph("B")), 0);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(1), Optional.empty()));
        var actions = BuiltInEditorActions.editMenuActions().stream()
                .collect(java.util.stream.Collectors.toMap(EditorAction::id, action -> action));
        var entries = new EditorContextActionResolver().resolve(session.current(), actions);
        var delete = entries.stream()
                .filter(entry -> entry.kind() == ContextMenuEntryKind.ACTION)
                .map(entry -> entry.action().orElseThrow())
                .filter(action -> action.id() == EditorActionId.DELETE)
                .findFirst()
                .orElseThrow();

        var result = delete.execute(new EditorActionContext(session, new FakeClipboard()));

        assertTrue(result.documentChanged());
        assertValid(session);
        assertEquals(List.of(Paragraph.class, Paragraph.class), blockTypes(session));
    }

    @Test
    void goldenStructuralSequenceKeepsValidStateAfterEveryStep() {
        var session = new EditorSession(document(
                paragraph("AlphaBeta"),
                heading("motion", 2, "Motion"),
                equation("eq", "x"),
                TableBlock.empty(2, 2),
                plotBlock(),
                diagramBlock(),
                figureBlock(),
                new TableOfContentsBlock(),
                paragraph("Omega")), 0);
        assertValid(session);

        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 5)));
        assertTrue(session.enter());
        assertValid(session);

        assertTrue(session.deleteBackward());
        assertValid(session);

        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(2), Optional.empty()));
        assertTrue(session.deleteForward());
        assertValid(session);

        var equationPayload = new dev.rgcb.scholar.clipboard.DocumentBlockClipboardPayload(equation("eq", "x"));
        assertTrue(session.pasteFromClipboard(Optional.of(equationPayload), "x"));
        assertValid(session);

        var plotIndex = indexOf(session.current().document(), PlotBlock.class);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(plotIndex), Optional.empty()));
        assertTrue(session.wrapSelectedPlotInFigure());
        assertValid(session);
        assertTrue(session.unwrapFigure());
        assertValid(session);

        var tableIndex = indexOf(session.current().document(), TableBlock.class);
        session.setCurrent(new EditorState(session.current().document(),
                new TableEditingSelection(tableIndex, TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 0)),
                Optional.empty()));
        assertTrue(session.insertTableRowBelow());
        assertValid(session);
        assertTrue(session.deleteTableRow());
        assertValid(session);

        var diagramIndex = indexOf(session.current().document(), DiagramBlock.class);
        session.setCurrent(new EditorState(session.current().document(),
                new DiagramEditingSelection(diagramIndex, new DiagramPropertyTarget(DiagramProperty.CANVAS)),
                Optional.empty()));
        assertTrue(session.addDiagramNode());
        assertValid(session);
        assertTrue(session.deleteDiagramNode());
        assertValid(session);

        var tocIndex = indexOf(session.current().document(), TableOfContentsBlock.class);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(tocIndex), Optional.empty()));
        assertTrue(session.deleteForward());
        assertValid(session);
        assertTrue(session.insertTableOfContents());
        assertValid(session);

        var last = session.current().document().blocks().size() - 1;
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(last, textOf(session.current().document().blocks().get(last)).length())));
        assertTrue(session.deleteBackward());
        assertValid(session);

        assertTrue(session.insertDefaultTable());
        assertValid(session);
        assertTrue(session.current().document().blocks().stream().anyMatch(EquationBlock.class::isInstance));
    }

    @Test
    void seededStructuralOperationsRemainValid() {
        var random = new Random(2404);
        var session = new EditorSession(document(paragraph("seed"), paragraph("tail")), 0);
        for (var step = 0; step < 80; step++) {
            switch (random.nextInt(6)) {
                case 0 -> {
                    if (session.supportsInsertEquation()) {
                        session.insertEmptyEquation();
                    }
                }
                case 1 -> {
                    if (session.current().isBlockSelection()) {
                        session.deleteForward();
                    }
                }
                case 2 -> {
                    var paragraphIndex = firstEditableBlock(session.current().document());
                    session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(paragraphIndex, 0)));
                    session.typeText("x");
                    session.enter();
                }
                case 3 -> {
                    var plotIndex = firstBlock(session.current().document(), PlotBlock.class);
                    if (plotIndex >= 0) {
                        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(plotIndex), Optional.empty()));
                        if (session.supportsWrapSelectedPlotInFigure()) {
                            session.wrapSelectedPlotInFigure();
                        }
                    } else if (session.supportsInsertPlot()) {
                        session.insertDefaultPlot();
                    }
                }
                case 4 -> {
                    var figureIndex = firstBlock(session.current().document(), FigureBlock.class);
                    if (figureIndex >= 0) {
                        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(figureIndex), Optional.empty()));
                        session.unwrapFigure();
                    }
                }
                case 5 -> {
                    if (session.supportsInsertTable()) {
                        session.insertDefaultTable();
                    }
                }
                default -> throw new IllegalStateException("Unexpected random branch.");
            }
            assertValid(session);
        }
    }

    private void copyPasteBlock(EditorSession session, EditorActionContext context, int blockIndex) {
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(blockIndex), Optional.empty()));
        assertFalse(BuiltInEditorActions.copy().execute(context).documentChanged());
        assertTrue(BuiltInEditorActions.paste().execute(context).documentChanged(),
                () -> "paste failed for block " + blockIndex + " / " + session.current().document().blocks().get(blockIndex).getClass().getSimpleName());
    }

    private void copyPasteTableIntoTextCaret(EditorSession session, EditorActionContext context, int blockIndex) {
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(blockIndex), Optional.empty()));
        assertFalse(BuiltInEditorActions.copy().execute(context).documentChanged());
        var last = session.current().document().blocks().size() - 1;
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(last, 0)));
        assertTrue(BuiltInEditorActions.paste().execute(context).documentChanged());
    }

    private static EditorSession sessionAt(Document document, EditorSelection selection) {
        return sessionAt(document, selection, Optional.empty());
    }

    private static EditorSession sessionAt(Document document, EditorSelection selection, Optional<Set<TextMark>> explicitTypingMarks) {
        var session = new EditorSession(document(paragraph("")), 0);
        session.setCurrent(new EditorState(document, selection, explicitTypingMarks));
        return session;
    }

    private void assertValid(EditorSession session) {
        var result = DocumentValidator.validate(session.current().document());
        assertTrue(result.errors().isEmpty(), () -> result.errors().toString());
        selectionValidator.validate(session.current().document(), session.current().selection());
    }

    private static void assertTextCaret(EditorSession session, int blockIndex, int offset) {
        assertEquals(new TextSelection(new DocumentPosition(blockIndex, offset), new DocumentPosition(blockIndex, offset)),
                session.current().selection());
    }

    private static int indexOf(Document document, Class<? extends BlockNode> type) {
        var index = firstBlock(document, type);
        if (index < 0) {
            throw new AssertionError("Missing block type " + type.getSimpleName());
        }
        return index;
    }

    private static int firstBlock(Document document, Class<? extends BlockNode> type) {
        for (var index = 0; index < document.blocks().size(); index++) {
            if (type.isInstance(document.blocks().get(index))) {
                return index;
            }
        }
        return -1;
    }

    private static int firstEditableBlock(Document document) {
        for (var index = 0; index < document.blocks().size(); index++) {
            if (document.blocks().get(index) instanceof Paragraph || document.blocks().get(index) instanceof Heading) {
                return index;
            }
        }
        return 0;
    }

    private static List<Class<? extends BlockNode>> blockTypes(EditorSession session) {
        return session.current().document().blocks().stream()
                .map(BlockNode::getClass)
                .toList();
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(inline(text));
    }

    private static Heading heading(String id, int level, String text) {
        return new Heading(id, level, inline(text));
    }

    private static InlineContent inline(String text) {
        return new InlineContent(List.of((InlineNode) new Text(text, Set.of())));
    }

    private static String textOf(BlockNode block) {
        return InlineContentEditor.logicalText(EditableInlineBlock.contentOf(block));
    }

    private static EquationBlock equation(String id, String identifier) {
        return new EquationBlock(id, new MathSequence(List.of(new MathIdentifier(identifier))));
    }

    private static TableBlock table(String id) {
        return new TableBlock(id, List.of(
                new TableRow(List.of(cell("A"), cell("B"))),
                new TableRow(List.of(cell("1"), cell("2")))), 1);
    }

    private static TableCell cell(String text) {
        return new TableCell(new TableCellContent(inline(text)));
    }

    private static PlotBlock plotBlock() {
        return new PlotBlock(PlotDefinition.of(
                "Plot",
                AxisDefinition.linear("t"),
                AxisDefinition.linear("x"),
                List.of(new PlotSeries("series", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(1, 1))))));
    }

    private static DiagramBlock diagramBlock() {
        return new DocumentEditor().insertDefaultDiagram(new EditorState(document(paragraph("")), new DocumentPosition(0, 0)))
                .editorState()
                .document()
                .blocks()
                .stream()
                .filter(DiagramBlock.class::isInstance)
                .map(DiagramBlock.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static FigureBlock figureBlock() {
        return FigureBlock.emptyCaption("figure", plotBlock());
    }

    private static TableBlock table(EditorSession session, int blockIndex) {
        return (TableBlock) session.current().document().blocks().get(blockIndex);
    }

    private static ScientificDataset dataset(String id) {
        return new ScientificDataset(
                id,
                "Data",
                List.of(
                        new DatasetColumn("time", "Time", DatasetColumnType.NUMBER),
                        new DatasetColumn("height", "Height", DatasetColumnType.NUMBER)),
                List.of(new DatasetRow(List.of(DatasetValue.number("0"), DatasetValue.number("1")))));
    }

    private static final class FakeClipboard implements ClipboardAdapter {
        private String text = "";

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean setText(String text) {
            this.text = text;
            return true;
        }
    }
}
