package dev.rgcb.scholar.data;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import dev.rgcb.scholar.quantity.UnitRegistry;

public final class DatasetTableResolver {
    private final DatasetRegistry registry = new DatasetRegistry();

    public TableBlock resolve(Document document, TableBlock table) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(table, "table");
        if (table.datasetBinding().isEmpty()) {
            return table;
        }
        var binding = table.datasetBinding().orElseThrow();
        var dataset = registry.find(document, binding.datasetId());
        if (dataset.isEmpty()) {
            return message("[Missing dataset: " + binding.datasetId() + "]");
        }
        return tableFor(dataset.orElseThrow(), binding);
    }

    public TableBlock tableFor(ScientificDataset dataset, DatasetTableBinding binding) {
        Objects.requireNonNull(dataset, "dataset");
        Objects.requireNonNull(binding, "binding");
        var columns = selectedColumns(dataset, binding);
        if (columns.isEmpty()) {
            return message("[Missing column]");
        }
        var rows = new ArrayList<TableRow>();
        rows.add(new TableRow(columns.stream()
                .map(column -> cell(column.displayName() + column.unit()
                        .map(unit -> " (" + unit.displaySymbol(UnitRegistry.builtIn()) + ")").orElse("")))
                .toList()));
        for (var row : dataset.rows()) {
            var cells = new ArrayList<TableCell>();
            for (var column : columns) {
                var index = dataset.columnIndex(column.id());
                cells.add(cell(row.values().get(index).displayText()));
            }
            rows.add(new TableRow(cells));
        }
        return new TableBlock(rows, 1);
    }

    private static List<DatasetColumn> selectedColumns(ScientificDataset dataset, DatasetTableBinding binding) {
        if (binding.usesAllColumns()) {
            return dataset.columns();
        }
        var selected = new ArrayList<DatasetColumn>();
        for (var columnId : binding.columnIds()) {
            var column = dataset.column(columnId);
            if (column.isEmpty()) {
                return List.of();
            }
            selected.add(column.orElseThrow());
        }
        return List.copyOf(selected);
    }

    private static TableBlock message(String text) {
        return new TableBlock(List.of(new TableRow(List.of(cell(text)))), 1);
    }

    private static TableCell cell(String text) {
        return new TableCell(new TableCellContent(new InlineContent(List.of((InlineNode) new Text(text, Set.of())))));
    }
}
