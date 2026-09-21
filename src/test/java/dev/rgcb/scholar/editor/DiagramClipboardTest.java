package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.clipboard.ScholarClipboardService;
import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import dev.rgcb.scholar.diagram.clipboard.DiagramClipboardPayload;
import dev.rgcb.scholar.diagram.clipboard.DiagramPlainTextSerializer;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.math.MathIdentifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiagramClipboardTest {
    private final DiagramPlainTextSerializer serializer = new DiagramPlainTextSerializer();

    @Test
    void plainTextSerializerProducesReadableDeterministicSummary() {
        var diagram = richDiagram();

        assertEquals("""
                Diagram: Control Loop
                Canvas: 120.0 x 60.0

                Node: Sensor [sensor]
                Bounds: [8.0, 8.0, 28.0, 18.0]
                Port: out [RIGHT @ 0.5] - measurement
                Port: power [BOTTOM @ 0.75] - V+

                Node: Processor [processor]
                Bounds: [72.0, 30.0, 36.0, 20.0]
                Port: in [LEFT @ 0.5] - input

                Connections:
                sensor/out -> processor/in : signal""", serializer.serialize(diagram));
        assertFalse(serializer.serialize(diagram).endsWith("\n"));
    }

    @Test
    void plainTextSerializerSanitizesStructuralWhitespaceAndKeepsUnicode() {
        var sensor = new DiagramElementId("sensor\nα");
        var out = new DiagramPortId("out\tβ");
        var diagram = new DiagramBlock(new DiagramDefinition(
                "Δx\nvs\tθ",
                new DiagramCanvas(50, 25),
                List.of(new DiagramNode(
                        sensor,
                        new DiagramBounds(5, 5, 20, 10),
                        "café\r\nλ",
                        List.of(new DiagramPort(out, "Ω\tport", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5))))),
                List.of()));

        var text = serializer.serialize(diagram);

        assertTrue(text.contains("Diagram: Δx vs θ"));
        assertTrue(text.contains("Node: café λ [sensor α]"));
        assertTrue(text.contains("Port: out β [RIGHT @ 0.5] - Ω port"));
    }

    @Test
    void plainTextSerializerRepresentsEmptyDiagramWithoutInventingStructure() {
        var diagram = new DiagramBlock(new DiagramDefinition(
                "Empty", new DiagramCanvas(80, 40), List.of(), List.of()));

        assertEquals("Diagram: Empty\nCanvas: 80.0 x 40.0", serializer.serialize(diagram));
    }

    @Test
    void diagramClipboardPayloadPreservesExactDiagramAstAndReferenceIds() {
        var diagram = richDiagram();
        var payload = new DiagramClipboardPayload(diagram);

        assertEquals(diagram, payload.diagram());
        var definition = payload.diagram().definition();
        assertEquals(new DiagramElementId("sensor"), definition.elements().get(0).id());
        assertEquals(new DiagramPortId("power"), definition.elements().get(0).ports().get(1).id());
        assertEquals(
                new DiagramEndpoint(new DiagramElementId("processor"), new DiagramPortId("in")),
                definition.connections().get(0).target());
        assertEquals(new DiagramBounds(72, 30, 36, 20), definition.elements().get(1).bounds());
    }

    @Test
    void wholeDiagramCopyWritesPlainFallbackInstallsPayloadAndLeavesStateUnchanged() {
        var diagram = richDiagram();
        var document = document(paragraph("Before"), diagram, paragraph("After"));
        var session = selectedDiagramSession(document);
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.copy().execute(new EditorActionContext(session, clipboard, sidecar));

        assertFalse(result.documentChanged());
        assertEquals(serializer.serialize(diagram), clipboard.text);
        assertEquals(diagram, dev.rgcb.scholar.editor.TransferClipboardAssertions.root(dev.rgcb.scholar.document.DiagramBlock.class, sidecar.snapshot().orElseThrow().payload()));
        assertEquals(new BlockSelection(1), session.current().selection());
        assertFalse(session.canUndo());
    }

    @Test
    void failedWholeDiagramCopyDoesNotInstallPayload() {
        var document = document(paragraph("Before"), richDiagram(), paragraph("After"));
        var session = selectedDiagramSession(document);
        var clipboard = new FakeClipboard();
        clipboard.writeSucceeds = false;
        var sidecar = new ScholarClipboardService();

        BuiltInEditorActions.copy().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(sidecar.snapshot().isEmpty());
        assertEquals(document, session.current().document());
        assertFalse(session.canUndo());
    }

    @Test
    void wholeDiagramCutWritesClipboardBeforeDeletingAndUndoRedoRestoresDiagramSelection() {
        var diagram = richDiagram();
        var document = document(paragraph("Before"), diagram, paragraph("After"));
        var session = selectedDiagramSession(document);
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(result.documentChanged());
        assertEquals(serializer.serialize(diagram), clipboard.text);
        dev.rgcb.scholar.editor.TransferClipboardAssertions.root(dev.rgcb.scholar.document.DiagramBlock.class, sidecar.snapshot().orElseThrow().payload());
        assertEquals(2, session.current().document().blocks().size());
        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertEquals(new BlockSelection(1), session.current().selection());
        assertTrue(session.redo());
        assertEquals(2, session.current().document().blocks().size());
    }

    @Test
    void failedWholeDiagramCutDoesNotDeleteOrCreateHistory() {
        var document = document(paragraph("Before"), richDiagram(), paragraph("After"));
        var session = selectedDiagramSession(document);
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
    void nativeDiagramPasteAtTextCaretSplitsParagraphAndSelectsInsertedDiagram() {
        var copied = richDiagram();
        var document = document(paragraph("helloworld"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 5)));

        var result = BuiltInEditorActions.paste().execute(contextWithPayload(session, copied));

        assertTrue(result.documentChanged());
        assertText("hello", session.current().document().blocks().get(0));
        assertEquals(copied, session.current().document().blocks().get(1));
        assertText("world", session.current().document().blocks().get(2));
        assertEquals(new BlockSelection(1), session.current().selection());
    }

    @Test
    void nativeDiagramPasteOverTextSelectionUsesStructuralReplacement() {
        var copied = richDiagram();
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
    void nativeDiagramPasteReplacesSelectedDiagramAndSupportsUndoRedo() {
        var original = simpleDiagram("Original", 10);
        var copied = richDiagram();
        var document = document(paragraph("Before"), original, paragraph("After"));
        var session = selectedDiagramSession(document);

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
    void nativeDiagramPasteDoesNotReplaceOtherAtomicBlockSelection() {
        var document = document(paragraph("Before"), new EquationBlock(new MathIdentifier("x")), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        var context = contextWithPayload(session, richDiagram());

        assertFalse(BuiltInEditorActions.paste().isEnabled(context));
        assertFalse(BuiltInEditorActions.paste().execute(context).documentChanged());
        assertEquals(document, session.current().document());
    }

    @Test
    void diagramEditingModeDoesNotUseWholeDiagramClipboardPayload() {
        var diagram = richDiagram();
        var document = document(paragraph("Before"), diagram, paragraph("After"));
        var session = selectedDiagramSession(document);
        session.enter();
        var context = contextWithPayload(session, simpleDiagram("Replacement", 20));

        assertFalse(BuiltInEditorActions.copy().isEnabled(context));
        assertFalse(BuiltInEditorActions.cut().isEnabled(context));
        assertFalse(BuiltInEditorActions.paste().isEnabled(context));
        assertEquals(diagram, session.current().document().blocks().get(1));
    }

    @Test
    void staleDiagramSidecarFallsBackToPlainTextAndDoesNotInferDiagram() {
        var session = new EditorSession(document(paragraph("abc")), 0);
        var clipboard = new FakeClipboard("plain external text");
        var sidecar = new ScholarClipboardService();
        sidecar.install(serializer.serialize(richDiagram()), new DiagramClipboardPayload(richDiagram()));

        var result = BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(result.documentChanged());
        assertEquals("abcplain external text", paragraphText(session.current().document()));
        assertTrue(sidecar.snapshot().isEmpty());
        assertEquals(1, session.current().document().blocks().size());
    }

    @Test
    void externalDiagramLookingTextRemainsPlainTextWithoutNativePayload() {
        var text = serializer.serialize(richDiagram());
        var document = document(paragraph("prefix:"));
        var session = new EditorSession(document, 0);
        var context = new EditorActionContext(session, new FakeClipboard(text), new ScholarClipboardService());

        var result = BuiltInEditorActions.paste().execute(context);

        assertTrue(result.documentChanged());
        assertEquals("prefix:" + text.replace('\n', ' '), paragraphText(session.current().document()));
        assertEquals(1, session.current().document().blocks().size());
    }

    private static EditorSession selectedDiagramSession(Document document) {
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        return session;
    }

    private EditorActionContext contextWithPayload(EditorSession session, DiagramBlock diagram) {
        var text = serializer.serialize(diagram);
        var clipboard = new FakeClipboard(text);
        var sidecar = new ScholarClipboardService();
        sidecar.install(text, new DiagramClipboardPayload(diagram));
        return new EditorActionContext(session, clipboard, sidecar);
    }

    private static DiagramBlock richDiagram() {
        var sensor = new DiagramElementId("sensor");
        var processor = new DiagramElementId("processor");
        var out = new DiagramPortId("out");
        var power = new DiagramPortId("power");
        var in = new DiagramPortId("in");
        return new DiagramBlock(new DiagramDefinition(
                "Control Loop",
                new DiagramCanvas(120, 60),
                List.of(
                        new DiagramNode(sensor, new DiagramBounds(8, 8, 28, 18), "Sensor", List.of(
                                new DiagramPort(out, "measurement", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)),
                                new DiagramPort(power, "V+", new DiagramPortPlacement(DiagramPortSide.BOTTOM, 0.75)))),
                        new DiagramNode(processor, new DiagramBounds(72, 30, 36, 20), "Processor", List.of(
                                new DiagramPort(in, "input", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5))))),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(sensor, out),
                        new DiagramEndpoint(processor, in),
                        "signal"))));
    }

    private static DiagramBlock simpleDiagram(String title, double y) {
        var id = new DiagramElementId("node");
        return new DiagramBlock(new DiagramDefinition(
                title,
                new DiagramCanvas(100, 50),
                List.of(new DiagramNode(id, new DiagramBounds(10, y, 20, 10), "Node", List.of())),
                List.of()));
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
