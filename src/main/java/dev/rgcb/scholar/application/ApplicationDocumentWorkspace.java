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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** One open application document and its clean baseline; never shared across opens. */
public final class ApplicationDocumentWorkspace implements EditorDocumentWorkspace {
    private final ScholarDocumentRepository repository;
    private ScholarDocumentDescriptor descriptor;
    private Document savedDocument;
    private final EditorSession session;
    private final Consumer<ScholarApplication.Event> events;
    private final DocumentRecoveryService recovery;
    private final RecoveryId recoveryId;
    private boolean originallyUntitled;
    private boolean canonicalPresent;
    private boolean unconfirmed;
    private long revision;
    private long capturedRevision = -1;

    ApplicationDocumentWorkspace(ScholarDocumentRepository repository, OpenedScholarDocument opened) {
        this(repository, opened, ignored -> {}, ScholarApplication.disabledRecovery(), false);
    }

    ApplicationDocumentWorkspace(ScholarDocumentRepository repository, OpenedScholarDocument opened,
                                 Consumer<ScholarApplication.Event> events) {
        this(repository, opened, events, ScholarApplication.disabledRecovery(), false);
    }

    ApplicationDocumentWorkspace(ScholarDocumentRepository repository, OpenedScholarDocument opened,
                                 Consumer<ScholarApplication.Event> events, DocumentRecoveryService recovery,
                                 boolean originallyUntitled) {
        this(repository, opened.descriptor(), opened.document(), opened.document(), events, recovery,
                RecoveryId.create(), originallyUntitled, true, false);
    }

    ApplicationDocumentWorkspace(ScholarDocumentRepository repository, ScholarDocumentDescriptor descriptor,
                                 Document savedDocument, Document workingDocument,
                                 Consumer<ScholarApplication.Event> events, DocumentRecoveryService recovery,
                                 RecoveryId recoveryId, boolean originallyUntitled,
                                 boolean canonicalPresent, boolean unconfirmed) {
        this.repository = Objects.requireNonNull(repository);
        this.events = Objects.requireNonNull(events);
        this.recovery = Objects.requireNonNull(recovery);
        this.recoveryId = Objects.requireNonNull(recoveryId);
        this.originallyUntitled = originallyUntitled;
        this.canonicalPresent = canonicalPresent;
        this.unconfirmed = unconfirmed;
        this.descriptor = Objects.requireNonNull(descriptor);
        this.savedDocument = Objects.requireNonNull(savedDocument);
        this.session = sessionFor(workingDocument);
        this.session.onDocumentChange(ignored -> {
            revision++;
            this.events.accept(new ScholarApplication.Event(ScholarApplication.EventKind.DOCUMENT_CHANGED, id()));
        });
    }

    public ScholarDocumentDescriptor descriptor() { return descriptor; }
    public ScholarDocumentId id() { return descriptor.id(); }
    public String displayName() { return descriptor.displayName(); }
    public Optional<String> name() { return Optional.of(displayName()); }
    public EditorSession session() { return session; }
    public boolean isDirty() { return unconfirmed || !savedDocument.equals(session.current().document()); }
    public RecoveryId recoveryId() { return recoveryId; }
    long revision() { return revision; }

    public PersistenceResult<String> save() {
        var snapshot = session.current().document();
        if (!canonicalPresent) return saveAs(displayName());
        var previousId = id();
        var result = repository.saveDocument(previousId, snapshot);
        if (result instanceof PersistenceResult.Success<ScholarDocumentDescriptor> success) {
            descriptor = success.value(); savedDocument = snapshot; unconfirmed = false; originallyUntitled = false;
            var diagnostics = new ArrayList<>(success.diagnostics());
            invalidateRecovery(diagnostics);
            events.accept(new ScholarApplication.Event(ScholarApplication.EventKind.DOCUMENT_SAVED, id()));
            return new PersistenceResult.Success<>(displayName(), List.copyOf(diagnostics));
        }
        return new PersistenceResult.Failure<>(result.diagnostics());
    }

    public PersistenceResult<String> saveAs(String displayName) {
        var snapshot = session.current().document();
        var result = repository.saveAs(displayName, snapshot);
        if (result instanceof PersistenceResult.Success<OpenedScholarDocument> success) {
            descriptor = success.value().descriptor(); savedDocument = snapshot;
            canonicalPresent = true; unconfirmed = false; originallyUntitled = false;
            var diagnostics = new ArrayList<>(success.diagnostics());
            invalidateRecovery(diagnostics);
            events.accept(new ScholarApplication.Event(ScholarApplication.EventKind.DOCUMENT_SAVED, id()));
            return new PersistenceResult.Success<>(this.displayName(), List.copyOf(diagnostics));
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

    PersistenceResult<Boolean> captureRecovery(long capturedAtEpochMillis) {
        if (!isDirty() || revision == capturedRevision) return new PersistenceResult.Success<>(false, List.of());
        var snapshot = new RecoverySnapshot(recoveryId,
                canonicalPresent ? Optional.of(id()) : Optional.empty(), originallyUntitled, displayName(),
                capturedAtEpochMillis,
                canonicalPresent ? Optional.of(descriptor.modifiedAtEpochMillis()) : Optional.empty(),
                revision, session.current().document());
        var result = recovery.capture(snapshot);
        if (result instanceof PersistenceResult.Success<RecoveryCandidate> success) {
            capturedRevision = revision;
            return new PersistenceResult.Success<>(true, success.diagnostics());
        }
        return new PersistenceResult.Failure<>(result.diagnostics());
    }

    private void invalidateRecovery(ArrayList<dev.rgcb.scholar.persistence.PersistenceDiagnostic> diagnostics) {
        var selected = recovery.discard(recoveryId);
        if (selected instanceof PersistenceResult.Failure<Boolean> failure) diagnostics.addAll(asWarnings(failure.diagnostics()));
        capturedRevision = revision;
    }

    private static List<dev.rgcb.scholar.persistence.PersistenceDiagnostic> asWarnings(
            List<dev.rgcb.scholar.persistence.PersistenceDiagnostic> diagnostics
    ) {
        return diagnostics.stream().map(value -> new dev.rgcb.scholar.persistence.PersistenceDiagnostic(
                dev.rgcb.scholar.persistence.PersistenceDiagnostic.Severity.WARNING, value.code(), value.path(),
                "Document saved, but stale recovery cleanup could not be completed.")).toList();
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
