package dev.rgcb.scholar.application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.persistence.FileDocumentStorage;
import dev.rgcb.scholar.persistence.PersistenceDiagnostic;
import dev.rgcb.scholar.persistence.PersistenceResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/** Application repository layered over the M26 document storage and codec. */
public final class FileScholarDocumentRepository implements ScholarDocumentRepository {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String INDEX_FORMAT = "scholar-workspace";
    private static final int INDEX_VERSION = 1;
    private static final long MAX_INDEX_BYTES = 1_048_576;
    private final Path root;
    private final Path indexPath;
    private final FileDocumentStorage storage;
    private final Clock clock;
    private final Supplier<ScholarDocumentId> ids;

    public FileScholarDocumentRepository(Path root) {
        this(root, Clock.systemUTC(), ScholarDocumentId::create);
    }

    public FileScholarDocumentRepository(Path root, Clock clock, Supplier<ScholarDocumentId> ids) {
        this.root = Objects.requireNonNull(root).toAbsolutePath().normalize();
        this.indexPath = this.root.resolve("workspace.json");
        this.storage = new FileDocumentStorage(this.root.resolve("documents"));
        this.clock = Objects.requireNonNull(clock);
        this.ids = Objects.requireNonNull(ids);
    }

    @Override public PersistenceResult<List<ScholarDocumentDescriptor>> listDocuments() {
        var records = loadIndex();
        var stored = storage.list();
        if (stored instanceof PersistenceResult.Failure<List<String>> failure) return new PersistenceResult.Failure<>(failure.diagnostics());
        var available = ((PersistenceResult.Success<List<String>>) stored).value();
        var result = new ArrayList<ScholarDocumentDescriptor>();
        for (var key : available) {
            var id = new ScholarDocumentId(key);
            var descriptor = records.get(key);
            result.add(descriptor != null ? descriptor : recoveredDescriptor(id));
        }
        result.sort(Comparator.comparingLong(ScholarDocumentDescriptor::modifiedAtEpochMillis).reversed()
                .thenComparing(ScholarDocumentDescriptor::displayName, String.CASE_INSENSITIVE_ORDER));
        return new PersistenceResult.Success<>(List.copyOf(result), List.of());
    }

    @Override public PersistenceResult<OpenedScholarDocument> createDocument(String displayName, Document document) {
        final String normalized;
        try { normalized = ScholarDocumentNames.normalize(displayName); }
        catch (IllegalArgumentException e) { return invalidName(); }
        var stored = storage.list();
        if (stored instanceof PersistenceResult.Failure<List<String>> failure) return new PersistenceResult.Failure<>(failure.diagnostics());
        final ScholarDocumentId id;
        try { id = nextUnusedId(((PersistenceResult.Success<List<String>>) stored).value()); }
        catch (IllegalStateException exception) {
            return PersistenceResult.failure(PersistenceDiagnostic.Code.IO_FAILURE, "identity", "A free document identity could not be allocated.");
        }
        var saved = storage.save(id.value(), document);
        if (saved instanceof PersistenceResult.Failure<String> failure) return new PersistenceResult.Failure<>(failure.diagnostics());
        var now = clock.millis();
        var descriptor = new ScholarDocumentDescriptor(id, normalized, now, now, DocumentPreview.from(document, normalized));
        var records = loadIndex(); records.put(id.value(), descriptor);
        var metadata = saveIndex(records);
        return new PersistenceResult.Success<>(new OpenedScholarDocument(descriptor, document), metadataWarnings(metadata));
    }

    @Override public PersistenceResult<OpenedScholarDocument> openDocument(ScholarDocumentId id) {
        var loaded = storage.load(id.value());
        if (loaded instanceof PersistenceResult.Failure<Document> failure) return new PersistenceResult.Failure<>(failure.diagnostics());
        var document = ((PersistenceResult.Success<Document>) loaded).value();
        var descriptor = loadIndex().getOrDefault(id.value(), recoveredDescriptor(id));
        return new PersistenceResult.Success<>(new OpenedScholarDocument(descriptor, document), loaded.diagnostics());
    }

    @Override public PersistenceResult<ScholarDocumentDescriptor> saveDocument(ScholarDocumentId id, Document document) {
        var existing = openDocument(id);
        if (existing instanceof PersistenceResult.Failure<OpenedScholarDocument> failure) return new PersistenceResult.Failure<>(failure.diagnostics());
        var current = ((PersistenceResult.Success<OpenedScholarDocument>) existing).value().descriptor();
        var saved = storage.save(id.value(), document);
        if (saved instanceof PersistenceResult.Failure<String> failure) return new PersistenceResult.Failure<>(failure.diagnostics());
        var descriptor = new ScholarDocumentDescriptor(id, current.displayName(), current.createdAtEpochMillis(),
                clock.millis(), DocumentPreview.from(document, current.displayName()));
        var records = loadIndex(); records.put(id.value(), descriptor);
        var metadata = saveIndex(records);
        return new PersistenceResult.Success<>(descriptor, metadataWarnings(metadata));
    }

    @Override public PersistenceResult<OpenedScholarDocument> saveAs(String displayName, Document document) {
        return createDocument(displayName, document);
    }

    @Override public PersistenceResult<ScholarDocumentDescriptor> renameDocument(ScholarDocumentId id, String displayName) {
        final String normalized;
        try { normalized = ScholarDocumentNames.normalize(displayName); }
        catch (IllegalArgumentException e) { return invalidName(); }
        var opened = openDocument(id);
        if (opened instanceof PersistenceResult.Failure<OpenedScholarDocument> failure) return new PersistenceResult.Failure<>(failure.diagnostics());
        var value = ((PersistenceResult.Success<OpenedScholarDocument>) opened).value();
        var previous = value.descriptor();
        var renamed = new ScholarDocumentDescriptor(id, normalized, previous.createdAtEpochMillis(), clock.millis(),
                DocumentPreview.from(value.document(), normalized));
        var records = loadIndex(); records.put(id.value(), renamed);
        var metadata = saveIndex(records);
        if (metadata instanceof PersistenceResult.Failure<Boolean> failure) return new PersistenceResult.Failure<>(failure.diagnostics());
        return new PersistenceResult.Success<>(renamed, List.of());
    }

    @Override public PersistenceResult<Boolean> deleteDocument(ScholarDocumentId id) {
        Objects.requireNonNull(id, "id");
        var stored = storage.list();
        if (stored instanceof PersistenceResult.Failure<List<String>> failure) return new PersistenceResult.Failure<>(failure.diagnostics());
        if (!((PersistenceResult.Success<List<String>>) stored).value().contains(id.value())) {
            return PersistenceResult.failure(PersistenceDiagnostic.Code.IO_FAILURE, "document", "Document no longer exists.");
        }
        var records = loadIndex();
        var retired = loadRetiredIds();
        retired.add(id.value());
        var reserved = saveIndex(records, retired);
        if (reserved instanceof PersistenceResult.Failure<Boolean> failure) return failure;
        var deleted = storage.delete(id.value());
        if (deleted instanceof PersistenceResult.Failure<Boolean> failure) return failure;
        records.remove(id.value());
        var metadata = saveIndex(records, retired);
        return new PersistenceResult.Success<>(true, metadataWarnings(metadata));
    }

    private ScholarDocumentId nextUnusedId(List<String> files) {
        var known = loadIndex();
        var retired = loadRetiredIds();
        for (var attempts = 0; attempts < 1000; attempts++) {
            var candidate = ids.get();
            if (!known.containsKey(candidate.value()) && !retired.contains(candidate.value())
                    && !files.contains(candidate.value())) return candidate;
        }
        throw new IllegalStateException("Document identity supplier did not produce a free identity.");
    }

    private ScholarDocumentDescriptor recoveredDescriptor(ScholarDocumentId id) {
        return new ScholarDocumentDescriptor(id, id.value(), 0, 0, new DocumentPreview(id.value(), "Preview unavailable", 0));
    }

    private Map<String, ScholarDocumentDescriptor> loadIndex() {
        var result = new LinkedHashMap<String, ScholarDocumentDescriptor>();
        try {
            if (!Files.isRegularFile(indexPath) || Files.size(indexPath) > MAX_INDEX_BYTES) return result;
            var root = JsonParser.parseString(Files.readString(indexPath, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!INDEX_FORMAT.equals(root.get("format").getAsString()) || root.get("version").getAsInt() != INDEX_VERSION) return result;
            for (var value : root.getAsJsonArray("documents")) {
                try {
                    var item = value.getAsJsonObject();
                    var id = new ScholarDocumentId(item.get("id").getAsString());
                    var preview = new DocumentPreview(item.get("previewTitle").getAsString(), item.get("previewExcerpt").getAsString(), item.get("blockCount").getAsInt());
                    result.put(id.value(), new ScholarDocumentDescriptor(id, item.get("name").getAsString(),
                            item.get("created").getAsLong(), item.get("modified").getAsLong(), preview));
                } catch (RuntimeException ignored) { }
            }
        } catch (IOException | RuntimeException ignored) { }
        return result;
    }

    private Set<String> loadRetiredIds() {
        var result = new HashSet<String>();
        try {
            if (!Files.isRegularFile(indexPath) || Files.size(indexPath) > MAX_INDEX_BYTES) return result;
            var json = JsonParser.parseString(Files.readString(indexPath, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!INDEX_FORMAT.equals(json.get("format").getAsString()) || json.get("version").getAsInt() != INDEX_VERSION) return result;
            var retired = json.getAsJsonArray("retiredIds");
            if (retired != null) for (var value : retired) {
                var id = value.getAsString();
                if (FileDocumentStorage.validName(id)) result.add(id);
            }
        } catch (IOException | RuntimeException ignored) { }
        return result;
    }

    private PersistenceResult<Boolean> saveIndex(Map<String, ScholarDocumentDescriptor> records) {
        return saveIndex(records, loadRetiredIds());
    }

    private PersistenceResult<Boolean> saveIndex(Map<String, ScholarDocumentDescriptor> records, Set<String> retiredIds) {
        Path temporary = null;
        try {
            Files.createDirectories(root);
            var json = new JsonObject(); json.addProperty("format", INDEX_FORMAT); json.addProperty("version", INDEX_VERSION);
            var documents = new JsonArray();
            records.values().stream().sorted(Comparator.comparing(d -> d.id().value())).forEach(descriptor -> {
                var item = new JsonObject();
                item.addProperty("id", descriptor.id().value()); item.addProperty("name", descriptor.displayName());
                item.addProperty("created", descriptor.createdAtEpochMillis()); item.addProperty("modified", descriptor.modifiedAtEpochMillis());
                item.addProperty("previewTitle", descriptor.preview().title()); item.addProperty("previewExcerpt", descriptor.preview().excerpt());
                item.addProperty("blockCount", descriptor.preview().blockCount()); documents.add(item);
            });
            json.add("documents", documents);
            var retired = new JsonArray();
            retiredIds.stream().sorted().forEach(retired::add);
            json.add("retiredIds", retired);
            temporary = Files.createTempFile(root, ".workspace-", ".tmp");
            Files.writeString(temporary, JSON.toJson(json), StandardCharsets.UTF_8);
            try { Files.move(temporary, indexPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temporary, indexPath, StandardCopyOption.REPLACE_EXISTING); }
            return new PersistenceResult.Success<>(true, List.of());
        } catch (IOException | SecurityException e) {
            return PersistenceResult.failure(PersistenceDiagnostic.Code.IO_FAILURE, "workspace", "Workspace metadata could not be saved.");
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private static <T> PersistenceResult<T> invalidName() {
        return PersistenceResult.failure(PersistenceDiagnostic.Code.INVALID_NAME, "name", "Use a non-empty document name of at most 64 characters.");
    }

    private static List<PersistenceDiagnostic> metadataWarnings(PersistenceResult<Boolean> metadata) {
        if (metadata instanceof PersistenceResult.Success<Boolean>) return List.of();
        return List.of(new PersistenceDiagnostic(PersistenceDiagnostic.Severity.WARNING, PersistenceDiagnostic.Code.IO_FAILURE,
                "workspace", "Document saved, but workspace metadata could not be updated; it will remain recoverable."));
    }
}
