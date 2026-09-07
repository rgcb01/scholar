package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.clipboard.ScholarClipboardService;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.AxisRange;
import dev.rgcb.scholar.plot.AxisScale;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.plot.clipboard.PlotClipboardPayload;
import dev.rgcb.scholar.plot.clipboard.PlotPlainTextSerializer;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlotClipboardTest {
    private final PlotPlainTextSerializer serializer = new PlotPlainTextSerializer();

    @Test
    void plainTextSerializerProducesReadableDeterministicSummary() {
        var plot = richPlot();

        assertEquals("""
                Plot: Position vs Time
                X Axis: Time (s)
                X Range: [0.0, 3.0]
                Y Axis: Position (m)
                Y Range: auto
                Legend: on
                Grid: off
                Height: 200

                Series: Motion [LINE]
                x\ty
                0.0\t0.0
                1.0\t1.0
                2.0\t4.0

                Series: Samples [SCATTER]
                x\ty
                0.5\t0.25
                1.5\t2.25""", serializer.serialize(plot));
        assertFalse(serializer.serialize(plot).endsWith("\n"));
    }

    @Test
    void plainTextSerializerSanitizesStructuralWhitespaceAndKeepsUnicode() {
        var plot = new PlotBlock(new PlotDefinition(
                "Δx\nvs\tθ",
                AxisDefinition.linear("café\r\nλ"),
                AxisDefinition.linear("Ω"),
                List.of(new PlotSeries("A\tB", PlotSeriesKind.SCATTER, List.of(new DataPoint(-1.25, 2.5)))),
                false,
                true,
                PlotDefinition.DEFAULT_HEIGHT));

        var text = serializer.serialize(plot);

        assertTrue(text.contains("Plot: Δx vs θ"));
        assertTrue(text.contains("X Axis: café λ"));
        assertTrue(text.contains("Y Axis: Ω"));
        assertTrue(text.contains("Series: A B [SCATTER]"));
        assertTrue(text.contains("-1.25\t2.5"));
    }

    @Test
    void plainTextSerializerRepresentsEmptySeriesWithoutInventingPoints() {
        var plot = new PlotBlock(PlotDefinition.of(
                "Empty",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("No data", PlotSeriesKind.LINE, List.of()))));

        assertTrue(serializer.serialize(plot).endsWith("Series: No data [LINE]\nx\ty"));
    }

    @Test
    void plotClipboardPayloadPreservesExactPlotAst() {
        var plot = richPlot();
        var payload = new PlotClipboardPayload(plot);

        assertEquals(plot, payload.plot());
        assertEquals(new AxisRange(0, 3), payload.plot().definition().xAxis().explicitRange().orElseThrow());
        assertEquals(PlotSeriesKind.SCATTER, payload.plot().definition().series().get(1).kind());
        assertEquals(new DataPoint(1.5, 2.25), payload.plot().definition().series().get(1).points().get(1));
        assertFalse(payload.plot().definition().gridVisible());
    }

    @Test
    void wholePlotCopyWritesPlainFallbackInstallsPayloadAndLeavesStateUnchanged() {
        var plot = richPlot();
        var document = document(paragraph("Before"), plot, paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.copy().execute(new EditorActionContext(session, clipboard, sidecar));

        assertFalse(result.documentChanged());
        assertEquals(serializer.serialize(plot), clipboard.text);
        assertEquals(plot, assertInstanceOf(PlotClipboardPayload.class, sidecar.snapshot().orElseThrow().payload()).plot());
        assertEquals(new BlockSelection(1), session.current().selection());
        assertFalse(session.canUndo());
    }

    @Test
    void failedWholePlotCopyDoesNotInstallPayload() {
        var plot = richPlot();
        var document = document(paragraph("Before"), plot, paragraph("After"));
        var session = selectedPlotSession(document);
        var clipboard = new FakeClipboard();
        clipboard.writeSucceeds = false;
        var sidecar = new ScholarClipboardService();

        BuiltInEditorActions.copy().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(sidecar.snapshot().isEmpty());
        assertEquals(document, session.current().document());
        assertFalse(session.canUndo());
    }

    @Test
    void wholePlotCutWritesClipboardBeforeDeletingAndUndoRedoRestoresPlotSelection() {
        var plot = richPlot();
        var document = document(paragraph("Before"), plot, paragraph("After"));
        var session = selectedPlotSession(document);
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(result.documentChanged());
        assertEquals(serializer.serialize(plot), clipboard.text);
        assertInstanceOf(PlotClipboardPayload.class, sidecar.snapshot().orElseThrow().payload());
        assertEquals(2, session.current().document().blocks().size());
        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertEquals(new BlockSelection(1), session.current().selection());
        assertTrue(session.redo());
        assertEquals(2, session.current().document().blocks().size());
    }

    @Test
    void failedWholePlotCutDoesNotDeleteOrCreateHistory() {
        var document = document(paragraph("Before"), richPlot(), paragraph("After"));
        var session = selectedPlotSession(document);
        var clipboard = new FakeClipboard();
        clipboard.writeSucceeds = false;
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard, sidecar));

        assertFalse(result.documentChanged());
        assertEquals(document, session.current().document());
        assertFalse(session.canUndo());
        assertTrue(sidecar.snapshot().isEmpty());
    }

    @Test
    void nativePlotPasteAtTextCaretSplitsParagraphAndSelectsInsertedPlot() {
        var copied = richPlot();
        var document = document(paragraph("helloworld"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 5)));
        var context = contextWithPayload(session, copied);

        var result = BuiltInEditorActions.paste().execute(context);

        assertTrue(result.documentChanged());
        assertText("hello", session.current().document().blocks().get(0));
        assertEquals(copied, session.current().document().blocks().get(1));
        assertText("world", session.current().document().blocks().get(2));
        assertEquals(new BlockSelection(1), session.current().selection());
    }

    @Test
    void nativePlotPasteOverTextSelectionUsesStructuralReplacement() {
        var copied = richPlot();
        var document = document(paragraph("abcdef"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 2), new DocumentPosition(0, 4)));

        BuiltInEditorActions.paste().execute(contextWithPayload(session, copied));

        assertText("ab", session.current().document().blocks().get(0));
        assertEquals(copied, session.current().document().blocks().get(1));
        assertText("ef", session.current().document().blocks().get(2));
        assertEquals(new BlockSelection(1), session.current().selection());
    }

    @Test
    void nativePlotPasteReplacesSelectedPlotAndSupportsUndoRedo() {
        var original = simplePlot("Original", 1);
        var copied = richPlot();
        var document = document(paragraph("Before"), original, paragraph("After"));
        var session = selectedPlotSession(document);

        var result = BuiltInEditorActions.paste().execute(contextWithPayload(session, copied));

        assertTrue(result.documentChanged());
        assertEquals(copied, session.current().document().blocks().get(1));
        assertEquals(new BlockSelection(1), session.current().selection());
        assertTrue(session.undo());
        assertEquals(original, session.current().document().blocks().get(1));
        assertEquals(new BlockSelection(1), session.current().selection());
        assertTrue(session.redo());
        assertEquals(copied, session.current().document().blocks().get(1));
    }

    @Test
    void nativePlotPasteDoesNotReplaceOtherAtomicBlockSelection() {
        var document = document(paragraph("Before"), new EquationBlock(new MathIdentifier("x")), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        var context = contextWithPayload(session, richPlot());

        assertFalse(BuiltInEditorActions.paste().isEnabled(context));
        assertFalse(BuiltInEditorActions.paste().execute(context).documentChanged());
        assertEquals(document, session.current().document());
    }

    @Test
    void plotEditingModeDoesNotUseWholePlotClipboardPayload() {
        var plot = richPlot();
        var document = document(paragraph("Before"), plot, paragraph("After"));
        var session = selectedPlotSession(document);
        session.enter();
        var context = contextWithPayload(session, simplePlot("Replacement", 2));

        assertFalse(BuiltInEditorActions.copy().isEnabled(context));
        assertFalse(BuiltInEditorActions.cut().isEnabled(context));
        assertFalse(BuiltInEditorActions.paste().isEnabled(context));
        assertEquals(plot, session.current().document().blocks().get(1));
    }

    @Test
    void stalePlotSidecarFallsBackToPlainTextAndDoesNotInferPlot() {
        var session = new EditorSession(document(paragraph("abc")), 0);
        var clipboard = new FakeClipboard("plain external text");
        var sidecar = new ScholarClipboardService();
        sidecar.install(serializer.serialize(richPlot()), new PlotClipboardPayload(richPlot()));

        var result = BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(result.documentChanged());
        assertEquals("abcplain external text", paragraphText(session.current().document()));
        assertTrue(sidecar.snapshot().isEmpty());
        assertEquals(1, session.current().document().blocks().size());
    }

    private static EditorSession selectedPlotSession(Document document) {
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        return session;
    }

    private EditorActionContext contextWithPayload(EditorSession session, PlotBlock plot) {
        var text = serializer.serialize(plot);
        var clipboard = new FakeClipboard(text);
        var sidecar = new ScholarClipboardService();
        sidecar.install(text, new PlotClipboardPayload(plot));
        return new EditorActionContext(session, clipboard, sidecar);
    }

    private static PlotBlock richPlot() {
        return new PlotBlock(new PlotDefinition(
                "Position vs Time",
                new AxisDefinition("Time (s)", Optional.of(new AxisRange(0, 3)), AxisScale.LINEAR),
                AxisDefinition.linear("Position (m)"),
                List.of(
                        new PlotSeries("Motion", PlotSeriesKind.LINE, List.of(
                                new DataPoint(0, 0), new DataPoint(1, 1), new DataPoint(2, 4))),
                        new PlotSeries("Samples", PlotSeriesKind.SCATTER, List.of(
                                new DataPoint(0.5, 0.25), new DataPoint(1.5, 2.25)))),
                true,
                false,
                200));
    }

    private static PlotBlock simplePlot(String title, double y) {
        return new PlotBlock(PlotDefinition.of(
                title,
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("A", PlotSeriesKind.LINE, List.of(new DataPoint(0, y))))));
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String value) {
        return new Paragraph(new InlineContent(List.of(new Text(value, Set.of()))));
    }

    private static void assertText(String expected, BlockNode block) {
        var text = assertInstanceOf(Text.class, assertInstanceOf(Paragraph.class, block).content().nodes().get(0));
        assertEquals(expected, text.content());
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
