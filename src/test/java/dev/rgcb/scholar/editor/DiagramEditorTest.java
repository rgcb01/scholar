package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

class DiagramEditorTest {
    private final DiagramEditor editor = new DiagramEditor();

    @Test
    void firstTargetPrefersFirstElementForImmediateManipulation() {
        var diagram = diagram();

        assertEquals(new DiagramElementTarget(0, new DiagramElementId("sensor")), editor.firstTarget(diagram));
    }

    @Test
    void emptyDiagramFallsBackToTitleTarget() {
        var empty = new DiagramBlock(new DiagramDefinition(
                "Empty", new DiagramCanvas(100, 50), List.of(), List.of()));

        assertEquals(new DiagramPropertyTarget(DiagramProperty.TITLE), editor.firstTarget(empty));
    }

    @Test
    void targetTraversalIsDeterministicAcrossPropertiesElementsPortsAndConnections() {
        var diagram = diagram();
        DiagramEditTarget current = new DiagramPropertyTarget(DiagramProperty.TITLE);

        current = editor.nextTarget(diagram, current);
        assertEquals(new DiagramElementTarget(0, new DiagramElementId("sensor")), current);
        current = editor.nextTarget(diagram, current);
        assertEquals(new DiagramPortTarget(
                0, 0, new DiagramElementId("sensor"), new DiagramPortId("out")), current);
        current = editor.nextTarget(diagram, current);
        assertEquals(new DiagramElementTarget(1, new DiagramElementId("processor")), current);
        current = editor.nextTarget(diagram, current);
        assertEquals(new DiagramPortTarget(
                1, 0, new DiagramElementId("processor"), new DiagramPortId("in")), current);
        current = editor.nextTarget(diagram, current);
        assertEquals(new DiagramConnectionTarget(0), current);
        current = editor.nextTarget(diagram, current);
        assertEquals(new DiagramPropertyTarget(DiagramProperty.CANVAS), current);
        assertEquals(current, editor.nextTarget(diagram, current));
        assertEquals(new DiagramConnectionTarget(0), editor.previousTarget(diagram, current));
    }

    @Test
    void staleElementIdentityIsRejectedEvenWhenIndexStillExists() {
        var target = new DiagramElementTarget(0, new DiagramElementId("processor"));

        assertThrows(IllegalArgumentException.class, () -> editor.validateSelection(diagram(), target));
    }

    @Test
    void stalePortIdentityIsRejectedEvenWhenIndicesStillExist() {
        var target = new DiagramPortTarget(
                0, 0, new DiagramElementId("sensor"), new DiagramPortId("wrong"));

        assertThrows(IllegalArgumentException.class, () -> editor.validateSelection(diagram(), target));
    }

    @Test
    void movePreservesElementIdentityPortsAndSemanticConnections() {
        var original = diagram();
        var target = (DiagramElementTarget) editor.firstTarget(original);

        var result = editor.moveElement(original, target, 40, 20);
        var moved = (DiagramNode) result.diagram().definition().elements().get(0);
        var before = (DiagramNode) original.definition().elements().get(0);

        assertTrue(result.changed());
        assertEquals(new DiagramBounds(40, 20, before.bounds().width(), before.bounds().height()), moved.bounds());
        assertEquals(before.id(), moved.id());
        assertEquals(before.label(), moved.label());
        assertEquals(before.ports(), moved.ports());
        assertEquals(original.definition().connections(), result.diagram().definition().connections());
    }

    @Test
    void movingToSameLogicalPositionIsNoOpAndReusesBlock() {
        var original = diagram();
        var target = (DiagramElementTarget) editor.firstTarget(original);
        var bounds = original.definition().elements().get(0).bounds();

        var result = editor.moveElement(original, target, bounds.x(), bounds.y());

        assertTrue(!result.changed());
        assertSame(original, result.diagram());
    }

    @Test
    void moveClampsWholeElementInsideLogicalCanvas() {
        var original = diagram();
        var target = (DiagramElementTarget) editor.firstTarget(original);

        var negative = editor.moveElement(original, target, -100, -200).diagram();
        var farEdge = editor.moveElement(original, target, 1000, 1000).diagram();

        assertEquals(new DiagramBounds(0, 0, 30, 20), negative.definition().elements().get(0).bounds());
        assertEquals(new DiagramBounds(70, 30, 30, 20), farEdge.definition().elements().get(0).bounds());
    }

    @Test
    void moveRejectsNonFiniteLogicalCoordinates() {
        var original = diagram();
        var target = (DiagramElementTarget) editor.firstTarget(original);

        assertThrows(IllegalArgumentException.class, () -> editor.moveElement(original, target, Double.NaN, 2));
        assertThrows(IllegalArgumentException.class, () -> editor.moveElement(original, target, 2, Double.POSITIVE_INFINITY));
    }

    @Test
    void movingNodeAutomaticallyReroutesDerivedConnectionGeometry() {
        var original = diagram();
        var target = (DiagramElementTarget) editor.firstTarget(original);
        var layoutEngine = new DiagramLayoutEngine();
        var measurer = new FixedTextMeasurer();
        var before = layoutEngine.layout(original, 0, 0, 0, 216, measurer);
        var moved = editor.moveElement(original, target, 20, 25).diagram();
        var after = layoutEngine.layout(moved, 0, 0, 0, 216, measurer);

        var beforeSource = before.connections().get(0).path().get(0);
        var afterSource = after.connections().get(0).path().get(0);

        assertTrue(beforeSource.x() != afterSource.x() || beforeSource.y() != afterSource.y());
        assertEquals(after.nodes().get(0).ports().get(0).centerX(), afterSource.x());
        assertEquals(after.nodes().get(0).ports().get(0).centerY(), afterSource.y());
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
