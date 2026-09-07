package dev.rgcb.scholar.mechanical;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.clipboard.DiagramPlainTextSerializer;
import dev.rgcb.scholar.diagram.layout.DiagramLayoutEngine;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.DiagramElementTarget;
import dev.rgcb.scholar.editor.DiagramProperty;
import dev.rgcb.scholar.editor.DiagramPropertyTarget;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.mechanical.editor.MechanicalDiagramEditor;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MechanicalConstraintTest {
    private final MechanicalDiagramEditor editor = new MechanicalDiagramEditor();

    @Test
    void constraintArityIsSemanticAndValidated() {
        var id = new DiagramElementId("c");
        var subject = new DiagramElementId("a");
        assertThrows(IllegalArgumentException.class, () ->
                new MechanicalConstraint(id, new DiagramBounds(0,0,4,4),
                        MechanicalConstraintKind.PARALLEL, subject, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () ->
                MechanicalConstraint.binary(id, new DiagramBounds(0,0,4,4),
                        MechanicalConstraintKind.COINCIDENT, subject, subject));
    }

    @Test
    void verticalConstraintRotatesDirectionalPrimitiveAndPreservesLength() {
        var line = new MechanicalPrimitive(
                new DiagramElementId("line"),
                new DiagramBounds(10, 20, 34, 8),
                MechanicalPrimitiveKind.LINE);
        var block = block(line);
        var target = new DiagramElementTarget(0, line.id());
        var result = editor.addUnaryConstraint(block, target, MechanicalConstraintKind.VERTICAL);
        assertTrue(result.changed());
        var rotated = assertInstanceOf(MechanicalPrimitive.class, result.diagram().definition().elements().get(0));
        assertEquals(MechanicalOrientation.DEG_90, rotated.orientation());
        assertEquals(8.0, rotated.bounds().width(), 1e-9);
        assertEquals(34.0, rotated.bounds().height(), 1e-9);
        assertInstanceOf(MechanicalConstraint.class, result.diagram().definition().elements().get(1));
    }

    @Test
    void horizontalConstraintCanRestoreQuarterTurn() {
        var line = new MechanicalPrimitive(
                new DiagramElementId("line"),
                new DiagramBounds(20, 10, 8, 34),
                MechanicalPrimitiveKind.LINE,
                MechanicalOrientation.DEG_90);
        var result = editor.addUnaryConstraint(
                block(line),
                new DiagramElementTarget(0, line.id()),
                MechanicalConstraintKind.HORIZONTAL);
        var rotated = (MechanicalPrimitive) result.diagram().definition().elements().get(0);
        assertEquals(MechanicalOrientation.DEG_0, rotated.orientation());
        assertEquals(34.0, rotated.bounds().width(), 1e-9);
        assertEquals(8.0, rotated.bounds().height(), 1e-9);
    }

    @Test
    void parallelAndPerpendicularRelationshipsDrivePeerOrientation() {
        var a = new MechanicalPrimitive(new DiagramElementId("a"), new DiagramBounds(8,8,30,8),
                MechanicalPrimitiveKind.LINE, MechanicalOrientation.DEG_0);
        var b = new MechanicalPrimitive(new DiagramElementId("b"), new DiagramBounds(8,30,8,30),
                MechanicalPrimitiveKind.LINE, MechanicalOrientation.DEG_90);
        var base = block(a,b);

        var parallel = editor.addBinaryConstraint(
                base,
                new DiagramElementTarget(0,a.id()),
                new DiagramElementTarget(1,b.id()),
                MechanicalConstraintKind.PARALLEL);
        var parallelPeer = (MechanicalPrimitive) parallel.diagram().definition().elements().get(1);
        assertEquals(MechanicalOrientation.DEG_0, parallelPeer.orientation());

        var c = new MechanicalPrimitive(new DiagramElementId("c"), new DiagramBounds(50,8,30,8),
                MechanicalPrimitiveKind.LINE, MechanicalOrientation.DEG_0);
        var d = new MechanicalPrimitive(new DiagramElementId("d"), new DiagramBounds(50,30,30,8),
                MechanicalPrimitiveKind.LINE, MechanicalOrientation.DEG_0);
        var perpendicular = editor.addBinaryConstraint(
                block(c,d),
                new DiagramElementTarget(0,c.id()),
                new DiagramElementTarget(1,d.id()),
                MechanicalConstraintKind.PERPENDICULAR);
        var perpPeer = (MechanicalPrimitive) perpendicular.diagram().definition().elements().get(1);
        assertEquals(MechanicalOrientation.DEG_90, perpPeer.orientation());
    }

    @Test
    void coincidentAndConcentricRelationshipsAlignCenters() {
        var a = new MechanicalPrimitive(new DiagramElementId("a"), new DiagramBounds(10,10,10,10),
                MechanicalPrimitiveKind.REFERENCE_POINT);
        var b = new MechanicalPrimitive(new DiagramElementId("b"), new DiagramBounds(60,50,8,8),
                MechanicalPrimitiveKind.REFERENCE_POINT);
        var coincident = editor.addBinaryConstraint(
                block(a,b), new DiagramElementTarget(0,a.id()), new DiagramElementTarget(1,b.id()),
                MechanicalConstraintKind.COINCIDENT);
        var movedB = (MechanicalPrimitive) coincident.diagram().definition().elements().get(1);
        assertEquals(centerX(a), centerX(movedB), 1e-9);
        assertEquals(centerY(a), centerY(movedB), 1e-9);

        var outer = new MechanicalPrimitive(new DiagramElementId("outer"), new DiagramBounds(70,10,28,28),
                MechanicalPrimitiveKind.CIRCLE);
        var inner = new MechanicalPrimitive(new DiagramElementId("inner"), new DiagramBounds(10,50,14,14),
                MechanicalPrimitiveKind.CIRCLE);
        var concentric = editor.addBinaryConstraint(
                block(outer,inner), new DiagramElementTarget(0,outer.id()), new DiagramElementTarget(1,inner.id()),
                MechanicalConstraintKind.CONCENTRIC);
        var movedInner = (MechanicalPrimitive) concentric.diagram().definition().elements().get(1);
        assertEquals(centerX(outer), centerX(movedInner), 1e-9);
        assertEquals(centerY(outer), centerY(movedInner), 1e-9);
    }

    @Test
    void deletingPrimitiveAlsoDeletesReferencingConstraints() {
        var a = new MechanicalPrimitive(new DiagramElementId("a"), new DiagramBounds(10,10,30,8), MechanicalPrimitiveKind.LINE);
        var b = new MechanicalPrimitive(new DiagramElementId("b"), new DiagramBounds(10,30,30,8), MechanicalPrimitiveKind.LINE);
        var constrained = editor.addBinaryConstraint(
                block(a,b), new DiagramElementTarget(0,a.id()), new DiagramElementTarget(1,b.id()),
                MechanicalConstraintKind.PARALLEL).diagram();
        assertEquals(3, constrained.definition().elements().size());
        var deleted = editor.deletePrimitive(constrained, new DiagramElementTarget(0,a.id()));
        assertTrue(deleted.diagram().definition().elements().stream().noneMatch(MechanicalConstraint.class::isInstance));
    }

    @Test
    void layoutPublishesDerivedConstraintMarker() {
        var line = new MechanicalPrimitive(new DiagramElementId("line"), new DiagramBounds(10,10,34,8), MechanicalPrimitiveKind.LINE);
        var constrained = editor.addUnaryConstraint(
                block(line), new DiagramElementTarget(0,line.id()), MechanicalConstraintKind.HORIZONTAL).diagram();
        var layout = new DiagramLayoutEngine().layout(constrained,0,0,0,240,new FixedMeasurer());
        assertEquals(1, layout.mechanicalConstraints().size());
        assertEquals(MechanicalConstraintKind.HORIZONTAL, layout.mechanicalConstraints().get(0).kind());
        assertTrue(layout.mechanicalConstraints().get(0).bounds().width() > 0);
    }

    @Test
    void plainTextFallbackPreservesConstraintReferencesAndOrientation() {
        var a = new MechanicalPrimitive(new DiagramElementId("a"), new DiagramBounds(10,10,30,8), MechanicalPrimitiveKind.LINE);
        var b = new MechanicalPrimitive(new DiagramElementId("b"), new DiagramBounds(10,30,30,8), MechanicalPrimitiveKind.LINE);
        var constrained = editor.addBinaryConstraint(
                block(a,b), new DiagramElementTarget(0,a.id()), new DiagramElementTarget(1,b.id()),
                MechanicalConstraintKind.PARALLEL).diagram();
        var text = new DiagramPlainTextSerializer().serialize(constrained);
        assertTrue(text.contains("Mechanical Constraint: PARALLEL"));
        assertTrue(text.contains("Subject: a"));
        assertTrue(text.contains("Peer: b"));
        assertTrue(text.contains("Orientation: DEG_0"));
    }

    private static DiagramBlock block(MechanicalPrimitive... primitives) {
        return new DiagramBlock(new DiagramDefinition(
                "Constraints", new DiagramCanvas(120,80), List.of(primitives), List.of()));
    }

    private static double centerX(MechanicalPrimitive p) {
        return p.bounds().x() + p.bounds().width()/2.0;
    }
    private static double centerY(MechanicalPrimitive p) {
        return p.bounds().y() + p.bounds().height()/2.0;
    }
    private static final class FixedMeasurer implements TextMeasurer {
        @Override public int measureWidth(String text, TextStyle style) { return text.length()*6; }
        @Override public int lineHeight(TextStyle style) { return 9; }
    }
}
