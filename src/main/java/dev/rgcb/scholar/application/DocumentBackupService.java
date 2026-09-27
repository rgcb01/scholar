package dev.rgcb.scholar.application;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.persistence.PersistenceResult;
import java.util.List;

/** Storage boundary for prior confirmed canonical document versions. */
public interface DocumentBackupService {
    PersistenceResult<BackupDescriptor> preserve(ScholarDocumentId id, long savedAtEpochMillis, Document document);
    PersistenceResult<Boolean> complete(BackupDescriptor backup);
    PersistenceResult<Boolean> rollback(BackupDescriptor backup);
    PersistenceResult<List<BackupDescriptor>> list(ScholarDocumentId id);
    PersistenceResult<Document> load(BackupDescriptor backup);
}
