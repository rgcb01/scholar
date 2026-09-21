package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.layout.LaidOutText;
import dev.rgcb.scholar.layout.TextMeasurer;
import java.util.Objects;

public final class CaretGeometryResolver {
    public CaretGeometry resolve(DocumentPosition position, LaidOutDocument document, TextMeasurer textMeasurer) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        if (position.blockIndex() < 0 || position.blockIndex() >= document.blocks().size()) {
            throw new IllegalArgumentException("blockIndex is outside the laid-out document.");
        }

        var block = document.blocks().get(position.blockIndex());
        if (block.lines().isEmpty()) {
            return new CaretGeometry(block.x(), block.y(), block.height());
        }
        if (position.characterOffset() == 0 && block.lines().get(0).textRuns().isEmpty()) {
            var line = block.lines().get(0);
            return new CaretGeometry(block.x() + line.x(), line.y(), line.height());
        }

        LaidOutText previousRun = null;
        CaretGeometry atomicEnd = null;
        var previousLineY = block.lines().get(0).y();
        var previousLineHeight = block.lines().get(0).height();
        for (var line : block.lines()) {
            for (var run : line.textRuns()) {
                if (run.sourceBlockIndex() != position.blockIndex()) {
                    continue;
                }
                if (position.characterOffset() >= run.sourceStart() && position.characterOffset() <= run.sourceEnd()) {
                    if (run.atomic()) {
                        if (position.characterOffset() == run.sourceStart()) {
                            return new CaretGeometry(run.x(), line.y(), line.height());
                        }
                        atomicEnd = new CaretGeometry(run.x() + run.width(), line.y(), line.height());
                        continue;
                    }
                    var prefix = TextBoundary.substring(run.text(), 0, position.characterOffset() - run.sourceStart());
                    var x = line.x() + run.x() + textMeasurer.measureWidth(prefix, run.style());
                    return new CaretGeometry(x, line.y(), line.height());
                }
                previousRun = run;
                previousLineY = line.y();
                previousLineHeight = line.height();
            }
        }

        if (atomicEnd != null) {
            return atomicEnd;
        }
        if (previousRun != null && position.characterOffset() == previousRun.sourceEnd()) {
            return new CaretGeometry(
                    previousRun.x() + previousRun.width(),
                    previousLineY,
                    previousLineHeight);
        }
        throw new IllegalArgumentException("No laid-out text range contains the requested caret position.");
    }
}
