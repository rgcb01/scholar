package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlainTextClipboardTest {
    private final DocumentEditor editor = new DocumentEditor();
    private final PlainTextClipboard clipboard = new PlainTextClipboard(editor);

    @Test
    void copiesPartialTextForwardAndBackwardSelections() {
        var document = document(paragraph(text("velocity changes quickly")));

        assertEquals("city changes qui", clipboard.copy(state(document, 4, 20)).orElseThrow());
        assertEquals("city changes qui", clipboard.copy(state(document, 20, 4)).orElseThrow());
    }

    @Test
    void copiesAcrossTextNodesAndMarks() {
        var document = document(paragraph(
                text("velocity "),
                text("changes", TextMark.BOLD),
                text(" quickly", TextMark.ITALIC)));

        assertEquals("city changes qui", clipboard.copy(state(document, 4, 20)).orElseThrow());
    }

    @Test
    void copyWithNoSelectionIsNoOp() {
        var document = document(paragraph(text("abc")));

        assertTrue(clipboard.copy(state(document, 1, 1)).isEmpty());
    }

    @Test
    void cutsSelectionAndPreservesCopiedText() {
        var document = document(paragraph(text("abcdef")));

        var cut = clipboard.cut(state(document, 2, 5)).orElseThrow();

        assertEquals("cde", cut.clipboardText());
        assertEquals("abf", paragraphText(cut.editResult().document()));
        assertEquals(new DocumentPosition(0, 2), cut.editResult().caret());
    }

    @Test
    void cutWithNoSelectionIsNoOp() {
        var document = document(paragraph(text("abc")));

        assertTrue(clipboard.cut(state(document, 1, 1)).isEmpty());
    }

    @Test
    void cutsMultiBlockSelectionWithPlainTextClipboard() {
        var document = document(paragraph(text("abc")), paragraph(text("def")));

        var cut = clipboard.cut(new EditorState(document, new DocumentPosition(0, 1), new DocumentPosition(1, 2))).orElseThrow();

        assertEquals("bc\nde", cut.clipboardText());
        assertEquals("af", paragraphText(cut.editResult().document()));
        assertEquals(new DocumentPosition(0, 1), cut.editResult().caret());
    }

    @Test
    void pastesAtCaretAndOverSelection() {
        var document = document(paragraph(text("abc")));
        var atCaret = clipboard.paste(state(document, 1, 1), "X").orElseThrow();
        var overSelection = clipboard.paste(state(document, 1, 3), "Y").orElseThrow();

        assertEquals("aXbc", paragraphText(atCaret.document()));
        assertEquals(new DocumentPosition(0, 2), atCaret.caret());
        assertEquals("aY", paragraphText(overSelection.document()));
        assertEquals(new DocumentPosition(0, 2), overSelection.caret());
    }

    @Test
    void emptyClipboardPasteIsNoOpEvenWithSelection() {
        var document = document(paragraph(text("abc")));

        assertTrue(clipboard.paste(state(document, 1, 3), "").isEmpty());
        assertEquals("abc", paragraphText(document));
    }

    @Test
    void multilinePasteNormalizesLineBreaksToSpaces() {
        var document = document(paragraph(text("abc")));

        var result = clipboard.paste(state(document, 1, 1), "x\r\ny\nz\rw").orElseThrow();

        assertEquals("ax y z wbc", paragraphText(result.document()));
    }

    @Test
    void pasteUsesFormattingAffinity() {
        var document = document(paragraph(
                text("normal "),
                text("bold", TextMark.BOLD)));

        var result = clipboard.paste(state(document, 9, 9), "X").orElseThrow();

        assertEquals(Set.of(TextMark.BOLD), textAt(result.document(), 1).marks());
    }

    @Test
    void copiesAcrossTwoBlocksWithNewlineBoundary() {
        var document = document(paragraph(text("abc")), paragraph(text("def")));

        assertEquals("bc\nde", clipboard.copy(new EditorState(
                document,
                new DocumentPosition(0, 1),
                new DocumentPosition(1, 2))).orElseThrow());
    }

    @Test
    void copiesAcrossHeadingAsPlainTextWithoutMarkdownSyntax() {
        var document = document(
                paragraph(text("abc")),
                heading(2, text("TITLE")),
                paragraph(text("xyz")));

        assertEquals("c\nTITLE\nx", clipboard.copy(new EditorState(
                document,
                new DocumentPosition(0, 2),
                new DocumentPosition(2, 1))).orElseThrow());
    }

    @Test
    void emptyMiddleBlockContributesBlankClipboardLine() {
        var document = document(
                paragraph(text("A")),
                new Paragraph(new InlineContent(List.of())),
                paragraph(text("B")));

        assertEquals("A\n\nB", clipboard.copy(new EditorState(
                document,
                new DocumentPosition(0, 0),
                new DocumentPosition(2, 1))).orElseThrow());
    }

    @Test
    void boundaryOnlySelectionCopiesNewline() {
        var document = document(paragraph(text("abc")), paragraph(text("def")));

        assertEquals("\n", clipboard.copy(new EditorState(
                document,
                new DocumentPosition(0, 3),
                new DocumentPosition(1, 0))).orElseThrow());
    }

    @Test
    void multipleBoundaryOnlySpanCopiesMultipleNewlines() {
        var document = document(
                paragraph(text("A")),
                new Paragraph(new InlineContent(List.of())),
                paragraph(text("B")));

        assertEquals("\n\n", clipboard.copy(new EditorState(
                document,
                new DocumentPosition(0, 1),
                new DocumentPosition(2, 0))).orElseThrow());
    }

    @Test
    void multiBlockCopyUsesUserCharacterOffsetsForUnicode() {
        var document = document(
                paragraph(text("café Δx")),
                paragraph(text("θ λ e\u0301")));

        assertEquals("é Δx\nθ λ", clipboard.copy(new EditorState(
                document,
                new DocumentPosition(0, 3),
                new DocumentPosition(1, 3))).orElseThrow());
    }

    @Test
    void preservesUnicodeClipboardContent() {
        var document = document(paragraph(text("abc")));
        var text = "café Δx θ λ e\u0301";

        var result = clipboard.paste(state(document, 1, 1), text).orElseThrow();

        assertEquals("a" + text + "bc", paragraphText(result.document()));
        assertEquals(text, clipboard.copy(new EditorState(
                result.document(),
                new DocumentPosition(0, 1),
                new DocumentPosition(0, 1 + TextBoundary.characterCount(text)))).orElseThrow());
    }

    @Test
    void previousDocumentRemainsImmutableAfterCutAndPaste() {
        var document = document(paragraph(text("abcdef")));

        clipboard.cut(state(document, 2, 5)).orElseThrow();
        clipboard.paste(state(document, 1, 1), "X").orElseThrow();

        assertEquals("abcdef", paragraphText(document));
    }

    private static EditorState state(Document document, int anchor, int active) {
        return new EditorState(document, new DocumentPosition(0, anchor), new DocumentPosition(0, active));
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(Text... text) {
        return new Paragraph(new InlineContent(List.of(text).stream().map(InlineNode.class::cast).toList()));
    }

    private static Heading heading(int level, Text... text) {
        return new Heading(level, new InlineContent(List.of(text).stream().map(InlineNode.class::cast).toList()));
    }

    private static Text text(String content, TextMark... marks) {
        return new Text(content, Set.of(marks));
    }

    private static Text textAt(Document document, int index) {
        return (Text) ((Paragraph) document.blocks().get(0)).content().nodes().get(index);
    }

    private static String paragraphText(Document document) {
        return ((Paragraph) document.blocks().get(0)).content().nodes().stream()
                .map(Text.class::cast)
                .map(Text::content)
                .reduce("", String::concat);
    }
}
