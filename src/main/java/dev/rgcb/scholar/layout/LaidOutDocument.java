package dev.rgcb.scholar.layout;

import java.util.List;

public record LaidOutDocument(int width, int height, List<LaidOutBlock> blocks) {
    public LaidOutDocument {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive.");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height cannot be negative.");
        }
        blocks = List.copyOf(blocks);
    }
}
