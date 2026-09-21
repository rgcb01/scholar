package dev.rgcb.scholar.document;

import java.util.Objects;

/** Changes the active document flow without forcing a page break. */
public record LayoutSectionBreak(ColumnLayout columnLayout) implements BlockNode {
    public LayoutSectionBreak {
        columnLayout = Objects.requireNonNull(columnLayout, "columnLayout");
    }
}
