package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.layout.LaidOutTableCell;
import dev.rgcb.scholar.layout.TextMeasurer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TableSelectionGeometryResolver {
    public List<SelectionRect> resolve(LaidOutTableCell cell, TableCellTextSelection selection, TextMeasurer textMeasurer) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        if (selection.isCaret()) {
            return List.of();
        }

        var rects = new ArrayList<SelectionRect>();
        for (var line : cell.lines()) {
            for (var run : line.textRuns()) {
                var start = Math.max(selection.startOffset(), run.sourceStart());
                var end = Math.min(selection.endOffset(), run.sourceEnd());
                if (start >= end) {
                    continue;
                }
                var prefix = TextBoundary.substring(run.text(), 0, start - run.sourceStart());
                var selected = TextBoundary.substring(run.text(), start - run.sourceStart(), end - run.sourceStart());
                var x = run.x() + textMeasurer.measureWidth(prefix, run.style());
                var width = textMeasurer.measureWidth(selected, run.style());
                if (width > 0) {
                    rects.add(new SelectionRect(x, line.y(), width, line.height()));
                }
            }
        }
        return List.copyOf(rects);
    }
}
