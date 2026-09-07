package dev.rgcb.scholar.diagram.layout;

import dev.rgcb.scholar.diagram.DiagramCanvas;
import java.util.Objects;

/**
 * Transient view state for one embedded diagram workspace.
 *
 * <p>Zoom is expressed relative to Fit. Center coordinates are logical-canvas
 * coordinates. The layout engine clamps center coordinates so panning never loses
 * the authored canvas completely. This state is intentionally not part of the AST,
 * clipboard payload, or editor history.</p>
 */
public record DiagramViewport(double zoom, double centerX, double centerY) {
    public static final double MIN_ZOOM = 0.50;
    public static final double MAX_ZOOM = 6.00;

    public DiagramViewport {
        if (!Double.isFinite(zoom) || zoom < MIN_ZOOM || zoom > MAX_ZOOM) {
            throw new IllegalArgumentException(
                    "Diagram zoom must be finite and between " + MIN_ZOOM + " and " + MAX_ZOOM + ".");
        }
        if (!Double.isFinite(centerX) || !Double.isFinite(centerY)) {
            throw new IllegalArgumentException("Diagram viewport center must be finite.");
        }
    }

    public static DiagramViewport fit(DiagramCanvas canvas) {
        Objects.requireNonNull(canvas, "canvas");
        return new DiagramViewport(1.0, canvas.width() / 2.0, canvas.height() / 2.0);
    }

    public DiagramViewport withZoom(double replacement) {
        return new DiagramViewport(clampZoom(replacement), centerX, centerY);
    }

    public DiagramViewport withCenter(double x, double y) {
        return new DiagramViewport(zoom, x, y);
    }

    public static double clampZoom(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Diagram zoom must be finite.");
        }
        return Math.max(MIN_ZOOM, Math.min(value, MAX_ZOOM));
    }
}
