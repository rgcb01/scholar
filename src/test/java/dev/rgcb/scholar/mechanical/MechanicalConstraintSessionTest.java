package dev.rgcb.scholar.mechanical;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.diagram.*;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.editor.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MechanicalConstraintSessionTest {
    @Test
    void binaryConstraintUsesEphemeralSourceThenOneUndoableCommit() {
        var a = new MechanicalPrimitive(new DiagramElementId("a"), new DiagramBounds(8,8,30,8), MechanicalPrimitiveKind.LINE);
        var b = new MechanicalPrimitive(new DiagramElementId("b"), new DiagramBounds(8,30,30,8), MechanicalPrimitiveKind.LINE, MechanicalOrientation.DEG_90);
        var block = new DiagramBlock(new DiagramDefinition("Constraints", new DiagramCanvas(120,80), List.of(a,b), List.of()));
        var document = new Document(List.of(paragraph("Before"), block));
        var session = new EditorSession(document, 0);
        session.enterDiagramEditing(1, new DiagramElementTarget(0,a.id()));

        assertTrue(session.startMechanicalConstraint(MechanicalConstraintKind.PARALLEL));
        assertTrue(session.hasPendingMechanicalConstraint());
        assertEquals(document, session.current().document());

        session.setDiagramEditingTarget(new DiagramElementTarget(1,b.id()));
        assertTrue(session.supportsFinishMechanicalConstraint());
        assertTrue(session.finishMechanicalConstraint());
        assertFalse(session.hasPendingMechanicalConstraint());

        var updated = (DiagramBlock)session.current().document().blocks().get(1);
        assertTrue(updated.definition().elements().stream().anyMatch(MechanicalConstraint.class::isInstance));
        var peer = (MechanicalPrimitive)updated.definition().elements().get(1);
        assertEquals(MechanicalOrientation.DEG_0, peer.orientation());

        assertTrue(session.undo());
        assertEquals(document, session.current().document());
    }

    @Test
    void pendingConstraintCanBeCancelledWithoutHistoryEdit() {
        var a = new MechanicalPrimitive(new DiagramElementId("a"), new DiagramBounds(8,8,30,8), MechanicalPrimitiveKind.LINE);
        var b = new MechanicalPrimitive(new DiagramElementId("b"), new DiagramBounds(8,30,30,8), MechanicalPrimitiveKind.LINE);
        var document = new Document(List.of(
                paragraph("Before"),
                new DiagramBlock(new DiagramDefinition(
                        "Constraints", new DiagramCanvas(120,80), List.of(a,b), List.of()))));
        var session = new EditorSession(document,0);
        session.enterDiagramEditing(1,new DiagramElementTarget(0,a.id()));
        assertTrue(session.startMechanicalConstraint(MechanicalConstraintKind.PERPENDICULAR));
        assertTrue(session.cancelMechanicalConstraint());
        assertFalse(session.hasPendingMechanicalConstraint());
        assertEquals(document, session.current().document());
    }
    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode)new Text(text, Set.of()))));
    }
}
