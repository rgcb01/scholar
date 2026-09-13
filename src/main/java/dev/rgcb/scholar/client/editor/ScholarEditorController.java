package dev.rgcb.scholar.client.editor;

import dev.rgcb.scholar.editor.ActionSelectionState;
import dev.rgcb.scholar.editor.BlockStyle;
import dev.rgcb.scholar.editor.BlockStyleSelectionState;
import dev.rgcb.scholar.editor.ClipboardAdapter;
import dev.rgcb.scholar.clipboard.ScholarClipboardService;
import dev.rgcb.scholar.editor.EditorAction;
import dev.rgcb.scholar.editor.EditorActionContext;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.editor.EditorState;
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.math.editor.MathSelection;
import dev.rgcb.scholar.math.editor.SemanticMathTokenDraft;
import dev.rgcb.scholar.math.editor.SemanticMathTokenKind;
import dev.rgcb.scholar.editor.TableCellTextSelection;
import dev.rgcb.scholar.editor.PlotEditTarget;
import dev.rgcb.scholar.editor.DiagramEditTarget;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.CrossReferenceTarget;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.editor.ElectricalComponentDraft;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ScholarEditorController {
    private static final ScholarClipboardService PROCESS_CLIPBOARD = new ScholarClipboardService();

    private final EditorSession session;
    private final ClipboardAdapter clipboard;
    private final ScholarClipboardService scholarClipboard;
    private final Runnable relayout;
    private final Runnable keepCaretVisible;
    private final Consumer<SemanticMathTokenKind> semanticTokenPopup;
    private final Runnable crossReferencePopup;
    private final Runnable toggleOutline;
    private final Supplier<LaidOutDocument> laidOutDocument;
    private final Supplier<TextMeasurer> textMeasurer;

    public ScholarEditorController(
            EditorSession session,
            ClipboardAdapter clipboard,
            Runnable relayout,
            Runnable keepCaretVisible
    ) {
        this(session, clipboard, relayout, keepCaretVisible, () -> null, () -> null, kind -> {
        }, () -> {
        }, () -> {
        });
    }

    public ScholarEditorController(
            EditorSession session,
            ClipboardAdapter clipboard,
            Runnable relayout,
            Runnable keepCaretVisible,
            Consumer<SemanticMathTokenKind> semanticTokenPopup
    ) {
        this(session, clipboard, relayout, keepCaretVisible, () -> null, () -> null, semanticTokenPopup, () -> {
        }, () -> {
        });
    }

    public ScholarEditorController(
            EditorSession session,
            ClipboardAdapter clipboard,
            Runnable relayout,
            Runnable keepCaretVisible,
            Supplier<LaidOutDocument> laidOutDocument,
            Supplier<TextMeasurer> textMeasurer
    ) {
        this(session, clipboard, relayout, keepCaretVisible, laidOutDocument, textMeasurer, kind -> {
        }, () -> {
        }, () -> {
        });
    }

    public ScholarEditorController(
            EditorSession session,
            ClipboardAdapter clipboard,
            Runnable relayout,
            Runnable keepCaretVisible,
            Supplier<LaidOutDocument> laidOutDocument,
            Supplier<TextMeasurer> textMeasurer,
            Consumer<SemanticMathTokenKind> semanticTokenPopup
    ) {
        this(session, clipboard, relayout, keepCaretVisible, laidOutDocument, textMeasurer, semanticTokenPopup, () -> {
        }, () -> {
        });
    }

    public ScholarEditorController(
            EditorSession session,
            ClipboardAdapter clipboard,
            Runnable relayout,
            Runnable keepCaretVisible,
            Supplier<LaidOutDocument> laidOutDocument,
            Supplier<TextMeasurer> textMeasurer,
            Consumer<SemanticMathTokenKind> semanticTokenPopup,
            Runnable crossReferencePopup,
            Runnable toggleOutline
    ) {
        this.session = Objects.requireNonNull(session, "session");
        this.clipboard = Objects.requireNonNull(clipboard, "clipboard");
        scholarClipboard = PROCESS_CLIPBOARD;
        this.relayout = Objects.requireNonNull(relayout, "relayout");
        this.keepCaretVisible = Objects.requireNonNull(keepCaretVisible, "keepCaretVisible");
        this.laidOutDocument = Objects.requireNonNull(laidOutDocument, "laidOutDocument");
        this.textMeasurer = Objects.requireNonNull(textMeasurer, "textMeasurer");
        this.semanticTokenPopup = Objects.requireNonNull(semanticTokenPopup, "semanticTokenPopup");
        this.crossReferencePopup = Objects.requireNonNull(crossReferencePopup, "crossReferencePopup");
        this.toggleOutline = Objects.requireNonNull(toggleOutline, "toggleOutline");
    }

    public EditorSession session() {
        return session;
    }

    public EditorState state() {
        return session.current();
    }

    public boolean isEnabled(EditorAction action) {
        return action.isEnabled(context());
    }

    public ActionSelectionState selectionState(EditorAction action) {
        return action.selectionState(context());
    }

    public Optional<BlockStyle> blockStyle() {
        return session.blockStyle();
    }

    public BlockStyleSelectionState blockStyleSelectionState() {
        return session.blockStyleSelectionState();
    }

    public void execute(EditorAction action) {
        var result = action.execute(context());
        result.semanticTokenPopup().ifPresent(semanticTokenPopup);
        if (result.crossReferencePopup()) {
            crossReferencePopup.run();
        }
        if (result.toggleOutline()) {
            toggleOutline.run();
        }
        if (result.documentChanged()) {
            relayout.run();
        }
        if (result.caretShouldBeVisible()) {
            keepCaretVisible.run();
        }
    }

    public void typeText(String text) {
        if (session.typeText(text)) {
            relayout.run();
        }
        keepCaretVisible.run();
    }

    public void enter() {
        if (session.enter()) {
            relayout.run();
        }
        keepCaretVisible.run();
    }

    public void deleteBackward() {
        if (session.deleteBackward()) {
            relayout.run();
        }
        keepCaretVisible.run();
    }

    public void deleteForward() {
        if (session.deleteForward()) {
            relayout.run();
        }
        keepCaretVisible.run();
    }

    public void moveLeft() {
        session.moveLeft();
        keepCaretVisible.run();
    }

    public void moveRight() {
        session.moveRight();
        keepCaretVisible.run();
    }

    public void extendLeft() {
        session.extendLeft();
        keepCaretVisible.run();
    }

    public void extendRight() {
        session.extendRight();
        keepCaretVisible.run();
    }

    public void moveUp() {
        withNavigationContext((layout, measurer) -> session.moveUp(layout, measurer));
    }

    public void moveDown() {
        withNavigationContext((layout, measurer) -> session.moveDown(layout, measurer));
    }

    public void extendUp() {
        withNavigationContext((layout, measurer) -> session.extendUp(layout, measurer));
    }

    public void extendDown() {
        withNavigationContext((layout, measurer) -> session.extendDown(layout, measurer));
    }

    public void moveHome() {
        var layout = laidOutDocument.get();
        if (layout != null) {
            session.moveHome(layout);
        }
        keepCaretVisible.run();
    }

    public void moveEnd() {
        var layout = laidOutDocument.get();
        if (layout != null) {
            session.moveEnd(layout);
        }
        keepCaretVisible.run();
    }

    public void extendHome() {
        var layout = laidOutDocument.get();
        if (layout != null) {
            session.extendHome(layout);
        }
        keepCaretVisible.run();
    }

    public void extendEnd() {
        var layout = laidOutDocument.get();
        if (layout != null) {
            session.extendEnd(layout);
        }
        keepCaretVisible.run();
    }

    public void selectAll() {
        session.selectAll();
        keepCaretVisible.run();
    }

    public void setState(EditorState state) {
        session.setCurrent(state);
        keepCaretVisible.run();
    }

    public void setEquationEditingSelection(MathSelection selection) {
        session.setEquationEditingSelection(selection);
        keepCaretVisible.run();
    }

    public void setTableEditingSelection(TableCellTextSelection selection) {
        session.setTableEditingSelection(selection);
        keepCaretVisible.run();
    }

    public void moveNextTableCell() {
        session.moveNextTableCell();
        keepCaretVisible.run();
    }

    public void movePreviousTableCell() {
        session.movePreviousTableCell();
        keepCaretVisible.run();
    }

    public void exitTableEditing() {
        session.exitTableEditing();
        keepCaretVisible.run();
    }

    public void moveNextPlotTarget() {
        session.moveNextPlotTarget();
        keepCaretVisible.run();
    }

    public void movePreviousPlotTarget() {
        session.movePreviousPlotTarget();
        keepCaretVisible.run();
    }

    public void enterPlotEditing(int blockIndex, PlotEditTarget target) {
        session.enterPlotEditing(blockIndex, target);
        keepCaretVisible.run();
    }

    public void setPlotEditingTarget(PlotEditTarget target) {
        session.setPlotEditingTarget(target);
        keepCaretVisible.run();
    }

    public void exitPlotEditing() {
        session.exitPlotEditing();
        keepCaretVisible.run();
    }

    public void moveNextDiagramTarget() {
        session.moveNextDiagramTarget();
        keepCaretVisible.run();
    }

    public void movePreviousDiagramTarget() {
        session.movePreviousDiagramTarget();
        keepCaretVisible.run();
    }

    public void enterDiagramEditing(int blockIndex, DiagramEditTarget target) {
        session.enterDiagramEditing(blockIndex, target);
        keepCaretVisible.run();
    }

    public void setDiagramEditingTarget(DiagramEditTarget target) {
        session.setDiagramEditingTarget(target);
        keepCaretVisible.run();
    }

    public void exitDiagramEditing() {
        session.exitDiagramEditing();
        keepCaretVisible.run();
    }

    public boolean beginDiagramElementDrag(double logicalX, double logicalY) {
        return session.beginDiagramElementDrag(logicalX, logicalY);
    }

    public Optional<Document> previewDiagramElementDrag(double logicalX, double logicalY) {
        return session.previewDiagramElementDrag(logicalX, logicalY);
    }

    public void commitDiagramElementDrag(double logicalX, double logicalY) {
        session.commitDiagramElementDrag(logicalX, logicalY);
        // Always restore layout from the committed semantic document even when
        // the final clamped position equals the original.
        relayout.run();
        keepCaretVisible.run();
    }

    public void cancelDiagramElementDrag() {
        if (session.cancelDiagramElementDrag()) {
            relayout.run();
        }
        keepCaretVisible.run();
    }

    public boolean isDiagramElementDragging() {
        return session.isDiagramElementDragging();
    }

    public Optional<String> diagramTextValue() {
        return session.diagramTextValue();
    }

    public Optional<DiagramCanvas> diagramCanvas() {
        return session.diagramCanvas();
    }

    public boolean resizeDiagramCanvas(double width, double height) {
        var changed = session.resizeDiagramCanvas(width, height);
        if (changed) {
            relayout.run();
        }
        keepCaretVisible.run();
        return changed;
    }

    public void applyDiagramText(String text) {
        if (session.applyDiagramText(text)) {
            relayout.run();
        }
        keepCaretVisible.run();
    }

    public boolean diagramConnectionInProgress() {
        return session.diagramConnectionInProgress();
    }

    public boolean canCompleteDiagramConnection() {
        return session.supportsCompleteDiagramConnection();
    }

    public void completeDiagramConnection() {
        if (session.completeDiagramConnection()) {
            relayout.run();
        }
        keepCaretVisible.run();
    }

    public Optional<dev.rgcb.scholar.editor.DiagramPortTarget> diagramConnectionSourceTarget() {
        return session.diagramConnectionSourceTarget();
    }

    public Optional<ElectricalComponent> selectedElectricalComponent() {
        return session.selectedElectricalComponent();
    }

    public Optional<ElectricalComponentDraft> electricalComponentDraft() {
        return session.electricalComponentDraft();
    }

    public void applyElectricalComponentAnnotations(String referenceDesignator, String valueLabel) {
        if (session.applyElectricalComponentAnnotations(referenceDesignator, valueLabel)) {
            relayout.run();
        }
        keepCaretVisible.run();
    }

    public Optional<String> plotTextValue() {
        return session.plotTextValue();
    }

    public Optional<DataPoint> plotPointValue() {
        return session.plotPointValue();
    }

    public void applyPlotText(String text) {
        if (session.applyPlotText(text)) {
            relayout.run();
        }
        keepCaretVisible.run();
    }

    public void applyPlotPoint(double x, double y) {
        if (session.applyPlotPoint(x, y)) {
            relayout.run();
        }
        keepCaretVisible.run();
    }

    public Optional<SemanticMathTokenDraft> semanticTokenDraft(SemanticMathTokenKind kind) {
        return session.semanticTokenDraft(kind);
    }

    public java.util.List<CrossReferenceTarget> availableCrossReferenceTargets() {
        return session.availableCrossReferenceTargets();
    }

    public void insertCrossReference(CrossReferenceTargetKind kind, String targetId) {
        if (session.insertCrossReference(kind, targetId)) {
            relayout.run();
        }
        keepCaretVisible.run();
    }

    public void navigateToHeadingId(String targetId) {
        if (session.navigateToHeadingId(targetId)) {
            keepCaretVisible.run();
        }
    }

    public boolean isValidSemanticTokenContent(SemanticMathTokenKind kind, String content) {
        return session.isValidSemanticTokenContent(kind, content);
    }

    public void applySemanticToken(SemanticMathTokenKind kind, String content) {
        if (session.applySemanticToken(kind, content)) {
            relayout.run();
        }
        keepCaretVisible.run();
    }

    public void clearPreferredCaretX() {
        session.clearPreferredCaretX();
    }

    private EditorActionContext context() {
        return new EditorActionContext(session, clipboard, scholarClipboard);
    }

    private void withNavigationContext(NavigationAction action) {
        var layout = laidOutDocument.get();
        var measurer = textMeasurer.get();
        if (layout != null && measurer != null) {
            action.run(layout, measurer);
        }
        keepCaretVisible.run();
    }

    private interface NavigationAction {
        void run(LaidOutDocument layout, TextMeasurer textMeasurer);
    }
}
