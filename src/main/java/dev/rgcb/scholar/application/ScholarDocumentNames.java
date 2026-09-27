package dev.rgcb.scholar.application;

import java.util.Collection;
import java.util.Objects;

final class ScholarDocumentNames {
    private ScholarDocumentNames() { }

    static String normalize(String value) {
        var result = Objects.requireNonNull(value, "displayName").replaceAll("[\\p{Cntrl}]", "").strip();
        if (result.isEmpty()) throw new IllegalArgumentException("Document name must not be blank.");
        return result.length() <= 64 ? result : result.substring(0, 64).strip();
    }

    static String unique(String base, Collection<String> existing) {
        base = normalize(base);
        Objects.requireNonNull(existing, "existing");
        if (!existing.contains(base)) return base;
        for (var suffix = 2; ; suffix++) {
            var candidate = base + " " + suffix;
            if (!existing.contains(candidate)) return candidate;
        }
    }
}
