package dev.rgcb.scholar.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import dev.rgcb.scholar.math.MathIdentifier;
import org.junit.jupiter.api.Test;

class DocumentModelTest {
    @Test
    void constructsSimpleDocumentWithOrderedBlocks() {
        var heading = new Heading(1, new InlineContent(List.of(new Text("Motion", Set.of()))));
        var paragraph = new Paragraph(new InlineContent(List.of(new Text("Second paragraph.", Set.of()))));

        var document = new Document(List.of(heading, paragraph));

        assertEquals(List.of(heading, paragraph), document.blocks());
        assertInstanceOf(Heading.class, document.blocks().get(0));
        assertInstanceOf(Paragraph.class, document.blocks().get(1));
    }

    @Test
    void rejectsHeadingLevelsOutsideOneThroughSix() {
        var content = new InlineContent(List.of(new Text("Invalid", Set.of())));

        assertThrows(IllegalArgumentException.class, () -> new Heading(0, content));
        assertThrows(IllegalArgumentException.class, () -> new Heading(7, content));
    }

    @Test
    void representsBoldAndItalicTextMarks() {
        var text = new Text("velocity", EnumSet.of(TextMark.BOLD, TextMark.ITALIC));

        assertEquals("velocity", text.content());
        assertEquals(Set.of(TextMark.BOLD, TextMark.ITALIC), text.marks());
    }

    @Test
    void defensivelyCopiesBlockAndInlineCollections() {
        var blocks = new ArrayList<BlockNode>();
        var inlineNodes = new ArrayList<InlineNode>();

        var text = new Text("Initial", Set.of());
        inlineNodes.add(text);

        var paragraph = new Paragraph(new InlineContent(inlineNodes));
        blocks.add(paragraph);

        var document = new Document(blocks);
        inlineNodes.add(new Text("Mutated", Set.of()));
        blocks.add(new Heading(1, new InlineContent(List.of(new Text("Extra", Set.of())))));

        assertEquals(1, document.blocks().size());
        assertEquals(1, paragraph.content().nodes().size());
        assertEquals(text, paragraph.content().nodes().get(0));
        assertThrows(UnsupportedOperationException.class, () -> document.blocks().add(paragraph));
        assertThrows(UnsupportedOperationException.class, () -> paragraph.content().nodes().add(text));
    }

    @Test
    void defensivelyCopiesTextMarks() {
        var marks = EnumSet.of(TextMark.BOLD);

        var text = new Text("Velocity", marks);
        marks.add(TextMark.ITALIC);

        assertEquals(Set.of(TextMark.BOLD), text.marks());
        assertThrows(UnsupportedOperationException.class, () -> text.marks().add(TextMark.ITALIC));
    }

    @Test
    void rejectsNullRequiredValuesAndCollectionElements() {
        assertThrows(NullPointerException.class, () -> new Document(null));
        assertThrows(NullPointerException.class, () -> new Document(List.of((BlockNode) null)));
        assertThrows(NullPointerException.class, () -> new InlineContent(null));
        assertThrows(NullPointerException.class, () -> new InlineContent(List.of((InlineNode) null)));
        assertThrows(NullPointerException.class, () -> new Heading(1, null));
        assertThrows(NullPointerException.class, () -> new Paragraph(null));
        assertThrows(NullPointerException.class, () -> new EquationBlock(null));
        assertThrows(NullPointerException.class, () -> new Text(null, Set.of()));
        assertThrows(NullPointerException.class, () -> new Text("content", null));
        assertThrows(NullPointerException.class, () -> new Text("content", Set.of((TextMark) null)));
    }

    @Test
    void representsMotionExampleWithoutNormalization() {
        var document = new Document(List.of(
                new Heading(1, new InlineContent(List.of(new Text("Motion", Set.of())))),
                new Paragraph(new InlineContent(List.of(
                        new Text("Velocity", Set.of(TextMark.BOLD)),
                        new Text(" describes how quickly position changes.", Set.of()))))));

        assertEquals(2, document.blocks().size());

        var heading = assertInstanceOf(Heading.class, document.blocks().get(0));
        assertEquals(1, heading.level());
        assertEquals(new Text("Motion", Set.of()), heading.content().nodes().get(0));

        var paragraph = assertInstanceOf(Paragraph.class, document.blocks().get(1));
        assertEquals(2, paragraph.content().nodes().size());
        assertEquals(new Text("Velocity", Set.of(TextMark.BOLD)), paragraph.content().nodes().get(0));
        assertEquals(new Text(" describes how quickly position changes.", Set.of()), paragraph.content().nodes().get(1));
        assertTrue(paragraph.content().nodes().get(0) instanceof Text);
    }

    @Test
    void representsEquationBlockAsOrderedBlockNode() {
        var equation = new EquationBlock(new MathIdentifier("x"));
        var document = new Document(List.of(
                new Paragraph(new InlineContent(List.of(new Text("Before.", Set.of())))),
                equation,
                new Paragraph(new InlineContent(List.of(new Text("After.", Set.of()))))));

        assertEquals(equation, document.blocks().get(1));
    }
}
