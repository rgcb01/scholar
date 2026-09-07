package dev.rgcb.scholar.markdown;

import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import java.util.Objects;
import java.util.Set;

/**
 * Canonical serializer for Scholar Markdown v0.1.
 */
public final class MarkdownSerializer {
    public String serialize(Document document) {
        Objects.requireNonNull(document, "document");

        var output = new StringBuilder();
        for (var index = 0; index < document.blocks().size(); index++) {
            if (index > 0) {
                output.append("\n\n");
            }

            var block = document.blocks().get(index);
            if (block instanceof Heading heading) {
                output.append("#".repeat(heading.level()))
                        .append(' ')
                        .append(serializeInline(heading.content()));
            } else if (block instanceof Paragraph paragraph) {
                output.append(serializeInline(paragraph.content()));
            } else if (block instanceof TableBlock table) {
                output.append(serializeTable(table));
            } else if (block instanceof PlotBlock) {
                throw new IllegalArgumentException("Markdown plot serialization is not supported.");
            } else if (block instanceof DiagramBlock) {
                throw new IllegalArgumentException("Markdown diagram serialization is not supported.");
            } else {
                throw new IllegalArgumentException("Unsupported block node: " + block.getClass().getName());
            }
        }

        output.append('\n');
        return output.toString();
    }

    private static String serializeTable(TableBlock table) {
        if (table.headerRowCount() != 1) {
            throw new IllegalArgumentException("Markdown table serialization requires exactly one semantic header row.");
        }

        var output = new StringBuilder();
        output.append(serializeTableRow(table.rows().get(0)));
        output.append('\n');
        output.append("| ");
        for (var columnIndex = 0; columnIndex < table.columnCount(); columnIndex++) {
            if (columnIndex > 0) {
                output.append(" | ");
            }
            output.append("---");
        }
        output.append(" |");
        for (var rowIndex = 1; rowIndex < table.rows().size(); rowIndex++) {
            output.append('\n');
            output.append(serializeTableRow(table.rows().get(rowIndex)));
        }
        return output.toString();
    }

    private static String serializeTableRow(dev.rgcb.scholar.document.TableRow row) {
        var output = new StringBuilder("| ");
        for (var columnIndex = 0; columnIndex < row.cells().size(); columnIndex++) {
            if (columnIndex > 0) {
                output.append(" | ");
            }
            output.append(serializeTableCell(row.cells().get(columnIndex)));
        }
        output.append(" |");
        return output.toString();
    }

    private static String serializeTableCell(TableCell cell) {
        return serializeInline(cell.content().content());
    }

    private static String serializeInline(InlineContent content) {
        var output = new StringBuilder();
        for (var node : content.nodes()) {
            output.append(serializeInlineNode(node));
        }
        return output.toString();
    }

    private static String serializeInlineNode(InlineNode node) {
        if (node instanceof Text text) {
            return serializeText(text);
        }
        throw new IllegalArgumentException("Unsupported inline node: " + node.getClass().getName());
    }

    private static String serializeText(Text text) {
        var escaped = escape(text.content());
        var marks = text.marks();
        if (marks.equals(Set.of(TextMark.BOLD, TextMark.ITALIC))) {
            return "***" + escaped + "***";
        }
        if (marks.equals(Set.of(TextMark.BOLD))) {
            return "**" + escaped + "**";
        }
        if (marks.equals(Set.of(TextMark.ITALIC))) {
            return "*" + escaped + "*";
        }
        if (marks.isEmpty()) {
            return escaped;
        }
        throw new IllegalArgumentException("Unsupported text marks: " + marks);
    }

    private static String escape(String source) {
        return source.replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("#", "\\#")
                .replace("|", "\\|");
    }
}
