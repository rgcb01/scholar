package dev.rgcb.scholar.data;

import java.util.Objects;
import java.util.Optional;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitExpression;

public record DatasetColumn(String id, String displayName, DatasetColumnType type, Optional<UnitExpression> unit,
                            QuantitySemantics quantitySemantics) {
    public DatasetColumn(String id, String displayName, DatasetColumnType type) {
        this(id, displayName, type, Optional.empty(), QuantitySemantics.LINEAR);
    }

    public DatasetColumn(String id, String displayName, DatasetColumnType type, Optional<UnitExpression> unit) {
        this(id, displayName, type, unit, unit.map(QuantitySemantics::defaultFor).orElse(QuantitySemantics.LINEAR));
    }

    public DatasetColumn {
        id = normalizeId(id, "column id");
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("column display name must not be blank.");
        }
        type = Objects.requireNonNull(type, "type");
        unit = Objects.requireNonNull(unit, "unit");
        quantitySemantics = Objects.requireNonNull(quantitySemantics, "quantitySemantics");
        if (type == DatasetColumnType.TEXT && unit.isPresent()) {
            throw new IllegalArgumentException("text dataset columns cannot carry physical units");
        }
        if (unit.isPresent()) quantitySemantics.validate(unit.orElseThrow());
        else if (quantitySemantics != QuantitySemantics.LINEAR) {
            throw new IllegalArgumentException("unitless dataset columns require linear quantity semantics");
        }
    }

    public DatasetColumn withDisplayName(String replacement) {
        return new DatasetColumn(id, replacement, type, unit, quantitySemantics);
    }

    public DatasetColumn withUnit(UnitExpression replacement) {
        return withUnit(replacement, QuantitySemantics.defaultFor(replacement));
    }

    public DatasetColumn withUnit(UnitExpression replacement, QuantitySemantics semantics) {
        return new DatasetColumn(id, displayName, type, Optional.of(replacement), semantics);
    }

    public DatasetColumn withoutUnit() { return new DatasetColumn(id, displayName, type, Optional.empty(), QuantitySemantics.LINEAR); }

    private static String normalizeId(String value, String name) {
        var normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return normalized;
    }
}
