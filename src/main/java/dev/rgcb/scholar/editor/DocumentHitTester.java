package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.layout.LaidOutBlock;
import dev.rgcb.scholar.layout.LaidOutBlockKind;
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.layout.LaidOutLine;
import dev.rgcb.scholar.layout.LaidOutText;
import dev.rgcb.scholar.layout.TextMeasurer;
import java.util.Objects;
import java.util.Optional;

public final class DocumentHitTester {
    public Optional<DocumentPosition> hitTest(LaidOutDocument document, int x, int y, TextMeasurer textMeasurer) {
        var hit = hit(document, x, y, textMeasurer);
        return hit.kind() == DocumentHit.Kind.TEXT ? hit.position() : Optional.empty();
    }

    public DocumentHit hit(LaidOutDocument document, int x, int y, TextMeasurer textMeasurer) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        for (var index = 0; index < document.blocks().size(); index++) {
            var block = document.blocks().get(index);
            if (!isEditableTextBlock(block) && contains(block, x, y)) {
                return DocumentHit.block(index);
            }
        }
        var editableBlockIndex = nearestEditableBlockIndex(document, x, y);
        if (editableBlockIndex.isEmpty()) {
            return DocumentHit.none();
        }
        return hitTestInBlock(document, editableBlockIndex.get(), x, y, textMeasurer)
                .map(DocumentHit::text)
                .orElseGet(DocumentHit::none);
    }

    public Optional<DocumentPosition> hitTestInBlock(LaidOutDocument document, int blockIndex, int x, int y, TextMeasurer textMeasurer) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        if (blockIndex < 0 || blockIndex >= document.blocks().size()) {
            return Optional.empty();
        }

        var block = document.blocks().get(blockIndex);
        if (!isEditableTextBlock(block)) {
            return Optional.empty();
        }
        if (block.lines().isEmpty()) {
            return Optional.of(new DocumentPosition(blockIndex, 0));
        }

        if (y < block.y()) {
            return Optional.of(new DocumentPosition(blockIndex, paragraphStart(block, blockIndex)));
        }
        if (y >= block.y() + block.height()) {
            return Optional.of(new DocumentPosition(blockIndex, paragraphEnd(block, blockIndex)));
        }

        var line = nearestLine(block, x, y);
        if (line.textRuns().isEmpty()) {
            return Optional.of(new DocumentPosition(blockIndex, lineStart(line, blockIndex)));
        }

        var firstRun = firstSourceRun(line, blockIndex);
        var lastRun = lastSourceRun(line, blockIndex);
        if (firstRun == null || lastRun == null) {
            return Optional.of(new DocumentPosition(blockIndex, nearestBoundaryForDisplayOnlyLine(block, line, blockIndex)));
        }

        if (x <= firstRun.x()) {
            return Optional.of(new DocumentPosition(blockIndex, firstRun.sourceStart()));
        }
        if (x >= lastRun.x() + lastRun.width()) {
            return Optional.of(new DocumentPosition(blockIndex, lastRun.sourceEnd()));
        }

        LaidOutText previousRun = null;
        for (var run : line.textRuns()) {
            if (run.sourceBlockIndex() != blockIndex) {
                continue;
            }
            if (x >= run.x() && x <= run.x() + run.width()) {
                return Optional.of(new DocumentPosition(blockIndex, nearestBoundary(run, x, textMeasurer)));
            }
            if (previousRun != null && x < run.x()) {
                var previousEnd = previousRun.x() + previousRun.width();
                var chooseNext = x - previousEnd >= run.x() - x;
                return Optional.of(new DocumentPosition(blockIndex, chooseNext ? run.sourceStart() : previousRun.sourceEnd()));
            }
            previousRun = run;
        }

        return Optional.of(new DocumentPosition(blockIndex, lastRun.sourceEnd()));
    }

    private static Optional<Integer> nearestEditableBlockIndex(LaidOutDocument document, int x, int y) {
        var bestIndex = -1;
        var bestDistance = Long.MAX_VALUE;
        var verticallyAligned = false;
        for (var index = 0; index < document.blocks().size(); index++) {
            var block = document.blocks().get(index);
            if (!isEditableTextBlock(block)) {
                continue;
            }
            var aligned = block.lines().stream().anyMatch(line -> containsY(line, y));
            var distance = block.lines().isEmpty() ? lineDistance(x, y, block.x(), block.y(), block.width(), block.height())
                    : block.lines().stream().filter(line -> !aligned || containsY(line, y))
                            .mapToLong(line -> lineDistance(x, y, line.x(), line.y(), line.width(), line.height()))
                            .min().orElse(Long.MAX_VALUE);
            if (aligned && !verticallyAligned || aligned == verticallyAligned && distance < bestDistance) {
                verticallyAligned = aligned;
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex == -1 ? Optional.empty() : Optional.of(bestIndex);
    }

    private static boolean isEditableTextBlock(LaidOutBlock block) {
        return block.kind() == LaidOutBlockKind.PARAGRAPH || block.kind() == LaidOutBlockKind.HEADING;
    }

    private static boolean contains(LaidOutBlock block, int x, int y) {
        return x >= block.x()
                && x <= block.x() + Math.max(1, block.width())
                && y >= block.y()
                && y <= block.y() + Math.max(1, block.height());
    }

    private static LaidOutLine nearestLine(LaidOutBlock block, int x, int y) {
        var bestLine = block.lines().get(0);
        var bestDistance = Long.MAX_VALUE;
        var verticallyAligned = false;
        for (var line : block.lines()) {
            var aligned = containsY(line, y);
            var distance = lineDistance(x, y, line.x(), line.y(), line.width(), line.height());
            if (aligned && !verticallyAligned || aligned == verticallyAligned && distance < bestDistance) {
                verticallyAligned = aligned;
                bestDistance = distance;
                bestLine = line;
            }
        }
        return bestLine;
    }

    private static boolean containsY(LaidOutLine line, int y) {
        return y >= line.y() && y < line.y() + line.height();
    }

    private static long lineDistance(int x, int y, int left, int top, int width, int height) {
        var dx = x < left ? (long) left - x : Math.max(0L, (long) x - left - Math.max(1, width));
        var dy = y < top ? (long) top - y : Math.max(0L, (long) y - top - Math.max(1, height) + 1);
        return dx * dx + dy * dy;
    }

    private static int nearestBoundary(LaidOutText run, int x, TextMeasurer textMeasurer) {
        if (run.atomic()) {
            return x - run.x() < run.width() / 2.0 ? run.sourceStart() : run.sourceEnd();
        }
        var bestOffset = 0;
        var bestDistance = Math.abs(x - run.x());
        var characters = TextBoundary.characterCount(run.text());
        for (var offset = 1; offset <= characters; offset++) {
            var prefix = TextBoundary.substring(run.text(), 0, offset);
            var boundaryX = run.x() + textMeasurer.measureWidth(prefix, run.style());
            var distance = Math.abs(x - boundaryX);
            if (distance <= bestDistance) {
                bestDistance = distance;
                bestOffset = offset;
            }
        }
        return run.sourceStart() + bestOffset;
    }

    private static int lineStart(LaidOutLine line, int blockIndex) {
        var first = firstSourceRun(line, blockIndex);
        return first == null ? 0 : first.sourceStart();
    }

    private static int paragraphStart(LaidOutBlock block, int blockIndex) {
        for (var line : block.lines()) {
            var first = firstSourceRun(line, blockIndex);
            if (first != null) {
                return first.sourceStart();
            }
        }
        return 0;
    }

    private static int paragraphEnd(LaidOutBlock block, int blockIndex) {
        LaidOutText last = null;
        for (var line : block.lines()) {
            var run = lastSourceRun(line, blockIndex);
            if (run != null) {
                last = run;
            }
        }
        return last == null ? 0 : last.sourceEnd();
    }

    private static int nearestBoundaryForDisplayOnlyLine(LaidOutBlock block, LaidOutLine targetLine, int blockIndex) {
        LaidOutText last = null;
        for (var line : block.lines()) {
            var first = firstSourceRun(line, blockIndex);
            if (first != null) {
                if (targetLine.y() <= line.y()) {
                    return first.sourceStart();
                }
                last = lastSourceRun(line, blockIndex);
            }
        }
        return last == null ? 0 : last.sourceEnd();
    }

    private static LaidOutText firstSourceRun(LaidOutLine line, int blockIndex) {
        for (var run : line.textRuns()) {
            if (run.sourceBlockIndex() == blockIndex) {
                return run;
            }
        }
        return null;
    }

    private static LaidOutText lastSourceRun(LaidOutLine line, int blockIndex) {
        LaidOutText last = null;
        for (var run : line.textRuns()) {
            if (run.sourceBlockIndex() == blockIndex) {
                last = run;
            }
        }
        return last;
    }
}
