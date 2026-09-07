package dev.rgcb.scholar.document;

import java.util.List;
import java.util.Objects;

public record TableBlock(List<TableRow> rows, int headerRowCount) implements BlockNode {
    public TableBlock {
        rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("rows must not be empty.");
        }
        if (headerRowCount < 0 || headerRowCount > 1) {
            throw new IllegalArgumentException("headerRowCount must be 0 or 1.");
        }
        if (headerRowCount > rows.size()) {
            throw new IllegalArgumentException("headerRowCount must not exceed row count.");
        }
        var columnCount = rows.get(0).cells().size();
        if (columnCount == 0) {
            throw new IllegalArgumentException("tables must have at least one column.");
        }
        for (var row : rows) {
            if (row.cells().size() != columnCount) {
                throw new IllegalArgumentException("table rows must have equal cell counts.");
            }
        }
    }

    public static TableBlock empty(int rows, int columns) {
        if (rows <= 0) {
            throw new IllegalArgumentException("rows must be positive.");
        }
        if (columns <= 0) {
            throw new IllegalArgumentException("columns must be positive.");
        }
        return new TableBlock(java.util.stream.IntStream.range(0, rows)
                .mapToObj(row -> new TableRow(java.util.stream.IntStream.range(0, columns)
                        .mapToObj(column -> TableCell.empty())
                        .toList()))
                .toList(), 0);
    }

    public int columnCount() {
        return rows.get(0).cells().size();
    }
}
