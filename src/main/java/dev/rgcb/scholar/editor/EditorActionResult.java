package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.math.editor.SemanticMathTokenKind;
import java.util.Optional;

public record EditorActionResult(
        boolean documentChanged,
        boolean caretShouldBeVisible,
        Optional<SemanticMathTokenKind> semanticTokenPopup
) {
    public static final EditorActionResult NONE = new EditorActionResult(false, false, Optional.empty());
    public static final EditorActionResult DOCUMENT_CHANGED = new EditorActionResult(true, true, Optional.empty());

    public EditorActionResult(boolean documentChanged, boolean caretShouldBeVisible) {
        this(documentChanged, caretShouldBeVisible, Optional.empty());
    }

    public EditorActionResult {
        semanticTokenPopup = semanticTokenPopup == null ? Optional.empty() : semanticTokenPopup;
    }

    public static EditorActionResult semanticTokenPopup(SemanticMathTokenKind kind) {
        return new EditorActionResult(false, false, Optional.of(kind));
    }
}
