package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagramStructuralEditingTest {
    private final DiagramEditor editor = new DiagramEditor();

    @Test
    void editsDiagramTitleNodePortAndConnectionLabelsImmutably() {
        var original = diagram();
        var title = new DiagramPropertyTarget(DiagramProperty.TITLE);
        var node = new DiagramElementTarget(0, new DiagramElementId("sensor"));
        var port = new DiagramPortTarget(0, 0, new DiagramElementId("sensor"), new DiagramPortId("out"));
        var connection = new DiagramConnectionTarget(0);

        var titled = editor.setText(original, title, "Control Loop").diagram();
        var nodeLabeled = editor.setText(titled, node, "Input Sensor").diagram();
        var portLabeled = editor.setText(nodeLabeled, port, "OUT").diagram();
        var connectionLabeled = editor.setText(portLabeled, connection, "measurement").diagram();

        assertEquals("System Diagram", original.definition().title());
        assertEquals("Control Loop", connectionLabeled.definition().title());
        assertEquals("Input Sensor", ((DiagramNode) connectionLabeled.definition().elements().get(0)).label());
        assertEquals("OUT", connectionLabeled.definition().elements().get(0).ports().get(0).label());
        assertEquals("measurement", connectionLabeled.definition().connections().get(0).label());
    }

    @Test
    void canvasHasNoEditableTextValue() {
        var target = new DiagramPropertyTarget(DiagramProperty.CANVAS);
        assertFalse(editor.canEditText(diagram(), target));
    }

    @Test
    void settingSameLabelIsNoOp() {
        var target = new DiagramElementTarget(0, new DiagramElementId("sensor"));
        var result = editor.setText(diagram(), target, "Sensor");
        assertFalse(result.changed());
    }

    @Test
    void addNodeCreatesGenericFourPortNodeInsideCanvasAndSelectsIt() {
        var original = diagram();
        var result = editor.addNode(original, new DiagramPropertyTarget(DiagramProperty.CANVAS));
        var node = assertInstanceOf(DiagramNode.class, result.diagram().definition().elements().get(2));

        assertTrue(result.changed());
        assertEquals("node-1", node.id().value());
        assertEquals("Node 1", node.label());
        assertEquals(4, node.ports().size());
        assertEquals(List.of(DiagramPortSide.LEFT, DiagramPortSide.RIGHT, DiagramPortSide.TOP, DiagramPortSide.BOTTOM),
                node.ports().stream().map(port -> port.placement().side()).toList());
        assertTrue(node.bounds().x() >= 0 && node.bounds().right() <= original.definition().canvas().width());
        assertTrue(node.bounds().y() >= 0 && node.bounds().bottom() <= original.definition().canvas().height());
        assertEquals(new DiagramElementTarget(2, node.id()), result.target());
    }

    @Test
    void addNodeUsesNextFreeStableLocalId() {
        var node1 = new DiagramNode(
                new DiagramElementId("node-1"),
                new DiagramBounds(0, 0, 10, 10),
                "Existing",
                List.of());
        var original = new DiagramBlock(new DiagramDefinition(
                "D", new DiagramCanvas(100, 50), List.of(node1), List.of()));

        var result = editor.addNode(original, new DiagramElementTarget(0, node1.id()));
        var added = (DiagramNode) result.diagram().definition().elements().get(1);

        assertEquals("node-2", added.id().value());
        assertEquals("Node 2", added.label());
    }

    @Test
    void deleteNodeRemovesIncidentConnectionsInSameEdit() {
        var original = threeNodeDiagram();
        var target = new DiagramElementTarget(1, new DiagramElementId("middle"));

        var result = editor.deleteNode(original, target);

        assertEquals(2, result.diagram().definition().elements().size());
        assertEquals(1, result.diagram().definition().connections().size());
        assertEquals("direct", result.diagram().definition().connections().get(0).label());
        assertEquals(new DiagramElementId("right"), ((DiagramElementTarget) result.target()).elementId());
    }

    @Test
    void addConnectionStoresSemanticEndpointsAndSelectsNewConnection() {
        var original = unconnectedDiagram();
        var source = new DiagramPortTarget(0, 0, new DiagramElementId("left"), new DiagramPortId("out"));
        var target = new DiagramPortTarget(1, 0, new DiagramElementId("right"), new DiagramPortId("in"));

        var result = editor.addConnection(original, source, target);

        assertEquals(1, result.diagram().definition().connections().size());
        assertEquals(new DiagramEndpoint(source.elementId(), source.portId()), result.diagram().definition().connections().get(0).source());
        assertEquals(new DiagramEndpoint(target.elementId(), target.portId()), result.diagram().definition().connections().get(0).target());
        assertEquals("", result.diagram().definition().connections().get(0).label());
        assertEquals(new DiagramConnectionTarget(0), result.target());
    }

    @Test
    void connectionCannotFinishOnItsOwnSourcePort() {
        var original = unconnectedDiagram();
        var source = new DiagramPortTarget(0, 0, new DiagramElementId("left"), new DiagramPortId("out"));

        assertFalse(editor.canAddConnection(original, source, source));
        assertFalse(editor.addConnection(original, source, source).changed());
    }

    @Test
    void deleteConnectionKeepsDefinitionValidAndMovesSelectionPredictably() {
        var original = threeNodeDiagram();

        var result = editor.deleteConnection(original, new DiagramConnectionTarget(1));

        assertEquals(2, result.diagram().definition().connections().size());
        assertEquals(new DiagramConnectionTarget(1), result.target());
        assertEquals("direct", result.diagram().definition().connections().get(1).label());
    }

    private static DiagramBlock diagram() {
        var sensor = new DiagramElementId("sensor");
        var processor = new DiagramElementId("processor");
        var out = new DiagramPortId("out");
        var in = new DiagramPortId("in");
        return new DiagramBlock(new DiagramDefinition(
                "System Diagram",
                new DiagramCanvas(100, 50),
                List.of(
                        new DiagramNode(sensor, new DiagramBounds(8, 10, 30, 20), "Sensor", List.of(
                                new DiagramPort(out, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)))),
                        new DiagramNode(processor, new DiagramBounds(62, 20, 30, 20), "Processor", List.of(
                                new DiagramPort(in, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5))))),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(sensor, out),
                        new DiagramEndpoint(processor, in),
                        "signal"))));
    }

    private static DiagramBlock unconnectedDiagram() {
        var left = new DiagramElementId("left");
        var right = new DiagramElementId("right");
        return new DiagramBlock(new DiagramDefinition(
                "D",
                new DiagramCanvas(100, 50),
                List.of(
                        new DiagramNode(left, new DiagramBounds(5, 10, 20, 15), "Left", List.of(
                                new DiagramPort(new DiagramPortId("out"), "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)))),
                        new DiagramNode(right, new DiagramBounds(70, 10, 20, 15), "Right", List.of(
                                new DiagramPort(new DiagramPortId("in"), "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5))))),
                List.of()));
    }

    private static DiagramBlock threeNodeDiagram() {
        var left = new DiagramElementId("left");
        var middle = new DiagramElementId("middle");
        var right = new DiagramElementId("right");
        var leftOut = new DiagramPortId("out");
        var middleIn = new DiagramPortId("in");
        var middleOut = new DiagramPortId("out");
        var rightIn = new DiagramPortId("in");
        var leftDirect = new DiagramPortId("direct");
        var rightDirect = new DiagramPortId("direct");
        return new DiagramBlock(new DiagramDefinition(
                "D",
                new DiagramCanvas(120, 60),
                List.of(
                        new DiagramNode(left, new DiagramBounds(5, 20, 20, 15), "Left", List.of(
                                new DiagramPort(leftOut, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.35)),
                                new DiagramPort(leftDirect, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.7)))),
                        new DiagramNode(middle, new DiagramBounds(50, 20, 20, 15), "Middle", List.of(
                                new DiagramPort(middleIn, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.35)),
                                new DiagramPort(middleOut, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.65)))),
                        new DiagramNode(right, new DiagramBounds(95, 20, 20, 15), "Right", List.of(
                                new DiagramPort(rightIn, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.35)),
                                new DiagramPort(rightDirect, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.7))))),
                List.of(
                        new DiagramConnection(new DiagramEndpoint(left, leftOut), new DiagramEndpoint(middle, middleIn), "left-middle"),
                        new DiagramConnection(new DiagramEndpoint(middle, middleOut), new DiagramEndpoint(right, rightIn), "middle-right"),
                        new DiagramConnection(new DiagramEndpoint(left, leftDirect), new DiagramEndpoint(right, rightDirect), "direct"))));
    }
}
