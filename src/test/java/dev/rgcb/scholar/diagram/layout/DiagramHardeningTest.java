package dev.rgcb.scholar.diagram.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Final M17 regression coverage for responsive and degenerate diagram geometry. */
class DiagramHardeningTest {
    private final DiagramLayoutEngine engine = new DiagramLayoutEngine();
    private final DiagramConnectionRouter router = new DiagramConnectionRouter();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void coincidentBoundaryEndpointsProduceStableZeroLengthSegment() {
        var canvas = new LaidOutDiagramRect(0, 0, 100, 50);
        var source = port(0, 0, DiagramPortSide.LEFT, "source");
        var target = port(0, 0, DiagramPortSide.LEFT, "target");

        var path = router.route(source, target, canvas);
        var bounds = router.bounds(path);

        assertEquals(2, path.size());
        assertEquals(new LaidOutDiagramPoint(0, 0), path.get(0));
        assertEquals(path.get(0), path.get(1));
        assertTrue(bounds.contains(path.get(0)));
    }

    @Test
    void coincidentSemanticPortsRemainLayoutable() {
        var a = new DiagramElementId("a");
        var b = new DiagramElementId("b");
        var aPort = new DiagramPortId("left-a");
        var bPort = new DiagramPortId("left-b");
        var block = new DiagramBlock(new DiagramDefinition(
                "",
                new DiagramCanvas(100, 50),
                List.of(
                        node(a, new DiagramBounds(0, 0, 20, 10), aPort, DiagramPortSide.LEFT, 0.0, ""),
                        node(b, new DiagramBounds(0, 0, 20, 10), bPort, DiagramPortSide.LEFT, 0.0, "")),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(a, aPort),
                        new DiagramEndpoint(b, bPort),
                        "coincident"))));

        var layout = engine.layout(block, 0, 0, 0, 116, textMeasurer);

        assertEquals(1, layout.connections().size());
        assertEquals(2, layout.connections().get(0).path().size());
        assertEquals(layout.connections().get(0).path().get(0), layout.connections().get(0).path().get(1));
    }

    @Test
    void veryNarrowLayoutKeepsNodeRectangleInsideCanvas() {
        var block = new DiagramBlock(new DiagramDefinition(
                "",
                new DiagramCanvas(100, 50),
                List.of(node(
                        new DiagramElementId("edge"),
                        new DiagramBounds(90, 40, 10, 10),
                        new DiagramPortId("right"),
                        DiagramPortSide.RIGHT,
                        1.0,
                        "")),
                List.of()));

        var layout = engine.layout(block, 0, 0, 0, 17, textMeasurer);
        var node = layout.nodes().get(0);
        var canvas = new LaidOutDiagramRect(layout.canvasX(), layout.canvasY(), layout.canvasWidth(), layout.canvasHeight());

        assertTrue(node.width() >= 1);
        assertTrue(node.height() >= 1);
        assertTrue(canvas.contains(new LaidOutDiagramPoint(node.x(), node.y())));
        assertTrue(canvas.contains(new LaidOutDiagramPoint(node.x() + node.width(), node.y() + node.height())));
    }

    @Test
    void edgePortLabelIsClampedInsideCanvasWhenItFits() {
        var id = new DiagramElementId("edge");
        var portId = new DiagramPortId("left");
        var block = new DiagramBlock(new DiagramDefinition(
                "",
                new DiagramCanvas(100, 50),
                List.of(node(id, new DiagramBounds(0, 15, 20, 20), portId, DiagramPortSide.LEFT, 0.5, "in")),
                List.of()));

        var layout = engine.layout(block, 0, 0, 0, 116, textMeasurer);
        var label = layout.nodes().get(0).ports().get(0).label().orElseThrow();

        assertTrue(label.x() >= layout.canvasX());
        assertTrue(label.x() + label.width() <= layout.canvasX() + layout.canvasWidth());
        assertTrue(label.y() >= layout.canvasY());
        assertTrue(label.y() + label.height() <= layout.canvasY() + layout.canvasHeight());
    }

    @Test
    void edgeNodeLabelIsClampedInsideCanvasWhenItFits() {
        var id = new DiagramElementId("edge");
        var block = new DiagramBlock(new DiagramDefinition(
                "",
                new DiagramCanvas(100, 50),
                List.of(new DiagramNode(id, new DiagramBounds(0, 0, 8, 10), "Node", List.of())),
                List.of()));

        var layout = engine.layout(block, 0, 0, 0, 116, textMeasurer);
        var label = layout.nodes().get(0).label().orElseThrow();

        assertTrue(label.x() >= layout.canvasX());
        assertTrue(label.x() + label.width() <= layout.canvasX() + layout.canvasWidth());
        assertTrue(label.y() >= layout.canvasY());
        assertTrue(label.y() + label.height() <= layout.canvasY() + layout.canvasHeight());
    }

    @Test
    void emptyDiagramRemainsValidAtMinimumDocumentWidth() {
        var block = new DiagramBlock(new DiagramDefinition(
                "", new DiagramCanvas(100, 50), List.of(), List.of()));

        var layout = engine.layout(block, 0, 7, 9, 1, textMeasurer);

        assertEquals(1, layout.canvasWidth());
        assertTrue(layout.canvasHeight() >= 1);
        assertTrue(layout.nodes().isEmpty());
        assertTrue(layout.connections().isEmpty());
    }

    @Test
    void responsiveReflowDoesNotChangeAuthoredLogicalBounds() {
        var bounds = new DiagramBounds(8.25, 10.5, 30.25, 20.5);
        var block = new DiagramBlock(new DiagramDefinition(
                "",
                new DiagramCanvas(100, 50),
                List.of(new DiagramNode(new DiagramElementId("node"), bounds, "Node", List.of())),
                List.of()));

        var narrow = engine.layout(block, 0, 0, 0, 76, textMeasurer);
        var wide = engine.layout(block, 0, 0, 0, 316, textMeasurer);

        assertFalse(narrow.nodes().get(0).width() == wide.nodes().get(0).width());
        assertEquals(bounds, ((DiagramNode) block.definition().elements().get(0)).bounds());
    }

    @Test
    void degenerateRouteRemainsOrthogonalAndDeterministic() {
        var canvas = new LaidOutDiagramRect(0, 0, 100, 50);
        var source = port(0, 25, DiagramPortSide.LEFT, "source");
        var target = port(0, 25, DiagramPortSide.LEFT, "target");

        var first = router.route(source, target, canvas);
        var second = router.route(source, target, canvas);

        assertEquals(first, second);
        for (var index = 1; index < first.size(); index++) {
            var a = first.get(index - 1);
            var b = first.get(index);
            assertTrue(a.x() == b.x() || a.y() == b.y());
        }
    }

    private static DiagramNode node(
            DiagramElementId id,
            DiagramBounds bounds,
            DiagramPortId portId,
            DiagramPortSide side,
            double offset,
            String portLabel
    ) {
        return new DiagramNode(
                id,
                bounds,
                id.value(),
                List.of(new DiagramPort(portId, portLabel, new DiagramPortPlacement(side, offset))));
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
