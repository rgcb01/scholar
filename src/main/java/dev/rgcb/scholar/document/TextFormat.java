package dev.rgcb.scholar.document;

import java.util.Optional;

public record TextFormat(Optional<ScholarFontFamily> fontFamily, Optional<Integer> fontSizeHalfPoints) {
    public TextFormat {
        fontFamily = fontFamily == null ? Optional.empty() : fontFamily;
        fontSizeHalfPoints = fontSizeHalfPoints == null ? Optional.empty() : fontSizeHalfPoints;
        fontSizeHalfPoints.ifPresent(size -> { if (size < 12 || size > 144) throw new IllegalArgumentException("Font size must be 6-72 points."); });
    }
    public static TextFormat none() { return new TextFormat(Optional.empty(), Optional.empty()); }
    public TextFormat overrideWith(TextFormat local) {
        return new TextFormat(
                local.fontFamily().isPresent() ? local.fontFamily() : fontFamily,
                local.fontSizeHalfPoints().isPresent() ? local.fontSizeHalfPoints() : fontSizeHalfPoints);
    }
}
