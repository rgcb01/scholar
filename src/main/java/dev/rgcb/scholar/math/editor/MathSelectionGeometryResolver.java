package dev.rgcb.scholar.math.editor;

import dev.rgcb.scholar.math.MathExpression;
import dev.rgcb.scholar.math.layout.LaidOutMath;
import dev.rgcb.scholar.math.layout.MathBox;
import dev.rgcb.scholar.math.layout.MathGlyphRun;
import dev.rgcb.scholar.math.layout.MathTextMeasurer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MathSelectionGeometryResolver {
    private final MathExpressionEditor editor = new MathExpressionEditor();

    public List<MathSelectionRect> resolve(
            MathExpression expression,
            MathSelection selection,
            LaidOutMath math,
            MathTextMeasurer measurer
    ) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(math, "math");
        Objects.requireNonNull(measurer, "measurer");
        if (!(selection instanceof MathRangeSelection rangeSelection)) {
            return List.of();
        }
        var range = editor.normalizeRange(expression, rangeSelection);
        var wholeChild = wholeChildRect(range, math.root());
        if (wholeChild != null) {
            return List.of(wholeChild);
        }

        var positions = editor.positions(expression);
        var start = positions.indexOf(range.start());
        var end = positions.indexOf(range.end());
        if (start < 0 || end < 0 || start >= end) {
            return List.of();
        }
        var rects = new ArrayList<MathSelectionRect>();
        for (var index = start; index < end; index++) {
            var current = positions.get(index);
            var next = positions.get(index + 1);
            if (current instanceof MathTokenPosition currentToken
                    && next instanceof MathTokenPosition nextToken
                    && currentToken.tokenPath().equals(nextToken.tokenPath())) {
                tokenRect(currentToken.tokenPath(), currentToken.characterOffset(), nextToken.characterOffset(), math.root(), measurer)
                        .ifPresent(rects::add);
            } else if (current instanceof MathSequencePosition sequenceStart
                    && next instanceof MathSequencePosition sequenceEnd
                    && sequenceStart.sequencePath().equals(sequenceEnd.sequencePath())
                    && sequenceEnd.childOffset() == sequenceStart.childOffset() + 1) {
                childRect(sequenceStart.sequencePath().append(new SequenceChild(sequenceStart.childOffset())), math.root())
                        .ifPresent(rects::add);
            }
        }
        return coalesced(rects);
    }

    private MathSelectionRect wholeChildRect(MathRange range, MathBox root) {
        if (range.start() instanceof MathSequencePosition start
                && range.end() instanceof MathSequencePosition end
                && start.sequencePath().equals(end.sequencePath())
                && end.childOffset() == start.childOffset() + 1) {
            return childRect(start.sequencePath().append(new SequenceChild(start.childOffset())), root).orElse(null);
        }
        return null;
    }

    private java.util.Optional<MathSelectionRect> childRect(MathPath path, MathBox root) {
        var child = MathCaretGeometryResolver.findBox(root, 0, 0, path);
        if (child == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new MathSelectionRect(
                child.x(),
                child.baselineY() - child.box().ascent(),
                child.box().width(),
                child.box().ascent() + child.box().descent()));
    }

    private java.util.Optional<MathSelectionRect> tokenRect(
            MathPath path,
            int startOffset,
            int endOffset,
            MathBox root,
            MathTextMeasurer measurer
    ) {
        var token = MathCaretGeometryResolver.findBox(root, 0, 0, path);
        if (token == null) {
            return java.util.Optional.empty();
        }
        var glyph = token.box().primitives().stream()
                .filter(MathGlyphRun.class::isInstance)
                .map(MathGlyphRun.class::cast)
                .findFirst();
        if (glyph.isEmpty()) {
            return java.util.Optional.empty();
        }
        var run = glyph.orElseThrow();
        var prefix = MathTextBoundary.substring(run.content(), 0, startOffset);
        var selected = MathTextBoundary.substring(run.content(), startOffset, endOffset);
        var x = token.x() + run.x() + MathCaretGeometryResolver.scaled(measurer.measureText(prefix, run.kind()).width(), run.scale());
        var width = MathCaretGeometryResolver.scaled(measurer.measureText(selected, run.kind()).width(), run.scale());
        return java.util.Optional.of(new MathSelectionRect(
                x,
                token.baselineY() + run.baselineOffset() - run.ascent(),
                width,
                run.ascent() + token.box().descent()));
    }

    private static List<MathSelectionRect> coalesced(List<MathSelectionRect> rects) {
        if (rects.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<MathSelectionRect>();
        for (var rect : rects) {
            if (!result.isEmpty()) {
                var previous = result.get(result.size() - 1);
                if (previous.y() == rect.y()
                        && previous.height() == rect.height()
                        && previous.x() + previous.width() == rect.x()) {
                    result.set(result.size() - 1, new MathSelectionRect(
                            previous.x(),
                            previous.y(),
                            previous.width() + rect.width(),
                            previous.height()));
                    continue;
                }
            }
            result.add(rect);
        }
        return List.copyOf(result);
    }
}
