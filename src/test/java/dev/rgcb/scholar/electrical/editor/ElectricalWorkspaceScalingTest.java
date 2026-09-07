package dev.rgcb.scholar.electrical.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.DiagramElementTarget;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalJunction;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectricalWorkspaceScalingTest {
    private final ElectricalDiagramEditor editor = new ElectricalDiagramEditor();

    @Test
    void globalSymbolScaleChangesComponentsOnlyAndPreservesTopology() {
        var resistor = new ElectricalComponent(
                new DiagramElementId("r1"),
                new DiagramBounds(20, 20, 28, 12),
                ElectricalComponentKind.RESISTOR,
                ElectricalOrientation.DEG_0,
                "R1",
                "1 kΩ");
        var junction = new ElectricalJunction(
                new DiagramElementId("junction-1"),
                new DiagramBounds(70, 24, 4, 4),
                "VOUT");
        var wire = new DiagramConnection(
                new DiagramEndpoint(resistor.id(), new DiagramPortId("b")),
                new DiagramEndpoint(junction.id(), new DiagramPortId("left")),
                "");
        var block = new DiagramBlock(
                new DiagramDefinition("", new DiagramCanvas(120, 70), List.of(resistor, junction), List.of(wire)),
                0.8);

        var result = editor.scaleAllComponents(block, new DiagramElementTarget(0, resistor.id()), 1.10);
        var scaled = (ElectricalComponent) result.diagram().definition().elements().get(0);

        assertTrue(result.changed());
        assertEquals(30.8, scaled.bounds().width(), 1.0e-9);
        assertEquals(13.2, scaled.bounds().height(), 1.0e-9);
        assertEquals(resistor.orientation(), scaled.orientation());
        assertEquals(resistor.referenceDesignator(), scaled.referenceDesignator());
        assertEquals(resistor.valueLabel(), scaled.valueLabel());
        assertEquals(junction, result.diagram().definition().elements().get(1));
        assertEquals(List.of(wire), result.diagram().definition().connections());
        assertEquals(0.8, result.diagram().workspaceAspectRatio());
    }
}
