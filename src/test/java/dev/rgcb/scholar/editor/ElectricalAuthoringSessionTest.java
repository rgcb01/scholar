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
import dev.rgcb.scholar.diagram.DiagramPortId;
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

class ElectricalAuthoringSessionTest {
    @Test
    void diagramMenuExposesExplicitElectricalAuthoringCommands() {
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
    void insertByKindIsOneUndoableGlobalEdit() {
        var session = emptyEditingSession();
        var before = session.current().document();

        assertTrue(session.addElectricalComponent(ElectricalComponentKind.RESISTOR));
        var component = selectedComponent(session);
        assertEquals(ElectricalComponentKind.RESISTOR, component.kind());
        assertEquals("R1", component.referenceDesignator());
        assertTrue(session.canUndo());
        assertTrue(session.undo());
        assertEquals(before, session.current().document());
        assertTrue(session.redo());
        assertEquals(1, currentDiagram(session).definition().elements().size());
    }

    @Test
    void componentAnnotationPopupContractCommitsBothFieldsAsOneEdit() {
        var session = componentEditingSession();
        var before = session.current().document();

        assertEquals(Optional.of("R1"), session.electricalComponentDraft().map(draft -> draft.referenceDesignator()));
        assertTrue(session.applyElectricalComponentAnnotations("R9", "22 kΩ"));
        var updated = selectedComponent(session);
        assertEquals("R9", updated.referenceDesignator());
        assertEquals("22 kΩ", updated.valueLabel());
        assertTrue(session.undo());
        assertEquals(before, session.current().document());
        assertFalse(session.canUndo());
    }

    @Test
    void rotationIsOneHistoryEditAndKeepsWireEndpointsStable() {
        var session = connectedEditingSession();
        var before = session.current().document();
        var beforeConnection = currentDiagram(session).definition().connections().get(0);

        assertTrue(session.supportsRotateElectricalComponent());
        assertTrue(session.rotateElectricalComponentClockwise());
        var rotated = selectedComponent(session);
        assertEquals(ElectricalOrientation.DEG_90, rotated.orientation());
        assertEquals(beforeConnection, currentDiagram(session).definition().connections().get(0));
        assertTrue(session.undo());
        assertEquals(before, session.current().document());
    }

    @Test
    void deletingComponentAndIncidentWireUndoTogether() {
        var session = connectedEditingSession();
        var before = session.current().document();

        assertTrue(session.supportsDeleteElectricalComponent());
        assertTrue(session.deleteElectricalComponent());
        assertEquals(1, currentDiagram(session).definition().elements().size());
        assertEquals(0, currentDiagram(session).definition().connections().size());
        assertTrue(session.undo());
        assertEquals(before, session.current().document());
    }

    @Test
    void electricalComponentDragPreviewsAndCommitsAsOneUndoStep() {
        var session = componentEditingSession();
        var before = session.current().document();
        var original = selectedComponent(session);
        var pointerX = original.bounds().x() + 2;
        var pointerY = original.bounds().y() + 2;

        assertTrue(session.beginDiagramElementDrag(pointerX, pointerY));
        var preview = session.previewDiagramElementDrag(70, 45).orElseThrow();
        var previewComponent = (ElectricalComponent) ((DiagramBlock) preview.blocks().get(1)).definition().elements().get(0);
        assertFalse(original.bounds().equals(previewComponent.bounds()));
        assertEquals(before, session.current().document());

        assertTrue(session.commitDiagramElementDrag(70, 45));
        assertFalse(original.bounds().equals(selectedComponent(session).bounds()));
        assertTrue(session.undo());
        assertEquals(before, session.current().document());
        assertFalse(session.canUndo());
    }

    @Test
    void insertedElectricalTerminalsUseExistingConnectionDraftWorkflow() {
        var session = emptyEditingSession();
        assertTrue(session.addElectricalComponent(ElectricalComponentKind.RESISTOR));
        var resistor = selectedComponent(session);
        assertTrue(session.addElectricalComponent(ElectricalComponentKind.DIODE));
        var diode = selectedComponent(session);

        session.setDiagramEditingTarget(new DiagramPortTarget(
                0, 1, resistor.id(), new DiagramPortId("b")));
        assertTrue(session.beginDiagramConnection());
        session.setDiagramEditingTarget(new DiagramPortTarget(
                1, 0, diode.id(), new DiagramPortId("anode")));
        assertTrue(session.supportsCompleteDiagramConnection());
        assertTrue(session.completeDiagramConnection());

        assertEquals(1, currentDiagram(session).definition().connections().size());
        assertEquals(new DiagramEndpoint(resistor.id(), new DiagramPortId("b")),
                currentDiagram(session).definition().connections().get(0).source());
        assertEquals(new DiagramEndpoint(diode.id(), new DiagramPortId("anode")),
                currentDiagram(session).definition().connections().get(0).target());
    }

    private static EditorSession emptyEditingSession() {
        return editingSession(new DiagramBlock(new DiagramDefinition(
                "Electrical",
                new DiagramCanvas(120, 70),
                List.of(),
                List.of())));
    }

    private static EditorSession componentEditingSession() {
        var resistor = resistor();
        return editingSession(new DiagramBlock(new DiagramDefinition(
                "Electrical",
                new DiagramCanvas(120, 70),
                List.of(resistor),
                List.of())), new DiagramElementTarget(0, resistor.id()));
    }

    private static EditorSession connectedEditingSession() {
        var resistor = resistor();
        var diode = new ElectricalComponent(
                new DiagramElementId("d1"),
                new DiagramBounds(70, 15, 28, 12),
                ElectricalComponentKind.DIODE,
                ElectricalOrientation.DEG_0,
                "D1",
                "1N4148");
        var connection = new DiagramConnection(
                new DiagramEndpoint(resistor.id(), new DiagramPortId("b")),
                new DiagramEndpoint(diode.id(), new DiagramPortId("anode")),
                "");
        return editingSession(new DiagramBlock(new DiagramDefinition(
                "Electrical",
                new DiagramCanvas(120, 70),
                List.of(resistor, diode),
                List.of(connection))), new DiagramElementTarget(0, resistor.id()));
    }

    private static EditorSession editingSession(DiagramBlock block) {
        var document = new Document(List.of(paragraph("Before"), block));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        session.enter();
        return session;
    }

    private static EditorSession editingSession(DiagramBlock block, DiagramEditTarget target) {
        var session = editingSession(block);
        session.setDiagramEditingTarget(target);
        return session;
    }

    private static DiagramBlock currentDiagram(EditorSession session) {
        return (DiagramBlock) session.current().document().blocks().get(1);
    }

    private static ElectricalComponent selectedComponent(EditorSession session) {
        return session.selectedElectricalComponent().orElseThrow();
    }

    private static ElectricalComponent resistor() {
        return new ElectricalComponent(
                new DiagramElementId("r1"),
                new DiagramBounds(20, 15, 28, 12),
                ElectricalComponentKind.RESISTOR,
                ElectricalOrientation.DEG_0,
                "R1",
                "10 kΩ");
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }
}
