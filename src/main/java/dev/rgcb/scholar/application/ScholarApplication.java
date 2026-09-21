package dev.rgcb.scholar.application;

import dev.rgcb.scholar.persistence.PersistenceDiagnostic;
import dev.rgcb.scholar.persistence.PersistenceResult;
import java.util.List;
import java.util.Objects;

/** Multi-document application facade. It has no Minecraft or editor-widget dependencies. */
public final class ScholarApplication {
    private final ScholarDocumentRepository repository;

    public ScholarApplication(ScholarDocumentRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public PersistenceResult<List<ScholarDocumentDescriptor>> documents() {
        return repository.listDocuments();
    }

    public PersistenceResult<ApplicationDocumentWorkspace> createDocument() {
        return createDocument(dev.rgcb.scholar.document.DocumentTemplateId.BLANK);
    }

    public PersistenceResult<ApplicationDocumentWorkspace> createDocument(dev.rgcb.scholar.document.DocumentTemplateId template) {
        var listed = documents();
        if (listed instanceof PersistenceResult.Failure<List<ScholarDocumentDescriptor>> failure) {
            return new PersistenceResult.Failure<>(failure.diagnostics());
        }
        var names = ((PersistenceResult.Success<List<ScholarDocumentDescriptor>>) listed).value().stream()
                .map(ScholarDocumentDescriptor::displayName).toList();
        var created = repository.createDocument(ScholarDocumentNames.untitled(names), ScholarDocuments.fromTemplate(template));
        return workspaceFrom(created);
    }

    public PersistenceResult<ApplicationDocumentWorkspace> openDocument(ScholarDocumentId id) {
        return workspaceFrom(repository.openDocument(id));
    }

    private PersistenceResult<ApplicationDocumentWorkspace> workspaceFrom(PersistenceResult<OpenedScholarDocument> result) {
        if (result instanceof PersistenceResult.Failure<OpenedScholarDocument> failure) {
            return new PersistenceResult.Failure<>(failure.diagnostics());
        }
        try {
            var opened = ((PersistenceResult.Success<OpenedScholarDocument>) result).value();
            return new PersistenceResult.Success<>(new ApplicationDocumentWorkspace(repository, opened), result.diagnostics());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return PersistenceResult.failure(PersistenceDiagnostic.Code.VALIDATION_FAILURE, "document", "Document has no valid editor entry selection.");
        }
    }
}
