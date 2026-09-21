package dev.rgcb.scholar.document;

import java.util.Objects;

/**
 * A paragraph containing ordered inline content.
 */
public record Paragraph(InlineContent content, SemanticStyle style, ParagraphFormat format) implements BlockNode {
    public Paragraph(InlineContent content) { this(content, SemanticStyle.BODY_TEXT, ParagraphFormat.none()); }
    public Paragraph {
        content = Objects.requireNonNull(content, "content");
        style = Objects.requireNonNull(style, "style");
        format = Objects.requireNonNull(format, "format");
    }
    public Paragraph withContent(InlineContent replacement) { return new Paragraph(replacement, style, format); }
}
