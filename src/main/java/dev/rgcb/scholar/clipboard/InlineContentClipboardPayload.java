package dev.rgcb.scholar.clipboard;

import dev.rgcb.scholar.document.InlineContent;
import java.util.Objects;

public record InlineContentClipboardPayload(InlineContent content) implements ScholarClipboardPayload {
    public InlineContentClipboardPayload {
        content = Objects.requireNonNull(content, "content");
    }
}
