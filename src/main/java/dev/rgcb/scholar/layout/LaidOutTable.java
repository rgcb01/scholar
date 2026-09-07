package dev.rgcb.scholar.layout;

import java.util.List;

public record LaidOutTable(int x, int y, int width, int height, int headerRowCount, List<LaidOutTableRow> rows) {
    public LaidOutTable {
        if (width < 0) {
            throw new IllegalArgumentException("width must not be negative.");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative.");
        }
        if (headerRowCount < 0 || headerRowCount > 1) {
            throw new IllegalArgumentException("headerRowCount must be 0 or 1.");
        }
        rows = List.copyOf(rows);
        if (headerRowCount > rows.size()) {
            throw new IllegalArgumentException("headerRowCount must not exceed row count.");
        }
    }
}
