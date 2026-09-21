package dev.rgcb.scholar.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static dev.rgcb.scholar.persistence.DocumentJsonCodecTest.*;
import dev.rgcb.scholar.client.DevelopmentDocument;
import dev.rgcb.scholar.clipboard.*;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.editor.*;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentWorkspaceTest {
    @TempDir Path directory;
    final Document initial = new Document(List.of(paragraph("initial")));
    DocumentWorkspace workspace() { return new DocumentWorkspace(initial, new FileDocumentStorage(directory)); }

    @Test void savedSnapshotDirtyStateTracksEditUndoRedoWithoutHistoryForSave() {
        var workspace = workspace(); var session = workspace.session();
        assertTrue(workspace.isDirty());
        success(workspace.saveAs("doc"));
        assertFalse(workspace.isDirty()); assertEquals(0, session.undoDepth());
        session.moveLeft(); assertFalse(workspace.isDirty());
        session.typeText("edit"); assertTrue(workspace.isDirty());
        session.undo(); assertFalse(workspace.isDirty());
        session.redo(); assertTrue(workspace.isDirty());
        var depth = session.undoDepth();
        success(workspace.save()); assertFalse(workspace.isDirty()); assertEquals(depth, session.undoDepth());
        session.undo(); assertTrue(workspace.isDirty());
        session.redo(); assertFalse(workspace.isDirty());
        success(workspace.saveAs("renamed")); assertEquals(depth, session.undoDepth());
    }

    @Test void failedSaveKeepsDirtySnapshotPathAndHistory() {
        var workspace = workspace();
        success(workspace.saveAs("good"));
        workspace.session().typeText("edited");
        var state = workspace.session().current();
        assertInstanceOf(PersistenceResult.Failure.class, workspace.saveAs("../bad"));
        assertTrue(workspace.isDirty()); assertEquals(Optional.of("good"), workspace.name());
        assertSame(state, workspace.session().current()); assertEquals(1, workspace.session().undoDepth());
    }

    @Test void ioSaveFailureLeavesWorkspaceDirtyAndPreviousSaveIntact() {
        var working = new FileDocumentStorage(directory);
        success(working.save("doc", initial));
        var failing = new FileDocumentStorage(directory) {
            @Override protected void writeTemporary(Path path, byte[] bytes) throws IOException { throw new IOException("injected"); }
        };
        var workspace = new DocumentWorkspace(initial, failing);
        success(workspace.open("doc"));
        workspace.session().typeText("edited"); var state = workspace.session().current();
        assertInstanceOf(PersistenceResult.Failure.class, workspace.save());
        assertTrue(workspace.isDirty()); assertSame(state, workspace.session().current());
        assertEquals(initial, success(working.load("doc")));
    }

    @Test void loadedNestedEditorsEnterWithoutHistoryAndRemainEditable() {
        var workspace = new DocumentWorkspace(DevelopmentDocument.createPersistenceFixture(), new FileDocumentStorage(directory));
        success(workspace.saveAs("mixed")); success(workspace.open("mixed"));
        var session = workspace.session(); var document = session.current().document();
        for (var family : List.of(EquationBlock.class, TableBlock.class, PlotBlock.class, DiagramBlock.class)) {
            var index = java.util.stream.IntStream.range(0, document.blocks().size()).filter(i -> family.isInstance(document.blocks().get(i))).findFirst().orElseThrow();
            session.setCurrent(new EditorState(document, new BlockSelection(index), Optional.empty()));
            session.enter();
            assertTrue(new EditorSelectionValidator().isValid(session.current()));
            assertEquals(0, session.undoDepth());
        }
        var authoredTable = java.util.stream.IntStream.range(0, document.blocks().size()).filter(i -> document.blocks().get(i) instanceof TableBlock t && t.datasetBinding().isEmpty()).findFirst().orElseThrow();
        session.setCurrent(new EditorState(document, new BlockSelection(authoredTable), Optional.empty()));
        session.enter(); var before = session.current();
        assertTrue(session.typeText("loaded edit")); var after = session.current();
        assertTrue(DocumentValidator.validate(after.document()).isValid()); assertEquals(1, session.undoDepth());
        session.undo(); assertEquals(before, session.current()); session.redo(); assertEquals(after, session.current());
    }

    @Test void validOpenStartsFreshEditableSessionHistoryAndSelection() {
        var workspace = workspace();
        success(workspace.saveAs("doc"));
        var old = workspace.session(); old.typeText("unsaved");
        success(workspace.open("doc"));
        assertNotSame(old, workspace.session()); assertEquals(initial, workspace.session().current().document());
        assertEquals(0, workspace.session().undoDepth()); assertEquals(0, workspace.session().redoDepth());
        assertTrue(new EditorSelectionValidator().isValid(workspace.session().current()));
        assertFalse(workspace.isDirty());
        workspace.session().typeText("after load"); assertTrue(workspace.isDirty());
        assertTrue(workspace.session().undo()); assertFalse(workspace.isDirty());
        assertTrue(workspace.session().redo()); assertTrue(workspace.isDirty());
    }

    @Test void invalidOpenLeavesCurrentSessionDocumentSelectionHistoryNameAndBaselineUntouched() throws IOException {
        var workspace = workspace(); success(workspace.saveAs("doc"));
        workspace.session().typeText("unsaved");
        var old = workspace.session(); var state = old.current();
        Files.writeString(directory.resolve("bad.scholar.json"), "{\"format\":\"scholar-document\",\"version\":999}");
        assertInstanceOf(PersistenceResult.Failure.class, workspace.open("bad"));
        assertSame(old, workspace.session()); assertSame(state, old.current());
        assertEquals(1, old.undoDepth()); assertEquals(Optional.of("doc"), workspace.name()); assertTrue(workspace.isDirty());
    }

    @Test void newDocumentHasMinimalParagraphNoResourcesFreshHistoryAndUnsavedName() {
        var workspace = new DocumentWorkspace(DevelopmentDocument.createPersistenceFixture(), new FileDocumentStorage(directory));
        var old = workspace.session(); old.typeText("edited");
        workspace.newDocument();
        assertNotSame(old, workspace.session());
        assertEquals(new Document(List.of(new Paragraph(new InlineContent(List.of())))), workspace.session().current().document());
        assertEquals(new DocumentPosition(0, 0), workspace.session().current().caret());
        assertEquals(0, workspace.session().undoDepth()); assertEquals(Optional.empty(), workspace.name());
        assertTrue(workspace.isDirty());
    }

    @Test void loadedAtomicOnlyDocumentUsesExistingBlockSelectionWithoutInventedParagraph() {
        var storage = new FileDocumentStorage(directory);
        var doc = new Document(List.of(new EquationBlock(new dev.rgcb.scholar.math.MathSequence(List.of()))));
        success(storage.save("atomic", doc));
        var workspace = new DocumentWorkspace(initial, storage);
        success(workspace.open("atomic"));
        assertEquals(doc, workspace.session().current().document());
        assertEquals(new BlockSelection(0), workspace.session().current().selection());
        assertTrue(new EditorSelectionValidator().isValid(workspace.session().current()));
    }

    @Test void reopenedSameIdsAreNotM25SameDocumentProofAndFigurePasteStillWorks() {
        var fixture = DevelopmentDocument.createPersistenceFixture();
        var workspace = new DocumentWorkspace(fixture, new FileDocumentStorage(directory));
        success(workspace.saveAs("a"));
        var source = workspace.session();
        var figureIndex = java.util.stream.IntStream.range(0, fixture.blocks().size()).filter(i -> fixture.blocks().get(i) instanceof FigureBlock f && f.id().equals("m25-transfer-figure")).findFirst().orElseThrow();
        source.setCurrent(new EditorState(fixture, new BlockSelection(figureIndex), Optional.empty()));
        var copy = source.copyForClipboard().orElseThrow();
        var payload = (DocumentFragmentClipboardPayload) copy.payload().orElseThrow();
        success(workspace.open("a"));
        var loaded = workspace.session();
        loaded.setCurrent(new EditorState(loaded.current().document(), new BlockSelection(figureIndex), Optional.empty()));
        var loadedCopy = (DocumentFragmentClipboardPayload) loaded.copyForClipboard().orElseThrow().payload().orElseThrow();
        assertNotEquals(payload.sourceMetadata().documentToken(), loadedCopy.sourceMetadata().documentToken());
        // Paste at prose rather than replace the original figure.
        loaded.setCurrent(new EditorState(loaded.current().document(), new DocumentPosition(1, 0)));
        var before = loaded.current();
        assertTrue(loaded.pasteFromClipboard(Optional.of(payload), copy.plainText()));
        var after = loaded.current();
        var pasted = (FigureBlock) after.document().blocks().get(1);
        assertNotEquals("m25-transfer-figure", pasted.id());
        assertEquals(new CrossReference(CrossReferenceTargetKind.FIGURE, pasted.id()), pasted.caption().nodes().get(1));
        assertInstanceOf(Text.class, pasted.caption().nodes().get(3));
        var plot = (PlotBlock) pasted.content();
        assertNotEquals("projectile-test", plot.definition().series().get(0).datasetBinding().orElseThrow().datasetId());
        assertTrue(DocumentValidator.validate(after.document()).isValid()); assertEquals(1, loaded.undoDepth());
        loaded.undo(); assertEquals(before, loaded.current());
        loaded.redo(); assertEquals(after, loaded.current());
    }
}
