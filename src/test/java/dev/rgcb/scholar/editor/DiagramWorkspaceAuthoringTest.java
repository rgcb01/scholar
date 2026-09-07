package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.document.DiagramBlock;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagramWorkspaceAuthoringTest {
    private final DiagramEditor editor = new DiagramEditor();
    private final DiagramEditTarget canvasTarget = new DiagramPropertyTarget(DiagramProperty.CANVAS);

    @Test
    void canvasResizeIsSemanticAndClampsElementsInsideNewExtent() {
        var block = diagram();
        var result = editor.resizeCanvas(block, canvasTarget, 70, 40);
        var node = result.diagram().definition().elements().get(0);

        assertTrue(result.changed());
        assertEquals(new DiagramCanvas(70, 40), result.diagram().definition().canvas());
        assertEquals(50.0, node.bounds().x());
        assertEquals(25.0, node.bounds().y());
        assertEquals(block.workspaceAspectRatio(), result.diagram().workspaceAspectRatio());
    }

    @Test
    void canvasResizeRejectsExtentSmallerThanExistingElement() {
        var block = diagram();
        var result = editor.resizeCanvas(block, canvasTarget, 10, 10);

        assertFalse(result.changed());
        assertEquals(block, result.diagram());
    }

    @Test
    void workspaceHeightCanChangeAndResetWithoutChangingCanvas() {
        var block = diagram();
        var taller = editor.scaleWorkspaceHeight(block, canvasTarget, 1.10);

        assertTrue(taller.changed());
        assertEquals(block.definition(), taller.diagram().definition());
        assertTrue(taller.diagram().workspaceAspectRatio() > block.workspaceAspectRatio());

        var reset = editor.resetWorkspaceHeight(taller.diagram(), canvasTarget);
        assertTrue(reset.changed());
        assertEquals(DiagramBlock.defaultWorkspaceAspectRatio(block.definition()), reset.diagram().workspaceAspectRatio());
    }

    private static DiagramBlock diagram() {
        return new DiagramBlock(new DiagramDefinition(
                "",
                new DiagramCanvas(100, 50),
                List.of(new DiagramNode(
                        new DiagramElementId("node"),
                        new DiagramBounds(80, 35, 20, 15),
                        "N",
                        List.of())),
                List.of()));
    }
}
