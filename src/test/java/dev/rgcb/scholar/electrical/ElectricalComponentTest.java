package dev.rgcb.scholar.electrical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectricalComponentTest {
    @Test
    void resistorOwnsStablePassiveTerminalIds() {
        var component = component(ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0);

        assertEquals(List.of("a", "b"), component.ports().stream().map(port -> port.id().value()).toList());
        assertEquals(DiagramPortSide.LEFT, component.ports().get(0).placement().side());
        assertEquals(DiagramPortSide.RIGHT, component.ports().get(1).placement().side());
    }

    @Test
    void dcSourceCatalogPreservesPositiveAndNegativeSemanticRoles() {
        var definition = ElectricalComponentCatalog.definition(ElectricalComponentKind.DC_VOLTAGE_SOURCE);

        assertEquals("V", definition.defaultReferencePrefix());
        assertEquals(ElectricalTerminalRole.POSITIVE, definition.terminals().get(0).role());
        assertEquals("positive", definition.terminals().get(0).id().value());
        assertEquals(ElectricalTerminalRole.NEGATIVE, definition.terminals().get(1).role());
        assertEquals("negative", definition.terminals().get(1).id().value());
    }

    @Test
    void groundOwnsOneStableGroundTerminal() {
        var component = component(ElectricalComponentKind.GROUND, ElectricalOrientation.DEG_0);

        assertEquals(1, component.ports().size());
        assertEquals("ground", component.ports().get(0).id().value());
        assertEquals(DiagramPortSide.TOP, component.ports().get(0).placement().side());
    }

    @Test
    void quarterTurnOrientationMovesPlacementButPreservesTerminalIdentity() {
        var zero = component(ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0);
        var ninety = component(ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_90);
        var oneEighty = component(ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_180);
        var twoSeventy = component(ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_270);

        assertEquals(zero.ports().stream().map(port -> port.id()).toList(), ninety.ports().stream().map(port -> port.id()).toList());
        assertEquals(DiagramPortSide.TOP, ninety.ports().get(0).placement().side());
        assertEquals(DiagramPortSide.BOTTOM, ninety.ports().get(1).placement().side());
        assertEquals(DiagramPortSide.RIGHT, oneEighty.ports().get(0).placement().side());
        assertEquals(DiagramPortSide.LEFT, oneEighty.ports().get(1).placement().side());
        assertEquals(DiagramPortSide.BOTTOM, twoSeventy.ports().get(0).placement().side());
        assertEquals(DiagramPortSide.TOP, twoSeventy.ports().get(1).placement().side());
    }

    @Test
    void nonCenteredPortRotationPreservesCorrectPerimeterOffset() {
        var topQuarter = new DiagramPortPlacement(DiagramPortSide.TOP, 0.25);
        var bottomQuarter = new DiagramPortPlacement(DiagramPortSide.BOTTOM, 0.25);
        var leftQuarter = new DiagramPortPlacement(DiagramPortSide.LEFT, 0.25);

        assertEquals(new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.25),
                ElectricalComponent.rotate(topQuarter, ElectricalOrientation.DEG_90));
        assertEquals(new DiagramPortPlacement(DiagramPortSide.LEFT, 0.25),
                ElectricalComponent.rotate(bottomQuarter, ElectricalOrientation.DEG_90));
        assertEquals(new DiagramPortPlacement(DiagramPortSide.TOP, 0.75),
                ElectricalComponent.rotate(leftQuarter, ElectricalOrientation.DEG_90));
    }

    @Test
    void diodeAndLedExposeStableAnodeCathodeTerminals() {
        for (var kind : List.of(ElectricalComponentKind.DIODE, ElectricalComponentKind.LED)) {
            var definition = ElectricalComponentCatalog.definition(kind);
            var component = component(kind, ElectricalOrientation.DEG_0);

            assertEquals(List.of("anode", "cathode"),
                    component.ports().stream().map(port -> port.id().value()).toList());
            assertEquals(ElectricalTerminalRole.ANODE, definition.terminals().get(0).role());
            assertEquals(ElectricalTerminalRole.CATHODE, definition.terminals().get(1).role());
        }
    }

    @Test
    void spstSwitchUsesStablePassiveTerminalsAndReferencePrefix() {
        var definition = ElectricalComponentCatalog.definition(ElectricalComponentKind.SWITCH_SPST);
        var component = component(ElectricalComponentKind.SWITCH_SPST, ElectricalOrientation.DEG_0);

        assertEquals("S", definition.defaultReferencePrefix());
        assertEquals(List.of("a", "b"), component.ports().stream().map(port -> port.id().value()).toList());
    }

    @Test
    void diodeQuarterTurnsPreserveTerminalIdsWhileMovingTerminalSides() {
        var zero = component(ElectricalComponentKind.DIODE, ElectricalOrientation.DEG_0);
        var ninety = component(ElectricalComponentKind.DIODE, ElectricalOrientation.DEG_90);

        assertEquals(zero.ports().stream().map(port -> port.id()).toList(),
                ninety.ports().stream().map(port -> port.id()).toList());
        assertEquals(DiagramPortSide.TOP, ninety.ports().get(0).placement().side());
        assertEquals(DiagramPortSide.BOTTOM, ninety.ports().get(1).placement().side());
    }

    @Test
    void annotationsAreAuthoredStringsAndNullsAreRejected() {
        var blank = new ElectricalComponent(
                new DiagramElementId("c"),
                new DiagramBounds(0, 0, 10, 10),
                ElectricalComponentKind.CAPACITOR,
                ElectricalOrientation.DEG_0,
                "",
                "");

        assertTrue(blank.referenceDesignator().isEmpty());
        assertTrue(blank.valueLabel().isEmpty());
        assertThrows(NullPointerException.class, () -> new ElectricalComponent(
                new DiagramElementId("bad"),
                new DiagramBounds(0, 0, 10, 10),
                null,
                ElectricalOrientation.DEG_0,
                "C1",
                "1 uF"));
    }

    private static ElectricalComponent component(ElectricalComponentKind kind, ElectricalOrientation orientation) {
        return new ElectricalComponent(
                new DiagramElementId("component"),
                new DiagramBounds(0, 0, 20, 10),
                kind,
                orientation,
                "X1",
                "value");
    }
}
