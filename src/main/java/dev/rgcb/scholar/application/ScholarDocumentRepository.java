package dev.rgcb.scholar.application;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.persistence.PersistenceResult;
import java.util.List;

public interface ScholarDocumentRepository {
    PersistenceResult<List<ScholarDocumentDescriptor>> listDocuments();
    PersistenceResult<OpenedScholarDocument> createDocument(String displayName, Document document);
    PersistenceResult<OpenedScholarDocument> openDocument(ScholarDocumentId id);
    PersistenceResult<ScholarDocumentDescriptor> saveDocument(ScholarDocumentId id, Document document);
    PersistenceResult<OpenedScholarDocument> saveAs(String displayName, Document document);
    PersistenceResult<ScholarDocumentDescriptor> renameDocument(ScholarDocumentId id, String displayName);
    PersistenceResult<Boolean> deleteDocument(ScholarDocumentId id);
}
