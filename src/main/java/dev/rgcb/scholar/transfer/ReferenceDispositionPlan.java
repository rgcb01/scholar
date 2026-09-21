package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.document.CrossReference;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ReferenceDispositionPlan(List<Decision> decisions) {
    public enum Disposition { REMAP_INTERNAL, PRESERVE_EXTERNAL_SAME_DOCUMENT, DEGRADE_TO_TEXT }

    public record Decision(String location, CrossReference source, Disposition disposition,
                           Optional<StableIdentityKey> destinationTarget, Optional<String> fallbackText) {
        public Decision {
            location = Objects.requireNonNull(location, "location");
            source = Objects.requireNonNull(source, "source");
            disposition = Objects.requireNonNull(disposition, "disposition");
            destinationTarget = Objects.requireNonNull(destinationTarget, "destinationTarget");
            fallbackText = Objects.requireNonNull(fallbackText, "fallbackText");
            var sourceKey = StableIdentityKey.targetOf(source);
            if (location.isBlank() || (disposition == Disposition.DEGRADE_TO_TEXT) != fallbackText.isPresent()
                    || (disposition == Disposition.DEGRADE_TO_TEXT) == destinationTarget.isPresent()
                    || destinationTarget.filter(key -> key.kind() != sourceKey.kind()).isPresent()
                    || disposition == Disposition.PRESERVE_EXTERNAL_SAME_DOCUMENT
                    && !destinationTarget.orElseThrow().equals(sourceKey)) {
                throw new IllegalArgumentException("Invalid reference disposition.");
            }
        }
    }

    public ReferenceDispositionPlan {
        decisions = List.copyOf(decisions);
        var locations = new HashSet<String>();
        for (var decision : decisions) {
            if (!locations.add(decision.location())) { throw new IllegalArgumentException("Duplicate reference location."); }
        }
    }
}
