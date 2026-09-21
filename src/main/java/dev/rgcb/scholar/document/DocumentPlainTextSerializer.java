package dev.rgcb.scholar.document;

import dev.rgcb.scholar.data.DatasetTableResolver;
import java.util.Objects;

public final class DocumentPlainTextSerializer {
    private final CrossReferenceResolver referenceResolver = new CrossReferenceResolver();
    private final DocumentStructureResolver structureResolver = new DocumentStructureResolver();
    private final DatasetTableResolver datasetTableResolver = new DatasetTableResolver();

    public String serialize(Document document) {
        Objects.requireNonNull(document, "document");
        var output = new StringBuilder();
        for (var index = 0; index < document.blocks().size(); index++) {
            if (index > 0) {
                output.append("\n\n");
            }
            output.append(serializeBlock(document, index, document.blocks().get(index)));
        }
        return output.toString();
    }

    public String serializeBlock(Document document, int blockIndex, BlockNode block) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(block, "block");
        if (block instanceof Heading heading) {
            var section = structureResolver.resolve(document).sectionAtBlock(blockIndex).orElseThrow();
            return section.displayLabel() + " " + referenceResolver.inlineText(document, heading.content());
        }
        if (block instanceof Paragraph paragraph) {
            return referenceResolver.inlineText(document, paragraph.content());
        }
        if (block instanceof TableOfContentsBlock) {
            return serializeTableOfContents(document);
        }
        if (block instanceof TableBlock table) {
            return serializeTable(document, datasetTableResolver.resolve(document, table));
        }
        if (block instanceof LayoutSectionBreak || block instanceof PageBreak) {
            return "";
        }
        return "";
    }

    private String serializeTable(Document document, TableBlock table) {
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
                output.append(referenceResolver.inlineText(document, row.cells().get(columnIndex).content().content()));
            }
        }
        return output.toString();
    }

    public String serializeTableOfContents(Document document) {
        Objects.requireNonNull(document, "document");
        var output = new StringBuilder("Contents");
        for (var section : structureResolver.resolve(document).sections()) {
            output.append('\n')
                    .append("  ".repeat(Math.max(0, section.level() - 1)))
                    .append(section.displayText());
        }
        return output.toString();
    }
}
