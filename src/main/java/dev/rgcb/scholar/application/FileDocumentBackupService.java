package dev.rgcb.scholar.application;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.persistence.FileDocumentStorage;
import dev.rgcb.scholar.persistence.PersistenceDiagnostic;
import dev.rgcb.scholar.persistence.PersistenceResult;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** Bounded storage for previous validated canonical versions. */
public class FileDocumentBackupService implements DocumentBackupService {
    public static final int DEFAULT_RETENTION = 5;
    private final Function<ScholarDocumentId, FileDocumentStorage> storage;
    private final int retention;

    public FileDocumentBackupService(Path directory) {
        this(id -> new FileDocumentStorage(directory.resolve(id.value())), DEFAULT_RETENTION);
    }

    FileDocumentBackupService(FileDocumentStorage storage, int retention) {
        this(ignored -> storage, retention);
    }

    FileDocumentBackupService(Function<ScholarDocumentId, FileDocumentStorage> storage, int retention) {
        if (retention < 1) throw new IllegalArgumentException("Backup retention must be positive.");
        this.storage = storage;
        this.retention = retention;
    }

    @Override public PersistenceResult<BackupDescriptor> preserve(
            ScholarDocumentId id, long savedAtEpochMillis, Document document
    ) {
        var documentStorage = storage.apply(id);
        var listed = documentStorage.list();
        if (listed instanceof PersistenceResult.Failure<List<String>> failure) return new PersistenceResult.Failure<>(failure.diagnostics());
        var existing = ((PersistenceResult.Success<List<String>>) listed).value();
        var timestampPrefix = String.format(java.util.Locale.ROOT, "%013d-", savedAtEpochMillis);
        var firstSequence = existing.stream().filter(name -> name.startsWith(timestampPrefix))
                .mapToInt(FileDocumentBackupService::sequence).max().orElse(-1) + 1;
        for (var sequence = firstSequence; sequence < 1000; sequence++) {
            var name = backupName(savedAtEpochMillis, sequence);
            if (existing.contains(name)) continue;
            var saved = documentStorage.save(name, document);
            if (saved instanceof PersistenceResult.Failure<String> failure) return new PersistenceResult.Failure<>(failure.diagnostics());
            return new PersistenceResult.Success<>(new BackupDescriptor(id, name, savedAtEpochMillis), saved.diagnostics());
        }
        return PersistenceResult.failure(PersistenceDiagnostic.Code.IO_FAILURE, "backup", "A unique backup identity could not be allocated.");
    }

    @Override public PersistenceResult<Boolean> complete(BackupDescriptor backup) {
        var listed = list(backup.documentId());
        if (listed instanceof PersistenceResult.Failure<List<BackupDescriptor>> failure) return new PersistenceResult.Failure<>(failure.diagnostics());
        var warnings = new ArrayList<PersistenceDiagnostic>();
        var backups = ((PersistenceResult.Success<List<BackupDescriptor>>) listed).value();
        for (var index = retention; index < backups.size(); index++) {
            var deleted = storage.apply(backup.documentId()).delete(backups.get(index).storageName());
            if (deleted instanceof PersistenceResult.Failure<Boolean> failure) {
                var diagnostic = failure.diagnostics().getFirst();
                warnings.add(new PersistenceDiagnostic(PersistenceDiagnostic.Severity.WARNING, diagnostic.code(),
                        "backup", "Old backup could not be pruned."));
            }
        }
        return new PersistenceResult.Success<>(true, warnings);
    }

    @Override public PersistenceResult<Boolean> rollback(BackupDescriptor backup) {
        return storage.apply(backup.documentId()).delete(backup.storageName());
    }

    @Override public PersistenceResult<List<BackupDescriptor>> list(ScholarDocumentId id) {
        var listed = storage.apply(id).list();
        if (listed instanceof PersistenceResult.Failure<List<String>> failure) return new PersistenceResult.Failure<>(failure.diagnostics());
        var result = ((PersistenceResult.Success<List<String>>) listed).value().stream()
                .map(name -> descriptor(id, name))
                .filter(java.util.Optional::isPresent).map(java.util.Optional::orElseThrow)
                .sorted(Comparator.comparingLong(BackupDescriptor::savedAtEpochMillis).reversed()
                        .thenComparing(BackupDescriptor::storageName, Comparator.reverseOrder())).toList();
        return new PersistenceResult.Success<>(result, listed.diagnostics());
    }

    @Override public PersistenceResult<Document> load(BackupDescriptor backup) {
        return storage.apply(backup.documentId()).load(backup.storageName());
    }

    private static String backupName(long timestamp, int sequence) {
        return String.format(java.util.Locale.ROOT, "%013d-%03d", timestamp, sequence);
    }

    private static java.util.Optional<BackupDescriptor> descriptor(ScholarDocumentId id, String name) {
        try {
            var separator = name.lastIndexOf('-');
            return java.util.Optional.of(new BackupDescriptor(id, name, Long.parseLong(name.substring(0, separator))));
        } catch (RuntimeException exception) {
            return java.util.Optional.empty();
        }
    }

    private static int sequence(String name) {
        try { return Integer.parseInt(name.substring(name.lastIndexOf('-') + 1)); }
        catch (RuntimeException exception) { return -1; }
    }
}
