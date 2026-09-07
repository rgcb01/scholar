package dev.rgcb.scholar.math.editor;

import java.util.Objects;

public record MathRangeSelection(MathPosition anchor, MathPosition active) implements MathSelection {
    public MathRangeSelection {
        anchor = Objects.requireNonNull(anchor, "anchor");
        active = Objects.requireNonNull(active, "active");
        if (anchor.equals(active)) {
            throw new IllegalArgumentException("Collapsed math selections must use MathCaretSelection.");
        }
    }
}
