package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.editor.DocumentPosition;
import dev.rgcb.scholar.editor.TextSelection;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaretBlinkTest {
    private final Document document = new Document(List.of(new Paragraph(new InlineContent(List.of()))));
    private final TextSelection first = caret(0);

    @Test void followsVanillaCadenceWithoutDependingOnWallClockPhase() {
        var blink = new CaretBlink();
        assertTrue(blink.visible(1_000, true, document, first));
        assertTrue(blink.visible(1_299, true, document, first));
        assertFalse(blink.visible(1_300, true, document, first));
        assertFalse(blink.visible(1_599, true, document, first));
        assertTrue(blink.visible(1_600, true, document, first));
    }

    @Test void caretMovementEditingAndRefocusRestartVisiblePhase() {
        var blink = new CaretBlink();
        blink.visible(0, true, document, first);
        assertFalse(blink.visible(310, true, document, first));
        var moved = caret(1);
        assertTrue(blink.visible(310, true, document, moved));
        assertFalse(blink.visible(610, true, document, moved));
        var edited = new Document(document.blocks());
        assertTrue(blink.visible(610, true, edited, moved));
        assertFalse(blink.visible(910, true, edited, moved));
        assertFalse(blink.visible(920, false, edited, moved));
        assertTrue(blink.visible(930, true, edited, moved));
    }

    private static TextSelection caret(int offset) {
        var position = new DocumentPosition(0, offset);
        return new TextSelection(position, position);
    }
}
