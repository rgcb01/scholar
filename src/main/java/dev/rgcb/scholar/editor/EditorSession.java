package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.DocumentPlainTextSerializer;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.FigureNumbering;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceResolver;
import dev.rgcb.scholar.document.CrossReferenceTarget;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.DocumentStructureResolver;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.clipboard.DocumentBlockClipboardPayload;
import dev.rgcb.scholar.clipboard.InlineContentClipboardPayload;
import dev.rgcb.scholar.clipboard.ScholarClipboardPayload;
import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetPlotBinding;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetTableBinding;
import dev.rgcb.scholar.data.DatasetTableResolver;
import dev.rgcb.scholar.data.DatasetTsvSerializer;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.data.clipboard.DatasetClipboardPayload;
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
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.plot.clipboard.PlotClipboardPayload;
import dev.rgcb.scholar.plot.clipboard.PlotPlainTextSerializer;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.clipboard.DiagramClipboardPayload;
import dev.rgcb.scholar.diagram.clipboard.DiagramPlainTextSerializer;
import dev.rgcb.scholar.figure.clipboard.FigureClipboardPayload;
import dev.rgcb.scholar.figure.clipboard.FigurePlainTextSerializer;
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
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
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
    private final FigurePlainTextSerializer figurePlainTextSerializer;
    private final CrossReferenceResolver crossReferenceResolver;
    private final DocumentStructureResolver structureResolver;
    private final DocumentPlainTextSerializer documentPlainTextSerializer;
    private final DatasetTableResolver datasetTableResolver;
    private final DatasetTsvSerializer datasetTsvSerializer;
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
        figurePlainTextSerializer = new FigurePlainTextSerializer();
        crossReferenceResolver = new CrossReferenceResolver();
        structureResolver = new DocumentStructureResolver();
        documentPlainTextSerializer = new DocumentPlainTextSerializer();
        datasetTableResolver = new DatasetTableResolver();
        datasetTsvSerializer = new DatasetTsvSerializer();
        clipboard = new PlainTextClipboard(editor);
        history = new EditorHistory(editor.initialState(initialDocument, initialBlockIndex));
    }

    public EditorState current() {
        return history.current();
    }

    public EditorFocusOwner focusOwner() {
        return EditorFocusOwner.fromSelection(current().selection());
    }

    public boolean canUndo() {
        return history.canUndo();
    }

    public boolean canRedo() {
        return history.canRedo();
    }

    public int undoDepth() {
        return history.undoDepth();
    }

    public int redoDepth() {
        return history.redoDepth();
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
            if (isEditingDatasetBackedTable()) {
                return applyDatasetBackedTableTyping(tableEditor.insertText(
                        currentTable(),
                        current().tableEditingSelection().selection(),
                        text,
                        Set.of()),
                        text);
            }
            return applyTableTyping(tableEditor.insertText(
                    currentTable(),
                    current().tableEditingSelection().selection(),
                    text,
                    tableMarksForReplacement()),
                    text);
        }
        if (current().isFigureCaptionSelection()) {
            return applyFigureCaptionTyping(text);
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
        if (current().isBlockSelection() && current().document().blocks().get(current().blockSelection().blockIndex()) instanceof FigureBlock figureBlock) {
            var blockIndex = current().blockSelection().blockIndex();
            if (figureBlock.content() instanceof PlotBlock plotBlock) {
                history.setCurrent(current().editPlot(blockIndex, plotEditor.firstTarget(plotBlock)));
                return false;
            }
            if (figureBlock.content() instanceof DiagramBlock diagramBlock) {
                history.setCurrent(current().editDiagram(blockIndex, diagramEditor.firstTarget(diagramBlock)));
                return false;
            }
        }
        if (current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
            if (isEditingDatasetBackedTable()) {
                return applyEditOrSelectionMove(applyDatasetBackedTableEdit(tableEditor.deleteBackward(
                        currentTable(),
                        current().tableEditingSelection().selection())));
            }
            return applyEditOrSelectionMove(applyTableEditResult(tableEditor.deleteBackward(currentTable(), current().tableEditingSelection().selection())));
        }
        if (current().isFigureCaptionSelection()) {
            return applyFigureCaptionDeleteBackward();
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
            if (isEditingDatasetBackedTable()) {
                return applyEditOrSelectionMove(applyDatasetBackedTableEdit(tableEditor.deleteForward(
                        currentTable(),
                        current().tableEditingSelection().selection())));
            }
            return applyEditOrSelectionMove(applyTableEditResult(tableEditor.deleteForward(currentTable(), current().tableEditingSelection().selection())));
        }
        if (current().isFigureCaptionSelection()) {
            return applyFigureCaptionDeleteForward();
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
        if (current().isFigureCaptionSelection()) {
            moveFigureCaption(-1, false);
            return;
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
        if (current().isFigureCaptionSelection()) {
            moveFigureCaption(1, false);
            return;
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
        if (current().isFigureCaptionSelection()) {
            moveFigureCaption(-1, true);
            return;
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
        if (current().isFigureCaptionSelection()) {
            moveFigureCaption(1, true);
            return;
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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

    public void selectAll() {
        clearPreferredCaretX();
        if (current().isEquationEditingSelection()) {
            history.setCurrent(current().editEquation(
                    current().equationEditingSelection().blockIndex(),
                    mathEditor.selectAll(currentEquation().expression())));
            return;
        }
        if (current().isTableEditingSelection()) {
            var selection = current().tableEditingSelection();
            var text = tableEditor.cellText(currentTable(), selection.selection().cell());
            history.setCurrent(current().editTable(
                    selection.blockIndex(),
                    new TableCellTextSelection(selection.selection().cell(), 0, TextBoundary.characterCount(text))));
            return;
        }
        if (current().isFigureCaptionSelection()) {
            var selection = current().figureCaptionSelection();
            history.setCurrent(current().editFigureCaption(
                    selection.blockIndex(),
                    new FigureCaptionSelection(selection.blockIndex(), 0, captionLength(currentFigureForCaption().caption()))));
            return;
        }
        if (current().isTextSelection()) {
            history.setCurrent(new EditorState(current().document(), selectCurrentEditableTextScope(), Optional.empty()));
            return;
        }
        history.closeTypingTransaction();
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
        return current().isTableEditingSelection() && !isEditingDatasetBackedTable();
    }

    public boolean supportsDeleteTableRow() {
        return current().isTableEditingSelection() && !isEditingDatasetBackedTable() && tableEditor.canDeleteRow(currentTable());
    }

    public boolean supportsInsertTableColumn() {
        return current().isTableEditingSelection() && !isEditingDatasetBackedTable();
    }

    public boolean supportsDeleteTableColumn() {
        return current().isTableEditingSelection() && !isEditingDatasetBackedTable() && tableEditor.canDeleteColumn(currentTable());
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
        var table=new TableBlock(rows,1);var blockIndex=current().diagramEditingSelection().blockIndex();var blocks=new java.util.ArrayList<>(current().document().blocks());blocks.add(blockIndex+1,table);var document=withCurrentDatasets(blocks);return history.applyEdit(new EditResult(document,new BlockSelection(blockIndex+1),Optional.empty(),true));
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
        if (current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
            return false;
        }
        return editor.supportsBlockStyle(current());
    }

    public Optional<BlockStyle> blockStyle() {
        var state = blockStyleSelectionState();
        return state.kind() == BlockStyleSelectionState.Kind.SINGLE ? state.style() : Optional.empty();
    }

    public BlockStyleSelectionState blockStyleSelectionState() {
        if (current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
            if (isEditingDatasetBackedTable()) {
                return false;
            }
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
        if (current().isBlockSelection() || current().isEquationEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
            return Optional.empty();
        }
        return clipboard.copy(current());
    }

    public List<CrossReferenceTarget> availableCrossReferenceTargets() {
        return crossReferenceResolver.targets(current().document());
    }

    public boolean supportsInsertCrossReference() {
        if (availableCrossReferenceTargets().isEmpty()) {
            return false;
        }
        if (current().isFigureCaptionSelection()) {
            return true;
        }
        return !current().isBlockSelection()
                && !current().isEquationEditingSelection()
                && !current().isTableEditingSelection()
                && !current().isPlotEditingSelection()
                && !current().isDiagramEditingSelection();
    }

    public boolean insertCrossReference(CrossReferenceTargetKind kind, String targetId) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(targetId, "targetId");
        if (!supportsInsertCrossReference()) {
            return false;
        }
        var reference = new CrossReference(kind, targetId);
        if (current().isFigureCaptionSelection()) {
            return history.applyEdit(applyFigureCaptionReplacement(new InlineContent(List.of(reference))));
        }
        return history.applyEdit(editor.insertCrossReference(current(), reference));
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
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
            return Optional.empty();
        }
        return copyInlineContentForClipboard().or(() -> clipboard.copy(current()).map(ClipboardCopyResult::plainText));
    }

    public Optional<ClipboardEditResult> cutSelection() {
        if (current().isTableEditingSelection()) {
            if (isEditingDatasetBackedTable()) {
                return Optional.empty();
            }
            return tableEditor.cut(new TableEditor.DocumentSource(current().document(), current().tableEditingSelection().blockIndex(), currentTable()), current().tableEditingSelection().selection());
        }
        if (current().isBlockSelection() || current().isEquationEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
            return Optional.empty();
        }
        return cutInlineContentForClipboard()
                .or(() -> clipboard.cut(current())
                        .map(cut -> ClipboardCutResult.plainText(cut.clipboardText(), cut.editResult())));
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
                    .filter(result -> history.applyEdit(clearTypingMarks(isEditingDatasetBackedTable()
                            ? applyDatasetBackedTableEdit(result)
                            : applyTableEditResult(result))))
                    .isPresent();
        }
        if (current().isBlockSelection() || current().isEquationEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
            return false;
        }
        if (inlineContentFromClipboard(payload).isPresent()) {
            return supportsPasteInlineContentFromClipboard();
        }
        if (documentBlockFromClipboard(payload).isPresent()) {
            return supportsPasteDocumentBlockFromClipboard(documentBlockFromClipboard(payload).orElseThrow().block());
        }
        if (datasetFromClipboard(payload).isPresent()) {
            return supportsDatasetDocumentAction();
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
        if (figureFromClipboard(payload).isPresent()) {
            return supportsPasteFigureFromClipboard();
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
                    .filter(result -> history.applyEdit(clearTypingMarks(isEditingDatasetBackedTable()
                            ? applyDatasetBackedTableEdit(result)
                            : applyTableEditResult(result))))
                    .isPresent();
        }
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
            return false;
        }
        var inlinePayload = inlineContentFromClipboard(payload);
        if (inlinePayload.isPresent()) {
            return pasteInlineContentFromClipboard(inlinePayload.orElseThrow().content());
        }
        var documentBlockPayload = documentBlockFromClipboard(payload);
        if (documentBlockPayload.isPresent()) {
            return pasteDocumentBlockFromClipboard(documentBlockPayload.orElseThrow().block());
        }
        var datasetPayload = datasetFromClipboard(payload);
        if (datasetPayload.isPresent()) {
            return addDataset(datasetPayload.orElseThrow().dataset());
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
        var figurePayload = figureFromClipboard(payload);
        if (figurePayload.isPresent()) {
            return pasteFigureFromClipboard(figurePayload.orElseThrow().figure());
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
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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

    public boolean insertTableOfContents() {
        clearPreferredCaretX();
        if (!supportsInsertTableOfContents()) {
            return false;
        }
        return history.applyEdit(editor.insertTableOfContents(current()));
    }

    public boolean supportsInsertTableOfContents() {
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
            return false;
        }
        return editor.supportsInsertBlock(current());
    }

    public boolean supportsDatasetDocumentAction() {
        return !current().isEquationEditingSelection()
                && !current().isTableEditingSelection()
                && !current().isPlotEditingSelection()
                && !current().isDiagramEditingSelection()
                && !current().isFigureCaptionSelection();
    }

    public boolean createDefaultDataset() {
        return addDataset(defaultDataset(uniqueDatasetId("projectile-test")));
    }

    public boolean addDataset(ScientificDataset dataset) {
        Objects.requireNonNull(dataset, "dataset");
        if (!supportsDatasetDocumentAction()) {
            return false;
        }
        var datasets = new java.util.ArrayList<>(current().document().datasets());
        var candidate = dataset;
        var candidateId = candidate.id();
        if (datasets.stream().anyMatch(existing -> existing.id().equals(candidateId))) {
            candidate = candidate.withId(uniqueDatasetId(candidate.id()));
        }
        datasets.add(candidate);
        return history.applyEdit(new EditResult(new Document(current().document().blocks(), datasets), current().selection(), current().explicitTypingMarks(), true));
    }

    public boolean deleteDataset(String datasetId) {
        Objects.requireNonNull(datasetId, "datasetId");
        var datasets = current().document().datasets().stream()
                .filter(dataset -> !dataset.id().equals(datasetId))
                .toList();
        if (datasets.size() == current().document().datasets().size()) {
            return false;
        }
        return history.applyEdit(new EditResult(new Document(current().document().blocks(), datasets), current().selection(), current().explicitTypingMarks(), true));
    }

    public Optional<ClipboardCopyResult> copyDatasetForClipboard(String datasetId) {
        Objects.requireNonNull(datasetId, "datasetId");
        return current().document().datasets().stream()
                .filter(dataset -> dataset.id().equals(datasetId))
                .findFirst()
                .map(dataset -> ClipboardCopyResult.structured(
                        datasetTsvSerializer.serialize(dataset),
                        new DatasetClipboardPayload(dataset)));
    }

    public boolean renameDataset(String datasetId, String displayName) {
        return replaceDataset(datasetId, dataset -> dataset.withDisplayName(displayName));
    }

    public boolean renameDatasetColumn(String datasetId, String columnId, String displayName) {
        return replaceDataset(datasetId, dataset -> dataset.withColumnDisplayName(columnId, displayName));
    }

    public boolean editDatasetCell(String datasetId, int rowIndex, String columnId, DatasetValue value) {
        return replaceDataset(datasetId, dataset -> dataset.withCell(rowIndex, columnId, value));
    }

    public boolean addDatasetRow(String datasetId, DatasetRow row) {
        return replaceDataset(datasetId, dataset -> dataset.withAddedRow(row));
    }

    public boolean deleteDatasetRow(String datasetId, int rowIndex) {
        return replaceDataset(datasetId, dataset -> dataset.withoutRow(rowIndex));
    }

    public boolean addDatasetColumn(String datasetId, DatasetColumn column, DatasetValue defaultValue) {
        return replaceDataset(datasetId, dataset -> dataset.withAddedColumn(column, defaultValue));
    }

    public boolean deleteDatasetColumn(String datasetId, String columnId) {
        return replaceDataset(datasetId, dataset -> dataset.withoutColumn(columnId));
    }

    public boolean supportsInsertDatasetTable() {
        return supportsDatasetDocumentAction()
                && !current().document().datasets().isEmpty()
                && editor.supportsInsertBlock(current());
    }

    public boolean insertDatasetTableForFirstDataset() {
        if (!supportsInsertDatasetTable()) {
            return false;
        }
        var dataset = current().document().datasets().get(0);
        return history.applyEdit(editor.insertBlock(current(), new TableBlock(new DatasetTableBinding(dataset.id()))));
    }

    public boolean supportsBindSelectedPlotToFirstDataset() {
        return current().isBlockSelection()
                && current().document().blocks().get(current().blockSelection().blockIndex()) instanceof PlotBlock
                && current().document().datasets().stream().anyMatch(dataset -> dataset.columns().size() >= 2);
    }

    public boolean bindSelectedPlotToFirstDataset() {
        if (!supportsBindSelectedPlotToFirstDataset()) {
            return false;
        }
        var blockIndex = current().blockSelection().blockIndex();
        var plot = (PlotBlock) current().document().blocks().get(blockIndex);
        var dataset = current().document().datasets().stream()
                .filter(candidate -> candidate.columns().size() >= 2)
                .findFirst()
                .orElseThrow();
        var binding = new DatasetPlotBinding(dataset.id(), dataset.columns().get(0).id(), dataset.columns().get(1).id());
        var definition = plot.definition();
        var series = definition.series().isEmpty()
                ? List.of(new PlotSeries(dataset.displayLabel(), PlotSeriesKind.LINE, binding))
                : replaceFirstSeriesBinding(definition.series(), binding);
        var updatedPlot = new PlotBlock(new PlotDefinition(definition.title(), definition.xAxis(), definition.yAxis(), series, definition.legendVisible(), definition.gridVisible(), definition.height()));
        var blocks = new java.util.ArrayList<BlockNode>(current().document().blocks());
        blocks.set(blockIndex, updatedPlot);
        return history.applyEdit(new EditResult(new Document(blocks, current().document().datasets()), new BlockSelection(blockIndex), Optional.empty(), true));
    }

    public boolean supportsInsertTable() {
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
            return false;
        }
        return editor.supportsInsertBlock(current());
    }

    public boolean navigateToHeadingId(String targetId) {
        Objects.requireNonNull(targetId, "targetId");
        var section = structureResolver.resolve(current().document()).sectionById(targetId);
        if (section.isEmpty()) {
            return false;
        }
        history.setCurrent(new EditorState(
                current().document(),
                new TextSelection(
                        new DocumentPosition(section.orElseThrow().blockIndex(), 0),
                        new DocumentPosition(section.orElseThrow().blockIndex(), 0)),
                current().explicitTypingMarks()));
        return true;
    }

    public boolean insertDefaultPlot() {
        clearPreferredCaretX();
        if (!supportsInsertPlot()) {
            return false;
        }
        return history.applyEdit(editor.insertDefaultPlot(current()));
    }

    public boolean supportsInsertPlot() {
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
            return false;
        }
        return editor.supportsInsertBlock(current());
    }

    public boolean supportsWrapSelectedPlotInFigure() {
        return current().isBlockSelection()
                && current().document().blocks().get(current().blockSelection().blockIndex()) instanceof PlotBlock;
    }

    public boolean supportsWrapSelectedDiagramInFigure() {
        return current().isBlockSelection()
                && current().document().blocks().get(current().blockSelection().blockIndex()) instanceof DiagramBlock;
    }

    public boolean wrapSelectedPlotInFigure() {
        return wrapSelectedBlockInFigure("figure");
    }

    public boolean wrapSelectedDiagramInFigure() {
        return wrapSelectedBlockInFigure("figure");
    }

    public boolean supportsEditFigureCaption() {
        return current().isBlockSelection()
                && current().document().blocks().get(current().blockSelection().blockIndex()) instanceof FigureBlock;
    }

    public boolean editFigureCaption() {
        if (!supportsEditFigureCaption()) {
            return false;
        }
        var blockIndex = current().blockSelection().blockIndex();
        var figure = (FigureBlock) current().document().blocks().get(blockIndex);
        history.setCurrent(current().editFigureCaption(
                blockIndex,
                FigureCaptionSelection.caret(blockIndex, captionLength(figure.caption()))));
        return true;
    }

    public boolean supportsUnwrapFigure() {
        return current().isBlockSelection()
                && current().document().blocks().get(current().blockSelection().blockIndex()) instanceof FigureBlock;
    }

    public boolean unwrapFigure() {
        clearPreferredCaretX();
        if (!supportsUnwrapFigure()) {
            return false;
        }
        var blockIndex = current().blockSelection().blockIndex();
        var figure = (FigureBlock) current().document().blocks().get(blockIndex);
        var blocks = new java.util.ArrayList<BlockNode>(current().document().blocks());
        blocks.set(blockIndex, figure.content());
        var updated = withCurrentDatasets(blocks);
        return history.applyEdit(new EditResult(updated, new BlockSelection(blockIndex), Optional.empty(), !updated.equals(current().document())));
    }

    public boolean setFigureCaptionText(int blockIndex, String captionText) {
        Objects.requireNonNull(captionText, "captionText");
        if (blockIndex < 0 || blockIndex >= current().document().blocks().size()) {
            throw new IllegalArgumentException("blockIndex is outside the document.");
        }
        if (!(current().document().blocks().get(blockIndex) instanceof FigureBlock figure)) {
            return false;
        }
        var caption = captionFromText(captionText);
        var updatedFigure = figure.withCaption(caption);
        var blocks = new java.util.ArrayList<BlockNode>(current().document().blocks());
        blocks.set(blockIndex, updatedFigure);
        var updated = withCurrentDatasets(blocks);
        return history.applyEdit(new EditResult(
                updated,
                FigureCaptionSelection.caret(blockIndex, captionLength(caption)),
                Optional.empty(),
                !updated.equals(current().document())));
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
        if (current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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

    private TextSelection selectCurrentEditableTextScope() {
        var blocks = current().document().blocks();
        var blockIndex = current().textSelection().active().blockIndex();
        var first = blockIndex;
        while (first > 0 && EditableInlineBlock.supports(blocks.get(first - 1))) {
            first--;
        }
        var last = blockIndex;
        while (last + 1 < blocks.size() && EditableInlineBlock.supports(blocks.get(last + 1))) {
            last++;
        }
        var endOffset = InlineContentEditor.characterCount(EditableInlineBlock.contentOf(blocks.get(last)));
        return new TextSelection(new DocumentPosition(first, 0), new DocumentPosition(last, endOffset));
    }

    private static dev.rgcb.scholar.layout.LaidOutTableCell laidOutTableCell(LaidOutDocument laidOutDocument, TableEditingSelection selection) {
        var block = laidOutDocument.blocks().get(selection.blockIndex());
        var table = block.table().orElseThrow();
        var coordinate = selection.selection().cell();
        return table.rows().get(coordinate.rowIndex()).cells().get(coordinate.columnIndex());
    }

    private boolean wrapSelectedBlockInFigure(String idBase) {
        clearPreferredCaretX();
        if (!current().isBlockSelection()) {
            return false;
        }
        var blockIndex = current().blockSelection().blockIndex();
        var block = current().document().blocks().get(blockIndex);
        if (!FigureBlock.supportsContent(block)) {
            return false;
        }
        var figure = new FigureBlock(uniqueFigureId(current().document(), idBase, Optional.empty()), block, new InlineContent(List.of()));
        var blocks = new java.util.ArrayList<BlockNode>(current().document().blocks());
        blocks.set(blockIndex, figure);
        return history.applyEdit(new EditResult(
                withCurrentDatasets(blocks),
                new BlockSelection(blockIndex),
                Optional.empty(),
                true));
    }

    private boolean applyFigureCaptionTyping(String text) {
        return history.applyEdit(applyFigureCaptionReplacement(text));
    }

    private boolean applyFigureCaptionDeleteBackward() {
        var selection = current().figureCaptionSelection();
        if (!selection.isCaret()) {
            return history.applyEdit(applyFigureCaptionReplacement(""));
        }
        if (selection.activeOffset() == 0) {
            return false;
        }
        var figure = currentFigureForCaption();
        var previous = TextBoundary.previousOffset(captionText(figure.caption()), selection.activeOffset());
        return history.applyEdit(applyFigureCaptionReplacement(previous, selection.activeOffset(), ""));
    }

    private boolean applyFigureCaptionDeleteForward() {
        var selection = current().figureCaptionSelection();
        if (!selection.isCaret()) {
            return history.applyEdit(applyFigureCaptionReplacement(""));
        }
        var figure = currentFigureForCaption();
        var text = captionText(figure.caption());
        if (selection.activeOffset() == TextBoundary.characterCount(text)) {
            return false;
        }
        var next = TextBoundary.nextOffset(text, selection.activeOffset());
        return history.applyEdit(applyFigureCaptionReplacement(selection.activeOffset(), next, ""));
    }

    private void moveFigureCaption(int direction, boolean extend) {
        var selection = current().figureCaptionSelection();
        var text = captionText(currentFigureForCaption().caption());
        var active = selection.activeOffset();
        var next = active;
        if (direction < 0 && active > 0) {
            next = TextBoundary.previousOffset(text, active);
        } else if (direction > 0 && active < TextBoundary.characterCount(text)) {
            next = TextBoundary.nextOffset(text, active);
        }
        var replacement = extend
                ? new FigureCaptionSelection(selection.blockIndex(), selection.anchorOffset(), next)
                : FigureCaptionSelection.caret(selection.blockIndex(), next);
        history.setCurrent(current().editFigureCaption(selection.blockIndex(), replacement));
    }

    private EditResult applyFigureCaptionReplacement(String replacement) {
        var selection = current().figureCaptionSelection();
        return applyFigureCaptionReplacement(selection.startOffset(), selection.endOffset(), captionFromText(replacement));
    }

    private EditResult applyFigureCaptionReplacement(InlineContent replacement) {
        var selection = current().figureCaptionSelection();
        return applyFigureCaptionReplacement(selection.startOffset(), selection.endOffset(), replacement);
    }

    private EditResult applyFigureCaptionReplacement(int startOffset, int endOffset, String replacement) {
        return applyFigureCaptionReplacement(startOffset, endOffset, captionFromText(replacement));
    }

    private EditResult applyFigureCaptionReplacement(int startOffset, int endOffset, InlineContent middle) {
        var selection = current().figureCaptionSelection();
        var figure = currentFigureForCaption();
        var captionText = captionText(figure.caption());
        TextBoundary.validateRange(captionText, startOffset, endOffset);
        var left = InlineContentEditor.split(figure.caption(), startOffset).left();
        var right = InlineContentEditor.split(figure.caption(), endOffset).right();
        var updatedCaption = InlineContentEditor.concat(InlineContentEditor.concat(left, middle), right);
        var blocks = new java.util.ArrayList<BlockNode>(current().document().blocks());
        blocks.set(selection.blockIndex(), figure.withCaption(updatedCaption));
        var updatedDocument = withCurrentDatasets(blocks);
        var caret = startOffset + InlineContentEditor.characterCount(middle);
        return new EditResult(
                updatedDocument,
                FigureCaptionSelection.caret(selection.blockIndex(), caret),
                Optional.empty(),
                !updatedDocument.equals(current().document()));
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
        blocks.set(blockIndex, replacePlotContent(blocks.get(blockIndex), result.plot()));
        return new EditResult(
                withCurrentDatasets(blocks),
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
        blocks.set(blockIndex, replaceDiagramContent(blocks.get(blockIndex), result.diagram()));
        return withCurrentDatasets(blocks);
    }

    private boolean applyStructuralMathEdit(dev.rgcb.scholar.math.editor.MathEditResult result) {
        if (!current().isEquationEditingSelection() || !result.changed()) {
            return false;
        }
        var blockIndex = current().equationEditingSelection().blockIndex();
        var blocks = new java.util.ArrayList<>(current().document().blocks());
        blocks.set(blockIndex, currentEquation().withExpression(result.expression()));
        return history.applyEdit(new EditResult(
                withCurrentDatasets(blocks),
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

    private boolean applyDatasetBackedTableTyping(TableEditResult result, String insertedText) {
        if (!current().isTableEditingSelection()) {
            return false;
        }
        return history.applyTyping(applyDatasetBackedTableEdit(result), insertedText);
    }

    private EditResult applyTableEditResult(TableEditResult result) {
        var blockIndex = current().tableEditingSelection().blockIndex();
        var blocks = new java.util.ArrayList<>(current().document().blocks());
        blocks.set(blockIndex, result.table());
        return new EditResult(
                withCurrentDatasets(blocks),
                new TableEditingSelection(blockIndex, result.selection()),
                result.explicitTypingMarks(),
                result.changed());
    }

    private EditResult applyDatasetBackedTableEdit(TableEditResult result) {
        var blockIndex = current().tableEditingSelection().blockIndex();
        if (!(current().document().blocks().get(blockIndex) instanceof TableBlock boundTable)
                || boundTable.datasetBinding().isEmpty()
                || !result.changed()) {
            return new EditResult(
                    current().document(),
                    new TableEditingSelection(blockIndex, result.selection()),
                    result.explicitTypingMarks(),
                    false);
        }
        var binding = boundTable.datasetBinding().orElseThrow();
        var datasetIndex = datasetIndex(binding.datasetId());
        if (datasetIndex < 0) {
            return new EditResult(
                    current().document(),
                    new TableEditingSelection(blockIndex, current().tableEditingSelection().selection()),
                    Optional.empty(),
                    false);
        }
        var dataset = current().document().datasets().get(datasetIndex);
        var originalCell = current().tableEditingSelection().selection().cell();
        var columnId = datasetColumnId(dataset, binding, originalCell.columnIndex());
        if (columnId.isEmpty()) {
            return new EditResult(
                    current().document(),
                    new TableEditingSelection(blockIndex, current().tableEditingSelection().selection()),
                    Optional.empty(),
                    false);
        }

        var replacementText = tableEditor.cellText(result.table(), originalCell);
        if (originalCell.rowIndex() == 0 && replacementText.trim().isEmpty()) {
            return new EditResult(
                    current().document(),
                    new TableEditingSelection(blockIndex, current().tableEditingSelection().selection()),
                    Optional.empty(),
                    false);
        }
        var updatedDataset = originalCell.rowIndex() == 0
                ? dataset.withColumnDisplayName(columnId.orElseThrow(), replacementText)
                : dataset.withCell(
                        originalCell.rowIndex() - 1,
                        columnId.orElseThrow(),
                        datasetValueFromText(replacementText, dataset.column(columnId.orElseThrow()).orElseThrow()));
        if (updatedDataset.equals(dataset)) {
            return new EditResult(
                    current().document(),
                    new TableEditingSelection(blockIndex, result.selection()),
                    result.explicitTypingMarks(),
                    false);
        }
        var datasets = new java.util.ArrayList<>(current().document().datasets());
        datasets.set(datasetIndex, updatedDataset);
        var document = new Document(current().document().blocks(), datasets);
        return new EditResult(
                document,
                new TableEditingSelection(blockIndex, result.selection()),
                result.explicitTypingMarks(),
                true);
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
        blocks.set(blockIndex, currentEquation().withExpression(result.expression()));
        var edit = new EditResult(
                withCurrentDatasets(blocks),
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

    private Optional<ClipboardCopyResult> copyInlineContentForClipboard() {
        if (!current().hasSelection() || !current().selectionRange().isSingleBlock()) {
            return Optional.empty();
        }
        var range = current().selectionRange();
        var block = current().document().blocks().get(range.start().blockIndex());
        if (!EditableInlineBlock.supports(block)) {
            return Optional.empty();
        }
        var selected = InlineContentEditor.slice(
                EditableInlineBlock.contentOf(block),
                range.start().characterOffset(),
                range.end().characterOffset());
        if (selected.nodes().stream().noneMatch(CrossReference.class::isInstance)) {
            return Optional.empty();
        }
        return clipboard.copy(current())
                .map(text -> ClipboardCopyResult.structured(text, new InlineContentClipboardPayload(selected)));
    }

    private Optional<ClipboardCutResult> cutInlineContentForClipboard() {
        var copy = copyInlineContentForClipboard();
        if (copy.isEmpty()) {
            return Optional.empty();
        }
        var result = editor.replaceRange(current().document(), current().selectionRange(), "");
        if (!result.changed()) {
            return Optional.empty();
        }
        var copied = copy.orElseThrow();
        return Optional.of(new ClipboardCutResult(copied.plainText(), copied.payload(), result));
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
        if (block instanceof EquationBlock equation) {
            return Optional.of(ClipboardCopyResult.structured(
                    documentPlainTextSerializer.serializeBlock(current().document(), blockIndex, equation),
                    new DocumentBlockClipboardPayload(equation)));
        }
        if (block instanceof FigureBlock figure) {
            var number = FigureNumbering.numberFor(current().document(), blockIndex).orElseThrow();
            return Optional.of(ClipboardCopyResult.structured(
                    figurePlainTextSerializer.serialize(current().document(), figure, number),
                    new FigureClipboardPayload(figure)));
        }
        if (block instanceof Heading || block instanceof TableOfContentsBlock) {
            return Optional.of(ClipboardCopyResult.structured(
                    documentPlainTextSerializer.serializeBlock(current().document(), blockIndex, block),
                    new DocumentBlockClipboardPayload(block)));
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
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isFigureCaptionSelection()) {
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
            blocks.set(blockIndex, remapTableForPaste(table, Optional.of(blockIndex)));
            var document = withCurrentDatasets(blocks);
            return history.applyEdit(new EditResult(
                    document,
                    new BlockSelection(blockIndex),
                    Optional.empty(),
                    !document.equals(current().document())));
        }
        return history.applyEdit(editor.insertBlock(current(), remapTableForPaste(table, Optional.empty())));
    }

    private boolean supportsPastePlotFromClipboard() {
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
            var document = withCurrentDatasets(blocks);
            return history.applyEdit(new EditResult(
                    document,
                    new BlockSelection(blockIndex),
                    Optional.empty(),
                    !document.equals(current().document())));
        }
        return history.applyEdit(editor.insertBlock(current(), plot));
    }

    private boolean supportsPasteDiagramFromClipboard() {
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
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
            var document = withCurrentDatasets(blocks);
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

    private boolean supportsPasteFigureFromClipboard() {
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
            return false;
        }
        if (current().isBlockSelection()) {
            return current().document().blocks().get(current().blockSelection().blockIndex()) instanceof FigureBlock;
        }
        return editor.supportsInsertBlock(current());
    }

    private boolean pasteFigureFromClipboard(FigureBlock figure) {
        if (!supportsPasteFigureFromClipboard()) {
            return false;
        }
        var replacing = current().isBlockSelection()
                ? Optional.of(current().blockSelection().blockIndex())
                : Optional.<Integer>empty();
        var pasted = figure.withId(uniqueFigureId(current().document(), figure.id(), replacing));
        if (current().isBlockSelection()) {
            var blockIndex = current().blockSelection().blockIndex();
            if (!(current().document().blocks().get(blockIndex) instanceof FigureBlock)) {
                return false;
            }
            var blocks = new java.util.ArrayList<>(current().document().blocks());
            blocks.set(blockIndex, pasted);
            var document = withCurrentDatasets(blocks);
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
        return history.applyEdit(editor.insertBlock(current(), pasted));
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
        blocks.set(blockIndex, currentEquation().withExpression(result.expression()));
        var edit = new EditResult(
                withCurrentDatasets(blocks),
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

    private static Optional<FigureClipboardPayload> figureFromClipboard(Optional<ScholarClipboardPayload> payload) {
        return payload.filter(FigureClipboardPayload.class::isInstance)
                .map(FigureClipboardPayload.class::cast);
    }

    private static Optional<InlineContentClipboardPayload> inlineContentFromClipboard(Optional<ScholarClipboardPayload> payload) {
        return payload.filter(InlineContentClipboardPayload.class::isInstance)
                .map(InlineContentClipboardPayload.class::cast);
    }

    private static Optional<DocumentBlockClipboardPayload> documentBlockFromClipboard(Optional<ScholarClipboardPayload> payload) {
        return payload.filter(DocumentBlockClipboardPayload.class::isInstance)
                .map(DocumentBlockClipboardPayload.class::cast);
    }

    private static Optional<DatasetClipboardPayload> datasetFromClipboard(Optional<ScholarClipboardPayload> payload) {
        return payload.filter(DatasetClipboardPayload.class::isInstance)
                .map(DatasetClipboardPayload.class::cast);
    }

    private boolean supportsPasteInlineContentFromClipboard() {
        return !current().isBlockSelection()
                && !current().isEquationEditingSelection()
                && !current().isTableEditingSelection()
                && !current().isPlotEditingSelection()
                && !current().isDiagramEditingSelection()
                && !current().isFigureCaptionSelection()
                && (!current().hasSelection() || current().selectionRange().isSingleBlock());
    }

    private boolean pasteInlineContentFromClipboard(InlineContent content) {
        if (!supportsPasteInlineContentFromClipboard()) {
            return false;
        }
        return history.applyEdit(editor.insertInlineContent(current(), content));
    }

    private boolean supportsPasteDocumentBlockFromClipboard(BlockNode block) {
        if (!(block instanceof Heading || block instanceof EquationBlock || block instanceof TableOfContentsBlock)) {
            return false;
        }
        if (current().isEquationEditingSelection() || current().isTableEditingSelection() || current().isPlotEditingSelection() || current().isDiagramEditingSelection() || current().isFigureCaptionSelection()) {
            return false;
        }
        return editor.supportsInsertBlock(current());
    }

    private boolean pasteDocumentBlockFromClipboard(BlockNode block) {
        if (!supportsPasteDocumentBlockFromClipboard(block)) {
            return false;
        }
        return history.applyEdit(editor.insertBlock(current(), remapDocumentBlockForPaste(block)));
    }

    private BlockNode remapDocumentBlockForPaste(BlockNode block) {
        if (block instanceof Heading heading && heading.id().isPresent()) {
            return heading.withId(uniqueHeadingId(heading.id().orElseThrow()));
        }
        if (block instanceof EquationBlock equation && equation.id().isPresent()) {
            return equation.withId(uniqueEquationId(equation.id().orElseThrow()));
        }
        return block;
    }

    private TableBlock remapTableForPaste(TableBlock table, Optional<Integer> replacingBlockIndex) {
        if (table.id().isEmpty()) {
            return table;
        }
        return table.withId(uniqueTableId(table.id().orElseThrow(), replacingBlockIndex));
    }

    private String uniqueHeadingId(String baseId) {
        var existing = new java.util.HashSet<String>();
        for (var block : current().document().blocks()) {
            if (block instanceof Heading heading) {
                heading.id().ifPresent(existing::add);
            }
        }
        if (!existing.contains(baseId)) {
            return baseId;
        }
        var suffix = 2;
        while (existing.contains(baseId + "-" + suffix)) {
            suffix++;
        }
        return baseId + "-" + suffix;
    }

    private String uniqueEquationId(String baseId) {
        var existing = new java.util.HashSet<String>();
        for (var block : current().document().blocks()) {
            if (block instanceof EquationBlock equation) {
                equation.id().ifPresent(existing::add);
            }
        }
        return uniqueId(baseId, "equation", existing);
    }

    private String uniqueTableId(String baseId, Optional<Integer> replacingBlockIndex) {
        var existing = new java.util.HashSet<String>();
        for (var index = 0; index < current().document().blocks().size(); index++) {
            if (replacingBlockIndex.isPresent() && replacingBlockIndex.orElseThrow() == index) {
                continue;
            }
            var block = current().document().blocks().get(index);
            if (block instanceof TableBlock table) {
                table.id().ifPresent(existing::add);
            }
        }
        return uniqueId(baseId, "table", existing);
    }

    private static String uniqueId(String baseId, String fallback, java.util.Set<String> existing) {
        var base = Objects.requireNonNull(baseId, "baseId").trim();
        if (base.isEmpty()) {
            base = fallback;
        }
        if (!existing.contains(base)) {
            return base;
        }
        var suffix = 2;
        while (existing.contains(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }

    private boolean replaceDataset(String datasetId, java.util.function.UnaryOperator<ScientificDataset> replacement) {
        Objects.requireNonNull(datasetId, "datasetId");
        Objects.requireNonNull(replacement, "replacement");
        var datasets = new java.util.ArrayList<>(current().document().datasets());
        for (var index = 0; index < datasets.size(); index++) {
            if (datasets.get(index).id().equals(datasetId)) {
                var updated = replacement.apply(datasets.get(index));
                if (updated.equals(datasets.get(index))) {
                    return false;
                }
                datasets.set(index, updated);
                return history.applyEdit(new EditResult(new Document(current().document().blocks(), datasets), current().selection(), current().explicitTypingMarks(), true));
            }
        }
        return false;
    }

    private String uniqueDatasetId(String baseId) {
        var existing = current().document().datasets().stream()
                .map(ScientificDataset::id)
                .collect(java.util.stream.Collectors.toSet());
        if (!existing.contains(baseId)) {
            return baseId;
        }
        var suffix = 2;
        while (existing.contains(baseId + "-" + suffix)) {
            suffix++;
        }
        return baseId + "-" + suffix;
    }

    private static ScientificDataset defaultDataset(String id) {
        return new ScientificDataset(
                id,
                "Projectile Test",
                List.of(
                        new DatasetColumn("time", "Time", DatasetColumnType.NUMBER),
                        new DatasetColumn("height", "Height", DatasetColumnType.NUMBER)),
                List.of(
                        new DatasetRow(List.of(DatasetValue.number("0.0"), DatasetValue.number("0.00"))),
                        new DatasetRow(List.of(DatasetValue.number("0.5"), DatasetValue.number("3.78"))),
                        new DatasetRow(List.of(DatasetValue.number("1.0"), DatasetValue.number("5.10"))),
                        new DatasetRow(List.of(DatasetValue.number("1.5"), DatasetValue.number("3.98"))),
                        new DatasetRow(List.of(DatasetValue.number("2.0"), DatasetValue.number("0.42")))));
    }

    private static List<PlotSeries> replaceFirstSeriesBinding(List<PlotSeries> source, DatasetPlotBinding binding) {
        var updated = new java.util.ArrayList<>(source);
        updated.set(0, source.get(0).withDatasetBinding(binding));
        return List.copyOf(updated);
    }

    private EquationBlock currentEquation() {
        var selection = current().equationEditingSelection();
        return (EquationBlock) current().document().blocks().get(selection.blockIndex());
    }

    private TableBlock currentTable() {
        var selection = current().tableEditingSelection();
        var table = (TableBlock) current().document().blocks().get(selection.blockIndex());
        if (table.datasetBinding().isPresent()) {
            return datasetTableResolver.resolve(current().document(), table);
        }
        return table;
    }

    private boolean isEditingDatasetBackedTable() {
        if (!current().isTableEditingSelection()) {
            return false;
        }
        return current().document().blocks().get(current().tableEditingSelection().blockIndex()) instanceof TableBlock table
                && table.datasetBinding().isPresent();
    }

    private int datasetIndex(String datasetId) {
        for (var index = 0; index < current().document().datasets().size(); index++) {
            if (current().document().datasets().get(index).id().equals(datasetId)) {
                return index;
            }
        }
        return -1;
    }

    private static Optional<String> datasetColumnId(ScientificDataset dataset, DatasetTableBinding binding, int visibleColumnIndex) {
        if (visibleColumnIndex < 0) {
            return Optional.empty();
        }
        if (binding.usesAllColumns()) {
            return visibleColumnIndex < dataset.columns().size()
                    ? Optional.of(dataset.columns().get(visibleColumnIndex).id())
                    : Optional.empty();
        }
        return visibleColumnIndex < binding.columnIds().size()
                ? Optional.of(binding.columnIds().get(visibleColumnIndex))
                : Optional.empty();
    }

    private static DatasetValue datasetValueFromText(String text, DatasetColumn column) {
        var trimmed = Objects.requireNonNull(text, "text").trim();
        if (trimmed.isEmpty()) {
            return DatasetValue.missing();
        }
        if (column.type() == DatasetColumnType.NUMBER) {
            try {
                return DatasetValue.number(new BigDecimal(trimmed));
            } catch (NumberFormatException exception) {
                return DatasetValue.text(text);
            }
        }
        return DatasetValue.text(text);
    }

    private Document withCurrentDatasets(List<BlockNode> blocks) {
        return new Document(blocks, current().document().datasets());
    }

    private PlotBlock currentPlot() {
        var selection = current().plotEditingSelection();
        var block = current().document().blocks().get(selection.blockIndex());
        if (block instanceof PlotBlock plotBlock) {
            return plotBlock;
        }
        if (block instanceof FigureBlock figureBlock && figureBlock.content() instanceof PlotBlock plotBlock) {
            return plotBlock;
        }
        throw new IllegalStateException("Current selection does not target a plot block.");
    }

    private DiagramBlock currentDiagram() {
        var selection = current().diagramEditingSelection();
        var block = current().document().blocks().get(selection.blockIndex());
        if (block instanceof DiagramBlock diagramBlock) {
            return diagramBlock;
        }
        if (block instanceof FigureBlock figureBlock && figureBlock.content() instanceof DiagramBlock diagramBlock) {
            return diagramBlock;
        }
        throw new IllegalStateException("Current selection does not target a diagram block.");
    }

    private static BlockNode replacePlotContent(BlockNode block, PlotBlock plot) {
        if (block instanceof FigureBlock figureBlock) {
            return figureBlock.withContent(plot);
        }
        return plot;
    }

    private static BlockNode replaceDiagramContent(BlockNode block, DiagramBlock diagram) {
        if (block instanceof FigureBlock figureBlock) {
            return figureBlock.withContent(diagram);
        }
        return diagram;
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

    private FigureBlock currentFigureForCaption() {
        var selection = current().figureCaptionSelection();
        return (FigureBlock) current().document().blocks().get(selection.blockIndex());
    }

    private static InlineContent captionFromText(String text) {
        Objects.requireNonNull(text, "text");
        return text.isEmpty()
                ? new InlineContent(List.of())
                : new InlineContent(List.of((InlineNode) new Text(text, Set.of())));
    }

    private static String captionText(InlineContent caption) {
        return InlineContentEditor.logicalText(caption);
    }

    private static int captionLength(InlineContent caption) {
        return TextBoundary.characterCount(captionText(caption));
    }

    private static String uniqueFigureId(Document document, String base, Optional<Integer> replacingBlockIndex) {
        var sanitized = Objects.requireNonNull(base, "base").trim().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("^-+|-+$", "");
        if (sanitized.isEmpty()) {
            sanitized = "figure";
        }
        var candidate = sanitized;
        var suffix = 2;
        while (figureIdExists(document, candidate, replacingBlockIndex)) {
            candidate = sanitized + "-" + suffix++;
        }
        return candidate;
    }

    private static boolean figureIdExists(Document document, String id, Optional<Integer> replacingBlockIndex) {
        for (var index = 0; index < document.blocks().size(); index++) {
            if (replacingBlockIndex.isPresent() && replacingBlockIndex.orElseThrow() == index) {
                continue;
            }
            if (document.blocks().get(index) instanceof FigureBlock figureBlock && figureBlock.id().equals(id)) {
                return true;
            }
        }
        return false;
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
