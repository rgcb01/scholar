package dev.rgcb.scholar.figure.clipboard;

import dev.rgcb.scholar.clipboard.ScholarClipboardPayload;
import dev.rgcb.scholar.document.FigureBlock;
import java.util.Objects;

/** Lossless process-local clipboard payload for a whole Scholar figure. */
public record FigureClipboardPayload(FigureBlock figure) implements ScholarClipboardPayload {
    public FigureClipboardPayload {
        figure = Objects.requireNonNull(figure, "figure");
    }
}
