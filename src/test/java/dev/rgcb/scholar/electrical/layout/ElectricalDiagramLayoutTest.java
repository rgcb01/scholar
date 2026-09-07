package dev.rgcb.scholar.electrical.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramPoint;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.TextBoundary;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectricalDiagramLayoutTest {
    private final DiagramLayoutEngine engine = new DiagramLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void mixedDiagramKeepsGenericNodesAndElectricalComponentsDistinct() {
        var layout = engine.layout(mixedDiagram(), 0, 0, 0, 216, textMeasurer);

        assertEquals(1, layout.nodes().size());
        assertEquals(1, layout.electricalComponents().size());
        assertEquals(ElectricalComponentKind.RESISTOR, layout.electricalComponents().get(0).kind());
    }

    @Test
    void connectionResolvesGenericPortToDerivedElectricalTerminal() {
        var layout = engine.layout(mixedDiagram(), 0, 0, 0, 216, textMeasurer);
        var nodePort = layout.nodes().get(0).ports().get(0);
        var componentPort = layout.electricalComponents().get(0).ports().get(0);
        var path = layout.connections().get(0).path();

        assertEquals(new LaidOutDiagramPoint(nodePort.centerX(), nodePort.centerY()), path.get(0));
        assertEquals(new LaidOutDiagramPoint(componentPort.centerX(), componentPort.centerY()), path.get(path.size() - 1));
    }

    @Test
    void referenceDesignatorAndValueArePositionedByCoreLayout() {
        var layout = engine.layout(mixedDiagram(), 0, 0, 0, 216, textMeasurer);
        var component = layout.electricalComponents().get(0);

        assertEquals("R1", component.referenceDesignator().orElseThrow().text());
        assertEquals("10 kΩ", component.valueLabel().orElseThrow().text());
        assertTrue(!component.primitives().isEmpty());
    }

    @Test
    void allFourM18bSymbolsCanShareOneValidDiagram() {
        var layout = engine.layout(fourComponentDiagram(), 0, 0, 0, 256, textMeasurer);

        assertEquals(4, layout.electricalComponents().size());
        assertEquals(4, layout.connections().size());
        assertEquals(List.of(
                        ElectricalComponentKind.DC_VOLTAGE_SOURCE,
                        ElectricalComponentKind.RESISTOR,
                        ElectricalComponentKind.CAPACITOR,
                        ElectricalComponentKind.GROUND),
                layout.electricalComponents().stream().map(component -> component.kind()).toList());
    }

    @Test
    void responsiveLayoutPreservesLogicalComponentPositionsWhileScalingPixels() {
        var narrow = engine.layout(fourComponentDiagram(), 0, 0, 0, 136, textMeasurer);
        var wide = engine.layout(fourComponentDiagram(), 0, 0, 0, 256, textMeasurer);

        assertEquals(narrow.transform().unmapX(narrow.electricalComponents().get(1).x()),
                wide.transform().unmapX(wide.electricalComponents().get(1).x()), 0.6);
        assertTrue(wide.electricalComponents().get(1).width() > narrow.electricalComponents().get(1).width());
    }


    @Test
    void electricalAnnotationsPreferGuttersThatStayClearOfTerminalLeads() {
        // This regression verifies the preferred annotation gutters, so give the fixture
        // enough width for both side labels. At 256 px the capacitor has only 36 px
        // available on its right, while "100 nF" needs 36 px plus the 4 px gutter;
        // the layout correctly falls back to another side in that constrained case.
        var layout = engine.layout(fourComponentDiagram(), 0, 0, 0, 320, textMeasurer);
        var source = layout.electricalComponents().get(0);
        var resistor = layout.electricalComponents().get(1);
        var capacitor = layout.electricalComponents().get(2);
        var ground = layout.electricalComponents().get(3);

        var sourceReference = source.referenceDesignator().orElseThrow();
        var sourceValue = source.valueLabel().orElseThrow();
        var sourceCenterX = source.x() + source.width() / 2;
        assertTrue(sourceReference.x() + sourceReference.width() <= sourceCenterX - DiagramLayoutEngine.ELECTRICAL_LABEL_GAP);
        assertTrue(sourceValue.x() >= sourceCenterX + DiagramLayoutEngine.ELECTRICAL_LABEL_GAP);

        var resistorReference = resistor.referenceDesignator().orElseThrow();
        var resistorValue = resistor.valueLabel().orElseThrow();
        var resistorCenterY = resistor.y() + resistor.height() / 2;
        assertTrue(resistorReference.y() + resistorReference.height() <= resistorCenterY - DiagramLayoutEngine.ELECTRICAL_LABEL_GAP);
        assertTrue(resistorValue.y() >= resistorCenterY + DiagramLayoutEngine.ELECTRICAL_LABEL_GAP);

        var capacitorReference = capacitor.referenceDesignator().orElseThrow();
        var capacitorValue = capacitor.valueLabel().orElseThrow();
        var capacitorCenterX = capacitor.x() + capacitor.width() / 2;
        assertTrue(capacitorReference.x() + capacitorReference.width() <= capacitorCenterX - DiagramLayoutEngine.ELECTRICAL_LABEL_GAP);
        assertTrue(capacitorValue.x() >= capacitorCenterX + DiagramLayoutEngine.ELECTRICAL_LABEL_GAP);

        var groundReference = ground.referenceDesignator().orElseThrow();
        var groundCenterX = ground.x() + ground.width() / 2;
        assertTrue(groundReference.x() >= groundCenterX + DiagramLayoutEngine.ELECTRICAL_LABEL_GAP);
        assertTrue(!overlaps(capacitorValue, groundReference));
    }

    private static boolean overlaps(
            dev.rgcb.scholar.diagram.layout.LaidOutDiagramLabel first,
            dev.rgcb.scholar.diagram.layout.LaidOutDiagramLabel second
    ) {
        return first.x() < second.x() + second.width()
                && first.x() + first.width() > second.x()
                && first.y() < second.y() + second.height()
                && first.y() + first.height() > second.y();
    }

    private static DiagramBlock mixedDiagram() {
        var nodeId = new DiagramElementId("source-node");
        var resistorId = new DiagramElementId("r1");
        return new DiagramBlock(new DiagramDefinition(
                "Mixed",
                new DiagramCanvas(100, 50),
                List.of(
                        new DiagramNode(
                                nodeId,
                                new DiagramBounds(5, 15, 20, 15),
                                "Input",
                                List.of(new DiagramPort(
                                        new DiagramPortId("out"),
                                        "",
                                        new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)))),
                        new ElectricalComponent(
                                resistorId,
                                new DiagramBounds(55, 17, 30, 10),
                                ElectricalComponentKind.RESISTOR,
                                ElectricalOrientation.DEG_0,
                                "R1",
                                "10 kΩ")),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(nodeId, new DiagramPortId("out")),
                        new DiagramEndpoint(resistorId, new DiagramPortId("a")),
                        "signal"))));
    }

    private static DiagramBlock fourComponentDiagram() {
        var sourceId = new DiagramElementId("v1");
        var resistorId = new DiagramElementId("r1");
        var capacitorId = new DiagramElementId("c1");
        var groundId = new DiagramElementId("gnd");
        return new DiagramBlock(new DiagramDefinition(
                "Basic DC Circuit",
                new DiagramCanvas(120, 75),
                List.of(
                        new ElectricalComponent(sourceId, new DiagramBounds(10, 18, 18, 30),
                                ElectricalComponentKind.DC_VOLTAGE_SOURCE, ElectricalOrientation.DEG_90, "V1", "5 V"),
                        new ElectricalComponent(resistorId, new DiagramBounds(42, 10, 34, 12),
                                ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "10 kΩ"),
                        new ElectricalComponent(capacitorId, new DiagramBounds(88, 24, 14, 26),
                                ElectricalComponentKind.CAPACITOR, ElectricalOrientation.DEG_90, "C1", "100 nF"),
                        new ElectricalComponent(groundId, new DiagramBounds(82, 58, 18, 12),
                                ElectricalComponentKind.GROUND, ElectricalOrientation.DEG_0, "GND", "")),
                List.of(
                        new DiagramConnection(new DiagramEndpoint(sourceId, new DiagramPortId("positive")),
                                new DiagramEndpoint(resistorId, new DiagramPortId("a")), ""),
                        new DiagramConnection(new DiagramEndpoint(resistorId, new DiagramPortId("b")),
                                new DiagramEndpoint(capacitorId, new DiagramPortId("a")), ""),
                        new DiagramConnection(new DiagramEndpoint(capacitorId, new DiagramPortId("b")),
                                new DiagramEndpoint(groundId, new DiagramPortId("ground")), ""),
                        new DiagramConnection(new DiagramEndpoint(sourceId, new DiagramPortId("negative")),
                                new DiagramEndpoint(groundId, new DiagramPortId("ground")), ""))));
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
