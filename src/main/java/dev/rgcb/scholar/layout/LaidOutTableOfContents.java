package dev.rgcb.scholar.layout;

import java.util.List;
import java.util.Objects;

public record LaidOutTableOfContents(List<LaidOutTableOfContentsEntry> entries) {
    public LaidOutTableOfContents {
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }
}
