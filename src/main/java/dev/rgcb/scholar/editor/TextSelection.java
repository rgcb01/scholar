package dev.rgcb.scholar.editor;

import java.util.Objects;

public record TextSelection(DocumentPosition anchor, DocumentPosition active) implements EditorSelection {
    public TextSelection {
        anchor = Objects.requireNonNull(anchor, "anchor");
        active = Objects.requireNonNull(active, "active");
    }

    public boolean isCaret() {
        return anchor.equals(active);
    }

    public DocumentRange range() {
        return DocumentRange.between(anchor, active);
    }
}
