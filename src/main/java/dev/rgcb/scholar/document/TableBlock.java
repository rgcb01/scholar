package dev.rgcb.scholar.document;

import dev.rgcb.scholar.data.DatasetTableBinding;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TableBlock(List<TableRow> rows, int headerRowCount, Optional<String> id, Optional<DatasetTableBinding> datasetBinding) implements BlockNode {
    public TableBlock(List<TableRow> rows, int headerRowCount) {
        this(rows, headerRowCount, Optional.empty(), Optional.empty());
    }

    public TableBlock(String id, List<TableRow> rows, int headerRowCount) {
        this(rows, headerRowCount, Optional.of(id), Optional.empty());
    }

    public TableBlock(DatasetTableBinding datasetBinding) {
        this(List.of(new TableRow(List.of(TableCell.empty()))), 1, Optional.empty(), Optional.of(datasetBinding));
    }

    public TableBlock {
        rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
        id = normalizeId(id);
        datasetBinding = Objects.requireNonNull(datasetBinding, "datasetBinding");
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

    public TableBlock withRows(List<TableRow> replacement) {
        return new TableBlock(replacement, headerRowCount, id, datasetBinding);
    }

    public TableBlock withId(String replacement) {
        return new TableBlock(rows, headerRowCount, Optional.of(replacement), datasetBinding);
    }

    public TableBlock withDatasetBinding(DatasetTableBinding binding) {
        return new TableBlock(rows, headerRowCount, id, Optional.of(binding));
    }

    private static Optional<String> normalizeId(Optional<String> id) {
        return Objects.requireNonNull(id, "id")
                .map(String::trim)
                .filter(value -> !value.isEmpty());
    }
}
