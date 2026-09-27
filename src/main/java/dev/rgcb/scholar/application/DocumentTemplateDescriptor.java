package dev.rgcb.scholar.application;

import dev.rgcb.scholar.document.Document;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

/** Immutable metadata and factory for one built-in production template. */
public record DocumentTemplateDescriptor(
        DocumentTemplateKey id,
        String titleKey,
        String descriptionKey,
        DocumentTemplatePreview preview,
        String initialDocumentName,
        Supplier<Document> factory
) {
    public DocumentTemplateDescriptor {
        Objects.requireNonNull(id, "id");
        requireTranslationKey(titleKey, "titleKey");
        requireTranslationKey(descriptionKey, "descriptionKey");
        Objects.requireNonNull(preview, "preview");
        initialDocumentName = ScholarDocumentNames.normalize(initialDocumentName);
        Objects.requireNonNull(factory, "factory");
    }

    public Document createDocument() {
        return Objects.requireNonNull(factory.get(), "template factory result");
    }

    public String uniqueDocumentName(Collection<String> existingNames) {
        return ScholarDocumentNames.unique(initialDocumentName, existingNames);
    }

    private static void requireTranslationKey(String key, String field) {
        Objects.requireNonNull(key, field);
        if (!key.matches("scholar\\.template\\.[a-z0-9_]+\\.(?:title|description)")) {
            throw new IllegalArgumentException(field + " is not a template translation key.");
        }
    }
}
