package dev.rgcb.scholar.client.render;

import dev.rgcb.scholar.math.layout.LaidOutMath;
import dev.rgcb.scholar.math.layout.MathBox;
import dev.rgcb.scholar.math.layout.MathGlyphRun;
import dev.rgcb.scholar.math.layout.MathHorizontalRule;
import dev.rgcb.scholar.math.layout.MathLineSegment;
import net.minecraft.client.gui.GuiGraphics;

public final class MinecraftMathRenderer {
    private final MinecraftTypographyResolver typographyResolver;

    public MinecraftMathRenderer(MinecraftTypographyResolver typographyResolver) {
        this.typographyResolver = typographyResolver;
    }

    public void render(GuiGraphics graphics, LaidOutMath math, int x, int baselineY) {
        renderBox(graphics, math.root(), x, baselineY);
    }

    private void renderBox(GuiGraphics graphics, MathBox box, int x, int baselineY) {
        for (var primitive : box.primitives()) {
            if (primitive instanceof MathGlyphRun glyph) {
                var resolved = typographyResolver.resolveMath(glyph.kind());
                var drawX = x + glyph.x();
                var drawY = baselineY + glyph.baselineOffset() - glyph.ascent();
                typographyResolver.drawDocumentString(graphics,
                        typographyResolver.component(glyph.content(), resolved), drawX, drawY,
                        resolved.color(), (float) glyph.scale());
            } else if (primitive instanceof MathHorizontalRule rule) {
                var resolved = typographyResolver.resolveMath(dev.rgcb.scholar.math.layout.MathTextKind.OPERATOR);
                graphics.fill(
                        x + rule.x(),
                        baselineY + rule.y(),
                        x + rule.x() + rule.width(),
                        baselineY + rule.y() + rule.thickness(),
                        resolved.color());
            } else if (primitive instanceof MathLineSegment line) {
                var resolved = typographyResolver.resolveMath(dev.rgcb.scholar.math.layout.MathTextKind.OPERATOR);
                ScientificStroke.draw(graphics, x + line.x1(), baselineY + line.y1(),
                        x + line.x2(), baselineY + line.y2(), line.thickness(), resolved.color());
            } else {
                throw new IllegalArgumentException("Unsupported math primitive: " + primitive.getClass().getName());
            }
        }

        for (var child : box.children()) {
            renderBox(graphics, child.box(), x + child.x(), baselineY + child.baselineOffset());
        }
    }

}
