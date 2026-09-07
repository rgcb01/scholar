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
        var editableBlockIndex = nearestEditableBlockIndex(document, y);
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

        var firstLine = block.lines().get(0);
        var lastLine = block.lines().get(block.lines().size() - 1);
        if (y < firstLine.y()) {
            return Optional.of(new DocumentPosition(blockIndex, paragraphStart(block, blockIndex)));
        }
        if (y >= lastLine.y() + lastLine.height()) {
            return Optional.of(new DocumentPosition(blockIndex, paragraphEnd(block, blockIndex)));
        }

        var line = nearestLine(block, y);
        if (line.textRuns().isEmpty()) {
            return Optional.of(new DocumentPosition(blockIndex, lineStart(line, blockIndex)));
        }

        var firstRun = firstSourceRun(line, blockIndex);
        var lastRun = lastSourceRun(line, blockIndex);
        if (firstRun == null || lastRun == null) {
            return Optional.empty();
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

    private static Optional<Integer> nearestEditableBlockIndex(LaidOutDocument document, int y) {
        var bestIndex = -1;
        var bestDistance = Integer.MAX_VALUE;
        for (var index = 0; index < document.blocks().size(); index++) {
            var block = document.blocks().get(index);
            if (!isEditableTextBlock(block)) {
                continue;
            }
            if (y >= block.y() && y <= block.y() + block.height()) {
                return Optional.of(index);
            }
            var distance = y < block.y() ? block.y() - y : y - (block.y() + block.height());
            if (distance < bestDistance) {
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

    private static LaidOutLine nearestLine(LaidOutBlock block, int y) {
        var bestLine = block.lines().get(0);
        var bestDistance = Integer.MAX_VALUE;
        for (var line : block.lines()) {
            if (y >= line.y() && y < line.y() + line.height()) {
                return line;
            }
            var distance = y < line.y() ? line.y() - y : y - (line.y() + line.height());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestLine = line;
            }
        }
        return bestLine;
    }

    private static int nearestBoundary(LaidOutText run, int x, TextMeasurer textMeasurer) {
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
