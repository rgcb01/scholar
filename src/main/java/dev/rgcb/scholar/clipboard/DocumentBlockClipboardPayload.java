package dev.rgcb.scholar.clipboard;

import dev.rgcb.scholar.document.BlockNode;
import java.util.Objects;

public record DocumentBlockClipboardPayload(BlockNode block) implements ScholarClipboardPayload {
    public DocumentBlockClipboardPayload {
        block = Objects.requireNonNull(block, "block");
    }
}
