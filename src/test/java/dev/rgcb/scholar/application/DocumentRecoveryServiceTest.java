package dev.rgcb.scholar.application;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.persistence.PersistenceDiagnostic;
import dev.rgcb.scholar.persistence.PersistenceResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentRecoveryServiceTest {
    @TempDir Path root;

    @Test void dirtyDocumentIsCapturedWithoutChangingDirtyOrHistory() {
        var fixture = fixture();
        var workspace = success(fixture.application.createDocument());
        workspace.session().typeText("draft");
        var history = workspace.session().undoDepth();

        fixture.clock.millis = 1_000;
        fixture.application.recoveryTick();

        assertTrue(workspace.isDirty());
        assertEquals(history, workspace.session().undoDepth());
        assertEquals(1, success(fixture.application.recoveryCandidates()).size());
    }

    @Test void cleanAndUnchangedDirtyDocumentsDoNotCauseUselessWrites() {
        var fixture = fixture();
        var workspace = success(fixture.application.createDocument());
        fixture.clock.millis = 1_000;
        fixture.application.recoveryTick();
        assertTrue(success(fixture.application.recoveryCandidates()).isEmpty());

        workspace.session().typeText("one");
        fixture.clock.millis = 2_000;
        fixture.application.recoveryTick();
        var first = success(fixture.application.recoveryCandidates()).getFirst();
        fixture.clock.millis = 3_000;
        fixture.application.recoveryTick();
        assertEquals(first.capturedAtEpochMillis(), success(fixture.application.recoveryCandidates()).getFirst().capturedAtEpochMillis());

        workspace.session().typeText("two");
        fixture.clock.millis = 4_000;
        fixture.application.recoveryTick();
        assertEquals(4_000, success(fixture.application.recoveryCandidates()).getFirst().capturedAtEpochMillis());
    }

    @Test void successfulSaveInvalidatesRecoveryButFailedSavePreservesIt() {
        var fixture = fixture();
        var workspace = success(fixture.application.createDocument());
        workspace.session().typeText("safe");
        fixture.application.captureRecoveryNow();
        assertEquals(1, success(fixture.application.recoveryCandidates()).size());
        success(workspace.save());
        assertFalse(workspace.isDirty());
        assertTrue(success(fixture.application.recoveryCandidates()).isEmpty());

        var failedFixture = fixture(new FailingSaveRepository(repository(root.resolve("failed"))));
        var failed = success(failedFixture.application.createDocument());
        failed.session().typeText("protected");
        failedFixture.application.captureRecoveryNow();
        assertInstanceOf(PersistenceResult.Failure.class, failed.save());
        assertTrue(failed.isDirty());
        assertEquals(1, success(failedFixture.application.recoveryCandidates()).size());
    }

    @Test void recoveredDocumentIsDirtyEditableAndStartsWithFreshHistory() {
        var fixture = fixture();
        var workspace = success(fixture.application.createDocument());
        workspace.session().typeText("recover me");
        var recoveredDocument = workspace.session().current().document();
        fixture.application.captureRecoveryNow();
        var candidate = success(fixture.application.recoveryCandidates()).getFirst();

        var recovered = success(fixture.application.recover(candidate.recoveryId()));
        assertEquals(recoveredDocument, recovered.session().current().document());
        assertTrue(recovered.isDirty());
        assertEquals(0, recovered.session().undoDepth());
        assertTrue(recovered.session().typeText(" more"));
        assertEquals(1, recovered.session().undoDepth());
    }

    @Test void supportsUntitledOrphanAndMultipleCandidatesAndScopedDiscard() {
        var service = new FileDocumentRecoveryService(root.resolve("recovery"));
        var first = snapshot("recovery-one", Optional.empty(), true, "Untitled", 100, document("one"));
        var missingId = new ScholarDocumentId("missing-doc");
        var second = snapshot("recovery-two", Optional.of(missingId), false, "Missing", 200, document("two"));
        success(service.capture(first));
        success(service.capture(second));

        var application = new ScholarApplication(repository(root), service, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                new RecoveryPolicy(Duration.ofSeconds(1)));
        var candidates = success(application.recoveryCandidates());
        assertEquals(2, candidates.size());
        assertEquals(RecoveryCandidate.SourceState.MISSING_DOCUMENT, candidates.getFirst().sourceState());
        assertEquals(RecoveryCandidate.SourceState.UNTITLED, candidates.get(1).sourceState());
        success(application.discardRecovery(candidates.getFirst().recoveryId()));
        assertEquals(List.of(new RecoveryId("recovery-one")), success(application.recoveryCandidates()).stream()
                .map(RecoveryCandidate::recoveryId).toList());
        assertTrue(Files.notExists(root.resolve("documents").resolve("missing-doc.scholar.json")));

        var recovered = success(application.recover(new RecoveryId("recovery-one")));
        assertTrue(recovered.isDirty());
        success(recovered.save());
        assertFalse(recovered.isDirty());
        assertEquals(document("one"), success(application.openDocument(recovered.id())).session().current().document());
        assertTrue(success(application.recoveryCandidates()).isEmpty());
    }

    @Test void corruptAndFutureRecoveryAreIsolatedAndKept() throws Exception {
        var directory = root.resolve("recovery");
        var service = new FileDocumentRecoveryService(directory);
        success(service.capture(snapshot("recovery-good", Optional.empty(), true, "Good", 10, document("good"))));
        Files.writeString(directory.resolve("corrupt.recovery.json"), "{broken");
        Files.writeString(directory.resolve("future.recovery.json"), """
                {"format":"scholar-recovery","version":99}
                """);

        var discovered = service.discover();
        assertEquals(1, success(discovered).size());
        assertEquals(2, discovered.diagnostics().size());
        assertTrue(discovered.diagnostics().stream().anyMatch(value -> value.code() == PersistenceDiagnostic.Code.UNSUPPORTED_VERSION));
        assertTrue(Files.exists(directory.resolve("corrupt.recovery.json")));
        assertTrue(Files.exists(directory.resolve("future.recovery.json")));
    }

    @Test void interruptedAtomicWriteKeepsLastKnownGoodRecovery() {
        var directory = root.resolve("recovery");
        var normal = new FileDocumentRecoveryService(directory);
        var original = snapshot("recovery-atomic", Optional.empty(), true, "Draft", 10, document("old"));
        success(normal.capture(original));
        var failing = new FileDocumentRecoveryService(directory) {
            @Override protected void replace(Path temporary, Path target) throws IOException {
                throw new IOException("injected");
            }
        };
        var update = snapshot("recovery-atomic", Optional.empty(), true, "Draft", 20, document("new"));
        assertInstanceOf(PersistenceResult.Failure.class, failing.capture(update));
        assertEquals(original.document(), success(normal.load(original.recoveryId())).document());
    }

    @Test void recoveryWriteFailureLeavesDocumentDirtyAndHistoryUntouched() {
        var clock = new MutableClock(0);
        var failing = new FileDocumentRecoveryService(root.resolve("recovery-failure")) {
            @Override protected void writeTemporary(Path path, byte[] bytes) throws IOException {
                throw new IOException("injected");
            }
        };
        var application = new ScholarApplication(repository(root.resolve("failure-app")), failing, clock,
                new RecoveryPolicy(Duration.ofSeconds(1)));
        var workspace = success(application.createDocument());
        workspace.session().typeText("still here");
        var history = workspace.session().undoDepth();

        application.captureRecoveryNow();

        assertTrue(workspace.isDirty());
        assertEquals(history, workspace.session().undoDepth());
        assertFalse(application.recoveryDiagnostics().isEmpty());
        assertTrue(success(application.recoveryCandidates()).isEmpty());
    }

    @Test void dirtyCloseRequestsImmediateCaptureWithoutWaitingForInterval() {
        var fixture = fixture();
        var workspace = success(fixture.application.createDocument());
        workspace.session().typeText("close safely");

        fixture.application.closeWorkspace(workspace);

        assertEquals(1, success(fixture.application.recoveryCandidates()).size());
    }

    private Fixture fixture() { return fixture(repository(root)); }

    private Fixture fixture(ScholarDocumentRepository repository) {
        var clock = new MutableClock(0);
        var recovery = new FileDocumentRecoveryService(root.resolve("recovery-" + Math.abs(repository.hashCode())));
        return new Fixture(new ScholarApplication(repository, recovery, clock,
                new RecoveryPolicy(Duration.ofSeconds(1))), clock);
    }

    private static FileScholarDocumentRepository repository(Path path) {
        var ids = new AtomicInteger();
        return new FileScholarDocumentRepository(path, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                () -> new ScholarDocumentId("doc-" + ids.incrementAndGet()));
    }

    private static RecoverySnapshot snapshot(String id, Optional<ScholarDocumentId> documentId, boolean untitled,
                                             String name, long captured, Document document) {
        return new RecoverySnapshot(new RecoveryId(id), documentId, untitled, name, captured, Optional.empty(), 1, document);
    }

    private static Document document(String text) {
        return new Document(List.of(new Paragraph(new InlineContent(List.of(new Text(text, Set.of()))))));
    }

    @SuppressWarnings("unchecked") private static <T> T success(PersistenceResult<T> result) {
        assertInstanceOf(PersistenceResult.Success.class, result, () -> result.diagnostics().toString());
        return ((PersistenceResult.Success<T>) result).value();
    }

    private record Fixture(ScholarApplication application, MutableClock clock) { }

    private static final class MutableClock extends Clock {
        private long millis;
        private MutableClock(long millis) { this.millis = millis; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
        @Override public long millis() { return millis; }
    }

    private record FailingSaveRepository(ScholarDocumentRepository delegate) implements ScholarDocumentRepository {
        @Override public PersistenceResult<List<ScholarDocumentDescriptor>> listDocuments() { return delegate.listDocuments(); }
        @Override public PersistenceResult<OpenedScholarDocument> createDocument(String name, Document document) { return delegate.createDocument(name, document); }
        @Override public PersistenceResult<OpenedScholarDocument> openDocument(ScholarDocumentId id) { return delegate.openDocument(id); }
        @Override public PersistenceResult<ScholarDocumentDescriptor> saveDocument(ScholarDocumentId id, Document document) {
            return PersistenceResult.failure(PersistenceDiagnostic.Code.IO_FAILURE, "document", "injected");
        }
        @Override public PersistenceResult<OpenedScholarDocument> saveAs(String name, Document document) { return delegate.saveAs(name, document); }
        @Override public PersistenceResult<ScholarDocumentDescriptor> renameDocument(ScholarDocumentId id, String name) { return delegate.renameDocument(id, name); }
        @Override public PersistenceResult<Boolean> deleteDocument(ScholarDocumentId id) { return delegate.deleteDocument(id); }
    }
}
