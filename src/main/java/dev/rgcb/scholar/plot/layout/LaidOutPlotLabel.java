package dev.rgcb.scholar.plot.layout;

import dev.rgcb.scholar.layout.TextStyle;
import java.util.Objects;

public record LaidOutPlotLabel(
        String text,
        int x,
        int y,
        int width,
        int height,
        TextStyle style
) {
    public LaidOutPlotLabel {
        text = Objects.requireNonNull(text, "text");
        style = Objects.requireNonNull(style, "style");
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Plot label dimensions must not be negative.");
        }
    }
}
