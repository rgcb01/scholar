package dev.rgcb.scholar.plot.layout;

import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.Objects;

/** Positioned legend sample and label for a laid-out plot series. */
public record LaidOutPlotLegendItem(
        int seriesIndex,
        PlotSeriesKind kind,
        LaidOutPlotSeriesStyle style,
        int sampleX1,
        int sampleX2,
        int sampleY,
        LaidOutPlotLabel label
) {
    public LaidOutPlotLegendItem {
        if (seriesIndex < 0) {
            throw new IllegalArgumentException("seriesIndex must not be negative.");
        }
        kind = Objects.requireNonNull(kind, "kind");
        style = Objects.requireNonNull(style, "style");
        label = Objects.requireNonNull(label, "label");
    }
}
