package dev.rgcb.scholar.diagram.layout;

import dev.rgcb.scholar.layout.TextStyle;
import java.util.Objects;

public record LaidOutDiagramLabel(
        String text,
        int x,
        int y,
        int width,
        int height,
        TextStyle style
) {
    public LaidOutDiagramLabel {
        text = Objects.requireNonNull(text, "text");
        style = Objects.requireNonNull(style, "style");
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Diagram label dimensions must not be negative.");
        }
    }
}
