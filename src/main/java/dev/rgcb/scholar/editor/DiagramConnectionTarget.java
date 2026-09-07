package dev.rgcb.scholar.editor;

public record DiagramConnectionTarget(int connectionIndex) implements DiagramEditTarget {
    public DiagramConnectionTarget {
        if (connectionIndex < 0) {
            throw new IllegalArgumentException("connectionIndex must not be negative.");
        }
    }
}
