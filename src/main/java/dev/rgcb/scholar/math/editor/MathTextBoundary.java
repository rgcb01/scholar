package dev.rgcb.scholar.math.editor;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class MathTextBoundary {
    private MathTextBoundary() {
    }

    static int characterCount(String text) {
        return boundaries(text).size() - 1;
    }

    static String substring(String text, int startOffset, int endOffset) {
        validateRange(text, startOffset, endOffset);
        return text.substring(toUtf16Index(text, startOffset), toUtf16Index(text, endOffset));
    }

    static int previousOffset(String text, int characterOffset) {
        validateOffset(text, characterOffset);
        return Math.max(0, characterOffset - 1);
    }

    static int nextOffset(String text, int characterOffset) {
        validateOffset(text, characterOffset);
        return Math.min(characterCount(text), characterOffset + 1);
    }

    private static int toUtf16Index(String text, int characterOffset) {
        var boundaries = boundaries(text);
        if (characterOffset < 0 || characterOffset >= boundaries.size()) {
            throw new IllegalArgumentException("characterOffset is outside the valid boundary range.");
        }
        return boundaries.get(characterOffset);
    }

    private static void validateOffset(String text, int characterOffset) {
        var count = characterCount(text);
        if (characterOffset < 0 || characterOffset > count) {
            throw new IllegalArgumentException("characterOffset must be between 0 and " + count + ".");
        }
    }

    private static void validateRange(String text, int startOffset, int endOffset) {
        validateOffset(text, startOffset);
        validateOffset(text, endOffset);
        if (startOffset > endOffset) {
            throw new IllegalArgumentException("startOffset must not be after endOffset.");
        }
    }

    private static List<Integer> boundaries(String text) {
        Objects.requireNonNull(text, "text");
        var iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(text);
        var boundaries = new ArrayList<Integer>();
        for (var boundary = iterator.first(); boundary != BreakIterator.DONE; boundary = iterator.next()) {
            boundaries.add(boundary);
        }
        return boundaries;
    }
}
