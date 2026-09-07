package dev.rgcb.scholar.diagram.layout;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record LaidOutDiagramConnection(
        int connectionIndex,
        List<LaidOutDiagramPoint> path,
        LaidOutDiagramRect bounds,
        Optional<LaidOutDiagramLabel> label
) {
    public LaidOutDiagramConnection {
        if (connectionIndex < 0) {
            throw new IllegalArgumentException("connectionIndex must not be negative.");
        }
        path = List.copyOf(Objects.requireNonNull(path, "path"));
        if (path.size() < 2) {
            throw new IllegalArgumentException("A laid-out connection path needs at least two points.");
        }
        bounds = Objects.requireNonNull(bounds, "bounds");
        label = Objects.requireNonNull(label, "label");
    }
}
