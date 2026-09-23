package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.math.editor.SemanticMathTokenKind;
import java.util.Optional;

public record EditorActionResult(
        boolean documentChanged,
        boolean caretShouldBeVisible,
        Optional<SemanticMathTokenKind> semanticTokenPopup,
        boolean crossReferencePopup,
        boolean toggleOutline,
        Optional<ComputationDialogKind> computationDialog
) {
    public static final EditorActionResult NONE = new EditorActionResult(false, false, Optional.empty(), false, false);
    public static final EditorActionResult DOCUMENT_CHANGED = new EditorActionResult(true, true, Optional.empty(), false, false);

    public EditorActionResult(boolean documentChanged, boolean caretShouldBeVisible,
                              Optional<SemanticMathTokenKind> semanticTokenPopup, boolean crossReferencePopup,
                              boolean toggleOutline) {
        this(documentChanged, caretShouldBeVisible, semanticTokenPopup, crossReferencePopup, toggleOutline, Optional.empty());
    }

    public EditorActionResult(boolean documentChanged, boolean caretShouldBeVisible) {
        this(documentChanged, caretShouldBeVisible, Optional.empty(), false, false);
    }

    public EditorActionResult(boolean documentChanged, boolean caretShouldBeVisible, Optional<SemanticMathTokenKind> semanticTokenPopup) {
        this(documentChanged, caretShouldBeVisible, semanticTokenPopup, false, false);
    }

    public EditorActionResult {
        semanticTokenPopup = semanticTokenPopup == null ? Optional.empty() : semanticTokenPopup;
        computationDialog = computationDialog == null ? Optional.empty() : computationDialog;
    }

    public static EditorActionResult semanticTokenPopup(SemanticMathTokenKind kind) {
        return new EditorActionResult(false, false, Optional.of(kind), false, false);
    }

    public static EditorActionResult openCrossReferencePopup() {
        return new EditorActionResult(false, false, Optional.empty(), true, false);
    }

    public static EditorActionResult requestToggleOutline() {
        return new EditorActionResult(false, false, Optional.empty(), false, true);
    }

    public static EditorActionResult openComputationDialog(ComputationDialogKind kind) {
        return new EditorActionResult(false, false, Optional.empty(), false, false, Optional.of(kind));
    }
}
