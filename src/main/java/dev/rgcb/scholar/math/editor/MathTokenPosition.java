package dev.rgcb.scholar.math.editor;

import java.util.Objects;

public record MathTokenPosition(MathPath tokenPath, int characterOffset) implements MathPosition {
    public MathTokenPosition {
        tokenPath = Objects.requireNonNull(tokenPath, "tokenPath");
        if (characterOffset <= 0) {
            throw new IllegalArgumentException("MathTokenPosition is only valid strictly inside a token.");
        }
    }
}
