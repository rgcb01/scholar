package dev.rgcb.scholar.document;

import java.util.Objects;

public record CrossReferenceResolution(
        CrossReference reference,
        boolean resolved,
        String displayText,
        CrossReferenceTarget target
) {
    public static final String MISSING_REFERENCE_TEXT = "[Missing reference]";

    public CrossReferenceResolution {
        reference = Objects.requireNonNull(reference, "reference");
        displayText = Objects.requireNonNull(displayText, "displayText");
        if (resolved && target == null) {
            throw new IllegalArgumentException("resolved reference requires a target.");
        }
    }

    public static CrossReferenceResolution resolved(CrossReference reference, CrossReferenceTarget target) {
        Objects.requireNonNull(target, "target");
        return new CrossReferenceResolution(reference, true, target.displayLabel(), target);
    }

    public static CrossReferenceResolution missing(CrossReference reference) {
        return new CrossReferenceResolution(reference, false, MISSING_REFERENCE_TEXT, null);
    }
}
