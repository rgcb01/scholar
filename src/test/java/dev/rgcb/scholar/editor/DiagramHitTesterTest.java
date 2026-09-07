package dev.rgcb.scholar.editor;

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
import dev.rgcb.scholar.diagram.layout.DiagramLayoutEngine;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagramHitTesterTest {
    private final DiagramLayoutEngine layoutEngine = new DiagramLayoutEngine();
    private final DiagramHitTester hitTester = new DiagramHitTester();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void portWinsOverOwningElementAtPerimeter() {
        var layout = layout();
        var port = layout.nodes().get(0).ports().get(0);

        var target = hitTester.hit(layout, port.centerX(), port.centerY()).orElseThrow();

        assertEquals(new DiagramPortTarget(0, 0, port.elementId(), port.portId()), target);
    }

    @Test
    void portLabelMapsBackToOwningPortForLabelEditing() {
        var layout = labeledPortLayout();
        var port = layout.nodes().get(0).ports().get(0);
        var label = port.label().orElseThrow();

        var target = hitTester.hit(layout, label.x() + 1, label.y() + 1).orElseThrow();

        assertEquals(new DiagramPortTarget(0, 0, port.elementId(), port.portId()), target);
    }

    @Test
    void elementWinsOverConnectionPassingBehindNode() {
        var layout = layout();
        var node = layout.nodes().get(0);

        var target = hitTester.hit(layout, node.x() + node.width() / 2, node.y() + node.height() / 2).orElseThrow();

        assertEquals(new DiagramElementTarget(0, node.elementId()), target);
    }

    @Test
    void connectionSegmentUsesBoundedClickTolerance() {
        var layout = layout();
        var connection = layout.connections().get(0);
        var first = connection.path().get(1);
        var second = connection.path().get(2);
        var x = (first.x() + second.x()) / 2;
        var y = (first.y() + second.y()) / 2;

        assertEquals(new DiagramConnectionTarget(0), hitTester.hit(layout, x, y + 3).orElseThrow());
        var outside = hitTester.hit(layout, x, y + 6);
        assertTrue(outside.isEmpty() || !(outside.orElseThrow() instanceof DiagramConnectionTarget));
    }

    @Test
    void connectionLabelMapsToConnectionTarget() {
        var layout = layout();
        var label = layout.connections().get(0).label().orElseThrow();

        var target = hitTester.hit(layout, label.x() + label.width() / 2, label.y() + label.height() / 2).orElseThrow();

        assertEquals(new DiagramConnectionTarget(0), target);
    }

    @Test
    void titleMapsToTitleProperty() {
        var layout = layout();
        var title = layout.title().orElseThrow();

        assertEquals(
                new DiagramPropertyTarget(DiagramProperty.TITLE),
                hitTester.hit(layout, title.x() + 1, title.y() + 1).orElseThrow());
    }

    @Test
    void blankCanvasMapsToCanvasProperty() {
        var layout = layout();

        assertEquals(
                new DiagramPropertyTarget(DiagramProperty.CANVAS),
                hitTester.hit(layout, layout.canvasX() + 2, layout.canvasY() + 2).orElseThrow());
    }

    @Test
    void pointOutsideDiagramAndTitleHasNoTarget() {
        var layout = layout();

        assertTrue(hitTester.hit(layout, layout.x() - 20, layout.y() - 20).isEmpty());
    }

    @Test
    void hitTargetsPreserveSemanticIdsAlongsideSnapshotIndices() {
        var layout = layout();
        var second = layout.nodes().get(1);

        assertEquals(
                new DiagramElementTarget(1, new DiagramElementId("processor")),
                hitTester.hit(layout, second.x() + 10, second.y() + 10).orElseThrow());
    }

    private dev.rgcb.scholar.diagram.layout.LaidOutDiagram labeledPortLayout() {
        var left = new DiagramElementId("left");
        var out = new DiagramPortId("out");
        var block = new DiagramBlock(new DiagramDefinition(
                "Ports",
                new DiagramCanvas(100, 50),
                List.of(new DiagramNode(left, new DiagramBounds(10, 15, 25, 18), "Node", List.of(
                        new DiagramPort(out, "output", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5))))),
                List.of()));
        return layoutEngine.layout(block, 0, 0, 20, 216, textMeasurer);
    }

    private dev.rgcb.scholar.diagram.layout.LaidOutDiagram layout() {
        var sensor = new DiagramElementId("sensor");
        var processor = new DiagramElementId("processor");
        var out = new DiagramPortId("out");
        var in = new DiagramPortId("in");
        var block = new DiagramBlock(new DiagramDefinition(
                "System Diagram",
                new DiagramCanvas(100, 50),
                List.of(
                        new DiagramNode(sensor, new DiagramBounds(8, 14, 30, 20), "Sensor", List.of(
                                new DiagramPort(out, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)))),
                        new DiagramNode(processor, new DiagramBounds(62, 14, 30, 20), "Processor", List.of(
                                new DiagramPort(in, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5))))),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(sensor, out),
                        new DiagramEndpoint(processor, in),
                        "signal"))));
        return layoutEngine.layout(block, 3, 0, 20, 216, textMeasurer);
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
