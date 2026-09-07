package dev.rgcb.scholar.editor;

import java.util.Objects;
import java.util.Optional;

public record DocumentHit(Kind kind, Optional<DocumentPosition> position, Optional<Integer> blockIndex) {
    public DocumentHit {
        kind = Objects.requireNonNull(kind, "kind");
        position = Objects.requireNonNull(position, "position");
        blockIndex = Objects.requireNonNull(blockIndex, "blockIndex");
        if (kind == Kind.TEXT && position.isEmpty()) {
            throw new IllegalArgumentException("TEXT hit requires a document position.");
        }
        if (kind == Kind.BLOCK && blockIndex.isEmpty()) {
            throw new IllegalArgumentException("BLOCK hit requires a block index.");
        }
        if (kind != Kind.TEXT && position.isPresent()) {
            throw new IllegalArgumentException("Only TEXT hits may carry a document position.");
        }
        if (kind != Kind.BLOCK && blockIndex.isPresent()) {
            throw new IllegalArgumentException("Only BLOCK hits may carry a block index.");
        }
    }

    public static DocumentHit text(DocumentPosition position) {
        return new DocumentHit(Kind.TEXT, Optional.of(position), Optional.empty());
    }

    public static DocumentHit block(int blockIndex) {
        return new DocumentHit(Kind.BLOCK, Optional.empty(), Optional.of(blockIndex));
    }

    public static DocumentHit none() {
        return new DocumentHit(Kind.NONE, Optional.empty(), Optional.empty());
    }

    public enum Kind {
        TEXT,
        BLOCK,
        NONE
    }
}
