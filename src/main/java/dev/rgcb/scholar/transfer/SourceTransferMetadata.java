package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.BlockNode;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Explicit operational companion; no live source Document, resolver, or editor pointer. */
public record SourceTransferMetadata(
        Optional<RuntimeDocumentToken> documentToken,
        Map<StableIdentityKey, BlockNode> targetWitnesses,
        Map<StableIdentityKey, ScientificDataset> datasetWitnesses,
        Map<StableIdentityKey, String> referenceText
) {
    public SourceTransferMetadata {
        documentToken = Objects.requireNonNull(documentToken, "documentToken");
        targetWitnesses = Map.copyOf(targetWitnesses);
        datasetWitnesses = Map.copyOf(datasetWitnesses);
        referenceText = Map.copyOf(referenceText);
        for (var entry : targetWitnesses.entrySet()) {
            if (!FragmentIdentityIndex.blockIdentity(entry.getValue()).filter(entry.getKey()::equals).isPresent()) {
                throw new IllegalArgumentException("Target witness must match its typed identity.");
            }
        }
        for (var entry : datasetWitnesses.entrySet()) {
            if (!entry.getKey().equals(new StableIdentityKey(StableIdentityKind.DATASET, entry.getValue().id()))) {
                throw new IllegalArgumentException("Dataset witness must match its typed identity.");
            }
        }
        if (referenceText.keySet().stream().anyMatch(key -> key.kind() == StableIdentityKind.DATASET
                || key.kind() == StableIdentityKind.VARIABLE || key.kind() == StableIdentityKind.ANALYSIS)) {
            throw new IllegalArgumentException("Reference text requires a CrossReference target namespace.");
        }
    }

    public static SourceTransferMetadata unknown() {
        return new SourceTransferMetadata(Optional.empty(), Map.of(), Map.of(), Map.of());
    }
}
