package dev.rgcb.scholar.document;

import java.util.Objects;

public record PageMargins(PhysicalLength top, PhysicalLength right, PhysicalLength bottom, PhysicalLength left) {
    public PageMargins {
        top = Objects.requireNonNull(top); right = Objects.requireNonNull(right);
        bottom = Objects.requireNonNull(bottom); left = Objects.requireNonNull(left);
    }
    public static PageMargins normal() { var v = PhysicalLength.inches(1); return new PageMargins(v, v, v, v); }
    public static PageMargins narrow() { var v = PhysicalLength.inches(0.5); return new PageMargins(v, v, v, v); }
}
