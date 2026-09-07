package dev.rgcb.scholar.document;

import java.util.Objects;

/**
 * A paragraph containing ordered inline content.
 */
public record Paragraph(InlineContent content) implements BlockNode {
    public Paragraph {
        content = Objects.requireNonNull(content, "content");
    }
}
