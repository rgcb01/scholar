package dev.rgcb.scholar.layout;

import java.util.Objects;

public record LaidOutTableOfContentsEntry(
        String targetId,
        int targetBlockIndex,
        int x,
        int y,
        int width,
        int height
) {
    public LaidOutTableOfContentsEntry {
        targetId = Objects.requireNonNull(targetId, "targetId");
        if (targetBlockIndex < 0) {
            throw new IllegalArgumentException("targetBlockIndex must not be negative.");
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("entry dimensions must not be negative.");
        }
    }

    public boolean contains(int localX, int localY) {
        return localX >= x && localX <= x + Math.max(1, width)
                && localY >= y && localY <= y + Math.max(1, height);
    }
}
