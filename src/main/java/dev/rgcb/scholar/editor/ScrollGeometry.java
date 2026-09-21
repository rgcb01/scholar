package dev.rgcb.scholar.editor;

/** Pure vertical viewport arithmetic; no semantic mutation or history. */
public final class ScrollGeometry {
    private ScrollGeometry() { }

    public static int clamp(int offset, int documentHeight, int viewportHeight) {
        return Math.max(0, Math.min(offset, Math.max(0, documentHeight - Math.max(0, viewportHeight))));
    }

    public static int reveal(int offset, int documentHeight, int viewportHeight, int top, int height) {
        var current = clamp(offset, documentHeight, viewportHeight);
        if (viewportHeight <= 0) { return current; }
        var bottom = (long) top + Math.max(0, height);
        var viewportBottom = (long) current + viewportHeight;
        // A tall selected object cannot fit. Keep the visible portion stable.
        if (height > viewportHeight && bottom > current && top < viewportBottom) { return current; }
        var desired = top < current ? top : bottom > viewportBottom ? bottom - viewportHeight : current;
        return clamp((int) Math.min(Integer.MAX_VALUE, Math.max(0L, desired)), documentHeight, viewportHeight);
    }
}
