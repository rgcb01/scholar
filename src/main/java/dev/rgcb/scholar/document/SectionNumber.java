package dev.rgcb.scholar.document;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record SectionNumber(List<Integer> parts) {
    public SectionNumber {
        parts = List.copyOf(Objects.requireNonNull(parts, "parts"));
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Section number must contain at least one part.");
        }
        if (parts.stream().anyMatch(part -> part < 0)) {
            throw new IllegalArgumentException("Section number parts must not be negative.");
        }
        if (parts.get(parts.size() - 1) <= 0) {
            throw new IllegalArgumentException("Section number final part must be positive.");
        }
    }

    public String displayText() {
        return parts.stream().map(String::valueOf).collect(Collectors.joining("."));
    }
}
