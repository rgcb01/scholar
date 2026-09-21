package dev.rgcb.scholar.application;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import java.util.List;

public final class ScholarDocuments {
    private ScholarDocuments() { }

    public static Document blank() {
        return dev.rgcb.scholar.document.DocumentTemplates.create(dev.rgcb.scholar.document.DocumentTemplateId.BLANK);
    }

    public static Document fromTemplate(dev.rgcb.scholar.document.DocumentTemplateId template) {
        return dev.rgcb.scholar.document.DocumentTemplates.create(template);
    }
}
