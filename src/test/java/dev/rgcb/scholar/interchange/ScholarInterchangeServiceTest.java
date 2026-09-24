package dev.rgcb.scholar.interchange;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.editor.EditorSession;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ScholarInterchangeServiceTest {
    @TempDir Path directory;

    @Test void confirmedImportIsOneUndoableTransactionAndFailureDoesNotMutate() throws Exception {
        var service = new ScholarInterchangeService();
        var session = new EditorSession(new Document(List.of(new Paragraph(new InlineContent(List.of())))), 0);
        var invalid = directory.resolve("bad.csv");
        Files.writeString(invalid, "A,B\n1\n");
        assertThrows(IllegalArgumentException.class, () -> service.previewCsv(invalid));
        assertTrue(session.current().document().datasets().isEmpty());
        var source = directory.resolve("data.csv");
        Files.writeString(source, "Time [s],Distance [m]\n0,0\n1,4.9\n");
        var preview = service.previewCsv(source);
        assertTrue(session.current().document().datasets().isEmpty());
        assertTrue(service.importCsv(session, preview, "Free fall"));
        var id = session.current().document().datasets().getFirst().id();
        assertTrue(session.undo());
        assertTrue(session.current().document().datasets().isEmpty());
        assertTrue(session.redo());
        assertEquals(id, session.current().document().datasets().getFirst().id());
        assertFalse(session.undo() && session.undo());
    }

    @Test void safeExportNeverOverwritesWithoutConfirmationAndKeepsOldBytesOnFailure() throws Exception {
        var file = directory.resolve("study.md");
        Files.writeString(file, "original");
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> InterchangeFiles.writeUtf8(file, "replacement", false));
        assertEquals("original", Files.readString(file));
        InterchangeFiles.writeUtf8(file, "replacement", true);
        assertEquals("replacement", Files.readString(file));
    }

    @Test void oversizedOrMalformedUtf8CsvFailsBeforePreviewCanMutateDocument() throws Exception {
        var service = new ScholarInterchangeService();
        var oversized = directory.resolve("oversized.csv");
        try (var file = new java.io.RandomAccessFile(oversized.toFile(), "rw")) {
            file.setLength((long) InterchangeFiles.MAX_IMPORT_BYTES + 1);
        }
        var failure = assertThrows(java.io.IOException.class, () -> service.previewCsv(oversized));
        assertTrue(failure.getMessage().contains("16 MiB"));

        var malformed = directory.resolve("malformed.csv");
        Files.write(malformed, new byte[] {(byte) 0xc3, 0x28});
        assertThrows(java.nio.charset.CharacterCodingException.class, () -> service.previewCsv(malformed));
    }
}
