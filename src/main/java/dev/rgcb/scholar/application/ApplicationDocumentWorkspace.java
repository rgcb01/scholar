package dev.rgcb.scholar.application;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.editor.BlockSelection;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.editor.EditorState;
import dev.rgcb.scholar.editor.EditorDocumentWorkspace;
import dev.rgcb.scholar.persistence.PersistenceResult;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.Objects;
import java.util.Optional;

/** One open application document and its clean baseline; never shared across opens. */
public final class ApplicationDocumentWorkspace implements EditorDocumentWorkspace {
    private final ScholarDocumentRepository repository;
    private ScholarDocumentDescriptor descriptor;
    private Document savedDocument;
    private final EditorSession session;

    ApplicationDocumentWorkspace(ScholarDocumentRepository repository, OpenedScholarDocument opened) {
        this.repository = Objects.requireNonNull(repository);
        this.descriptor = opened.descriptor();
        this.savedDocument = opened.document();
        this.session = sessionFor(opened.document());
    }

    public ScholarDocumentDescriptor descriptor() { return descriptor; }
    public ScholarDocumentId id() { return descriptor.id(); }
    public String displayName() { return descriptor.displayName(); }
    public Optional<String> name() { return Optional.of(displayName()); }
    public EditorSession session() { return session; }
    public boolean isDirty() { return !savedDocument.equals(session.current().document()); }

    public PersistenceResult<String> save() {
        var snapshot = session.current().document();
        var result = repository.saveDocument(id(), snapshot);
        if (result instanceof PersistenceResult.Success<ScholarDocumentDescriptor> success) {
            descriptor = success.value(); savedDocument = snapshot;
            return new PersistenceResult.Success<>(displayName(), success.diagnostics());
        }
        return new PersistenceResult.Failure<>(result.diagnostics());
    }

    public PersistenceResult<String> saveAs(String displayName) {
        var snapshot = session.current().document();
        var result = repository.saveAs(displayName, snapshot);
        if (result instanceof PersistenceResult.Success<OpenedScholarDocument> success) {
            descriptor = success.value().descriptor(); savedDocument = snapshot;
            return new PersistenceResult.Success<>(this.displayName(), success.diagnostics());
        }
        return new PersistenceResult.Failure<>(result.diagnostics());
    }

    public PersistenceResult<String> rename(String displayName) {
        var result = repository.renameDocument(id(), displayName);
        if (result instanceof PersistenceResult.Success<ScholarDocumentDescriptor> success) {
            descriptor = success.value();
            return new PersistenceResult.Success<>(this.displayName(), success.diagnostics());
        }
        return new PersistenceResult.Failure<>(result.diagnostics());
    }

    private static EditorSession sessionFor(Document document) {
        if (!DocumentValidator.validate(document).isValid()) throw new IllegalArgumentException("Invalid document.");
        for (var index = 0; index < document.blocks().size(); index++) {
            if (document.blocks().get(index) instanceof Paragraph || document.blocks().get(index) instanceof Heading) {
                return new EditorSession(document, index);
            }
        }
        if (document.blocks().isEmpty()) throw new IllegalArgumentException("Document has no selectable content.");
        return new EditorSession(new EditorState(document, new BlockSelection(0), Optional.empty()));
    }
}
