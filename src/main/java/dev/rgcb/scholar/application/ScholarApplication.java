package dev.rgcb.scholar.application;

import dev.rgcb.scholar.persistence.PersistenceDiagnostic;
import dev.rgcb.scholar.persistence.PersistenceResult;
import java.util.List;
import java.util.Objects;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Multi-document application facade. It has no Minecraft or editor-widget dependencies. */
public final class ScholarApplication {
    private final ScholarDocumentRepository repository;
    private final Set<ApplicationDocumentWorkspace> openWorkspaces = Collections.newSetFromMap(new WeakHashMap<>());

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

    public PersistenceResult<ApplicationDocumentWorkspace> createReadabilitySample() {
        var listed = documents();
        if (listed instanceof PersistenceResult.Failure<List<ScholarDocumentDescriptor>> failure) {
            return new PersistenceResult.Failure<>(failure.diagnostics());
        }
        var names = ((PersistenceResult.Success<List<ScholarDocumentDescriptor>>) listed).value().stream()
                .map(ScholarDocumentDescriptor::displayName).toList();
        var name = "M34";
        for (var suffix = 2; names.contains(name); suffix++) name = "M34 " + suffix;
        return workspaceFrom(repository.createDocument(name, ScholarDocuments.m34Readability()));
    }

    public void closeWorkspace(ApplicationDocumentWorkspace workspace) {
        openWorkspaces.remove(workspace);
    }

    public PersistenceResult<Boolean> deleteDocument(ScholarDocumentId id) {
        Objects.requireNonNull(id, "id");
        for (var workspace : openWorkspaces) {
            if (workspace.id().equals(id)) {
                return PersistenceResult.failure(PersistenceDiagnostic.Code.VALIDATION_FAILURE, "document",
                        workspace.isDirty() ? "Save or discard changes and close the document before deleting it."
                                : "Close the document before deleting it.");
            }
        }
        return repository.deleteDocument(id);
    }

    private PersistenceResult<ApplicationDocumentWorkspace> workspaceFrom(PersistenceResult<OpenedScholarDocument> result) {
        if (result instanceof PersistenceResult.Failure<OpenedScholarDocument> failure) {
            return new PersistenceResult.Failure<>(failure.diagnostics());
        }
        try {
            var opened = ((PersistenceResult.Success<OpenedScholarDocument>) result).value();
            var workspace = new ApplicationDocumentWorkspace(repository, opened);
            openWorkspaces.add(workspace);
            return new PersistenceResult.Success<>(workspace, result.diagnostics());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return PersistenceResult.failure(PersistenceDiagnostic.Code.VALIDATION_FAILURE, "document", "Document has no valid editor entry selection.");
        }
    }
}
