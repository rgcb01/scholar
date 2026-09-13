package dev.rgcb.scholar.document;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DocumentPlainTextSerializerTest {
    private final DocumentPlainTextSerializer serializer = new DocumentPlainTextSerializer();

    @Test
    void serializesHeadingsWithDerivedSectionNumbers() {
        var document = document(
                heading("motion", 1, "Motion"),
                heading("average", 2, "Average velocity"));

        assertEquals("""
                1 Motion

                1.1 Average velocity
                """.trim(), serializer.serialize(document).trim());
    }

    @Test
    void serializesSkippedHeadingLevelsWithZeroPlaceholders() {
        var document = document(
                heading("motion", 1, "Motion"),
                heading("calibration", 3, "Calibration"));

        assertEquals("1.0.1 Calibration", serializer.serializeBlock(document, 1, document.blocks().get(1)));
    }

    @Test
    void serializesTableOfContentsFromCurrentHeadings() {
        var document = document(
                new TableOfContentsBlock(),
                heading("motion", 1, "Motion"),
                heading("average", 2, "Average velocity"));

        assertEquals("""
                Contents
                1 Motion
                  1.1 Average velocity
                """.trim(), serializer.serializeBlock(document, 0, document.blocks().get(0)).trim());
    }

    @Test
    void serializesCrossReferencesUsingHierarchicalSectionLabels() {
        var document = document(
                heading("motion", 1, "Motion"),
                heading("average", 2, "Average velocity"),
                paragraph(new Text("See ", Set.of()), new CrossReference(CrossReferenceTargetKind.SECTION, "average")));

        assertEquals("See Section 1.1", serializer.serializeBlock(document, 2, document.blocks().get(2)));
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Heading heading(String id, int level, String text) {
        return new Heading(id, level, inline(text));
    }

    private static Paragraph paragraph(InlineNode... nodes) {
        return new Paragraph(new InlineContent(List.of(nodes)));
    }

    private static InlineContent inline(String text) {
        return new InlineContent(List.of(new Text(text, Set.of())));
    }
}
