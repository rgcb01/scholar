package dev.rgcb.scholar.document;

import java.util.Objects;
import java.util.Optional;

public record SectionEntry(
        int blockIndex,
        int level,
        Optional<String> id,
        SectionNumber number,
        String title
) {
    public SectionEntry {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must not be negative.");
        }
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("level must be between 1 and 6.");
        }
        id = Objects.requireNonNull(id, "id");
        number = Objects.requireNonNull(number, "number");
        title = Objects.requireNonNull(title, "title");
    }

    public String displayLabel() {
        return number.displayText();
    }

    public String displayText() {
        return title.isBlank() ? displayLabel() : displayLabel() + " " + title;
    }
}
