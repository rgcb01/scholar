package dev.rgcb.scholar.application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.rgcb.scholar.persistence.DocumentJsonCodec;
import dev.rgcb.scholar.persistence.PersistenceDiagnostic;
import dev.rgcb.scholar.persistence.PersistenceResult;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Versioned, atomic file storage for recovery envelopes. */
public class FileDocumentRecoveryService implements DocumentRecoveryService {
    static final String FORMAT = "scholar-recovery";
    static final int VERSION = 1;
    static final String EXTENSION = ".recovery.json";
    private static final long MAX_BYTES = (long) DocumentJsonCodec.MAX_CHARACTERS * 4 + 16_384;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
    private final Path directory;
    private final DocumentJsonCodec codec;

    public FileDocumentRecoveryService(Path directory) {
        this(directory, new DocumentJsonCodec());
    }

    FileDocumentRecoveryService(Path directory, DocumentJsonCodec codec) {
        this.directory = directory.toAbsolutePath().normalize();
        this.codec = codec;
    }

    @Override public PersistenceResult<RecoveryCandidate> capture(RecoverySnapshot snapshot) {
        var encoded = encode(snapshot);
        if (encoded instanceof PersistenceResult.Failure<String> failure) {
            return new PersistenceResult.Failure<>(failure.diagnostics());
        }
        Path temporary = null;
        try {
            ensureDirectory();
            temporary = Files.createTempFile(directory, ".recovery-", ".tmp");
            writeTemporary(temporary, ((PersistenceResult.Success<String>) encoded).value().getBytes(StandardCharsets.UTF_8));
            var verified = decode(readUtf8(temporary));
            if (verified instanceof PersistenceResult.Failure<RecoverySnapshot> failure) {
                return new PersistenceResult.Failure<>(failure.diagnostics());
            }
            replace(temporary, target(snapshot.recoveryId()));
            return new PersistenceResult.Success<>(candidate(snapshot), List.of());
        } catch (IOException | SecurityException exception) {
            return failure(PersistenceDiagnostic.Code.IO_FAILURE, "Recovery snapshot could not be saved.");
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException | SecurityException ignored) { }
        }
    }

    @Override public PersistenceResult<List<RecoveryCandidate>> discover() {
        var candidates = new ArrayList<RecoveryCandidate>();
        var diagnostics = new ArrayList<PersistenceDiagnostic>();
        try {
            ensureDirectory();
            cleanupTemporaryFiles();
            try (var paths = Files.list(directory)) {
                for (var path : paths.filter(value -> Files.isRegularFile(value, LinkOption.NOFOLLOW_LINKS))
                        .filter(value -> value.getFileName().toString().endsWith(EXTENSION)).sorted().toList()) {
                    var loaded = loadPath(path);
                    if (loaded instanceof PersistenceResult.Success<RecoverySnapshot> success) {
                        candidates.add(candidate(success.value()));
                    } else {
                        diagnostics.add(asWarning(loaded.diagnostics().getFirst(), path.getFileName().toString()));
                    }
                }
            }
        } catch (IOException | SecurityException exception) {
            return failure(PersistenceDiagnostic.Code.IO_FAILURE, "Recovery candidates could not be discovered.");
        }
        candidates.sort(Comparator.comparingLong(RecoveryCandidate::capturedAtEpochMillis).reversed()
                .thenComparing(value -> value.recoveryId().value()));
        return new PersistenceResult.Success<>(List.copyOf(candidates), List.copyOf(diagnostics));
    }

    @Override public PersistenceResult<RecoverySnapshot> load(RecoveryId id) {
        try {
            return loadPath(target(id));
        } catch (IOException | SecurityException exception) {
            return failure(PersistenceDiagnostic.Code.IO_FAILURE, "Recovery snapshot could not be loaded.");
        }
    }

    @Override public PersistenceResult<Boolean> discard(RecoveryId id) {
        try {
            Files.deleteIfExists(target(id));
            return new PersistenceResult.Success<>(true, List.of());
        } catch (IOException | SecurityException exception) {
            return failure(PersistenceDiagnostic.Code.IO_FAILURE, "Recovery snapshot could not be discarded.");
        }
    }

    @Override public PersistenceResult<Boolean> discardForDocument(ScholarDocumentId id) {
        var discovered = discover();
        if (discovered instanceof PersistenceResult.Failure<List<RecoveryCandidate>> failure) {
            return new PersistenceResult.Failure<>(failure.diagnostics());
        }
        var diagnostics = new ArrayList<>(discovered.diagnostics());
        for (var candidate : ((PersistenceResult.Success<List<RecoveryCandidate>>) discovered).value()) {
            if (candidate.documentId().filter(id::equals).isEmpty()) continue;
            var discarded = discard(candidate.recoveryId());
            if (discarded instanceof PersistenceResult.Failure<Boolean> failure) diagnostics.addAll(failure.diagnostics());
        }
        if (diagnostics.stream().anyMatch(value -> value.severity() == PersistenceDiagnostic.Severity.ERROR)) {
            return new PersistenceResult.Failure<>(diagnostics);
        }
        return new PersistenceResult.Success<>(true, diagnostics);
    }

    protected void writeTemporary(Path path, byte[] bytes) throws IOException {
        try (var channel = FileChannel.open(path, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            var data = ByteBuffer.wrap(bytes);
            while (data.hasRemaining()) channel.write(data);
            channel.force(true);
        }
    }

    protected void replace(Path temporary, Path target) throws IOException {
        try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private PersistenceResult<String> encode(RecoverySnapshot snapshot) {
        var document = codec.encode(snapshot.document());
        if (document instanceof PersistenceResult.Failure<String> failure) return failure;
        var json = new JsonObject();
        json.addProperty("format", FORMAT);
        json.addProperty("version", VERSION);
        json.addProperty("recoveryId", snapshot.recoveryId().value());
        snapshot.documentId().ifPresentOrElse(value -> json.addProperty("documentId", value.value()),
                () -> json.add("documentId", null));
        json.addProperty("originallyUntitled", snapshot.originallyUntitled());
        json.addProperty("displayName", snapshot.displayName());
        json.addProperty("capturedAt", snapshot.capturedAtEpochMillis());
        snapshot.confirmedModifiedAtEpochMillis().ifPresentOrElse(value -> json.addProperty("confirmedModifiedAt", value),
                () -> json.add("confirmedModifiedAt", null));
        json.addProperty("revision", snapshot.revision());
        json.add("document", JsonParser.parseString(((PersistenceResult.Success<String>) document).value()));
        return new PersistenceResult.Success<>(JSON.toJson(json), document.diagnostics());
    }

    private PersistenceResult<RecoverySnapshot> decode(String value) {
        try {
            var json = JsonParser.parseString(value).getAsJsonObject();
            if (!FORMAT.equals(requiredString(json, "format"))) {
                return failure(PersistenceDiagnostic.Code.WRONG_FORMAT, "File is not a Scholar recovery envelope.");
            }
            var version = json.get("version").getAsInt();
            if (version != VERSION) {
                return failure(PersistenceDiagnostic.Code.UNSUPPORTED_VERSION, "Recovery format version is not supported.");
            }
            var document = codec.decode(json.get("document").toString());
            if (document instanceof PersistenceResult.Failure<dev.rgcb.scholar.document.Document> failure) {
                return new PersistenceResult.Failure<>(failure.diagnostics());
            }
            var documentId = json.has("documentId") && !json.get("documentId").isJsonNull()
                    ? Optional.of(new ScholarDocumentId(json.get("documentId").getAsString())) : Optional.<ScholarDocumentId>empty();
            var confirmed = json.has("confirmedModifiedAt") && !json.get("confirmedModifiedAt").isJsonNull()
                    ? Optional.of(json.get("confirmedModifiedAt").getAsLong()) : Optional.<Long>empty();
            var snapshot = new RecoverySnapshot(new RecoveryId(requiredString(json, "recoveryId")), documentId,
                    json.get("originallyUntitled").getAsBoolean(), requiredString(json, "displayName"),
                    json.get("capturedAt").getAsLong(), confirmed, json.get("revision").getAsLong(),
                    ((PersistenceResult.Success<dev.rgcb.scholar.document.Document>) document).value());
            return new PersistenceResult.Success<>(snapshot, document.diagnostics());
        } catch (RuntimeException exception) {
            return failure(PersistenceDiagnostic.Code.MALFORMED_JSON, "Recovery envelope is malformed.");
        }
    }

    private PersistenceResult<RecoverySnapshot> loadPath(Path path) {
        try {
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > MAX_BYTES) {
                return failure(PersistenceDiagnostic.Code.SIZE_LIMIT, "Recovery envelope is unavailable or too large.");
            }
            return decode(readUtf8(path));
        } catch (IOException | SecurityException exception) {
            return failure(PersistenceDiagnostic.Code.IO_FAILURE, "Recovery snapshot could not be loaded.");
        }
    }

    private String readUtf8(Path path) throws IOException {
        var bytes = Files.readAllBytes(path);
        var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        return decoder.decode(ByteBuffer.wrap(bytes)).toString();
    }

    private Path target(RecoveryId id) throws IOException {
        ensureDirectory();
        var path = directory.resolve(id.value() + EXTENSION).normalize();
        if (!path.getParent().equals(directory) || Files.isSymbolicLink(path)) throw new IOException("Unsafe recovery path.");
        return path;
    }

    private void ensureDirectory() throws IOException {
        for (var current = directory; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) throw new IOException("Symbolic link in recovery directory.");
        }
        Files.createDirectories(directory);
    }

    private void cleanupTemporaryFiles() throws IOException {
        try (var paths = Files.list(directory)) {
            for (var path : paths.filter(value -> value.getFileName().toString().startsWith(".recovery-"))
                    .filter(value -> value.getFileName().toString().endsWith(".tmp")).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static RecoveryCandidate candidate(RecoverySnapshot snapshot) {
        var state = snapshot.originallyUntitled() || snapshot.documentId().isEmpty()
                ? RecoveryCandidate.SourceState.UNTITLED : RecoveryCandidate.SourceState.MISSING_DOCUMENT;
        return new RecoveryCandidate(snapshot.recoveryId(), snapshot.documentId(), snapshot.displayName(),
                snapshot.capturedAtEpochMillis(), snapshot.confirmedModifiedAtEpochMillis(), snapshot.revision(), state);
    }

    private static String requiredString(JsonObject json, String name) {
        if (!json.has(name) || json.get(name).isJsonNull()) throw new IllegalArgumentException("Missing " + name);
        return json.get(name).getAsString();
    }

    private static PersistenceDiagnostic asWarning(PersistenceDiagnostic source, String path) {
        return new PersistenceDiagnostic(PersistenceDiagnostic.Severity.WARNING, source.code(), path, source.message());
    }

    private static <T> PersistenceResult<T> failure(PersistenceDiagnostic.Code code, String message) {
        return PersistenceResult.failure(code, "recovery", message);
    }
}
