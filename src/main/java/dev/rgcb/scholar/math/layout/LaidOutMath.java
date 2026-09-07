package dev.rgcb.scholar.math.layout;

import java.util.Objects;

public record LaidOutMath(MathBox root) {
    public LaidOutMath {
        root = Objects.requireNonNull(root, "root");
    }

    public int width() {
        return root.width();
    }

    public int height() {
        return root.ascent() + root.descent();
    }
}
