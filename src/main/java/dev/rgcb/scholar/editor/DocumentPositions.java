package dev.rgcb.scholar.editor;

import java.util.Objects;

public final class DocumentPositions {
    private DocumentPositions() {
    }

    public static int compare(DocumentPosition first, DocumentPosition second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        var blockComparison = Integer.compare(first.blockIndex(), second.blockIndex());
        return blockComparison != 0
                ? blockComparison
                : Integer.compare(first.characterOffset(), second.characterOffset());
    }

    public static boolean isBeforeOrEqual(DocumentPosition first, DocumentPosition second) {
        return compare(first, second) <= 0;
    }
}
