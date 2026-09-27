package dev.rgcb.scholar.application;

import dev.rgcb.scholar.document.DocumentTemplateId;
import dev.rgcb.scholar.document.DocumentTemplates;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Ordered catalog of the built-in templates exposed by production Home. */
public final class DocumentTemplateCatalog {
    public static final DocumentTemplateKey BLANK = new DocumentTemplateKey("blank");
    public static final DocumentTemplateKey IEEE_STYLE = new DocumentTemplateKey("ieee-style");
    public static final DocumentTemplateKey READABILITY_SAMPLE = new DocumentTemplateKey("readability-sample");

    private static final List<DocumentTemplateDescriptor> TEMPLATES = List.of(
            new DocumentTemplateDescriptor(
                    BLANK,
                    "scholar.template.blank.title",
                    "scholar.template.blank.description",
                    DocumentTemplatePreview.SINGLE_COLUMN,
                    "Untitled",
                    () -> DocumentTemplates.create(DocumentTemplateId.BLANK)),
            new DocumentTemplateDescriptor(
                    IEEE_STYLE,
                    "scholar.template.ieee.title",
                    "scholar.template.ieee.description",
                    DocumentTemplatePreview.TWO_COLUMN,
                    "Untitled",
                    () -> DocumentTemplates.create(DocumentTemplateId.IEEE_STYLE)),
            new DocumentTemplateDescriptor(
                    READABILITY_SAMPLE,
                    "scholar.template.readability.title",
                    "scholar.template.readability.description",
                    DocumentTemplatePreview.TWO_COLUMN,
                    "M34",
                    M34ReadabilityDocument::create));
    private static final Map<DocumentTemplateKey, DocumentTemplateDescriptor> BY_ID = index(TEMPLATES);

    private DocumentTemplateCatalog() {
    }

    public static List<DocumentTemplateDescriptor> templates() {
        return TEMPLATES;
    }

    public static Optional<DocumentTemplateDescriptor> find(DocumentTemplateKey id) {
        return Optional.ofNullable(BY_ID.get(Objects.requireNonNull(id, "id")));
    }

    public static DocumentTemplateDescriptor require(DocumentTemplateKey id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown document template: " + id.value()));
    }

    public static DocumentTemplateKey keyFor(DocumentTemplateId id) {
        return switch (Objects.requireNonNull(id, "id")) {
            case BLANK -> BLANK;
            case IEEE_STYLE -> IEEE_STYLE;
        };
    }

    private static Map<DocumentTemplateKey, DocumentTemplateDescriptor> index(
            List<DocumentTemplateDescriptor> templates
    ) {
        var result = new LinkedHashMap<DocumentTemplateKey, DocumentTemplateDescriptor>();
        for (var template : templates) {
            if (result.put(template.id(), template) != null) {
                throw new IllegalStateException("Duplicate document template ID: " + template.id().value());
            }
        }
        return Map.copyOf(result);
    }
}
