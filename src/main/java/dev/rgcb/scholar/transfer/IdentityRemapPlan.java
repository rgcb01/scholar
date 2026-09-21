package dev.rgcb.scholar.transfer;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

public record IdentityRemapPlan(Map<StableIdentityKey, Decision> decisions) {
    public enum Disposition { PRESERVED, REUSED_EXISTING, REMAPPED_NEW }

    public record Decision(StableIdentityKey source, StableIdentityKey destination, Disposition disposition) {
        public Decision {
            source = Objects.requireNonNull(source, "source");
            destination = Objects.requireNonNull(destination, "destination");
            disposition = Objects.requireNonNull(disposition, "disposition");
            if (source.kind() != destination.kind() || (disposition == Disposition.REMAPPED_NEW) == source.equals(destination)) {
                throw new IllegalArgumentException("Identity decision must preserve namespace and match disposition.");
            }
        }
    }

    public IdentityRemapPlan {
        decisions = Map.copyOf(decisions);
        var destinations = new HashSet<StableIdentityKey>();
        for (var entry : decisions.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().source()) || !destinations.add(entry.getValue().destination())) {
                throw new IllegalArgumentException("Identity decisions require consistent keys and unique destinations.");
            }
        }
    }

    public StableIdentityKey destinationOf(StableIdentityKey source) {
        return Objects.requireNonNull(decisions.get(source), "Source identity is not planned").destination();
    }
}
