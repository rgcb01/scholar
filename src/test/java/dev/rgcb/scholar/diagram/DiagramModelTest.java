package dev.rgcb.scholar.diagram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagramModelTest {
    @Test
    void validDefinitionPreservesSemanticConnectivity() {
        var definition = fixture();

        assertEquals("System", definition.title());
        assertEquals(2, definition.elements().size());
        assertEquals(1, definition.connections().size());
        assertEquals(new DiagramEndpoint(new DiagramElementId("a"), new DiagramPortId("out")),
                definition.connections().get(0).source());
    }

    @Test
    void canvasRequiresFinitePositiveDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new DiagramCanvas(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new DiagramCanvas(10, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new DiagramCanvas(Double.POSITIVE_INFINITY, 10));
    }

    @Test
    void boundsRequireFinitePositiveDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new DiagramBounds(0, 0, 0, 5));
        assertThrows(IllegalArgumentException.class, () -> new DiagramBounds(Double.NaN, 0, 5, 5));
    }

    @Test
    void portPlacementRequiresNormalizedFiniteOffset() {
        assertThrows(IllegalArgumentException.class,
                () -> new DiagramPortPlacement(DiagramPortSide.LEFT, -0.01));
        assertThrows(IllegalArgumentException.class,
                () -> new DiagramPortPlacement(DiagramPortSide.RIGHT, 1.01));
        assertThrows(IllegalArgumentException.class,
                () -> new DiagramPortPlacement(DiagramPortSide.TOP, Double.NaN));
    }

    @Test
    void elementAndPortIdsMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new DiagramElementId(" "));
        assertThrows(IllegalArgumentException.class, () -> new DiagramPortId(""));
    }

    @Test
    void duplicateElementIdsAreRejected() {
        var id = new DiagramElementId("same");
        var first = node(id, 5, "p1", DiagramPortSide.RIGHT);
        var second = new DiagramNode(id, new DiagramBounds(50, 5, 20, 15), "B", List.of());

        assertThrows(IllegalArgumentException.class,
                () -> new DiagramDefinition("", new DiagramCanvas(100, 50), List.of(first, second), List.of()));
    }

    @Test
    void duplicatePortIdsWithinNodeAreRejected() {
        var id = new DiagramPortId("p");
        var portA = new DiagramPort(id, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.25));
        var portB = new DiagramPort(id, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.75));

        assertThrows(IllegalArgumentException.class,
                () -> new DiagramNode(new DiagramElementId("n"), new DiagramBounds(0, 0, 20, 10), "", List.of(portA, portB)));
    }

    @Test
    void elementBoundsMustRemainInsideCanvas() {
        var outside = new DiagramNode(
                new DiagramElementId("outside"),
                new DiagramBounds(90, 10, 20, 10),
                "",
                List.of());

        assertThrows(IllegalArgumentException.class,
                () -> new DiagramDefinition("", new DiagramCanvas(100, 50), List.of(outside), List.of()));
    }

    @Test
    void danglingConnectionEndpointsAreRejected() {
        var first = node(new DiagramElementId("a"), 5, "out", DiagramPortSide.RIGHT);
        var connection = new DiagramConnection(
                new DiagramEndpoint(first.id(), new DiagramPortId("out")),
                new DiagramEndpoint(new DiagramElementId("missing"), new DiagramPortId("in")),
                "");

        assertThrows(IllegalArgumentException.class,
                () -> new DiagramDefinition("", new DiagramCanvas(100, 50), List.of(first), List.of(connection)));
    }

    @Test
    void connectionCannotTargetSameEndpoint() {
        var endpoint = new DiagramEndpoint(new DiagramElementId("a"), new DiagramPortId("p"));
        assertThrows(IllegalArgumentException.class, () -> new DiagramConnection(endpoint, endpoint, ""));
    }

    @Test
    void semanticListsAreDefensiveImmutableCopies() {
        var elements = new ArrayList<DiagramElement>();
        elements.add(node(new DiagramElementId("a"), 5, "p", DiagramPortSide.RIGHT));
        var definition = new DiagramDefinition("", new DiagramCanvas(100, 50), elements, List.of());
        elements.clear();

        assertEquals(1, definition.elements().size());
        assertThrows(UnsupportedOperationException.class, () -> definition.elements().clear());
    }

    private static DiagramDefinition fixture() {
        var a = node(new DiagramElementId("a"), 5, "out", DiagramPortSide.RIGHT);
        var b = new DiagramNode(
                new DiagramElementId("b"),
                new DiagramBounds(65, 5, 20, 15),
                "B",
                List.of(new DiagramPort(
                        new DiagramPortId("in"),
                        "",
                        new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5))));
        return new DiagramDefinition(
                "System",
                new DiagramCanvas(100, 50),
                List.of(a, b),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(a.id(), new DiagramPortId("out")),
                        new DiagramEndpoint(b.id(), new DiagramPortId("in")),
                        "signal")));
    }

    private static DiagramNode node(DiagramElementId id, double x, String portId, DiagramPortSide side) {
        return new DiagramNode(
                id,
                new DiagramBounds(x, 5, 20, 15),
                id.value(),
                List.of(new DiagramPort(
                        new DiagramPortId(portId),
                        "",
                        new DiagramPortPlacement(side, 0.5))));
    }
}
