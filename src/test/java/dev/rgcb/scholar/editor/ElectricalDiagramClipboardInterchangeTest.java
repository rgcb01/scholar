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
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalJunction;
import dev.rgcb.scholar.electrical.ElectricalNetResolver;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import dev.rgcb.scholar.math.MathIdentifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** M18F clipboard/interchange regressions for electrical and mixed diagrams. */
class ElectricalDiagramClipboardInterchangeTest {
    private final DiagramPlainTextSerializer serializer = new DiagramPlainTextSerializer();

    @Test
    void plainTextFallbackDescribesElectricalSemanticsJunctionNetAndAuthoredWorkspaceHeight() {
        var text = serializer.serialize(mixedElectricalDiagram());

        assertTrue(text.startsWith("Diagram: Mixed Electrical\nCanvas: 180.0 x 100.0\nWorkspace Aspect Ratio: 0.75"));
        assertTrue(text.contains("Node: Sensor [sensor]"));
        assertTrue(text.contains("Component: RESISTOR R1 [r1]"));
        assertTrue(text.contains("Orientation: DEG_90"));
        assertTrue(text.contains("Value: 10 kΩ"));
        assertTrue(text.contains("Terminal: a [PASSIVE_A, TOP @ 0.5]"));
        assertTrue(text.contains("Terminal: b [PASSIVE_B, BOTTOM @ 0.5]"));
        assertTrue(text.contains("Component: DIODE D1 [d1]"));
        assertTrue(text.contains("Terminal: anode [ANODE, RIGHT @ 0.5]"));
        assertTrue(text.contains("Terminal: cathode [CATHODE, LEFT @ 0.5]"));
        assertTrue(text.contains("Junction: [j1]"));
        assertTrue(text.contains("Net Label: VOUT"));
        assertTrue(text.contains("r1/b -> j1/left"));
        assertTrue(text.contains("j1/right -> c1/a"));
        assertFalse(text.endsWith("\n"));
    }

    @Test
    void nativePayloadPreservesExactElectricalMixedDiagramIncludingWorkspaceAndLocalIds() {
        var diagram = mixedElectricalDiagram();
        var payload = new DiagramClipboardPayload(diagram);

        assertEquals(diagram, payload.diagram());
        assertEquals(0.75, payload.diagram().workspaceAspectRatio());
        assertEquals(diagram.definition().elements(), payload.diagram().definition().elements());
        assertEquals(diagram.definition().connections(), payload.diagram().definition().connections());
        assertEquals(new DiagramElementId("r1"), payload.diagram().definition().elements().get(1).id());
        assertEquals(new DiagramElementId("j1"), payload.diagram().definition().elements().get(2).id());

        var connectivity = new ElectricalNetResolver().resolve(payload.diagram().definition());
        assertTrue(connectivity.electricallyConnected(endpoint("r1", "b"), endpoint("c1", "a")));
        assertTrue(connectivity.electricallyConnected(endpoint("r1", "b"), endpoint("d1", "anode")));
        assertEquals("VOUT", connectivity.netFor(endpoint("r1", "b")).orElseThrow().label().orElseThrow());
    }

    @Test
    void wholeElectricalDiagramCopyWritesReadableFallbackAndLosslessNativePayload() {
        var diagram = mixedElectricalDiagram();
        var document = document(heading("Circuit"), paragraph("Before"), diagram, new EquationBlock(new MathIdentifier("V")), paragraph("After"));
        var session = selectedBlockSession(document, 2);
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.copy().execute(new EditorActionContext(session, clipboard, sidecar));

        assertFalse(result.documentChanged());
        assertEquals(serializer.serialize(diagram), clipboard.text);
        assertEquals(diagram, dev.rgcb.scholar.editor.TransferClipboardAssertions.root(dev.rgcb.scholar.document.DiagramBlock.class, sidecar.snapshot().orElseThrow().payload()));
        assertEquals(document, session.current().document());
        assertEquals(new BlockSelection(2), session.current().selection());
        assertFalse(session.canUndo());
    }

    @Test
    void nativeElectricalDiagramPasteIntoMixedDocumentPreservesTopologyAndSurroundingBlocks() {
        var copied = mixedElectricalDiagram();
        var before = heading("Lab");
        var afterEquation = new EquationBlock(new MathIdentifier("I"));
        var document = document(before, paragraph("abcdef"), afterEquation, paragraph("tail"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(1, 3)));

        var result = BuiltInEditorActions.paste().execute(contextWithPayload(session, copied));

        assertTrue(result.documentChanged());
        assertEquals(before, session.current().document().blocks().get(0));
        assertText("abc", session.current().document().blocks().get(1));
        var pasted = assertInstanceOf(DiagramBlock.class, session.current().document().blocks().get(2));
        assertEquals(copied, pasted);
        assertText("def", session.current().document().blocks().get(3));
        assertEquals(afterEquation, session.current().document().blocks().get(4));
        assertText("tail", session.current().document().blocks().get(5));
        assertEquals(new BlockSelection(2), session.current().selection());
        assertEquals(
                new ElectricalNetResolver().resolve(copied.definition()),
                new ElectricalNetResolver().resolve(pasted.definition()));
    }

    @Test
    void nativeElectricalDiagramPasteReplacementIsOneUndoableEditAndRestoresOriginal() {
        var original = simpleElectricalDiagram("Original", "R9");
        var copied = mixedElectricalDiagram();
        var document = document(paragraph("Before"), original, paragraph("After"));
        var session = selectedBlockSession(document, 1);

        assertTrue(BuiltInEditorActions.paste().execute(contextWithPayload(session, copied)).documentChanged());
        assertEquals(copied, session.current().document().blocks().get(1));
        assertEquals(new BlockSelection(1), session.current().selection());

        assertTrue(session.undo());
        assertEquals(original, session.current().document().blocks().get(1));
        assertEquals(new BlockSelection(1), session.current().selection());
        assertFalse(session.canUndo());

        assertTrue(session.redo());
        assertEquals(copied, session.current().document().blocks().get(1));
        assertEquals(new BlockSelection(1), session.current().selection());
    }

    @Test
    void wholeElectricalDiagramCutIsAtomicAndUndoRestoresExactDiagram() {
        var diagram = mixedElectricalDiagram();
        var document = document(heading("Circuit"), diagram, paragraph("After"));
        var session = selectedBlockSession(document, 1);
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(result.documentChanged());
        assertEquals(serializer.serialize(diagram), clipboard.text);
        assertEquals(diagram, dev.rgcb.scholar.editor.TransferClipboardAssertions.root(dev.rgcb.scholar.document.DiagramBlock.class, sidecar.snapshot().orElseThrow().payload()));
        assertEquals(2, session.current().document().blocks().size());
        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertEquals(new BlockSelection(1), session.current().selection());
        assertFalse(session.canUndo());
    }

    @Test
    void copyingAndPastingWholeDiagramPreservesLocalIdsWithoutGlobalDocumentCollision() {
        var diagram = mixedElectricalDiagram();
        var document = document(diagram, paragraph("middle"));
        var sourceSession = selectedBlockSession(document, 0);
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        BuiltInEditorActions.copy().execute(new EditorActionContext(sourceSession, clipboard, sidecar));

        var targetSession = new EditorSession(document, 1);
        targetSession.setCurrent(new EditorState(document, new DocumentPosition(1, 3)));
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(targetSession, clipboard, sidecar)).documentChanged());

        var first = assertInstanceOf(DiagramBlock.class, targetSession.current().document().blocks().get(0));
        var second = assertInstanceOf(DiagramBlock.class, targetSession.current().document().blocks().get(2));
        assertEquals(first, second);
        assertEquals(
                first.definition().elements().stream().map(element -> element.id()).toList(),
                second.definition().elements().stream().map(element -> element.id()).toList());
    }

    @Test
    void externalElectricalLookingTextRemainsPlainTextAndNeverInfersDiagramAst() {
        var electricalText = serializer.serialize(mixedElectricalDiagram());
        var document = document(paragraph("prefix:"));
        var session = new EditorSession(document, 0);
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.paste().execute(
                new EditorActionContext(session, new FakeClipboard(electricalText), sidecar));

        assertTrue(result.documentChanged());
        assertEquals(1, session.current().document().blocks().size());
        assertText("prefix:" + electricalText.replace('\n', ' '), session.current().document().blocks().get(0));
        assertTrue(sidecar.snapshot().isEmpty());
    }

    @Test
    void staleElectricalNativePayloadIsInvalidatedWhenSystemClipboardTextChanges() {
        var diagram = mixedElectricalDiagram();
        var copiedText = serializer.serialize(diagram);
        var sidecar = new ScholarClipboardService();
        sidecar.install(copiedText, new DiagramClipboardPayload(diagram));
        var document = document(paragraph("abc"));
        var session = new EditorSession(document, 0);

        var result = BuiltInEditorActions.paste().execute(
                new EditorActionContext(session, new FakeClipboard("external replacement"), sidecar));

        assertTrue(result.documentChanged());
        assertText("abcexternal replacement", session.current().document().blocks().get(0));
        assertTrue(sidecar.snapshot().isEmpty());
        assertEquals(1, session.current().document().blocks().size());
    }

    @Test
    void defaultWorkspaceAspectRatioDoesNotAddNoiseToLegacyPlainTextFallback() {
        var diagram = simpleElectricalDiagram("Default Workspace", "R1");
        var text = serializer.serialize(diagram);

        assertFalse(text.contains("Workspace Aspect Ratio:"));
        assertTrue(text.contains("Terminal: a [PASSIVE_A, LEFT @ 0.5]"));
        assertTrue(text.contains("Terminal: b [PASSIVE_B, RIGHT @ 0.5]"));
    }

    private EditorActionContext contextWithPayload(EditorSession session, DiagramBlock diagram) {
        var text = serializer.serialize(diagram);
        var clipboard = new FakeClipboard(text);
        var sidecar = new ScholarClipboardService();
        sidecar.install(text, new DiagramClipboardPayload(diagram));
        return new EditorActionContext(session, clipboard, sidecar);
    }

    private static EditorSession selectedBlockSession(Document document, int blockIndex) {
        var initialTextBlock = 0;
        while (initialTextBlock < document.blocks().size()
                && !(document.blocks().get(initialTextBlock) instanceof Paragraph)
                && !(document.blocks().get(initialTextBlock) instanceof Heading)) {
            initialTextBlock++;
        }
        if (initialTextBlock >= document.blocks().size()) {
            throw new IllegalArgumentException("Test document must contain at least one editable text block.");
        }
        var session = new EditorSession(document, initialTextBlock);
        session.setCurrent(new EditorState(document, new BlockSelection(blockIndex), Optional.empty()));
        return session;
    }

    private static DiagramBlock mixedElectricalDiagram() {
        var sensorId = new DiagramElementId("sensor");
        var r1Id = new DiagramElementId("r1");
        var junctionId = new DiagramElementId("j1");
        var capacitorId = new DiagramElementId("c1");
        var diodeId = new DiagramElementId("d1");
        var groundId = new DiagramElementId("gnd1");
        var sensorOut = new DiagramPortId("out");

        var sensor = new DiagramNode(
                sensorId,
                new DiagramBounds(6, 10, 24, 16),
                "Sensor",
                List.of(new DiagramPort(sensorOut, "signal", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5))));
        var resistor = new ElectricalComponent(
                r1Id,
                new DiagramBounds(45, 8, 14, 34),
                ElectricalComponentKind.RESISTOR,
                ElectricalOrientation.DEG_90,
                "R1",
                "10 kΩ");
        var junction = new ElectricalJunction(junctionId, new DiagramBounds(82, 22, 4, 4), "VOUT");
        var capacitor = new ElectricalComponent(
                capacitorId,
                new DiagramBounds(112, 15, 28, 14),
                ElectricalComponentKind.CAPACITOR,
                ElectricalOrientation.DEG_0,
                "C1",
                "100 nF");
        var diode = new ElectricalComponent(
                diodeId,
                new DiagramBounds(108, 48, 30, 14),
                ElectricalComponentKind.DIODE,
                ElectricalOrientation.DEG_180,
                "D1",
                "1N4148");
        var ground = new ElectricalComponent(
                groundId,
                new DiagramBounds(78, 76, 16, 16),
                ElectricalComponentKind.GROUND,
                ElectricalOrientation.DEG_0,
                "GND1",
                "");

        return new DiagramBlock(new DiagramDefinition(
                "Mixed Electrical",
                new DiagramCanvas(180, 100),
                List.of(sensor, resistor, junction, capacitor, diode, ground),
                List.of(
                        connection(sensorId, "out", r1Id, "a", "sensor"),
                        connection(r1Id, "b", junctionId, "left", ""),
                        connection(junctionId, "right", capacitorId, "a", ""),
                        connection(junctionId, "bottom", diodeId, "anode", ""),
                        connection(capacitorId, "b", groundId, "ground", ""),
                        connection(diodeId, "cathode", groundId, "ground", ""))),
                0.75);
    }

    private static DiagramBlock simpleElectricalDiagram(String title, String reference) {
        var resistor = new ElectricalComponent(
                new DiagramElementId("r1"),
                new DiagramBounds(10, 10, 30, 10),
                ElectricalComponentKind.RESISTOR,
                ElectricalOrientation.DEG_0,
                reference,
                "1 kΩ");
        return new DiagramBlock(new DiagramDefinition(
                title,
                new DiagramCanvas(100, 50),
                List.of(resistor),
                List.of()));
    }

    private static DiagramConnection connection(
            DiagramElementId sourceElement,
            String sourcePort,
            DiagramElementId targetElement,
            String targetPort,
            String label
    ) {
        return new DiagramConnection(
                new DiagramEndpoint(sourceElement, new DiagramPortId(sourcePort)),
                new DiagramEndpoint(targetElement, new DiagramPortId(targetPort)),
                label);
    }

    private static DiagramEndpoint endpoint(String elementId, String portId) {
        return new DiagramEndpoint(new DiagramElementId(elementId), new DiagramPortId(portId));
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(inline(text));
    }

    private static Heading heading(String text) {
        return new Heading(2, inline(text));
    }

    private static InlineContent inline(String text) {
        return new InlineContent(List.of(new Text(text, Set.of())));
    }

    private static void assertText(String expected, BlockNode block) {
        var paragraph = assertInstanceOf(Paragraph.class, block);
        var text = paragraph.content().nodes().stream()
                .map(Text.class::cast)
                .map(Text::content)
                .reduce("", String::concat);
        assertEquals(expected, text);
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
