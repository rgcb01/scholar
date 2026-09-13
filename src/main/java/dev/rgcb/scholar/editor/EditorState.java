package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.TextMark;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record EditorState(
        Document document,
        EditorSelection selection,
        Optional<Set<TextMark>> explicitTypingMarks
) {
    public EditorState(Document document, DocumentPosition caret) {
        this(document, new TextSelection(caret, caret), Optional.empty());
    }

    public EditorState(Document document, DocumentPosition anchor, DocumentPosition active) {
        this(document, new TextSelection(anchor, active), Optional.empty());
    }

    public EditorState(Document document, DocumentPosition anchor, DocumentPosition active, Optional<Set<TextMark>> explicitTypingMarks) {
        this(document, new TextSelection(anchor, active), explicitTypingMarks);
    }

    public EditorState {
        document = Objects.requireNonNull(document, "document");
        selection = Objects.requireNonNull(selection, "selection");
        explicitTypingMarks = Objects.requireNonNull(explicitTypingMarks, "explicitTypingMarks")
                .map(Set::copyOf);
        new EditorSelectionValidator().validate(document, selection);
    }

    public DocumentPosition caret() {
        return textSelection().active();
    }

    public DocumentPosition anchor() {
        return textSelection().anchor();
    }

    public DocumentPosition active() {
        return textSelection().active();
    }

    public boolean isTextSelection() {
        return selection instanceof TextSelection;
    }

    public boolean isBlockSelection() {
        return selection instanceof BlockSelection;
    }

    public boolean isEquationEditingSelection() {
        return selection instanceof EquationEditingSelection;
    }

    public boolean isTableEditingSelection() {
        return selection instanceof TableEditingSelection;
    }

    public boolean isPlotEditingSelection() {
        return selection instanceof PlotEditingSelection;
    }

    public boolean isDiagramEditingSelection() {
        return selection instanceof DiagramEditingSelection;
    }

    public boolean isFigureCaptionSelection() {
        return selection instanceof FigureCaptionSelection;
    }

    public TextSelection textSelection() {
        if (selection instanceof TextSelection textSelection) {
            return textSelection;
        }
        throw new IllegalStateException("Current editor selection is not a text selection.");
    }

    public BlockSelection blockSelection() {
        if (selection instanceof BlockSelection blockSelection) {
            return blockSelection;
        }
        throw new IllegalStateException("Current editor selection is not a block selection.");
    }

    public EquationEditingSelection equationEditingSelection() {
        if (selection instanceof EquationEditingSelection equationEditingSelection) {
            return equationEditingSelection;
        }
        throw new IllegalStateException("Current editor selection is not an equation editing selection.");
    }

    public TableEditingSelection tableEditingSelection() {
        if (selection instanceof TableEditingSelection tableEditingSelection) {
            return tableEditingSelection;
        }
        throw new IllegalStateException("Current editor selection is not a table editing selection.");
    }

    public PlotEditingSelection plotEditingSelection() {
        if (selection instanceof PlotEditingSelection plotEditingSelection) {
            return plotEditingSelection;
        }
        throw new IllegalStateException("Current editor selection is not a plot editing selection.");
    }

    public DiagramEditingSelection diagramEditingSelection() {
        if (selection instanceof DiagramEditingSelection diagramEditingSelection) {
            return diagramEditingSelection;
        }
        throw new IllegalStateException("Current editor selection is not a diagram editing selection.");
    }

    public FigureCaptionSelection figureCaptionSelection() {
        if (selection instanceof FigureCaptionSelection figureCaptionSelection) {
            return figureCaptionSelection;
        }
        throw new IllegalStateException("Current editor selection is not a figure caption selection.");
    }

    public boolean hasSelection() {
        return isTextSelection() && !textSelection().isCaret();
    }

    public DocumentRange selectionRange() {
        return textSelection().range();
    }

    public boolean hasMultiBlockSelection() {
        return hasSelection() && !selectionRange().isSingleBlock();
    }

    public EditorState collapseTo(DocumentPosition position) {
        return new EditorState(document, position, position);
    }

    public EditorState selectBlock(int blockIndex) {
        return new EditorState(document, new BlockSelection(blockIndex), Optional.empty());
    }

    public EditorState editEquation(int blockIndex, dev.rgcb.scholar.math.editor.MathSelection mathSelection) {
        return new EditorState(document, new EquationEditingSelection(blockIndex, mathSelection), Optional.empty());
    }

    public EditorState editTable(int blockIndex, TableCellTextSelection tableSelection) {
        return new EditorState(document, new TableEditingSelection(blockIndex, tableSelection), Optional.empty());
    }

    public EditorState editPlot(int blockIndex, PlotEditTarget target) {
        return new EditorState(document, new PlotEditingSelection(blockIndex, target), Optional.empty());
    }

    public EditorState editDiagram(int blockIndex, DiagramEditTarget target) {
        return new EditorState(document, new DiagramEditingSelection(blockIndex, target), Optional.empty());
    }

    public EditorState editFigureCaption(int blockIndex, FigureCaptionSelection captionSelection) {
        if (captionSelection.blockIndex() != blockIndex) {
            throw new IllegalArgumentException("Caption selection block index must match.");
        }
        return new EditorState(document, captionSelection, Optional.empty());
    }

    public EditorState withActive(DocumentPosition position) {
        return new EditorState(document, anchor(), position, explicitTypingMarks);
    }

    public EditorState withExplicitTypingMarks(Set<TextMark> marks) {
        return new EditorState(document, selection, Optional.of(Set.copyOf(marks)));
    }

    public EditorState withoutExplicitTypingMarks() {
        return explicitTypingMarks.isPresent()
                ? new EditorState(document, selection, Optional.empty())
                : this;
    }

}
