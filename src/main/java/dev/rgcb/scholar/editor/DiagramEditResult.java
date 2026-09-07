package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.DiagramBlock;
import java.util.Objects;

/** Result of one immutable semantic DiagramBlock edit. */
public record DiagramEditResult(DiagramBlock diagram, DiagramEditTarget target, boolean changed) {
    public DiagramEditResult {
        diagram = Objects.requireNonNull(diagram, "diagram");
        target = Objects.requireNonNull(target, "target");
    }
}
