package dev.rgcb.scholar.table.clipboard;

import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.Text;
import java.util.Objects;

public final class TableTsvSerializer {
    public String serialize(TableBlock table) {
        Objects.requireNonNull(table, "table");
        var output = new StringBuilder();
        for (var rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
            if (rowIndex > 0) {
                output.append('\n');
            }
            var row = table.rows().get(rowIndex);
            for (var columnIndex = 0; columnIndex < row.cells().size(); columnIndex++) {
                if (columnIndex > 0) {
                    output.append('\t');
                }
                output.append(cellText(row.cells().get(columnIndex)));
            }
        }
        return output.toString();
    }

    private static String cellText(TableCell cell) {
        var output = new StringBuilder();
        for (InlineNode node : cell.content().content().nodes()) {
            if (node instanceof Text text) {
                output.append(text.content());
            } else {
                throw new IllegalArgumentException("Unsupported inline node in table cell: " + node.getClass().getName());
            }
        }
        var result = output.toString();
        if (result.indexOf('\t') >= 0 || result.indexOf('\n') >= 0 || result.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("TSV v0.1 table cells must not contain tab or newline characters.");
        }
        return result;
    }
}
