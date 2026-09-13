package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.clipboard.InlineContentClipboardPayload;
import dev.rgcb.scholar.clipboard.ScholarClipboardService;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceResolver;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CrossReferenceEditingTest {
    @Test
    void insertsCrossReferenceAsAtomicInlineNodeWithUndoRedo() {
        var document = document(paragraph("See "), figure("velocity"));
        var session = new EditorSession(document, 0);

        assertTrue(session.insertCrossReference(CrossReferenceTargetKind.FIGURE, "velocity"));

        var nodes = ((Paragraph) session.current().document().blocks().get(0)).content().nodes();
        assertEquals(new Text("See ", Set.of()), nodes.get(0));
        assertEquals(ref("velocity"), nodes.get(1));
        assertEquals(new DocumentPosition(0, 5), session.current().caret());
        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertTrue(session.redo());
        assertEquals(ref("velocity"), ((Paragraph) session.current().document().blocks().get(0)).content().nodes().get(1));
    }

    @Test
    void arrowAndDeleteTreatCrossReferenceAsOneLogicalCharacter() {
        var document = document(paragraph(new Text("A", Set.of()), ref("velocity"), new Text("B", Set.of())), figure("velocity"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 1)));

        session.moveRight();
        assertEquals(new DocumentPosition(0, 2), session.current().caret());
        assertTrue(session.deleteBackward());

        var nodes = ((Paragraph) session.current().document().blocks().get(0)).content().nodes();
        assertEquals(List.of(new Text("A", Set.of()), new Text("B", Set.of())), nodes);
    }

    @Test
    void figureCaptionCanContainCrossReference() {
        var document = document(paragraph("Before"), figure("setup"), figure("velocity"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(2), Optional.empty()));

        assertTrue(session.editFigureCaption());
        assertTrue(session.insertCrossReference(CrossReferenceTargetKind.FIGURE, "setup"));

        var figure = assertInstanceOf(FigureBlock.class, session.current().document().blocks().get(2));
        assertEquals(List.of(ref("setup")), figure.caption().nodes());
    }

    @Test
    void copyingTextContainingCrossReferenceKeepsNativePayloadAndResolvedFallback() {
        var document = document(paragraph(new Text("See ", Set.of()), ref("velocity"), new Text(".", Set.of())), figure("velocity"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 4), new DocumentPosition(0, 5), Optional.empty()));

        var copy = session.copyForClipboard().orElseThrow();

        assertEquals("Figure 1", copy.plainText());
        var payload = assertInstanceOf(InlineContentClipboardPayload.class, copy.payload().orElseThrow());
        assertEquals(List.of(ref("velocity")), payload.content().nodes());
    }

    @Test
    void nativeInlineClipboardPastePreservesCrossReferenceTargetId() {
        var document = document(new Paragraph(new InlineContent(List.of())), figure("velocity"));
        var session = new EditorSession(document, 0);
        var payload = new InlineContentClipboardPayload(new InlineContent(List.of(ref("velocity"))));

        assertTrue(session.pasteFromClipboard(Optional.of(payload), "Figure 1"));

        var nodes = ((Paragraph) session.current().document().blocks().get(0)).content().nodes();
        assertEquals(ref("velocity"), nodes.get(0));
        assertEquals(new DocumentPosition(0, 1), session.current().caret());
    }

    @Test
    void copyActionInstallsInlineReferenceSidecar() {
        var document = document(paragraph(new Text("See ", Set.of()), ref("velocity")), figure("velocity"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 4), new DocumentPosition(0, 5), Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.copy().execute(new EditorActionContext(session, clipboard, sidecar));

        assertFalse(result.documentChanged());
        assertEquals("Figure 1", clipboard.text);
        assertInstanceOf(InlineContentClipboardPayload.class, sidecar.snapshot().orElseThrow().payload());
    }

    @Test
    void targetDeletionLeavesReferenceBrokenAndUndoRestoresResolution() {
        var document = document(paragraph(ref("velocity")), figure("velocity"));
        var session = new EditorSession(document, 0);
        var resolver = new CrossReferenceResolver();

        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        assertTrue(session.deleteForward());
        assertEquals("[Missing reference]", resolver.inlineText(session.current().document(), ((Paragraph) session.current().document().blocks().get(0)).content()));
        assertTrue(session.undo());
        assertEquals("Figure 1", resolver.inlineText(session.current().document(), ((Paragraph) session.current().document().blocks().get(0)).content()));
    }

    private static CrossReference ref(String id) {
        return new CrossReference(CrossReferenceTargetKind.FIGURE, id);
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return paragraph(new Text(text, Set.of()));
    }

    private static Paragraph paragraph(InlineNode... nodes) {
        return new Paragraph(new InlineContent(List.of(nodes)));
    }

    private static FigureBlock figure(String id) {
        return new FigureBlock(id, plot(), new InlineContent(List.of()));
    }

    private static PlotBlock plot() {
        return new PlotBlock(PlotDefinition.of(
                "Plot",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("Series", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(1, 1))))));
    }

    private static final class FakeClipboard implements ClipboardAdapter {
        private String text = "";

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean setText(String text) {
            this.text = text;
            return true;
        }
    }
}
