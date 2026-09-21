package dev.rgcb.scholar.mechanical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.clipboard.DiagramPlainTextSerializer;
import dev.rgcb.scholar.diagram.layout.DiagramLayoutEngine;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.DiagramElementTarget;
import dev.rgcb.scholar.editor.DiagramProperty;
import dev.rgcb.scholar.editor.DiagramPropertyTarget;
import dev.rgcb.scholar.mechanical.editor.MechanicalDiagramEditor;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import org.junit.jupiter.api.Test;

class MechanicalPrimitiveTest {
    private final MechanicalDiagramEditor editor = new MechanicalDiagramEditor();

    @Test
    void allM19APrimitiveKindsCanBeAuthoredWithoutPorts() {
        var block = emptyBlock();
        for (var kind : MechanicalPrimitiveKind.values()) {
            var result = editor.addPrimitive(block, new DiagramPropertyTarget(DiagramProperty.TITLE), kind);
            assertTrue(result.changed());
            var primitive = assertInstanceOf(MechanicalPrimitive.class,
                    result.diagram().definition().elements().get(0));
            assertEquals(kind, primitive.kind());
            assertTrue(primitive.ports().isEmpty());
        }
    }

    @Test
    void authoredPrimitiveStaysInsideLogicalCanvas() {
        var result = editor.addPrimitive(emptyBlock(), new DiagramPropertyTarget(DiagramProperty.TITLE),
                MechanicalPrimitiveKind.RECTANGLE);
        var primitive = (MechanicalPrimitive) result.diagram().definition().elements().get(0);
        assertTrue(primitive.bounds().x() >= 0.0);
        assertTrue(primitive.bounds().y() >= 0.0);
        assertTrue(primitive.bounds().right() <= 120.0);
        assertTrue(primitive.bounds().bottom() <= 80.0);
    }

    @Test
    void successiveAuthoredPrimitivesUseDistinctDefaultGeometry() {
        var first = editor.addPrimitive(emptyBlock(), new DiagramPropertyTarget(DiagramProperty.TITLE),
                MechanicalPrimitiveKind.RECTANGLE);
        var second = editor.addPrimitive(first.diagram(), first.target(), MechanicalPrimitiveKind.CIRCLE);
        var firstBounds = second.diagram().definition().elements().get(0).bounds();
        var secondBounds = second.diagram().definition().elements().get(1).bounds();

        assertFalse(firstBounds.x() < secondBounds.right() + 2.0
                && firstBounds.right() + 2.0 > secondBounds.x()
                && firstBounds.y() < secondBounds.bottom() + 2.0
                && firstBounds.bottom() + 2.0 > secondBounds.y());
    }

    @Test
    void primitiveUsesGenericWithBoundsForDragging() {
        var result = editor.addPrimitive(emptyBlock(), new DiagramPropertyTarget(DiagramProperty.TITLE),
                MechanicalPrimitiveKind.LINE);
        var primitive = (MechanicalPrimitive) result.diagram().definition().elements().get(0);
        var moved = primitive.withBounds(new dev.rgcb.scholar.diagram.DiagramBounds(5, 6, primitive.bounds().width(), primitive.bounds().height()));
        assertEquals(5.0, moved.bounds().x());
        assertEquals(6.0, moved.bounds().y());
        assertEquals(primitive.id(), moved.id());
    }

    @Test
    void deletePrimitiveIsOneImmutableReplacement() {
        var added = editor.addPrimitive(emptyBlock(), new DiagramPropertyTarget(DiagramProperty.TITLE),
                MechanicalPrimitiveKind.CIRCLE);
        var target = assertInstanceOf(DiagramElementTarget.class, added.target());
        var deleted = editor.deletePrimitive(added.diagram(), target);
        assertTrue(deleted.changed());
        assertTrue(deleted.diagram().definition().elements().isEmpty());
        assertFalse(editor.canDelete(deleted.diagram(), deleted.target()));
    }

    @Test
    void layoutPublishesMechanicalPrimitiveSeparatelyFromElectricalAndGenericNodes() {
        var added = editor.addPrimitive(emptyBlock(), new DiagramPropertyTarget(DiagramProperty.TITLE),
                MechanicalPrimitiveKind.ARC);
        var layout = new DiagramLayoutEngine().layout(added.diagram(), 0, 0, 0, 240, new FixedMeasurer());
        assertEquals(1, layout.mechanicalPrimitives().size());
        assertEquals(MechanicalPrimitiveKind.ARC, layout.mechanicalPrimitives().get(0).kind());
        assertTrue(layout.electricalComponents().isEmpty());
        assertTrue(layout.nodes().isEmpty());
    }

    @Test
    void plainTextFallbackNamesMechanicalPrimitiveAndBounds() {
        var added = editor.addPrimitive(emptyBlock(), new DiagramPropertyTarget(DiagramProperty.TITLE),
                MechanicalPrimitiveKind.REFERENCE_POINT);
        var text = new DiagramPlainTextSerializer().serialize(added.diagram());
        assertTrue(text.contains("Mechanical: REFERENCE_POINT [mechanical-1]"));
        assertTrue(text.contains("Bounds: ["));
    }

    private static DiagramBlock emptyBlock() {
        return new DiagramBlock(new DiagramDefinition(
                "Mechanical Primitives",
                new DiagramCanvas(120, 80),
                List.of(),
                List.of()));
    }

    private static final class FixedMeasurer implements TextMeasurer {
        @Override public int measureWidth(String text, TextStyle style) { return text.length() * 6; }
        @Override public int lineHeight(TextStyle style) { return 9; }
    }
}
