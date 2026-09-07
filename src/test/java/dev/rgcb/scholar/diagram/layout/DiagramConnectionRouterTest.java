package dev.rgcb.scholar.diagram.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DiagramConnectionRouterTest {
    private final DiagramConnectionRouter router = new DiagramConnectionRouter();
    private final LaidOutDiagramRect canvas = new LaidOutDiagramRect(0, 0, 200, 120);

    @Test
    void opposingHorizontalPortsReceiveOutwardExitSegments() {
        var source = port(20, 40, DiagramPortSide.RIGHT, "source");
        var target = port(160, 80, DiagramPortSide.LEFT, "target");

        var path = router.route(source, target, canvas);

        assertEquals(new LaidOutDiagramPoint(20, 40), path.get(0));
        assertEquals(new LaidOutDiagramPoint(28, 40), path.get(1));
        assertEquals(new LaidOutDiagramPoint(152, 80), path.get(path.size() - 2));
        assertEquals(new LaidOutDiagramPoint(160, 80), path.get(path.size() - 1));
        assertOrthogonal(path);
    }

    @Test
    void opposingVerticalPortsUseHorizontalTrunk() {
        var source = port(40, 20, DiagramPortSide.BOTTOM, "source");
        var target = port(160, 100, DiagramPortSide.TOP, "target");

        var path = router.route(source, target, canvas);

        assertEquals(new LaidOutDiagramPoint(40, 28), path.get(1));
        assertEquals(new LaidOutDiagramPoint(160, 92), path.get(path.size() - 2));
        assertOrthogonal(path);
    }

    @Test
    void mixedPortOrientationsUseSingleDeterministicElbowBetweenExits() {
        var source = port(20, 40, DiagramPortSide.RIGHT, "source");
        var target = port(140, 100, DiagramPortSide.TOP, "target");

        var path = router.route(source, target, canvas);

        assertEquals(List.of(
                new LaidOutDiagramPoint(20, 40),
                new LaidOutDiagramPoint(28, 40),
                new LaidOutDiagramPoint(140, 40),
                new LaidOutDiagramPoint(140, 92),
                new LaidOutDiagramPoint(140, 100)), path);
    }

    @Test
    void alignedPortsKeepStraightOrthogonalRouteWithExplicitExits() {
        var source = port(20, 50, DiagramPortSide.RIGHT, "source");
        var target = port(160, 50, DiagramPortSide.LEFT, "target");

        var path = router.route(source, target, canvas);

        assertEquals(List.of(
                new LaidOutDiagramPoint(20, 50),
                new LaidOutDiagramPoint(28, 50),
                new LaidOutDiagramPoint(152, 50),
                new LaidOutDiagramPoint(160, 50)), path);
    }

    @Test
    void exitsAreClampedWhenPortIsOnCanvasBoundary() {
        var source = port(0, 50, DiagramPortSide.LEFT, "source");
        var target = port(200, 50, DiagramPortSide.RIGHT, "target");

        var path = router.route(source, target, canvas);

        assertEquals(new LaidOutDiagramPoint(0, 50), path.get(0));
        assertEquals(new LaidOutDiagramPoint(200, 50), path.get(path.size() - 1));
        assertTrue(path.stream().allMatch(canvas::contains));
        assertOrthogonal(path);
    }

    @Test
    void everyDerivedRoutePointRemainsInsideCanvas() {
        var combinations = List.of(DiagramPortSide.LEFT, DiagramPortSide.RIGHT, DiagramPortSide.TOP, DiagramPortSide.BOTTOM);
        for (var sourceSide : combinations) {
            for (var targetSide : combinations) {
                var path = router.route(
                        port(12, 12, sourceSide, "source"),
                        port(188, 108, targetSide, "target"),
                        canvas);
                assertTrue(path.stream().allMatch(canvas::contains), sourceSide + " -> " + targetSide);
                assertOrthogonal(path);
            }
        }
    }

    @Test
    void routeBoundsEncloseAllPathPoints() {
        var path = router.route(
                port(20, 30, DiagramPortSide.RIGHT, "source"),
                port(150, 90, DiagramPortSide.TOP, "target"),
                canvas);

        var bounds = router.bounds(path);

        assertTrue(path.stream().allMatch(bounds::contains));
        assertEquals(20, bounds.x());
        assertEquals(30, bounds.y());
        assertEquals(130, bounds.width());
        assertEquals(60, bounds.height());
    }

    private static LaidOutDiagramPort port(int x, int y, DiagramPortSide side, String id) {
        return new LaidOutDiagramPort(
                0,
                0,
                new DiagramElementId("element-" + id),
                new DiagramPortId(id),
                side,
                x,
                y,
                new LaidOutDiagramRect(x - 5, y - 5, 10, 10),
                Optional.empty());
    }

    private static void assertOrthogonal(List<LaidOutDiagramPoint> path) {
        for (var index = 1; index < path.size(); index++) {
            var first = path.get(index - 1);
            var second = path.get(index);
            assertTrue(first.x() == second.x() || first.y() == second.y(), first + " -> " + second);
        }
    }
}
