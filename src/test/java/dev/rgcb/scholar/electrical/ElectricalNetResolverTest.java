package dev.rgcb.scholar.electrical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramPortId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectricalNetResolverTest {
    private final ElectricalNetResolver resolver = new ElectricalNetResolver();

    @Test
    void explicitJunctionMergesThreeBranchesIntoOneNet() {
        var r1 = component("r1", ElectricalComponentKind.RESISTOR, 10, 10);
        var c1 = component("c1", ElectricalComponentKind.CAPACITOR, 60, 8);
        var d1 = component("d1", ElectricalComponentKind.DIODE, 60, 32);
        var j1 = new ElectricalJunction(new DiagramElementId("j1"), new DiagramBounds(48, 20, 4, 4), "VOUT");
        var definition = new DiagramDefinition("branch", new DiagramCanvas(120, 60), List.of(r1, c1, d1, j1), List.of(
                connection(r1, "b", j1, "left"),
                connection(j1, "right", c1, "a"),
                connection(j1, "bottom", d1, "anode")));

        var connectivity = resolver.resolve(definition);
        var r1b = endpoint(r1, "b");
        var c1a = endpoint(c1, "a");
        var d1a = endpoint(d1, "anode");

        assertTrue(connectivity.electricallyConnected(r1b, c1a));
        assertTrue(connectivity.electricallyConnected(r1b, d1a));
        assertEquals("VOUT", connectivity.netFor(r1b).orElseThrow().label().orElseThrow());
    }

    @Test
    void visualCrossingCannotCreateConnectivityBecauseGeometryIsNotInput() {
        var r1 = component("r1", ElectricalComponentKind.RESISTOR, 10, 10);
        var r2 = component("r2", ElectricalComponentKind.RESISTOR, 10, 35);
        var c1 = component("c1", ElectricalComponentKind.CAPACITOR, 70, 10);
        var c2 = component("c2", ElectricalComponentKind.CAPACITOR, 70, 35);
        var definition = new DiagramDefinition("cross", new DiagramCanvas(120, 60), List.of(r1, r2, c1, c2), List.of(
                connection(r1, "b", c2, "a"),
                connection(r2, "b", c1, "a")));

        var connectivity = resolver.resolve(definition);

        assertFalse(connectivity.electricallyConnected(endpoint(r1, "b"), endpoint(r2, "b")));
        assertEquals(2, connectivity.nets().size());
    }

    @Test
    void allFourPortsOfOneJunctionAreInternallyEquivalent() {
        var junction = new ElectricalJunction(new DiagramElementId("j"), new DiagramBounds(20, 20, 4, 4), "");
        var definition = new DiagramDefinition("j", new DiagramCanvas(50, 50), List.of(junction), List.of());
        var connectivity = resolver.resolve(definition);

        assertEquals(1, connectivity.nets().size());
        assertEquals(4, connectivity.nets().get(0).endpoints().size());
        assertTrue(connectivity.electricallyConnected(endpoint(junction, "left"), endpoint(junction, "top")));
    }

    @Test
    void unconnectedComponentTerminalsAreNotInventedAsStandaloneNets() {
        var resistor = component("r1", ElectricalComponentKind.RESISTOR, 10, 10);
        var definition = new DiagramDefinition("open", new DiagramCanvas(80, 40), List.of(resistor), List.of());

        assertTrue(resolver.resolve(definition).nets().isEmpty());
    }

    @Test
    void conflictingLabelsOnOneConnectedNetAreRejected() {
        var j1 = new ElectricalJunction(new DiagramElementId("j1"), new DiagramBounds(10, 10, 4, 4), "VCC");
        var j2 = new ElectricalJunction(new DiagramElementId("j2"), new DiagramBounds(40, 10, 4, 4), "VOUT");
        var definition = new DiagramDefinition("labels", new DiagramCanvas(80, 40), List.of(j1, j2), List.of(
                new DiagramConnection(endpoint(j1, "right"), endpoint(j2, "left"), "")));

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(definition));
    }

    private static ElectricalComponent component(String id, ElectricalComponentKind kind, double x, double y) {
        return new ElectricalComponent(new DiagramElementId(id), new DiagramBounds(x, y, 28, 12), kind,
                ElectricalOrientation.DEG_0, id.toUpperCase(), "");
    }

    private static DiagramConnection connection(
            dev.rgcb.scholar.diagram.DiagramElement first,
            String firstPort,
            dev.rgcb.scholar.diagram.DiagramElement second,
            String secondPort
    ) {
        return new DiagramConnection(endpoint(first, firstPort), endpoint(second, secondPort), "");
    }

    private static DiagramEndpoint endpoint(dev.rgcb.scholar.diagram.DiagramElement element, String port) {
        return new DiagramEndpoint(element.id(), new DiagramPortId(port));
    }
}
