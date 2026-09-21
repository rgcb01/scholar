package dev.rgcb.scholar.persistence;

import dev.rgcb.scholar.document.Document;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Local UTF-8 storage. The caller supplies an application-owned directory, never a file path. */
public class FileDocumentStorage implements DocumentStorage {
    public static final String EXTENSION = ".scholar.json";
    private static final long MAX_BYTES = (long) DocumentJsonCodec.MAX_CHARACTERS * 4;
    private final Path directory;
    private final DocumentJsonCodec codec;

    public FileDocumentStorage(Path directory) { this(directory, new DocumentJsonCodec()); }
    public FileDocumentStorage(Path directory, DocumentJsonCodec codec) {
        this.directory = Objects.requireNonNull(directory).toAbsolutePath().normalize();
        this.codec = Objects.requireNonNull(codec);
    }

    public static boolean validName(String name) {
        if (name == null || !name.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")) return false;
        var upper = name.toUpperCase(Locale.ROOT);
        return !upper.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]");
    }

    @Override public PersistenceResult<String> save(String name, Document document) {
        if (!validName(name)) return badName();
        var encoded = codec.encode(document);
        if (encoded instanceof PersistenceResult.Failure<String> failure) return failure;
        var success = (PersistenceResult.Success<String>) encoded;
        Path temporary = null;
        try {
            var target = target(name);
            temporary = Files.createTempFile(directory, ".scholar-", ".tmp");
            writeTemporary(temporary, success.value().getBytes(StandardCharsets.UTF_8));
            replace(temporary, target);
            return new PersistenceResult.Success<>(name, success.diagnostics());
        } catch (IOException | SecurityException e) {
            return ioFailure("Document could not be saved.");
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException | SecurityException ignored) { }
            }
        }
    }

    protected void writeTemporary(Path path, byte[] bytes) throws IOException {
        try (var channel = FileChannel.open(path, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            var data = ByteBuffer.wrap(bytes);
            while (data.hasRemaining()) channel.write(data);
            channel.force(true);
        }
    }

    protected void replace(Path temporary, Path target) throws IOException {
        try {
            atomicReplace(temporary, target);
        } catch (AtomicMoveNotSupportedException e) {
            // Closed, flushed same-directory file; old file is not truncated. Not crash-atomic.
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    protected void atomicReplace(Path temporary, Path target) throws IOException {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override public PersistenceResult<Document> load(String name) {
        if (!validName(name)) return badName();
        try {
            var target = target(name);
            byte[] bytes;
            try (var channel = FileChannel.open(target, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                if (channel.size() > MAX_BYTES) return PersistenceResult.failure(PersistenceDiagnostic.Code.SIZE_LIMIT, "$", "File exceeds the V1 size limit.");
                var buffer = ByteBuffer.allocate((int) channel.size());
                while (buffer.hasRemaining() && channel.read(buffer) >= 0) { }
                if (channel.read(ByteBuffer.allocate(1)) > 0) return ioFailure("Document changed while reading.");
                buffer.flip(); bytes = new byte[buffer.remaining()]; buffer.get(bytes);
            }
            var utf8 = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            return codec.decode(utf8.decode(ByteBuffer.wrap(bytes)).toString());
        } catch (IOException | SecurityException e) {
            return ioFailure("Document could not be loaded.");
        }
    }

    @Override public PersistenceResult<List<String>> list() {
        try {
            ensureDirectory();
            try (var paths = Files.list(directory)) {
                var names = paths.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
                        .map(p -> p.getFileName().toString()).filter(n -> n.endsWith(EXTENSION))
                        .map(n -> n.substring(0, n.length() - EXTENSION.length())).filter(FileDocumentStorage::validName).sorted().toList();
                return new PersistenceResult.Success<>(names, List.of());
            }
        } catch (IOException | SecurityException e) {
            return ioFailure("Saved documents could not be listed.");
        }
    }

    private Path target(String name) throws IOException {
        ensureDirectory();
        var path = directory.resolve(name + EXTENSION).normalize();
        if (!path.getParent().equals(directory) || Files.isSymbolicLink(path)) throw new IOException("Unsafe document path.");
        return path;
    }
    private void ensureDirectory() throws IOException {
        // Refuse symlink ancestors, including a redirected document directory.
        for (var current = directory; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) throw new IOException("Symbolic link in document directory.");
        }
        Files.createDirectories(directory);
    }
    private static <T> PersistenceResult<T> badName() {
        return PersistenceResult.failure(PersistenceDiagnostic.Code.INVALID_NAME, "name", "Use 1-64 letters, numbers, hyphens or underscores; reserved names are not allowed.");
    }
    private static <T> PersistenceResult<T> ioFailure(String message) {
        return PersistenceResult.failure(PersistenceDiagnostic.Code.IO_FAILURE, "$", message);
    }
}
