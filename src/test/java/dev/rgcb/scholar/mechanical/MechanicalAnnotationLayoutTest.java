package dev.rgcb.scholar.mechanical;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.diagram.*;
import dev.rgcb.scholar.diagram.layout.DiagramLayoutEngine;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import org.junit.jupiter.api.Test;

class MechanicalAnnotationLayoutTest {
    @Test
    void partLabelVisualFootprintExpandsForEditedTextWithoutChangingSemanticBounds() {
        var semanticBounds = new DiagramBounds(20, 20, 30, 12);
        var annotation = new MechanicalAnnotation(
                new DiagramElementId("label"),
                semanticBounds,
                MechanicalAnnotationKind.PART_LABEL,
                "BRACKET A SI");
        var block = block(annotation);

        var layout = new DiagramLayoutEngine().layout(block, 0, 0, 0, 240, new FixedMeasurer());
        var laidOut = layout.mechanicalAnnotations().get(0);

        assertTrue(laidOut.bounds().width() >= laidOut.label().width() + 16);
        assertEquals(semanticBounds, annotation.bounds());
    }

    @Test
    void noteFootprintTracksTextWidth() {
        var annotation = new MechanicalAnnotation(
                new DiagramElementId("note"),
                new DiagramBounds(20, 20, 20, 10),
                MechanicalAnnotationKind.NOTE,
                "REMOVE BURRS SI");
        var layout = new DiagramLayoutEngine().layout(block(annotation), 0, 0, 0, 240, new FixedMeasurer());
        var laidOut = layout.mechanicalAnnotations().get(0);

        assertTrue(laidOut.bounds().width() >= laidOut.label().width() + 4);
        assertTrue(laidOut.label().x() >= laidOut.bounds().x());
    }

    @Test
    void leaderReservesVerticalGapBelowTextForRule() {
        var annotation = new MechanicalAnnotation(
                new DiagramElementId("leader"),
                new DiagramBounds(20, 20, 42, 18),
                MechanicalAnnotationKind.LEADER,
                "M6 HOLE SI");
        var layout = new DiagramLayoutEngine().layout(block(annotation), 0, 0, 0, 240, new FixedMeasurer());
        var laidOut = layout.mechanicalAnnotations().get(0);

        assertTrue(laidOut.bounds().height() >= laidOut.label().height() + 14);
        assertTrue(laidOut.label().y() + laidOut.label().height() + 3 < laidOut.bounds().bottom());
    }

    private static DiagramBlock block(MechanicalAnnotation annotation) {
        return new DiagramBlock(new DiagramDefinition(
                "Annotations",
                new DiagramCanvas(120, 80),
                List.of(annotation),
                List.of()));
    }

    private static final class FixedMeasurer implements TextMeasurer {
        @Override public int measureWidth(String text, TextStyle style) { return text.length() * 6; }
        @Override public int lineHeight(TextStyle style) { return 9; }
    }
}
