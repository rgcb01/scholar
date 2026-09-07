package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.layout.LaidOutLine;
import dev.rgcb.scholar.layout.LaidOutTableCell;
import dev.rgcb.scholar.layout.TextMeasurer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TableCellTextNavigator {
    public Optional<Integer> moveUp(LaidOutTableCell cell, int activeOffset, TextMeasurer textMeasurer, int preferredX) {
        return moveVertical(cell, activeOffset, textMeasurer, preferredX, true);
    }

    public Optional<Integer> moveDown(LaidOutTableCell cell, int activeOffset, TextMeasurer textMeasurer, int preferredX) {
        return moveVertical(cell, activeOffset, textMeasurer, preferredX, false);
    }

    public Optional<Integer> lineStart(LaidOutTableCell cell, int activeOffset) {
        return lineContaining(cell, activeOffset).map(TableCellTextNavigator::lineStart);
    }

    public Optional<Integer> lineEnd(LaidOutTableCell cell, int activeOffset) {
        return lineContaining(cell, activeOffset).map(TableCellTextNavigator::lineEnd);
    }

    private static Optional<Integer> moveVertical(LaidOutTableCell cell, int activeOffset, TextMeasurer textMeasurer, int preferredX, boolean up) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        var line = lineContaining(cell, activeOffset);
        if (line.isEmpty()) {
            return Optional.empty();
        }
        var lineIndex = line.orElseThrow().lineIndex();
        var targetIndex = up ? lineIndex - 1 : lineIndex + 1;
        if (targetIndex < 0 || targetIndex >= cell.lines().size()) {
            return Optional.of(activeOffset);
        }
        return Optional.of(closestOffset(cell.lines().get(targetIndex), textMeasurer, preferredX));
    }

    private static Optional<CellLine> lineContaining(LaidOutTableCell cell, int offset) {
        for (var index = 0; index < cell.lines().size(); index++) {
            var line = cell.lines().get(index);
            if (line.textRuns().isEmpty() && offset == 0) {
                return Optional.of(new CellLine(index, line));
            }
            for (var run : line.textRuns()) {
                if (offset >= run.sourceStart() && offset <= run.sourceEnd()) {
                    return Optional.of(new CellLine(index, line));
                }
            }
        }
        return Optional.empty();
    }

    private static int closestOffset(LaidOutLine line, TextMeasurer textMeasurer, int preferredX) {
        var candidates = new ArrayList<CaretCandidate>();
        if (line.textRuns().isEmpty()) {
            return 0;
        }
        for (var run : line.textRuns()) {
            var characters = TextBoundary.characterCount(run.text());
            for (var offset = 0; offset <= characters; offset++) {
                var prefix = TextBoundary.substring(run.text(), 0, offset);
                candidates.add(new CaretCandidate(run.sourceStart() + offset, run.x() + textMeasurer.measureWidth(prefix, run.style())));
            }
        }
        return candidates.stream()
                .min(Comparator
                        .comparingInt((CaretCandidate candidate) -> Math.abs(candidate.x() - preferredX))
                        .thenComparing(Comparator.comparingInt(CaretCandidate::offset).reversed()))
                .map(CaretCandidate::offset)
                .orElse(0);
    }

    private static int lineStart(CellLine line) {
        return line.line().textRuns().isEmpty() ? 0 : line.line().textRuns().get(0).sourceStart();
    }

    private static int lineEnd(CellLine line) {
        return line.line().textRuns().isEmpty() ? 0 : line.line().textRuns().get(line.line().textRuns().size() - 1).sourceEnd();
    }

    private record CellLine(int lineIndex, LaidOutLine line) {
    }

    private record CaretCandidate(int offset, int x) {
    }
}
