package dev.rgcb.scholar.document;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/** Pure first-free suffix convention; callers own normalization, namespaces and reservations. */
public final class StableIdAllocator {
    private StableIdAllocator() {}

    public static String firstFree(String base, Set<String> occupied) {
        return firstFree(base, Objects.requireNonNull(occupied, "occupied")::contains);
    }

    public static String firstFree(String base, Predicate<String> occupied) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(occupied, "occupied");
        if (!occupied.test(base)) { return base; }
        for (var suffix = 2; suffix > 0; suffix++) {
            var candidate = base + "-" + suffix;
            if (!occupied.test(candidate)) { return candidate; }
        }
        throw new IllegalStateException("Stable identity suffix space is exhausted.");
    }
}
