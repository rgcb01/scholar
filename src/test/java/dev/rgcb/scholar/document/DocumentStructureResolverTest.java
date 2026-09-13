package dev.rgcb.scholar.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DocumentStructureResolverTest {
    private final DocumentStructureResolver resolver = new DocumentStructureResolver();

    @Test
    void derivesSingleTopLevelSectionNumber() {
        var structure = resolver.resolve(document(heading("motion", 1, "Motion")));

        assertEquals(List.of("1"), numbers(structure));
        assertEquals("1 Motion", structure.sections().get(0).displayText());
    }

    @Test
    void derivesNestedSectionNumbersFromHeadingLevels() {
        var structure = resolver.resolve(document(
                heading("methods", 1, "Methods"),
                heading("setup", 2, "Setup"),
                heading("calibration", 3, "Calibration")));

        assertEquals(List.of("1", "1.1", "1.1.1"), numbers(structure));
    }

    @Test
    void incrementsSiblingCountersAndResetsDeeperCounters() {
        var structure = resolver.resolve(document(
                heading("a", 1, "A"),
                heading("a1", 2, "A1"),
                heading("a2", 2, "A2"),
                heading("a2a", 3, "A2a"),
                heading("b", 1, "B"),
                heading("b1", 2, "B1")));

        assertEquals(List.of("1", "1.1", "1.2", "1.2.1", "2", "2.1"), numbers(structure));
    }

    @Test
    void skippedLevelsUseExplicitZeroPlaceholders() {
        var structure = resolver.resolve(document(
                heading("methods", 1, "Methods"),
                heading("calibration", 3, "Calibration")));

        assertEquals(List.of("1", "1.0.1"), numbers(structure));
    }

    @Test
    void topLevelSkippedHeadingAlsoUsesZeroPlaceholder() {
        var structure = resolver.resolve(document(heading("setup", 2, "Setup")));

        assertEquals(List.of("0.1"), numbers(structure));
    }

    @Test
    void ignoresNonHeadingBlocksButKeepsHeadingBlockIndex() {
        var structure = resolver.resolve(document(
                paragraph("Intro"),
                heading("methods", 1, "Methods"),
                paragraph("Body"),
                heading("setup", 2, "Setup")));

        assertEquals(1, structure.sections().get(0).blockIndex());
        assertEquals(3, structure.sections().get(1).blockIndex());
    }

    @Test
    void stableHeadingIdsRemainIndependentOfTextAndNumber() {
        var before = resolver.resolve(document(heading("target", 2, "Old title")));
        var after = resolver.resolve(document(
                heading("intro", 1, "Intro"),
                heading("target", 2, "New title")));

        assertEquals("target", before.sections().get(0).id().orElseThrow());
        assertEquals("target", after.sections().get(1).id().orElseThrow());
        assertEquals("0.1", before.sections().get(0).number().displayText());
        assertEquals("1.1", after.sections().get(1).number().displayText());
    }

    @Test
    void sectionLookupByIdUsesStableId() {
        var structure = resolver.resolve(document(heading("target", 1, "Target")));

        assertEquals("Target", structure.sectionById("target").orElseThrow().title());
        assertTrue(structure.sectionById("missing").isEmpty());
    }

    @Test
    void sectionLookupByBlockIndexFindsHeadingEntry() {
        var structure = resolver.resolve(document(paragraph("Intro"), heading("target", 1, "Target")));

        assertEquals("1", structure.sectionAtBlock(1).orElseThrow().number().displayText());
        assertTrue(structure.sectionAtBlock(0).isEmpty());
    }

    @Test
    void longStructureIsDeterministic() {
        var structure = resolver.resolve(document(
                heading("h1", 1, "One"),
                heading("h2", 2, "Two"),
                heading("h3", 3, "Three"),
                heading("h4", 4, "Four"),
                heading("h5", 5, "Five"),
                heading("h6", 6, "Six"),
                heading("h2b", 2, "Two B")));

        assertEquals(List.of("1", "1.1", "1.1.1", "1.1.1.1", "1.1.1.1.1", "1.1.1.1.1.1", "1.2"), numbers(structure));
    }

    private static List<String> numbers(DocumentStructure structure) {
        return structure.sections().stream()
                .map(section -> section.number().displayText())
                .toList();
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Heading heading(String id, int level, String text) {
        return new Heading(id, level, inline(text));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(inline(text));
    }

    private static InlineContent inline(String text) {
        return new InlineContent(List.of(new Text(text, Set.of())));
    }
}
