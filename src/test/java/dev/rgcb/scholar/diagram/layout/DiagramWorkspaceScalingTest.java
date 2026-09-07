package dev.rgcb.scholar.diagram.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.editor.DiagramHitTester;
import dev.rgcb.scholar.editor.DiagramProperty;
import dev.rgcb.scholar.editor.DiagramPropertyTarget;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagramWorkspaceScalingTest {
    private final DiagramLayoutEngine engine = new DiagramLayoutEngine();
    private final TextMeasurer measurer = new FixedTextMeasurer();

    @Test
    void legacyBlockDefaultsToCanvasAspectAndFitViewport() {
        var block = diagram();
        var layout = engine.layout(block, 0, 0, 0, 216, measurer);

        assertEquals(0.5, block.workspaceAspectRatio());
        assertEquals(layout.workspaceX(), layout.canvasX());
        assertEquals(layout.workspaceY(), layout.canvasY());
        assertEquals(layout.workspaceWidth(), layout.canvasWidth());
        assertEquals(layout.workspaceHeight(), layout.canvasHeight());
        assertEquals(DiagramViewport.fit(block.definition().canvas()), layout.viewport());
    }

    @Test
    void authoredWorkspaceAspectChangesBlockHeightWithoutChangingLogicalCanvas() {
        var base = diagram();
        var tall = base.withWorkspaceAspectRatio(0.8);
        var baseLayout = engine.layout(base, 0, 0, 0, 216, measurer);
        var tallLayout = engine.layout(tall, 0, 0, 0, 216, measurer);

        assertTrue(tallLayout.workspaceHeight() > baseLayout.workspaceHeight());
        assertTrue(tallLayout.height() > baseLayout.height());
        assertEquals(base.definition().canvas(), tall.definition().canvas());
        assertEquals(baseLayout.workspaceWidth(), tallLayout.workspaceWidth());
    }

    @Test
    void zoomUsesStableWorkspaceAndLargerDerivedCanvas() {
        var block = diagram();
        var fit = engine.layout(block, 0, 0, 0, 216, measurer);
        var zoomed = engine.layout(
                block,
                0,
                0,
                0,
                216,
                measurer,
                new DiagramViewport(2.0, 50.0, 25.0));

        assertEquals(fit.workspaceBounds(), zoomed.workspaceBounds());
        assertEquals(2.0, zoomed.viewport().zoom());
        assertEquals(fit.canvasWidth() * 2, zoomed.canvasWidth());
        assertEquals(fit.canvasHeight() * 2, zoomed.canvasHeight());
        assertEquals(fit.transform().scale() * 2.0, zoomed.transform().scale());
    }

    @Test
    void panChangesTransformWhileWorkspaceRemainsFixed() {
        var block = diagram();
        var centered = engine.layout(block, 0, 0, 0, 216, measurer, new DiagramViewport(2.0, 50.0, 25.0));
        var panned = engine.layout(block, 0, 0, 0, 216, measurer, new DiagramViewport(2.0, 65.0, 25.0));

        assertEquals(centered.workspaceBounds(), panned.workspaceBounds());
        assertTrue(panned.canvasX() < centered.canvasX());
        assertEquals(centered.canvasY(), panned.canvasY());
        assertEquals(65.0, panned.viewport().centerX());
    }

    @Test
    void hitTestingRejectsGeometryOutsideInternalWorkspace() {
        var block = new DiagramBlock(new DiagramDefinition(
                "",
                new DiagramCanvas(100, 50),
                List.of(new DiagramNode(new DiagramElementId("node"), new DiagramBounds(0, 10, 20, 15), "N", List.of())),
                List.of()));
        var layout = engine.layout(block, 0, 0, 0, 216, measurer, new DiagramViewport(2.0, 70.0, 25.0));
        var node = layout.nodes().get(0);
        var tester = new DiagramHitTester();

        assertTrue(node.x() < layout.workspaceX());
        assertTrue(tester.hit(layout, node.x() + 1, node.y() + 1).isEmpty());
        var canvasHit = tester.hit(
                layout,
                layout.workspaceX() + layout.workspaceWidth() - 8,
                layout.workspaceY() + 8);
        assertFalse(canvasHit.isEmpty());
        assertEquals(new DiagramPropertyTarget(DiagramProperty.CANVAS), canvasHit.orElseThrow());
    }

    private static DiagramBlock diagram() {
        return new DiagramBlock(new DiagramDefinition(
                "",
                new DiagramCanvas(100, 50),
                List.of(new DiagramNode(new DiagramElementId("node"), new DiagramBounds(35, 18, 30, 14), "N", List.of())),
                List.of()));
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            return text.length() * 6;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return 9;
        }
    }
}
