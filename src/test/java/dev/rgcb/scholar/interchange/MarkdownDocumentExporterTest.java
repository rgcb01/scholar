package dev.rgcb.scholar.interchange;

import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.math.MathIdentifier;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownDocumentExporterTest {
    @Test void exportsSemanticContentWithoutInternalIds() {
        var document = new Document(List.of(
                new Heading("secret-heading-id", 1, new InlineContent(List.of(new Text("Study", Set.of())))),
                new Paragraph(new InlineContent(List.of(new Text("Strong", Set.of(TextMark.BOLD)),
                        new Text(" and ", Set.of()), new CrossReference(CrossReferenceTargetKind.SECTION, "secret-heading-id")))),
                new EquationBlock("private-equation-id", new MathIdentifier("x")),
                new TableOfContentsBlock()));
        var markdown = new MarkdownDocumentExporter().export(document);
        assertTrue(markdown.contains("# Study"));
        assertTrue(markdown.contains("**Strong**"));
        assertTrue(markdown.contains("Section 1"));
        assertTrue(markdown.contains("$$\nx\n$$"));
        assertTrue(markdown.contains("Contents"));
        assertFalse(markdown.contains("secret-heading-id"));
        assertFalse(markdown.contains("private-equation-id"));
    }
}
