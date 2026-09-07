package dev.rgcb.scholar.math.layout;

import java.util.Objects;

public record PositionedMathBox(MathBox box, int x, int baselineOffset) {
    public PositionedMathBox {
        box = Objects.requireNonNull(box, "box");
    }
}
