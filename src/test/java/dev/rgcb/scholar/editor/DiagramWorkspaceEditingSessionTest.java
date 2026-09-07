package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiagramWorkspaceEditingSessionTest {
    @Test
    void canvasResizeIsOneUndoableSemanticEdit() {
        var session = editingSession(diagram());
        var before = session.current().document();

        assertTrue(session.resizeDiagramCanvas(180, 100));
        assertEquals(new DiagramCanvas(180, 100), currentDiagram(session).definition().canvas());
        assertTrue(session.canUndo());
        assertTrue(session.undo());
        assertEquals(before, session.current().document());
    }

    @Test
    void workspaceHeightChangeAndResetAreUndoable() {
        var session = editingSession(diagram());
        var originalAspect = currentDiagram(session).workspaceAspectRatio();

        assertTrue(session.scaleDiagramWorkspaceHeight(1.10));
        assertEquals(originalAspect * 1.10, currentDiagram(session).workspaceAspectRatio(), 1.0e-9);
        assertTrue(session.undo());
        assertEquals(originalAspect, currentDiagram(session).workspaceAspectRatio(), 1.0e-9);

        assertTrue(session.scaleDiagramWorkspaceHeight(1.10));
        assertTrue(session.resetDiagramWorkspaceHeight());
        assertEquals(originalAspect, currentDiagram(session).workspaceAspectRatio(), 1.0e-9);
    }

    @Test
    void globalElectricalSymbolScaleIsUndoableWithoutChangingCanvas() {
        var session = editingSession(diagram());
        var before = session.current().document();
        var canvas = currentDiagram(session).definition().canvas();
        var oldBounds = component(session).bounds();

        assertTrue(session.supportsScaleElectricalSymbols());
        assertTrue(session.scaleElectricalSymbols(1.10));
        var scaled = component(session).bounds();
        assertEquals(oldBounds.width() * 1.10, scaled.width(), 1.0e-9);
        assertEquals(oldBounds.height() * 1.10, scaled.height(), 1.0e-9);
        assertEquals(canvas, currentDiagram(session).definition().canvas());
        assertTrue(session.undo());
        assertEquals(before, session.current().document());
    }

    private static EditorSession editingSession(DiagramBlock block) {
        var document = new Document(List.of(paragraph("Before"), block));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        session.enter();
        session.setDiagramEditingTarget(new DiagramPropertyTarget(DiagramProperty.CANVAS));
        return session;
    }

    private static DiagramBlock currentDiagram(EditorSession session) {
        return (DiagramBlock) session.current().document().blocks().get(1);
    }

    private static ElectricalComponent component(EditorSession session) {
        return (ElectricalComponent) currentDiagram(session).definition().elements().get(0);
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }

    private static DiagramBlock diagram() {
        var component = new ElectricalComponent(
                new DiagramElementId("r1"),
                new DiagramBounds(35, 24, 30, 12),
                ElectricalComponentKind.RESISTOR,
                ElectricalOrientation.DEG_0,
                "R1",
                "1 kΩ");
        return new DiagramBlock(new DiagramDefinition(
                "Workspace",
                new DiagramCanvas(120, 70),
                List.of(component),
                List.of()));
    }
}
