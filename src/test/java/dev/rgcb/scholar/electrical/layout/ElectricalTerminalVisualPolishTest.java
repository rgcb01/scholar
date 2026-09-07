package dev.rgcb.scholar.electrical.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.layout.DiagramConnectionRouter;
import dev.rgcb.scholar.diagram.layout.DiagramLayoutEngine;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramPoint;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.DiagramHitTester;
import dev.rgcb.scholar.editor.DiagramPortTarget;
import dev.rgcb.scholar.editor.TextBoundary;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalJunction;
import dev.rgcb.scholar.electrical.ElectricalNetResolver;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Final M18 visual-polish regressions for electrical terminal/wire joins. */
class ElectricalTerminalVisualPolishTest {
    private final DiagramLayoutEngine layoutEngine = new DiagramLayoutEngine();
    private final TextMeasurer measurer = new FixedTextMeasurer();

    @Test
    void electricalExitReservationIsCompactInsteadOfLongerThanGenericRouting() {
        assertEquals(DiagramConnectionRouter.PORT_EXIT_LENGTH, DiagramLayoutEngine.ELECTRICAL_PORT_EXIT_LENGTH);
        assertTrue(DiagramLayoutEngine.ELECTRICAL_PORT_EXIT_LENGTH < 14);
    }

    @Test
    void alignedVoltageSourceToResistorRouteHasNoTerminalSpike() {
        var source = new ElectricalComponent(
                new DiagramElementId("v1"), new DiagramBounds(8, 24, 18, 30),
                ElectricalComponentKind.DC_VOLTAGE_SOURCE, ElectricalOrientation.DEG_90, "V1", "5 V");
        var resistor = new ElectricalComponent(
                new DiagramElementId("r1"), new DiagramBounds(34, 18, 28, 12),
                ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "1 kΩ");
        var connection = new DiagramConnection(
                endpoint(source, "positive"), endpoint(resistor, "a"), "");
        var layout = layout(source, resistor, connection);
        var route = layout.connections().get(0).path();

        var sourcePort = layout.electricalComponents().get(0).ports().stream()
                .filter(port -> port.portId().value().equals("positive")).findFirst().orElseThrow();
        var resistorPort = layout.electricalComponents().get(1).ports().stream()
                .filter(port -> port.portId().value().equals("a")).findFirst().orElseThrow();

        assertEquals(List.of(
                new LaidOutDiagramPoint(sourcePort.centerX(), sourcePort.centerY()),
                new LaidOutDiagramPoint(resistorPort.centerX(), resistorPort.centerY())), route);
    }

    @Test
    void junctionToTopFacingCapacitorStopsAtTerminalInsteadOfOvershootingAboveIt() {
        var junction = new ElectricalJunction(new DiagramElementId("j1"), new DiagramBounds(67, 24, 4, 4), "VOUT");
        var capacitor = new ElectricalComponent(
                new DiagramElementId("c1"), new DiagramBounds(82, 14, 14, 28),
                ElectricalComponentKind.CAPACITOR, ElectricalOrientation.DEG_90, "C1", "100 nF");
        var connection = new DiagramConnection(
                new DiagramEndpoint(junction.id(), ElectricalJunction.RIGHT), endpoint(capacitor, "a"), "");
        var layout = layout(junction, capacitor, connection);
        var route = layout.connections().get(0).path();
        var terminal = layout.electricalComponents().get(0).ports().stream()
                .filter(port -> port.portId().value().equals("a")).findFirst().orElseThrow();

        assertEquals(new LaidOutDiagramPoint(terminal.centerX(), terminal.centerY()), route.get(route.size() - 1));
        assertTrue(route.stream().allMatch(point -> point.y() >= terminal.centerY()));
        assertTrue(!hasCollinearBacktrack(route));
    }

    @Test
    void rightFacingDiodeTowardLeftGroundDoesNotProtrudePastCathode() {
        var diode = new ElectricalComponent(
                new DiagramElementId("d1"), new DiagramBounds(82, 48, 28, 12),
                ElectricalComponentKind.DIODE, ElectricalOrientation.DEG_0, "D1", "1N4148");
        var ground = new ElectricalComponent(
                new DiagramElementId("gnd"), new DiagramBounds(52, 66, 18, 12),
                ElectricalComponentKind.GROUND, ElectricalOrientation.DEG_0, "GND", "");
        var connection = new DiagramConnection(endpoint(diode, "cathode"), endpoint(ground, "ground"), "");
        var layout = layout(diode, ground, connection);
        var route = layout.connections().get(0).path();
        var cathode = layout.electricalComponents().get(0).ports().stream()
                .filter(port -> port.portId().value().equals("cathode")).findFirst().orElseThrow();

        assertEquals(new LaidOutDiagramPoint(cathode.centerX(), cathode.centerY()), route.get(0));
        assertTrue(route.stream().allMatch(point -> point.x() <= cathode.centerX()));
        assertTrue(!hasCollinearBacktrack(route));
    }

    @Test
    void visualPolishPreservesTerminalHitTargetAndSemanticNet() {
        var resistor = new ElectricalComponent(
                new DiagramElementId("r1"), new DiagramBounds(10, 20, 28, 12),
                ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "1 kΩ");
        var junction = new ElectricalJunction(new DiagramElementId("j1"), new DiagramBounds(60, 24, 4, 4), "N1");
        var resistorEndpoint = endpoint(resistor, "b");
        var junctionEndpoint = new DiagramEndpoint(junction.id(), ElectricalJunction.LEFT);
        var connection = new DiagramConnection(resistorEndpoint, junctionEndpoint, "");
        var definition = new DiagramDefinition(
                "net", new DiagramCanvas(100, 60), List.of(resistor, junction), List.of(connection));
        var layout = layoutEngine.layout(new DiagramBlock(definition), 0, 0, 0, 320, measurer);
        var terminal = layout.electricalComponents().get(0).ports().stream()
                .filter(port -> port.portId().value().equals("b")).findFirst().orElseThrow();

        var hit = new DiagramHitTester().hit(layout, terminal.centerX(), terminal.centerY()).orElseThrow();
        var portTarget = assertInstanceOf(DiagramPortTarget.class, hit);
        assertEquals(resistor.id(), portTarget.elementId());
        assertEquals(new DiagramPortId("b"), portTarget.portId());
        assertTrue(new ElectricalNetResolver().resolve(definition)
                .electricallyConnected(resistorEndpoint, junctionEndpoint));
    }

    @Test
    void m18h1ShortensVisibleSymbolLegsWithoutMovingLogicalPorts() {
        var resistor = new ElectricalComponent(
                new DiagramElementId("r-short"), new DiagramBounds(10, 10, 28, 12),
                ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "1 kΩ");
        var definition = new DiagramDefinition(
                "short legs", new DiagramCanvas(80, 50), List.of(resistor), List.of());
        var layout = layoutEngine.layout(new DiagramBlock(definition), 0, 0, 0, 400, measurer);
        var component = layout.electricalComponents().get(0);
        var leftPort = component.ports().stream().filter(port -> port.portId().value().equals("a")).findFirst().orElseThrow();
        var rightPort = component.ports().stream().filter(port -> port.portId().value().equals("b")).findFirst().orElseThrow();

        // Logical anchors remain on the exact component perimeter.
        assertEquals(component.x(), leftPort.centerX());
        assertEquals(component.x() + component.width(), rightPort.centerX());

        // Symbol body begins close to each perimeter, leaving only short visible leads.
        var first = assertInstanceOf(LaidOutElectricalLine.class, component.primitives().get(0));
        var last = assertInstanceOf(LaidOutElectricalLine.class, component.primitives().get(component.primitives().size() - 1));
        assertTrue(first.end().x() - first.start().x() < component.width() / 4);
        assertTrue(last.end().x() - last.start().x() < component.width() / 4);
    }

    private dev.rgcb.scholar.diagram.layout.LaidOutDiagram layout(
            dev.rgcb.scholar.diagram.DiagramElement first,
            dev.rgcb.scholar.diagram.DiagramElement second,
            DiagramConnection connection
    ) {
        return layoutEngine.layout(new DiagramBlock(new DiagramDefinition(
                "polish", new DiagramCanvas(130, 82), List.of(first, second), List.of(connection))),
                0, 0, 0, 1095, measurer);
    }

    private static DiagramEndpoint endpoint(ElectricalComponent component, String portId) {
        return new DiagramEndpoint(component.id(), new DiagramPortId(portId));
    }

    private static boolean hasCollinearBacktrack(List<LaidOutDiagramPoint> path) {
        for (var index = 2; index < path.size(); index++) {
            var first = path.get(index - 2);
            var middle = path.get(index - 1);
            var end = path.get(index);
            var vertical = first.x() == middle.x() && middle.x() == end.x();
            var horizontal = first.y() == middle.y() && middle.y() == end.y();
            if (!vertical && !horizontal) {
                continue;
            }
            var before = vertical ? middle.y() - first.y() : middle.x() - first.x();
            var after = vertical ? end.y() - middle.y() : end.x() - middle.x();
            if (Integer.signum(before) != 0 && Integer.signum(after) != 0
                    && Integer.signum(before) != Integer.signum(after)) {
                return true;
            }
        }
        return false;
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
