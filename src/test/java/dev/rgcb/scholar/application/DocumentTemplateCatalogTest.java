package dev.rgcb.scholar.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import dev.rgcb.scholar.document.ColumnLayout;
import dev.rgcb.scholar.document.DocumentTemplateId;
import dev.rgcb.scholar.document.LayoutSectionBreak;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.SemanticStyle;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentTemplateCatalogTest {
    private static final Path LANG = Path.of("src/main/resources/assets/scholar/lang");

    @Test
    void catalogHasStableUniqueIdsAndDeterministicOrder() {
        var templates = DocumentTemplateCatalog.templates();

        assertEquals(List.of("blank", "ieee-style", "readability-sample"),
                templates.stream().map(template -> template.id().value()).toList());
        assertEquals(templates.size(), new HashSet<>(templates.stream().map(DocumentTemplateDescriptor::id).toList()).size());
        assertEquals(templates, DocumentTemplateCatalog.templates());
        assertThrows(UnsupportedOperationException.class, () -> templates.add(templates.getFirst()));
        assertThrows(IllegalArgumentException.class, () -> new DocumentTemplateKey("Visible Name"));
    }

    @Test
    void everyDescriptorTranslationExistsInBothSupportedLocales() throws Exception {
        var english = translations("en_us.json");
        var spanish = translations("es_mx.json");

        for (var template : DocumentTemplateCatalog.templates()) {
            assertTrue(english.contains(template.titleKey()), template.titleKey());
            assertTrue(english.contains(template.descriptionKey()), template.descriptionKey());
            assertTrue(spanish.contains(template.titleKey()), template.titleKey());
            assertTrue(spanish.contains(template.descriptionKey()), template.descriptionKey());
        }
    }

    @Test
    void blankAndIeeeFactoriesPreserveExistingTemplateSemantics() {
        var blank = DocumentTemplateCatalog.require(DocumentTemplateCatalog.BLANK).createDocument();
        assertEquals(DocumentTemplateId.BLANK, blank.settings().template());
        assertEquals(1, blank.blocks().size());
        assertTrue(blank.blocks().getFirst() instanceof Paragraph);

        var ieee = DocumentTemplateCatalog.require(DocumentTemplateCatalog.IEEE_STYLE).createDocument();
        assertEquals(DocumentTemplateId.IEEE_STYLE, ieee.settings().template());
        assertEquals(1, ieee.settings().columns().count());
        assertTrue(ieee.blocks().contains(new LayoutSectionBreak(ColumnLayout.two())));
        assertTrue(ieee.blocks().stream().anyMatch(block -> block instanceof Paragraph paragraph
                && paragraph.style() == SemanticStyle.ABSTRACT));
    }

    @Test
    void readabilityFactoryIsDiscoverableAndPreservesProductionSample() {
        var descriptor = DocumentTemplateCatalog.require(DocumentTemplateCatalog.READABILITY_SAMPLE);
        var document = descriptor.createDocument();

        assertEquals(ScholarDocuments.m34Readability(), document);
        assertEquals(DocumentTemplatePreview.TWO_COLUMN, descriptor.preview());
        assertTrue(document.blocks().size() > 10);
    }

    @Test
    void factoriesAreValidDeterministicAndDoNotShareDocumentInstances() {
        for (var template : DocumentTemplateCatalog.templates()) {
            var first = template.createDocument();
            var second = template.createDocument();
            assertNotSame(first, second);
            assertEquals(first, second);
            assertTrue(DocumentValidator.validate(first).isValid(), template.id().value());
        }
    }

    @Test
    void templateNamingPolicyPreservesExistingNamesWithoutSharedState() {
        var blank = DocumentTemplateCatalog.require(DocumentTemplateCatalog.BLANK);
        var sample = DocumentTemplateCatalog.require(DocumentTemplateCatalog.READABILITY_SAMPLE);

        assertEquals("Untitled", blank.uniqueDocumentName(List.of()));
        assertEquals("Untitled 3", blank.uniqueDocumentName(List.of("Untitled", "Untitled 2")));
        assertEquals("M34 2", sample.uniqueDocumentName(List.of("M34")));
        assertFalse(blank.initialDocumentName().isBlank());
    }

    private static java.util.Set<String> translations(String file) throws Exception {
        var object = JsonParser.parseString(Files.readString(LANG.resolve(file))).getAsJsonObject();
        return object.keySet();
    }
}
