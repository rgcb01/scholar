package dev.rgcb.scholar.table.clipboard;

import dev.rgcb.scholar.clipboard.ScholarClipboardPayload;
import dev.rgcb.scholar.document.TableBlock;
import java.util.Objects;

public record TableClipboardPayload(TableBlock table) implements ScholarClipboardPayload {
    public TableClipboardPayload {
        table = Objects.requireNonNull(table, "table");
    }
}
