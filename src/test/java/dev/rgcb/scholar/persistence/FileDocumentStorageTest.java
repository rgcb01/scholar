package dev.rgcb.scholar.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static dev.rgcb.scholar.persistence.DocumentJsonCodecTest.*;
import dev.rgcb.scholar.document.Document;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class FileDocumentStorageTest {
    @TempDir Path directory;
    Document first = new Document(List.of(paragraph("first")));
    Document second = new Document(List.of(paragraph("second")));

    @Test void saveOverwriteLoadListAndRecreateStorage() throws IOException {
        var storage = new FileDocumentStorage(directory);
        assertEquals(List.of(), success(storage.list()));
        assertEquals("alpha", success(storage.save("alpha", first)));
        success(storage.save("beta", second));
        assertEquals(List.of("alpha", "beta"), success(storage.list()));
        assertEquals(first, success(storage.load("alpha")));
        success(storage.save("alpha", second));
        assertEquals(second, success(new FileDocumentStorage(directory).load("alpha")));
        try (var paths = Files.list(directory)) { assertEquals(2, paths.count()); }
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"", "../outside", "..", ".", "a/b", "a\\b", "C:\\absolute", "/absolute", "a.json", " name", "name ", "CON", "aux", "LPT1", "a:b", "a?b"})
    void unsafeNamesNeverReachFilesystem(String name) throws IOException {
        var storage = new FileDocumentStorage(directory);
        assertInstanceOf(PersistenceResult.Failure.class, storage.save(name, first));
        assertInstanceOf(PersistenceResult.Failure.class, storage.load(name));
        try (var paths = Files.list(directory)) { assertEquals(0, paths.count()); }
    }

    @Test void writeFailureRetainsPreviousFileAndCleansTemporary() throws IOException {
        var storage = new FileDocumentStorage(directory);
        success(storage.save("safe", first));
        var before = Files.readString(directory.resolve("safe.scholar.json"));
        var failing = new FileDocumentStorage(directory) {
            @Override protected void writeTemporary(Path path, byte[] bytes) throws IOException {
                Files.writeString(path, "incomplete"); throw new IOException("injected write failure");
            }
        };
        assertInstanceOf(PersistenceResult.Failure.class, failing.save("safe", second));
        assertEquals(before, Files.readString(directory.resolve("safe.scholar.json")));
        assertEquals(first, success(storage.load("safe")));
        try (var paths = Files.list(directory)) { assertEquals(1, paths.count()); }
    }

    @Test void replacementFailureNeverTruncatesGoodFile() {
        var storage = new FileDocumentStorage(directory);
        success(storage.save("safe", first));
        var failing = new FileDocumentStorage(directory) {
            @Override protected void replace(Path temporary, Path target) throws IOException { throw new IOException("injected move failure"); }
        };
        assertInstanceOf(PersistenceResult.Failure.class, failing.save("safe", second));
        assertEquals(first, success(storage.load("safe")));
    }

    @Test void atomicMoveUnavailableUsesClosedCompleteFileReplacement() {
        var storage = new FileDocumentStorage(directory);
        success(storage.save("safe", first));
        var fallback = new FileDocumentStorage(directory) {
            @Override protected void atomicReplace(Path temporary, Path target) throws IOException {
                assertEquals(first, success(new FileDocumentStorage(directory).load("safe")));
                assertEquals(second, success(new DocumentJsonCodec().decode(Files.readString(temporary))));
                throw new java.nio.file.AtomicMoveNotSupportedException(temporary.toString(), target.toString(), "injected");
            }
        };
        success(fallback.save("safe", second));
        assertEquals(second, success(storage.load("safe")));
    }

    @Test void invalidSemanticSaveNeverOverwrites() {
        var storage = new FileDocumentStorage(directory);
        success(storage.save("safe", first));
        var h = new dev.rgcb.scholar.document.Heading("h", 1, paragraph("").content());
        assertInstanceOf(PersistenceResult.Failure.class, storage.save("safe", new Document(List.of(h, h))));
        assertEquals(first, success(storage.load("safe")));
    }

    @Test void missingMalformedUnreadableAndInvalidUtf8ReturnFailures() throws IOException {
        var storage = new FileDocumentStorage(directory);
        assertInstanceOf(PersistenceResult.Failure.class, storage.load("missing"));
        Files.writeString(directory.resolve("bad.scholar.json"), "{");
        assertInstanceOf(PersistenceResult.Failure.class, storage.load("bad"));
        Files.createDirectory(directory.resolve("folder.scholar.json"));
        assertInstanceOf(PersistenceResult.Failure.class, storage.load("folder"));
        Files.write(directory.resolve("utf8.scholar.json"), new byte[] {(byte) 0xc3, 0x28});
        assertInstanceOf(PersistenceResult.Failure.class, storage.load("utf8"));
    }

    @Test void nonDirectoryStorageReturnsIoFailure() throws IOException {
        var file = directory.resolve("not-directory"); Files.writeString(file, "file");
        var storage = new FileDocumentStorage(file);
        assertInstanceOf(PersistenceResult.Failure.class, storage.save("doc", first));
        assertInstanceOf(PersistenceResult.Failure.class, storage.load("doc"));
        assertInstanceOf(PersistenceResult.Failure.class, storage.list());
    }

    @Test void listOnlySupportedSafeRegularDocumentNames() throws IOException {
        var storage = new FileDocumentStorage(directory);
        success(storage.save("good", first));
        Files.writeString(directory.resolve("ignored.txt"), "text");
        Files.writeString(directory.resolve(".scholar.json"), "text");
        Files.createDirectory(directory.resolve("directory.scholar.json"));
        assertEquals(List.of("good"), success(storage.list()));
    }

    @Test void utf8SaveKeepsUnicodeAndUnpairedUtf16TextWithoutReplacement() {
        var doc = new Document(List.of(paragraph("\u03bb\ud83d\ude80 high \ud800 low \udc00")));
        var storage = new FileDocumentStorage(directory);
        success(storage.save("unicode", doc));
        assertEquals(doc, success(storage.load("unicode")));
    }

    @Test void fullScientificGoldenSurvivesFileSaveAndNewStorageInstance() {
        var source = dev.rgcb.scholar.client.DevelopmentDocument.createPersistenceFixture();
        success(new FileDocumentStorage(directory).save("golden", source));
        assertEquals(source, success(new FileDocumentStorage(directory).load("golden")));
    }
}
