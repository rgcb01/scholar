package dev.rgcb.scholar.electrical;

import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Stable semantic metadata for the built-in electrical component vocabulary. */
public final class ElectricalComponentCatalog {
    private static final DiagramPortPlacement LEFT_CENTER = new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5);
    private static final DiagramPortPlacement RIGHT_CENTER = new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5);
    private static final DiagramPortPlacement TOP_CENTER = new DiagramPortPlacement(DiagramPortSide.TOP, 0.5);

    private static final Map<ElectricalComponentKind, ElectricalComponentDefinition> DEFINITIONS = definitions();

    private ElectricalComponentCatalog() {
    }

    public static ElectricalComponentDefinition definition(ElectricalComponentKind kind) {
        Objects.requireNonNull(kind, "kind");
        var definition = DEFINITIONS.get(kind);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported electrical component kind: " + kind);
        }
        return definition;
    }

    private static Map<ElectricalComponentKind, ElectricalComponentDefinition> definitions() {
        var definitions = new EnumMap<ElectricalComponentKind, ElectricalComponentDefinition>(ElectricalComponentKind.class);
        definitions.put(ElectricalComponentKind.RESISTOR, new ElectricalComponentDefinition(
                ElectricalComponentKind.RESISTOR,
                "R",
                List.of(
                        terminal("a", ElectricalTerminalRole.PASSIVE_A, LEFT_CENTER),
                        terminal("b", ElectricalTerminalRole.PASSIVE_B, RIGHT_CENTER))));
        definitions.put(ElectricalComponentKind.CAPACITOR, new ElectricalComponentDefinition(
                ElectricalComponentKind.CAPACITOR,
                "C",
                List.of(
                        terminal("a", ElectricalTerminalRole.PASSIVE_A, LEFT_CENTER),
                        terminal("b", ElectricalTerminalRole.PASSIVE_B, RIGHT_CENTER))));
        definitions.put(ElectricalComponentKind.DC_VOLTAGE_SOURCE, new ElectricalComponentDefinition(
                ElectricalComponentKind.DC_VOLTAGE_SOURCE,
                "V",
                List.of(
                        terminal("positive", ElectricalTerminalRole.POSITIVE, LEFT_CENTER),
                        terminal("negative", ElectricalTerminalRole.NEGATIVE, RIGHT_CENTER))));
        definitions.put(ElectricalComponentKind.GROUND, new ElectricalComponentDefinition(
                ElectricalComponentKind.GROUND,
                "GND",
                List.of(terminal("ground", ElectricalTerminalRole.GROUND, TOP_CENTER))));
        definitions.put(ElectricalComponentKind.DIODE, new ElectricalComponentDefinition(
                ElectricalComponentKind.DIODE,
                "D",
                List.of(
                        terminal("anode", ElectricalTerminalRole.ANODE, LEFT_CENTER),
                        terminal("cathode", ElectricalTerminalRole.CATHODE, RIGHT_CENTER))));
        definitions.put(ElectricalComponentKind.LED, new ElectricalComponentDefinition(
                ElectricalComponentKind.LED,
                "D",
                List.of(
                        terminal("anode", ElectricalTerminalRole.ANODE, LEFT_CENTER),
                        terminal("cathode", ElectricalTerminalRole.CATHODE, RIGHT_CENTER))));
        definitions.put(ElectricalComponentKind.SWITCH_SPST, new ElectricalComponentDefinition(
                ElectricalComponentKind.SWITCH_SPST,
                "S",
                List.of(
                        terminal("a", ElectricalTerminalRole.PASSIVE_A, LEFT_CENTER),
                        terminal("b", ElectricalTerminalRole.PASSIVE_B, RIGHT_CENTER))));
        return Map.copyOf(definitions);
    }

    private static ElectricalTerminalDefinition terminal(
            String id,
            ElectricalTerminalRole role,
            DiagramPortPlacement placement
    ) {
        return new ElectricalTerminalDefinition(new DiagramPortId(id), role, placement);
    }
}
