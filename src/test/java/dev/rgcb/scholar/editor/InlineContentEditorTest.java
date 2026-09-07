package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InlineContentEditorTest {
    @Test
    void splitsEmptyContentAtOffsetZero() {
        var split = InlineContentEditor.split(inline(), 0);

        assertTrue(split.left().nodes().isEmpty());
        assertTrue(split.right().nodes().isEmpty());
    }

    @Test
    void splitsOneTextAtStartMiddleAndEndWithoutEmptyTextNodes() {
        var content = inline(text("hello world"));
        var middle = InlineContentEditor.split(content, 5);

        assertEquals(List.of(), InlineContentEditor.split(content, 0).left().nodes());
        assertEquals(text("hello"), middle.left().nodes().get(0));
        assertEquals(text(" world"), middle.right().nodes().get(0));
        assertEquals(List.of(), InlineContentEditor.split(content, 11).right().nodes());
        assertNoEmptyTextNodes(middle.left());
        assertNoEmptyTextNodes(middle.right());
    }

    @Test
    void splitBetweenNodesPreservesExistingNodesAndMarks() {
        var first = text("a", TextMark.BOLD);
        var second = text("b", TextMark.BOLD);
        var third = text("c", TextMark.ITALIC);
        var content = inline(first, second, third);

        var split = InlineContentEditor.split(content, 2);

        assertEquals(List.of(first, second), split.left().nodes());
        assertEquals(List.of(third), split.right().nodes());
    }

    @Test
    void splitInsideMarkedTextPreservesMarksAndUserCharacterBoundaries() {
        var content = inline(text("café Δx θ λ e\u0301", TextMark.BOLD, TextMark.ITALIC));

        var split = InlineContentEditor.split(content, 12);

        assertEquals(text("café Δx θ λ ", TextMark.BOLD, TextMark.ITALIC), split.left().nodes().get(0));
        assertEquals(text("e\u0301", TextMark.BOLD, TextMark.ITALIC), split.right().nodes().get(0));
    }

    @Test
    void splitRejectsInvalidOffsets() {
        var content = inline(text("abc"));

        assertThrows(IllegalArgumentException.class, () -> InlineContentEditor.split(content, -1));
        assertThrows(IllegalArgumentException.class, () -> InlineContentEditor.split(content, 4));
    }

    @Test
    void splitLeavesOldInlineContentUnchanged() {
        var content = inline(text("abc"));

        InlineContentEditor.split(content, 1);

        assertEquals(List.of(text("abc")), content.nodes());
    }

    @Test
    void concatDoesNotNormalizeAdjacentTextNodes() {
        var left = inline(text("a", TextMark.BOLD));
        var right = inline(text("b", TextMark.BOLD));

        var concatenated = InlineContentEditor.concat(left, right);

        assertEquals(List.of(text("a", TextMark.BOLD), text("b", TextMark.BOLD)), concatenated.nodes());
    }

    @Test
    void concatSupportsEmptySides() {
        assertEquals(List.of(), InlineContentEditor.concat(inline(), inline()).nodes());
        assertEquals(List.of(text("a")), InlineContentEditor.concat(inline(), inline(text("a"))).nodes());
        assertEquals(List.of(text("a")), InlineContentEditor.concat(inline(text("a")), inline()).nodes());
    }

    private static InlineContent inline(Text... text) {
        return new InlineContent(List.of(text).stream().map(InlineNode.class::cast).toList());
    }

    private static Text text(String content, TextMark... marks) {
        return new Text(content, Set.of(marks));
    }

    private static void assertNoEmptyTextNodes(InlineContent content) {
        assertTrue(content.nodes().stream()
                .map(Text.class::cast)
                .noneMatch(text -> text.content().isEmpty()));
    }
}
