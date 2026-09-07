package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiagramEditingSessionTest {
    @Test
    void editorStateValidatesDiagramEditingTargetAgainstBlock() {
        var document = document(diagram());
        var valid = new DiagramEditingSelection(
                0, new DiagramElementTarget(0, new DiagramElementId("sensor")));

        var state = new EditorState(document, valid, Optional.empty());

        assertEquals(valid, state.selection());
        assertThrows(IllegalArgumentException.class, () -> new EditorState(
                document,
                new DiagramEditingSelection(0, new DiagramElementTarget(0, new DiagramElementId("wrong"))),
                Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new EditorState(
                document(paragraph("not a diagram")),
                valid,
                Optional.empty()));
    }

    @Test
    void enterFromAtomicDiagramSelectsFirstElementAndEscapeReturnsToBlockSelection() {
        var document = document(paragraph("Before"), diagram(), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));

        assertFalse(session.enter());
        var editing = assertInstanceOf(DiagramEditingSelection.class, session.current().selection());
        assertEquals(1, editing.blockIndex());
        assertEquals(new DiagramElementTarget(0, new DiagramElementId("sensor")), editing.target());

        session.exitDiagramEditing();

        assertEquals(new BlockSelection(1), session.current().selection());
    }

    @Test
    void tabStyleTargetTraversalChangesSelectionWithoutCreatingHistory() {
        var document = document(paragraph("Before"), diagram());
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        session.enter();

        assertFalse(session.canUndo());
        session.moveNextDiagramTarget();
        assertEquals(
                new DiagramPortTarget(0, 0, new DiagramElementId("sensor"), new DiagramPortId("out")),
                session.current().diagramEditingSelection().target());
        session.movePreviousDiagramTarget();
        assertEquals(
                new DiagramElementTarget(0, new DiagramElementId("sensor")),
                session.current().diagramEditingSelection().target());
        assertFalse(session.canUndo());
    }

    @Test
    void dragPreviewNeverMutatesCurrentSemanticDocumentOrHistory() {
        var document = document(paragraph("Before"), diagram());
        var session = editingSession(document);
        var original = session.current().document();

        assertTrue(session.beginDiagramElementDrag(10, 12));
        var preview = session.previewDiagramElementDrag(50, 30).orElseThrow();
        var previewNode = firstNode(preview);

        assertEquals(original, session.current().document());
        assertFalse(session.canUndo());
        assertTrue(session.isDiagramElementDragging());
        assertEquals(new DiagramBounds(48, 28, 30, 20), previewNode.bounds());
    }

    @Test
    void completedDragCommitsFinalPositionAsExactlyOneUndoStep() {
        var document = document(paragraph("Before"), diagram());
        var session = editingSession(document);
        var original = session.current();

        assertTrue(session.beginDiagramElementDrag(10, 12));
        session.previewDiagramElementDrag(30, 20);
        session.previewDiagramElementDrag(45, 27);
        session.previewDiagramElementDrag(70, 35);

        assertTrue(session.commitDiagramElementDrag(70, 35));
        assertFalse(session.isDiagramElementDragging());
        assertEquals(new DiagramBounds(68, 30, 30, 20), firstNode(session.current().document()).bounds());
        assertTrue(session.canUndo());

        assertTrue(session.undo());
        assertEquals(original.document(), session.current().document());
        assertEquals(original.selection(), session.current().selection());
        assertFalse(session.canUndo());
    }

    @Test
    void canceledDragLeavesDocumentAndHistoryUntouched() {
        var document = document(paragraph("Before"), diagram());
        var session = editingSession(document);
        var original = session.current();

        assertTrue(session.beginDiagramElementDrag(10, 12));
        session.previewDiagramElementDrag(60, 30);
        assertTrue(session.cancelDiagramElementDrag());

        assertEquals(original, session.current());
        assertFalse(session.isDiagramElementDragging());
        assertFalse(session.canUndo());
    }

    @Test
    void dragUsesGrabOffsetSoNodeDoesNotSnapItsCornerToPointer() {
        var document = document(paragraph("Before"), diagram());
        var session = editingSession(document);

        // Original node top-left is (8,10); grab at (20,18) creates offset (12,8).
        assertTrue(session.beginDiagramElementDrag(20, 18));
        assertTrue(session.commitDiagramElementDrag(50, 30));

        assertEquals(new DiagramBounds(38, 22, 30, 20), firstNode(session.current().document()).bounds());
    }

    @Test
    void dragCommitClampsFinalNodeInsideCanvas() {
        var document = document(paragraph("Before"), diagram());
        var session = editingSession(document);

        assertTrue(session.beginDiagramElementDrag(10, 12));
        assertTrue(session.commitDiagramElementDrag(1000, 1000));

        assertEquals(new DiagramBounds(70, 30, 30, 20), firstNode(session.current().document()).bounds());
    }

    @Test
    void onlyElementTargetsCanBeginDrag() {
        var document = document(paragraph("Before"), diagram());
        var session = editingSession(document);
        session.setDiagramEditingTarget(new DiagramConnectionTarget(0));

        assertFalse(session.beginDiagramElementDrag(10, 10));
        assertFalse(session.isDiagramElementDragging());
    }

    private static EditorSession editingSession(Document document) {
        var session = new EditorSession(document, 0);
        var diagramIndex = firstDiagramBlockIndex(document);
        session.setCurrent(new EditorState(document, new BlockSelection(diagramIndex), Optional.empty()));
        session.enter();
        return session;
    }

    private static DiagramNode firstNode(Document document) {
        var diagram = (DiagramBlock) document.blocks().get(firstDiagramBlockIndex(document));
        return (DiagramNode) diagram.definition().elements().get(0);
    }

    private static int firstDiagramBlockIndex(Document document) {
        for (var index = 0; index < document.blocks().size(); index++) {
            if (document.blocks().get(index) instanceof DiagramBlock) {
                return index;
            }
        }
        throw new IllegalArgumentException("Test document contains no DiagramBlock.");
    }

    private static Document document(dev.rgcb.scholar.document.BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }

    private static DiagramBlock diagram() {
        var sensor = new DiagramElementId("sensor");
        var processor = new DiagramElementId("processor");
        var out = new DiagramPortId("out");
        var in = new DiagramPortId("in");
        return new DiagramBlock(new DiagramDefinition(
                "System Diagram",
                new DiagramCanvas(100, 50),
                List.of(
                        new DiagramNode(sensor, new DiagramBounds(8, 10, 30, 20), "Sensor", List.of(
                                new DiagramPort(out, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)))),
                        new DiagramNode(processor, new DiagramBounds(62, 20, 30, 20), "Processor", List.of(
                                new DiagramPort(in, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5))))),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(sensor, out),
                        new DiagramEndpoint(processor, in),
                        "signal"))));
    }
}
