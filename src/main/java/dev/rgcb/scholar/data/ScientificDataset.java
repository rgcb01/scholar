package dev.rgcb.scholar.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ScientificDataset(String id, Optional<String> displayName, List<DatasetColumn> columns, List<DatasetRow> rows) {
    public ScientificDataset(String id, String displayName, List<DatasetColumn> columns, List<DatasetRow> rows) {
        this(id, Optional.of(displayName), columns, rows);
    }

    public ScientificDataset {
        id = normalizeId(id, "dataset id");
        displayName = Objects.requireNonNull(displayName, "displayName")
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        columns = List.copyOf(Objects.requireNonNull(columns, "columns"));
        rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
        var columnIds = new HashSet<String>();
        for (var column : columns) {
            if (!columnIds.add(column.id())) {
                throw new IllegalArgumentException("dataset column IDs must be unique.");
            }
        }
        for (var row : rows) {
            if (row.values().size() != columns.size()) {
                throw new IllegalArgumentException("dataset row value count must match column count.");
            }
        }
    }

    public String displayLabel() {
        return displayName.orElse(id);
    }

    public Optional<DatasetColumn> column(String columnId) {
        Objects.requireNonNull(columnId, "columnId");
        return columns.stream().filter(column -> column.id().equals(columnId)).findFirst();
    }

    public int columnIndex(String columnId) {
        for (var index = 0; index < columns.size(); index++) {
            if (columns.get(index).id().equals(columnId)) {
                return index;
            }
        }
        return -1;
    }

    public ScientificDataset withDisplayName(String replacement) {
        return new ScientificDataset(id, Optional.of(replacement), columns, rows);
    }

    public ScientificDataset withColumnDisplayName(String columnId, String replacement) {
        var index = columnIndex(columnId);
        if (index < 0) {
            return this;
        }
        var updated = new ArrayList<>(columns);
        updated.set(index, columns.get(index).withDisplayName(replacement));
        return new ScientificDataset(id, displayName, updated, rows);
    }

    public ScientificDataset withCell(int rowIndex, String columnId, DatasetValue value) {
        Objects.requireNonNull(value, "value");
        var columnIndex = columnIndex(columnId);
        if (rowIndex < 0 || rowIndex >= rows.size() || columnIndex < 0) {
            return this;
        }
        var rowValues = new ArrayList<>(rows.get(rowIndex).values());
        rowValues.set(columnIndex, value);
        var updatedRows = new ArrayList<>(rows);
        updatedRows.set(rowIndex, rows.get(rowIndex).withValues(rowValues));
        return new ScientificDataset(id, displayName, columns, updatedRows);
    }

    public ScientificDataset withAddedRow(DatasetRow row) {
        var updated = new ArrayList<>(rows);
        updated.add(Objects.requireNonNull(row, "row"));
        return new ScientificDataset(id, displayName, columns, updated);
    }

    public ScientificDataset withoutRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return this;
        }
        var updated = new ArrayList<>(rows);
        updated.remove(rowIndex);
        return new ScientificDataset(id, displayName, columns, updated);
    }

    public ScientificDataset withAddedColumn(DatasetColumn column, DatasetValue defaultValue) {
        Objects.requireNonNull(column, "column");
        Objects.requireNonNull(defaultValue, "defaultValue");
        var updatedColumns = new ArrayList<>(columns);
        updatedColumns.add(column);
        var updatedRows = rows.stream()
                .map(row -> {
                    var values = new ArrayList<>(row.values());
                    values.add(defaultValue);
                    return row.withValues(values);
                })
                .toList();
        return new ScientificDataset(id, displayName, updatedColumns, updatedRows);
    }

    public ScientificDataset withoutColumn(String columnId) {
        var columnIndex = columnIndex(columnId);
        if (columnIndex < 0 || columns.size() == 1) {
            return this;
        }
        var updatedColumns = new ArrayList<>(columns);
        updatedColumns.remove(columnIndex);
        var updatedRows = rows.stream()
                .map(row -> {
                    var values = new ArrayList<>(row.values());
                    values.remove(columnIndex);
                    return row.withValues(values);
                })
                .toList();
        return new ScientificDataset(id, displayName, updatedColumns, updatedRows);
    }

    public ScientificDataset withId(String replacement) {
        return new ScientificDataset(replacement, displayName, columns, rows);
    }

    private static String normalizeId(String value, String name) {
        var normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return normalized;
    }
}
