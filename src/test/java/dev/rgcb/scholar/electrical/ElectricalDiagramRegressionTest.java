package dev.rgcb.scholar.electrical;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.clipboard.DiagramPlainTextSerializer;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.DiagramEditor;
import dev.rgcb.scholar.editor.DiagramElementTarget;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectricalDiagramRegressionTest {
    @Test
    void wholeDiagramPlainTextFallbackDoesNotSilentlyDropElectricalComponents() {
        var diagram = diagram();
        var text = new DiagramPlainTextSerializer().serialize(diagram);

        assertTrue(text.contains("Component: RESISTOR R1 [r1]"));
        assertTrue(text.contains("Orientation: DEG_0"));
        assertTrue(text.contains("Value: 10 kΩ"));
        assertTrue(text.contains("Terminal: a [PASSIVE_A, LEFT @ 0.5]"));
    }

    @Test
    void m18dUsesGenericDragWithoutTreatingElectricalComponentAsGenericNodeTextOrDeletion() {
        var diagram = diagram();
        var editor = new DiagramEditor();
        var target = new DiagramElementTarget(0, new DiagramElementId("r1"));

        assertFalse(editor.canEditText(diagram, target));
        assertFalse(editor.canDeleteNode(diagram, target));
        assertTrue(editor.moveElement(diagram, target, 20, 20).changed());
    }

    private static DiagramBlock diagram() {
        return new DiagramBlock(new DiagramDefinition(
                "Electrical",
                new DiagramCanvas(100, 50),
                List.of(new ElectricalComponent(
                        new DiagramElementId("r1"),
                        new DiagramBounds(10, 10, 30, 10),
                        ElectricalComponentKind.RESISTOR,
                        ElectricalOrientation.DEG_0,
                        "R1",
                        "10 kΩ")),
                List.of()));
    }
}
