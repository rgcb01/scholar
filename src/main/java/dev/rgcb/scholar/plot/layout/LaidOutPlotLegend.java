package dev.rgcb.scholar.plot.layout;

import java.util.List;
import java.util.Objects;

/** Optional in-plot legend geometry. */
public record LaidOutPlotLegend(
        int x,
        int y,
        int width,
        int height,
        List<LaidOutPlotLegendItem> items
) {
    public LaidOutPlotLegend {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Legend dimensions must be positive.");
        }
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Legend must contain at least one item.");
        }
    }
}
