package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.data.ScientificDataset;
import java.util.Map;
import java.util.Objects;

public record ResourceTransferPlan(Map<StableIdentityKey, Decision> decisions) {
    public enum Disposition { TRANSFER_AS_NEW, REUSE_EXISTING_SAME_DOCUMENT }

    /** Source value remains unchanged even when destination id differs. */
    public record Decision(ScientificDataset sourceValue, StableIdentityKey destination, Disposition disposition) {
        public Decision {
            sourceValue = Objects.requireNonNull(sourceValue, "sourceValue");
            destination = Objects.requireNonNull(destination, "destination");
            disposition = Objects.requireNonNull(disposition, "disposition");
            if (destination.kind() != StableIdentityKind.DATASET
                    || disposition == Disposition.REUSE_EXISTING_SAME_DOCUMENT && !sourceValue.id().equals(destination.id())) {
                throw new IllegalArgumentException("Invalid resource decision.");
            }
        }
    }

    public ResourceTransferPlan {
        decisions = Map.copyOf(decisions);
        for (var entry : decisions.entrySet()) {
            if (!entry.getKey().equals(new StableIdentityKey(StableIdentityKind.DATASET, entry.getValue().sourceValue().id()))) {
                throw new IllegalArgumentException("Resource key must match source value.");
            }
        }
    }
}
