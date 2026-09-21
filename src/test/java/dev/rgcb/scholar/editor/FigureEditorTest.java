package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.clipboard.ScholarClipboardService;
import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.figure.clipboard.FigureClipboardPayload;
import dev.rgcb.scholar.figure.clipboard.FigurePlainTextSerializer;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FigureEditorTest {
    private final FigurePlainTextSerializer serializer = new FigurePlainTextSerializer();

    @Test
    void figureMenuActionsWrapSelectedPlotOrDiagramInFigure() {
        var plotDocument = document(paragraph("Before"), plot("Plot"), paragraph("After"));
        var plotSession = selectedBlockSession(plotDocument, 1);
        var diagramDocument = document(paragraph("Before"), diagram("Diagram"), paragraph("After"));
        var diagramSession = selectedBlockSession(diagramDocument, 1);

        assertTrue(BuiltInEditorActions.figureMenuActions().stream().anyMatch(action -> action.id() == EditorActionId.FIGURE_WRAP_PLOT));
        assertTrue(BuiltInEditorActions.wrapPlotInFigure().isEnabled(context(plotSession)));
        assertTrue(BuiltInEditorActions.wrapPlotInFigure().execute(context(plotSession)).documentChanged());
        var plotFigure = assertInstanceOf(FigureBlock.class, plotSession.current().document().blocks().get(1));
        assertInstanceOf(PlotBlock.class, plotFigure.content());
        assertEquals("figure", plotFigure.id());

        assertTrue(BuiltInEditorActions.wrapDiagramInFigure().isEnabled(context(diagramSession)));
        assertTrue(BuiltInEditorActions.wrapDiagramInFigure().execute(context(diagramSession)).documentChanged());
        var diagramFigure = assertInstanceOf(FigureBlock.class, diagramSession.current().document().blocks().get(1));
        assertInstanceOf(DiagramBlock.class, diagramFigure.content());
        assertEquals("figure", diagramFigure.id());
    }

    @Test
    void figureCaptionCanBeEditedWithUndoRedo() {
        var figure = figure("velocity", plot("Plot"), "Old caption");
        var document = document(paragraph("Before"), figure, paragraph("After"));
        var session = selectedBlockSession(document, 1);

        assertTrue(session.supportsEditFigureCaption());
        assertTrue(session.editFigureCaption());
        assertEquals(new FigureCaptionSelection(1, 11, 11), session.current().selection());
        assertTrue(session.typeText(" updated"));

        var updated = assertInstanceOf(FigureBlock.class, session.current().document().blocks().get(1));
        assertEquals("Old caption updated", captionText(updated));
        assertTrue(session.undo());
        assertEquals(figure, session.current().document().blocks().get(1));
        assertEquals(new FigureCaptionSelection(1, 11, 11), session.current().selection());
        assertTrue(session.redo());
        assertEquals("Old caption updated", captionText((FigureBlock) session.current().document().blocks().get(1)));
    }

    @Test
    void enteringSelectedFigureEditsContainedPlotAndPreservesCaptionWhenPlotChanges() {
        var figure = figure("velocity", plot("Plot"), "Plot caption");
        var document = document(paragraph("Before"), figure, paragraph("After"));
        var session = selectedBlockSession(document, 1);

        session.enter();
        assertTrue(session.current().isPlotEditingSelection());
        assertTrue(session.applyPlotText("Updated Plot"));

        var updated = assertInstanceOf(FigureBlock.class, session.current().document().blocks().get(1));
        var updatedPlot = assertInstanceOf(PlotBlock.class, updated.content());
        assertEquals("velocity", updated.id());
        assertEquals("Plot caption", captionText(updated));
        assertEquals("Updated Plot", updatedPlot.definition().title());
    }

    @Test
    void unwrapFigureRestoresContainedBlockAndSupportsUndoRedo() {
        var figure = figure("velocity", plot("Plot"), "Caption");
        var document = document(paragraph("Before"), figure, paragraph("After"));
        var session = selectedBlockSession(document, 1);

        assertTrue(BuiltInEditorActions.unwrapFigure().isEnabled(context(session)));
        assertTrue(BuiltInEditorActions.unwrapFigure().execute(context(session)).documentChanged());
        assertInstanceOf(PlotBlock.class, session.current().document().blocks().get(1));
        assertEquals(new BlockSelection(1), session.current().selection());

        assertTrue(session.undo());
        assertEquals(figure, session.current().document().blocks().get(1));
        assertTrue(session.redo());
        assertInstanceOf(PlotBlock.class, session.current().document().blocks().get(1));
    }

    @Test
    void wholeFigureCopyWritesPlainFallbackPayloadAndLeavesStateUnchanged() {
        var figure = figure("velocity", plot("Plot"), "Velocity caption");
        var document = document(paragraph("Before"), figure, paragraph("After"));
        var session = selectedBlockSession(document, 1);
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.copy().execute(new EditorActionContext(session, clipboard, sidecar));

        assertFalse(result.documentChanged());
        assertEquals(serializer.serialize(figure, 1), clipboard.text);
        assertEquals(figure, dev.rgcb.scholar.editor.TransferClipboardAssertions.root(dev.rgcb.scholar.document.FigureBlock.class, sidecar.snapshot().orElseThrow().payload()));
        assertEquals(new BlockSelection(1), session.current().selection());
        assertFalse(session.canUndo());
    }

    @Test
    void pastedFigureKeepsStructureButGetsUniqueStableIdWhenDocumentAlreadyContainsIt() {
        var copied = figure("velocity", plot("Plot"), "Velocity caption");
        var document = document(paragraph("Before"), copied, paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 6)));
        var context = contextWithFigurePayload(session, copied);

        assertTrue(BuiltInEditorActions.paste().execute(context).documentChanged());

        var pasted = assertInstanceOf(FigureBlock.class, session.current().document().blocks().get(1));
        var original = assertInstanceOf(FigureBlock.class, session.current().document().blocks().get(2));
        assertEquals("velocity-2", pasted.id());
        assertEquals("velocity", original.id());
        assertEquals(copied.content(), pasted.content());
        assertEquals(copied.caption(), pasted.caption());
        assertEquals(new BlockSelection(1), session.current().selection());
    }

    @Test
    void staleFigureSidecarFallsBackToPlainTextAndDoesNotInferFigure() {
        var figure = figure("velocity", plot("Plot"), "Velocity caption");
        var session = new EditorSession(document(paragraph("abc")), 0);
        var clipboard = new FakeClipboard("plain external text");
        var sidecar = new ScholarClipboardService();
        sidecar.install(serializer.serialize(figure, 1), new FigureClipboardPayload(figure));

        var result = BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(result.documentChanged());
        assertEquals("abcplain external text", paragraphText(session.current().document()));
        assertTrue(sidecar.snapshot().isEmpty());
        assertEquals(1, session.current().document().blocks().size());
    }

    private EditorActionContext contextWithFigurePayload(EditorSession session, FigureBlock figure) {
        var text = serializer.serialize(figure, 1);
        var clipboard = new FakeClipboard(text);
        var sidecar = new ScholarClipboardService();
        sidecar.install(text, new FigureClipboardPayload(figure));
        return new EditorActionContext(session, clipboard, sidecar);
    }

    private static EditorActionContext context(EditorSession session) {
        return new EditorActionContext(session, new FakeClipboard(), new ScholarClipboardService());
    }

    private static EditorSession selectedBlockSession(Document document, int blockIndex) {
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(blockIndex), Optional.empty()));
        return session;
    }

    private static FigureBlock figure(String id, BlockNode content, String caption) {
        return new FigureBlock(id, content, inline(caption));
    }

    private static PlotBlock plot(String title) {
        return new PlotBlock(PlotDefinition.of(
                title,
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("Series", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(1, 1))))));
    }

    private static DiagramBlock diagram(String title) {
        return new DiagramBlock(new DiagramDefinition(
                title,
                new DiagramCanvas(100, 50),
                List.of(new DiagramNode(new DiagramElementId("node"), new DiagramBounds(10, 10, 20, 10), "Node", List.of())),
                List.of()));
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(inline(text));
    }

    private static InlineContent inline(String text) {
        return new InlineContent(List.of(new Text(text, Set.of())));
    }

    private static String captionText(FigureBlock figure) {
        return figure.caption().nodes().stream()
                .map(Text.class::cast)
                .map(Text::content)
                .reduce("", String::concat);
    }

    private static String paragraphText(Document document) {
        return ((Paragraph) document.blocks().get(0)).content().nodes().stream()
                .map(Text.class::cast)
                .map(Text::content)
                .reduce("", String::concat);
    }

    private static final class FakeClipboard implements ClipboardAdapter {
        private String text;
        private boolean writeSucceeds = true;

        private FakeClipboard() {
            this("");
        }

        private FakeClipboard(String text) {
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean setText(String text) {
            if (!writeSucceeds) {
                return false;
            }
            this.text = text;
            return true;
        }
    }
}
