package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.persistence.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Application lifecycle state outside semantic history. UI owns Save/Discard/Cancel choices. */
public final class DocumentWorkspace implements EditorDocumentWorkspace {
    private EditorSession session;
    private Document savedDocument;
    private Optional<String> name = Optional.empty();
    private final DocumentStorage storage;

    public DocumentWorkspace(Document initial, DocumentStorage storage) {
        this.storage = Objects.requireNonNull(storage);
        session = sessionFor(initial);
        // An unnamed initial document has no saved baseline.
    }
    public EditorSession session() { return session; }
    public Optional<String> name() { return name; }
    public boolean isDirty() { return savedDocument == null || !session.current().document().equals(savedDocument); }
    public PersistenceResult<List<String>> list() { return storage.list(); }

    public PersistenceResult<String> saveAs(String requestedName) {
        var snapshot = session.current().document();
        var result = storage.save(requestedName, snapshot);
        if (result instanceof PersistenceResult.Success<String>) {
            savedDocument = snapshot;
            name = Optional.of(requestedName);
        }
        return result;
    }
    public PersistenceResult<String> save() {
        return name.map(this::saveAs).orElseGet(() -> PersistenceResult.failure(PersistenceDiagnostic.Code.INVALID_NAME, "name", "Choose a document name first."));
    }
    /** Caller must obtain an explicit unsaved-work decision before invoking open/new. */
    public PersistenceResult<Document> open(String requestedName) {
        var result = storage.load(requestedName);
        if (result instanceof PersistenceResult.Success<Document> success) {
            try {
                var candidate = sessionFor(success.value());
                session = candidate;
                savedDocument = success.value();
                name = Optional.of(requestedName);
            } catch (IllegalArgumentException | IllegalStateException e) {
                return PersistenceResult.failure(PersistenceDiagnostic.Code.VALIDATION_FAILURE, "selection", "Document has no valid initial editor selection.");
            }
        }
        return result;
    }
    public void newDocument() {
        session = sessionFor(new Document(List.of(new Paragraph(new InlineContent(List.of())))));
        savedDocument = null;
        name = Optional.empty();
    }
    private static EditorSession sessionFor(Document document) {
        if (!dev.rgcb.scholar.validation.DocumentValidator.validate(document).isValid()) throw new IllegalArgumentException("Invalid document.");
        for (var i = 0; i < document.blocks().size(); i++) {
            if (document.blocks().get(i) instanceof Paragraph || document.blocks().get(i) instanceof Heading) return new EditorSession(document, i);
        }
        if (document.blocks().isEmpty()) throw new IllegalArgumentException("Document has no selectable content.");
        return new EditorSession(new EditorState(document, new BlockSelection(0), Optional.empty()));
    }
}
