package dev.rgcb.scholar.editor;

/**
 * Names the single semantic owner that should receive editor input for the
 * current selection.
 */
public enum EditorFocusOwner {
    DOCUMENT_TEXT,
    BLOCK,
    EQUATION,
    TABLE,
    PLOT,
    DIAGRAM,
    FIGURE_CAPTION;

    static EditorFocusOwner fromSelection(EditorSelection selection) {
        if (selection instanceof TextSelection) {
            return DOCUMENT_TEXT;
        }
        if (selection instanceof BlockSelection) {
            return BLOCK;
        }
        if (selection instanceof EquationEditingSelection) {
            return EQUATION;
        }
        if (selection instanceof TableEditingSelection) {
            return TABLE;
        }
        if (selection instanceof PlotEditingSelection) {
            return PLOT;
        }
        if (selection instanceof DiagramEditingSelection) {
            return DIAGRAM;
        }
        if (selection instanceof FigureCaptionSelection) {
            return FIGURE_CAPTION;
        }
        throw new IllegalStateException("Unknown editor selection type: " + selection.getClass().getName());
    }
}
