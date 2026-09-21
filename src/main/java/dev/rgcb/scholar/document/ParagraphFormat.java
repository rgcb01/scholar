package dev.rgcb.scholar.document;

import java.util.Optional;

/** Explicit paragraph overrides; absent values inherit from semantic style/template. */
public record ParagraphFormat(Optional<ParagraphAlignment> alignment, Optional<Integer> lineSpacingPermille,
                              Optional<Integer> spaceBefore, Optional<Integer> spaceAfter,
                              Optional<Integer> leftIndent, Optional<Integer> rightIndent,
                              Optional<Integer> firstLineIndent) {
    public ParagraphFormat {
        alignment = alignment == null ? Optional.empty() : alignment;
        lineSpacingPermille = value(lineSpacingPermille, 500, 4000, "line spacing");
        spaceBefore = value(spaceBefore, 0, 500, "space before"); spaceAfter = value(spaceAfter, 0, 500, "space after");
        leftIndent = value(leftIndent, 0, 1000, "left indent"); rightIndent = value(rightIndent, 0, 1000, "right indent");
        firstLineIndent = value(firstLineIndent, -1000, 1000, "first-line indent");
    }
    public static ParagraphFormat none() { return new ParagraphFormat(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()); }
    private static Optional<Integer> value(Optional<Integer> value, int min, int max, String name) {
        value = value == null ? Optional.empty() : value;
        value.ifPresent(v -> { if (v < min || v > max) throw new IllegalArgumentException("Invalid " + name + '.'); });
        return value;
    }
}
