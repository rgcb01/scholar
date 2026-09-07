package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.clipboard.ScholarClipboardPayload;
import dev.rgcb.scholar.layout.LaidOutBlock;
import dev.rgcb.scholar.math.MathDelimiter;
import dev.rgcb.scholar.math.clipboard.MathClipboardPayload;
import dev.rgcb.scholar.math.clipboard.MathPlainTextImporter;
import dev.rgcb.scholar.math.clipboard.MathPlainTextImportResult;
import dev.rgcb.scholar.math.clipboard.MathPlainTextSerializer;
import dev.rgcb.scholar.math.editor.MathExpressionEditor;
import dev.rgcb.scholar.math.editor.MathRangeSelection;
import dev.rgcb.scholar.math.editor.ScriptSlot;
import dev.rgcb.scholar.math.editor.SemanticMathTokenDraft;
import dev.rgcb.scholar.math.editor.SemanticMathTokenKind;
import dev.rgcb.scholar.table.clipboard.TableClipboardPayload;
import dev.rgcb.scholar.table.clipboard.TableTsvSerializer;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.plot.clipboard.PlotClipboardPayload;
import dev.rgcb.scholar.plot.clipboard.PlotPlainTextSerializer;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.clipboard.DiagramClipboardPayload;
import dev.rgcb.scholar.diagram.clipboard.DiagramPlainTextSerializer;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalJunction;
import dev.rgcb.scholar.electrical.editor.ElectricalComponentDraft;
import dev.rgcb.scholar.electrical.editor.ElectricalDiagramEditor;
import dev.rgcb.scholar.mechanical.MechanicalPrimitiveKind;
import dev.rgcb.scholar.mechanical.MechanicalSymbolKind;
import dev.rgcb.scholar.mechanical.MechanicalAnnotationKind;
import dev.rgcb.scholar.mechanical.MechanicalPartReference;
import dev.rgcb.scholar.mechanical.MechanicalDimensionKind;
import dev.rgcb.scholar.mechanical.MechanicalConstraintKind;
import dev.rgcb.scholar.mechanical.editor.MechanicalDiagramEditor;
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.layout.TextMeasurer;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class EditorSession {
    private final DocumentEditor editor;
    private final MathExpressionEditor mathEditor;
    private final TableEditor tableEditor;
    private final PlotEditor plotEditor;
    private final DiagramEditor diagramEditor;
    private final ElectricalDiagramEditor electricalDiagramEditor;
    private final MechanicalDiagramEditor mechanicalDiagramEditor;
    private final MathPlainTextImporter mathPlainTextImporter;
    private final MathPlainTextSerializer mathPlainTextSerializer;
    private final TableTsvSerializer tableTsvSerializer;
    private final PlotPlainTextSerializer plotPlainTextSerializer;
    private final DiagramPlainTextSerializer diagramPlainTextSerializer;
    private final PlainTextClipboard clipboard;
    private final EditorHistory history;
    private final CaretGeometryResolver caretGeometryResolver = new CaretGeometryResolver();
    private final VisualLineNavigator visualLineNavigator = new VisualLineNavigator();
    private final TableCaretGeometryResolver tableCaretGeometryResolver = new TableCaretGeometryResolver();
    private final TableCellTextNavigator tableCellTextNavigator = new TableCellTextNavigator();
    private Optional<Integer> preferredCaretX = Optional.empty();
    private DiagramDragInteraction diagramDragInteraction;
    private DiagramPortTarget diagramConnectionSource;
    private DiagramElementTarget mechanicalConstraintSource;
    private MechanicalConstraintKind pendingMechanicalConstraintKind;

    public EditorSession(Document initialDocument, int initialBlockIndex) {
        this(new DocumentEditor(), initialDocument, initialBlockIndex);
    }

    public EditorSession(DocumentEditor editor, Document initialDocument, int initialBlockIndex) {
        this.editor = Objects.requireNonNull(editor, "editor");
        mathEditor = new MathExpressionEditor();
        tableEditor = new TableEditor();
        plotEditor = new PlotEditor();
        diagramEditor = new DiagramEditor();
        electricalDiagramEditor = new ElectricalDiagramEditor();
        mechanicalDiagramEditor = new MechanicalDiagramEditor();
        mathPlainTextImporter = new MathPlainTextImporter();
        mathPlainTextSerializer = new MathPlainTextSerializer();
        tableTsvSerializer = new TableTsvSerializer();
        plotPlainTextSerializer = new PlotPlainTextSerializer();
        diagramPlainTextSerializer = new DiagramPlainTextSerializer();
        clipboard = new PlainTextClipboard(editor);
        history = new EditorHistory(editor.initialState(initialDocument, initialBlockIndex));
    }

    public EditorState current() {
        return history.current();
    }

    public boolean canUndo() {
        return history.canUndo();
    }

    public boolean canRedo() {
        return history.canRedo();
    }

    public void setCurrent(EditorState state) {
        clearPreferredCaretX();
        diagramDragInteraction = null;
        diagramConnectionSource = null;
        mechanicalConstraintSource = null;
        pendingMechanicalConstraintKind = null;
        history.setCurrent(state);
    }

    public boolean typeText(String text) {
        Objects.requireNonNull(text, "text");
        clearPreferredCaretX();
        if (current().isEquationEditingSelection()) {
            var result = mathEditor.insertText(currentEquation().expression(), current().equationEditingSelection().selection(), text);
            if ("^".equals(text) || "_".equals(text)) {
                if (result.changed()) {
                    return applyStructuralMathEdit(result);
                }
                if (!result.selection().equals(current().equationEditingSelection().selection())) {
                    setEquationEditingSelection(result.selection());
                }
                return false;
            }
            return applyMathEdit(result, text);
        }
        if (current().isTableEditingSelection()) {
            return applyTableTyping(tableEditor.insertText(
                    currentTable(),
                    current().tableEditingSelection().selection(),
                    text,
                    tableMarksForReplacement()),
                    text);
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            history.closeTypingTransaction();
            return false;
        }
        return history.applyTyping(editor.insertText(current(), text, editor.marksForReplacement(current())), text);
    }

    public boolean enter() {
        clearPreferredCaretX();
        if (current().isEquationEditingSelection()) {
            return false;
        }
        if (current().isBlockSelection() && current().document().blocks().get(current().blockSelection().blockIndex()) instanceof EquationBlock) {
            history.setCurrent(current().editEquation(current().blockSelection().blockIndex(), mathEditor.rootStart()));
            return false;
        }
        if (current().isBlockSelection() && current().document().blocks().get(current().blockSelection().blockIndex()) instanceof TableBlock tableBlock) {
            history.setCurrent(current().editTable(current().blockSelection().blockIndex(), tableEditor.firstCellStart(tableBlock)));
            return false;
        }
        if (current().isBlockSelection() && current().document().blocks().get(current().blockSelection().blockIndex()) instanceof PlotBlock plotBlock) {
            history.setCurrent(current().editPlot(current().blockSelection().blockIndex(), plotEditor.firstTarget(plotBlock)));
            return false;
        }
        if (current().isBlockSelection() && current().document().blocks().get(current().blockSelection().blockIndex()) instanceof DiagramBlock diagramBlock) {
            history.setCurrent(current().editDiagram(current().blockSelection().blockIndex(), diagramEditor.firstTarget(diagramBlock)));
            return false;
        }
        if (current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            history.closeTypingTransaction();
            return false;
        }
        return history.applyEdit(editor.insertParagraphBreak(current(), editor.marksForReplacement(current())));
    }

    public boolean deleteBackward() {
        clearPreferredCaretX();
        if (current().isEquationEditingSelection()) {
            return applyMathEdit(mathEditor.deleteBackward(currentEquation().expression(), current().equationEditingSelection().selection()), "");
        }
        if (current().isTableEditingSelection()) {
            return applyEditOrSelectionMove(applyTableEditResult(tableEditor.deleteBackward(currentTable(), current().tableEditingSelection().selection())));
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            history.closeTypingTransaction();
            return false;
        }
        return applyEditOrSelectionMove(clearTypingMarks(editor.deleteBackward(current())));
    }

    public boolean deleteForward() {
        clearPreferredCaretX();
        if (current().isEquationEditingSelection()) {
            return applyMathEdit(mathEditor.deleteForward(currentEquation().expression(), current().equationEditingSelection().selection()), "");
        }
        if (current().isTableEditingSelection()) {
            return applyEditOrSelectionMove(applyTableEditResult(tableEditor.deleteForward(currentTable(), current().tableEditingSelection().selection())));
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            history.closeTypingTransaction();
            return false;
        }
        return applyEditOrSelectionMove(clearTypingMarks(editor.deleteForward(current())));
    }

    public void moveLeft() {
        clearPreferredCaretX();
        if (current().isEquationEditingSelection()) {
            moveMathLeft();
            return;
        }
        if (current().isTableEditingSelection()) {
            setCurrent(current().editTable(
                    current().tableEditingSelection().blockIndex(),
                    tableEditor.moveLeft(currentTable(), current().tableEditingSelection().selection())));
            return;
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            history.closeTypingTransaction();
            return;
        }
        setCurrent(editor.moveLeft(current()).editorState());
    }

    public void moveRight() {
        clearPreferredCaretX();
        if (current().isEquationEditingSelection()) {
            moveMathRight();
            return;
        }
        if (current().isTableEditingSelection()) {
            setCurrent(current().editTable(
                    current().tableEditingSelection().blockIndex(),
                    tableEditor.moveRight(currentTable(), current().tableEditingSelection().selection())));
            return;
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            history.closeTypingTransaction();
            return;
        }
        setCurrent(editor.moveRight(current()).editorState());
    }

    public void extendLeft() {
        clearPreferredCaretX();
        if (current().isEquationEditingSelection()) {
            extendMathLeft();
            return;
        }
        if (current().isTableEditingSelection()) {
            history.setCurrent(current().editTable(
                    current().tableEditingSelection().blockIndex(),
                    tableEditor.extendLeft(currentTable(), current().tableEditingSelection().selection())));
            return;
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            history.closeTypingTransaction();
            return;
        }
        setCurrent(editor.extendLeft(current()));
    }

    public void extendRight() {
        clearPreferredCaretX();
        if (current().isEquationEditingSelection()) {
            extendMathRight();
            return;
        }
        if (current().isTableEditingSelection()) {
            history.setCurrent(current().editTable(
                    current().tableEditingSelection().blockIndex(),
                    tableEditor.extendRight(currentTable(), current().tableEditingSelection().selection())));
            return;
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            history.closeTypingTransaction();
            return;
        }
        setCurrent(editor.extendRight(current()));
    }

    public void moveUp(LaidOutDocument laidOutDocument, TextMeasurer textMeasurer) {
        moveVertically(laidOutDocument, textMeasurer, true, false);
    }

    public void moveDown(LaidOutDocument laidOutDocument, TextMeasurer textMeasurer) {
        moveVertically(laidOutDocument, textMeasurer, false, false);
    }

    public void extendUp(LaidOutDocument laidOutDocument, TextMeasurer textMeasurer) {
        moveVertically(laidOutDocument, textMeasurer, true, true);
    }

    public void extendDown(LaidOutDocument laidOutDocument, TextMeasurer textMeasurer) {
        moveVertically(laidOutDocument, textMeasurer, false, true);
    }

    public void moveHome(LaidOutDocument laidOutDocument) {
        moveToVisualLineBoundary(laidOutDocument, true, false);
    }

    public void moveEnd(LaidOutDocument laidOutDocument) {
        moveToVisualLineBoundary(laidOutDocument, false, false);
    }

    public void extendHome(LaidOutDocument laidOutDocument) {
        moveToVisualLineBoundary(laidOutDocument, true, true);
    }

    public void extendEnd(LaidOutDocument laidOutDocument) {
        moveToVisualLineBoundary(laidOutDocument, false, true);
    }

    public void clearPreferredCaretX() {
        preferredCaretX = Optional.empty();
    }

    public void moveNextTableCell() {
        clearPreferredCaretX();
        if (!current().isTableEditingSelection()) {
            return;
        }
        history.setCurrent(current().editTable(
                current().tableEditingSelection().blockIndex(),
                tableEditor.moveNextCell(currentTable(), current().tableEditingSelection().selection())));
    }

    public void movePreviousTableCell() {
        clearPreferredCaretX();
        if (!current().isTableEditingSelection()) {
            return;
        }
        history.setCurrent(current().editTable(
                current().tableEditingSelection().blockIndex(),
                tableEditor.movePreviousCell(currentTable(), current().tableEditingSelection().selection())));
    }

    public boolean supportsInsertTableRow() {
        return current().isTableEditingSelection();
    }

    public boolean supportsDeleteTableRow() {
        return current().isTableEditingSelection() && tableEditor.canDeleteRow(currentTable());
    }

    public boolean supportsInsertTableColumn() {
        return current().isTableEditingSelection();
    }

    public boolean supportsDeleteTableColumn() {
        return current().isTableEditingSelection() && tableEditor.canDeleteColumn(currentTable());
    }

    public boolean insertTableRowAbove() {
        clearPreferredCaretX();
        if (!supportsInsertTableRow()) {
            return false;
        }
        return history.applyEdit(applyTableEditResult(tableEditor.insertRowAbove(
                currentTable(),
                current().tableEditingSelection().selection())));
    }

    public boolean insertTableRowBelow() {
        clearPreferredCaretX();
        if (!supportsInsertTableRow()) {
            return false;
        }
        return history.applyEdit(applyTableEditResult(tableEditor.insertRowBelow(
                currentTable(),
                current().tableEditingSelection().selection())));
    }

    public boolean deleteTableRow() {
        clearPreferredCaretX();
        if (!supportsDeleteTableRow()) {
            return false;
        }
        return history.applyEdit(applyTableEditResult(tableEditor.deleteRow(
                currentTable(),
                current().tableEditingSelection().selection())));
    }

    public boolean insertTableColumnLeft() {
        clearPreferredCaretX();
        if (!supportsInsertTableColumn()) {
            return false;
        }
        return history.applyEdit(applyTableEditResult(tableEditor.insertColumnLeft(
                currentTable(),
                current().tableEditingSelection().selection())));
    }

    public boolean insertTableColumnRight() {
        clearPreferredCaretX();
        if (!supportsInsertTableColumn()) {
            return false;
        }
        return history.applyEdit(applyTableEditResult(tableEditor.insertColumnRight(
                currentTable(),
                current().tableEditingSelection().selection())));
    }

    public boolean deleteTableColumn() {
        clearPreferredCaretX();
        if (!supportsDeleteTableColumn()) {
            return false;
        }
        return history.applyEdit(applyTableEditResult(tableEditor.deleteColumn(
                currentTable(),
                current().tableEditingSelection().selection())));
    }

    public void exitTableEditing() {
        clearPreferredCaretX();
        if (current().isTableEditingSelection()) {
            history.setCurrent(current().selectBlock(current().tableEditingSelection().blockIndex()));
        }
    }

    public void moveNextPlotTarget() {
        clearPreferredCaretX();
        if (!current().isPlotEditingSelection()) {
            return;
        }
        history.setCurrent(current().editPlot(
                current().plotEditingSelection().blockIndex(),
                plotEditor.nextTarget(currentPlot(), current().plotEditingSelection().target())));
    }

    public void movePreviousPlotTarget() {
        clearPreferredCaretX();
        if (!current().isPlotEditingSelection()) {
            return;
        }
        history.setCurrent(current().editPlot(
                current().plotEditingSelection().blockIndex(),
                plotEditor.previousTarget(currentPlot(), current().plotEditingSelection().target())));
    }

    public void setPlotEditingTarget(PlotEditTarget target) {
        Objects.requireNonNull(target, "target");
        clearPreferredCaretX();
        if (!current().isPlotEditingSelection()) {
            return;
        }
        plotEditor.validateSelection(currentPlot(), target);
        history.setCurrent(current().editPlot(current().plotEditingSelection().blockIndex(), target));
    }

    public void enterPlotEditing(int blockIndex, PlotEditTarget target) {
        Objects.requireNonNull(target, "target");
        clearPreferredCaretX();
        if (blockIndex < 0 || blockIndex >= current().document().blocks().size()) {
            return;
        }
        if (!(current().document().blocks().get(blockIndex) instanceof PlotBlock plotBlock)) {
            return;
        }
        plotEditor.validateSelection(plotBlock, target);
        history.setCurrent(current().editPlot(blockIndex, target));
    }

    public void exitPlotEditing() {
        clearPreferredCaretX();
        if (current().isPlotEditingSelection()) {
            history.setCurrent(current().selectBlock(current().plotEditingSelection().blockIndex()));
        }
    }

    public Optional<String> plotTextValue() {
        if (!current().isPlotEditingSelection()) {
            return Optional.empty();
        }
        var target = current().plotEditingSelection().target();
        if (target instanceof PlotPointTarget) {
            return Optional.empty();
        }
        return Optional.of(plotEditor.textValue(currentPlot(), target));
    }

    public Optional<DataPoint> plotPointValue() {
        if (!current().isPlotEditingSelection() || !(current().plotEditingSelection().target() instanceof PlotPointTarget pointTarget)) {
            return Optional.empty();
        }
        return Optional.of(currentPlot().definition().series().get(pointTarget.seriesIndex()).points().get(pointTarget.pointIndex()));
    }

    public boolean applyPlotText(String text) {
        Objects.requireNonNull(text, "text");
        clearPreferredCaretX();
        if (!current().isPlotEditingSelection()) {
            return false;
        }
        return history.applyEdit(applyPlotEditResult(plotEditor.setText(currentPlot(), current().plotEditingSelection().target(), text)));
    }

    public boolean applyPlotPoint(double x, double y) {
        clearPreferredCaretX();
        if (!current().isPlotEditingSelection() || !(current().plotEditingSelection().target() instanceof PlotPointTarget pointTarget)) {
            return false;
        }
        return history.applyEdit(applyPlotEditResult(plotEditor.setPoint(currentPlot(), pointTarget, x, y)));
    }

    public boolean supportsPlotEditingAction() {
        return current().isPlotEditingSelection();
    }

    public boolean togglePlotGrid() {
        return applyPlotMutation(() -> plotEditor.toggleGrid(currentPlot(), current().plotEditingSelection().target()));
    }

    public boolean togglePlotLegend() {
        return applyPlotMutation(() -> plotEditor.toggleLegend(currentPlot(), current().plotEditingSelection().target()));
    }

    public boolean addPlotSeries(PlotSeriesKind kind) {
        Objects.requireNonNull(kind, "kind");
        return applyPlotMutation(() -> plotEditor.addSeries(currentPlot(), current().plotEditingSelection().target(), kind));
    }

    public boolean supportsDeletePlotSeries() {
        return current().isPlotEditingSelection() && plotEditor.canDeleteSeries(currentPlot(), current().plotEditingSelection().target());
    }

    public boolean deletePlotSeries() {
        if (!supportsDeletePlotSeries()) {
            return false;
        }
        return applyPlotMutation(() -> plotEditor.deleteSeries(currentPlot(), current().plotEditingSelection().target()));
    }

    public boolean supportsSetPlotSeriesKind() {
        return current().isPlotEditingSelection() && plotEditor.canSetSeriesKind(currentPlot(), current().plotEditingSelection().target());
    }

    public boolean setPlotSeriesKind(PlotSeriesKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (!supportsSetPlotSeriesKind()) {
            return false;
        }
        return applyPlotMutation(() -> plotEditor.setSeriesKind(currentPlot(), current().plotEditingSelection().target(), kind));
    }

    public boolean supportsAddPlotPoint() {
        return current().isPlotEditingSelection() && plotEditor.canAddPoint(currentPlot(), current().plotEditingSelection().target());
    }

    public boolean addPlotPoint() {
        if (!supportsAddPlotPoint()) {
            return false;
        }
        return applyPlotMutation(() -> plotEditor.addPoint(currentPlot(), current().plotEditingSelection().target()));
    }

    public boolean supportsDeletePlotPoint() {
        return current().isPlotEditingSelection() && plotEditor.canDeletePoint(currentPlot(), current().plotEditingSelection().target());
    }

    public boolean deletePlotPoint() {
        if (!supportsDeletePlotPoint()) {
            return false;
        }
        return applyPlotMutation(() -> plotEditor.deletePoint(currentPlot(), current().plotEditingSelection().target()));
    }

    public boolean plotGridVisible() {
        return current().isPlotEditingSelection() && currentPlot().definition().gridVisible();
    }

    public boolean plotLegendVisible() {
        return current().isPlotEditingSelection() && currentPlot().definition().legendVisible();
    }

    public Optional<PlotSeriesKind> selectedPlotSeriesKind() {
        if (!current().isPlotEditingSelection()) {
            return Optional.empty();
        }
        var index = plotEditor.selectedSeriesIndex(currentPlot(), current().plotEditingSelection().target());
        if (index < 0) {
            return Optional.empty();
        }
        return Optional.of(currentPlot().definition().series().get(index).kind());
    }

    public void moveNextDiagramTarget() {
        clearPreferredCaretX();
        if (!current().isDiagramEditingSelection()) {
            return;
        }
        cancelDiagramElementDrag();
        history.setCurrent(current().editDiagram(
                current().diagramEditingSelection().blockIndex(),
                diagramEditor.nextTarget(currentDiagram(), current().diagramEditingSelection().target())));
    }

    public void movePreviousDiagramTarget() {
        clearPreferredCaretX();
        if (!current().isDiagramEditingSelection()) {
            return;
        }
        cancelDiagramElementDrag();
        history.setCurrent(current().editDiagram(
                current().diagramEditingSelection().blockIndex(),
                diagramEditor.previousTarget(currentDiagram(), current().diagramEditingSelection().target())));
    }

    public void setDiagramEditingTarget(DiagramEditTarget target) {
        Objects.requireNonNull(target, "target");
        clearPreferredCaretX();
        if (!current().isDiagramEditingSelection()) {
            return;
        }
        cancelDiagramElementDrag();
        diagramEditor.validateSelection(currentDiagram(), target);
        history.setCurrent(current().editDiagram(current().diagramEditingSelection().blockIndex(), target));
    }

    public void enterDiagramEditing(int blockIndex, DiagramEditTarget target) {
        Objects.requireNonNull(target, "target");
        clearPreferredCaretX();
        cancelDiagramElementDrag();
        diagramConnectionSource = null;
        mechanicalConstraintSource = null;
        pendingMechanicalConstraintKind = null;
        if (blockIndex < 0 || blockIndex >= current().document().blocks().size()) {
            return;
        }
        if (!(current().document().blocks().get(blockIndex) instanceof DiagramBlock diagramBlock)) {
            return;
        }
        diagramEditor.validateSelection(diagramBlock, target);
        history.setCurrent(current().editDiagram(blockIndex, target));
    }

    public void exitDiagramEditing() {
        clearPreferredCaretX();
        cancelDiagramElementDrag();
        diagramConnectionSource = null;
        mechanicalConstraintSource = null;
        pendingMechanicalConstraintKind = null;
        if (current().isDiagramEditingSelection()) {
            history.setCurrent(current().selectBlock(current().diagramEditingSelection().blockIndex()));
        }
    }

    /**
     * Starts an element drag. Pointer coordinates are logical diagram-canvas
     * coordinates, so GUI scale and document reflow do not become authored data.
     */
    public boolean beginDiagramElementDrag(double pointerLogicalX, double pointerLogicalY) {
        clearPreferredCaretX();
        diagramConnectionSource = null;
        if (hasPendingMechanicalConstraint()) {
            diagramDragInteraction = null;
            return false;
        }
        if (!Double.isFinite(pointerLogicalX) || !Double.isFinite(pointerLogicalY)
                || !current().isDiagramEditingSelection()
                || !(current().diagramEditingSelection().target() instanceof DiagramElementTarget target)) {
            diagramDragInteraction = null;
            return false;
        }
        diagramEditor.validateSelection(currentDiagram(), target);
        var element = currentDiagram().definition().elements().get(target.elementIndex());
        if (element instanceof dev.rgcb.scholar.mechanical.MechanicalConstraint) {
            diagramDragInteraction = null;
            return false;
        }
        var bounds = element.bounds();
        diagramDragInteraction = new DiagramDragInteraction(
                current().diagramEditingSelection().blockIndex(),
                target,
                pointerLogicalX - bounds.x(),
                pointerLogicalY - bounds.y());
        history.closeTypingTransaction();
        return true;
    }

    /**
     * Produces a transient semantic preview document without changing
     * EditorHistory or the current semantic document.
     */
    public Optional<Document> previewDiagramElementDrag(double pointerLogicalX, double pointerLogicalY) {
        if (!Double.isFinite(pointerLogicalX) || !Double.isFinite(pointerLogicalY)
                || diagramDragInteraction == null
                || !current().isDiagramEditingSelection()) {
            return Optional.empty();
        }
        var drag = diagramDragInteraction;
        if (drag.blockIndex() != current().diagramEditingSelection().blockIndex()) {
            diagramDragInteraction = null;
            return Optional.empty();
        }
        var result = diagramEditor.moveElement(
                currentDiagram(),
                drag.target(),
                pointerLogicalX - drag.pointerOffsetX(),
                pointerLogicalY - drag.pointerOffsetY());
        if (result.changed()) {
            var reconciled = mechanicalDiagramEditor.reconcileConstraints(result.diagram());
            result = new DiagramEditResult(reconciled, result.target(), true);
        }
        return Optional.of(documentWithDiagramEdit(result));
    }

    /** Commits the final dragged element position as one global history edit. */
    public boolean commitDiagramElementDrag(double pointerLogicalX, double pointerLogicalY) {
        if (!Double.isFinite(pointerLogicalX) || !Double.isFinite(pointerLogicalY)
                || diagramDragInteraction == null
                || !current().isDiagramEditingSelection()) {
            diagramDragInteraction = null;
            return false;
        }
        var drag = diagramDragInteraction;
        diagramDragInteraction = null;
        if (drag.blockIndex() != current().diagramEditingSelection().blockIndex()) {
            return false;
        }
        var result = diagramEditor.moveElement(
                currentDiagram(),
                drag.target(),
                pointerLogicalX - drag.pointerOffsetX(),
                pointerLogicalY - drag.pointerOffsetY());
        if (result.changed()) {
            var reconciled = mechanicalDiagramEditor.reconcileConstraints(result.diagram());
            result = new DiagramEditResult(reconciled, result.target(), true);
        }
        return history.applyEdit(applyDiagramEditResult(result));
    }

    /** Cancels only transient drag interaction; semantic document/history stay untouched. */
    public boolean cancelDiagramElementDrag() {
        var active = diagramDragInteraction != null;
        diagramDragInteraction = null;
        return active;
    }

    public boolean isDiagramElementDragging() {
        return diagramDragInteraction != null;
    }

    public Optional<String> diagramTextValue() {
        if (!current().isDiagramEditingSelection()) {
            return Optional.empty();
        }
        var target = current().diagramEditingSelection().target();
        var partReference=mechanicalDiagramEditor.selectedPartReference(currentDiagram(),target);
        if(partReference.isPresent())return Optional.of(partReference.orElseThrow().partName());
        var annotation=mechanicalDiagramEditor.selectedAnnotation(currentDiagram(),target);
        if(annotation.isPresent())return Optional.of(annotation.orElseThrow().text());
        var junctionLabel = electricalDiagramEditor.junctionNetLabel(currentDiagram(), target);
        if (junctionLabel.isPresent()) {
            return junctionLabel;
        }
        if (!diagramEditor.canEditText(currentDiagram(), target)) {
            return Optional.empty();
        }
        return Optional.of(diagramEditor.textValue(currentDiagram(), target));
    }

    public boolean applyDiagramText(String text) {
        Objects.requireNonNull(text, "text");
        clearPreferredCaretX();
        if (!current().isDiagramEditingSelection()) {
            return false;
        }
        diagramConnectionSource = null;
        var target = current().diagramEditingSelection().target();
        if(mechanicalDiagramEditor.selectedPartReference(currentDiagram(),target).isPresent())return history.applyEdit(applyDiagramEditResult(mechanicalDiagramEditor.setPartReferenceName(currentDiagram(),target,text)));
        if(mechanicalDiagramEditor.selectedAnnotation(currentDiagram(),target).isPresent())return history.applyEdit(applyDiagramEditResult(mechanicalDiagramEditor.setAnnotationText(currentDiagram(),target,text)));
        if (electricalDiagramEditor.selectedJunction(currentDiagram(), target).isPresent()) {
            return history.applyEdit(applyDiagramEditResult(
                    electricalDiagramEditor.setJunctionNetLabel(currentDiagram(), target, text)));
        }
        return history.applyEdit(applyDiagramEditResult(
                diagramEditor.setText(currentDiagram(), target, text)));
    }

    public boolean supportsDiagramEditingAction() {
        return current().isDiagramEditingSelection();
    }

    public Optional<DiagramCanvas> diagramCanvas() {
        if (!current().isDiagramEditingSelection()) {
            return Optional.empty();
        }
        return Optional.of(currentDiagram().definition().canvas());
    }

    public boolean resizeDiagramCanvas(double width, double height) {
        if (!supportsDiagramEditingAction()) {
            return false;
        }
        clearPreferredCaretX();
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                diagramEditor.resizeCanvas(
                        currentDiagram(), current().diagramEditingSelection().target(), width, height)));
    }

    public boolean scaleDiagramWorkspaceHeight(double factor) {
        if (!supportsDiagramEditingAction()) {
            return false;
        }
        clearPreferredCaretX();
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                diagramEditor.scaleWorkspaceHeight(
                        currentDiagram(), current().diagramEditingSelection().target(), factor)));
    }

    public boolean resetDiagramWorkspaceHeight() {
        if (!supportsDiagramEditingAction()) {
            return false;
        }
        clearPreferredCaretX();
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                diagramEditor.resetWorkspaceHeight(
                        currentDiagram(), current().diagramEditingSelection().target())));
    }

    public boolean supportsScaleElectricalSymbols() {
        return current().isDiagramEditingSelection()
                && electricalDiagramEditor.canScaleAllComponents(currentDiagram());
    }

    public boolean scaleElectricalSymbols(double factor) {
        if (!supportsScaleElectricalSymbols()) {
            return false;
        }
        clearPreferredCaretX();
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                electricalDiagramEditor.scaleAllComponents(
                        currentDiagram(), current().diagramEditingSelection().target(), factor)));
    }

    public boolean addDiagramNode() {
        if (!supportsDiagramEditingAction()) {
            return false;
        }
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                diagramEditor.addNode(currentDiagram(), current().diagramEditingSelection().target())));
    }

    public boolean supportsDeleteDiagramNode() {
        return current().isDiagramEditingSelection()
                && diagramEditor.canDeleteNode(currentDiagram(), current().diagramEditingSelection().target());
    }

    public boolean deleteDiagramNode() {
        if (!supportsDeleteDiagramNode()) {
            return false;
        }
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                diagramEditor.deleteNode(currentDiagram(), current().diagramEditingSelection().target())));
    }

    public boolean addMechanicalPrimitive(MechanicalPrimitiveKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (!supportsDiagramEditingAction()) {
            return false;
        }
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                mechanicalDiagramEditor.addPrimitive(
                        currentDiagram(), current().diagramEditingSelection().target(), kind)));
    }

    public boolean supportsAddMechanicalPartReference(){return current().isDiagramEditingSelection()&&mechanicalDiagramEditor.canAddPartReference(currentDiagram(),current().diagramEditingSelection().target());}
    public boolean addMechanicalPartReference(){if(!supportsAddMechanicalPartReference())return false;return history.applyEdit(applyDiagramEditResult(mechanicalDiagramEditor.addPartReference(currentDiagram(),current().diagramEditingSelection().target())));}
    public boolean supportsDeleteMechanicalPartReference(){return current().isDiagramEditingSelection()&&mechanicalDiagramEditor.canDeletePartReference(currentDiagram(),current().diagramEditingSelection().target());}
    public boolean deleteMechanicalPartReference(){if(!supportsDeleteMechanicalPartReference())return false;cancelDiagramElementDrag();return history.applyEdit(applyDiagramEditResult(mechanicalDiagramEditor.deletePartReference(currentDiagram(),current().diagramEditingSelection().target())));}
    public boolean supportsGenerateMechanicalBom(){return current().isDiagramEditingSelection()&&currentDiagram().definition().elements().stream().anyMatch(MechanicalPartReference.class::isInstance);}
    public boolean generateMechanicalBom(){
        if(!supportsGenerateMechanicalBom())return false;var refs=currentDiagram().definition().elements().stream().filter(MechanicalPartReference.class::isInstance).map(MechanicalPartReference.class::cast).sorted(java.util.Comparator.comparingInt(MechanicalPartReference::itemNumber)).toList();
        var rows=new java.util.ArrayList<TableRow>();rows.add(bomRow("ITEM","PART","QTY","DESCRIPTION"));for(var ref:refs)rows.add(bomRow(Integer.toString(ref.itemNumber()),ref.partName(),Integer.toString(ref.quantity()),ref.description()));
        var table=new TableBlock(rows,1);var blockIndex=current().diagramEditingSelection().blockIndex();var blocks=new java.util.ArrayList<>(current().document().blocks());blocks.add(blockIndex+1,table);var document=new Document(blocks);return history.applyEdit(new EditResult(document,new BlockSelection(blockIndex+1),Optional.empty(),true));
    }
    private static TableRow bomRow(String item,String part,String qty,String description){return new TableRow(java.util.List.of(bomCell(item),bomCell(part),bomCell(qty),bomCell(description)));}
    private static TableCell bomCell(String text){return new TableCell(new TableCellContent(new InlineContent(java.util.List.of((InlineNode)new Text(text,Set.of())))));}

    public boolean addMechanicalAnnotation(MechanicalAnnotationKind kind){if(!supportsDiagramEditingAction())return false;return history.applyEdit(applyDiagramEditResult(mechanicalDiagramEditor.addAnnotation(currentDiagram(),current().diagramEditingSelection().target(),kind)));}
    public boolean supportsDeleteMechanicalAnnotation(){return current().isDiagramEditingSelection()&&mechanicalDiagramEditor.canDeleteAnnotation(currentDiagram(),current().diagramEditingSelection().target());}
    public boolean deleteMechanicalAnnotation(){if(!supportsDeleteMechanicalAnnotation())return false;cancelDiagramElementDrag();return history.applyEdit(applyDiagramEditResult(mechanicalDiagramEditor.deleteAnnotation(currentDiagram(),current().diagramEditingSelection().target())));}

    public boolean addMechanicalSymbol(MechanicalSymbolKind kind) {
        Objects.requireNonNull(kind); if(!supportsDiagramEditingAction()) return false; diagramConnectionSource=null; cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(mechanicalDiagramEditor.addSymbol(currentDiagram(), current().diagramEditingSelection().target(), kind)));
    }

    public boolean supportsDeleteMechanicalSymbol() { return current().isDiagramEditingSelection() && mechanicalDiagramEditor.canDeleteSymbol(currentDiagram(),current().diagramEditingSelection().target()); }
    public boolean deleteMechanicalSymbol() { if(!supportsDeleteMechanicalSymbol()) return false; cancelDiagramElementDrag(); return history.applyEdit(applyDiagramEditResult(mechanicalDiagramEditor.deleteSymbol(currentDiagram(),current().diagramEditingSelection().target()))); }

    public boolean addMechanicalDimension(MechanicalDimensionKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (!supportsDiagramEditingAction()) return false;
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                mechanicalDiagramEditor.addDimension(
                        currentDiagram(), current().diagramEditingSelection().target(), kind)));
    }

    public boolean supportsAddMechanicalUnaryConstraint(MechanicalConstraintKind kind) {
        return current().isDiagramEditingSelection()
                && mechanicalDiagramEditor.canApplyUnaryConstraint(
                        currentDiagram(), current().diagramEditingSelection().target(), kind);
    }

    public boolean addMechanicalUnaryConstraint(MechanicalConstraintKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (!supportsAddMechanicalUnaryConstraint(kind)) return false;
        diagramConnectionSource = null;
        mechanicalConstraintSource = null;
        pendingMechanicalConstraintKind = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                mechanicalDiagramEditor.addUnaryConstraint(
                        currentDiagram(), current().diagramEditingSelection().target(), kind)));
    }

    public boolean supportsStartMechanicalConstraint(MechanicalConstraintKind kind) {
        return current().isDiagramEditingSelection()
                && mechanicalDiagramEditor.canStartBinaryConstraint(
                        currentDiagram(), current().diagramEditingSelection().target(), kind);
    }

    public boolean startMechanicalConstraint(MechanicalConstraintKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (!supportsStartMechanicalConstraint(kind)
                || !(current().diagramEditingSelection().target() instanceof DiagramElementTarget source)) {
            return false;
        }
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        mechanicalConstraintSource = source;
        pendingMechanicalConstraintKind = kind;
        return true;
    }

    public boolean supportsFinishMechanicalConstraint() {
        return current().isDiagramEditingSelection()
                && mechanicalConstraintSource != null
                && pendingMechanicalConstraintKind != null
                && mechanicalDiagramEditor.canFinishBinaryConstraint(
                        currentDiagram(),
                        mechanicalConstraintSource,
                        current().diagramEditingSelection().target(),
                        pendingMechanicalConstraintKind);
    }

    public boolean finishMechanicalConstraint() {
        if (!supportsFinishMechanicalConstraint()) return false;
        var source = mechanicalConstraintSource;
        var kind = pendingMechanicalConstraintKind;
        mechanicalConstraintSource = null;
        pendingMechanicalConstraintKind = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                mechanicalDiagramEditor.addBinaryConstraint(
                        currentDiagram(), source, current().diagramEditingSelection().target(), kind)));
    }

    public boolean cancelMechanicalConstraint() {
        var active = mechanicalConstraintSource != null || pendingMechanicalConstraintKind != null;
        mechanicalConstraintSource = null;
        pendingMechanicalConstraintKind = null;
        return active;
    }

    public boolean hasPendingMechanicalConstraint() {
        return mechanicalConstraintSource != null && pendingMechanicalConstraintKind != null;
    }

    public boolean supportsDeleteMechanicalConstraint() {
        return current().isDiagramEditingSelection()
                && mechanicalDiagramEditor.canDeleteConstraint(
                        currentDiagram(), current().diagramEditingSelection().target());
    }

    public boolean deleteMechanicalConstraint() {
        if (!supportsDeleteMechanicalConstraint()) return false;
        mechanicalConstraintSource = null;
        pendingMechanicalConstraintKind = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                mechanicalDiagramEditor.deleteConstraint(
                        currentDiagram(), current().diagramEditingSelection().target())));
    }

    public boolean supportsDeleteMechanicalDimension() {
        return current().isDiagramEditingSelection()
                && mechanicalDiagramEditor.canDeleteDimension(
                        currentDiagram(), current().diagramEditingSelection().target());
    }

    public boolean deleteMechanicalDimension() {
        if (!supportsDeleteMechanicalDimension()) return false;
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                mechanicalDiagramEditor.deleteDimension(
                        currentDiagram(), current().diagramEditingSelection().target())));
    }

    public boolean supportsDeleteMechanicalPrimitive() {
        return current().isDiagramEditingSelection()
                && mechanicalDiagramEditor.canDelete(
                        currentDiagram(), current().diagramEditingSelection().target());
    }

    public boolean deleteMechanicalPrimitive() {
        if (!supportsDeleteMechanicalPrimitive()) {
            return false;
        }
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                mechanicalDiagramEditor.deletePrimitive(
                        currentDiagram(), current().diagramEditingSelection().target())));
    }

    public boolean addElectricalJunction() {
        if (!supportsDiagramEditingAction()) {
            return false;
        }
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                electricalDiagramEditor.addJunction(
                        currentDiagram(), current().diagramEditingSelection().target())));
    }

    public boolean supportsDeleteElectricalJunction() {
        return current().isDiagramEditingSelection()
                && electricalDiagramEditor.canDeleteJunction(
                        currentDiagram(), current().diagramEditingSelection().target());
    }

    public boolean deleteElectricalJunction() {
        if (!supportsDeleteElectricalJunction()) {
            return false;
        }
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                electricalDiagramEditor.deleteJunction(
                        currentDiagram(), current().diagramEditingSelection().target())));
    }

    public boolean addElectricalComponent(ElectricalComponentKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (!supportsDiagramEditingAction()) {
            return false;
        }
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                electricalDiagramEditor.addComponent(
                        currentDiagram(), current().diagramEditingSelection().target(), kind)));
    }

    public Optional<ElectricalComponent> selectedElectricalComponent() {
        if (!current().isDiagramEditingSelection()) {
            return Optional.empty();
        }
        return electricalDiagramEditor.selectedComponent(
                currentDiagram(), current().diagramEditingSelection().target());
    }

    public Optional<ElectricalComponentDraft> electricalComponentDraft() {
        if (!current().isDiagramEditingSelection()) {
            return Optional.empty();
        }
        return electricalDiagramEditor.draft(
                currentDiagram(), current().diagramEditingSelection().target());
    }

    public boolean applyElectricalComponentAnnotations(String referenceDesignator, String valueLabel) {
        Objects.requireNonNull(referenceDesignator, "referenceDesignator");
        Objects.requireNonNull(valueLabel, "valueLabel");
        if (!current().isDiagramEditingSelection()) {
            return false;
        }
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                electricalDiagramEditor.setAnnotations(
                        currentDiagram(),
                        current().diagramEditingSelection().target(),
                        referenceDesignator,
                        valueLabel)));
    }

    public boolean supportsRotateElectricalComponent() {
        return current().isDiagramEditingSelection()
                && electricalDiagramEditor.canRotate(
                        currentDiagram(), current().diagramEditingSelection().target());
    }

    public boolean rotateElectricalComponentClockwise() {
        if (!supportsRotateElectricalComponent()) {
            return false;
        }
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                electricalDiagramEditor.rotateClockwise(
                        currentDiagram(), current().diagramEditingSelection().target())));
    }

    public boolean rotateElectricalComponentCounterClockwise() {
        if (!supportsRotateElectricalComponent()) {
            return false;
        }
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                electricalDiagramEditor.rotateCounterClockwise(
                        currentDiagram(), current().diagramEditingSelection().target())));
    }

    public boolean supportsDeleteElectricalComponent() {
        return current().isDiagramEditingSelection()
                && electricalDiagramEditor.canDelete(
                        currentDiagram(), current().diagramEditingSelection().target());
    }

    public boolean deleteElectricalComponent() {
        if (!supportsDeleteElectricalComponent()) {
            return false;
        }
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                electricalDiagramEditor.deleteComponent(
                        currentDiagram(), current().diagramEditingSelection().target())));
    }

    public boolean supportsBeginDiagramConnection() {
        return current().isDiagramEditingSelection()
                && diagramConnectionSource == null
                && current().diagramEditingSelection().target() instanceof DiagramPortTarget;
    }

    /** Arms the currently selected port as the explicit source endpoint. */
    public boolean beginDiagramConnection() {
        if (!supportsBeginDiagramConnection()) {
            return false;
        }
        diagramConnectionSource = (DiagramPortTarget) current().diagramEditingSelection().target();
        cancelDiagramElementDrag();
        return true;
    }

    public boolean diagramConnectionInProgress() {
        return diagramConnectionSource != null;
    }

    public Optional<DiagramPortTarget> diagramConnectionSourceTarget() {
        return Optional.ofNullable(diagramConnectionSource);
    }

    public boolean supportsCompleteDiagramConnection() {
        if (!current().isDiagramEditingSelection()
                || diagramConnectionSource == null
                || !(current().diagramEditingSelection().target() instanceof DiagramPortTarget target)) {
            return false;
        }
        return diagramEditor.canAddConnection(currentDiagram(), diagramConnectionSource, target);
    }

    /** Commits a complete source/target pair; incomplete connections never enter the AST. */
    public boolean completeDiagramConnection() {
        if (!supportsCompleteDiagramConnection()) {
            return false;
        }
        var source = diagramConnectionSource;
        var target = (DiagramPortTarget) current().diagramEditingSelection().target();
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                diagramEditor.addConnection(currentDiagram(), source, target)));
    }

    public boolean cancelDiagramConnection() {
        var active = diagramConnectionSource != null;
        diagramConnectionSource = null;
        return active;
    }

    public boolean supportsDeleteDiagramConnection() {
        return current().isDiagramEditingSelection()
                && diagramEditor.canDeleteConnection(currentDiagram(), current().diagramEditingSelection().target());
    }

    public boolean deleteDiagramConnection() {
        if (!supportsDeleteDiagramConnection()) {
            return false;
        }
        diagramConnectionSource = null;
        cancelDiagramElementDrag();
        return history.applyEdit(applyDiagramEditResult(
                diagramEditor.deleteConnection(currentDiagram(), current().diagramEditingSelection().target())));
    }

    Optional<Integer> preferredCaretX() {
        return preferredCaretX;
    }

    public boolean undo() {
        clearPreferredCaretX();
        diagramDragInteraction = null;
        diagramConnectionSource = null;
        mechanicalConstraintSource = null;
        pendingMechanicalConstraintKind = null;
        return history.undo();
    }

    public boolean redo() {
        clearPreferredCaretX();
        diagramDragInteraction = null;
        diagramConnectionSource = null;
        mechanicalConstraintSource = null;
        pendingMechanicalConstraintKind = null;
        return history.redo();
    }

    public boolean supportsInlineFormatting() {
        if (current().isTableEditingSelection()) {
            return true;
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return false;
        }
        return editor.supportsInlineFormatting(current());
    }

    public boolean supportsBlockStyle() {
        if (current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return false;
        }
        return editor.supportsBlockStyle(current());
    }

    public Optional<BlockStyle> blockStyle() {
        var state = blockStyleSelectionState();
        return state.kind() == BlockStyleSelectionState.Kind.SINGLE ? state.style() : Optional.empty();
    }

    public BlockStyleSelectionState blockStyleSelectionState() {
        if (current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return BlockStyleSelectionState.notApplicable();
        }
        return editor.blockStyleSelectionState(current());
    }

    public boolean setBlockStyle(BlockStyle style) {
        Objects.requireNonNull(style, "style");
        clearPreferredCaretX();
        if (!supportsBlockStyle()) {
            return false;
        }
        return history.applyEdit(editor.setBlockStyle(current(), style));
    }

    public FormattingState formattingState(TextMark mark) {
        Objects.requireNonNull(mark, "mark");
        if (current().isTableEditingSelection()) {
            return tableEditor.formattingState(currentTable(), current().tableEditingSelection().selection(), mark);
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return FormattingState.NOT_APPLICABLE;
        }
        return editor.formattingState(current(), mark);
    }

    public Set<TextMark> effectiveTypingMarks() {
        if (current().isTableEditingSelection()) {
            return current().explicitTypingMarks()
                    .map(Set::copyOf)
                    .orElseGet(() -> tableEditor.marksForInsertion(currentTable(), current().tableEditingSelection().selection()));
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return Set.of();
        }
        return editor.marksForInsertion(current());
    }

    public boolean toggleMark(TextMark mark) {
        Objects.requireNonNull(mark, "mark");
        clearPreferredCaretX();
        if (!supportsInlineFormatting()) {
            return false;
        }
        if (current().isTableEditingSelection()) {
            if (!current().tableEditingSelection().selection().isCaret()) {
                return history.applyEdit(applyTableEditResult(tableEditor.toggleMark(
                        currentTable(),
                        current().tableEditingSelection().selection(),
                        mark)));
            }

            history.closeTypingTransaction();
            var marks = current().explicitTypingMarks()
                    .map(existing -> existing.isEmpty() ? EnumSet.noneOf(TextMark.class) : EnumSet.copyOf(existing))
                    .orElseGet(() -> effectiveTypingMarks().isEmpty()
                            ? EnumSet.noneOf(TextMark.class)
                            : EnumSet.copyOf(effectiveTypingMarks()));
            if (marks.contains(mark)) {
                marks.remove(mark);
            } else {
                marks.add(mark);
            }
            history.replaceCurrent(new EditorState(current().document(), current().selection(), Optional.of(Set.copyOf(marks))));
            return true;
        }
        if (current().hasSelection()) {
            return history.applyEdit(editor.toggleMark(current(), mark));
        }

        history.closeTypingTransaction();
        var marks = current().explicitTypingMarks()
                .map(existing -> existing.isEmpty() ? EnumSet.noneOf(TextMark.class) : EnumSet.copyOf(existing))
                .orElseGet(() -> effectiveTypingMarks().isEmpty()
                        ? EnumSet.noneOf(TextMark.class)
                        : EnumSet.copyOf(effectiveTypingMarks()));
        if (marks.contains(mark)) {
            marks.remove(mark);
        } else {
            marks.add(mark);
        }
        history.replaceCurrent(current().withExplicitTypingMarks(marks));
        return true;
    }

    public Optional<String> copySelection() {
        if (current().isTableEditingSelection()) {
            return tableEditor.copy(currentTable(), current().tableEditingSelection().selection());
        }
        if (current().isBlockSelection() || current().isEquationEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return Optional.empty();
        }
        return clipboard.copy(current());
    }

    public Optional<ClipboardCopyResult> copyForClipboard() {
        if (current().isBlockSelection()) {
            return copySelectedBlockForClipboard();
        }
        if (current().isEquationEditingSelection()) {
            return copyMathForClipboard();
        }
        if (current().isTableEditingSelection()) {
            return tableEditor.copy(currentTable(), current().tableEditingSelection().selection())
                    .map(ClipboardCopyResult::plainText);
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return Optional.empty();
        }
        return clipboard.copy(current()).map(ClipboardCopyResult::plainText);
    }

    public Optional<ClipboardEditResult> cutSelection() {
        if (current().isTableEditingSelection()) {
            return tableEditor.cut(new TableEditor.DocumentSource(current().document(), current().tableEditingSelection().blockIndex(), currentTable()), current().tableEditingSelection().selection());
        }
        if (current().isBlockSelection() || current().isEquationEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return Optional.empty();
        }
        return clipboard.cut(current());
    }

    public Optional<ClipboardCutResult> cutForClipboard() {
        if (current().isBlockSelection()) {
            return cutSelectedBlockForClipboard();
        }
        if (current().isEquationEditingSelection()) {
            return cutMathForClipboard();
        }
        if (current().isTableEditingSelection()) {
            return cutTableForClipboard();
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return Optional.empty();
        }
        return clipboard.cut(current())
                .map(cut -> ClipboardCutResult.plainText(cut.clipboardText(), cut.editResult()));
    }

    public boolean applyCut(ClipboardEditResult cut) {
        Objects.requireNonNull(cut, "cut");
        clearPreferredCaretX();
        return history.applyEdit(clearTypingMarks(cut.editResult()));
    }

    public boolean applyCut(ClipboardCutResult cut) {
        Objects.requireNonNull(cut, "cut");
        clearPreferredCaretX();
        return history.applyEdit(clearTypingMarks(cut.editResult()));
    }

    public boolean pasteText(String text) {
        Objects.requireNonNull(text, "text");
        clearPreferredCaretX();
        if (current().isTableEditingSelection()) {
            return pasteIntoTableCell(text)
                    .filter(result -> history.applyEdit(clearTypingMarks(applyTableEditResult(result))))
                    .isPresent();
        }
        if (current().isBlockSelection() || current().isEquationEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return false;
        }
        var result = clipboard.paste(current(), text);
        return result.map(EditorSession::clearTypingMarks).filter(history::applyEdit).isPresent();
    }

    public boolean canCopyForClipboard() {
        return copyForClipboard().isPresent();
    }

    public boolean canCutForClipboard() {
        return cutForClipboard().isPresent();
    }

    public boolean canPasteFromClipboard(Optional<ScholarClipboardPayload> payload, String text) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(text, "text");
        if (current().isEquationEditingSelection()) {
            return mathFragmentFromClipboard(payload, text)
                    .filter(mathPayload -> mathEditor.pasteFragment(
                            currentEquation().expression(),
                            current().equationEditingSelection().selection(),
                            mathPayload.fragment()).changed())
                    .isPresent();
        }
        if (current().isTableEditingSelection()) {
            return pasteIntoTableCell(text)
                    .filter(TableEditResult::changed)
                    .isPresent();
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return false;
        }
        if (tableFromClipboard(payload).isPresent()) {
            return supportsPasteTableFromClipboard();
        }
        if (plotFromClipboard(payload).isPresent()) {
            return supportsPastePlotFromClipboard();
        }
        if (diagramFromClipboard(payload).isPresent()) {
            return supportsPasteDiagramFromClipboard();
        }
        return !current().isBlockSelection();
    }

    public boolean pasteFromClipboard(Optional<ScholarClipboardPayload> payload, String text) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(text, "text");
        clearPreferredCaretX();
        if (current().isEquationEditingSelection()) {
            return mathFragmentFromClipboard(payload, text)
                    .map(mathPayload -> mathEditor.pasteFragment(
                            currentEquation().expression(),
                            current().equationEditingSelection().selection(),
                            mathPayload.fragment()))
                    .filter(result -> applyStructuralMathEdit(result))
                    .isPresent();
        }
        if (current().isTableEditingSelection()) {
            return pasteIntoTableCell(text)
                    .filter(result -> history.applyEdit(clearTypingMarks(applyTableEditResult(result))))
                    .isPresent();
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return false;
        }
        var tablePayload = tableFromClipboard(payload);
        if (tablePayload.isPresent()) {
            return pasteTableFromClipboard(tablePayload.orElseThrow().table());
        }
        var plotPayload = plotFromClipboard(payload);
        if (plotPayload.isPresent()) {
            return pastePlotFromClipboard(plotPayload.orElseThrow().plot());
        }
        var diagramPayload = diagramFromClipboard(payload);
        if (diagramPayload.isPresent()) {
            return pasteDiagramFromClipboard(diagramPayload.orElseThrow().diagram());
        }
        return pasteText(text);
    }

    public boolean insertEmptyEquation() {
        clearPreferredCaretX();
        if (!supportsInsertEquation()) {
            return false;
        }
        return history.applyEdit(editor.insertEmptyEquation(current()));
    }

    public boolean supportsInsertEquation() {
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return false;
        }
        return editor.supportsInsertBlock(current());
    }

    public boolean insertDefaultTable() {
        clearPreferredCaretX();
        if (!supportsInsertTable()) {
            return false;
        }
        return history.applyEdit(editor.insertDefaultTable(current()));
    }

    public boolean supportsInsertTable() {
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return false;
        }
        return editor.supportsInsertBlock(current());
    }

    public boolean insertDefaultPlot() {
        clearPreferredCaretX();
        if (!supportsInsertPlot()) {
            return false;
        }
        return history.applyEdit(editor.insertDefaultPlot(current()));
    }

    public boolean supportsInsertPlot() {
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return false;
        }
        return editor.supportsInsertBlock(current());
    }

    public boolean insertDefaultDiagram() {
        clearPreferredCaretX();
        if (!supportsInsertDiagram()) {
            return false;
        }
        return history.applyEdit(editor.insertDefaultDiagram(current()));
    }

    public boolean supportsInsertDiagram() {
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return false;
        }
        return editor.supportsInsertBlock(current());
    }

    public boolean supportsInsertFraction() {
        if (!current().isEquationEditingSelection()) {
            return false;
        }
        try {
            mathEditor.validateSelection(currentEquation().expression(), current().equationEditingSelection().selection());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean insertFraction() {
        clearPreferredCaretX();
        if (!supportsInsertFraction()) {
            return false;
        }
        var result = mathEditor.insertFraction(currentEquation().expression(), current().equationEditingSelection().selection());
        return applyStructuralMathEdit(result);
    }

    public boolean supportsInsertRoot() {
        if (!current().isEquationEditingSelection()) {
            return false;
        }
        try {
            var result = mathEditor.insertRoot(currentEquation().expression(), current().equationEditingSelection().selection());
            return result.changed();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean insertRoot() {
        clearPreferredCaretX();
        if (!supportsInsertRoot()) {
            return false;
        }
        var result = mathEditor.insertRoot(currentEquation().expression(), current().equationEditingSelection().selection());
        return applyStructuralMathEdit(result);
    }

    public boolean supportsInsertGroup(MathDelimiter delimiter) {
        Objects.requireNonNull(delimiter, "delimiter");
        if (!current().isEquationEditingSelection()) {
            return false;
        }
        try {
            var result = mathEditor.insertGroup(currentEquation().expression(), current().equationEditingSelection().selection(), delimiter);
            return result.changed();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean insertGroup(MathDelimiter delimiter) {
        Objects.requireNonNull(delimiter, "delimiter");
        clearPreferredCaretX();
        if (!supportsInsertGroup(delimiter)) {
            return false;
        }
        var result = mathEditor.insertGroup(currentEquation().expression(), current().equationEditingSelection().selection(), delimiter);
        return applyStructuralMathEdit(result);
    }

    public boolean supportsInsertScriptSlot(ScriptSlot slot) {
        Objects.requireNonNull(slot, "slot");
        if (!current().isEquationEditingSelection()) {
            return false;
        }
        try {
            var result = mathEditor.insertScriptSlot(currentEquation().expression(), current().equationEditingSelection().selection(), slot);
            return result.changed() || !result.selection().equals(current().equationEditingSelection().selection());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean insertScriptSlot(ScriptSlot slot) {
        Objects.requireNonNull(slot, "slot");
        clearPreferredCaretX();
        if (!supportsInsertScriptSlot(slot)) {
            return false;
        }
        var result = mathEditor.insertScriptSlot(currentEquation().expression(), current().equationEditingSelection().selection(), slot);
        if (result.changed()) {
            return applyStructuralMathEdit(result);
        }
        setEquationEditingSelection(result.selection());
        return false;
    }

    public boolean supportsSemanticTokenConversion(SemanticMathTokenKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (!current().isEquationEditingSelection()) {
            return false;
        }
        return semanticTokenDraft(kind).isPresent();
    }

    public Optional<SemanticMathTokenDraft> semanticTokenDraft(SemanticMathTokenKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (!current().isEquationEditingSelection()) {
            return Optional.empty();
        }
        return mathEditor.semanticTokenDraft(
                currentEquation().expression(),
                current().equationEditingSelection().selection(),
                kind);
    }

    public boolean isValidSemanticTokenContent(SemanticMathTokenKind kind, String content) {
        return MathExpressionEditor.isValidSemanticTokenContent(kind, content);
    }

    public boolean applySemanticToken(SemanticMathTokenKind kind, String content) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(content, "content");
        clearPreferredCaretX();
        if (!current().isEquationEditingSelection()) {
            return false;
        }
        var result = mathEditor.convertSelectionToSemanticToken(
                currentEquation().expression(),
                current().equationEditingSelection().selection(),
                kind,
                content);
        return applyStructuralMathEdit(result);
    }

    public void setEquationEditingSelection(dev.rgcb.scholar.math.editor.MathSelection selection) {
        Objects.requireNonNull(selection, "selection");
        clearPreferredCaretX();
        if (!current().isEquationEditingSelection()) {
            return;
        }
        mathEditor.validateSelection(currentEquation().expression(), selection);
        history.setCurrent(current().editEquation(current().equationEditingSelection().blockIndex(), selection));
    }

    public void setTableEditingSelection(TableCellTextSelection selection) {
        Objects.requireNonNull(selection, "selection");
        clearPreferredCaretX();
        if (!current().isTableEditingSelection()) {
            return;
        }
        tableEditor.validateSelection(currentTable(), selection);
        history.setCurrent(current().editTable(current().tableEditingSelection().blockIndex(), selection));
    }

    private void moveVertically(LaidOutDocument laidOutDocument, TextMeasurer textMeasurer, boolean up, boolean extend) {
        Objects.requireNonNull(laidOutDocument, "laidOutDocument");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        if (current().isEquationEditingSelection()) {
            history.closeTypingTransaction();
            return;
        }
        if (current().isTableEditingSelection()) {
            moveWithinTableCell(laidOutDocument, textMeasurer, up, extend);
            return;
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            history.closeTypingTransaction();
            return;
        }
        var preferredX = preferredCaretX.orElseGet(() -> initialPreferredX(laidOutDocument, textMeasurer));
        preferredCaretX = Optional.of(preferredX);
        var base = current();
        if (!extend && base.hasSelection()) {
            base = base.collapseTo(base.active());
        }
        var target = up
                ? visualLineNavigator.moveUp(base, laidOutDocument, textMeasurer, preferredX, !extend)
                : visualLineNavigator.moveDown(base, laidOutDocument, textMeasurer, preferredX, !extend);
        var nextSelection = target.orElse(base.selection());
        var nextState = stateWithSelection(base, extend, nextSelection);
        history.setCurrent(nextState);
    }

    private void moveToVisualLineBoundary(LaidOutDocument laidOutDocument, boolean start, boolean extend) {
        Objects.requireNonNull(laidOutDocument, "laidOutDocument");
        clearPreferredCaretX();
        if (!current().isTextSelection()) {
            if (current().isTableEditingSelection()) {
                moveToTableCellVisualLineBoundary(laidOutDocument, start, extend);
            } else {
                history.closeTypingTransaction();
            }
            return;
        }
        var base = current();
        if (!extend && base.hasSelection()) {
            base = base.collapseTo(base.active());
        }
        var boundary = start
                ? visualLineNavigator.lineStart(base.active(), laidOutDocument)
                : visualLineNavigator.lineEnd(base.active(), laidOutDocument);
        var navigationBase = base;
        var nextSelection = boundary.<EditorSelection>map(position -> extend
                        ? new TextSelection(navigationBase.anchor(), position)
                        : new TextSelection(position, position))
                .orElse(navigationBase.selection());
        history.setCurrent(new EditorState(navigationBase.document(), nextSelection, navigationBase.explicitTypingMarks()));
    }

    private EditorState stateWithSelection(EditorState base, boolean extend, EditorSelection target) {
        if (extend && target instanceof TextSelection textSelection && base.isTextSelection()) {
            return new EditorState(base.document(), new TextSelection(base.anchor(), textSelection.active()), base.explicitTypingMarks());
        }
        return new EditorState(base.document(), target, base.explicitTypingMarks());
    }

    private int initialPreferredX(LaidOutDocument laidOutDocument, TextMeasurer textMeasurer) {
        if (current().isTextSelection()) {
            return caretGeometryResolver.resolve(current().active(), laidOutDocument, textMeasurer).x();
        }
        if (current().isBlockSelection()) {
            return visualLineNavigator.preferredXForBlockSelection(current().blockSelection(), laidOutDocument);
        }
        if (current().isTableEditingSelection()) {
            var tableSelection = current().tableEditingSelection();
            var cell = laidOutTableCell(laidOutDocument, tableSelection);
            return tableCaretGeometryResolver.resolve(cell, tableSelection.selection().activeOffset(), textMeasurer).x();
        }
        return 0;
    }

    private void moveWithinTableCell(LaidOutDocument laidOutDocument, TextMeasurer textMeasurer, boolean up, boolean extend) {
        var tableSelection = current().tableEditingSelection();
        var cell = laidOutTableCell(laidOutDocument, tableSelection);
        var preferredX = preferredCaretX.orElseGet(() -> tableCaretGeometryResolver.resolve(
                cell,
                tableSelection.selection().activeOffset(),
                textMeasurer).x());
        preferredCaretX = Optional.of(preferredX);
        var activeOffset = tableSelection.selection().activeOffset();
        var target = up
                ? tableCellTextNavigator.moveUp(cell, activeOffset, textMeasurer, preferredX)
                : tableCellTextNavigator.moveDown(cell, activeOffset, textMeasurer, preferredX);
        var nextOffset = target.orElse(activeOffset);
        var nextSelection = extend
                ? new TableCellTextSelection(tableSelection.selection().cell(), tableSelection.selection().anchorOffset(), nextOffset)
                : TableCellTextSelection.caret(tableSelection.selection().cell(), nextOffset);
        history.setCurrent(current().editTable(tableSelection.blockIndex(), nextSelection));
    }

    private void moveToTableCellVisualLineBoundary(LaidOutDocument laidOutDocument, boolean start, boolean extend) {
        var tableSelection = current().tableEditingSelection();
        var cell = laidOutTableCell(laidOutDocument, tableSelection);
        var activeOffset = tableSelection.selection().activeOffset();
        var target = start
                ? tableCellTextNavigator.lineStart(cell, activeOffset)
                : tableCellTextNavigator.lineEnd(cell, activeOffset);
        var nextOffset = target.orElse(activeOffset);
        var nextSelection = extend
                ? new TableCellTextSelection(tableSelection.selection().cell(), tableSelection.selection().anchorOffset(), nextOffset)
                : TableCellTextSelection.caret(tableSelection.selection().cell(), nextOffset);
        history.setCurrent(current().editTable(tableSelection.blockIndex(), nextSelection));
    }

    private static dev.rgcb.scholar.layout.LaidOutTableCell laidOutTableCell(LaidOutDocument laidOutDocument, TableEditingSelection selection) {
        var block = laidOutDocument.blocks().get(selection.blockIndex());
        var table = block.table().orElseThrow();
        var coordinate = selection.selection().cell();
        return table.rows().get(coordinate.rowIndex()).cells().get(coordinate.columnIndex());
    }

    private boolean applyEditOrSelectionMove(EditResult result) {
        if (result.changed()) {
            return history.applyEdit(result);
        }
        if (!result.editorState().equals(current())) {
            history.setCurrent(result.editorState());
        }
        return false;
    }

    private boolean applyPlotMutation(java.util.function.Supplier<PlotEditResult> mutation) {
        Objects.requireNonNull(mutation, "mutation");
        clearPreferredCaretX();
        if (!current().isPlotEditingSelection()) {
            return false;
        }
        return history.applyEdit(applyPlotEditResult(mutation.get()));
    }

    private EditResult applyPlotEditResult(PlotEditResult result) {
        var blockIndex = current().plotEditingSelection().blockIndex();
        var blocks = new java.util.ArrayList<>(current().document().blocks());
        blocks.set(blockIndex, result.plot());
        return new EditResult(
                new Document(blocks),
                new PlotEditingSelection(blockIndex, result.target()),
                Optional.empty(),
                result.changed());
    }

    private EditResult applyDiagramEditResult(DiagramEditResult result) {
        if (result.changed()) {
            result = new DiagramEditResult(
                    mechanicalDiagramEditor.reconcileConstraints(result.diagram()),
                    result.target(),
                    true);
            mechanicalConstraintSource = null;
            pendingMechanicalConstraintKind = null;
        }
        var blockIndex = current().diagramEditingSelection().blockIndex();
        var document = documentWithDiagramEdit(result);
        return new EditResult(
                document,
                new DiagramEditingSelection(blockIndex, result.target()),
                Optional.empty(),
                result.changed());
    }

    private Document documentWithDiagramEdit(DiagramEditResult result) {
        var blockIndex = current().diagramEditingSelection().blockIndex();
        var blocks = new java.util.ArrayList<>(current().document().blocks());
        blocks.set(blockIndex, result.diagram());
        return new Document(blocks);
    }

    private boolean applyStructuralMathEdit(dev.rgcb.scholar.math.editor.MathEditResult result) {
        if (!current().isEquationEditingSelection() || !result.changed()) {
            return false;
        }
        var blockIndex = current().equationEditingSelection().blockIndex();
        var blocks = new java.util.ArrayList<>(current().document().blocks());
        blocks.set(blockIndex, new EquationBlock(result.expression()));
        return history.applyEdit(new EditResult(
                new Document(blocks),
                new EquationEditingSelection(blockIndex, result.selection()),
                Optional.empty(),
                true));
    }

    private boolean applyTableTyping(TableEditResult result, String insertedText) {
        if (!current().isTableEditingSelection()) {
            return false;
        }
        return history.applyTyping(applyTableEditResult(result), insertedText);
    }

    private EditResult applyTableEditResult(TableEditResult result) {
        var blockIndex = current().tableEditingSelection().blockIndex();
        var blocks = new java.util.ArrayList<>(current().document().blocks());
        blocks.set(blockIndex, result.table());
        return new EditResult(
                new Document(blocks),
                new TableEditingSelection(blockIndex, result.selection()),
                result.explicitTypingMarks(),
                result.changed());
    }

    private boolean applyMathEdit(dev.rgcb.scholar.math.editor.MathEditResult result, String insertedText) {
        if (!current().isEquationEditingSelection()) {
            return false;
        }
        if (!result.changed()) {
            history.setCurrent(current().editEquation(current().equationEditingSelection().blockIndex(), result.selection()));
            return false;
        }
        var blockIndex = current().equationEditingSelection().blockIndex();
        var blocks = new java.util.ArrayList<>(current().document().blocks());
        blocks.set(blockIndex, new EquationBlock(result.expression()));
        var edit = new EditResult(
                new Document(blocks),
                new EquationEditingSelection(blockIndex, result.selection()),
                Optional.empty(),
                true);
        return history.applyTyping(edit, insertedText);
    }

    private void moveMathLeft() {
        var expression = currentEquation().expression();
        var currentSelection = current().equationEditingSelection().selection();
        if (mathEditor.isRootStart(expression, currentSelection)) {
            moveToPreviousDocumentPosition();
            return;
        }
        applyMathEdit(mathEditor.moveLeft(expression, currentSelection), "");
    }

    private void moveMathRight() {
        var expression = currentEquation().expression();
        var currentSelection = current().equationEditingSelection().selection();
        if (mathEditor.isRootEnd(expression, currentSelection)) {
            moveToNextDocumentPosition();
            return;
        }
        applyMathEdit(mathEditor.moveRight(expression, currentSelection), "");
    }

    private void extendMathLeft() {
        var expression = currentEquation().expression();
        var selection = mathEditor.extendLeft(expression, current().equationEditingSelection().selection());
        history.setCurrent(current().editEquation(current().equationEditingSelection().blockIndex(), selection));
    }

    private void extendMathRight() {
        var expression = currentEquation().expression();
        var selection = mathEditor.extendRight(expression, current().equationEditingSelection().selection());
        history.setCurrent(current().editEquation(current().equationEditingSelection().blockIndex(), selection));
    }

    private void moveToPreviousDocumentPosition() {
        var blockIndex = current().equationEditingSelection().blockIndex();
        if (blockIndex > 0) {
            var selection = new EditorState(current().document(), new BlockSelection(blockIndex), Optional.empty());
            history.setCurrent(editor.moveLeft(selection).editorState());
        }
    }

    private void moveToNextDocumentPosition() {
        var blockIndex = current().equationEditingSelection().blockIndex();
        var selection = new EditorState(current().document(), new BlockSelection(blockIndex), Optional.empty());
        history.setCurrent(editor.moveRight(selection).editorState());
    }

    private Optional<ClipboardCopyResult> copyMathForClipboard() {
        var selection = current().equationEditingSelection().selection();
        if (!(selection instanceof MathRangeSelection rangeSelection)) {
            return Optional.empty();
        }
        var fragment = mathEditor.extractSelection(currentEquation().expression(), rangeSelection);
        if (fragment.isEmpty()) {
            return Optional.empty();
        }
        var plainText = mathPlainTextSerializer.serialize(fragment.orElseThrow());
        return plainText.map(text -> ClipboardCopyResult.structured(text, new MathClipboardPayload(fragment.orElseThrow())));
    }

    private Optional<ClipboardCopyResult> copySelectedBlockForClipboard() {
        var blockIndex = current().blockSelection().blockIndex();
        var block = current().document().blocks().get(blockIndex);
        if (block instanceof TableBlock table) {
            return Optional.of(ClipboardCopyResult.structured(
                    tableTsvSerializer.serialize(table),
                    new TableClipboardPayload(table)));
        }
        if (block instanceof PlotBlock plot) {
            return Optional.of(ClipboardCopyResult.structured(
                    plotPlainTextSerializer.serialize(plot),
                    new PlotClipboardPayload(plot)));
        }
        if (block instanceof DiagramBlock diagram) {
            return Optional.of(ClipboardCopyResult.structured(
                    diagramPlainTextSerializer.serialize(diagram),
                    new DiagramClipboardPayload(diagram)));
        }
        return Optional.empty();
    }

    private Optional<ClipboardCutResult> cutSelectedBlockForClipboard() {
        var copy = copySelectedBlockForClipboard();
        if (copy.isEmpty()) {
            return Optional.empty();
        }
        var edit = editor.deleteSelectedBlock(current());
        if (!edit.changed()) {
            return Optional.empty();
        }
        var copied = copy.orElseThrow();
        return Optional.of(new ClipboardCutResult(copied.plainText(), copied.payload(), edit));
    }

    private boolean supportsPasteTableFromClipboard() {
        if (current().isEquationEditingSelection() || current().isTableEditingSelection()) {
            return false;
        }
        if (current().isBlockSelection()) {
            return current().document().blocks().get(current().blockSelection().blockIndex()) instanceof TableBlock;
        }
        return editor.supportsInsertBlock(current());
    }

    private boolean pasteTableFromClipboard(TableBlock table) {
        if (!supportsPasteTableFromClipboard()) {
            return false;
        }
        if (current().isBlockSelection()) {
            var blockIndex = current().blockSelection().blockIndex();
            if (!(current().document().blocks().get(blockIndex) instanceof TableBlock)) {
                return false;
            }
            var blocks = new java.util.ArrayList<>(current().document().blocks());
            blocks.set(blockIndex, table);
            var document = new Document(blocks);
            return history.applyEdit(new EditResult(
                    document,
                    new BlockSelection(blockIndex),
                    Optional.empty(),
                    !document.equals(current().document())));
        }
        return history.applyEdit(editor.insertBlock(current(), table));
    }

    private boolean supportsPastePlotFromClipboard() {
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return false;
        }
        if (current().isBlockSelection()) {
            return current().document().blocks().get(current().blockSelection().blockIndex()) instanceof PlotBlock;
        }
        return editor.supportsInsertBlock(current());
    }

    private boolean pastePlotFromClipboard(PlotBlock plot) {
        if (!supportsPastePlotFromClipboard()) {
            return false;
        }
        if (current().isBlockSelection()) {
            var blockIndex = current().blockSelection().blockIndex();
            if (!(current().document().blocks().get(blockIndex) instanceof PlotBlock)) {
                return false;
            }
            var blocks = new java.util.ArrayList<>(current().document().blocks());
            blocks.set(blockIndex, plot);
            var document = new Document(blocks);
            return history.applyEdit(new EditResult(
                    document,
                    new BlockSelection(blockIndex),
                    Optional.empty(),
                    !document.equals(current().document())));
        }
        return history.applyEdit(editor.insertBlock(current(), plot));
    }

    private boolean supportsPasteDiagramFromClipboard() {
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection()) {
            return false;
        }
        if (current().isBlockSelection()) {
            return current().document().blocks().get(current().blockSelection().blockIndex()) instanceof DiagramBlock;
        }
        return editor.supportsInsertBlock(current());
    }

    private boolean pasteDiagramFromClipboard(DiagramBlock diagram) {
        if (!supportsPasteDiagramFromClipboard()) {
            return false;
        }
        if (current().isBlockSelection()) {
            var blockIndex = current().blockSelection().blockIndex();
            if (!(current().document().blocks().get(blockIndex) instanceof DiagramBlock)) {
                return false;
            }
            var blocks = new java.util.ArrayList<>(current().document().blocks());
            blocks.set(blockIndex, diagram);
            var document = new Document(blocks);
            diagramConnectionSource = null;
            diagramDragInteraction = null;
            return history.applyEdit(new EditResult(
                    document,
                    new BlockSelection(blockIndex),
                    Optional.empty(),
                    !document.equals(current().document())));
        }
        diagramConnectionSource = null;
        diagramDragInteraction = null;
        return history.applyEdit(editor.insertBlock(current(), diagram));
    }

    private Optional<ClipboardCutResult> cutMathForClipboard() {
        var copy = copyMathForClipboard();
        if (copy.isEmpty()) {
            return Optional.empty();
        }
        var result = mathEditor.deleteSelection(currentEquation().expression(), current().equationEditingSelection().selection());
        if (!result.changed()) {
            return Optional.empty();
        }
        var blockIndex = current().equationEditingSelection().blockIndex();
        var blocks = new java.util.ArrayList<>(current().document().blocks());
        blocks.set(blockIndex, new EquationBlock(result.expression()));
        var edit = new EditResult(
                new Document(blocks),
                new EquationEditingSelection(blockIndex, result.selection()),
                Optional.empty(),
                true);
        var copied = copy.orElseThrow();
        return Optional.of(new ClipboardCutResult(copied.plainText(), copied.payload(), edit));
    }

    private Optional<ClipboardCutResult> cutTableForClipboard() {
        return tableEditor.cut(
                        new TableEditor.DocumentSource(current().document(), current().tableEditingSelection().blockIndex(), currentTable()),
                        current().tableEditingSelection().selection())
                .map(cut -> ClipboardCutResult.plainText(cut.clipboardText(), cut.editResult()));
    }

    private Optional<MathClipboardPayload> mathFragmentFromClipboard(Optional<ScholarClipboardPayload> payload, String text) {
        var nativePayload = payload.filter(MathClipboardPayload.class::isInstance)
                .map(MathClipboardPayload.class::cast);
        if (nativePayload.isPresent()) {
            return nativePayload;
        }
        return switch (mathPlainTextImporter.importText(text)) {
            case MathPlainTextImportResult.Success success -> Optional.of(new MathClipboardPayload(success.fragment()));
            case MathPlainTextImportResult.Failure ignored -> Optional.empty();
        };
    }

    private static Optional<TableClipboardPayload> tableFromClipboard(Optional<ScholarClipboardPayload> payload) {
        return payload.filter(TableClipboardPayload.class::isInstance)
                .map(TableClipboardPayload.class::cast);
    }

    private static Optional<PlotClipboardPayload> plotFromClipboard(Optional<ScholarClipboardPayload> payload) {
        return payload.filter(PlotClipboardPayload.class::isInstance)
                .map(PlotClipboardPayload.class::cast);
    }

    private static Optional<DiagramClipboardPayload> diagramFromClipboard(Optional<ScholarClipboardPayload> payload) {
        return payload.filter(DiagramClipboardPayload.class::isInstance)
                .map(DiagramClipboardPayload.class::cast);
    }

    private EquationBlock currentEquation() {
        var selection = current().equationEditingSelection();
        return (EquationBlock) current().document().blocks().get(selection.blockIndex());
    }

    private TableBlock currentTable() {
        var selection = current().tableEditingSelection();
        return (TableBlock) current().document().blocks().get(selection.blockIndex());
    }

    private PlotBlock currentPlot() {
        var selection = current().plotEditingSelection();
        return (PlotBlock) current().document().blocks().get(selection.blockIndex());
    }

    private DiagramBlock currentDiagram() {
        var selection = current().diagramEditingSelection();
        return (DiagramBlock) current().document().blocks().get(selection.blockIndex());
    }

    private Set<TextMark> tableMarksForReplacement() {
        return current().explicitTypingMarks()
                .map(Set::copyOf)
                .orElseGet(() -> tableEditor.marksForReplacement(currentTable(), current().tableEditingSelection().selection()));
    }

    private Optional<TableEditResult> pasteIntoTableCell(String text) {
        var normalized = clipboard.normalizeMultilineClipboardText(text);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(tableEditor.insertText(
                currentTable(),
                current().tableEditingSelection().selection(),
                normalized,
                tableMarksForReplacement()));
    }

    private static EditResult clearTypingMarks(EditResult result) {
        return new EditResult(result.document(), result.selection(), Optional.empty(), result.changed());
    }

    private record DiagramDragInteraction(
            int blockIndex,
            DiagramElementTarget target,
            double pointerOffsetX,
            double pointerOffsetY
    ) {
        private DiagramDragInteraction {
            target = Objects.requireNonNull(target, "target");
            if (blockIndex < 0) {
                throw new IllegalArgumentException("blockIndex must not be negative.");
            }
            if (!Double.isFinite(pointerOffsetX) || !Double.isFinite(pointerOffsetY)) {
                throw new IllegalArgumentException("Diagram drag pointer offsets must be finite.");
            }
        }
    }

}
