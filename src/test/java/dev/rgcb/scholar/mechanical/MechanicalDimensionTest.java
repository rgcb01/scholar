package dev.rgcb.scholar.mechanical;

import static org.junit.jupiter.api.Assertions.*;
import dev.rgcb.scholar.diagram.*;
import dev.rgcb.scholar.diagram.clipboard.DiagramPlainTextSerializer;
import dev.rgcb.scholar.diagram.layout.DiagramLayoutEngine;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.*;
import dev.rgcb.scholar.mechanical.editor.MechanicalDiagramEditor;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import org.junit.jupiter.api.Test;

class MechanicalDimensionTest {
    private final MechanicalDiagramEditor editor = new MechanicalDiagramEditor();

    @Test void allDimensionKindsAreSemanticPortlessElements() {
        for (var kind : MechanicalDimensionKind.values()) {
            var result = editor.addDimension(emptyBlock(), new DiagramPropertyTarget(DiagramProperty.TITLE), kind);
            var d = assertInstanceOf(MechanicalDimension.class, result.diagram().definition().elements().get(0));
            assertEquals(kind, d.kind());
            assertTrue(d.ports().isEmpty());
        }
    }

    @Test void measurementsAreDerivedFromGeometry() {
        assertEquals(40.0, d(MechanicalDimensionKind.HORIZONTAL, 40, 30).measuredValue(), 1e-9);
        assertEquals(30.0, d(MechanicalDimensionKind.VERTICAL, 40, 30).measuredValue(), 1e-9);
        assertEquals(50.0, d(MechanicalDimensionKind.ALIGNED, 40, 30).measuredValue(), 1e-9);
        assertEquals(15.0, d(MechanicalDimensionKind.RADIUS, 40, 30).measuredValue(), 1e-9);
        assertEquals(30.0, d(MechanicalDimensionKind.DIAMETER, 40, 30).measuredValue(), 1e-9);
        assertEquals(Math.toDegrees(Math.atan2(30,40)), d(MechanicalDimensionKind.ANGLE,40,30).measuredValue(), 1e-9);
    }

    @Test void calloutPrefixesAreDerived() {
        assertEquals("R 15", d(MechanicalDimensionKind.RADIUS,40,30).displayText());
        assertEquals("⌀ 30", d(MechanicalDimensionKind.DIAMETER,40,30).displayText());
        assertEquals("36.9°", d(MechanicalDimensionKind.ANGLE,40,30).displayText());
    }

    @Test void layoutPublishesDimensionWithDerivedLabel() {
        var result=editor.addDimension(emptyBlock(),new DiagramPropertyTarget(DiagramProperty.TITLE),MechanicalDimensionKind.HORIZONTAL);
        var layout=new DiagramLayoutEngine().layout(result.diagram(),0,0,0,240,new FixedMeasurer());
        assertEquals(1,layout.mechanicalDimensions().size());
        assertFalse(layout.mechanicalDimensions().get(0).label().text().isBlank());
    }

    @Test void deletionIsUndoFriendlyImmutableReplacement() {
        var added=editor.addDimension(emptyBlock(),new DiagramPropertyTarget(DiagramProperty.TITLE),MechanicalDimensionKind.DIAMETER);
        var target=assertInstanceOf(DiagramElementTarget.class,added.target());
        var deleted=editor.deleteDimension(added.diagram(),target);
        assertTrue(deleted.changed());
        assertTrue(deleted.diagram().definition().elements().isEmpty());
    }

    @Test void genericMovePreservesMeasurementSizeAndValue() {
        var added=editor.addDimension(emptyBlock(),new DiagramPropertyTarget(DiagramProperty.TITLE),MechanicalDimensionKind.ALIGNED);
        var target=assertInstanceOf(DiagramElementTarget.class,added.target());
        var before=(MechanicalDimension)added.diagram().definition().elements().get(0);
        var moved=new DiagramEditor().moveElement(added.diagram(),target,3,4);
        var after=(MechanicalDimension)moved.diagram().definition().elements().get(0);
        assertEquals(before.measuredValue(),after.measuredValue(),1e-9);
        assertEquals(3,after.bounds().x(),1e-9);
    }

    @Test void plainTextFallbackIncludesSemanticMeasurement() {
        var added=editor.addDimension(emptyBlock(),new DiagramPropertyTarget(DiagramProperty.TITLE),MechanicalDimensionKind.RADIUS);
        var text=new DiagramPlainTextSerializer().serialize(added.diagram());
        assertTrue(text.contains("Mechanical Dimension: RADIUS [dimension-1]"));
        assertTrue(text.contains("Measured Value: R "));
    }

    private static MechanicalDimension d(MechanicalDimensionKind k,double w,double h) {
        return new MechanicalDimension(new DiagramElementId("d"),new DiagramBounds(0,0,w,h),k);
    }
    private static DiagramBlock emptyBlock() {
        return new DiagramBlock(new DiagramDefinition("Dimensions",new DiagramCanvas(120,80),List.of(),List.of()));
    }
    private static final class FixedMeasurer implements TextMeasurer {
        public int measureWidth(String text,TextStyle style){return text.length()*6;}
        public int lineHeight(TextStyle style){return 9;}
    }
}
