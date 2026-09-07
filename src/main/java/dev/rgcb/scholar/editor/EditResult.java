package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.TextMark;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record EditResult(
        Document document,
        EditorSelection selection,
        Optional<Set<TextMark>> explicitTypingMarks,
        boolean changed
) {
    public EditResult(Document document, DocumentPosition caret, boolean changed) {
        this(document, new TextSelection(caret, caret), Optional.empty(), changed);
    }

    public EditResult(Document document, DocumentPosition caret, Optional<Set<TextMark>> explicitTypingMarks, boolean changed) {
        this(document, new TextSelection(caret, caret), explicitTypingMarks, changed);
    }

    public EditResult(Document document, DocumentPosition anchor, DocumentPosition active, boolean changed) {
        this(document, new TextSelection(anchor, active), Optional.empty(), changed);
    }

    public EditResult(Document document, DocumentPosition anchor, DocumentPosition active, Optional<Set<TextMark>> explicitTypingMarks, boolean changed) {
        this(document, new TextSelection(anchor, active), explicitTypingMarks, changed);
    }

    public EditResult {
        document = Objects.requireNonNull(document, "document");
        selection = Objects.requireNonNull(selection, "selection");
        explicitTypingMarks = Objects.requireNonNull(explicitTypingMarks, "explicitTypingMarks")
                .map(Set::copyOf);
    }

    public DocumentPosition caret() {
        return editorState().caret();
    }

    public DocumentPosition anchor() {
        return editorState().anchor();
    }

    public DocumentPosition active() {
        return editorState().active();
    }

    public EditorState editorState() {
        return new EditorState(document, selection, explicitTypingMarks);
    }
}
