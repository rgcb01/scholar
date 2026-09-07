package dev.rgcb.scholar.electrical.layout;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.layout.DiagramCoordinateTransform;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramPoint;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.symbol.ElectricalSymbolCircle;
import dev.rgcb.scholar.electrical.symbol.ElectricalSymbolLibrary;
import dev.rgcb.scholar.electrical.symbol.ElectricalSymbolLine;
import dev.rgcb.scholar.electrical.symbol.ElectricalSymbolPolyline;
import dev.rgcb.scholar.electrical.symbol.NormalizedElectricalPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Maps semantic component symbols into positioned document-space geometry. */
public final class ElectricalSymbolLayoutEngine {
    private final ElectricalSymbolLibrary symbolLibrary;

    public ElectricalSymbolLayoutEngine() {
        this(new ElectricalSymbolLibrary());
    }

    public ElectricalSymbolLayoutEngine(ElectricalSymbolLibrary symbolLibrary) {
        this.symbolLibrary = Objects.requireNonNull(symbolLibrary, "symbolLibrary");
    }

    public List<LaidOutElectricalPrimitive> layout(
            ElectricalComponent component,
            DiagramCoordinateTransform transform,
            int laidOutWidth,
            int laidOutHeight
    ) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(transform, "transform");
        if (laidOutWidth <= 0 || laidOutHeight <= 0) {
            throw new IllegalArgumentException("Laid-out electrical dimensions must be positive.");
        }

        if (component.kind() == ElectricalComponentKind.DC_VOLTAGE_SOURCE) {
            return layoutVoltageSource(component, transform);
        }

        var result = new ArrayList<LaidOutElectricalPrimitive>();
        for (var primitive : symbolLibrary.symbol(component.kind()).primitives()) {
            if (primitive instanceof ElectricalSymbolLine line) {
                result.add(new LaidOutElectricalLine(
                        map(component, line.start(), transform),
                        map(component, line.end(), transform)));
            } else if (primitive instanceof ElectricalSymbolPolyline polyline) {
                result.add(new LaidOutElectricalPolyline(polyline.points().stream()
                        .map(point -> map(component, point, transform))
                        .toList()));
            } else if (primitive instanceof ElectricalSymbolCircle circle) {
                var center = map(component, circle.center(), transform);
                var radius = Math.max(1, (int) Math.round(Math.min(laidOutWidth, laidOutHeight) * circle.radius()));
                result.add(new LaidOutElectricalCircle(center, radius));
            } else {
                throw new IllegalArgumentException("Unsupported electrical symbol primitive: " + primitive.getClass().getName());
            }
        }
        return List.copyOf(result);
    }


    /**
     * Keeps the DC source body circular even when its authored bounds are not square,
     * while extending both terminal leads all the way from the perimeter to the circle.
     * Polarity glyphs stay screen-upright when the component is rotated.
     */
    private static List<LaidOutElectricalPrimitive> layoutVoltageSource(
            ElectricalComponent component,
            DiagramCoordinateTransform transform
    ) {
        var bounds = component.bounds();
        var left = (int) Math.round(transform.mapX(bounds.x()));
        var top = (int) Math.round(transform.mapY(bounds.y()));
        var right = (int) Math.round(transform.mapX(bounds.right()));
        var bottom = (int) Math.round(transform.mapY(bounds.bottom()));
        var width = Math.max(1, right - left);
        var height = Math.max(1, bottom - top);
        var center = new LaidOutDiagramPoint(left + width / 2, top + height / 2);

        var shortSide = Math.min(width, height);
        var radius = Math.max(2, (int) Math.round(shortSide * 0.32));
        radius = Math.min(radius, Math.max(1, (shortSide - 2) / 2));

        var positive = mapPort(bounds, requirePort(component, "positive"), transform);
        var negative = mapPort(bounds, requirePort(component, "negative"), transform);
        var positiveCircleEdge = circleEdge(center, positive, radius);
        var negativeCircleEdge = circleEdge(center, negative, radius);

        var markerDistance = Math.max(2, (int) Math.round(radius * 0.46));
        var markerHalf = Math.max(1, (int) Math.round(radius * 0.22));
        var plusCenter = moveToward(center, positive, markerDistance);
        var minusCenter = moveToward(center, negative, markerDistance);

        return List.of(
                new LaidOutElectricalLine(positive, positiveCircleEdge),
                new LaidOutElectricalCircle(center, radius),
                new LaidOutElectricalLine(negativeCircleEdge, negative),
                new LaidOutElectricalLine(
                        new LaidOutDiagramPoint(plusCenter.x() - markerHalf, plusCenter.y()),
                        new LaidOutDiagramPoint(plusCenter.x() + markerHalf, plusCenter.y())),
                new LaidOutElectricalLine(
                        new LaidOutDiagramPoint(plusCenter.x(), plusCenter.y() - markerHalf),
                        new LaidOutDiagramPoint(plusCenter.x(), plusCenter.y() + markerHalf)),
                new LaidOutElectricalLine(
                        new LaidOutDiagramPoint(minusCenter.x() - markerHalf, minusCenter.y()),
                        new LaidOutDiagramPoint(minusCenter.x() + markerHalf, minusCenter.y())));
    }

    private static DiagramPortPlacement requirePort(ElectricalComponent component, String id) {
        return component.ports().stream()
                .filter(port -> port.id().value().equals(id))
                .map(DiagramPort::placement)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing electrical terminal: " + id));
    }

    private static LaidOutDiagramPoint mapPort(
            DiagramBounds bounds,
            DiagramPortPlacement placement,
            DiagramCoordinateTransform transform
    ) {
        double logicalX;
        double logicalY;
        switch (placement.side()) {
            case LEFT -> {
                logicalX = bounds.x();
                logicalY = bounds.y() + bounds.height() * placement.offset();
            }
            case RIGHT -> {
                logicalX = bounds.right();
                logicalY = bounds.y() + bounds.height() * placement.offset();
            }
            case TOP -> {
                logicalX = bounds.x() + bounds.width() * placement.offset();
                logicalY = bounds.y();
            }
            case BOTTOM -> {
                logicalX = bounds.x() + bounds.width() * placement.offset();
                logicalY = bounds.bottom();
            }
            default -> throw new IllegalStateException("Unexpected diagram port side: " + placement.side());
        }
        return new LaidOutDiagramPoint(
                (int) Math.round(transform.mapX(logicalX)),
                (int) Math.round(transform.mapY(logicalY)));
    }

    private static LaidOutDiagramPoint circleEdge(
            LaidOutDiagramPoint center,
            LaidOutDiagramPoint terminal,
            int radius
    ) {
        var dx = Integer.compare(terminal.x(), center.x());
        var dy = Integer.compare(terminal.y(), center.y());
        if (Math.abs(terminal.x() - center.x()) >= Math.abs(terminal.y() - center.y())) {
            return new LaidOutDiagramPoint(center.x() + dx * radius, center.y());
        }
        return new LaidOutDiagramPoint(center.x(), center.y() + dy * radius);
    }

    private static LaidOutDiagramPoint moveToward(
            LaidOutDiagramPoint center,
            LaidOutDiagramPoint target,
            int distance
    ) {
        if (Math.abs(target.x() - center.x()) >= Math.abs(target.y() - center.y())) {
            return new LaidOutDiagramPoint(
                    center.x() + Integer.compare(target.x(), center.x()) * distance,
                    center.y());
        }
        return new LaidOutDiagramPoint(
                center.x(),
                center.y() + Integer.compare(target.y(), center.y()) * distance);
    }

    private static LaidOutDiagramPoint map(
            ElectricalComponent component,
            NormalizedElectricalPoint point,
            DiagramCoordinateTransform transform
    ) {
        var rotated = rotate(point, component.orientation().quarterTurnsClockwise());
        var bounds = component.bounds();
        var logicalX = bounds.x() + rotated.x() * bounds.width();
        var logicalY = bounds.y() + rotated.y() * bounds.height();
        return new LaidOutDiagramPoint(
                (int) Math.round(transform.mapX(logicalX)),
                (int) Math.round(transform.mapY(logicalY)));
    }

    static NormalizedElectricalPoint rotate(NormalizedElectricalPoint point, int quarterTurnsClockwise) {
        var x = point.x();
        var y = point.y();
        for (var turn = 0; turn < quarterTurnsClockwise; turn++) {
            var nextX = 1.0 - y;
            var nextY = x;
            x = nextX;
            y = nextY;
        }
        return new NormalizedElectricalPoint(clampUnit(x), clampUnit(y));
    }

    private static double clampUnit(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
