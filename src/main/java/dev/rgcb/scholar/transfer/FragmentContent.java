package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.InlineContent;
import java.util.List;
import java.util.Set;

/** Source semantic shapes, not clipboard strings or editor selections. */
public sealed interface FragmentContent {
    record Blocks(List<BlockNode> roots) implements FragmentContent {
        public Blocks {
            roots = List.copyOf(roots);
            if (roots.isEmpty()) {
                throw new IllegalArgumentException("Blocks requires at least one root.");
            }
        }
    }

    record InlineSegments(List<InlineContent> segments) implements FragmentContent {
        public InlineSegments {
            segments = List.copyOf(segments);
            if (segments.isEmpty()) {
                throw new IllegalArgumentException("InlineSegments requires at least one segment.");
            }
        }
    }

    record ResourcePrimary(Set<StableIdentityKey> selectedResources) implements FragmentContent {
        public ResourcePrimary {
            selectedResources = Set.copyOf(selectedResources);
            if (selectedResources.isEmpty()
                    || selectedResources.stream().anyMatch(key -> key.kind() != StableIdentityKind.DATASET)) {
                throw new IllegalArgumentException("ResourcePrimary requires dataset identities.");
            }
        }
    }
}
