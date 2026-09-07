package dev.rgcb.scholar.diagram.clipboard;

import dev.rgcb.scholar.clipboard.ScholarClipboardPayload;
import dev.rgcb.scholar.document.DiagramBlock;
import java.util.Objects;

/** Lossless process-local clipboard payload for a whole Scholar diagram block. */
public record DiagramClipboardPayload(DiagramBlock diagram) implements ScholarClipboardPayload {
    public DiagramClipboardPayload {
        diagram = Objects.requireNonNull(diagram, "diagram");
    }
}
