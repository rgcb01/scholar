package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class DiagramStructuralEditingSessionTest {
    @Test
    void diagramMenuExposesExplicitStructuralCommands() {
        assertEquals(
                List.of(
                        EditorActionId.DIAGRAM_ADD_NODE,
                        EditorActionId.DIAGRAM_ADD_RESISTOR,
                        EditorActionId.DIAGRAM_ADD_CAPACITOR,
                        EditorActionId.DIAGRAM_ADD_DC_VOLTAGE_SOURCE,
                        EditorActionId.DIAGRAM_ADD_GROUND,
                        EditorActionId.DIAGRAM_ADD_DIODE,
                        EditorActionId.DIAGRAM_ADD_LED,
                        EditorActionId.DIAGRAM_ADD_SWITCH_SPST,
                        EditorActionId.DIAGRAM_ADD_JUNCTION,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_LINE,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_CENTERLINE,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_RECTANGLE,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_CIRCLE,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ARC,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ARROW,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_REFERENCE_POINT,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_PART_REFERENCE,
                        EditorActionId.DIAGRAM_GENERATE_MECHANICAL_BOM,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_PART_LABEL,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_NOTE,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_LEADER,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SHAFT,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_GEAR,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BEARING,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SPRING,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_PISTON,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BOLT,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_HORIZONTAL,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_VERTICAL,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ALIGNED,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_RADIUS,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_DIAMETER,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ANGLE,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_HORIZONTAL,
                        EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_VERTICAL,
                        EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_COINCIDENT,
                        EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PARALLEL,
                        EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PERPENDICULAR,
                        EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_CONCENTRIC,
                        EditorActionId.DIAGRAM_FINISH_MECHANICAL_CONSTRAINT,
                        EditorActionId.DIAGRAM_CANCEL_MECHANICAL_CONSTRAINT,
                        EditorActionId.DIAGRAM_SCALE_SYMBOLS_DOWN,
                        EditorActionId.DIAGRAM_SCALE_SYMBOLS_UP,
                        EditorActionId.DIAGRAM_WORKSPACE_SHORTER,
                        EditorActionId.DIAGRAM_WORKSPACE_TALLER,
                        EditorActionId.DIAGRAM_WORKSPACE_RESET_HEIGHT,
                        EditorActionId.DIAGRAM_ROTATE_CLOCKWISE,
                        EditorActionId.DIAGRAM_ROTATE_COUNTERCLOCKWISE,
                        EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT,
                        EditorActionId.DIAGRAM_DELETE_JUNCTION,
                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_CONSTRAINT,
                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_DIMENSION,
                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_PART_REFERENCE,
                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_ANNOTATION,
                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_SYMBOL,
                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE,
                        EditorActionId.DIAGRAM_DELETE_NODE,
                        EditorActionId.DIAGRAM_START_CONNECTION,
                        EditorActionId.DIAGRAM_FINISH_CONNECTION,
                        EditorActionId.DIAGRAM_CANCEL_CONNECTION,
                        EditorActionId.DIAGRAM_DELETE_CONNECTION),
                BuiltInEditorActions.diagramMenuActions().stream().map(EditorAction::id).toList());
    }

    @Test
    void addNodeIsOneGlobalUndoableEdit() {
        var session = editingSession();
        var before = session.current().document();

        assertTrue(session.addDiagramNode());
        assertEquals(3, currentDiagram(session).definition().elements().size());
        assertTrue(session.canUndo());
        assertTrue(session.undo());
        assertEquals(before, session.current().document());
    }

    @Test
    void nodeDeletionAndIncidentConnectionCleanupUndoTogether() {
        var session = editingSession();
        var before = session.current().document();
        session.setDiagramEditingTarget(new DiagramElementTarget(0, new DiagramElementId("sensor")));

        assertTrue(session.deleteDiagramNode());
        assertEquals(1, currentDiagram(session).definition().elements().size());
        assertEquals(0, currentDiagram(session).definition().connections().size());

        assertTrue(session.undo());
        assertEquals(before, session.current().document());
    }

    @Test
    void sourceSelectionIsEphemeralUntilConnectionIsComplete() {
        var session = editingSession();
        var before = session.current().document();
        var source = new DiagramPortTarget(0, 0, new DiagramElementId("sensor"), new DiagramPortId("out"));
        session.setDiagramEditingTarget(source);

        assertTrue(session.beginDiagramConnection());
        assertTrue(session.diagramConnectionInProgress());
        assertEquals(Optional.of(source), session.diagramConnectionSourceTarget());
        assertEquals(before, session.current().document());
        assertFalse(session.canUndo());
    }

    @Test
    void completeConnectionCommitsExactlyOneHistoryEdit() {
        var session = editingSessionWithoutConnection();
        var before = session.current().document();
        var source = new DiagramPortTarget(0, 0, new DiagramElementId("sensor"), new DiagramPortId("out"));
        var target = new DiagramPortTarget(1, 0, new DiagramElementId("processor"), new DiagramPortId("in"));

        session.setDiagramEditingTarget(source);
        assertTrue(session.beginDiagramConnection());
        session.setDiagramEditingTarget(target);
        assertTrue(session.supportsCompleteDiagramConnection());
        assertTrue(session.completeDiagramConnection());

        assertFalse(session.diagramConnectionInProgress());
        assertEquals(1, currentDiagram(session).definition().connections().size());
        assertEquals(new DiagramConnectionTarget(0), session.current().diagramEditingSelection().target());
        assertTrue(session.canUndo());
        assertTrue(session.undo());
        assertEquals(before, session.current().document());
        assertFalse(session.canUndo());
    }

    @Test
    void sameSourcePortCannotCompleteConnection() {
        var session = editingSessionWithoutConnection();
        var source = new DiagramPortTarget(0, 0, new DiagramElementId("sensor"), new DiagramPortId("out"));
        session.setDiagramEditingTarget(source);
        session.beginDiagramConnection();

        assertFalse(session.supportsCompleteDiagramConnection());
        assertFalse(session.completeDiagramConnection());
        assertTrue(session.diagramConnectionInProgress());
        assertEquals(0, currentDiagram(session).definition().connections().size());
    }

    @Test
    void cancelConnectionDraftDoesNotTouchDocumentOrHistory() {
        var session = editingSession();
        var before = session.current();
        session.setDiagramEditingTarget(new DiagramPortTarget(
                0, 0, new DiagramElementId("sensor"), new DiagramPortId("out")));
        session.beginDiagramConnection();

        assertTrue(session.cancelDiagramConnection());
        assertFalse(session.diagramConnectionInProgress());
        assertEquals(before.document(), session.current().document());
        assertFalse(session.canUndo());
    }

    @Test
    void labelEditingIsUndoableAndKeepsTargetSelected() {
        var session = editingSession();
        var target = new DiagramConnectionTarget(0);
        session.setDiagramEditingTarget(target);

        assertEquals(Optional.of("signal"), session.diagramTextValue());
        assertTrue(session.applyDiagramText("data"));
        assertEquals("data", currentDiagram(session).definition().connections().get(0).label());
        assertEquals(target, session.current().diagramEditingSelection().target());
        assertTrue(session.undo());
        assertEquals("signal", currentDiagram(session).definition().connections().get(0).label());
    }

    @Test
    void undoAndExitClearAnyPendingConnectionDraft() {
        var session = editingSession();
        session.setDiagramEditingTarget(new DiagramPortTarget(
                0, 0, new DiagramElementId("sensor"), new DiagramPortId("out")));
        session.beginDiagramConnection();
        session.exitDiagramEditing();
        assertFalse(session.diagramConnectionInProgress());

        session.enterDiagramEditing(1, new DiagramPortTarget(
                0, 0, new DiagramElementId("sensor"), new DiagramPortId("out")));
        session.addDiagramNode();
        session.setDiagramEditingTarget(new DiagramPortTarget(
                0, 0, new DiagramElementId("sensor"), new DiagramPortId("out")));
        session.beginDiagramConnection();
        assertTrue(session.undo());
        assertFalse(session.diagramConnectionInProgress());
    }

    private static EditorSession editingSession() {
        return editingSession(diagram(true));
    }

    private static EditorSession editingSessionWithoutConnection() {
        return editingSession(diagram(false));
    }

    private static EditorSession editingSession(DiagramBlock block) {
        var document = new Document(List.of(paragraph("Before"), block));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        session.enter();
        return session;
    }

    private static DiagramBlock currentDiagram(EditorSession session) {
        return (DiagramBlock) session.current().document().blocks().get(1);
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }

    private static DiagramBlock diagram(boolean connected) {
        var sensor = new DiagramElementId("sensor");
        var processor = new DiagramElementId("processor");
        var out = new DiagramPortId("out");
        var in = new DiagramPortId("in");
        var connections = connected
                ? List.of(new DiagramConnection(new DiagramEndpoint(sensor, out), new DiagramEndpoint(processor, in), "signal"))
                : List.<DiagramConnection>of();
        return new DiagramBlock(new DiagramDefinition(
                "System Diagram",
                new DiagramCanvas(100, 50),
                List.of(
                        new DiagramNode(sensor, new DiagramBounds(8, 10, 30, 20), "Sensor", List.of(
                                new DiagramPort(out, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)))),
                        new DiagramNode(processor, new DiagramBounds(62, 20, 30, 20), "Processor", List.of(
                                new DiagramPort(in, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5))))),
                connections));
    }
}
