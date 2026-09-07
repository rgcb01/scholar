package dev.rgcb.scholar.layout;

import java.util.List;

public record LaidOutTableRow(int rowIndex, int x, int y, int width, int height, List<LaidOutTableCell> cells) {
    public LaidOutTableRow {
        if (rowIndex < 0) {
            throw new IllegalArgumentException("rowIndex must not be negative.");
        }
        if (width < 0) {
            throw new IllegalArgumentException("width must not be negative.");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative.");
        }
        cells = List.copyOf(cells);
    }
}
