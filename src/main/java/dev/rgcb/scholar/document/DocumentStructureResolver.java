package dev.rgcb.scholar.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class DocumentStructureResolver {
    public DocumentStructure resolve(Document document) {
        Objects.requireNonNull(document, "document");
        var entries = new ArrayList<SectionEntry>();
        var counters = new int[6];
        for (var blockIndex = 0; blockIndex < document.blocks().size(); blockIndex++) {
            var block = document.blocks().get(blockIndex);
            if (!(block instanceof Heading heading)) {
                continue;
            }
            var level = heading.level();
            counters[level - 1]++;
            for (var index = level; index < counters.length; index++) {
                counters[index] = 0;
            }
            var parts = Arrays.stream(counters, 0, level)
                    .boxed()
                    .toList();
            entries.add(new SectionEntry(
                    blockIndex,
                    level,
                    heading.id(),
                    new SectionNumber(parts),
                    inlinePreview(heading.content())));
        }
        return new DocumentStructure(entries);
    }

    private static String inlinePreview(InlineContent content) {
        var text = new StringBuilder();
        for (var node : content.nodes()) {
            if (node instanceof Text run) {
                text.append(run.content());
            } else if (node instanceof CrossReference) {
                text.append("[Reference]");
            } else if (node instanceof QuantityInline quantity) {
                text.append(new dev.rgcb.scholar.quantity.ScientificNumberFormatter()
                        .format(quantity.value(), quantity.notation(), true));
            }
        }
        return text.toString();
    }
}
