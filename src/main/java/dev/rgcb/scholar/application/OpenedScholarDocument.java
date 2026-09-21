package dev.rgcb.scholar.application;

import dev.rgcb.scholar.document.Document;
import java.util.Objects;

public record OpenedScholarDocument(ScholarDocumentDescriptor descriptor, Document document) {
    public OpenedScholarDocument {
        descriptor = Objects.requireNonNull(descriptor, "descriptor");
        document = Objects.requireNonNull(document, "document");
    }
}
