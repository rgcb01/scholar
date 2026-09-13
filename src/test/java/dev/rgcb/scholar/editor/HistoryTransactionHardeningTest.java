package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.clipboard.ScholarClipboardService;
import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetPlotBinding;
import dev.rgcb.scholar.data.DatasetPlotResolver;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetTableBinding;
import dev.rgcb.scholar.data.DatasetTableResolver;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceResolver;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
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
import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathOperatorRole;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HistoryTransactionHardeningTest {
    private final EditorSelectionValidator selectionValidator = new EditorSelectionValidator();

    @Test
    void coreHistoryExposesDeterministicDepthsAndRedoBranching() {
        var editor = new DocumentEditor();
        var history = new EditorHistory(new EditorState(document(paragraph("A")), new DocumentPosition(0, 1)));

        assertDepth(history, 0, 0);
        assertTrue(history.applyEdit(editor.insertText(history.current(), "B")));
        assertDepth(history, 1, 0);
        assertTrue(history.applyEdit(editor.insertText(history.current(), "C")));
        assertDepth(history, 2, 0);

        assertTrue(history.undo());
        assertEquals("AB", textOf(history.current().document().blocks().get(0)));
        assertDepth(history, 1, 1);

        history.setCurrent(editor.moveLeft(history.current()).editorState());
        assertDepth(history, 1, 1);
        assertTrue(history.redo());
        assertEquals("ABC", textOf(history.current().document().blocks().get(0)));

        assertTrue(history.undo());
        assertTrue(history.applyEdit(editor.insertText(history.current(), "D")));
        assertEquals("ADB", textOf(history.current().document().blocks().get(0)));
        assertDepth(history, 2, 0);
        assertFalse(history.redo());
    }

    @Test
    void noOpsAndTransientNavigationDoNotCreateTransactions() {
        var document = document(paragraph("abc"), TableBlock.empty(1, 1), diagramBlock());
        var session = new EditorSession(document, 0);
        assertSessionDepth(session, 0, 0);

        session.moveLeft();
        session.moveRight();
        session.extendLeft();
        session.extendRight();
        session.selectAll();
        assertSessionDepth(session, 0, 0);

        session.setCurrent(new EditorState(document, new TableEditingSelection(1,
                TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 0)), Optional.empty()));
        assertFalse(session.deleteTableRow());
        assertFalse(session.deleteTableColumn());
        assertSessionDepth(session, 0, 0);

        session.setCurrent(new EditorState(document, new DiagramEditingSelection(2,
                new DiagramPropertyTarget(DiagramProperty.CANVAS)), Optional.empty()));
        assertFalse(session.cancelDiagramConnection());
        assertFalse(session.cancelDiagramElementDrag());
        assertSessionDepth(session, 0, 0);
    }

    @Test
    void copyIsNotHistoryButCutPasteAndContextDeleteAreSingleTransactions() {
        var document = document(paragraph("A"), equation("eq", "x"), paragraph("B"));
        var session = new EditorSession(document, 0);
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        var context = new EditorActionContext(session, clipboard, sidecar);

        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        assertFalse(BuiltInEditorActions.copy().execute(context).documentChanged());
        assertSessionDepth(session, 0, 0);

        assertTrue(BuiltInEditorActions.cut().execute(context).documentChanged());
        assertSessionDepth(session, 1, 0);
        assertValid(session);
        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertSessionDepth(session, 0, 1);

        assertTrue(BuiltInEditorActions.paste().execute(context).documentChanged());
        assertSessionDepth(session, 1, 0);

        var actions = BuiltInEditorActions.editMenuActions().stream()
                .collect(java.util.stream.Collectors.toMap(EditorAction::id, action -> action));
        var entries = new EditorContextActionResolver().resolve(session.current(), actions);
        var delete = entries.stream()
                .filter(entry -> entry.kind() == ContextMenuEntryKind.ACTION)
                .map(entry -> entry.action().orElseThrow())
                .filter(action -> action.id() == EditorActionId.DELETE)
                .findFirst()
                .orElseThrow();
        assertTrue(delete.execute(context).documentChanged());
        assertSessionDepth(session, 2, 0);
        assertTrue(session.undo());
        assertValid(session);
    }

    @Test
    void textEquationTablePlotDiagramAndDatasetEditsEachCreateOneTransaction() {
        var dataset = dataset("projectile");
        var document = new Document(List.of(
                paragraph("abc"),
                equation("eq", "x"),
                TableBlock.empty(2, 2),
                plotBlock(),
                diagramBlock()), List.of(dataset));
        var session = new EditorSession(document, 0);

        assertOneTransaction(session, () -> session.typeText("X"));

        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1,
                new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), Optional.empty()));
        assertOneTransaction(session, () -> session.typeText("+"));

        session.setCurrent(new EditorState(session.current().document(), new TableEditingSelection(2,
                TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 0)), Optional.empty()));
        assertOneTransaction(session, session::insertTableRowBelow);

        session.setCurrent(new EditorState(session.current().document(), new PlotEditingSelection(3,
                new PlotSeriesTarget(0)), Optional.empty()));
        assertOneTransaction(session, session::addPlotPoint);

        session.setCurrent(new EditorState(session.current().document(), new DiagramEditingSelection(4,
                new DiagramPropertyTarget(DiagramProperty.CANVAS)), Optional.empty()));
        assertOneTransaction(session, session::addDiagramNode);

        assertOneTransaction(session, () -> session.editDatasetCell("projectile", 0, "height", DatasetValue.number("8")));
        assertValid(session);
    }

    @Test
    void figureWrapCaptionDeleteUndoRedoSequenceRestoresDeterministically() {
        var plot = plotBlock("Trajectory");
        var initial = document(paragraph("Before"), plot, paragraph("After"));
        var session = new EditorSession(initial, 0);

        session.setCurrent(new EditorState(initial, new BlockSelection(1), Optional.empty()));
        assertOneTransaction(session, session::wrapSelectedPlotInFigure);
        var wrapped = session.current().document();
        var figure = assertInstanceOf(FigureBlock.class, wrapped.blocks().get(1));
        assertEquals("figure", figure.id());

        assertTrue(session.editFigureCaption());
        assertSessionDepth(session, 1, 0);
        assertOneTransaction(session, () -> session.typeText("Velocity caption"));
        var captioned = session.current().document();

        session.setCurrent(new EditorState(captioned, new BlockSelection(1), Optional.empty()));
        assertOneTransaction(session, session::deleteForward);
        assertEquals(List.of(Paragraph.class, Paragraph.class), blockTypes(session.current().document()));

        assertUndoTo(session, captioned);
        assertUndoTo(session, wrapped);
        assertUndoTo(session, initial);
        assertTrue(session.redo());
        assertEquals(wrapped, session.current().document());
        assertTrue(session.redo());
        assertEquals(captioned, session.current().document());
        assertTrue(session.redo());
        assertEquals(List.of(Paragraph.class, Paragraph.class), blockTypes(session.current().document()));
        assertValid(session);
    }

    @Test
    void tablePlotAndDiagramCompositeEditsUndoRedoAsOneSnapshot() {
        var session = new EditorSession(document(TableBlock.empty(2, 2), plotBlock(), connectedDiagramBlock(), paragraph("tail")), 3);

        session.setCurrent(new EditorState(session.current().document(), new TableEditingSelection(0,
                TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 0)), Optional.empty()));
        var beforeTable = session.current().document();
        assertOneTransaction(session, session::deleteTableRow);
        assertUndoRedoRoundTrip(session, beforeTable);

        session.setCurrent(new EditorState(session.current().document(), new PlotEditingSelection(1,
                new PlotPointTarget(0, 0)), Optional.empty()));
        var beforePlot = session.current().document();
        assertOneTransaction(session, session::deletePlotPoint);
        assertUndoRedoRoundTrip(session, beforePlot);

        session.setCurrent(new EditorState(session.current().document(), new DiagramEditingSelection(2,
                elementTarget(session.current().document(), 2, 0)), Optional.empty()));
        var beforeDiagram = session.current().document();
        assertOneTransaction(session, session::deleteDiagramNode);
        var afterDiagram = (DiagramBlock) session.current().document().blocks().get(2);
        assertEquals(1, afterDiagram.definition().elements().size());
        assertEquals(0, afterDiagram.definition().connections().size());
        assertUndoRedoRoundTrip(session, beforeDiagram);
    }

    @Test
    void diagramDragPreviewCancelAndCommitRespectTransactionBoundary() {
        var document = document(paragraph("Before"), diagramBlock());
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DiagramEditingSelection(1,
                elementTarget(document, 1, 0)), Optional.empty()));

        assertTrue(session.beginDiagramElementDrag(12, 12));
        assertTrue(session.previewDiagramElementDrag(40, 30).isPresent());
        assertEquals(document, session.current().document());
        assertSessionDepth(session, 0, 0);
        assertTrue(session.cancelDiagramElementDrag());
        assertSessionDepth(session, 0, 0);

        assertTrue(session.beginDiagramElementDrag(12, 12));
        assertOneTransaction(session, () -> session.commitDiagramElementDrag(40, 30));
        var moved = session.current().document();
        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertTrue(session.redo());
        assertEquals(moved, session.current().document());
    }

    @Test
    void datasetMutationIsOneTransactionAndDerivedViewsRecomputeFromRestoredDocument() {
        var dataset = dataset("projectile");
        var table = new TableBlock(new DatasetTableBinding("projectile"));
        var plot = new PlotBlock(new PlotDefinition(
                "Projectile Plot",
                AxisDefinition.linear("time"),
                AxisDefinition.linear("height"),
                List.of(new PlotSeries("height", PlotSeriesKind.LINE, new DatasetPlotBinding("projectile", "time", "height"))),
                true,
                true,
                PlotDefinition.DEFAULT_HEIGHT));
        var document = new Document(List.of(table, plot, paragraph("End")), List.of(dataset));
        var session = new EditorSession(document, 2);

        assertResolvedDatasetValues(session.current().document(), "5", 5);
        assertOneTransaction(session, () -> session.editDatasetCell("projectile", 0, "height", DatasetValue.number("8")));
        assertResolvedDatasetValues(session.current().document(), "8", 8);

        assertTrue(session.undo());
        assertResolvedDatasetValues(session.current().document(), "5", 5);
        assertTrue(session.redo());
        assertResolvedDatasetValues(session.current().document(), "8", 8);
        assertValid(session);
    }

    @Test
    void pastedStableIdsAreRestoredByRedoInsteadOfRegenerated() {
        var figure = FigureBlock.emptyCaption("figure", plotBlock("Copied Plot"));
        var document = document(paragraph("A"), heading("motion", 2, "Motion"), equation("eq", "x"), table("table"), figure, paragraph("B"));
        var session = new EditorSession(document, 5);
        var context = new EditorActionContext(session, new FakeClipboard(), new ScholarClipboardService());

        copyPasteBlockAtEnd(session, context, 1);
        copyPasteBlockAtEnd(session, context, 3);
        copyPasteBlockAtEnd(session, context, indexOf(session.current().document(), FigureBlock.class));
        var pastedDocument = session.current().document();

        assertTrue(pastedDocument.blocks().stream()
                .filter(Heading.class::isInstance)
                .map(Heading.class::cast)
                .anyMatch(heading -> heading.id().equals(Optional.of("motion-2"))));
        assertTrue(pastedDocument.blocks().stream()
                .filter(TableBlock.class::isInstance)
                .map(TableBlock.class::cast)
                .anyMatch(table -> table.id().equals(Optional.of("table-2"))));
        assertTrue(pastedDocument.blocks().stream()
                .filter(FigureBlock.class::isInstance)
                .map(FigureBlock.class::cast)
                .anyMatch(candidate -> candidate.id().equals("figure-2")));

        assertTrue(session.undo());
        assertTrue(session.undo());
        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertTrue(session.redo());
        assertTrue(session.redo());
        assertTrue(session.redo());
        assertEquals(pastedDocument, session.current().document());
    }

    @Test
    void referencesAndDerivedStructureRestoreAcrossUndoRedo() {
        var resolver = new CrossReferenceResolver();
        var document = document(
                heading("intro", 1, "Introduction"),
                heading("methods", 1, "Methods"),
                new Paragraph(new InlineContent(List.of(
                        new Text("See ", Set.of()),
                        new CrossReference(CrossReferenceTargetKind.SECTION, "methods")))));
        var session = new EditorSession(document, 0);
        assertEquals("See Section 2", resolver.inlineText(document, ((Paragraph) document.blocks().get(2)).content()));

        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        assertOneTransaction(session, session::deleteForward);
        assertEquals("See [Missing reference]",
                resolver.inlineText(session.current().document(), ((Paragraph) session.current().document().blocks().get(1)).content()));

        assertTrue(session.undo());
        assertEquals("See Section 2", resolver.inlineText(session.current().document(), ((Paragraph) session.current().document().blocks().get(2)).content()));
        assertTrue(session.redo());
        assertEquals("See [Missing reference]",
                resolver.inlineText(session.current().document(), ((Paragraph) session.current().document().blocks().get(1)).content()));
    }

    @Test
    void goldenMixedDocumentSequenceUndoesAndRedoesEveryLogicalAction() {
        var initial = new Document(List.of(
                paragraph("AlphaBeta"),
                heading("motion", 2, "Motion"),
                equation("eq", "x"),
                TableBlock.empty(2, 2),
                plotBlock(),
                diagramBlock(),
                FigureBlock.emptyCaption("fig", plotBlock("Figure Plot")),
                new TableOfContentsBlock(),
                new TableBlock(new DatasetTableBinding("projectile")),
                datasetPlotBlock(),
                paragraph("Omega")), List.of(dataset("projectile")));
        var session = new EditorSession(initial, 0);
        var snapshots = new ArrayList<Document>();
        snapshots.add(initial);

        doEdit(session, snapshots, () -> session.typeText("!"));
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 5)));
        doEdit(session, snapshots, session::enter);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(2, 6)));
        doEdit(session, snapshots, () -> session.typeText("!"));
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(3,
                new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), Optional.empty()));
        doEdit(session, snapshots, () -> session.typeText("+"));
        session.setCurrent(new EditorState(session.current().document(), new TableEditingSelection(4,
                TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 0)), Optional.empty()));
        doEdit(session, snapshots, session::insertTableRowBelow);
        session.setCurrent(new EditorState(session.current().document(), new PlotEditingSelection(5,
                new PlotSeriesTarget(0)), Optional.empty()));
        doEdit(session, snapshots, session::addPlotPoint);
        session.setCurrent(new EditorState(session.current().document(), new DiagramEditingSelection(6,
                elementTarget(session.current().document(), 6, 0)), Optional.empty()));
        assertTrue(session.beginDiagramElementDrag(12, 12));
        doEdit(session, snapshots, () -> session.commitDiagramElementDrag(60, 40));
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(7), Optional.empty()));
        assertTrue(session.editFigureCaption());
        doEdit(session, snapshots, () -> session.typeText("caption"));
        doEdit(session, snapshots, () -> session.editDatasetCell("projectile", 0, "height", DatasetValue.number("8")));
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(8), Optional.empty()));
        doEdit(session, snapshots, session::deleteForward);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(indexOf(session.current().document(), EquationBlock.class)), Optional.empty()));
        var context = new EditorActionContext(session, new FakeClipboard(), new ScholarClipboardService());
        assertFalse(BuiltInEditorActions.copy().execute(context).documentChanged());
        doEdit(session, snapshots, () -> BuiltInEditorActions.paste().execute(context).documentChanged());
        doEdit(session, snapshots, session::deleteForward);

        for (var index = snapshots.size() - 2; index >= 0; index--) {
            assertTrue(session.undo(), "undo " + index);
            assertEquals(snapshots.get(index), session.current().document());
            assertValid(session);
        }
        for (var index = 1; index < snapshots.size(); index++) {
            assertTrue(session.redo(), "redo " + index);
            assertEquals(snapshots.get(index), session.current().document());
            assertValid(session);
        }
    }

    @Test
    void seededRandomHistoryNavigationKeepsValidDocumentsAndSelections() {
        var random = new Random(2405);
        var session = new EditorSession(document(paragraph("seed"), paragraph("tail")), 0);
        var snapshots = new ArrayList<Document>();
        snapshots.add(session.current().document());
        var cursor = 0;

        for (var step = 0; step < 60; step++) {
            var choice = random.nextInt(8);
            if (choice == 0 && session.canUndo()) {
                assertTrue(session.undo());
                cursor--;
            } else if (choice == 1 && session.canRedo()) {
                assertTrue(session.redo());
                cursor++;
            } else {
                session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(firstEditableBlock(session.current().document()), 0)));
                var changed = switch (choice) {
                    case 2 -> session.typeText("x");
                    case 3 -> session.enter();
                    case 4 -> session.insertEmptyEquation();
                    case 5 -> session.insertDefaultTable();
                    case 6 -> session.insertDefaultPlot();
                    default -> session.insertDefaultDiagram();
                };
                if (changed) {
                    while (snapshots.size() > cursor + 1) {
                        snapshots.remove(snapshots.size() - 1);
                    }
                    snapshots.add(session.current().document());
                    cursor++;
                }
            }
            assertEquals(snapshots.get(cursor), session.current().document());
            assertValid(session);
        }
    }

    private void assertOneTransaction(EditorSession session, BooleanEdit edit) {
        var beforeUndo = session.undoDepth();
        var beforeRedo = session.redoDepth();
        assertTrue(edit.run());
        assertEquals(beforeUndo + 1, session.undoDepth());
        assertEquals(0, session.redoDepth());
        assertValid(session);
        if (beforeRedo > 0) {
            assertFalse(session.canRedo());
        }
    }

    private void assertUndoRedoRoundTrip(EditorSession session, Document before) {
        var after = session.current().document();
        assertTrue(session.undo());
        assertEquals(before, session.current().document());
        assertValid(session);
        assertTrue(session.redo());
        assertEquals(after, session.current().document());
        assertValid(session);
    }

    private void assertUndoTo(EditorSession session, Document expected) {
        assertTrue(session.undo());
        assertEquals(expected, session.current().document());
        assertValid(session);
    }

    private void doEdit(EditorSession session, List<Document> snapshots, BooleanEdit edit) {
        assertOneTransaction(session, edit);
        snapshots.add(session.current().document());
    }

    private void copyPasteBlock(EditorSession session, EditorActionContext context, int blockIndex) {
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(blockIndex), Optional.empty()));
        assertFalse(BuiltInEditorActions.copy().execute(context).documentChanged());
        assertOneTransaction(session, () -> BuiltInEditorActions.paste().execute(context).documentChanged());
    }

    private void copyPasteBlockAtEnd(EditorSession session, EditorActionContext context, int blockIndex) {
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(blockIndex), Optional.empty()));
        assertFalse(BuiltInEditorActions.copy().execute(context).documentChanged());
        var lastIndex = session.current().document().blocks().size() - 1;
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(lastIndex, textOf(session.current().document().blocks().get(lastIndex)).length())));
        assertOneTransaction(session, () -> BuiltInEditorActions.paste().execute(context).documentChanged());
    }

    private void assertValid(EditorSession session) {
        var result = DocumentValidator.validate(session.current().document());
        assertTrue(result.errors().isEmpty(), () -> result.errors().toString());
        selectionValidator.validate(session.current().document(), session.current().selection());
    }

    private static void assertDepth(EditorHistory history, int undoDepth, int redoDepth) {
        assertEquals(undoDepth, history.undoDepth());
        assertEquals(redoDepth, history.redoDepth());
    }

    private static void assertSessionDepth(EditorSession session, int undoDepth, int redoDepth) {
        assertEquals(undoDepth, session.undoDepth());
        assertEquals(redoDepth, session.redoDepth());
    }

    private static void assertResolvedDatasetValues(Document document, String tableValue, double plotValue) {
        var table = new DatasetTableResolver().resolve(document, (TableBlock) document.blocks().get(0));
        var cellText = textOf(table.rows().get(1).cells().get(1).content().content());
        assertEquals(tableValue, cellText);
        var plot = new DatasetPlotResolver().resolve(document, (PlotBlock) document.blocks().get(1));
        assertEquals(plotValue, plot.definition().series().get(0).points().get(0).y());
    }

    private static int firstEditableBlock(Document document) {
        for (var index = 0; index < document.blocks().size(); index++) {
            if (document.blocks().get(index) instanceof Paragraph || document.blocks().get(index) instanceof Heading) {
                return index;
            }
        }
        return 0;
    }

    private static int indexOf(Document document, Class<? extends BlockNode> type) {
        for (var index = 0; index < document.blocks().size(); index++) {
            if (type.isInstance(document.blocks().get(index))) {
                return index;
            }
        }
        throw new AssertionError("Missing block type " + type.getSimpleName());
    }

    private static List<Class<? extends BlockNode>> blockTypes(Document document) {
        return document.blocks().stream()
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

    private static String textOf(InlineContent content) {
        return InlineContentEditor.logicalText(content);
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
        return plotBlock("Plot");
    }

    private static PlotBlock plotBlock(String title) {
        return new PlotBlock(PlotDefinition.of(
                title,
                AxisDefinition.linear("t"),
                AxisDefinition.linear("x"),
                List.of(new PlotSeries("series", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(1, 5))))));
    }

    private static PlotBlock datasetPlotBlock() {
        return new PlotBlock(new PlotDefinition(
                "Dataset Plot",
                AxisDefinition.linear("time"),
                AxisDefinition.linear("height"),
                List.of(new PlotSeries("height", PlotSeriesKind.LINE, new DatasetPlotBinding("projectile", "time", "height"))),
                true,
                true,
                PlotDefinition.DEFAULT_HEIGHT));
    }

    private static DiagramBlock diagramBlock() {
        var sensor = new DiagramElementId("sensor");
        var out = new DiagramPortId("out");
        return new DiagramBlock(new DiagramDefinition(
                "System Diagram",
                new DiagramCanvas(100, 50),
                List.of(new DiagramNode(sensor, new DiagramBounds(8, 10, 30, 20), "Sensor", List.of(
                        new DiagramPort(out, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5))))),
                List.of()));
    }

    private static DiagramBlock connectedDiagramBlock() {
        var sensor = new DiagramElementId("sensor");
        var processor = new DiagramElementId("processor");
        var out = new DiagramPortId("out");
        var in = new DiagramPortId("in");
        return new DiagramBlock(new DiagramDefinition(
                "System Diagram",
                new DiagramCanvas(100, 50),
                List.of(
                        new DiagramNode(sensor, new DiagramBounds(8, 10, 30, 20), "Sensor", List.of(
                                new DiagramPort(out, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)))),
                        new DiagramNode(processor, new DiagramBounds(62, 20, 30, 20), "Processor", List.of(
                                new DiagramPort(in, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5))))),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(sensor, out),
                        new DiagramEndpoint(processor, in),
                        "signal"))));
    }

    private static DiagramElementTarget elementTarget(Document document, int blockIndex, int elementIndex) {
        var diagram = (DiagramBlock) document.blocks().get(blockIndex);
        var id = diagram.definition().elements().get(elementIndex).id();
        return new DiagramElementTarget(elementIndex, id);
    }

    private static DiagramPortTarget portTarget(Document document, int blockIndex, int elementIndex, int portIndex) {
        var diagram = (DiagramBlock) document.blocks().get(blockIndex);
        var element = diagram.definition().elements().get(elementIndex);
        var ports = element.ports();
        var portId = ports.isEmpty() ? new DiagramPortId("center") : ports.get(portIndex).id();
        return new DiagramPortTarget(elementIndex, portIndex, element.id(), portId);
    }

    private static ScientificDataset dataset(String id) {
        return new ScientificDataset(
                id,
                "Projectile",
                List.of(
                        new DatasetColumn("time", "Time", DatasetColumnType.NUMBER),
                        new DatasetColumn("height", "Height", DatasetColumnType.NUMBER)),
                List.of(new DatasetRow(List.of(DatasetValue.number("0"), DatasetValue.number("5")))));
    }

    @FunctionalInterface
    private interface BooleanEdit {
        boolean run();
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
