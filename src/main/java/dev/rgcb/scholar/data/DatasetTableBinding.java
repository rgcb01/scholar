package dev.rgcb.scholar.data;

import java.util.List;
import java.util.Objects;

public record DatasetTableBinding(String datasetId, List<String> columnIds) {
    public DatasetTableBinding(String datasetId) {
        this(datasetId, List.of());
    }

    public DatasetTableBinding {
        datasetId = normalizeId(datasetId, "datasetId");
        columnIds = List.copyOf(Objects.requireNonNull(columnIds, "columnIds").stream()
                .map(id -> normalizeId(id, "columnId"))
                .toList());
    }

    public boolean usesAllColumns() {
        return columnIds.isEmpty();
    }

    private static String normalizeId(String value, String name) {
        var normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return normalized;
    }
}
