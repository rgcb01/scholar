package dev.rgcb.scholar.document;

import java.util.Objects;
import java.util.Set;

/**
 * A run of text with semantic inline marks.
 */
public record Text(String content, Set<TextMark> marks) implements InlineNode {
    public Text {
        content = Objects.requireNonNull(content, "content");
        marks = Set.copyOf(marks);
    }
}
