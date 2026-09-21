package dev.rgcb.scholar.application;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import java.util.Objects;

/** Disposable, lightweight Home-card data derived from semantic content. */
public record DocumentPreview(String title, String excerpt, int blockCount) {
    private static final int MAX_TEXT = 120;

    public DocumentPreview {
        title = Objects.requireNonNull(title, "title");
        excerpt = Objects.requireNonNull(excerpt, "excerpt");
        if (blockCount < 0) throw new IllegalArgumentException("blockCount must not be negative.");
    }

    public static DocumentPreview from(Document document, String fallbackTitle) {
        Objects.requireNonNull(document, "document");
        var title = document.blocks().stream()
                .filter(Heading.class::isInstance).map(Heading.class::cast)
                .map(heading -> inlineText(heading.content())).filter(text -> !text.isBlank())
                .findFirst().orElse(fallbackTitle);
        var excerpt = document.blocks().stream()
                .filter(Paragraph.class::isInstance).map(Paragraph.class::cast)
                .map(paragraph -> inlineText(paragraph.content())).filter(text -> !text.isBlank())
                .findFirst().orElse(document.blocks().isEmpty() ? "Empty document" : document.blocks().size() + " blocks");
        return new DocumentPreview(limit(title), limit(excerpt), document.blocks().size());
    }

    private static String inlineText(InlineContent content) {
        var result = new StringBuilder();
        for (var node : content.nodes()) {
            if (node instanceof Text text) result.append(text.content());
            else if (node instanceof dev.rgcb.scholar.document.QuantityInline quantity) result.append(
                    new dev.rgcb.scholar.quantity.ScientificNumberFormatter()
                            .format(quantity.value(), quantity.notation(), true));
        }
        return result.toString().strip();
    }

    private static String limit(String value) {
        var normalized = Objects.requireNonNull(value).replaceAll("\\s+", " ").strip();
        return normalized.length() <= MAX_TEXT ? normalized : normalized.substring(0, MAX_TEXT - 3) + "...";
    }
}
