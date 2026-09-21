package dev.rgcb.scholar.document;

/** Deterministic physical length stored as integer micrometres. */
public record PhysicalLength(long micrometres) implements Comparable<PhysicalLength> {
    public PhysicalLength {
        if (micrometres < 0) throw new IllegalArgumentException("Physical length cannot be negative.");
    }
    public static PhysicalLength millimetres(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Millimetres must be finite and non-negative.");
        return new PhysicalLength(Math.round(value * 1_000.0));
    }
    public static PhysicalLength inches(double value) { return millimetres(value * 25.4); }
    public int logicalUnits() { return Math.toIntExact(Math.round(micrometres / 500.0)); }
    public int compareTo(PhysicalLength other) { return Long.compare(micrometres, other.micrometres); }
}
