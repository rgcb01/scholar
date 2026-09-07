package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.PlotBlock;
import java.util.Objects;

public record PlotEditResult(PlotBlock plot, PlotEditTarget target, boolean changed) {
    public PlotEditResult {
        plot = Objects.requireNonNull(plot, "plot");
        target = Objects.requireNonNull(target, "target");
    }
}
