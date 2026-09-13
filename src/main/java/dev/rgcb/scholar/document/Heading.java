package dev.rgcb.scholar.document;

import java.util.Objects;
import java.util.Optional;

/**
 * A section heading with a Markdown-compatible level from 1 through 6.
 */
public record Heading(int level, InlineContent content, Optional<String> id) implements BlockNode {
    public Heading(int level, InlineContent content) {
        this(level, content, Optional.empty());
    }

    public Heading(String id, int level, InlineContent content) {
        this(level, content, Optional.of(id));
    }

    public Heading {
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("Heading level must be between 1 and 6.");
        }
        content = Objects.requireNonNull(content, "content");
        id = normalizeId(id);
    }

    public Heading withContent(InlineContent replacement) {
        return new Heading(level, replacement, id);
    }

    public Heading withId(String replacement) {
        return new Heading(level, content, Optional.of(replacement));
    }

    private static Optional<String> normalizeId(Optional<String> id) {
        var normalized = Objects.requireNonNull(id, "id")
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        normalized.ifPresent(value -> {
            if (value.isBlank()) {
                throw new IllegalArgumentException("Heading id must not be blank.");
            }
        });
        return normalized;
    }
}
