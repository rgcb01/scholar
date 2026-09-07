package dev.rgcb.scholar.math.editor;

import dev.rgcb.scholar.math.layout.LaidOutMath;
import dev.rgcb.scholar.math.layout.MathBox;
import dev.rgcb.scholar.math.layout.MathGlyphRun;
import dev.rgcb.scholar.math.layout.MathTextKind;
import dev.rgcb.scholar.math.layout.MathTextMeasurer;
import java.util.Objects;

public final class MathHitTester {
    public MathPosition hit(LaidOutMath math, int x, int y, MathTextMeasurer measurer) {
        Objects.requireNonNull(math, "math");
        Objects.requireNonNull(measurer, "measurer");
        return hitBox(math.root(), 0, 0, x, y, measurer);
    }

    private MathPosition hitBox(MathBox box, int boxX, int baselineY, int x, int y, MathTextMeasurer measurer) {
        for (var child : box.children()) {
            var childX = boxX + child.x();
            var childBaseline = baselineY + child.baselineOffset();
            if (x >= childX && x <= childX + Math.max(1, child.box().width())
                    && y >= childBaseline - child.box().ascent()
                    && y <= childBaseline + child.box().descent()) {
                return hitBox(child.box(), childX, childBaseline, x, y, measurer);
            }
        }
        if (!box.primitives().isEmpty()) {
            var glyph = box.primitives().stream()
                    .filter(MathGlyphRun.class::isInstance)
                    .map(MathGlyphRun.class::cast)
                    .findFirst()
                    .orElse(null);
            if (glyph != null && box.sourcePath().isPresent()) {
                var sourcePath = box.sourcePath().orElseThrow();
                if (glyph.kind() == MathTextKind.NUMBER || glyph.kind() == MathTextKind.IDENTIFIER) {
                    return tokenHit(sourcePath, glyph, boxX + glyph.x(), x, measurer);
                }
                return atomicGlyphHit(sourcePath, box.width(), boxX, x);
            }
        }
        var path = box.sourcePath().orElse(MathPath.ROOT);
        var childOffset = nearestChildOffset(box, boxX, x);
        return new MathSequencePosition(path, childOffset);
    }

    private MathPosition atomicGlyphHit(MathPath sourcePath, int width, int boxX, int x) {
        var virtualToken = sourcePath.segments().isEmpty() || !(sourcePath.last() instanceof SequenceChild);
        var parent = virtualToken ? new MathSequencePosition(sourcePath, 0) : parentPosition(sourcePath);
        var midpoint = boxX + width / 2;
        return x <= midpoint
                ? new MathSequencePosition(parent.sequencePath(), parent.childOffset())
                : new MathSequencePosition(parent.sequencePath(), parent.childOffset() + 1);
    }

    private MathPosition tokenHit(MathPath tokenPath, MathGlyphRun glyph, int glyphX, int x, MathTextMeasurer measurer) {
        var length = MathTextBoundary.characterCount(glyph.content());
        var bestOffset = 0;
        var bestDistance = Math.abs(x - glyphX);
        for (var offset = 1; offset <= length; offset++) {
            var prefix = MathTextBoundary.substring(glyph.content(), 0, offset);
            var boundaryX = glyphX + MathCaretGeometryResolver.scaled(measurer.measureText(prefix, glyph.kind()).width(), glyph.scale());
            var distance = Math.abs(x - boundaryX);
            if (distance <= bestDistance) {
                bestDistance = distance;
                bestOffset = offset;
            }
        }
        var virtualToken = tokenPath.segments().isEmpty() || !(tokenPath.last() instanceof SequenceChild);
        var parent = virtualToken ? new MathSequencePosition(tokenPath, 0) : parentPosition(tokenPath);
        if (bestOffset == 0) {
            return new MathSequencePosition(parent.sequencePath(), parent.childOffset());
        }
        if (bestOffset == length) {
            return new MathSequencePosition(parent.sequencePath(), parent.childOffset() + 1);
        }
        return new MathTokenPosition(virtualToken ? tokenPath.append(new SequenceChild(0)) : tokenPath, bestOffset);
    }

    private static int nearestChildOffset(MathBox box, int boxX, int x) {
        var bestOffset = 0;
        var bestDistance = Math.abs(x - boxX);
        for (var index = 0; index < box.children().size(); index++) {
            var child = box.children().get(index);
            var left = boxX + child.x();
            var right = left + child.box().width();
            var leftDistance = Math.abs(x - left);
            if (leftDistance < bestDistance) {
                bestDistance = leftDistance;
                bestOffset = index;
            }
            var rightDistance = Math.abs(x - right);
            if (rightDistance <= bestDistance) {
                bestDistance = rightDistance;
                bestOffset = index + 1;
            }
        }
        return bestOffset;
    }

    private static MathSequencePosition parentPosition(MathPath tokenPath) {
        var last = tokenPath.last();
        if (!(last instanceof SequenceChild child)) {
            throw new IllegalArgumentException("Token path must end in SequenceChild.");
        }
        return new MathSequencePosition(tokenPath.parent(), child.index());
    }
}
