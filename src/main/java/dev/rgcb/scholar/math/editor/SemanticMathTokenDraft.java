package dev.rgcb.scholar.math.editor;

import java.util.Objects;

public record SemanticMathTokenDraft(SemanticMathTokenKind kind, String content) {
    public SemanticMathTokenDraft {
        kind = Objects.requireNonNull(kind, "kind");
        content = Objects.requireNonNull(content, "content");
    }
}
