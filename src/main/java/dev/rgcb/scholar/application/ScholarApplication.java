package dev.rgcb.scholar.application;

import dev.rgcb.scholar.persistence.PersistenceDiagnostic;
import dev.rgcb.scholar.persistence.PersistenceResult;
import dev.rgcb.scholar.document.DocumentTemplateId;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Multi-document application facade. It has no Minecraft or editor-widget dependencies. */
public final class ScholarApplication {
    public enum EventKind { DOCUMENT_OPENED, DOCUMENT_CHANGED, DOCUMENT_SAVED, DOCUMENT_CLOSED }
    public record Event(EventKind kind, ScholarDocumentId id) {}
    private final ScholarDocumentRepository repository;
    private final DocumentRecoveryService recovery;
    private final Clock clock;
    private final RecoveryPolicy recoveryPolicy;
    private final Set<ApplicationDocumentWorkspace> openWorkspaces = Collections.newSetFromMap(new WeakHashMap<>());
    private final CopyOnWriteArrayList<Consumer<Event>> listeners = new CopyOnWriteArrayList<>();
    private volatile List<PersistenceDiagnostic> recoveryDiagnostics = List.of();
    private long nextRecoveryAt;

    public AutoCloseable subscribe(Consumer<Event> listener) {
        listeners.add(Objects.requireNonNull(listener));
        return () -> listeners.remove(listener);
    }

    private void emit(Event event) {
        for (var listener : listeners) {
            try { listener.accept(event); }
            catch (RuntimeException failure) { System.getLogger(ScholarApplication.class.getName())
                    .log(System.Logger.Level.WARNING, "Scholar lifecycle listener failed", failure); }
        }
    }

    public java.util.Optional<ApplicationDocumentWorkspace> activeWorkspace(ScholarDocumentId id) {
        return openWorkspaces.stream().filter(workspace -> workspace.id().equals(id)).findFirst();
    }

    public boolean isOpen(ApplicationDocumentWorkspace workspace) { return openWorkspaces.contains(workspace); }

    public boolean isDirtyOpen(ScholarDocumentId id) {
        return openWorkspaces.stream().anyMatch(workspace -> workspace.id().equals(id) && workspace.isDirty());
    }

    public ScholarApplication(ScholarDocumentRepository repository) {
        this(repository, disabledRecovery(), Clock.systemUTC(), RecoveryPolicy.DEFAULT);
    }

    public ScholarApplication(ScholarDocumentRepository repository, DocumentRecoveryService recovery,
                              Clock clock, RecoveryPolicy recoveryPolicy) {
        this.repository = Objects.requireNonNull(repository);
        this.recovery = Objects.requireNonNull(recovery);
        this.clock = Objects.requireNonNull(clock);
        this.recoveryPolicy = Objects.requireNonNull(recoveryPolicy);
        this.nextRecoveryAt = clock.millis() + recoveryPolicy.interval().toMillis();
    }

    public PersistenceResult<List<ScholarDocumentDescriptor>> documents() {
        return repository.listDocuments();
    }

    public PersistenceResult<ApplicationDocumentWorkspace> createDocument() {
        return createDocument(DocumentTemplateCatalog.BLANK);
    }

    public PersistenceResult<ApplicationDocumentWorkspace> createDocument(DocumentTemplateId template) {
        return createDocument(DocumentTemplateCatalog.keyFor(template));
    }

    public PersistenceResult<ApplicationDocumentWorkspace> createDocument(DocumentTemplateKey templateId) {
        var listed = documents();
        if (listed instanceof PersistenceResult.Failure<List<ScholarDocumentDescriptor>> failure) {
            return new PersistenceResult.Failure<>(failure.diagnostics());
        }
        var template = DocumentTemplateCatalog.require(templateId);
        var names = ((PersistenceResult.Success<List<ScholarDocumentDescriptor>>) listed).value().stream()
                .map(ScholarDocumentDescriptor::displayName).toList();
        var created = repository.createDocument(template.uniqueDocumentName(names), template.createDocument());
        return workspaceFrom(created, true);
    }

    public PersistenceResult<ApplicationDocumentWorkspace> openDocument(ScholarDocumentId id) {
        return workspaceFrom(repository.openDocument(id), false);
    }

    /** Reuses an active editor workspace when an integration and the GUI address the same document. */
    public PersistenceResult<ApplicationDocumentWorkspace> openActiveDocument(ScholarDocumentId id) {
        for (var workspace : openWorkspaces) {
            if (workspace.id().equals(id)) return new PersistenceResult.Success<>(workspace, List.of());
        }
        return openDocument(id);
    }

    public PersistenceResult<ApplicationDocumentWorkspace> createReadabilitySample() {
        return createDocument(DocumentTemplateCatalog.READABILITY_SAMPLE);
    }

    public void closeWorkspace(ApplicationDocumentWorkspace workspace) {
        if (workspace.isDirty()) recordRecoveryResult(workspace.captureRecovery(clock.millis()));
        if (openWorkspaces.remove(workspace)) emit(new Event(EventKind.DOCUMENT_CLOSED, workspace.id()));
    }

    /** Called by the client lifecycle; snapshot capture is synchronous and never mutates editor state. */
    public void recoveryTick() {
        var now = clock.millis();
        if (now < nextRecoveryAt) return;
        nextRecoveryAt = now + recoveryPolicy.interval().toMillis();
        captureRecoveryNow();
    }

    public void captureRecoveryNow() {
        var now = clock.millis();
        var diagnostics = new ArrayList<PersistenceDiagnostic>();
        for (var workspace : List.copyOf(openWorkspaces)) {
            var result = workspace.captureRecovery(now);
            if (result instanceof PersistenceResult.Failure<Boolean> failure) diagnostics.addAll(failure.diagnostics());
            else diagnostics.addAll(result.diagnostics());
        }
        recoveryDiagnostics = List.copyOf(diagnostics);
    }

    public List<PersistenceDiagnostic> recoveryDiagnostics() { return recoveryDiagnostics; }

    public PersistenceResult<List<RecoveryCandidate>> recoveryCandidates() {
        var discovered = recovery.discover();
        if (discovered instanceof PersistenceResult.Failure<List<RecoveryCandidate>> failure) {
            return new PersistenceResult.Failure<>(failure.diagnostics());
        }
        var candidates = new ArrayList<RecoveryCandidate>();
        var diagnostics = new ArrayList<>(discovered.diagnostics());
        for (var candidate : ((PersistenceResult.Success<List<RecoveryCandidate>>) discovered).value()) {
            if (candidate.documentId().isEmpty()) {
                candidates.add(candidate.withSourceState(RecoveryCandidate.SourceState.UNTITLED));
                continue;
            }
            var opened = repository.openDocument(candidate.documentId().orElseThrow());
            if (opened instanceof PersistenceResult.Success<OpenedScholarDocument> canonical) {
                var snapshot = recovery.load(candidate.recoveryId());
                if (snapshot instanceof PersistenceResult.Success<RecoverySnapshot> loaded
                        && loaded.value().document().equals(canonical.value().document())) {
                    var discarded = recovery.discard(candidate.recoveryId());
                    if (discarded instanceof PersistenceResult.Failure<Boolean> failure) {
                        diagnostics.addAll(asWarnings(failure.diagnostics(), "Stale recovery could not be removed."));
                    }
                    continue;
                }
                candidates.add(candidate.withSourceState(candidate.sourceState() == RecoveryCandidate.SourceState.UNTITLED
                        ? RecoveryCandidate.SourceState.UNTITLED : RecoveryCandidate.SourceState.EXISTING_DOCUMENT));
            } else {
                candidates.add(candidate.withSourceState(candidate.sourceState() == RecoveryCandidate.SourceState.UNTITLED
                        ? RecoveryCandidate.SourceState.UNTITLED : RecoveryCandidate.SourceState.MISSING_DOCUMENT));
            }
        }
        return new PersistenceResult.Success<>(List.copyOf(candidates), List.copyOf(diagnostics));
    }

    public PersistenceResult<ApplicationDocumentWorkspace> recover(RecoveryId id) {
        var loaded = recovery.load(Objects.requireNonNull(id));
        if (loaded instanceof PersistenceResult.Failure<RecoverySnapshot> failure) {
            return new PersistenceResult.Failure<>(failure.diagnostics());
        }
        var snapshot = ((PersistenceResult.Success<RecoverySnapshot>) loaded).value();
        OpenedScholarDocument canonical = null;
        if (snapshot.documentId().isPresent()) {
            var opened = repository.openDocument(snapshot.documentId().orElseThrow());
            if (opened instanceof PersistenceResult.Success<OpenedScholarDocument> success) canonical = success.value();
        }
        var descriptor = canonical == null
                ? new ScholarDocumentDescriptor(snapshot.documentId().orElseGet(ScholarDocumentId::create),
                        snapshot.displayName(), 0, 0, DocumentPreview.from(snapshot.document(), snapshot.displayName()))
                : canonical.descriptor();
        var baseline = canonical == null ? snapshot.document() : canonical.document();
        try {
            var workspace = new ApplicationDocumentWorkspace(repository, descriptor, baseline, snapshot.document(),
                    this::emit, recovery, snapshot.recoveryId(), snapshot.originallyUntitled(), canonical != null, true);
            openWorkspaces.add(workspace);
            emit(new Event(EventKind.DOCUMENT_OPENED, workspace.id()));
            return new PersistenceResult.Success<>(workspace, loaded.diagnostics());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return PersistenceResult.failure(PersistenceDiagnostic.Code.VALIDATION_FAILURE, "recovery",
                    "Recovered document has no valid editor entry selection.");
        }
    }

    public PersistenceResult<Boolean> discardRecovery(RecoveryId id) {
        return recovery.discard(Objects.requireNonNull(id));
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

    public PersistenceResult<String> renameDocument(ScholarDocumentId id, String displayName) {
        Objects.requireNonNull(id, "id");
        var active = activeWorkspace(id);
        if (active.isPresent()) return active.orElseThrow().rename(displayName);
        var renamed = repository.renameDocument(id, displayName);
        if (renamed instanceof PersistenceResult.Success<ScholarDocumentDescriptor> success) {
            return new PersistenceResult.Success<>(success.value().displayName(), success.diagnostics());
        }
        return new PersistenceResult.Failure<>(renamed.diagnostics());
    }

    private PersistenceResult<ApplicationDocumentWorkspace> workspaceFrom(PersistenceResult<OpenedScholarDocument> result,
                                                                          boolean originallyUntitled) {
        if (result instanceof PersistenceResult.Failure<OpenedScholarDocument> failure) {
            return new PersistenceResult.Failure<>(failure.diagnostics());
        }
        try {
            var opened = ((PersistenceResult.Success<OpenedScholarDocument>) result).value();
            var workspace = new ApplicationDocumentWorkspace(repository, opened, this::emit, recovery, originallyUntitled);
            openWorkspaces.add(workspace);
            emit(new Event(EventKind.DOCUMENT_OPENED, workspace.id()));
            return new PersistenceResult.Success<>(workspace, result.diagnostics());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return PersistenceResult.failure(PersistenceDiagnostic.Code.VALIDATION_FAILURE, "document", "Document has no valid editor entry selection.");
        }
    }

    private void recordRecoveryResult(PersistenceResult<Boolean> result) {
        recoveryDiagnostics = result.diagnostics();
    }

    private static List<PersistenceDiagnostic> asWarnings(List<PersistenceDiagnostic> diagnostics, String message) {
        return diagnostics.stream().map(value -> new PersistenceDiagnostic(PersistenceDiagnostic.Severity.WARNING,
                value.code(), value.path(), message)).toList();
    }

    static DocumentRecoveryService disabledRecovery() { return DisabledRecovery.INSTANCE; }

    private enum DisabledRecovery implements DocumentRecoveryService {
        INSTANCE;
        @Override public PersistenceResult<RecoveryCandidate> capture(RecoverySnapshot snapshot) {
            return new PersistenceResult.Success<>(new RecoveryCandidate(snapshot.recoveryId(), snapshot.documentId(),
                    snapshot.displayName(), snapshot.capturedAtEpochMillis(), snapshot.confirmedModifiedAtEpochMillis(),
                    snapshot.revision(), RecoveryCandidate.SourceState.MISSING_DOCUMENT), List.of());
        }
        @Override public PersistenceResult<List<RecoveryCandidate>> discover() {
            return new PersistenceResult.Success<>(List.of(), List.of());
        }
        @Override public PersistenceResult<RecoverySnapshot> load(RecoveryId id) {
            return PersistenceResult.failure(PersistenceDiagnostic.Code.IO_FAILURE, "recovery", "Recovery is unavailable.");
        }
        @Override public PersistenceResult<Boolean> discard(RecoveryId id) {
            return new PersistenceResult.Success<>(true, List.of());
        }
        @Override public PersistenceResult<Boolean> discardForDocument(ScholarDocumentId id) {
            return new PersistenceResult.Success<>(true, List.of());
        }
    }
}
