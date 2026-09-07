package dev.rgcb.scholar.client.ui;

import java.util.List;
import java.util.Objects;

public record MenuDefinition(String title, List<MenuEntry> entries) {
    public MenuDefinition {
        title = Objects.requireNonNull(title, "title");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank.");
        }
        entries = List.copyOf(entries);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("entries must not be empty.");
        }
    }
}
