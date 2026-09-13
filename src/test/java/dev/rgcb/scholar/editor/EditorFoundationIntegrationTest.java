package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.clipboard.ScholarClipboardService;
import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetPlotResolver;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetTableResolver;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.clipboard.DatasetClipboardPayload;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceResolver;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.DocumentStructureResolver;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.math.editor.MathCaretSelection;
import dev.rgcb.scholar.math.editor.MathPath;
import dev.rgcb.scholar.math.editor.MathSequencePosition;
import dev.rgcb.scholar.mechanical.MechanicalPrimitiveKind;
import dev.rgcb.scholar.mechanical.MechanicalSymbolKind;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.validation.DocumentDiagnosticCode;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EditorFoundationIntegrationTest {
    private final EditorSelectionValidator selectionValidator = new EditorSelectionValidator();

    @Test
    void canonicalMixedDocumentValidatesWithoutWarningsAndSelectionsAreValid() {
        var document = EditorFoundationFixture.canonicalDocument();
        var result = DocumentValidator.validate(document);

        assertTrue(result.errors().isEmpty(), () -> result.errors().toString());
        assertTrue(result.warnings().isEmpty(), () -> result.warnings().toString());

        assertSelectionValid(document, new EditorState(document, new DocumentPosition(0, 0)).selection());
        assertSelectionValid(document, new BlockSelection(indexOf(document, EquationBlock.class)));
        assertSelectionValid(document, new EquationEditingSelection(indexOf(document, EquationBlock.class),
                new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))));
        assertSelectionValid(document, new TableEditingSelection(indexOf(document, TableBlock.class),
                TableCellTextSelection.caret(new TableCellCoordinate(1, 1), 0)));
        assertSelectionValid(document, new BlockSelection(indexOf(document, TableOfContentsBlock.class)));
        assertSelectionValid(document, FigureCaptionSelection.caret(indexOf(document, FigureBlock.class), 0));
    }

    @Test
    void longGoldenCrossSystemSequenceUndoAllAndRedoAllRestoresExactStates() {
        var session = new EditorSession(EditorFoundationFixture.canonicalDocument(), 0);
        var context = new EditorActionContext(session, new FakeClipboard(), new ScholarClipboardService());
        var snapshots = new ArrayList<EditorState>();
        snapshots.add(session.current());

        doEdit(session, snapshots, () -> session.typeText("!"));
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(4, 0), new DocumentPosition(4, 8)));
        syncCurrent(session, snapshots);
        doEdit(session, snapshots, () -> session.toggleMark(TextMark.ITALIC));
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(16, 12)));
        syncCurrent(session, snapshots);
        doEdit(session, snapshots, session::enter);
        doEdit(session, snapshots, () -> session.typeText("split"));
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(17, 0)));
        syncCurrent(session, snapshots);
        doEdit(session, snapshots, session::deleteBackward);

        session.setCurrent(state(session.current().document(), new BlockSelection(indexOf(session.current().document(), EquationBlock.class))));
        session.enter();
        session.setEquationEditingSelection(new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 3)));
        syncCurrent(session, snapshots);
        doEdit(session, snapshots, () -> session.typeText("+"));
        doEdit(session, snapshots, session::insertRoot);

        session.setCurrent(state(session.current().document(), new TableEditingSelection(indexOf(session.current().document(), TableBlock.class),
                TableCellTextSelection.caret(new TableCellCoordinate(1, 1), 1))));
        syncCurrent(session, snapshots);
        doEdit(session, snapshots, () -> session.typeText("9"));
        doEdit(session, snapshots, session::insertTableRowBelow);
        doEdit(session, snapshots, session::insertTableColumnRight);
        doEdit(session, snapshots, session::deleteTableColumn);

        session.setCurrent(state(session.current().document(), new PlotEditingSelection(indexOf(session.current().document(), PlotBlock.class),
                new PlotSeriesTarget(0))));
        syncCurrent(session, snapshots);
        doEdit(session, snapshots, session::addPlotPoint);
        doEdit(session, snapshots, () -> session.addPlotSeries(PlotSeriesKind.SCATTER));

        session.setCurrent(state(session.current().document(), new DiagramEditingSelection(indexOf(session.current().document(), DiagramBlock.class),
                elementTarget(session.current().document(), indexOf(session.current().document(), DiagramBlock.class), 0))));
        syncCurrent(session, snapshots);
        assertTrue(session.beginDiagramElementDrag(9, 21));
        doEdit(session, snapshots, () -> session.commitDiagramElementDrag(22, 34));
        session.setCurrent(state(session.current().document(), new DiagramEditingSelection(indexOf(session.current().document(), DiagramBlock.class),
                new DiagramPropertyTarget(DiagramProperty.CANVAS))));
        syncCurrent(session, snapshots);
        doEdit(session, snapshots, session::addDiagramNode);

        var electricalIndex = diagramIndexByTitle(session.current().document(), "Electrical Diagram");
        session.setCurrent(state(session.current().document(), new DiagramEditingSelection(electricalIndex,
                new DiagramPropertyTarget(DiagramProperty.CANVAS))));
        syncCurrent(session, snapshots);
        doEdit(session, snapshots, () -> session.addElectricalComponent(dev.rgcb.scholar.electrical.ElectricalComponentKind.LED));

        var mechanicalIndex = diagramIndexByTitle(session.current().document(), "Mechanical Diagram");
        session.setCurrent(state(session.current().document(), new DiagramEditingSelection(mechanicalIndex,
                new DiagramPropertyTarget(DiagramProperty.CANVAS))));
        syncCurrent(session, snapshots);
        doEdit(session, snapshots, () -> session.addMechanicalPrimitive(MechanicalPrimitiveKind.RECTANGLE));
        doEdit(session, snapshots, () -> session.addMechanicalSymbol(MechanicalSymbolKind.SPRING));

        session.setCurrent(state(session.current().document(), new BlockSelection(indexOf(session.current().document(), FigureBlock.class))));
        assertTrue(session.editFigureCaption());
        syncCurrent(session, snapshots);
        doEdit(session, snapshots, () -> session.typeText(" updated"));

        doEdit(session, snapshots, () -> session.editDatasetCell("projectile", 1, "height", DatasetValue.number("6")));
        doEdit(session, snapshots, () -> session.addDatasetRow("projectile", new DatasetRow(List.of(
                DatasetValue.number("3"), DatasetValue.number("1"), DatasetValue.text("extra")))));
        doEdit(session, snapshots, () -> session.addDatasetColumn("projectile", new DatasetColumn("quality", "Quality", DatasetColumnType.TEXT), DatasetValue.text("ok")));
        doEdit(session, snapshots, () -> session.deleteDatasetColumn("projectile", "quality"));

        session.setCurrent(state(session.current().document(), new BlockSelection(3)));
        syncCurrent(session, snapshots);
        var beforeCopy = session.current();
        assertFalse(action(EditorActionId.COPY).execute(context).documentChanged());
        assertEquals(beforeCopy, session.current());
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(session.current().document().blocks().size() - 1, 0)));
        syncCurrent(session, snapshots);
        doEdit(session, snapshots, () -> action(EditorActionId.PASTE).execute(context).documentChanged());

        var finalState = session.current();
        undoAll(session, snapshots);
        redoAll(session, snapshots);
        assertEquals(finalState, session.current());
    }

    @Test
    void selectionTransitionMatrixRejectsStaleNestedStateAfterTargetChanges() {
        var document = EditorFoundationFixture.canonicalDocument();
        var session = new EditorSession(document, 0);

        session.moveRight();
        assertValid(session);
        session.setCurrent(state(document, new BlockSelection(indexOf(document, EquationBlock.class))));
        session.enter();
        assertEquals(EditorFocusOwner.EQUATION, session.focusOwner());
        session.setCurrent(new EditorState(document, new DocumentPosition(4, 0)));
        assertEquals(EditorFocusOwner.DOCUMENT_TEXT, session.focusOwner());

        session.setCurrent(state(document, new TableEditingSelection(indexOf(document, TableBlock.class),
                TableCellTextSelection.caret(new TableCellCoordinate(1, 0), 0))));
        session.exitTableEditing();
        assertEquals(EditorFocusOwner.BLOCK, session.focusOwner());

        session.setCurrent(state(document, new DiagramEditingSelection(indexOf(document, DiagramBlock.class),
                elementTarget(document, indexOf(document, DiagramBlock.class), 0))));
        session.setCurrent(state(document, new BlockSelection(indexOf(document, FigureBlock.class))));
        assertEquals(EditorFocusOwner.BLOCK, session.focusOwner());
        assertTrue(session.editFigureCaption());
        assertEquals(EditorFocusOwner.FIGURE_CAPTION, session.focusOwner());
        assertValid(session);
    }

    @Test
    void contextMenuDeleteUsesSameActionPathAsKeyboardDelete() {
        var document = EditorFoundationFixture.canonicalDocument();
        var keyboard = new EditorSession(document, 0);
        var contextMenu = new EditorSession(document, 0);
        var blockIndex = indexOf(document, EquationBlock.class);

        keyboard.setCurrent(state(document, new BlockSelection(blockIndex)));
        contextMenu.setCurrent(state(document, new BlockSelection(blockIndex)));

        assertTrue(keyboard.deleteForward());
        var deleteAction = new EditorContextActionResolver().resolve(
                        contextMenu.current(),
                        BuiltInEditorActions.editMenuActions().stream().collect(Collectors.toMap(EditorAction::id, action -> action)))
                .stream()
                .filter(entry -> entry.kind() == ContextMenuEntryKind.ACTION)
                .map(entry -> entry.action().orElseThrow())
                .filter(action -> action.id() == EditorActionId.DELETE)
                .findFirst()
                .orElseThrow();
        assertTrue(deleteAction.execute(new EditorActionContext(contextMenu, new FakeClipboard())).documentChanged());

        assertEquals(keyboard.current(), contextMenu.current());
        assertEquals(1, keyboard.undoDepth());
        assertEquals(1, contextMenu.undoDepth());
        assertValidAllowWarnings(contextMenu);
    }

    @Test
    void clipboardCrossSystemRegressionPreservesCurrentSupportedPayloadsAndIds() {
        var document = EditorFoundationFixture.canonicalDocument();
        var session = new EditorSession(document, 0);
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        var context = new EditorActionContext(session, clipboard, sidecar);

        copyDoesNotMutate(session, context, new TextSelection(new DocumentPosition(4, 0), new DocumentPosition(4, 8)));
        cutAndUndo(session, context, new TextSelection(new DocumentPosition(4, 0), new DocumentPosition(4, 8)));
        copyPasteBlockTwice(session, context, headingIndexById(session.current().document(), "analysis"));
        copyPasteBlockTwice(session, context, indexOf(session.current().document(), EquationBlock.class));
        copyPasteBlockTwice(session, context, indexOf(session.current().document(), TableBlock.class));
        copyPasteBlockTwice(session, context, indexOf(session.current().document(), PlotBlock.class));
        copyPasteBlockTwice(session, context, indexOf(session.current().document(), DiagramBlock.class));
        copyPasteBlockTwice(session, context, indexOf(session.current().document(), FigureBlock.class));
        copyPasteBlockTwice(session, context, indexOf(session.current().document(), TableOfContentsBlock.class));

        var copy = session.copyDatasetForClipboard("projectile").orElseThrow();
        assertInstanceOf(DatasetClipboardPayload.class, copy.payload().orElseThrow());
        sidecar.install(copy.plainText(), copy.payload().orElseThrow());
        clipboard.setText(copy.plainText());
        assertTrue(action(EditorActionId.PASTE).execute(context).documentChanged());

        assertValid(session);
    }

    @Test
    void crossReferencesTocOutlineAndDerivedDatasetViewsRecomputeAfterChangesAndUndo() {
        var document = EditorFoundationFixture.canonicalDocument();
        var session = new EditorSession(document, 0);
        var referenceResolver = new CrossReferenceResolver();
        var structureResolver = new DocumentStructureResolver();
        var tableResolver = new DatasetTableResolver();
        var plotResolver = new DatasetPlotResolver();

        assertEquals("Section 1.1", referenceResolver.resolve(document, new CrossReference(CrossReferenceTargetKind.SECTION, "analysis")).displayText());
        assertEquals("Figure 1", referenceResolver.resolve(document, new CrossReference(CrossReferenceTargetKind.FIGURE, "trajectory-figure")).displayText());
        assertEquals(List.of("1", "1.1", "1.2"), structureResolver.resolve(document).sections().stream()
                .map(entry -> entry.number().displayText())
                .toList());

        assertTrue(session.editDatasetCell("projectile", 1, "height", DatasetValue.number("8")));
        var resolvedTable = tableResolver.resolve(session.current().document(),
                (TableBlock) session.current().document().blocks().get(datasetTableIndex(session.current().document())));
        assertEquals("8", cellText(resolvedTable, 2, 1));
        var resolvedPlot = plotResolver.resolve(session.current().document(),
                (PlotBlock) session.current().document().blocks().get(datasetPlotIndex(session.current().document())));
        assertEquals(8.0, resolvedPlot.definition().series().get(0).points().get(1).y());

        session.setCurrent(state(session.current().document(), new BlockSelection(indexOf(session.current().document(), FigureBlock.class))));
        assertTrue(session.deleteForward());
        var warnings = DocumentValidator.validate(session.current().document()).warnings();
        assertTrue(warnings.stream().anyMatch(warning -> warning.code() == DocumentDiagnosticCode.MISSING_CROSS_REFERENCE_TARGET));
        assertTrue(session.undo());
        assertTrue(DocumentValidator.validate(session.current().document()).warnings().isEmpty());
        assertEquals("Figure 1", referenceResolver.resolve(session.current().document(), new CrossReference(CrossReferenceTargetKind.FIGURE, "trajectory-figure")).displayText());
        assertValid(session);
    }

    @Test
    void atomicBoundaryAndMinimalDocumentPoliciesRemainStable() {
        for (var atomic : List.of(
                EditorFoundationFixture.equation("eq"),
                EditorFoundationFixture.table("table"),
                EditorFoundationFixture.plot("plot"),
                EditorFoundationFixture.genericDiagram(),
                new FigureBlock("fig", EditorFoundationFixture.plot("figure"), EditorFoundationFixture.inline("caption")),
                new TableOfContentsBlock())) {
            var document = new Document(List.of(EditorFoundationFixture.paragraph("left"), atomic, EditorFoundationFixture.paragraph("right")));
            var session = new EditorSession(document, 0);
            session.setCurrent(new EditorState(document, new DocumentPosition(0, 4)));
            session.deleteForward();
            assertEquals(new BlockSelection(1), session.current().selection());
            assertEquals(document, session.current().document());
            assertTrue(session.deleteForward());
            assertEquals(2, session.current().document().blocks().size());
            assertTrue(session.undo());
            assertEquals(document, session.current().document());
            assertValid(session);
        }

        var session = new EditorSession(new Document(List.of(EditorFoundationFixture.heading("only", 1, "Only"))), 0);
        session.setCurrent(state(session.current().document(), new BlockSelection(0)));
        assertTrue(session.deleteForward());
        assertEquals(List.of(Paragraph.class), session.current().document().blocks().stream().map(BlockNode::getClass).toList());
        assertEquals(new TextSelection(new DocumentPosition(0, 0), new DocumentPosition(0, 0)), session.current().selection());
        assertValid(session);
    }

    @Test
    void degradedWarningsAreDeterministicAndNotSilentlyRepairedByHistory() {
        var document = EditorFoundationFixture.degradedDocument();
        var first = DocumentValidator.validate(document);
        var second = DocumentValidator.validate(document);
        assertEquals(first.warnings(), second.warnings());
        assertTrue(first.errors().isEmpty());
        assertEquals(Set.of(
                        DocumentDiagnosticCode.MISSING_CROSS_REFERENCE_TARGET,
                        DocumentDiagnosticCode.MISSING_DATASET,
                        DocumentDiagnosticCode.MISSING_DATASET_COLUMN),
                first.warnings().stream().map(warning -> warning.code()).collect(Collectors.toSet()));

        var session = new EditorSession(document, 0);
        assertTrue(session.typeText("!"));
        assertTrue(session.undo());
        assertEquals(first.warnings(), DocumentValidator.validate(session.current().document()).warnings());
        assertValidAllowWarnings(session);
    }

    @Test
    void moderatelyLargeDocumentSmokeTestDoesNotHang() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            var blocks = new ArrayList<BlockNode>();
            for (var index = 0; index < 120; index++) {
                blocks.add(EditorFoundationFixture.paragraph("Paragraph " + index + " velocity displacement time"));
                if (index % 20 == 0) {
                    blocks.add(EditorFoundationFixture.equation("eq-" + index));
                    blocks.add(EditorFoundationFixture.table("table-" + index));
                    blocks.add(EditorFoundationFixture.plot("plot-" + index));
                    blocks.add(EditorFoundationFixture.genericDiagram());
                }
            }
            blocks.add(new TableBlock(new dev.rgcb.scholar.data.DatasetTableBinding("projectile")));
            blocks.add(EditorFoundationFixture.datasetPlot());
            var document = new Document(blocks, List.of(EditorFoundationFixture.projectileDataset()));
            var session = new EditorSession(document, 0);
            for (var index = 0; index < 80; index++) {
                session.moveRight();
                assertValid(session);
            }
            new DocumentStructureResolver().resolve(document);
            DocumentValidator.validate(document);
            assertTrue(session.typeText("x"));
            assertTrue(session.undo());
            assertTrue(session.redo());
            assertValid(session);
        });
    }

    private void doEdit(EditorSession session, List<EditorState> snapshots, BooleanSupplier edit) {
        var before = session.undoDepth();
        assertTrue(edit.getAsBoolean());
        assertTrue(session.undoDepth() == before || session.undoDepth() == before + 1,
                "semantic edit must create or extend exactly one history transaction");
        assertEquals(0, session.redoDepth());
        assertValid(session);
        if (session.undoDepth() == before + 1) {
            snapshots.add(session.current());
        } else {
            snapshots.set(snapshots.size() - 1, session.current());
        }
    }

    private void syncCurrent(EditorSession session, List<EditorState> snapshots) {
        snapshots.set(snapshots.size() - 1, session.current());
    }

    private void undoAll(EditorSession session, List<EditorState> snapshots) {
        for (var index = snapshots.size() - 2; index >= 0; index--) {
            assertTrue(session.undo(), "undo " + index);
            assertEquals(snapshots.get(index), session.current());
            assertValid(session);
        }
        assertFalse(session.canUndo());
    }

    private void redoAll(EditorSession session, List<EditorState> snapshots) {
        for (var index = 1; index < snapshots.size(); index++) {
            assertTrue(session.redo(), "redo " + index);
            assertEquals(snapshots.get(index), session.current());
            assertValid(session);
        }
        assertFalse(session.canRedo());
    }

    private void copyDoesNotMutate(EditorSession session, EditorActionContext context, EditorSelection selection) {
        var before = session.current();
        session.setCurrent(state(session.current().document(), selection));
        assertFalse(action(EditorActionId.COPY).execute(context).documentChanged());
        assertEquals(before.document(), session.current().document());
        assertEquals(0, session.undoDepth());
    }

    private void cutAndUndo(EditorSession session, EditorActionContext context, EditorSelection selection) {
        session.setCurrent(state(session.current().document(), selection));
        var before = session.current();
        assertTrue(action(EditorActionId.CUT).execute(context).documentChanged());
        assertTrue(session.undo());
        assertEquals(before, session.current());
        assertValid(session);
    }

    private void copyPasteBlockTwice(EditorSession session, EditorActionContext context, int blockIndex) {
        session.setCurrent(state(session.current().document(), new BlockSelection(blockIndex)));
        assertFalse(action(EditorActionId.COPY).execute(context).documentChanged());
        var insertionIndex = session.current().document().blocks().size() - 1;
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(insertionIndex, textLength(session.current().document(), insertionIndex))));
        assertTrue(action(EditorActionId.PASTE).execute(context).documentChanged());
        insertionIndex = session.current().document().blocks().size() - 1;
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(insertionIndex, textLength(session.current().document(), insertionIndex))));
        assertTrue(action(EditorActionId.PASTE).execute(context).documentChanged());
        assertValid(session);
    }

    private void assertValid(EditorSession session) {
        var validation = DocumentValidator.validate(session.current().document());
        assertTrue(validation.errors().isEmpty(), () -> validation.errors().toString());
        assertTrue(validation.warnings().isEmpty(), () -> validation.warnings().toString());
        selectionValidator.validate(session.current().document(), session.current().selection());
    }

    private void assertValidAllowWarnings(EditorSession session) {
        var validation = DocumentValidator.validate(session.current().document());
        assertTrue(validation.errors().isEmpty(), () -> validation.errors().toString());
        selectionValidator.validate(session.current().document(), session.current().selection());
    }

    private void assertSelectionValid(Document document, EditorSelection selection) {
        selectionValidator.validate(document, selection);
    }

    private static EditorState state(Document document, EditorSelection selection) {
        return new EditorState(document, selection, Optional.empty());
    }

    private static EditorAction action(EditorActionId id) {
        return allActions().stream()
                .filter(action -> action.id() == id)
                .findFirst()
                .orElseThrow();
    }

    private static List<EditorAction> allActions() {
        var actions = new ArrayList<EditorAction>();
        actions.addAll(BuiltInEditorActions.editMenuActions());
        actions.addAll(BuiltInEditorActions.insertMenuActions());
        actions.addAll(BuiltInEditorActions.blockStyleActions());
        actions.addAll(BuiltInEditorActions.formatMenuActions());
        actions.addAll(BuiltInEditorActions.tableMenuActions());
        actions.addAll(BuiltInEditorActions.plotMenuActions());
        actions.addAll(BuiltInEditorActions.diagramMenuActions());
        actions.addAll(BuiltInEditorActions.figureMenuActions());
        actions.addAll(BuiltInEditorActions.viewMenuActions());
        actions.addAll(BuiltInEditorActions.dataMenuActions());
        return actions;
    }

    private static int indexOf(Document document, Class<? extends BlockNode> type) {
        return EditorFoundationFixture.indexOf(document, type);
    }

    private static int headingIndexById(Document document, String id) {
        for (var index = 0; index < document.blocks().size(); index++) {
            if (document.blocks().get(index) instanceof Heading heading && heading.id().equals(Optional.of(id))) {
                return index;
            }
        }
        throw new AssertionError("Missing heading " + id);
    }

    private static int diagramIndexByTitle(Document document, String title) {
        for (var index = 0; index < document.blocks().size(); index++) {
            if (document.blocks().get(index) instanceof DiagramBlock diagram && diagram.definition().title().equals(title)) {
                return index;
            }
        }
        throw new AssertionError("Missing diagram " + title);
    }

    private static int datasetTableIndex(Document document) {
        for (var index = 0; index < document.blocks().size(); index++) {
            if (document.blocks().get(index) instanceof TableBlock table && table.datasetBinding().isPresent()) {
                return index;
            }
        }
        throw new AssertionError("Missing dataset table");
    }

    private static int datasetPlotIndex(Document document) {
        for (var index = 0; index < document.blocks().size(); index++) {
            if (document.blocks().get(index) instanceof PlotBlock plot
                    && plot.definition().series().stream().anyMatch(series -> series.datasetBinding().isPresent())) {
                return index;
            }
        }
        throw new AssertionError("Missing dataset plot");
    }

    private static DiagramElementTarget elementTarget(Document document, int blockIndex, int elementIndex) {
        var diagram = (DiagramBlock) document.blocks().get(blockIndex);
        return new DiagramElementTarget(elementIndex, diagram.definition().elements().get(elementIndex).id());
    }

    private static int textLength(Document document, int blockIndex) {
        var block = document.blocks().get(blockIndex);
        return block instanceof Paragraph paragraph
                ? InlineContentEditor.characterCount(paragraph.content())
                : 0;
    }

    private static String cellText(TableBlock table, int row, int column) {
        return InlineContentEditor.logicalText(table.rows().get(row).cells().get(column).content().content());
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
