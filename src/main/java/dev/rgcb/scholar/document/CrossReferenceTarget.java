package dev.rgcb.scholar.document;

import java.util.Objects;

public record CrossReferenceTarget(
        CrossReferenceTargetKind kind,
        String targetId,
        int blockIndex,
        int number,
        String displayLabel,
        String description
) {
    public CrossReferenceTarget {
        kind = Objects.requireNonNull(kind, "kind");
        targetId = Objects.requireNonNull(targetId, "targetId");
        displayLabel = Objects.requireNonNull(displayLabel, "displayLabel");
        description = Objects.requireNonNull(description, "description");
        if (targetId.isBlank()) {
            throw new IllegalArgumentException("targetId must not be blank.");
        }
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must not be negative.");
        }
        if (number < 1) {
            throw new IllegalArgumentException("number must be positive.");
        }
    }

    public String pickerLabel() {
        return description.isBlank() ? displayLabel : displayLabel + " - " + description;
    }
}
