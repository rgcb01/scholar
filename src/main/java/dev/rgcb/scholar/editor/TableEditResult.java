package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TextMark;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record TableEditResult(
        TableBlock table,
        TableCellTextSelection selection,
        Optional<Set<TextMark>> explicitTypingMarks,
        boolean changed
) {
    public TableEditResult {
        table = Objects.requireNonNull(table, "table");
        selection = Objects.requireNonNull(selection, "selection");
        explicitTypingMarks = Objects.requireNonNull(explicitTypingMarks, "explicitTypingMarks").map(Set::copyOf);
    }
}
