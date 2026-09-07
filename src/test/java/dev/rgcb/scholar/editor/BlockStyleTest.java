package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlockStyleTest {
    @Test
    void createsParagraphAndHeadingStyles() {
        assertTrue(BlockStyle.paragraph().isParagraph());
        assertFalse(BlockStyle.paragraph().isHeading());
        assertEquals("Paragraph", BlockStyle.paragraph().displayName());
        assertEquals("Heading 1", BlockStyle.heading(1).displayName());
        assertEquals("Heading 6", BlockStyle.heading(6).displayName());
    }

    @Test
    void rejectsInvalidHeadingLevels() {
        assertThrows(IllegalArgumentException.class, () -> BlockStyle.heading(-1));
        assertThrows(IllegalArgumentException.class, () -> BlockStyle.heading(7));
    }

    @Test
    void hasValueSemantics() {
        assertEquals(new BlockStyle(2), BlockStyle.heading(2));
    }
}
