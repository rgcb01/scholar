package dev.rgcb.scholar.math.editor;

import dev.rgcb.scholar.math.layout.LaidOutMath;
import dev.rgcb.scholar.math.layout.MathBox;
import dev.rgcb.scholar.math.layout.MathGlyphRun;
import dev.rgcb.scholar.math.layout.MathTextMeasurer;
import java.util.Objects;

public final class MathCaretGeometryResolver {
    public MathCaretGeometry resolve(MathPosition position, LaidOutMath math, MathTextMeasurer measurer) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(math, "math");
        Objects.requireNonNull(measurer, "measurer");
        if (position instanceof MathSequencePosition sequencePosition) {
            return resolveSequencePosition(sequencePosition, math.root(), 0, 0, measurer);
        }
        if (position instanceof MathTokenPosition tokenPosition) {
            return resolveTokenPosition(tokenPosition, math.root(), 0, 0, measurer);
        }
        throw new IllegalArgumentException("Unsupported math position: " + position.getClass().getName());
    }

    private MathCaretGeometry resolveSequencePosition(
            MathSequencePosition position,
            MathBox box,
            int boxX,
            int baselineY,
            MathTextMeasurer measurer
    ) {
        var container = findBox(box, boxX, baselineY, position.sequencePath());
        if (container == null) {
            throw new IllegalArgumentException("No laid-out math box matches the requested sequence path.");
        }
        var virtualSingleChild = container.box().children().isEmpty() && !container.box().primitives().isEmpty();
        var childCount = virtualSingleChild ? 1 : container.box().children().size();
        if (position.childOffset() < 0 || position.childOffset() > childCount) {
            throw new IllegalArgumentException("childOffset is outside the laid-out sequence.");
        }
        var x = container.x();
        if (virtualSingleChild) {
            x += position.childOffset() == 0 ? 0 : container.box().width();
        } else if (position.childOffset() < container.box().children().size()) {
            x += container.box().children().get(position.childOffset()).x();
        } else {
            x += container.box().width();
        }
        var metrics = measurer.measureText("0", dev.rgcb.scholar.math.layout.MathTextKind.NUMBER);
        var ascent = Math.max(container.box().ascent(), metrics.ascent());
        var descent = Math.max(container.box().descent(), metrics.descent());
        return new MathCaretGeometry(x, container.baselineY(), ascent, descent);
    }

    private MathCaretGeometry resolveTokenPosition(
            MathTokenPosition position,
            MathBox box,
            int boxX,
            int baselineY,
            MathTextMeasurer measurer
    ) {
        var token = findBox(box, boxX, baselineY, position.tokenPath());
        if (token == null) {
            throw new IllegalArgumentException("No laid-out math box matches the requested token path.");
        }
        var glyph = token.box().primitives().stream()
                .filter(MathGlyphRun.class::isInstance)
                .map(MathGlyphRun.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Token path does not resolve to a glyph."));
        var length = MathTextBoundary.characterCount(glyph.content());
        if (position.characterOffset() <= 0 || position.characterOffset() >= length) {
            throw new IllegalArgumentException("MathTokenPosition is only valid strictly inside a token.");
        }
        var prefix = MathTextBoundary.substring(glyph.content(), 0, position.characterOffset());
        var x = token.x() + glyph.x() + scaled(measurer.measureText(prefix, glyph.kind()).width(), glyph.scale());
        return new MathCaretGeometry(x, token.baselineY(), glyph.ascent(), token.box().descent());
    }

    static PositionedBox findBox(MathBox box, int x, int baselineY, MathPath path) {
        var exact = findExactBox(box, x, baselineY, path);
        if (exact != null) {
            return exact;
        }
        if (!path.segments().isEmpty() && path.last() instanceof SequenceChild child && child.index() == 0) {
            var parent = findExactBox(box, x, baselineY, path.parent());
            if (parent != null && parent.box().children().isEmpty() && !parent.box().primitives().isEmpty()) {
                return parent;
            }
        }
        return null;
    }

    private static PositionedBox findExactBox(MathBox box, int x, int baselineY, MathPath path) {
        if (box.sourcePath().isPresent() && box.sourcePath().orElseThrow().equals(path)) {
            return new PositionedBox(box, x, baselineY);
        }
        for (var child : box.children()) {
            var found = findExactBox(child.box(), x + child.x(), baselineY + child.baselineOffset(), path);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    record PositionedBox(MathBox box, int x, int baselineY) {
    }

    static int scaled(int value, double scale) {
        return Math.round((float) (value * scale));
    }
}
