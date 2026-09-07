package dev.rgcb.scholar.electrical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElement;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.layout.DiagramLayoutEngine;
import dev.rgcb.scholar.diagram.layout.DiagramViewport;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.editor.BlockSelection;
import dev.rgcb.scholar.editor.DiagramEditor;
import dev.rgcb.scholar.editor.DiagramElementTarget;
import dev.rgcb.scholar.editor.DiagramHitTester;
import dev.rgcb.scholar.editor.DiagramProperty;
import dev.rgcb.scholar.editor.DiagramPropertyTarget;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.editor.EditorState;
import dev.rgcb.scholar.editor.TextBoundary;
import dev.rgcb.scholar.electrical.editor.ElectricalDiagramEditor;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Final M18G electrical responsive/edge-case hardening regressions. */
class ElectricalDiagramHardeningTest {
    private final DiagramLayoutEngine layoutEngine = new DiagramLayoutEngine();
    private final ElectricalDiagramEditor electricalEditor = new ElectricalDiagramEditor();
    private final ElectricalNetResolver netResolver = new ElectricalNetResolver();
    private final TextMeasurer measurer = new FixedTextMeasurer();

    @Test
    void everyKindAndQuarterTurnRemainsBoundedAtCanvasEdgeAcrossResponsiveWidths() {
        var canvas = new DiagramCanvas(100, 60);
        for (var kind : ElectricalComponentKind.values()) {
            for (var orientation : ElectricalOrientation.values()) {
                var component = new ElectricalComponent(
                        new DiagramElementId(kind.name().toLowerCase() + "-" + orientation.quarterTurnsClockwise()),
                        new DiagramBounds(72, 48, 28, 12),
                        kind,
                        orientation,
                        "X1",
                        "value");
                var block = new DiagramBlock(new DiagramDefinition(
                        "Edge", canvas, List.of(component), List.of()));

                for (var width : List.of(1, 17, 80, 216, 640)) {
                    var layout = layoutEngine.layout(block, 0, 0, 0, width, measurer);
                    var laidOut = layout.electricalComponents().get(0);
                    var canvasRect = layout.canvasBounds();
                    assertTrue(laidOut.width() >= 1);
                    assertTrue(laidOut.height() >= 1);
                    assertTrue(laidOut.x() >= canvasRect.x());
                    assertTrue(laidOut.y() >= canvasRect.y());
                    assertTrue(laidOut.x() + laidOut.width() <= canvasRect.right());
                    assertTrue(laidOut.y() + laidOut.height() <= canvasRect.bottom());
                    laidOut.ports().forEach(port -> assertTrue(canvasRect.contains(port.centerX(), port.centerY())));
                }
            }
        }
    }

    @Test
    void minAndMaxZoomKeepWorkspaceStableAndRejectOffWorkspaceHits() {
        var component = new ElectricalComponent(
                new DiagramElementId("r1"),
                new DiagramBounds(0, 20, 24, 12),
                ElectricalComponentKind.RESISTOR,
                ElectricalOrientation.DEG_0,
                "R1",
                "1 kΩ");
        var block = new DiagramBlock(new DiagramDefinition(
                "Zoom", new DiagramCanvas(100, 50), List.of(component), List.of()));
        var fit = layoutEngine.layout(block, 0, 0, 0, 216, measurer);
        var min = layoutEngine.layout(block, 0, 0, 0, 216, measurer,
                new DiagramViewport(DiagramViewport.MIN_ZOOM, 50, 25));
        var max = layoutEngine.layout(block, 0, 0, 0, 216, measurer,
                new DiagramViewport(DiagramViewport.MAX_ZOOM, 80, 25));

        assertEquals(fit.workspaceBounds(), min.workspaceBounds());
        assertEquals(fit.workspaceBounds(), max.workspaceBounds());
        assertTrue(max.electricalComponents().get(0).x() < max.workspaceX());
        assertTrue(new DiagramHitTester().hit(
                max,
                max.electricalComponents().get(0).x() + 1,
                max.electricalComponents().get(0).y() + 1).isEmpty());
    }

    @Test
    void degenerateElectricalWireAtOnePixelResponsiveLayoutKeepsValidRouteContract() {
        var junction = new ElectricalJunction(
                new DiagramElementId("j"), new DiagramBounds(48, 23, 4, 4), "N");
        var connection = new DiagramConnection(
                new DiagramEndpoint(junction.id(), ElectricalJunction.TOP),
                new DiagramEndpoint(junction.id(), ElectricalJunction.BOTTOM),
                "collapsed");
        var block = new DiagramBlock(new DiagramDefinition(
                "Degenerate", new DiagramCanvas(100, 50), List.of(junction), List.of(connection)));

        var layout = layoutEngine.layout(block, 0, 0, 0, 1, measurer);
        var routed = layout.connections().get(0);

        assertTrue(routed.path().size() >= 2);
        assertTrue(routed.bounds().width() >= 1);
        assertTrue(routed.bounds().height() >= 1);
        for (var index = 1; index < routed.path().size(); index++) {
            var a = routed.path().get(index - 1);
            var b = routed.path().get(index);
            assertTrue(a.x() == b.x() || a.y() == b.y());
        }
    }

    @Test
    void rotationAtEveryCanvasCornerPreservesConnectionsAndNetConnectivity() {
        var positions = List.of(
                new double[]{0, 0},
                new double[]{72, 0},
                new double[]{0, 48},
                new double[]{72, 48});
        for (var position : positions) {
            var component = new ElectricalComponent(
                    new DiagramElementId("r1"),
                    new DiagramBounds(position[0], position[1], 28, 12),
                    ElectricalComponentKind.RESISTOR,
                    ElectricalOrientation.DEG_0,
                    "R1",
                    "1 kΩ");
            var junction = new ElectricalJunction(
                    new DiagramElementId("j1"), new DiagramBounds(48, 28, 4, 4), "NET");
            var wire = new DiagramConnection(
                    new DiagramEndpoint(component.id(), new DiagramPortId("b")),
                    new DiagramEndpoint(junction.id(), ElectricalJunction.LEFT),
                    "");
            var block = new DiagramBlock(new DiagramDefinition(
                    "Rotate", new DiagramCanvas(100, 60), List.of(component, junction), List.of(wire)));
            var beforeConnectivity = netResolver.resolve(block.definition());

            var result = electricalEditor.rotateClockwise(
                    block, new DiagramElementTarget(0, component.id()));
            var rotated = (ElectricalComponent) result.diagram().definition().elements().get(0);

            assertTrue(result.changed());
            assertEquals(List.of(wire), result.diagram().definition().connections());
            assertInside(rotated.bounds(), result.diagram().definition().canvas());
            assertTrue(beforeConnectivity.electricallyConnected(wire.source(), wire.target()));
            assertTrue(netResolver.resolve(result.diagram().definition()).electricallyConnected(wire.source(), wire.target()));
        }
    }

    @Test
    void fourQuarterTurnsAtCanvasEdgeKeepTopologyAndReturnOrientation() {
        var component = new ElectricalComponent(
                new DiagramElementId("d1"), new DiagramBounds(72, 48, 28, 12),
                ElectricalComponentKind.DIODE, ElectricalOrientation.DEG_0, "D1", "1N4148");
        var junction = new ElectricalJunction(
                new DiagramElementId("j1"), new DiagramBounds(50, 30, 4, 4), "OUT");
        var wire = new DiagramConnection(
                new DiagramEndpoint(component.id(), new DiagramPortId("anode")),
                new DiagramEndpoint(junction.id(), ElectricalJunction.RIGHT), "");
        var current = new DiagramBlock(new DiagramDefinition(
                "Rotate x4", new DiagramCanvas(100, 60), List.of(component, junction), List.of(wire)));
        var target = new DiagramElementTarget(0, component.id());

        for (var turn = 0; turn < 4; turn++) {
            current = electricalEditor.rotateClockwise(current, target).diagram();
            assertInside(((ElectricalComponent) current.definition().elements().get(0)).bounds(), current.definition().canvas());
            assertEquals(List.of(wire), current.definition().connections());
            assertTrue(netResolver.resolve(current.definition()).electricallyConnected(wire.source(), wire.target()));
        }
        assertEquals(ElectricalOrientation.DEG_0,
                ((ElectricalComponent) current.definition().elements().get(0)).orientation());
    }

    @Test
    void extremeBulkScaleFactorsStayBoundedAndLeaveElectricalTopologyUntouched() {
        var block = connectedPair();
        var originalConnections = block.definition().connections();
        var endpointA = originalConnections.get(0).source();
        var endpointB = originalConnections.get(0).target();
        var target = new DiagramPropertyTarget(DiagramProperty.CANVAS);

        var huge = electricalEditor.scaleAllComponents(block, target, 1_000.0).diagram();
        var tiny = electricalEditor.scaleAllComponents(huge, target, 0.00001).diagram();

        for (var diagram : List.of(huge, tiny)) {
            assertEquals(originalConnections, diagram.definition().connections());
            diagram.definition().elements().stream()
                    .filter(ElectricalComponent.class::isInstance)
                    .map(ElectricalComponent.class::cast)
                    .forEach(component -> assertInside(component.bounds(), diagram.definition().canvas()));
            assertTrue(netResolver.resolve(diagram.definition()).electricallyConnected(endpointA, endpointB));
        }
    }

    @Test
    void canvasShrinkTranslatesEdgeElementsWithoutChangingConnectionsOrNetMeaning() {
        var source = new ElectricalComponent(
                new DiagramElementId("v1"), new DiagramBounds(112, 50, 18, 30),
                ElectricalComponentKind.DC_VOLTAGE_SOURCE, ElectricalOrientation.DEG_90, "V1", "5 V");
        var junction = new ElectricalJunction(
                new DiagramElementId("j1"), new DiagramBounds(100, 70, 4, 4), "VOUT");
        var wire = new DiagramConnection(
                new DiagramEndpoint(source.id(), new DiagramPortId("positive")),
                new DiagramEndpoint(junction.id(), ElectricalJunction.RIGHT), "");
        var block = new DiagramBlock(new DiagramDefinition(
                "Shrink", new DiagramCanvas(130, 82), List.of(source, junction), List.of(wire)));
        var editor = new DiagramEditor();

        var result = editor.resizeCanvas(block, new DiagramPropertyTarget(DiagramProperty.CANVAS), 120, 75);

        assertTrue(result.changed());
        assertEquals(List.of(wire), result.diagram().definition().connections());
        result.diagram().definition().elements().forEach(element -> assertInside(
                element.bounds(), result.diagram().definition().canvas()));
        assertTrue(netResolver.resolve(result.diagram().definition()).electricallyConnected(wire.source(), wire.target()));
    }

    @Test
    void emptyElectricalDiagramSurvivesMinimumWidthAndViewportExtremes() {
        var block = new DiagramBlock(
                new DiagramDefinition("", new DiagramCanvas(130, 82), List.of(), List.of()),
                0.25);
        for (var viewport : List.of(
                new DiagramViewport(DiagramViewport.MIN_ZOOM, -1_000_000, -1_000_000),
                new DiagramViewport(DiagramViewport.MAX_ZOOM, 1_000_000, 1_000_000))) {
            var layout = layoutEngine.layout(block, 0, 0, 0, 1, measurer, viewport);
            assertTrue(layout.electricalComponents().isEmpty());
            assertTrue(layout.electricalJunctions().isEmpty());
            assertTrue(layout.connections().isEmpty());
            assertTrue(layout.workspaceWidth() >= 1);
            assertTrue(layout.workspaceHeight() >= 1);
        }
    }

    @Test
    void narrowMixedReflowNeverMutatesAuthoredElectricalOrGenericGeometry() {
        var generic = new DiagramNode(
                new DiagramElementId("node"), new DiagramBounds(2.5, 3.5, 20.25, 14.25), "N", List.of());
        var component = new ElectricalComponent(
                new DiagramElementId("r1"), new DiagramBounds(90.25, 40.5, 28, 12),
                ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_180, "R1", "10 kΩ");
        var junction = new ElectricalJunction(
                new DiagramElementId("j1"), new DiagramBounds(70.5, 30.25, 4, 4), "VOUT");
        var elements = List.<DiagramElement>of(generic, component, junction);
        var block = new DiagramBlock(
                new DiagramDefinition("Mixed", new DiagramCanvas(130, 82), elements, List.of()), 0.75);

        layoutEngine.layout(block, 0, 0, 0, 1, measurer,
                new DiagramViewport(DiagramViewport.MAX_ZOOM, 100, 50));
        layoutEngine.layout(block, 0, 0, 0, 640, measurer,
                new DiagramViewport(DiagramViewport.MIN_ZOOM, 20, 20));

        assertEquals(elements, block.definition().elements());
        assertEquals(0.75, block.workspaceAspectRatio());
    }

    @Test
    void derivedNetEndpointIterationFollowsSemanticElementAndPortOrderDeterministically() {
        var resistor = new ElectricalComponent(
                new DiagramElementId("r1"), new DiagramBounds(0, 0, 20, 10),
                ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "");
        var junction = new ElectricalJunction(
                new DiagramElementId("j1"), new DiagramBounds(30, 3, 4, 4), "VOUT");
        var capacitor = new ElectricalComponent(
                new DiagramElementId("c1"), new DiagramBounds(40, 0, 20, 10),
                ElectricalComponentKind.CAPACITOR, ElectricalOrientation.DEG_0, "C1", "");
        var connections = List.of(
                new DiagramConnection(
                        new DiagramEndpoint(resistor.id(), new DiagramPortId("b")),
                        new DiagramEndpoint(junction.id(), ElectricalJunction.LEFT), ""),
                new DiagramConnection(
                        new DiagramEndpoint(junction.id(), ElectricalJunction.RIGHT),
                        new DiagramEndpoint(capacitor.id(), new DiagramPortId("a")), ""));
        var definition = new DiagramDefinition(
                "", new DiagramCanvas(70, 20), List.of(resistor, junction, capacitor), connections);
        var anchor = connections.get(0).source();
        var expected = List.of(
                new DiagramEndpoint(resistor.id(), new DiagramPortId("b")),
                new DiagramEndpoint(junction.id(), ElectricalJunction.LEFT),
                new DiagramEndpoint(junction.id(), ElectricalJunction.RIGHT),
                new DiagramEndpoint(junction.id(), ElectricalJunction.TOP),
                new DiagramEndpoint(junction.id(), ElectricalJunction.BOTTOM),
                new DiagramEndpoint(capacitor.id(), new DiagramPortId("a")));

        var first = new ArrayList<>(netResolver.resolve(definition).netFor(anchor).orElseThrow().endpoints());
        var second = new ArrayList<>(netResolver.resolve(definition).netFor(anchor).orElseThrow().endpoints());

        assertEquals(expected, first);
        assertEquals(first, second);
    }

    @Test
    void chainedElectricalEditsUndoAndRedoThroughExactSemanticSnapshots() {
        var diagram = connectedPair();
        var document = new Document(List.of(paragraph("before"), diagram));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        session.enter();
        var d0 = session.current().document();

        assertTrue(session.rotateElectricalComponentClockwise());
        var d1 = session.current().document();
        assertTrue(session.scaleElectricalSymbols(1.10));
        var d2 = session.current().document();
        assertTrue(session.resizeDiagramCanvas(150, 90));
        var d3 = session.current().document();
        assertTrue(session.applyElectricalComponentAnnotations("R_EDGE", "2.2 kΩ"));
        var d4 = session.current().document();

        assertTrue(session.undo());
        assertEquals(d3, session.current().document());
        assertTrue(session.undo());
        assertEquals(d2, session.current().document());
        assertTrue(session.undo());
        assertEquals(d1, session.current().document());
        assertTrue(session.undo());
        assertEquals(d0, session.current().document());
        assertFalse(session.canUndo());

        assertTrue(session.redo());
        assertEquals(d1, session.current().document());
        assertTrue(session.redo());
        assertEquals(d2, session.current().document());
        assertTrue(session.redo());
        assertEquals(d3, session.current().document());
        assertTrue(session.redo());
        assertEquals(d4, session.current().document());
    }

    private static DiagramBlock connectedPair() {
        var resistor = new ElectricalComponent(
                new DiagramElementId("r1"), new DiagramBounds(72, 48, 28, 12),
                ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "1 kΩ");
        var junction = new ElectricalJunction(
                new DiagramElementId("j1"), new DiagramBounds(50, 30, 4, 4), "VOUT");
        var wire = new DiagramConnection(
                new DiagramEndpoint(resistor.id(), new DiagramPortId("b")),
                new DiagramEndpoint(junction.id(), ElectricalJunction.RIGHT), "");
        return new DiagramBlock(new DiagramDefinition(
                "Connected", new DiagramCanvas(130, 82), List.of(resistor, junction), List.of(wire)));
    }

    private static void assertInside(DiagramBounds bounds, DiagramCanvas canvas) {
        assertTrue(bounds.x() >= -1.0e-9);
        assertTrue(bounds.y() >= -1.0e-9);
        assertTrue(bounds.right() <= canvas.width() + 1.0e-9);
        assertTrue(bounds.bottom() <= canvas.height() + 1.0e-9);
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
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
