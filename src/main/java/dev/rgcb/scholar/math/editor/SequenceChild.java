package dev.rgcb.scholar.math.editor;

public record SequenceChild(int index) implements MathPathSegment {
    public SequenceChild {
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative.");
        }
    }
}
