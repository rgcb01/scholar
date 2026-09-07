package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.layout.LaidOutLine;
import dev.rgcb.scholar.layout.LaidOutTable;
import dev.rgcb.scholar.layout.LaidOutTableCell;
import dev.rgcb.scholar.layout.LaidOutText;
import dev.rgcb.scholar.layout.TextMeasurer;
import java.util.Objects;
import java.util.Optional;

public final class TableHitTester {
    public Optional<TableCellHit> hit(LaidOutTable table, int x, int y, TextMeasurer textMeasurer) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        for (var row : table.rows()) {
            for (var cell : row.cells()) {
                var hit = hit(cell, x, y, textMeasurer);
                if (hit.isPresent()) {
                    return hit;
                }
            }
        }
        return Optional.empty();
    }

    public Optional<TableCellHit> hit(LaidOutTableCell cell, int x, int y, TextMeasurer textMeasurer) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        if (!contains(cell, x, y)) {
            return Optional.empty();
        }
        return Optional.of(new TableCellHit(
                new TableCellCoordinate(cell.rowIndex(), cell.columnIndex()),
                hitOffset(cell, x, y, textMeasurer)));
    }

    private static int hitOffset(LaidOutTableCell cell, int x, int y, TextMeasurer textMeasurer) {
        if (cell.lines().isEmpty()) {
            return 0;
        }
        var firstLine = cell.lines().get(0);
        var lastLine = cell.lines().get(cell.lines().size() - 1);
        if (y < firstLine.y()) {
            return lineStart(firstLine);
        }
        if (y >= lastLine.y() + lastLine.height()) {
            return lineEnd(lastLine);
        }

        var line = nearestLine(cell, y);
        if (line.textRuns().isEmpty()) {
            return 0;
        }
        var firstRun = line.textRuns().get(0);
        var lastRun = line.textRuns().get(line.textRuns().size() - 1);
        if (x <= firstRun.x()) {
            return firstRun.sourceStart();
        }
        if (x >= lastRun.x() + lastRun.width()) {
            return lastRun.sourceEnd();
        }

        LaidOutText previousRun = null;
        for (var run : line.textRuns()) {
            if (x >= run.x() && x <= run.x() + run.width()) {
                return nearestBoundary(run, x, textMeasurer);
            }
            if (previousRun != null && x < run.x()) {
                var previousEnd = previousRun.x() + previousRun.width();
                return x - previousEnd >= run.x() - x ? run.sourceStart() : previousRun.sourceEnd();
            }
            previousRun = run;
        }
        return lastRun.sourceEnd();
    }

    private static boolean contains(LaidOutTableCell cell, int x, int y) {
        return x >= cell.x()
                && x <= cell.x() + Math.max(1, cell.width())
                && y >= cell.y()
                && y <= cell.y() + Math.max(1, cell.height());
    }

    private static LaidOutLine nearestLine(LaidOutTableCell cell, int y) {
        var best = cell.lines().get(0);
        var bestDistance = Integer.MAX_VALUE;
        for (var line : cell.lines()) {
            if (y >= line.y() && y < line.y() + line.height()) {
                return line;
            }
            var distance = y < line.y() ? line.y() - y : y - (line.y() + line.height());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = line;
            }
        }
        return best;
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

    private static int lineStart(LaidOutLine line) {
        return line.textRuns().isEmpty() ? 0 : line.textRuns().get(0).sourceStart();
    }

    private static int lineEnd(LaidOutLine line) {
        return line.textRuns().isEmpty() ? 0 : line.textRuns().get(line.textRuns().size() - 1).sourceEnd();
    }
}
