package dev.rgcb.scholar.electrical;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramElement;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import java.util.List;
import java.util.Objects;

/**
 * Explicit electrical splice point. All four authored perimeter ports are
 * electrically equivalent and are collapsed into one net by
 * {@link ElectricalNetResolver}.
 */
public record ElectricalJunction(
        DiagramElementId id,
        DiagramBounds bounds,
        String netLabel
) implements DiagramElement {
    public static final DiagramPortId LEFT = new DiagramPortId("left");
    public static final DiagramPortId RIGHT = new DiagramPortId("right");
    public static final DiagramPortId TOP = new DiagramPortId("top");
    public static final DiagramPortId BOTTOM = new DiagramPortId("bottom");

    public ElectricalJunction {
        id = Objects.requireNonNull(id, "id");
        bounds = Objects.requireNonNull(bounds, "bounds");
        netLabel = Objects.requireNonNull(netLabel, "netLabel");
    }

    @Override
    public ElectricalJunction withBounds(DiagramBounds bounds) {
        return new ElectricalJunction(id, Objects.requireNonNull(bounds, "bounds"), netLabel);
    }

    public ElectricalJunction withNetLabel(String netLabel) {
        return new ElectricalJunction(id, bounds, Objects.requireNonNull(netLabel, "netLabel"));
    }

    @Override
    public List<DiagramPort> ports() {
        return List.of(
                new DiagramPort(LEFT, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5)),
                new DiagramPort(RIGHT, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)),
                new DiagramPort(TOP, "", new DiagramPortPlacement(DiagramPortSide.TOP, 0.5)),
                new DiagramPort(BOTTOM, "", new DiagramPortPlacement(DiagramPortSide.BOTTOM, 0.5)));
    }
}
