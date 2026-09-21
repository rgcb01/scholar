package dev.rgcb.scholar.data;

import java.util.Objects;
import java.util.Optional;
import dev.rgcb.scholar.quantity.UnitExpression;

public record DatasetColumn(String id, String displayName, DatasetColumnType type, Optional<UnitExpression> unit) {
    public DatasetColumn(String id, String displayName, DatasetColumnType type) {
        this(id, displayName, type, Optional.empty());
    }

    public DatasetColumn {
        id = normalizeId(id, "column id");
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("column display name must not be blank.");
        }
        type = Objects.requireNonNull(type, "type");
        unit = Objects.requireNonNull(unit, "unit");
        if (type == DatasetColumnType.TEXT && unit.isPresent()) {
            throw new IllegalArgumentException("text dataset columns cannot carry physical units");
        }
    }

    public DatasetColumn withDisplayName(String replacement) {
        return new DatasetColumn(id, replacement, type, unit);
    }

    public DatasetColumn withUnit(UnitExpression replacement) {
        return new DatasetColumn(id, displayName, type, Optional.of(replacement));
    }

    public DatasetColumn withoutUnit() { return new DatasetColumn(id, displayName, type, Optional.empty()); }

    private static String normalizeId(String value, String name) {
        var normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return normalized;
    }
}
