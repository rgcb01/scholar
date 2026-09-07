package dev.rgcb.scholar.math.editor;

import java.util.Objects;

public record MathCaretSelection(MathPosition caret) implements MathSelection {
    public MathCaretSelection {
        caret = Objects.requireNonNull(caret, "caret");
    }
}
