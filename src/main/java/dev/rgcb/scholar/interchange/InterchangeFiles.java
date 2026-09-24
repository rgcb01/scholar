package dev.rgcb.scholar.interchange;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/** Safe file boundary for user-initiated interchange, separate from Scholar persistence. */
public final class InterchangeFiles {
    public static final int MAX_IMPORT_BYTES = 16 * 1024 * 1024;
    private InterchangeFiles() { }

    public static Path directory(Path gameDirectory) throws IOException {
        var path = Objects.requireNonNull(gameDirectory).resolve("scholar").resolve("interchange");
        return Files.createDirectories(path);
    }

    public static String readUtf8(Path source) throws IOException {
        try (var input = Files.newInputStream(Objects.requireNonNull(source), StandardOpenOption.READ)) {
            var bytes = input.readNBytes(MAX_IMPORT_BYTES + 1);
            if (bytes.length > MAX_IMPORT_BYTES) throw new IOException("CSV import exceeds the 16 MiB V1 limit.");
            var decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        }
    }

    public static void writeUtf8(Path destination, String content, boolean overwrite) throws IOException {
        write(destination, Objects.requireNonNull(content).getBytes(StandardCharsets.UTF_8), overwrite);
    }

    public static void write(Path destination, byte[] content, boolean overwrite) throws IOException {
        Objects.requireNonNull(destination);
        Objects.requireNonNull(content);
        var parent = destination.toAbsolutePath().getParent();
        if (parent == null || !Files.isDirectory(parent)) throw new IOException("Destination directory does not exist");
        if (Files.exists(destination) && !overwrite) throw new java.nio.file.FileAlreadyExistsException(destination.toString());
        var temporary = Files.createTempFile(parent, ".scholar-export-", ".tmp");
        try {
            Files.write(temporary, content);
            if (!overwrite) Files.move(temporary, destination);
            else try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
