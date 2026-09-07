package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.layout.LaidOutBlock;
import dev.rgcb.scholar.layout.LaidOutBlockKind;
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.layout.TextMeasurer;
import java.util.Objects;
import java.util.Optional;

public final class SelectionDragResolver {
    private final DocumentHitTester hitTester = new DocumentHitTester();

    public Optional<DocumentPosition> resolve(
            LaidOutDocument document,
            DocumentPosition anchor,
            int x,
            int y,
            TextMeasurer textMeasurer
    ) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        if (anchor.blockIndex() < 0 || anchor.blockIndex() >= document.blocks().size()) {
            return Optional.empty();
        }
        if (!isEditableTextBlock(document.blocks().get(anchor.blockIndex()))) {
            return Optional.empty();
        }

        var regionStart = regionStart(document, anchor.blockIndex());
        var regionEnd = regionEnd(document, anchor.blockIndex());
        for (var blockIndex = regionStart; blockIndex < regionEnd; blockIndex++) {
            var previous = document.blocks().get(blockIndex);
            var next = document.blocks().get(blockIndex + 1);
            var gapStart = previous.y() + previous.height();
            var gapEnd = next.y();
            if (y >= gapStart && y < gapEnd) {
                var midpoint = gapStart + (gapEnd - gapStart) / 2;
                return Optional.of(y < midpoint
                        ? blockEndPosition(previous, blockIndex)
                        : new DocumentPosition(blockIndex + 1, 0));
            }
        }

        var directHit = hitTester.hitTest(document, x, y, textMeasurer)
                .filter(position -> position.blockIndex() >= regionStart && position.blockIndex() <= regionEnd);
        if (directHit.isPresent()) {
            return directHit;
        }

        if (y < document.blocks().get(regionStart).y()) {
            return Optional.of(new DocumentPosition(regionStart, 0));
        }
        if (y >= document.blocks().get(regionEnd).y() + document.blocks().get(regionEnd).height()) {
            return Optional.of(blockEndPosition(document.blocks().get(regionEnd), regionEnd));
        }

        return Optional.empty();
    }

    private static int regionStart(LaidOutDocument document, int anchorBlockIndex) {
        var index = anchorBlockIndex;
        while (index > 0 && isEditableTextBlock(document.blocks().get(index - 1))) {
            index--;
        }
        return index;
    }

    private static int regionEnd(LaidOutDocument document, int anchorBlockIndex) {
        var index = anchorBlockIndex;
        while (index + 1 < document.blocks().size() && isEditableTextBlock(document.blocks().get(index + 1))) {
            index++;
        }
        return index;
    }

    private static DocumentPosition blockEndPosition(LaidOutBlock block, int blockIndex) {
        var end = 0;
        for (var line : block.lines()) {
            for (var run : line.textRuns()) {
                if (run.sourceBlockIndex() == blockIndex) {
                    end = Math.max(end, run.sourceEnd());
                }
            }
        }
        return new DocumentPosition(blockIndex, end);
    }

    private static boolean isEditableTextBlock(LaidOutBlock block) {
        return block.kind() == LaidOutBlockKind.PARAGRAPH || block.kind() == LaidOutBlockKind.HEADING;
    }
}
