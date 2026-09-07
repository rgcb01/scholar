package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.clipboard.ScholarClipboardService;
import java.util.Objects;

public record EditorActionContext(EditorSession session, ClipboardAdapter clipboard, ScholarClipboardService scholarClipboard) {
    public EditorActionContext(EditorSession session, ClipboardAdapter clipboard) {
        this(session, clipboard, new ScholarClipboardService());
    }

    public EditorActionContext {
        session = Objects.requireNonNull(session, "session");
        clipboard = Objects.requireNonNull(clipboard, "clipboard");
        scholarClipboard = Objects.requireNonNull(scholarClipboard, "scholarClipboard");
    }
}
