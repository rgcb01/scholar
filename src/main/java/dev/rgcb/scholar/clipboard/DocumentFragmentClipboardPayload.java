package dev.rgcb.scholar.clipboard;

import dev.rgcb.scholar.transfer.DocumentFragment;
import dev.rgcb.scholar.transfer.SourceTransferMetadata;
import java.util.Objects;

/** Runtime carrier, not a serialized document format or nested-editor payload. */
public record DocumentFragmentClipboardPayload(DocumentFragment fragment, SourceTransferMetadata sourceMetadata)
        implements ScholarClipboardPayload {
    public DocumentFragmentClipboardPayload {
        fragment = Objects.requireNonNull(fragment);
        sourceMetadata = Objects.requireNonNull(sourceMetadata);
    }
}
