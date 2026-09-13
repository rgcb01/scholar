package dev.rgcb.scholar.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class DatasetTabularImporter {
    public ScientificDataset importText(String id, String displayName, String text) {
        Objects.requireNonNull(text, "text");
        var lines = text.strip().lines()
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("tabular data must contain at least one row.");
        }
        var delimiter = lines.get(0).contains("\t") ? "\t" : ",";
        var headers = split(lines.get(0), delimiter);
        var columns = new ArrayList<DatasetColumn>();
        for (var index = 0; index < headers.size(); index++) {
            var header = headers.get(index).isBlank() ? "Column " + (index + 1) : headers.get(index).trim();
            columns.add(new DatasetColumn(slug(header, "column-" + (index + 1)), header, DatasetColumnType.TEXT));
        }
        var rows = new ArrayList<DatasetRow>();
        for (var lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
            var values = split(lines.get(lineIndex), delimiter);
            var rowValues = new ArrayList<DatasetValue>();
            for (var columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                rowValues.add(parseValue(columnIndex < values.size() ? values.get(columnIndex) : ""));
            }
            rows.add(new DatasetRow(rowValues));
        }
        var inferredColumns = new ArrayList<DatasetColumn>();
        for (var columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            var currentColumnIndex = columnIndex;
            var numeric = !rows.isEmpty() && rows.stream()
                    .map(row -> row.values().get(currentColumnIndex))
                    .filter(value -> value.kind() != DatasetValueKind.MISSING)
                    .allMatch(value -> value.kind() == DatasetValueKind.NUMBER);
            var column = columns.get(columnIndex);
            inferredColumns.add(new DatasetColumn(column.id(), column.displayName(), numeric ? DatasetColumnType.NUMBER : DatasetColumnType.TEXT));
        }
        return new ScientificDataset(id, displayName, inferredColumns, rows);
    }

    private static DatasetValue parseValue(String raw) {
        var value = raw.trim();
        if (value.isEmpty()) {
            return DatasetValue.missing();
        }
        try {
            return DatasetValue.number(value);
        } catch (NumberFormatException ignored) {
            return DatasetValue.text(value);
        }
    }

    private static List<String> split(String line, String delimiter) {
        return List.of(line.split(java.util.regex.Pattern.quote(delimiter), -1));
    }

    private static String slug(String value, String fallback) {
        var slug = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? fallback : slug;
    }
}
