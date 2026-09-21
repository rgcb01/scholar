package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.document.CrossReference;
import java.util.Objects;

public record StableIdentityKey(StableIdentityKind kind, String id) {
    public StableIdentityKey {
        kind = Objects.requireNonNull(kind, "kind");
        id = Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Identity id must not be blank.");
        }
    }

    public static StableIdentityKey targetOf(CrossReference reference) {
        Objects.requireNonNull(reference, "reference");
        var kind = switch (reference.kind()) {
            case SECTION -> StableIdentityKind.SECTION;
            case EQUATION -> StableIdentityKind.EQUATION;
            case TABLE -> StableIdentityKind.TABLE;
            case FIGURE -> StableIdentityKind.FIGURE;
        };
        return new StableIdentityKey(kind, reference.targetId());
    }
}
