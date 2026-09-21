package dev.rgcb.scholar.markdown;

import dev.rgcb.scholar.data.DatasetTableResolver;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceResolver;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import java.util.Objects;
import java.util.Set;

/**
 * Canonical serializer for Scholar Markdown v0.1.
 */
public final class MarkdownSerializer {
    private final CrossReferenceResolver referenceResolver = new CrossReferenceResolver();
    private final DatasetTableResolver datasetTableResolver = new DatasetTableResolver();

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
                        .append(serializeInline(document, heading.content()));
            } else if (block instanceof Paragraph paragraph) {
                output.append(serializeInline(document, paragraph.content()));
            } else if (block instanceof TableBlock table) {
                output.append(serializeTable(document, datasetTableResolver.resolve(document, table)));
            } else if (block instanceof TableOfContentsBlock) {
                output.append("Contents");
            } else if (block instanceof PlotBlock) {
                throw new IllegalArgumentException("Markdown plot serialization is not supported.");
            } else if (block instanceof DiagramBlock) {
                throw new IllegalArgumentException("Markdown diagram serialization is not supported.");
            } else if (block instanceof FigureBlock) {
                throw new IllegalArgumentException("Markdown figure serialization is not supported.");
            } else if (block instanceof dev.rgcb.scholar.document.LayoutSectionBreak
                    || block instanceof dev.rgcb.scholar.document.PageBreak) {
                // Markdown has no Scholar page-flow semantics; omit structural layout markers.
            } else {
                throw new IllegalArgumentException("Unsupported block node: " + block.getClass().getName());
            }
        }

        output.append('\n');
        return output.toString();
    }

    private String serializeTable(Document document, TableBlock table) {
        if (table.headerRowCount() != 1) {
            throw new IllegalArgumentException("Markdown table serialization requires exactly one semantic header row.");
        }

        var output = new StringBuilder();
        output.append(serializeTableRow(document, table.rows().get(0)));
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
            output.append(serializeTableRow(document, table.rows().get(rowIndex)));
        }
        return output.toString();
    }

    private String serializeTableRow(Document document, dev.rgcb.scholar.document.TableRow row) {
        var output = new StringBuilder("| ");
        for (var columnIndex = 0; columnIndex < row.cells().size(); columnIndex++) {
            if (columnIndex > 0) {
                output.append(" | ");
            }
            output.append(serializeTableCell(document, row.cells().get(columnIndex)));
        }
        output.append(" |");
        return output.toString();
    }

    private String serializeTableCell(Document document, TableCell cell) {
        return serializeInline(document, cell.content().content());
    }

    private String serializeInline(Document document, InlineContent content) {
        var output = new StringBuilder();
        for (var node : content.nodes()) {
            output.append(serializeInlineNode(document, node));
        }
        return output.toString();
    }

    private String serializeInlineNode(Document document, InlineNode node) {
        if (node instanceof Text text) {
            return serializeText(text);
        }
        if (node instanceof CrossReference reference) {
            return escape(referenceResolver.resolve(document, reference).displayText());
        }
        if (node instanceof dev.rgcb.scholar.document.QuantityInline quantity) {
            return escape(new dev.rgcb.scholar.quantity.ScientificNumberFormatter()
                    .format(quantity.value(), quantity.notation(), false));
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
