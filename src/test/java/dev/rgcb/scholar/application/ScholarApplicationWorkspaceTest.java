package dev.rgcb.scholar.application;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.clipboard.DocumentFragmentClipboardPayload;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.DocumentTemplateId;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.SemanticStyle;
import dev.rgcb.scholar.document.LayoutSectionBreak;
import dev.rgcb.scholar.document.ColumnLayout;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.validation.DocumentValidator;
import dev.rgcb.scholar.editor.BlockSelection;
import dev.rgcb.scholar.editor.DocumentPosition;
import dev.rgcb.scholar.editor.EditorState;
import dev.rgcb.scholar.persistence.PersistenceResult;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScholarApplicationWorkspaceTest {
    @TempDir Path directory;

    @Test void readabilitySampleIsOptInPersistentAndNotRecreatedAfterDelete() {
        var application = application();
        assertTrue(success(application.documents()).isEmpty());
        var sample = success(application.createReadabilitySample());
        assertEquals("M34", sample.displayName());
        var document = sample.session().current().document();
        assertTrue(DocumentValidator.validate(document).isValid());
        assertTrue(document.blocks().stream().anyMatch(FigureBlock.class::isInstance));
        assertTrue(document.blocks().stream().anyMatch(TableBlock.class::isInstance));
        assertTrue(document.blocks().stream().anyMatch(DatasetAnalysisBlock.class::isInstance));
        assertTrue(document.blocks().stream().filter(EquationBlock.class::isInstance).count() >= 5);
        assertTrue(document.blocks().stream().anyMatch(block -> block.equals(new LayoutSectionBreak(ColumnLayout.two()))));
        var firstOpen = success(application.openDocument(sample.id()));
        assertEquals(document, firstOpen.session().current().document());
        application.closeWorkspace(sample);
        assertInstanceOf(PersistenceResult.Failure.class, application.deleteDocument(sample.id()));
        firstOpen.session().typeText("edited");
        assertTrue(firstOpen.isDirty());
        var historyDepth = firstOpen.session().undoDepth();
        assertInstanceOf(PersistenceResult.Failure.class, application.deleteDocument(sample.id()));
        application.closeWorkspace(firstOpen);
        assertTrue(success(application.deleteDocument(sample.id())));
        assertEquals(historyDepth, firstOpen.session().undoDepth());
        assertTrue(success(application.documents()).isEmpty());
        assertTrue(success(application.documents()).isEmpty());
        assertEquals("M34", success(application.createReadabilitySample()).displayName());
    }

    @Test void neutralDocumentsUseCollisionSafeNamesAndValidFreshSessions() {
        var application = application();
        var first = success(application.createDocument());
        var second = success(application.createDocument());

        assertEquals("Untitled", first.displayName());
        assertEquals("Untitled 2", second.displayName());
        assertNotEquals(first.id(), second.id());
        assertEquals(ScholarDocuments.blank(), first.session().current().document());
        assertEquals(0, first.session().undoDepth());
        assertFalse(first.isDirty());
    }

    @Test void templateCreationPersistsTheSelectedScientificDocumentPolicy() {
        var application = application();
        var created = success(application.createDocument(DocumentTemplateId.IEEE_STYLE));
        var document = created.session().current().document();
        assertEquals(DocumentTemplateId.IEEE_STYLE, document.settings().template());
        assertEquals(1, document.settings().columns().count());
        assertTrue(document.blocks().stream().anyMatch(block -> block.equals(
                new LayoutSectionBreak(ColumnLayout.two()))));
        assertTrue(document.blocks().stream().anyMatch(block -> block instanceof Paragraph paragraph
                && paragraph.style() == SemanticStyle.ABSTRACT));

        success(created.save());
        var reopened = success(application.openDocument(created.id())).session().current().document();
        assertEquals(document, reopened);
    }

    @Test void openAlwaysCreatesFreshSessionHistorySelectionAndRuntimeTransferIdentity() {
        var repository = repository();
        var document = new Document(List.of(new Heading("shared", 1, inline("Shared")), new Paragraph(inline("Body"))));
        var created = success(repository.createDocument("One", document));
        var other = success(repository.createDocument("Two", document));
        var application = new ScholarApplication(repository);
        var firstOpen = success(application.openDocument(created.descriptor().id()));
        firstOpen.session().typeText("edited");
        var reopened = success(application.openDocument(created.descriptor().id()));
        var secondDocument = success(application.openDocument(other.descriptor().id()));

        assertNotSame(firstOpen.session(), reopened.session());
        assertEquals(0, reopened.session().undoDepth());
        assertEquals(document, reopened.session().current().document());
        var firstPayload = payloadForWholeBlock(reopened, 0);
        var secondPayload = payloadForWholeBlock(secondDocument, 0);
        assertNotEquals(firstPayload.sourceMetadata().documentToken(), secondPayload.sourceMetadata().documentToken());
    }

    @Test void saveRenameAndSaveAsDoNotCreateEditorHistoryOrChangeSemanticIds() {
        var workspace = success(application().createDocument());
        workspace.session().typeText("alpha");
        var depth = workspace.session().undoDepth();
        assertTrue(workspace.isDirty());
        success(workspace.save());
        assertFalse(workspace.isDirty());
        assertEquals(depth, workspace.session().undoDepth());
        var originalId = workspace.id();
        success(workspace.rename("Lab Notes"));
        assertEquals(originalId, workspace.id());
        assertEquals(depth, workspace.session().undoDepth());
        success(workspace.saveAs("Lab Notes Copy"));
        assertNotEquals(originalId, workspace.id());
        assertEquals(depth, workspace.session().undoDepth());
        assertFalse(workspace.isDirty());
    }

    @Test void saveFailureKeepsDirtyBaselineAndHistory() {
        var delegate = repository();
        var opened = success(delegate.createDocument("Failure", ScholarDocuments.blank()));
        var workspace = new ApplicationDocumentWorkspace(new FailingSaveRepository(delegate), opened);
        workspace.session().typeText("dirty");
        var state = workspace.session().current();
        var result = workspace.save();
        assertInstanceOf(PersistenceResult.Failure.class, result);
        assertTrue(workspace.isDirty());
        assertSame(state, workspace.session().current());
        assertEquals(1, workspace.session().undoDepth());
    }

    @Test void failedOpenDoesNotReplaceCurrentWorkspaceOrItsRuntimeState() throws Exception {
        var application = application();
        var current = success(application.createDocument());
        current.session().typeText("kept");
        var state = current.session().current();
        Files.writeString(directory.resolve("documents").resolve("broken.scholar.json"), "{broken");

        assertInstanceOf(PersistenceResult.Failure.class, application.openDocument(new ScholarDocumentId("broken")));
        assertSame(state, current.session().current());
        assertTrue(current.isDirty());
        assertEquals(1, current.session().undoDepth());
    }

    @Test void integratedCreateEditSaveReopenRenameSaveAsAndCrossDocumentPaste() {
        var application = application();
        var documentA = success(application.createDocument());
        assertTrue(documentA.session().typeText("Document A"));
        success(documentA.save());
        var reopenedA = success(application.openDocument(documentA.id()));
        assertEquals(0, reopenedA.session().undoDepth());
        success(reopenedA.rename("Experiment A"));
        var originalApplicationId = reopenedA.id();
        success(reopenedA.saveAs("Experiment A Copy"));
        assertNotEquals(originalApplicationId, reopenedA.id());

        var sourceState = reopenedA.session().current();
        reopenedA.session().setCurrent(new EditorState(sourceState.document(), new DocumentPosition(0, 0), new DocumentPosition(0, 10)));
        var copy = reopenedA.session().copyForClipboard().orElseThrow();
        var sourcePayload = (DocumentFragmentClipboardPayload) copy.payload().orElseThrow();

        var documentB = success(application.createDocument());
        var bState = documentB.session().current();
        documentB.session().setCurrent(new EditorState(bState.document(), new DocumentPosition(0, 0), new DocumentPosition(0, 0)));
        assertTrue(documentB.session().pasteFromClipboard(Optional.of(sourcePayload), copy.plainText()));
        assertEquals(1, documentB.session().undoDepth());
        assertNotEquals(sourcePayload.sourceMetadata().documentToken(),
                payloadForTextRange(documentB, 0, 10).sourceMetadata().documentToken());
        success(documentB.save());
        assertFalse(documentB.isDirty());
        assertEquals(3, success(application.documents()).size());
    }

    private ScholarApplication application() { return new ScholarApplication(repository()); }
    private FileScholarDocumentRepository repository() {
        var counter = new AtomicInteger();
        return new FileScholarDocumentRepository(directory, Clock.fixed(Instant.parse("2026-09-19T12:00:00Z"), ZoneOffset.UTC),
                () -> new ScholarDocumentId("app-" + counter.incrementAndGet()));
    }
    private static InlineContent inline(String text) { return new InlineContent(List.of(new Text(text, Set.of()))); }
    private static DocumentFragmentClipboardPayload payloadForWholeBlock(ApplicationDocumentWorkspace workspace, int index) {
        var state = workspace.session().current();
        workspace.session().setCurrent(new EditorState(state.document(), new BlockSelection(index), Optional.empty()));
        return (DocumentFragmentClipboardPayload) workspace.session().copyForClipboard().orElseThrow().payload().orElseThrow();
    }
    private static DocumentFragmentClipboardPayload payloadForTextRange(ApplicationDocumentWorkspace workspace, int start, int end) {
        var state = workspace.session().current();
        workspace.session().setCurrent(new EditorState(state.document(), new DocumentPosition(0, start), new DocumentPosition(0, end)));
        return (DocumentFragmentClipboardPayload) workspace.session().copyForClipboard().orElseThrow().payload().orElseThrow();
    }
    @SuppressWarnings("unchecked") private static <T> T success(PersistenceResult<T> result) { return ((PersistenceResult.Success<T>) result).value(); }

    private record FailingSaveRepository(ScholarDocumentRepository delegate) implements ScholarDocumentRepository {
        public PersistenceResult<List<ScholarDocumentDescriptor>> listDocuments() { return delegate.listDocuments(); }
        public PersistenceResult<OpenedScholarDocument> createDocument(String name, Document document) { return delegate.createDocument(name, document); }
        public PersistenceResult<OpenedScholarDocument> openDocument(ScholarDocumentId id) { return delegate.openDocument(id); }
        public PersistenceResult<ScholarDocumentDescriptor> saveDocument(ScholarDocumentId id, Document document) {
            return PersistenceResult.failure(dev.rgcb.scholar.persistence.PersistenceDiagnostic.Code.IO_FAILURE, "$", "injected");
        }
        public PersistenceResult<OpenedScholarDocument> saveAs(String name, Document document) { return delegate.saveAs(name, document); }
        public PersistenceResult<ScholarDocumentDescriptor> renameDocument(ScholarDocumentId id, String name) { return delegate.renameDocument(id, name); }
        public PersistenceResult<Boolean> deleteDocument(ScholarDocumentId id) { return delegate.deleteDocument(id); }
    }
}
