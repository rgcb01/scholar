package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.document.VariableDependencyReference;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Successful plans contain only authorized dependency rewrites. */
public record VariableDependencyPlan(List<Decision> decisions) {
    public enum Disposition { REMAP_TRAVELING_TARGET, PRESERVE_PROVEN_SAME_DOCUMENT_TARGET }

    public record Decision(String location, VariableDependencyReference source, Disposition disposition,
                           StableIdentityKey destination) {
        public Decision {
            location = Objects.requireNonNull(location, "location");
            source = Objects.requireNonNull(source, "source");
            disposition = Objects.requireNonNull(disposition, "disposition");
            destination = Objects.requireNonNull(destination, "destination");
            if (location.isBlank() || destination.kind() != StableIdentityKind.VARIABLE
                    || disposition == Disposition.PRESERVE_PROVEN_SAME_DOCUMENT_TARGET
                    && !destination.equals(new StableIdentityKey(StableIdentityKind.VARIABLE, source.variableId()))) {
                throw new IllegalArgumentException("Invalid variable dependency decision.");
            }
        }
    }

    public VariableDependencyPlan {
        decisions = List.copyOf(decisions);
        var locations = new HashSet<String>();
        for (var decision : decisions) {
            if (!locations.add(decision.location())) {
                throw new IllegalArgumentException("Duplicate variable dependency location.");
            }
        }
    }
}
