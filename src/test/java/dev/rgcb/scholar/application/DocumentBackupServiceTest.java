package dev.rgcb.scholar.application;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.persistence.PersistenceDiagnostic;
import dev.rgcb.scholar.persistence.PersistenceResult;
import dev.rgcb.scholar.persistence.FileDocumentStorage;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentBackupServiceTest {
    @TempDir Path root;

    @Test void createAndUnchangedSaveDoNotInventPriorVersions() {
        var backups = new FileDocumentBackupService(root.resolve("backups"));
        var repository = repository(backups);
        var created = success(repository.createDocument("Draft", document("A")));
        success(repository.saveDocument(created.descriptor().id(), document("A")));
        assertTrue(success(backups.list(created.descriptor().id())).isEmpty());
    }

    @Test void changedSavePreservesPreviousVersionAndCommitsNewCanonical() {
        var backups = new FileDocumentBackupService(root.resolve("backups"));
        var repository = repository(backups);
        var created = success(repository.createDocument("Draft", document("A")));
        success(repository.saveDocument(created.descriptor().id(), document("B")));

        var backup = success(backups.list(created.descriptor().id())).getFirst();
        assertEquals(document("A"), success(backups.load(backup)));
        assertEquals(document("B"), success(repository.openDocument(created.descriptor().id())).document());
    }

    @Test void retentionIsBoundedPerDocumentAndRapidSavesDoNotCollide() {
        var backups = new FileDocumentBackupService(root.resolve("backups"));
        var repository = repository(backups);
        var first = success(repository.createDocument("First", document("0")));
        var second = success(repository.createDocument("Second", document("other")));
        for (var index = 1; index <= 8; index++) success(repository.saveDocument(first.descriptor().id(), document("" + index)));
        success(repository.saveDocument(second.descriptor().id(), document("changed")));

        var firstBackups = success(backups.list(first.descriptor().id()));
        assertEquals(FileDocumentBackupService.DEFAULT_RETENTION, firstBackups.size());
        assertEquals(firstBackups.size(), firstBackups.stream().map(BackupDescriptor::storageName).distinct().count());
        assertEquals(document("7"), success(backups.load(firstBackups.getFirst())));
        assertEquals(1, success(backups.list(second.descriptor().id())).size());
    }

    @Test void backupFailureLeavesCanonicalUntouched() {
        var failing = new DocumentBackupService() {
            @Override public PersistenceResult<BackupDescriptor> preserve(ScholarDocumentId id, long time, Document document) {
                return PersistenceResult.failure(PersistenceDiagnostic.Code.IO_FAILURE, "backup", "injected");
            }
            @Override public PersistenceResult<Boolean> complete(BackupDescriptor backup) { return successResult(true); }
            @Override public PersistenceResult<Boolean> rollback(BackupDescriptor backup) { return successResult(true); }
            @Override public PersistenceResult<List<BackupDescriptor>> list(ScholarDocumentId id) { return successResult(List.of()); }
            @Override public PersistenceResult<Document> load(BackupDescriptor backup) { return successResult(document("unused")); }
        };
        var repository = repository(failing);
        var created = success(repository.createDocument("Draft", document("A")));
        assertInstanceOf(PersistenceResult.Failure.class,
                repository.saveDocument(created.descriptor().id(), document("B")));
        assertEquals(document("A"), success(repository.openDocument(created.descriptor().id())).document());
    }

    @Test void canonicalFailureRollsBackPreparedBackup() throws Exception {
        var backups = new FileDocumentBackupService(root.resolve("backups"));
        var storage = new FailingStorage(root.resolve("documents"));
        var ids = new AtomicInteger();
        var repository = new FileScholarDocumentRepository(root, Clock.fixed(Instant.ofEpochMilli(100), ZoneOffset.UTC),
                () -> new ScholarDocumentId("doc-" + ids.incrementAndGet()), storage, backups);
        var created = success(repository.createDocument("Draft", document("A")));
        storage.fail = true;

        assertInstanceOf(PersistenceResult.Failure.class,
                repository.saveDocument(created.descriptor().id(), document("B")));
        assertTrue(success(backups.list(created.descriptor().id())).isEmpty());
        storage.fail = false;
        assertEquals(document("A"), success(repository.openDocument(created.descriptor().id())).document());
    }

    @Test void pruneFailureIsWarningAndDoesNotUndoCanonicalSave() {
        var storage = new FailingPruneStorage(root.resolve("backups"));
        var backups = new FileDocumentBackupService(storage, 2);
        var repository = repository(backups);
        var created = success(repository.createDocument("Draft", document("0")));
        success(repository.saveDocument(created.descriptor().id(), document("1")));
        success(repository.saveDocument(created.descriptor().id(), document("2")));
        storage.failDeletes = true;

        var saved = repository.saveDocument(created.descriptor().id(), document("3"));

        assertInstanceOf(PersistenceResult.Success.class, saved);
        assertFalse(saved.diagnostics().isEmpty());
        assertEquals(document("3"), success(repository.openDocument(created.descriptor().id())).document());
    }

    @Test void maximumLengthCanonicalIdentityStillHasSafeBackupPath() {
        var id = new ScholarDocumentId("d".repeat(64));
        var backups = new FileDocumentBackupService(root.resolve("backups"));
        var backup = success(backups.preserve(id, 123, document("previous")));

        assertEquals(document("previous"), success(backups.load(backup)));
        assertTrue(root.resolve("backups").resolve(id.value()).normalize()
                .startsWith(root.resolve("backups").normalize()));
    }

    private FileScholarDocumentRepository repository(DocumentBackupService backups) {
        var ids = new AtomicInteger();
        return new FileScholarDocumentRepository(root, Clock.fixed(Instant.ofEpochMilli(100), ZoneOffset.UTC),
                () -> new ScholarDocumentId("doc-" + ids.incrementAndGet()), backups);
    }

    private static Document document(String text) {
        return new Document(List.of(new Paragraph(new InlineContent(List.of(new Text(text, Set.of()))))));
    }

    private static <T> PersistenceResult<T> successResult(T value) {
        return new PersistenceResult.Success<>(value, List.of());
    }

    private static final class FailingStorage extends FileDocumentStorage {
        private boolean fail;
        private FailingStorage(Path directory) { super(directory); }
        @Override public PersistenceResult<String> save(String name, Document document) {
            return fail ? PersistenceResult.failure(PersistenceDiagnostic.Code.IO_FAILURE, "document", "injected")
                    : super.save(name, document);
        }
    }

    private static final class FailingPruneStorage extends FileDocumentStorage {
        private boolean failDeletes;
        private FailingPruneStorage(Path directory) { super(directory); }
        @Override public PersistenceResult<Boolean> delete(String name) {
            return failDeletes ? PersistenceResult.failure(PersistenceDiagnostic.Code.IO_FAILURE, "backup", "injected")
                    : super.delete(name);
        }
    }

    @SuppressWarnings("unchecked") private static <T> T success(PersistenceResult<T> result) {
        assertInstanceOf(PersistenceResult.Success.class, result, () -> result.diagnostics().toString());
        return ((PersistenceResult.Success<T>) result).value();
    }
}
