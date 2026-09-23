package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.layout.LaidOutBlock;
import dev.rgcb.scholar.layout.LaidOutBlockKind;
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.layout.TextMeasurer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SelectionGeometryResolver {
    private static final int BOUNDARY_SELECTION_WIDTH = 8;

    public List<SelectionRect> resolve(DocumentRange range, LaidOutDocument document, TextMeasurer textMeasurer) {
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        if (range.isEmpty()) {
            return List.of();
        }
        validateRange(document, range);

        if (range.isSingleBlock()) {
            return resolveBlock(range.start().blockIndex(), range.start().characterOffset(), range.end().characterOffset(), document, textMeasurer, false);
        }

        var rects = new ArrayList<SelectionRect>();
        rects.addAll(resolveBlock(
                range.start().blockIndex(),
                range.start().characterOffset(),
                blockEndOffset(document.blocks().get(range.start().blockIndex()), range.start().blockIndex()),
                document,
                textMeasurer,
                false));

        for (var blockIndex = range.start().blockIndex() + 1; blockIndex < range.end().blockIndex(); blockIndex++) {
            rects.addAll(resolveBlock(
                    blockIndex,
                    0,
                    blockEndOffset(document.blocks().get(blockIndex), blockIndex),
                    document,
                    textMeasurer,
                    true));
        }

        rects.addAll(resolveBlock(
                range.end().blockIndex(),
                0,
                range.end().characterOffset(),
                document,
                textMeasurer,
                true));

        if (rects.isEmpty()) {
            rects.add(boundaryRect(document.blocks().get(range.end().blockIndex())));
        }
        return List.copyOf(rects);
    }

    private static List<SelectionRect> resolveBlock(
            int blockIndex,
            int startOffset,
            int endOffset,
            LaidOutDocument document,
            TextMeasurer textMeasurer,
            boolean includeStructuralBoundary
    ) {
        var rects = new ArrayList<SelectionRect>();
        var block = document.blocks().get(blockIndex);
        if (block.lines().isEmpty()) {
            return List.of();
        }
        if (startOffset == 0 && endOffset == 0 && includeStructuralBoundary) {
            rects.add(boundaryRect(block));
            return List.copyOf(rects);
        }

        for (var line : block.lines()) {
            for (var run : line.textRuns()) {
                if (run.sourceBlockIndex() != blockIndex) {
                    continue;
                }
                var start = Math.max(startOffset, run.sourceStart());
                var end = Math.min(endOffset, run.sourceEnd());
                if (start >= end) {
                    continue;
                }
                if (run.atomic()) {
                    rects.add(new SelectionRect(run.x(), line.y(), run.width(), line.height()));
                    continue;
                }
                var prefix = TextBoundary.substring(run.text(), 0, start - run.sourceStart());
                var selected = TextBoundary.substring(run.text(), start - run.sourceStart(), end - run.sourceStart());
                var prefixWidth = textMeasurer.measureWidth(prefix, run.style());
                var x = run.x() + prefixWidth;
                var width = textMeasurer.measureWidth(prefix + selected, run.style()) - prefixWidth;
                if (width > 0) {
                    rects.add(new SelectionRect(x, line.y(), width, line.height()));
                }
            }
        }
        if (rects.isEmpty() && includeStructuralBoundary) {
            rects.add(boundaryRect(block));
        }
        return List.copyOf(rects);
    }

    private static SelectionRect boundaryRect(LaidOutBlock block) {
        var line = block.lines().isEmpty() ? null : block.lines().get(0);
        var x = line == null ? block.x() : line.x();
        var y = line == null ? block.y() : line.y();
        var height = line == null ? block.height() : line.height();
        return new SelectionRect(x, y, Math.min(BOUNDARY_SELECTION_WIDTH, Math.max(1, block.width())), height);
    }

    private static int blockEndOffset(LaidOutBlock block, int blockIndex) {
        var end = 0;
        for (var line : block.lines()) {
            for (var run : line.textRuns()) {
                if (run.sourceBlockIndex() == blockIndex) {
                    end = Math.max(end, run.sourceEnd());
                }
            }
        }
        return end;
    }

    private static void validateRange(LaidOutDocument document, DocumentRange range) {
        if (range.start().blockIndex() < 0 || range.end().blockIndex() >= document.blocks().size()) {
            throw new IllegalArgumentException("range block index is outside the laid-out document.");
        }
        for (var blockIndex = range.start().blockIndex(); blockIndex <= range.end().blockIndex(); blockIndex++) {
            var block = document.blocks().get(blockIndex);
            if (block.kind() != LaidOutBlockKind.PARAGRAPH && block.kind() != LaidOutBlockKind.HEADING) {
                throw new IllegalArgumentException("Selection geometry cannot cross unsupported blocks.");
            }
        }
    }
}
