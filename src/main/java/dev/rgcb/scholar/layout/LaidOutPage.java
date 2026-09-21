package dev.rgcb.scholar.layout;

import java.util.List;

public record LaidOutPage(
        int index,
        int x,
        int y,
        int width,
        int height,
        int contentX,
        int contentY,
        int contentWidth,
        int contentHeight,
        List<LaidOutColumn> columns,
        String headerText,
        String footerText,
        boolean pageNumberVisible
) {
    public LaidOutPage {
        if (index < 0 || width <= 0 || height <= 0 || contentWidth <= 0 || contentHeight <= 0) {
            throw new IllegalArgumentException("Page geometry must be positive and its index non-negative.");
        }
        columns = List.copyOf(columns);
        headerText = java.util.Objects.requireNonNull(headerText, "headerText");
        footerText = java.util.Objects.requireNonNull(footerText, "footerText");
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("A page must contain at least one column.");
        }
    }
}
