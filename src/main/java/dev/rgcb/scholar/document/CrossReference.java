package dev.rgcb.scholar.document;

import java.util.Objects;

/**
 * Semantic inline reference to a stable document target.
 */
public record CrossReference(CrossReferenceTargetKind kind, String targetId) implements InlineNode {
    public CrossReference {
        kind = Objects.requireNonNull(kind, "kind");
        targetId = Objects.requireNonNull(targetId, "targetId").trim();
        if (targetId.isEmpty()) {
            throw new IllegalArgumentException("Cross-reference target id must not be blank.");
        }
    }
}
