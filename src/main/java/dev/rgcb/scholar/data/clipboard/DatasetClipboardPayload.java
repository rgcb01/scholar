package dev.rgcb.scholar.data.clipboard;

import dev.rgcb.scholar.clipboard.ScholarClipboardPayload;
import dev.rgcb.scholar.data.ScientificDataset;
import java.util.Objects;

public record DatasetClipboardPayload(ScientificDataset dataset) implements ScholarClipboardPayload {
    public DatasetClipboardPayload {
        dataset = Objects.requireNonNull(dataset, "dataset");
    }
}
