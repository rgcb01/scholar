package dev.rgcb.scholar.transfer;

import java.util.List;
import java.util.Objects;

/** Proof for each fit overlay's analysis target; not a textual-ID lookup at materialization. */
public record AnalysisDependencyPlan(List<Decision> decisions) {
    public enum Disposition { REMAP_TRAVELING_TARGET, PRESERVE_PROVEN_SAME_DOCUMENT_TARGET }
    public record Decision(String location, String sourceId, Disposition disposition, StableIdentityKey destination) {
        public Decision {
            Objects.requireNonNull(location);
            Objects.requireNonNull(sourceId);
            Objects.requireNonNull(disposition);
            Objects.requireNonNull(destination);
            if (location.isBlank() || sourceId.isBlank() || destination.kind() != StableIdentityKind.ANALYSIS
                    || disposition == Disposition.PRESERVE_PROVEN_SAME_DOCUMENT_TARGET
                    && !destination.id().equals(sourceId)) {
                throw new IllegalArgumentException("Invalid fit dependency decision.");
            }
        }
    }
    public AnalysisDependencyPlan { decisions = List.copyOf(decisions); }
}
