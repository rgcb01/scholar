package dev.rgcb.scholar.typography;

import dev.rgcb.scholar.document.ParagraphFormat;
import dev.rgcb.scholar.document.TextFormat;
import dev.rgcb.scholar.document.TextMark;
import java.util.Set;

public record SemanticStyleDefinition(TextFormat text, Set<TextMark> marks, ParagraphFormat paragraph) {
    public SemanticStyleDefinition {
        marks = Set.copyOf(marks);
    }
}
