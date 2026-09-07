package dev.rgcb.scholar.plot.layout;

import java.util.Objects;

/**
 * Deterministic presentation style assigned during plot layout. It is not authored
 * semantic PlotDefinition state and contains no Minecraft colors.
 */
public record LaidOutPlotSeriesStyle(
        int styleIndex,
        PlotLinePattern linePattern,
        PlotMarkerShape markerShape
) {
    public LaidOutPlotSeriesStyle {
        if (styleIndex < 0) {
            throw new IllegalArgumentException("styleIndex must not be negative.");
        }
        linePattern = Objects.requireNonNull(linePattern, "linePattern");
        markerShape = Objects.requireNonNull(markerShape, "markerShape");
    }

    public static LaidOutPlotSeriesStyle forSeriesIndex(int seriesIndex) {
        if (seriesIndex < 0) {
            throw new IllegalArgumentException("seriesIndex must not be negative.");
        }
        var patterns = PlotLinePattern.values();
        var markers = PlotMarkerShape.values();
        return new LaidOutPlotSeriesStyle(
                seriesIndex,
                patterns[seriesIndex % patterns.length],
                markers[seriesIndex % markers.length]);
    }
}
