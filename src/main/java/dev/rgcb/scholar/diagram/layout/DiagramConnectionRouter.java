package dev.rgcb.scholar.diagram.layout;

import dev.rgcb.scholar.diagram.DiagramPortSide;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure-Java deterministic orthogonal routing for M17.
 *
 * <p>The router deliberately does not perform obstacle avoidance. It honors
 * each perimeter port's preferred outward side with a short exit segment,
 * then connects those exits using at most one orthogonal trunk/elbow. All
 * route points are constrained to the laid-out diagram canvas.</p>
 */
public final class DiagramConnectionRouter {
    public static final int PORT_EXIT_LENGTH = 8;

    public List<LaidOutDiagramPoint> route(
            LaidOutDiagramPort source,
            LaidOutDiagramPort target,
            LaidOutDiagramRect canvasBounds
    ) {
        return route(source, target, canvasBounds, PORT_EXIT_LENGTH, PORT_EXIT_LENGTH);
    }

    /**
     * Routes with independent straight-lead reservations for each endpoint.
     * Domain layers such as electrical schematics can request a slightly longer
     * initial wire before the first elbow without changing generic M17 routing.
     */
    public List<LaidOutDiagramPoint> route(
            LaidOutDiagramPort source,
            LaidOutDiagramPort target,
            LaidOutDiagramRect canvasBounds,
            int sourceExitLength,
            int targetExitLength
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(canvasBounds, "canvasBounds");
        if (sourceExitLength < 0 || targetExitLength < 0) {
            throw new IllegalArgumentException("Diagram port exit lengths must not be negative.");
        }

        var start = new LaidOutDiagramPoint(source.centerX(), source.centerY());
        var end = new LaidOutDiagramPoint(target.centerX(), target.centerY());
        var sourceExit = exitPoint(start, source.side(), canvasBounds, sourceExitLength);
        var targetExit = exitPoint(end, target.side(), canvasBounds, targetExitLength);

        var points = new ArrayList<LaidOutDiagramPoint>();
        addDistinct(points, start);
        addDistinct(points, sourceExit);

        if (sourceExit.x() == targetExit.x() || sourceExit.y() == targetExit.y()) {
            addDistinct(points, targetExit);
        } else if (isHorizontal(source.side()) && isHorizontal(target.side())) {
            var trunkX = midpoint(sourceExit.x(), targetExit.x());
            addDistinct(points, new LaidOutDiagramPoint(trunkX, sourceExit.y()));
            addDistinct(points, new LaidOutDiagramPoint(trunkX, targetExit.y()));
            addDistinct(points, targetExit);
        } else if (isVertical(source.side()) && isVertical(target.side())) {
            var trunkY = midpoint(sourceExit.y(), targetExit.y());
            addDistinct(points, new LaidOutDiagramPoint(sourceExit.x(), trunkY));
            addDistinct(points, new LaidOutDiagramPoint(targetExit.x(), trunkY));
            addDistinct(points, targetExit);
        } else if (isHorizontal(source.side())) {
            // Horizontal source -> vertical target: source direction remains
            // horizontal until the elbow, then target is approached vertically.
            addDistinct(points, new LaidOutDiagramPoint(targetExit.x(), sourceExit.y()));
            addDistinct(points, targetExit);
        } else {
            // Vertical source -> horizontal target.
            addDistinct(points, new LaidOutDiagramPoint(sourceExit.x(), targetExit.y()));
            addDistinct(points, targetExit);
        }

        addDistinct(points, end);

        // Distinct semantic endpoints are allowed to occupy the exact same
        // laid-out coordinate (for example overlapping nodes/ports at a canvas
        // boundary). Keep the path contract usable for layout/render/hit testing
        // by representing that degenerate connection as one zero-length segment.
        if (points.size() == 1) {
            points.add(end);
        }
        return List.copyOf(points);
    }

    public LaidOutDiagramRect bounds(List<LaidOutDiagramPoint> path) {
        Objects.requireNonNull(path, "path");
        if (path.size() < 2) {
            throw new IllegalArgumentException("Diagram route must contain at least two points.");
        }
        var minX = path.get(0).x();
        var maxX = minX;
        var minY = path.get(0).y();
        var maxY = minY;
        for (var point : path) {
            Objects.requireNonNull(point, "path point");
            minX = Math.min(minX, point.x());
            maxX = Math.max(maxX, point.x());
            minY = Math.min(minY, point.y());
            maxY = Math.max(maxY, point.y());
        }
        return new LaidOutDiagramRect(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY));
    }

    private static LaidOutDiagramPoint exitPoint(
            LaidOutDiagramPoint center,
            DiagramPortSide side,
            LaidOutDiagramRect canvasBounds,
            int exitLength
    ) {
        var x = center.x();
        var y = center.y();
        switch (side) {
            case LEFT -> x -= exitLength;
            case RIGHT -> x += exitLength;
            case TOP -> y -= exitLength;
            case BOTTOM -> y += exitLength;
        }
        return new LaidOutDiagramPoint(
                clamp(x, canvasBounds.x(), canvasBounds.right()),
                clamp(y, canvasBounds.y(), canvasBounds.bottom()));
    }

    private static boolean isHorizontal(DiagramPortSide side) {
        return side == DiagramPortSide.LEFT || side == DiagramPortSide.RIGHT;
    }

    private static boolean isVertical(DiagramPortSide side) {
        return side == DiagramPortSide.TOP || side == DiagramPortSide.BOTTOM;
    }

    private static int midpoint(int first, int second) {
        return first + (second - first) / 2;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static void addDistinct(List<LaidOutDiagramPoint> points, LaidOutDiagramPoint point) {
        if (points.isEmpty() || !points.get(points.size() - 1).equals(point)) {
            points.add(point);
        }
    }
}
