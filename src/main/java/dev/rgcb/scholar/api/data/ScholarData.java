package dev.rgcb.scholar.api.data;

import dev.rgcb.scholar.api.quantity.ScholarQuantity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable public descriptions of authored scientific data. */
public final class ScholarData {
    private ScholarData() {}
    public enum ColumnType { NUMBER, TEXT }
    public enum Analysis { DESCRIPTIVE, LINEAR_REGRESSION, QUADRATIC_FIT, CUBIC_FIT }
    public enum CellKind { NUMBER, TEXT, MISSING }

    public record ColumnSpec(String name, ColumnType type, Optional<String> unit,
                             ScholarQuantity.Semantics semantics) {
        public ColumnSpec {
            Objects.requireNonNull(name); Objects.requireNonNull(type);
            unit = Objects.requireNonNull(unit); Objects.requireNonNull(semantics);
        }
        public static ColumnSpec number(String name, String unit) {
            return new ColumnSpec(name, ColumnType.NUMBER, Optional.of(unit), ScholarQuantity.Semantics.LINEAR);
        }
        public static ColumnSpec text(String name) {
            return new ColumnSpec(name, ColumnType.TEXT, Optional.empty(), ScholarQuantity.Semantics.LINEAR);
        }
    }

    public record Column(String id, String name, ColumnType type, Optional<String> unit,
                         ScholarQuantity.Semantics semantics) {}
    public record Cell(CellKind kind, Optional<BigDecimal> number, Optional<String> text) {
        public Cell {
            Objects.requireNonNull(kind); number = Objects.requireNonNull(number); text = Objects.requireNonNull(text);
            if (kind == CellKind.NUMBER && (number.isEmpty() || text.isPresent())
                    || kind == CellKind.TEXT && (text.isEmpty() || number.isPresent())
                    || kind == CellKind.MISSING && (number.isPresent() || text.isPresent())) {
                throw new IllegalArgumentException("Cell kind and value disagree");
            }
        }
        public static Cell number(BigDecimal value) { return new Cell(CellKind.NUMBER, Optional.of(value), Optional.empty()); }
        public static Cell text(String value) { return new Cell(CellKind.TEXT, Optional.empty(), Optional.of(value)); }
        public static Cell missing() { return new Cell(CellKind.MISSING, Optional.empty(), Optional.empty()); }
    }
    public record Dataset(String id, String name, List<Column> columns, List<List<Cell>> rows) {
        public Dataset { columns = List.copyOf(columns); rows = rows.stream().map(List::copyOf).toList(); }
    }
    public record Variable(String id, String name, ScholarQuantity value) {}
}
