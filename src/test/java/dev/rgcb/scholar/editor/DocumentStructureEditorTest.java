package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.clipboard.DocumentBlockClipboardPayload;
import dev.rgcb.scholar.clipboard.ScholarClipboardPayload;
import dev.rgcb.scholar.clipboard.ScholarClipboardService;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.Text;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DocumentStructureEditorTest {
    @Test
    void insertTableOfContentsInsertsSemanticBlockAndUsesHistory() {
        var session = new EditorSession(document(paragraph("Intro")), 0);

        assertTrue(session.insertTableOfContents());

        assertInstanceOf(TableOfContentsBlock.class, session.current().document().blocks().get(1));
        assertEquals(new BlockSelection(1), session.current().selection());
        assertTrue(session.canUndo());

        session.undo();

        assertEquals(1, session.current().document().blocks().size());
        assertFalse(session.canUndo());
    }

    @Test
    void insertTableOfContentsActionUsesSharedInsertAction() {
        var session = new EditorSession(document(paragraph("Intro")), 0);
        var action = BuiltInEditorActions.insertMenuActions().stream()
                .filter(candidate -> candidate.id() == EditorActionId.INSERT_TABLE_OF_CONTENTS)
                .findFirst()
                .orElseThrow();

        assertTrue(action.isEnabled(new EditorActionContext(session, new FakeClipboard())));
        assertTrue(action.execute(new EditorActionContext(session, new FakeClipboard())).documentChanged());
        assertInstanceOf(TableOfContentsBlock.class, session.current().document().blocks().get(1));
    }

    @Test
    void navigationToHeadingIdMovesCaretWithoutHistoryMutation() {
        var session = new EditorSession(document(
                paragraph("Intro"),
                heading("methods", 1, "Methods"),
                paragraph("Body")), 0);

        assertTrue(session.navigateToHeadingId("methods"));

        assertEquals(new TextSelection(new DocumentPosition(1, 0), new DocumentPosition(1, 0)), session.current().selection());
        assertFalse(session.canUndo());
    }

    @Test
    void navigationToMissingHeadingIdIsNoOp() {
        var session = new EditorSession(document(paragraph("Intro")), 0);
        var before = session.current();

        assertFalse(session.navigateToHeadingId("missing"));
        assertEquals(before, session.current());
        assertFalse(session.canUndo());
    }

    @Test
    void viewOutlineActionRequestsShellToggleWithoutDocumentMutation() {
        var session = new EditorSession(document(paragraph("Intro")), 0);
        var action = BuiltInEditorActions.viewMenuActions().get(0);
        var result = action.execute(new EditorActionContext(session, new FakeClipboard()));

        assertEquals(EditorActionId.TOGGLE_OUTLINE, action.id());
        assertTrue(result.toggleOutline());
        assertFalse(result.documentChanged());
    }

    @Test
    void headingBlockCopyInstallsStructuredPayloadAndReadableNumberedFallback() {
        var session = new EditorSession(document(
                heading("motion", 1, "Motion"),
                heading("average", 2, "Average velocity")), 0);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(1), Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();

        BuiltInEditorActions.copy().execute(new EditorActionContext(session, clipboard, sidecar));

        assertEquals("1.1 Average velocity", clipboard.text);
        var payload = dev.rgcb.scholar.editor.TransferClipboardAssertions.root(dev.rgcb.scholar.document.BlockNode.class, sidecar.snapshot().orElseThrow().payload());
        var heading = assertInstanceOf(Heading.class, payload);
        assertEquals("average", heading.id().orElseThrow());
    }

    @Test
    void headingBlockPasteRemapsDuplicateStableId() {
        var session = new EditorSession(document(heading("motion", 1, "Motion")), 0);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(0), Optional.empty()));
        var clipboard = new FakeClipboard();
        clipboard.text = "1 Motion";
        var sidecar = new ScholarClipboardService();
        sidecar.install("1 Motion", new DocumentBlockClipboardPayload(heading("motion", 1, "Motion")));
        var context = new EditorActionContext(session, clipboard, sidecar);

        assertTrue(BuiltInEditorActions.paste().isEnabled(context));
        assertTrue(BuiltInEditorActions.paste().execute(context).documentChanged());

        var pasted = assertInstanceOf(Heading.class, session.current().document().blocks().get(1));
        assertEquals("motion-2", pasted.id().orElseThrow());
    }

    @Test
    void tableOfContentsBlockCopyPasteUsesSemanticPayload() {
        var session = new EditorSession(document(paragraph("Intro"), new TableOfContentsBlock()), 0);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(1), Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        var context = new EditorActionContext(session, clipboard, sidecar);

        assertTrue(BuiltInEditorActions.copy().isEnabled(context));
        BuiltInEditorActions.copy().execute(context);

        assertTrue(clipboard.text.startsWith("Contents"));
        dev.rgcb.scholar.editor.TransferClipboardAssertions.root(dev.rgcb.scholar.document.BlockNode.class, sidecar.snapshot().orElseThrow().payload());

        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 5)));
        assertTrue(BuiltInEditorActions.paste().execute(context).documentChanged());
        assertInstanceOf(TableOfContentsBlock.class, session.current().document().blocks().get(1));
    }

    @Test
    void tableOfContentsPasteDoesNotDependOnCopiedDerivedEntries() {
        var source = new EditorSession(document(paragraph("Intro"), new TableOfContentsBlock(), heading("source", 1, "Source")), 0);
        source.setCurrent(new EditorState(source.current().document(), new BlockSelection(1), Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        BuiltInEditorActions.copy().execute(new EditorActionContext(source, clipboard, sidecar));

        var target = new EditorSession(document(paragraph("Intro"), heading("target", 1, "Target")), 0);
        target.setCurrent(new EditorState(target.current().document(), new DocumentPosition(0, 5)));
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(target, clipboard, sidecar)).documentChanged());

        assertInstanceOf(TableOfContentsBlock.class, target.current().document().blocks().get(1));
    }

    @Test
    void headingBlockCutWritesClipboardBeforeDeleting() {
        var session = new EditorSession(document(heading("motion", 1, "Motion"), paragraph("Body")), 0);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(0), Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();

        assertTrue(BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard, sidecar)).documentChanged());

        assertEquals("1 Motion", clipboard.text);
        assertInstanceOf(Paragraph.class, session.current().document().blocks().get(0));
        assertTrue(session.canUndo());
        session.undo();
        assertInstanceOf(Heading.class, session.current().document().blocks().get(0));
    }

    @Test
    void failedHeadingCutDoesNotDeleteBlock() {
        var session = new EditorSession(document(heading("motion", 1, "Motion"), paragraph("Body")), 0);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(0), Optional.empty()));
        var clipboard = new FakeClipboard();
        clipboard.writeSucceeds = false;

        assertFalse(BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard)).documentChanged());
        assertInstanceOf(Heading.class, session.current().document().blocks().get(0));
        assertFalse(session.canUndo());
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Heading heading(String id, int level, String text) {
        return new Heading(id, level, inline(text));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(inline(text));
    }

    private static InlineContent inline(String text) {
        return new InlineContent(List.of(new Text(text, Set.of())).stream()
                .map(InlineNode.class::cast)
                .toList());
    }

    private static final class FakeClipboard implements ClipboardAdapter {
        private String text = "";
        private boolean writeSucceeds = true;

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
