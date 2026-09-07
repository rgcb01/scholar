package dev.rgcb.scholar.electrical.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.layout.DiagramLayoutEngine;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.DiagramElementTarget;
import dev.rgcb.scholar.editor.DiagramHitTester;
import dev.rgcb.scholar.editor.TextBoundary;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalJunction;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectricalJunctionLayoutTest {
    private final DiagramLayoutEngine layoutEngine = new DiagramLayoutEngine();
    private final DiagramHitTester hitTester = new DiagramHitTester();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void junctionProducesDotGeometryPortsAndOptionalNetLabel() {
        var junction = new ElectricalJunction(new DiagramElementId("j1"), new DiagramBounds(45, 20, 4, 4), "VOUT");
        var layout = layoutEngine.layout(new DiagramBlock(new DiagramDefinition(
                "net", new DiagramCanvas(100, 50), List.of(junction), List.of())), 0, 0, 0, 240, textMeasurer);

        assertEquals(1, layout.electricalJunctions().size());
        var laidOut = layout.electricalJunctions().get(0);
        assertEquals(4, laidOut.ports().size());
        assertEquals("VOUT", laidOut.netLabel().orElseThrow().text());
        assertEquals(new DiagramElementTarget(0, junction.id()),
                hitTester.hit(layout, laidOut.centerX(), laidOut.centerY()).orElseThrow());
    }

    @Test
    void wireIntoJunctionTerminatesAtJunctionPerimeterWithoutElectricalExitStub() {
        var resistor = new ElectricalComponent(new DiagramElementId("r1"), new DiagramBounds(10, 20, 28, 12),
                ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "1 kΩ");
        var junction = new ElectricalJunction(new DiagramElementId("j1"), new DiagramBounds(60, 24, 4, 4), "");
        var connection = new DiagramConnection(
                new DiagramEndpoint(resistor.id(), new DiagramPortId("b")),
                new DiagramEndpoint(junction.id(), ElectricalJunction.LEFT), "");
        var layout = layoutEngine.layout(new DiagramBlock(new DiagramDefinition(
                "wire", new DiagramCanvas(100, 60), List.of(resistor, junction), List.of(connection))),
                0, 0, 0, 240, textMeasurer);

        var path = layout.connections().get(0).path();
        var junctionPort = layout.electricalJunctions().get(0).ports().stream()
                .filter(port -> port.portId().equals(ElectricalJunction.LEFT)).findFirst().orElseThrow();
        var end = path.get(path.size() - 1);
        assertEquals(junctionPort.centerX(), end.x());
        assertEquals(junctionPort.centerY(), end.y());
        assertTrue(path.size() >= 2);
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
