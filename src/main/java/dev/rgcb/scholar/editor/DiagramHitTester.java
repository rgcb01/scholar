package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.diagram.layout.LaidOutDiagram;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramLabel;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramPoint;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramPort;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramRect;
import dev.rgcb.scholar.electrical.layout.LaidOutElectricalCircle;
import dev.rgcb.scholar.electrical.layout.LaidOutElectricalComponent;
import dev.rgcb.scholar.electrical.layout.LaidOutElectricalLine;
import dev.rgcb.scholar.electrical.layout.LaidOutElectricalPolyline;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Pure-Java mapping from laid-out diagram geometry to semantic edit targets. */
public final class DiagramHitTester {
    public static final double CONNECTION_TOLERANCE = 4.0;
    public static final double ELECTRICAL_SYMBOL_TOLERANCE = 4.0;

    public Optional<DiagramEditTarget> hit(LaidOutDiagram diagram, int x, int y) {
        Objects.requireNonNull(diagram, "diagram");

        // Internal zoom/pan may place derived geometry outside the embedded
        // workspace. Off-workspace geometry is clipped visually and must not
        // remain interactive. The title lives above the workspace and remains
        // independently selectable.
        if (!diagram.workspaceBounds().contains(x, y)) {
            if (diagram.title().filter(label -> contains(label, x, y)).isPresent()) {
                return Optional.of(new DiagramPropertyTarget(DiagramProperty.TITLE));
            }
            return Optional.empty();
        }

        // Ports intentionally win over their owning element because their hit
        // regions overlap the element perimeter by design. Electrical terminals
        // use exactly the same generic DiagramPort target contract.
        for (var node : diagram.nodes()) {
            var hit = hitPort(node.ports(), x, y);
            if (hit.isPresent()) {
                return hit;
            }
        }
        for (var component : diagram.electricalComponents()) {
            var hit = hitPort(component.ports(), x, y);
            if (hit.isPresent()) {
                return hit;
            }
        }

        // Elements win over connections so a routed line behind an element never
        // steals an ordinary element click.
        for (var node : diagram.nodes()) {
            if (contains(node.x(), node.y(), node.width(), node.height(), x, y)) {
                return Optional.of(new DiagramElementTarget(node.elementIndex(), node.elementId()));
            }
        }
        for (var component : diagram.electricalComponents()) {
            if (hitElectricalComponent(component, x, y)) {
                return Optional.of(new DiagramElementTarget(component.elementIndex(), component.elementId()));
            }
        }
        for (var junction : diagram.electricalJunctions()) {
            if (Math.hypot(x - junction.centerX(), y - junction.centerY()) <= 3.0
                    || junction.netLabel().filter(label -> contains(label, x, y)).isPresent()) {
                return Optional.of(new DiagramElementTarget(junction.elementIndex(), junction.elementId()));
            }
        }
        for (var primitive : diagram.mechanicalPrimitives()) {
            var rect = primitive.bounds();
            if (contains(rect.x(), rect.y(), rect.width(), rect.height(), x, y)) {
                return Optional.of(new DiagramElementTarget(primitive.elementIndex(), primitive.elementId()));
            }
        }
        for (var symbol : diagram.mechanicalSymbols()) {
            var rect = symbol.bounds();
            if (contains(rect.x(), rect.y(), rect.width(), rect.height(), x, y)) return Optional.of(new DiagramElementTarget(symbol.elementIndex(), symbol.elementId()));
        }
        for (var reference : diagram.mechanicalPartReferences()) {
            var rect=reference.balloonBounds(); if(contains(rect.x(),rect.y(),rect.width(),rect.height(),x,y)||contains(reference.itemLabel(),x,y))return Optional.of(new DiagramElementTarget(reference.elementIndex(),reference.elementId()));
        }
        for (var annotation : diagram.mechanicalAnnotations()) {
            var rect=annotation.bounds();
            if(contains(rect.x(),rect.y(),rect.width(),rect.height(),x,y)||contains(annotation.label(),x,y)) return Optional.of(new DiagramElementTarget(annotation.elementIndex(),annotation.elementId()));
        }
        for (var dimension : diagram.mechanicalDimensions()) {
            var rect = dimension.bounds();
            if (contains(rect.x(), rect.y(), rect.width(), rect.height(), x, y)
                    || contains(dimension.label(), x, y)) {
                return Optional.of(new DiagramElementTarget(dimension.elementIndex(), dimension.elementId()));
            }
        }
        for (var constraint : diagram.mechanicalConstraints()) {
            var rect = constraint.bounds();
            if (contains(rect.x(), rect.y(), rect.width(), rect.height(), x, y)) {
                return Optional.of(new DiagramElementTarget(constraint.elementIndex(), constraint.elementId()));
            }
        }

        // Junction centers select/move the splice itself. Its four side-port hit
        // regions remain available just outside the dot for starting branches.
        for (var junction : diagram.electricalJunctions()) {
            var hit = hitPort(junction.ports(), x, y);
            if (hit.isPresent()) {
                return hit;
            }
        }

        for (var connection : diagram.connections()) {
            if (connection.label().filter(label -> contains(label, x, y)).isPresent()) {
                return Optional.of(new DiagramConnectionTarget(connection.connectionIndex()));
            }
            var tolerance = (int) Math.ceil(CONNECTION_TOLERANCE);
            if (!connection.bounds().expanded(tolerance).contains(x, y)) {
                continue;
            }
            var path = connection.path();
            for (var index = 1; index < path.size(); index++) {
                if (distanceToSegment(x, y, path.get(index - 1), path.get(index)) <= CONNECTION_TOLERANCE) {
                    return Optional.of(new DiagramConnectionTarget(connection.connectionIndex()));
                }
            }
        }

        if (diagram.title().filter(label -> contains(label, x, y)).isPresent()) {
            return Optional.of(new DiagramPropertyTarget(DiagramProperty.TITLE));
        }

        if (diagram.workspaceBounds().contains(x, y)) {
            return Optional.of(new DiagramPropertyTarget(DiagramProperty.CANVAS));
        }
        return Optional.empty();
    }

    private static Optional<DiagramEditTarget> hitPort(List<LaidOutDiagramPort> ports, int x, int y) {
        for (var port : ports) {
            if (port.hitBounds().contains(x, y)
                    || port.label().filter(label -> contains(label, x, y)).isPresent()) {
                return Optional.of(new DiagramPortTarget(
                        port.elementIndex(),
                        port.portIndex(),
                        port.elementId(),
                        port.portId()));
            }
        }
        return Optional.empty();
    }

    private static boolean hitElectricalComponent(LaidOutElectricalComponent component, int x, int y) {
        if (component.referenceDesignator().filter(label -> contains(label, x, y)).isPresent()
                || component.valueLabel().filter(label -> contains(label, x, y)).isPresent()) {
            return true;
        }

        for (var primitive : component.primitives()) {
            if (primitive instanceof LaidOutElectricalLine line) {
                if (distanceToSegment(x, y, line.start(), line.end()) <= ELECTRICAL_SYMBOL_TOLERANCE) {
                    return true;
                }
            } else if (primitive instanceof LaidOutElectricalPolyline polyline) {
                var points = polyline.points();
                for (var index = 1; index < points.size(); index++) {
                    if (distanceToSegment(x, y, points.get(index - 1), points.get(index)) <= ELECTRICAL_SYMBOL_TOLERANCE) {
                        return true;
                    }
                }
            } else if (primitive instanceof LaidOutElectricalCircle circle) {
                if (Math.hypot(x - circle.center().x(), y - circle.center().y())
                        <= circle.radius() + ELECTRICAL_SYMBOL_TOLERANCE) {
                    return true;
                }
            }
        }

        // The authored component bounds remain a convenient forgiving interaction
        // target for sparse symbols such as an open SPST switch. Geometry above is
        // still tested explicitly so future shapes are not tied to Minecraft pixels.
        return contains(component.x(), component.y(), component.width(), component.height(), x, y);
    }

    private static boolean contains(LaidOutDiagramLabel label, int x, int y) {
        return contains(label.x(), label.y(), Math.max(1, label.width()), Math.max(1, label.height()), x, y);
    }

    private static boolean contains(int left, int top, int width, int height, int x, int y) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }

    private static double distanceToSegment(
            double px,
            double py,
            LaidOutDiagramPoint start,
            LaidOutDiagramPoint end
    ) {
        var dx = end.x() - start.x();
        var dy = end.y() - start.y();
        if (dx == 0.0 && dy == 0.0) {
            return Math.hypot(px - start.x(), py - start.y());
        }
        var t = ((px - start.x()) * dx + (py - start.y()) * dy) / (dx * dx + dy * dy);
        t = Math.max(0.0, Math.min(1.0, t));
        var closestX = start.x() + t * dx;
        var closestY = start.y() + t * dy;
        return Math.hypot(px - closestX, py - closestY);
    }
}
