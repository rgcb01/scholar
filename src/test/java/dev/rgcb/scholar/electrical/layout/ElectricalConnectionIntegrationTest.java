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

class ElectricalConnectionIntegrationTest {
    private final DiagramLayoutEngine engine = new DiagramLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void wirePathTerminatesAtQuarterTurnLedStableTerminalIds() {
        var sourceId = new DiagramElementId("source");
        var ledId = new DiagramElementId("led1");
        var out = new DiagramPortId("out");
        var block = new DiagramBlock(new DiagramDefinition(
                "Rotated LED",
                new DiagramCanvas(100, 60),
                List.of(
                        new DiagramNode(sourceId, new DiagramBounds(8, 10, 20, 14), "Source", List.of(
                                new DiagramPort(out, "", new DiagramPortPlacement(DiagramPortSide.BOTTOM, 0.5)))),
                        new ElectricalComponent(
                                ledId,
                                new DiagramBounds(48, 20, 14, 28),
                                ElectricalComponentKind.LED,
                                ElectricalOrientation.DEG_90,
                                "D1",
                                "LED")),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(sourceId, out),
                        new DiagramEndpoint(ledId, new DiagramPortId("anode")),
                        "drive"))));

        var layout = engine.layout(block, 0, 0, 0, 240, textMeasurer);
        var led = layout.electricalComponents().get(0);
        var anode = led.ports().stream().filter(port -> port.portId().value().equals("anode")).findFirst().orElseThrow();
        var path = layout.connections().get(0).path();

        assertEquals(DiagramPortSide.TOP, anode.side());
        assertEquals(new LaidOutDiagramPoint(anode.centerX(), anode.centerY()), path.get(path.size() - 1));

        // M18H keeps the semantic endpoint exact but no longer forces a long
        // outward electrical stub when that would point away from the route.
        // The resulting path must stay orthogonal and free of collinear
        // out-and-back spikes at either endpoint.
        for (var index = 1; index < path.size(); index++) {
            var first = path.get(index - 1);
            var second = path.get(index);
            assertTrue(first.x() == second.x() || first.y() == second.y());
        }
        assertTrue(!hasCollinearBacktrack(path));
    }

    @Test
    void allSevenApprovedBasicKindsCanShareOneMixedDiagramLayout() {
        var elements = new java.util.ArrayList<dev.rgcb.scholar.diagram.DiagramElement>();
        elements.add(new DiagramNode(
                new DiagramElementId("node"),
                new DiagramBounds(2, 2, 12, 8),
                "N",
                List.of()));
        var kinds = ElectricalComponentKind.values();
        for (var index = 0; index < kinds.length; index++) {
            elements.add(new ElectricalComponent(
                    new DiagramElementId("e" + index),
                    new DiagramBounds(18 + index * 14, 18, 12, 10),
                    kinds[index],
                    ElectricalOrientation.values()[index % ElectricalOrientation.values().length],
                    "X" + index,
                    ""));
        }
        var block = new DiagramBlock(new DiagramDefinition(
                "All kinds",
                new DiagramCanvas(120, 50),
                elements,
                List.of()));

        var layout = engine.layout(block, 0, 0, 0, 256, textMeasurer);

        assertEquals(1, layout.nodes().size());
        assertEquals(7, layout.electricalComponents().size());
        assertEquals(List.of(kinds), layout.electricalComponents().stream().map(c -> c.kind()).toList());
        assertTrue(layout.electricalComponents().stream().allMatch(c -> !c.primitives().isEmpty()));
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
