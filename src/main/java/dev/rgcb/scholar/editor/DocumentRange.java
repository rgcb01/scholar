package dev.rgcb.scholar.editor;

import java.util.Objects;

public record DocumentRange(DocumentPosition start, DocumentPosition end) {
    public DocumentRange {
        start = Objects.requireNonNull(start, "start");
        end = Objects.requireNonNull(end, "end");
        if (DocumentPositions.compare(start, end) > 0) {
            throw new IllegalArgumentException("start must not be after end.");
        }
    }

    public static DocumentRange caret(DocumentPosition position) {
        return new DocumentRange(position, position);
    }

    public static DocumentRange between(DocumentPosition first, DocumentPosition second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return DocumentPositions.isBeforeOrEqual(first, second)
                ? new DocumentRange(first, second)
                : new DocumentRange(second, first);
    }

    public boolean isEmpty() {
        return start.equals(end);
    }

    public boolean isSingleBlock() {
        return start.blockIndex() == end.blockIndex();
    }
}
