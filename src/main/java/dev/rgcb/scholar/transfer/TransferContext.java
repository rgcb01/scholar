package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.document.Document;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Original destination snapshot and approved survivor facts; concrete insertion remains deferred. */
public record TransferContext(
        Document destination,
        Optional<RuntimeDocumentToken> destinationToken,
        SourceTransferMetadata sourceMetadata,
        Scope scope,
        Set<Integer> removedBlockIndices
) {
    public enum Scope { DOCUMENT_CONTENT, BLOCKS, INLINE, RESOURCES }

    public TransferContext(Document destination, Optional<RuntimeDocumentToken> token, SourceTransferMetadata metadata) {
        this(destination, token, metadata, Scope.DOCUMENT_CONTENT, Set.of());
    }

    public TransferContext {
        destination = Objects.requireNonNull(destination, "destination");
        destinationToken = Objects.requireNonNull(destinationToken, "destinationToken");
        sourceMetadata = Objects.requireNonNull(sourceMetadata, "sourceMetadata");
        scope = Objects.requireNonNull(scope, "scope");
        removedBlockIndices = Set.copyOf(removedBlockIndices);
        for (var index : removedBlockIndices) {
            if (index < 0 || index >= destination.blocks().size()) {
                throw new IllegalArgumentException("Removed root index is outside destination.");
            }
        }
    }

    /** Proves the live document relation only, not individual target/resource applicability. */
    public boolean hasSameDocumentToken() {
        return destinationToken.isPresent() && sourceMetadata.documentToken().isPresent()
                && destinationToken.get() == sourceMetadata.documentToken().get();
    }
}
