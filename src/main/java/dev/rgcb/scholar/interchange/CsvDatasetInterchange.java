package dev.rgcb.scholar.interchange;

import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitExpression;
import dev.rgcb.scholar.quantity.UnitParseResult;
import dev.rgcb.scholar.quantity.UnitParser;
import dev.rgcb.scholar.quantity.UnitRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** CSV is a value interchange format; Scholar IDs are allocated on import. */
public final class CsvDatasetInterchange {
    private final UnitParser units = new UnitParser();
    private final UnitRegistry registry = UnitRegistry.builtIn();

    public record Preview(List<DatasetColumn> columns, List<DatasetRow> rows, List<String> warnings) {
        public Preview {
            columns = List.copyOf(columns);
            rows = List.copyOf(rows);
            warnings = List.copyOf(warnings);
        }

        public List<DatasetRow> sample(int limit) { return rows.subList(0, Math.min(Math.max(0, limit), rows.size())); }

        public ScientificDataset dataset(String id, String name) {
            return new ScientificDataset(id, name, columns, rows);
        }
    }

    public Preview preview(String csv) {
        var records = CsvRecords.parse(csv);
        if (records.isEmpty()) throw new IllegalArgumentException("CSV is empty");
        var header = records.getFirst();
        if (header.isEmpty() || header.stream().anyMatch(field -> field.value().isBlank())) {
            throw new IllegalArgumentException("CSV requires nonempty column headings");
        }
        for (var i = 1; i < records.size(); i++) {
            if (records.get(i).size() != header.size()) {
                throw new IllegalArgumentException("CSV row " + (i + 1) + " has " + records.get(i).size()
                        + " fields; expected " + header.size());
            }
        }
        var warnings = new ArrayList<String>();
        var columns = new ArrayList<DatasetColumn>();
        var usedIds = new HashSet<String>();
        for (var columnIndex = 0; columnIndex < header.size(); columnIndex++) {
            var raw = header.get(columnIndex).value().trim();
            var name = raw;
            Optional<UnitExpression> unit = Optional.empty();
            var semantics = QuantitySemantics.LINEAR;
            var bracket = raw.lastIndexOf(" [");
            if (bracket > 0 && raw.endsWith("]")) {
                var symbol = raw.substring(bracket + 2, raw.length() - 1);
                var difference = symbol.startsWith("Δ");
                var parsed = units.parse(difference ? symbol.substring(1) : symbol);
                if (parsed instanceof UnitParseResult.Success success) {
                    unit = Optional.of(success.expression());
                    name = raw.substring(0, bracket).trim();
                    semantics = difference ? QuantitySemantics.TEMPERATURE_DIFFERENCE
                            : QuantitySemantics.defaultFor(success.expression());
                    try { semantics.validate(success.expression()); }
                    catch (IllegalArgumentException invalid) {
                        unit = Optional.empty();
                        name = raw;
                        semantics = QuantitySemantics.LINEAR;
                        warnings.add("Unsupported unit semantics in header: " + raw);
                    }
                } else warnings.add("Unrecognized unit in header: " + raw);
            }
            var numeric = true;
            for (var rowIndex = 1; rowIndex < records.size(); rowIndex++) {
                var field = records.get(rowIndex).get(columnIndex);
                if (field.value().isEmpty() && field.quoted()) { numeric = false; break; }
                if (!field.value().isEmpty()) {
                    try { new BigDecimal(field.value()); }
                    catch (NumberFormatException invalid) { numeric = false; break; }
                }
            }
            if (!numeric && unit.isPresent()) {
                warnings.add("Unit ignored for text column: " + raw);
                name = raw;
                unit = Optional.empty();
                semantics = QuantitySemantics.LINEAR;
            }
            var baseId = "column-" + (columnIndex + 1);
            var id = baseId;
            var suffix = 2;
            while (!usedIds.add(id)) id = baseId + "-" + suffix++;
            columns.add(new DatasetColumn(id, name, numeric ? DatasetColumnType.NUMBER : DatasetColumnType.TEXT,
                    unit, semantics));
        }
        var rows = new ArrayList<DatasetRow>();
        for (var rowIndex = 1; rowIndex < records.size(); rowIndex++) {
            var values = new ArrayList<DatasetValue>();
            for (var columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                var field = records.get(rowIndex).get(columnIndex);
                var value = field.value();
                values.add(value.isEmpty() && !field.quoted() ? DatasetValue.missing()
                        : columns.get(columnIndex).type() == DatasetColumnType.NUMBER
                        ? DatasetValue.number(new BigDecimal(value)) : DatasetValue.text(value));
            }
            rows.add(new DatasetRow(values));
        }
        return new Preview(columns, rows, warnings);
    }

    public String export(ScientificDataset dataset) {
        Objects.requireNonNull(dataset);
        var records = new ArrayList<List<CsvRecords.Field>>();
        records.add(dataset.columns().stream().map(column -> {
            var name = column.displayName();
            if (column.unit().isEmpty()) return new CsvRecords.Field(name, false);
            var symbol = registry.displaySymbol(column.unit().orElseThrow(), true);
            return new CsvRecords.Field(name + " [" + column.quantitySemantics().valuePrefix(true) + symbol + "]", false);
        }).toList());
        for (var row : dataset.rows()) records.add(row.values().stream().map(value -> switch (value.kind()) {
            case NUMBER -> new CsvRecords.Field(value.number().orElseThrow().toString(), false);
            case TEXT -> new CsvRecords.Field(value.text().orElseThrow(), value.text().orElseThrow().isEmpty());
            case MISSING -> new CsvRecords.Field("", false);
        }).toList());
        return CsvRecords.writeDataset(records);
    }
}
