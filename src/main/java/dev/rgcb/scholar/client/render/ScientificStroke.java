package dev.rgcb.scholar.client.render;

import net.minecraft.client.gui.GuiGraphics;

/** Subpixel coverage for document-only scientific strokes; no global graphics state changes. */
final class ScientificStroke {
    private static final int SAMPLES_PER_UNIT = 2;

    private ScientificStroke() {
    }

    static void draw(GuiGraphics graphics, double x1, double y1, double x2, double y2, int thickness, int color) {
        draw(graphics, x1, y1, x2, y2, thickness, color,
                Integer.MIN_VALUE / 4, Integer.MIN_VALUE / 4, Integer.MAX_VALUE / 4, Integer.MAX_VALUE / 4);
    }

    static void draw(GuiGraphics graphics, double x1, double y1, double x2, double y2, int thickness, int color,
                     int clipLeft, int clipTop, int clipRight, int clipBottom) {
        if (thickness < 1) {
            throw new IllegalArgumentException("Stroke thickness must be positive");
        }
        if (x1 == x2 || y1 == y2) {
            var left = (int) Math.round(Math.min(x1, x2));
            var top = (int) Math.round(Math.min(y1, y2));
            var right = (int) Math.round(Math.max(x1, x2)) + thickness;
            var bottom = (int) Math.round(Math.max(y1, y2)) + thickness;
            left = Math.max(left, clipLeft);
            top = Math.max(top, clipTop);
            right = Math.min(right, clipRight);
            bottom = Math.min(bottom, clipBottom);
            if (left < right && top < bottom) graphics.fill(left, top, right, bottom, color);
            return;
        }
        var ax = x1 * SAMPLES_PER_UNIT + 1;
        var ay = y1 * SAMPLES_PER_UNIT + 1;
        var bx = x2 * SAMPLES_PER_UNIT + 1;
        var by = y2 * SAMPLES_PER_UNIT + 1;
        var radius = (double) thickness * SAMPLES_PER_UNIT / 2;
        var left = (int) Math.floor(Math.min(ax, bx) - radius - 1);
        var top = (int) Math.floor(Math.min(ay, by) - radius - 1);
        var right = (int) Math.ceil(Math.max(ax, bx) + radius + 1);
        var bottom = (int) Math.ceil(Math.max(ay, by) + radius + 1);

        graphics.pose().pushPose();
        graphics.pose().scale(1.0f / SAMPLES_PER_UNIT, 1.0f / SAMPLES_PER_UNIT, 1.0f);
        try {
            for (var py = top; py < bottom; py++) {
                for (var px = left; px < right; px++) {
                    if (px < clipLeft * SAMPLES_PER_UNIT || px >= clipRight * SAMPLES_PER_UNIT
                            || py < clipTop * SAMPLES_PER_UNIT || py >= clipBottom * SAMPLES_PER_UNIT) continue;
                    var coverage = coverage(px + 0.5, py + 0.5, ax, ay, bx, by, radius);
                    if (coverage <= 0) continue;
                    var alpha = Math.round(((color >>> 24) & 0xFF) * coverage);
                    if (alpha > 0) graphics.fill(px, py, px + 1, py + 1, (alpha << 24) | (color & 0xFFFFFF));
                }
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    static float coverage(double px, double py, double ax, double ay, double bx, double by, double radius) {
        var dx = bx - ax;
        var dy = by - ay;
        var lengthSquared = dx * dx + dy * dy;
        var t = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / lengthSquared));
        var distance = Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
        return (float) Math.max(0, Math.min(1, radius + 0.5 - distance));
    }
}
