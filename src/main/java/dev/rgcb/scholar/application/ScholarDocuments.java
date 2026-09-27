package dev.rgcb.scholar.application;

import dev.rgcb.scholar.document.Document;

public final class ScholarDocuments {
    private ScholarDocuments() { }

    public static Document blank() {
        return dev.rgcb.scholar.document.DocumentTemplates.create(dev.rgcb.scholar.document.DocumentTemplateId.BLANK);
    }

    public static Document m34Readability() { return M34ReadabilityDocument.create(); }
}
