package dev.rgcb.scholar.editor;

import java.util.Objects;
import java.util.Optional;

public record BlockStyleSelectionState(Kind kind, Optional<BlockStyle> style) {
    public BlockStyleSelectionState {
        kind = Objects.requireNonNull(kind, "kind");
        style = Objects.requireNonNull(style, "style");
        if (kind == Kind.SINGLE && style.isEmpty()) {
            throw new IllegalArgumentException("SINGLE block style state requires a style.");
        }
        if (kind != Kind.SINGLE && style.isPresent()) {
            throw new IllegalArgumentException("Only SINGLE block style state may carry a style.");
        }
    }

    public static BlockStyleSelectionState notApplicable() {
        return new BlockStyleSelectionState(Kind.NOT_APPLICABLE, Optional.empty());
    }

    public static BlockStyleSelectionState single(BlockStyle style) {
        return new BlockStyleSelectionState(Kind.SINGLE, Optional.of(style));
    }

    public static BlockStyleSelectionState mixed() {
        return new BlockStyleSelectionState(Kind.MIXED, Optional.empty());
    }

    public enum Kind {
        NOT_APPLICABLE,
        SINGLE,
        MIXED
    }
}
