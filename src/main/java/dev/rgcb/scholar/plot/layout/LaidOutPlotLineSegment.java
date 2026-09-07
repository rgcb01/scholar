package dev.rgcb.scholar.plot.layout;

/** Clipped document-space segment between two consecutive authored line-series points. */
public record LaidOutPlotLineSegment(
        int sourceFromPointIndex,
        int sourceToPointIndex,
        int x1,
        int y1,
        int x2,
        int y2
) {
    public LaidOutPlotLineSegment {
        if (sourceFromPointIndex < 0 || sourceToPointIndex < 0) {
            throw new IllegalArgumentException("Source point indices must not be negative.");
        }
        if (sourceToPointIndex != sourceFromPointIndex + 1) {
            throw new IllegalArgumentException("Line segments must connect consecutive authored points.");
        }
    }
}
