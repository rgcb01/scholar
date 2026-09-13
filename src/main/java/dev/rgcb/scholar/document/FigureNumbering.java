package dev.rgcb.scholar.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/** Deterministic document-order figure numbering. */
public final class FigureNumbering {
    private FigureNumbering() {
    }

    public static OptionalInt numberFor(Document document, int blockIndex) {
        Objects.requireNonNull(document, "document");
        if (blockIndex < 0 || blockIndex >= document.blocks().size()) {
            throw new IllegalArgumentException("blockIndex is outside the document.");
        }
        var number = 0;
        for (var index = 0; index <= blockIndex; index++) {
            if (document.blocks().get(index) instanceof FigureBlock) {
                number++;
            }
        }
        return document.blocks().get(blockIndex) instanceof FigureBlock
                ? OptionalInt.of(number)
                : OptionalInt.empty();
    }

    public static List<Integer> numbers(Document document) {
        Objects.requireNonNull(document, "document");
        var numbers = new ArrayList<Integer>();
        var next = 1;
        for (var block : document.blocks()) {
            if (block instanceof FigureBlock) {
                numbers.add(next++);
            }
        }
        return List.copyOf(numbers);
    }
}
