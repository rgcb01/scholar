package dev.rgcb.scholar.document;

import java.util.Objects;

public record TableCell(TableCellContent content) {
    public TableCell {
        content = Objects.requireNonNull(content, "content");
    }

    public static TableCell empty() {
        return new TableCell(TableCellContent.empty());
    }
}
