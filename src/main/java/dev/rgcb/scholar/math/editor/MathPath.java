package dev.rgcb.scholar.math.editor;

import java.util.List;
import java.util.Objects;

public record MathPath(List<MathPathSegment> segments) {
    public static final MathPath ROOT = new MathPath(List.of());

    public MathPath {
        segments = List.copyOf(Objects.requireNonNull(segments, "segments"));
    }

    public MathPath append(MathPathSegment segment) {
        Objects.requireNonNull(segment, "segment");
        var updated = new java.util.ArrayList<>(segments);
        updated.add(segment);
        return new MathPath(updated);
    }

    public MathPath parent() {
        if (segments.isEmpty()) {
            throw new IllegalStateException("Root path has no parent.");
        }
        return new MathPath(segments.subList(0, segments.size() - 1));
    }

    public MathPathSegment last() {
        if (segments.isEmpty()) {
            throw new IllegalStateException("Root path has no last segment.");
        }
        return segments.get(segments.size() - 1);
    }
}
