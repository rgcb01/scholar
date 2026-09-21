package dev.rgcb.scholar.layout;

import java.util.List;

public record LaidOutDocument(int width, int height, List<LaidOutBlock> blocks, List<LaidOutPage> pages) {
    public LaidOutDocument(int width, int height, List<LaidOutBlock> blocks) {
        this(width, height, blocks, List.of());
    }

    public LaidOutDocument {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive.");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height cannot be negative.");
        }
        blocks = List.copyOf(blocks);
        pages = List.copyOf(pages);
    }

    public boolean paginated() {
        return !pages.isEmpty();
    }
}
