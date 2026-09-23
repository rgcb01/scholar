package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.layout.LaidOutTableCell;
import dev.rgcb.scholar.layout.LaidOutText;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.Objects;

public final class TableCaretGeometryResolver {
    public CaretGeometry resolve(LaidOutTableCell cell, int characterOffset, TextMeasurer textMeasurer) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        if (cell.lines().isEmpty()) {
            return caret(cell.contentX(), cell.contentY(), cell.height(), TextStyle.paragraph(), textMeasurer);
        }
        if (characterOffset == 0 && cell.lines().get(0).textRuns().isEmpty()) {
            var line = cell.lines().get(0);
            return caret(cell.contentX(), line.y(), line.height(), TextStyle.paragraph(), textMeasurer);
        }

        LaidOutText previousRun = null;
        var previousLineY = cell.lines().get(0).y();
        var previousLineHeight = cell.lines().get(0).height();
        for (var line : cell.lines()) {
            for (var run : line.textRuns()) {
                if (characterOffset >= run.sourceStart() && characterOffset <= run.sourceEnd()) {
                    var prefix = TextBoundary.substring(run.text(), 0, characterOffset - run.sourceStart());
                    return caret(run.x() + textMeasurer.measureWidth(prefix, run.style()), line.y(), line.height(), run.style(), textMeasurer);
                }
                previousRun = run;
                previousLineY = line.y();
                previousLineHeight = line.height();
            }
        }
        if (previousRun != null && characterOffset == previousRun.sourceEnd()) {
            return caret(previousRun.x() + previousRun.width(), previousLineY, previousLineHeight, previousRun.style(), textMeasurer);
        }
        throw new IllegalArgumentException("No laid-out cell text range contains the requested caret position.");
    }

    private static CaretGeometry caret(int x, int lineY, int lineHeight, TextStyle style, TextMeasurer measurer) {
        var metrics = measurer.caretMetrics(style, lineHeight);
        return new CaretGeometry(x, lineY + metrics.topInset(), metrics.height());
    }
}
