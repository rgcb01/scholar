package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.math.editor.MathSelection;
import java.util.Objects;

public record EquationEditingSelection(int blockIndex, MathSelection selection) implements EditorSelection {
    public EquationEditingSelection {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must not be negative.");
        }
        selection = Objects.requireNonNull(selection, "selection");
    }
}
