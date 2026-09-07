package dev.rgcb.scholar.layout;

import java.util.List;

public record LaidOutTableCell(
        int rowIndex,
        int columnIndex,
        int x,
        int y,
        int width,
        int height,
        int contentX,
        int contentY,
        int contentWidth,
        int contentHeight,
        List<LaidOutLine> lines
) {
    public LaidOutTableCell {
        if (rowIndex < 0) {
            throw new IllegalArgumentException("rowIndex must not be negative.");
        }
        if (columnIndex < 0) {
            throw new IllegalArgumentException("columnIndex must not be negative.");
        }
        if (width < 0 || height < 0 || contentWidth < 0 || contentHeight < 0) {
            throw new IllegalArgumentException("dimensions must not be negative.");
        }
        lines = List.copyOf(lines);
    }
}
