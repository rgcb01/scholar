package dev.rgcb.scholar.diagram.layout;

/** Integer document-space rectangle used by diagram layout and hit testing. */
public record LaidOutDiagramRect(int x, int y, int width, int height) {
    public LaidOutDiagramRect {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Laid-out diagram rectangle dimensions must be positive.");
        }
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(int px, int py) {
        return px >= x && px <= right() && py >= y && py <= bottom();
    }

    public boolean contains(LaidOutDiagramPoint point) {
        return contains(point.x(), point.y());
    }

    public LaidOutDiagramRect expanded(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Expansion amount must not be negative.");
        }
        return new LaidOutDiagramRect(x - amount, y - amount, width + amount * 2, height + amount * 2);
    }
}
