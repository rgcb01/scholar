package dev.rgcb.scholar.document;

import java.util.Objects;

/**
 * A section heading with a Markdown-compatible level from 1 through 6.
 */
public record Heading(int level, InlineContent content) implements BlockNode {
    public Heading {
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("Heading level must be between 1 and 6.");
        }
        content = Objects.requireNonNull(content, "content");
    }
}
