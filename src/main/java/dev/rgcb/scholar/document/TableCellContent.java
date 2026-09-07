package dev.rgcb.scholar.document;

import java.util.List;
import java.util.Objects;

public record TableCellContent(InlineContent content) {
    public TableCellContent {
        content = Objects.requireNonNull(content, "content");
    }

    public static TableCellContent empty() {
        return new TableCellContent(new InlineContent(List.of()));
    }
}
