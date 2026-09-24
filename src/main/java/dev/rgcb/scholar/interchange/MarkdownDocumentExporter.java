package dev.rgcb.scholar.interchange;

import dev.rgcb.scholar.data.DatasetTableResolver;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.math.clipboard.MathPlainTextSerializer;
import dev.rgcb.scholar.quantity.ScientificNumberFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Human-readable export, intentionally independent of native Markdown interchange parsing. */
public final class MarkdownDocumentExporter {
    private final CrossReferenceResolver references = new CrossReferenceResolver();
    private final DocumentPlainTextSerializer plainText = new DocumentPlainTextSerializer();
    private final DatasetTableResolver datasets = new DatasetTableResolver();
    private final MathPlainTextSerializer math = new MathPlainTextSerializer();

    public String export(Document document) {
        Objects.requireNonNull(document);
        var sections = new ArrayList<String>();
        var figureNumber = 0;
        for (var i = 0; i < document.blocks().size(); i++) {
            var block = document.blocks().get(i);
            String result;
            if (block instanceof Heading heading) result = "#".repeat(heading.level()) + " " + inline(document, heading.content());
            else if (block instanceof Paragraph paragraph) result = inline(document, paragraph.content());
            else if (block instanceof EquationBlock equation) result = "$$\n" + math.serialize(equation.expression()).orElse("[Equation]") + "\n$$";
            else if (block instanceof TableBlock table) result = table(document, datasets.resolve(document, table));
            else if (block instanceof FigureBlock figure) {
                figureNumber++;
                result = visual(figure.content()) + "\n\n**Figure " + figureNumber + ".** " + inline(document, figure.caption());
            } else if (block instanceof PlotBlock || block instanceof DiagramBlock) result = visual(block);
            else if (block instanceof TableOfContentsBlock) result = plainText.serializeTableOfContents(document);
            else if (block instanceof LayoutSectionBreak || block instanceof PageBreak) result = "";
            else result = plainText.serializeBlock(document, i, block);
            if (!result.isBlank()) sections.add(result);
        }
        return String.join("\n\n", sections) + "\n";
    }

    private String visual(BlockNode block) {
        if (block instanceof PlotBlock plot) return "*[Plot: " + escape(plot.definition().title())
                + "; see Scholar or PDF for the visual.]*";
        if (block instanceof DiagramBlock diagram) return "*[Diagram: " + escape(diagram.definition().title())
                + "; see Scholar or PDF for the visual.]*";
        return "*[Scientific visual; see Scholar or PDF.]*";
    }

    private String table(Document document, TableBlock block) {
        var lines = new ArrayList<String>();
        var header = block.headerRowCount() == 1 ? block.rows().getFirst() : null;
        if (header != null) lines.add(row(document, header));
        else lines.add("| " + " | ".repeat(block.columnCount() - 1) + " |");
        lines.add("| " + "--- | ".repeat(block.columnCount()));
        for (var i = block.headerRowCount(); i < block.rows().size(); i++) lines.add(row(document, block.rows().get(i)));
        return String.join("\n", lines);
    }

    private String row(Document document, TableRow row) {
        return "| " + row.cells().stream().map(cell -> inline(document, cell.content().content())).reduce((a, b) -> a + " | " + b).orElse("") + " |";
    }

    private String inline(Document document, InlineContent content) {
        var text = new StringBuilder();
        for (var node : content.nodes()) {
            if (node instanceof Text run) {
                var value = escape(run.content());
                var marks = run.marks();
                text.append(marks.equals(Set.of(TextMark.BOLD, TextMark.ITALIC)) ? "***" + value + "***"
                        : marks.contains(TextMark.BOLD) ? "**" + value + "**"
                        : marks.contains(TextMark.ITALIC) ? "*" + value + "*" : value);
            } else if (node instanceof CrossReference reference) {
                text.append(escape(references.resolve(document, reference).displayText()));
            } else if (node instanceof QuantityInline quantity) {
                text.append(escape(new ScientificNumberFormatter().format(quantity.value(), quantity.notation(), true)));
            }
        }
        return text.toString();
    }

    private String escape(String value) { return value.replace("\\", "\\\\").replace("|", "\\|").replace("*", "\\*"); }
}
