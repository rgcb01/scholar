package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.data.ScientificDataset;
import java.util.List;
import java.util.Objects;

/** Immutable source semantics; proof/export metadata lives separately in SourceTransferMetadata. */
public record DocumentFragment(
        FragmentContent content,
        List<ScientificDataset> resources,
        FragmentIdentityIndex identities
) {
    public DocumentFragment(FragmentContent content, List<ScientificDataset> resources) {
        this(content, resources, FragmentIdentityIndex.derive(
                Objects.requireNonNull(content, "content"), List.copyOf(resources)));
    }

    public DocumentFragment {
        content = Objects.requireNonNull(content, "content");
        resources = List.copyOf(resources);
        identities = Objects.requireNonNull(identities, "identities");
        if (!identities.equals(FragmentIdentityIndex.derive(content, resources))) {
            throw new IllegalArgumentException("Identity index must agree with fragment content and resources.");
        }
    }
}
