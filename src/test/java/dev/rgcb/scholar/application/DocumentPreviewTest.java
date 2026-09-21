package dev.rgcb.scholar.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.rgcb.scholar.document.Document;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentPreviewTest {
    @Test void emptyDocumentUsesStableSemanticFallbackForHomeThumbnail() {
        var preview = DocumentPreview.from(new Document(List.of()), "Untitled");

        assertEquals("Untitled", preview.title());
        assertEquals("Empty document", preview.excerpt());
        assertEquals(0, preview.blockCount());
    }
}
