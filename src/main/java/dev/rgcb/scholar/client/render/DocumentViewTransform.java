package dev.rgcb.scholar.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.rgcb.scholar.editor.CaretGeometry;

/** Maps existing layout/GUI coordinates to the visible document viewport. */
public record DocumentViewTransform(int originX, int originY, double zoom) {
    public DocumentViewTransform {
        if (!Double.isFinite(zoom) || zoom <= 0) throw new IllegalArgumentException("zoom must be positive");
    }

    public double screenX(double logicalX) {
        return originX + (logicalX - originX) * zoom;
    }

    public double screenY(double logicalY) {
        return originY + (logicalY - originY) * zoom;
    }

    public double logicalX(double screenX) {
        return originX + (screenX - originX) / zoom;
    }

    public double logicalY(double screenY) {
        return originY + (screenY - originY) / zoom;
    }

    public void apply(PoseStack pose) {
        pose.translate(originX, originY, 0);
        pose.scale((float) zoom, (float) zoom, 1.0f);
        pose.translate(-originX, -originY, 0);
    }

    public ScreenClip clip(int left, int top, int right, int bottom) {
        return new ScreenClip((int) Math.floor(screenX(left)), (int) Math.floor(screenY(top)),
                (int) Math.ceil(screenX(right)), (int) Math.ceil(screenY(bottom)));
    }

    /** Final GUI-space rectangle; callers must draw this without the document zoom pose. */
    public ScreenClip caretRect(CaretGeometry caret, int viewportX, int viewportY, int scrollOffset) {
        var left = screenX(viewportX + caret.x());
        var top = screenY(viewportY + caret.y() - scrollOffset);
        var bottom = screenY(viewportY + caret.bottom() - scrollOffset);
        var x = (int) Math.round(left);
        var y = (int) Math.round(top);
        return new ScreenClip(x, y, x + 1, Math.max(y + 1, (int) Math.round(bottom)));
    }

    public record ScreenClip(int left, int top, int right, int bottom) {
    }
}
