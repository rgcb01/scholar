package dev.rgcb.scholar.data;

import java.util.Objects;

public record DatasetColumn(String id, String displayName, DatasetColumnType type) {
    public DatasetColumn {
        id = normalizeId(id, "column id");
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("column display name must not be blank.");
        }
        type = Objects.requireNonNull(type, "type");
    }

    public DatasetColumn withDisplayName(String replacement) {
        return new DatasetColumn(id, replacement, type);
    }

    private static String normalizeId(String value, String name) {
        var normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return normalized;
    }
}
