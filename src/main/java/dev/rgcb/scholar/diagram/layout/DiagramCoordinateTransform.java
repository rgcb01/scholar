package dev.rgcb.scholar.diagram.layout;

import dev.rgcb.scholar.diagram.DiagramCanvas;
import java.util.Objects;

/** Uniform logical-canvas <-> document-space transform for one laid-out diagram. */
public record DiagramCoordinateTransform(
        DiagramCanvas canvas,
        double scale,
        int documentX,
        int documentY
) {
    public DiagramCoordinateTransform {
        canvas = Objects.requireNonNull(canvas, "canvas");
        if (!Double.isFinite(scale) || scale <= 0.0) {
            throw new IllegalArgumentException("scale must be finite and positive.");
        }
    }

    public double mapX(double logicalX) {
        return documentX + logicalX * scale;
    }

    public double mapY(double logicalY) {
        return documentY + logicalY * scale;
    }

    public double unmapX(double documentX) {
        return (documentX - this.documentX) / scale;
    }

    public double unmapY(double documentY) {
        return (documentY - this.documentY) / scale;
    }
}
