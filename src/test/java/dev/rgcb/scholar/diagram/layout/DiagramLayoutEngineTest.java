package dev.rgcb.scholar.diagram.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.TextBoundary;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagramLayoutEngineTest {
    private final DiagramLayoutEngine engine = new DiagramLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void layoutMapsLogicalCanvasResponsivelyAtUniformScale() {
        var narrow = engine.layout(diagram(), 2, 0, 0, 116, textMeasurer);
        var wide = engine.layout(diagram(), 2, 0, 0, 216, textMeasurer);

        assertEquals(100, narrow.canvasWidth());
        assertEquals(50, narrow.canvasHeight());
        assertEquals(200, wide.canvasWidth());
        assertEquals(100, wide.canvasHeight());
        assertEquals(1.0, narrow.transform().scale());
        assertEquals(2.0, wide.transform().scale());
    }

    @Test
    void layoutMapsNodeBoundsAndPortCenters() {
        var layout = engine.layout(diagram(), 0, 10, 20, 116, textMeasurer);
        var first = layout.nodes().get(0);
        var port = first.ports().get(0);

        assertEquals(38, first.x());
        assertEquals(layout.canvasY() + 10, first.y());
        assertEquals(20, first.width());
        assertEquals(15, first.height());
        assertEquals(first.x() + first.width(), port.centerX());
        assertEquals(first.y() + 8, port.centerY());
    }

    @Test
    void connectionPathStartsAndEndsAtResolvedPortsAndIsOrthogonal() {
        var layout = engine.layout(diagram(), 0, 0, 0, 216, textMeasurer);
        var connection = layout.connections().get(0);
        var source = layout.nodes().get(0).ports().get(0);
        var target = layout.nodes().get(1).ports().get(0);

        assertEquals(new LaidOutDiagramPoint(source.centerX(), source.centerY()), connection.path().get(0));
        assertEquals(new LaidOutDiagramPoint(target.centerX(), target.centerY()),
                connection.path().get(connection.path().size() - 1));
        for (var index = 1; index < connection.path().size(); index++) {
            var a = connection.path().get(index - 1);
            var b = connection.path().get(index);
            assertTrue(a.x() == b.x() || a.y() == b.y());
        }
    }

    @Test
    void horizontalConnectionLabelIsCenteredOnSegmentInsteadOfTargetEndpoint() {
        var sensorId = new DiagramElementId("sensor");
        var processorId = new DiagramElementId("processor");
        var out = new DiagramPortId("out");
        var in = new DiagramPortId("in");
        var block = new DiagramBlock(new DiagramDefinition(
                "System Diagram",
                new DiagramCanvas(100, 50),
                List.of(
                        new DiagramNode(sensorId, new DiagramBounds(8, 14, 30, 20), "Sensor", List.of(
                                new DiagramPort(out, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)))),
                        new DiagramNode(processorId, new DiagramBounds(62, 14, 30, 20), "Processor", List.of(
                                new DiagramPort(in, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5))))),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(sensorId, out),
                        new DiagramEndpoint(processorId, in),
                        "signal"))));

        var layout = engine.layout(block, 0, 0, 0, 216, textMeasurer);
        var connection = layout.connections().get(0);
        var label = connection.label().orElseThrow();
        var start = connection.path().get(0);
        var end = connection.path().get(connection.path().size() - 1);
        var expectedCenterX = (start.x() + end.x()) / 2;

        assertEquals(expectedCenterX, label.x() + label.width() / 2);
        assertTrue(label.x() > layout.nodes().get(0).x() + layout.nodes().get(0).width());
        assertTrue(label.x() + label.width() < layout.nodes().get(1).x());
    }


    @Test
    void portCentersMapAuthoredPerimeterCoordinatesDirectly() {
        var nodeId = new DiagramElementId("node");
        var block = new DiagramBlock(new DiagramDefinition(
                "",
                new DiagramCanvas(100, 50),
                List.of(new DiagramNode(
                        nodeId,
                        new DiagramBounds(10.25, 10.25, 20.5, 15.5),
                        "",
                        List.of(
                                new DiagramPort(new DiagramPortId("left"), "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.25)),
                                new DiagramPort(new DiagramPortId("bottom"), "", new DiagramPortPlacement(DiagramPortSide.BOTTOM, 0.75))))),
                List.of()));

        var layout = engine.layout(block, 0, 0, 0, 216, textMeasurer);
        var transform = layout.transform();
        var ports = layout.nodes().get(0).ports();

        assertEquals((int) Math.round(transform.mapX(10.25)), ports.get(0).centerX());
        assertEquals((int) Math.round(transform.mapY(10.25 + 15.5 * 0.25)), ports.get(0).centerY());
        assertEquals((int) Math.round(transform.mapX(10.25 + 20.5 * 0.75)), ports.get(1).centerX());
        assertEquals((int) Math.round(transform.mapY(10.25 + 15.5)), ports.get(1).centerY());
    }

    @Test
    void laidOutConnectionBoundsEncloseEveryRoutedPoint() {
        var layout = engine.layout(diagram(), 0, 0, 0, 216, textMeasurer);
        var connection = layout.connections().get(0);

        assertTrue(connection.path().stream().allMatch(connection.bounds()::contains));
    }

    @Test
    void connectionRoutesRemainInsideCanvasAndLabelsAreKeptInsideWhenTheyFit() {
        var a = new DiagramElementId("a");
        var b = new DiagramElementId("b");
        var out = new DiagramPortId("out");
        var in = new DiagramPortId("in");
        var block = new DiagramBlock(new DiagramDefinition(
                "",
                new DiagramCanvas(100, 50),
                List.of(
                        new DiagramNode(a, new DiagramBounds(5, 0, 20, 10), "A", List.of(
                                new DiagramPort(out, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.0)))),
                        new DiagramNode(b, new DiagramBounds(70, 0, 20, 10), "B", List.of(
                                new DiagramPort(in, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.0))))),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(a, out),
                        new DiagramEndpoint(b, in),
                        "top signal"))));

        var layout = engine.layout(block, 0, 0, 0, 216, textMeasurer);
        var connection = layout.connections().get(0);
        var canvas = new LaidOutDiagramRect(layout.canvasX(), layout.canvasY(), layout.canvasWidth(), layout.canvasHeight());
        var label = connection.label().orElseThrow();

        assertTrue(connection.path().stream().allMatch(canvas::contains));
        assertTrue(label.y() >= layout.canvasY());
        assertTrue(label.x() >= layout.canvasX());
        assertTrue(label.x() + label.width() <= layout.canvasX() + layout.canvasWidth());
    }

    @Test
    void coordinateTransformRoundTripsLogicalCoordinates() {
        var transform = engine.layout(diagram(), 0, 0, 0, 216, textMeasurer).transform();
        var mappedX = transform.mapX(37.5);
        var mappedY = transform.mapY(22.25);

        assertEquals(37.5, transform.unmapX(mappedX));
        assertEquals(22.25, transform.unmapY(mappedY));
    }

    @Test
    void titleAndSemanticLabelsArePositionedByCoreLayout() {
        var layout = engine.layout(diagram(), 0, 0, 0, 216, textMeasurer);

        assertEquals("System Diagram", layout.title().orElseThrow().text());
        assertEquals("Node A", layout.nodes().get(0).label().orElseThrow().text());
        assertEquals("signal", layout.connections().get(0).label().orElseThrow().text());
    }

    @Test
    void blankTitleDoesNotReserveTitleLabel() {
        var source = diagram().definition();
        var blank = new DiagramBlock(new DiagramDefinition("", source.canvas(), source.elements(), source.connections()));
        var layout = engine.layout(blank, 0, 0, 0, 116, textMeasurer);

        assertTrue(layout.title().isEmpty());
        assertEquals(DiagramLayoutEngine.OUTER_PADDING, layout.canvasY());
    }

    @Test
    void layoutIsDeterministicForSameInputs() {
        var first = engine.layout(diagram(), 3, 7, 11, 216, textMeasurer);
        var second = engine.layout(diagram(), 3, 7, 11, 216, textMeasurer);

        assertEquals(first, second);
    }

    private static DiagramBlock diagram() {
        var aId = new DiagramElementId("a");
        var bId = new DiagramElementId("b");
        var out = new DiagramPortId("out");
        var in = new DiagramPortId("in");
        return new DiagramBlock(new DiagramDefinition(
                "System Diagram",
                new DiagramCanvas(100, 50),
                List.of(
                        new DiagramNode(aId, new DiagramBounds(20, 10, 20, 15), "Node A", List.of(
                                new DiagramPort(out, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)))),
                        new DiagramNode(bId, new DiagramBounds(65, 25, 20, 15), "Node B", List.of(
                                new DiagramPort(in, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5))))),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(aId, out),
                        new DiagramEndpoint(bId, in),
                        "signal"))));
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            return TextBoundary.characterCount(text) * 6;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return 10;
        }
    }
}
