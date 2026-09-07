package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.layout.DiagramLayoutEngine;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectricalDiagramHitTesterTest {
    private final DiagramLayoutEngine layoutEngine = new DiagramLayoutEngine();
    private final DiagramHitTester hitTester = new DiagramHitTester();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void electricalTerminalWinsOverOwningComponentAtPerimeter() {
        var layout = layout();
        var component = layout.electricalComponents().get(0);
        var port = component.ports().get(0);

        var target = hitTester.hit(layout, port.centerX(), port.centerY()).orElseThrow();

        assertEquals(new DiagramPortTarget(
                component.elementIndex(),
                port.portIndex(),
                component.elementId(),
                new DiagramPortId("anode")), target);
    }

    @Test
    void clickOnElectricalSymbolBodyMapsToElementTarget() {
        var layout = layout();
        var component = layout.electricalComponents().get(0);

        var target = hitTester.hit(
                layout,
                component.x() + component.width() / 2,
                component.y() + component.height() / 2).orElseThrow();

        assertEquals(new DiagramElementTarget(component.elementIndex(), component.elementId()), target);
    }


    @Test
    void authoredInteractionBoundsRemainForgivingAroundCompactSymbolGeometry() {
        var layout = layout();
        var component = layout.electricalComponents().get(0);
        var expected = new DiagramElementTarget(component.elementIndex(), component.elementId());

        var target = hitTester.hit(layout, component.x() + 2, component.y() + 2).orElseThrow();

        assertEquals(expected, target);
    }

    @Test
    void electricalReferenceAndValueLabelsMapToOwningComponent() {
        var layout = layout();
        var component = layout.electricalComponents().get(0);
        var expected = new DiagramElementTarget(component.elementIndex(), component.elementId());
        var reference = component.referenceDesignator().orElseThrow();
        var value = component.valueLabel().orElseThrow();

        assertEquals(expected, hitTester.hit(layout, reference.x() + 1, reference.y() + 1).orElseThrow());
        assertEquals(expected, hitTester.hit(layout, value.x() + 1, value.y() + 1).orElseThrow());
    }

    @Test
    void rotatedElectricalTerminalHitKeepsStableSemanticPortId() {
        var layout = layout();
        var led = layout.electricalComponents().get(1);
        var anode = led.ports().stream().filter(port -> port.portId().value().equals("anode")).findFirst().orElseThrow();

        var target = hitTester.hit(layout, anode.centerX(), anode.centerY()).orElseThrow();

        assertEquals(new DiagramPortTarget(
                led.elementIndex(),
                anode.portIndex(),
                led.elementId(),
                new DiagramPortId("anode")), target);
    }

    private dev.rgcb.scholar.diagram.layout.LaidOutDiagram layout() {
        var block = new DiagramBlock(new DiagramDefinition(
                "Electrical hits",
                new DiagramCanvas(100, 55),
                List.of(
                        new ElectricalComponent(
                                new DiagramElementId("d1"),
                                new DiagramBounds(12, 16, 30, 12),
                                ElectricalComponentKind.DIODE,
                                ElectricalOrientation.DEG_0,
                                "D1",
                                "1N4148"),
                        new ElectricalComponent(
                                new DiagramElementId("led1"),
                                new DiagramBounds(62, 10, 14, 30),
                                ElectricalComponentKind.LED,
                                ElectricalOrientation.DEG_90,
                                "D2",
                                "LED")),
                List.of()));
        return layoutEngine.layout(block, 2, 0, 20, 260, textMeasurer);
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
