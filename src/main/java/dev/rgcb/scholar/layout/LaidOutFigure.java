package dev.rgcb.scholar.layout;

import java.util.List;
import java.util.Objects;

public record LaidOutFigure(
        String id,
        int number,
        int x,
        int y,
        int width,
        int height,
        LaidOutBlock content,
        int captionY,
        List<LaidOutLine> captionLines
) {
    public LaidOutFigure {
        id = Objects.requireNonNull(id, "id");
        content = Objects.requireNonNull(content, "content");
        captionLines = List.copyOf(captionLines);
        if (number <= 0) {
            throw new IllegalArgumentException("Figure number must be positive.");
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Figure dimensions must not be negative.");
        }
    }
}
