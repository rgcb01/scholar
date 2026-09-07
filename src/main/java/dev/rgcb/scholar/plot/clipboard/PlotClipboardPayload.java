package dev.rgcb.scholar.plot.clipboard;

import dev.rgcb.scholar.clipboard.ScholarClipboardPayload;
import dev.rgcb.scholar.document.PlotBlock;
import java.util.Objects;

public record PlotClipboardPayload(PlotBlock plot) implements ScholarClipboardPayload {
    public PlotClipboardPayload {
        plot = Objects.requireNonNull(plot, "plot");
    }
}
