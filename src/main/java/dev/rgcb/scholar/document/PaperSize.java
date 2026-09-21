package dev.rgcb.scholar.document;

import java.util.Objects;

public record PaperSize(PaperKind kind, PhysicalLength width, PhysicalLength height) {
    public PaperSize {
        kind = Objects.requireNonNull(kind, "kind");
        width = Objects.requireNonNull(width, "width");
        height = Objects.requireNonNull(height, "height");
        if (width.micrometres() == 0 || height.micrometres() == 0) throw new IllegalArgumentException("Paper dimensions must be positive.");
        if (kind != PaperKind.CUSTOM) {
            var expectedWidth = switch (kind) { case US_LETTER, LEGAL -> PhysicalLength.inches(8.5).micrometres(); case A4 -> 210_000L; case CUSTOM -> 0L; };
            var expectedHeight = switch (kind) { case US_LETTER -> PhysicalLength.inches(11).micrometres(); case LEGAL -> PhysicalLength.inches(14).micrometres(); case A4 -> 297_000L; case CUSTOM -> 0L; };
            if (width.micrometres() != expectedWidth || height.micrometres() != expectedHeight) throw new IllegalArgumentException("Standard paper dimensions are fixed.");
        }
    }
    public static PaperSize letter() { return new PaperSize(PaperKind.US_LETTER, PhysicalLength.inches(8.5), PhysicalLength.inches(11)); }
    public static PaperSize a4() { return new PaperSize(PaperKind.A4, PhysicalLength.millimetres(210), PhysicalLength.millimetres(297)); }
    public static PaperSize legal() { return new PaperSize(PaperKind.LEGAL, PhysicalLength.inches(8.5), PhysicalLength.inches(14)); }
    public static PaperSize custom(PhysicalLength width, PhysicalLength height) { return new PaperSize(PaperKind.CUSTOM, width, height); }
}
