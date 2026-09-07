package dev.rgcb.scholar.math.layout;

import dev.rgcb.scholar.math.editor.MathPath;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MathBox(
        int width,
        int ascent,
        int descent,
        List<PositionedMathBox> children,
        List<MathPrimitive> primitives,
        Optional<MathPath> sourcePath
) {
    public MathBox(int width, int ascent, int descent, List<PositionedMathBox> children, List<MathPrimitive> primitives) {
        this(width, ascent, descent, children, primitives, Optional.empty());
    }

    public MathBox {
        if (width < 0) {
            throw new IllegalArgumentException("width must not be negative.");
        }
        if (ascent < 0) {
            throw new IllegalArgumentException("ascent must not be negative.");
        }
        if (descent < 0) {
            throw new IllegalArgumentException("descent must not be negative.");
        }
        children = List.copyOf(children);
        primitives = List.copyOf(primitives);
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
    }
}
