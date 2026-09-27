package dev.rgcb.scholar.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ScholarHomeTemplateCatalogBoundaryTest {
    @Test
    void homeRendersAndCreatesFromCatalogWithoutPerTemplateBranches() throws Exception {
        var source = Files.readString(Path.of(
                "src/main/java/dev/rgcb/scholar/client/screen/ScholarHomeScreen.java"));

        assertTrue(source.contains("DocumentTemplateCatalog.templates()"));
        assertTrue(source.contains("TEMPLATES.get(index).id()"));
        assertTrue(source.contains("template.titleKey()"));
        assertTrue(source.contains("template.descriptionKey()"));
        assertFalse(source.contains("DocumentTemplateId.BLANK"));
        assertFalse(source.contains("DocumentTemplateId.IEEE_STYLE"));
        assertFalse(source.contains("blankTemplateCard"));
        assertFalse(source.contains("ieeeTemplateCard"));
        assertFalse(source.contains("m34TemplateCard"));
    }
}
