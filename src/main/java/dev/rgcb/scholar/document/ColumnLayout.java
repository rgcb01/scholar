package dev.rgcb.scholar.document;

import java.util.Objects;

public record ColumnLayout(int count, PhysicalLength gap) {
    public ColumnLayout {
        if (count < 1 || count > 2) throw new IllegalArgumentException("M30 supports one or two columns.");
        gap = Objects.requireNonNull(gap);
    }
    public static ColumnLayout one() { return new ColumnLayout(1, PhysicalLength.millimetres(0)); }
    public static ColumnLayout two() { return new ColumnLayout(2, PhysicalLength.millimetres(6)); }
}
