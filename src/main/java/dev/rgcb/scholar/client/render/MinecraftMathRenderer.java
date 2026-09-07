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
                if (glyph.scale() == 1.0) {
                    graphics.drawString(
                            typographyResolver.fontFor(resolved.role()),
                            typographyResolver.component(glyph.content(), resolved),
                            drawX,
                            drawY,
                            resolved.color(),
                            false);
                } else {
                    graphics.pose().pushPose();
                    graphics.pose().translate(drawX, drawY, 0);
                    graphics.pose().scale((float) glyph.scale(), (float) glyph.scale(), 1.0F);
                    graphics.drawString(
                            typographyResolver.fontFor(resolved.role()),
                            typographyResolver.component(glyph.content(), resolved),
                            0,
                            0,
                            resolved.color(),
                            false);
                    graphics.pose().popPose();
                }
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
                drawLine(graphics, x + line.x1(), baselineY + line.y1(), x + line.x2(), baselineY + line.y2(), line.thickness(), resolved.color());
            } else {
                throw new IllegalArgumentException("Unsupported math primitive: " + primitive.getClass().getName());
            }
        }

        for (var child : box.children()) {
            renderBox(graphics, child.box(), x + child.x(), baselineY + child.baselineOffset());
        }
    }

    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int thickness, int color) {
        var dx = Math.abs(x2 - x1);
        var dy = Math.abs(y2 - y1);
        var sx = x1 < x2 ? 1 : -1;
        var sy = y1 < y2 ? 1 : -1;
        var error = dx - dy;
        var x = x1;
        var y = y1;

        while (true) {
            graphics.fill(x, y, x + thickness, y + thickness, color);
            if (x == x2 && y == y2) {
                break;
            }
            var doubledError = error * 2;
            if (doubledError > -dy) {
                error -= dy;
                x += sx;
            }
            if (doubledError < dx) {
                error += dx;
                y += sy;
            }
        }
    }
}
