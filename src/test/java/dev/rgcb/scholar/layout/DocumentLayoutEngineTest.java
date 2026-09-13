package dev.rgcb.scholar.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathOperatorRole;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.MathSymbol;
import dev.rgcb.scholar.math.MathSymbolKind;
import dev.rgcb.scholar.math.layout.MathTextKind;
import dev.rgcb.scholar.math.layout.MathTextMeasurer;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import dev.rgcb.scholar.typography.ScholarTypography;
import dev.rgcb.scholar.typography.TypographyRole;
import dev.rgcb.scholar.typography.TypographyRoleStyle;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DocumentLayoutEngineTest {
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void laysOutEmptyDocument() {
        var layout = layoutEngine.layout(new Document(List.of()), 40, textMeasurer);

        assertEquals(40, layout.width());
        assertEquals(0, layout.height());
        assertTrue(layout.blocks().isEmpty());
    }

    @Test
    void laysOutParagraphAsBlockWithLine() {
        var layout = layoutEngine.layout(document(paragraph(text("hello world"))), 80, textMeasurer);

        assertEquals(1, layout.blocks().size());
        var block = layout.blocks().get(0);
        assertEquals(LaidOutBlockKind.PARAGRAPH, block.kind());
        assertEquals(0, block.y());
        assertEquals(1, block.lines().size());
        assertEquals("hello", block.lines().get(0).textRuns().get(0).text());
    }

    @Test
    void emptyParagraphHasOneLogicalEmptyLine() {
        var layout = layoutEngine.layout(document(new Paragraph(new InlineContent(List.of()))), 80, textMeasurer);

        var block = layout.blocks().get(0);
        assertEquals(LaidOutBlockKind.PARAGRAPH, block.kind());
        assertEquals(80, block.width());
        assertEquals(10, block.height());
        assertEquals(1, block.lines().size());
        assertEquals(0, block.lines().get(0).width());
        assertEquals(10, block.lines().get(0).height());
        assertTrue(block.lines().get(0).textRuns().isEmpty());
    }

    @Test
    void emptyHeadingUsesHeadingLineHeight() {
        var layout = layoutEngine.layout(document(new Heading(2, new InlineContent(List.of()))), 80, textMeasurer);

        var block = layout.blocks().get(0);
        assertEquals(LaidOutBlockKind.HEADING, block.kind());
        assertEquals(2, block.headingLevel());
        assertEquals(12, block.height());
        assertEquals(1, block.lines().size());
        assertEquals(12, block.lines().get(0).height());
        assertEquals("0.1 ", block.lines().get(0).textRuns().get(0).text());
        assertEquals(-1, block.lines().get(0).textRuns().get(0).sourceBlockIndex());
        assertEquals(0, block.lines().get(0).textRuns().get(1).width());
        assertEquals(0, block.lines().get(0).textRuns().get(1).sourceStart());
        assertEquals(0, block.lines().get(0).textRuns().get(1).sourceEnd());
    }

    @Test
    void preservesHeadingAndParagraphOrdering() {
        var layout = layoutEngine.layout(document(
                heading(1, text("Motion")),
                paragraph(text("Velocity changes."))), 120, textMeasurer);

        assertEquals(2, layout.blocks().size());
        assertEquals(LaidOutBlockKind.HEADING, layout.blocks().get(0).kind());
        assertEquals(1, layout.blocks().get(0).headingLevel());
        assertEquals(LaidOutBlockKind.PARAGRAPH, layout.blocks().get(1).kind());
        assertTrue(layout.blocks().get(1).y() > layout.blocks().get(0).y());
    }

    @Test
    void headingLayoutPrefixesDerivedSectionNumberAsDisplayOnlyText() {
        var layout = layoutEngine.layout(document(
                heading("motion", 1, text("Motion")),
                heading("average", 2, text("Average velocity"))), 120, textMeasurer);

        var secondHeading = layout.blocks().get(1);
        var runs = secondHeading.lines().get(0).textRuns();

        assertEquals("1.1 ", runs.get(0).text());
        assertEquals(-1, runs.get(0).sourceBlockIndex());
        assertEquals("Average", runs.get(1).text());
        assertEquals(1, runs.get(1).sourceBlockIndex());
    }

    @Test
    void tableOfContentsBlockLaysOutDerivedEntries() {
        var layout = layoutEngine.layout(document(
                new TableOfContentsBlock(),
                heading("motion", 1, text("Motion")),
                heading("average", 2, text("Average velocity"))), 160, textMeasurer);

        var toc = layout.blocks().get(0);

        assertEquals(LaidOutBlockKind.TABLE_OF_CONTENTS, toc.kind());
        assertEquals("Contents", lineText(toc.lines().get(0)));
        assertEquals(2, toc.tableOfContents().orElseThrow().entries().size());
        assertEquals("motion", toc.tableOfContents().orElseThrow().entries().get(0).targetId());
        assertEquals(1, toc.tableOfContents().orElseThrow().entries().get(0).targetBlockIndex());
        assertEquals("average", toc.tableOfContents().orElseThrow().entries().get(1).targetId());
        assertTrue(lineText(toc.lines().get(2)).contains("1.1 Average velocity"));
    }

    @Test
    void tableOfContentsEntryHitGeometryIsDeterministic() {
        var layout = layoutEngine.layout(document(
                new TableOfContentsBlock(),
                heading("motion", 1, text("Motion"))), 160, textMeasurer);

        var entry = layout.blocks().get(0).tableOfContents().orElseThrow().entries().get(0);

        assertTrue(entry.contains(entry.x(), entry.y()));
        assertTrue(entry.contains(entry.x() + entry.width() - 1, entry.y() + entry.height() - 1));
    }

    @Test
    void wrapsAtSpacesWhenPossible() {
        var layout = layoutEngine.layout(document(paragraph(text("alpha beta gamma"))), 60, textMeasurer);

        var lines = layout.blocks().get(0).lines();
        assertEquals(2, lines.size());
        assertEquals("alpha beta", lineText(lines.get(0)));
        assertEquals("gamma", lineText(lines.get(1)));
    }

    @Test
    void splitsLongWordsAtCharacterBoundaries() {
        var layout = layoutEngine.layout(document(paragraph(text("superlongword"))), 25, textMeasurer);

        var lines = layout.blocks().get(0).lines();
        assertEquals(List.of("super", "longw", "ord"), lines.stream().map(DocumentLayoutEngineTest::lineText).toList());
    }

    @Test
    void preservesTextOrderAcrossMarkedRuns() {
        var layout = layoutEngine.layout(document(paragraph(
                text("alpha "),
                text("beta", TextMark.BOLD),
                text(" gamma", TextMark.ITALIC))), 45, textMeasurer);

        var renderedText = layout.blocks().get(0).lines().stream()
                .map(DocumentLayoutEngineTest::lineText)
                .reduce("", String::concat);
        assertEquals("alphabetagamma", renderedText);
    }

    @Test
    void preservesBoldAndItalicMarksAcrossLines() {
        var layout = layoutEngine.layout(document(paragraph(
                text("alpha "),
                text("beta", TextMark.BOLD),
                text(" gamma", TextMark.ITALIC))), 45, textMeasurer);

        var runs = layout.blocks().get(0).lines().stream()
                .flatMap(line -> line.textRuns().stream())
                .toList();

        assertTrue(runs.stream().anyMatch(run -> run.text().contains("beta")
                && run.style().marks().equals(Set.of(TextMark.BOLD))));
        assertTrue(runs.stream().anyMatch(run -> run.text().contains("gamma")
                && run.style().marks().equals(Set.of(TextMark.ITALIC))));
    }

    @Test
    void reportsTotalDocumentHeight() {
        var layout = layoutEngine.layout(document(
                heading(1, text("Motion")),
                paragraph(text("alpha beta gamma delta epsilon zeta"))), 60, textMeasurer);

        assertTrue(layout.height() > 20);
    }

    @Test
    void differentWidthsProduceDifferentLineCounts() {
        var document = document(paragraph(text("alpha beta gamma delta")));

        var narrow = layoutEngine.layout(document, 35, textMeasurer);
        var wide = layoutEngine.layout(document, 200, textMeasurer);

        assertTrue(narrow.blocks().get(0).lines().size() > wide.blocks().get(0).lines().size());
    }

    @Test
    void layoutIsDeterministicForSameInputs() {
        var document = document(
                heading(2, text("Average velocity")),
                paragraph(text("alpha beta gamma delta")));

        var first = layoutEngine.layout(document, 70, textMeasurer);
        var second = layoutEngine.layout(document, 70, textMeasurer);

        assertEquals(first, second);
    }

    @Test
    void laysOutEquationBlockBetweenParagraphs() {
        var layout = layoutEngine.layout(document(
                paragraph(text("Before.")),
                targetEquationBlock(),
                paragraph(text("After."))), 80, textMeasurer, new FixedMathTextMeasurer());

        assertEquals(3, layout.blocks().size());
        assertEquals(LaidOutBlockKind.PARAGRAPH, layout.blocks().get(0).kind());
        assertEquals(LaidOutBlockKind.EQUATION, layout.blocks().get(1).kind());
        assertEquals(LaidOutBlockKind.PARAGRAPH, layout.blocks().get(2).kind());
        assertTrue(layout.blocks().get(1).math().isPresent());
        assertTrue(layout.blocks().get(2).y() > layout.blocks().get(1).y());
    }

    @Test
    void equationContributesHeightAndIsCenteredWhenItFits() {
        var layout = layoutEngine.layout(document(targetEquationBlock()), 80, textMeasurer, new FixedMathTextMeasurer());
        var equation = layout.blocks().get(0);

        assertEquals(LaidOutBlockKind.EQUATION, equation.kind());
        assertEquals(40, equation.width());
        assertEquals(27, equation.height());
        assertEquals(20, equation.x());
        assertEquals(27, layout.height());
    }

    @Test
    void emptyEquationHasMinimumAuthoringGeometryWithoutFakeMath() {
        var layout = layoutEngine.layout(document(new EquationBlock(new MathSequence(List.of()))), 80, textMeasurer, new FixedMathTextMeasurer());
        var equation = layout.blocks().get(0);

        assertEquals(LaidOutBlockKind.EQUATION, equation.kind());
        assertEquals(48, equation.width());
        assertEquals(18, equation.height());
        assertTrue(equation.math().orElseThrow().root().primitives().isEmpty());
        assertTrue(equation.math().orElseThrow().root().children().isEmpty());
    }

    @Test
    void normalDocumentsDoNotRequireMathMeasurer() {
        var layout = layoutEngine.layout(document(paragraph(text("hello"))), 80, textMeasurer);

        assertEquals(LaidOutBlockKind.PARAGRAPH, layout.blocks().get(0).kind());
    }

    @Test
    void equationDocumentsRequireMathMeasurer() {
        assertThrows(IllegalArgumentException.class, () -> layoutEngine.layout(document(targetEquationBlock()), 80, textMeasurer));
    }

    @Test
    void typographyProfileCanInfluenceLayoutMeasurements() {
        var compact = new DocumentLayoutEngine(typographyWithParagraphSpacing(2));
        var airy = new DocumentLayoutEngine(typographyWithParagraphSpacing(20));
        var document = document(paragraph(text("One.")), paragraph(text("Two.")));

        var compactLayout = compact.layout(document, 80, textMeasurer);
        var airyLayout = airy.layout(document, 80, textMeasurer);

        assertTrue(airyLayout.height() > compactLayout.height());
    }

    @Test
    void structuralEditRelayoutRefreshesSourceBlockIndices() {
        var editor = new dev.rgcb.scholar.editor.DocumentEditor();
        var split = editor.insertParagraphBreak(
                new dev.rgcb.scholar.editor.EditorState(document(paragraph(text("alpha beta")), paragraph(text("gamma"))),
                        new dev.rgcb.scholar.editor.DocumentPosition(0, 5)),
                Set.of());
        var layoutAfterSplit = layoutEngine.layout(split.document(), 80, textMeasurer);

        assertTrue(layoutAfterSplit.blocks().get(1).lines().stream()
                .flatMap(line -> line.textRuns().stream())
                .allMatch(run -> run.sourceBlockIndex() == 1));

        var joined = editor.deleteForward(new dev.rgcb.scholar.editor.EditorState(split.document(), new dev.rgcb.scholar.editor.DocumentPosition(0, 5)));
        var layoutAfterJoin = layoutEngine.layout(joined.document(), 80, textMeasurer);

        assertTrue(layoutAfterJoin.blocks().get(1).lines().stream()
                .flatMap(line -> line.textRuns().stream())
                .allMatch(run -> run.sourceBlockIndex() == 1));
    }

    @Test
    void multiBlockReplacementRelayoutRefreshesFollowingSourceBlockIndices() {
        var editor = new dev.rgcb.scholar.editor.DocumentEditor();
        var source = document(paragraph(text("abc")), paragraph(text("middle")), paragraph(text("xyz")), paragraph(text("later")));

        var replaced = editor.replaceRange(
                source,
                new dev.rgcb.scholar.editor.DocumentRange(
                        new dev.rgcb.scholar.editor.DocumentPosition(0, 1),
                        new dev.rgcb.scholar.editor.DocumentPosition(2, 2)),
                "");
        var layout = layoutEngine.layout(replaced.document(), 80, textMeasurer);

        assertEquals(2, layout.blocks().size());
        assertEquals("later", lineText(layout.blocks().get(1).lines().get(0)));
        assertTrue(layout.blocks().get(1).lines().stream()
                .flatMap(line -> line.textRuns().stream())
                .allMatch(run -> run.sourceBlockIndex() == 1));
    }

    private static String lineText(LaidOutLine line) {
        return line.textRuns().stream()
                .map(LaidOutText::text)
                .reduce("", String::concat);
    }

    private static Document document(dev.rgcb.scholar.document.BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Heading heading(int level, Text... text) {
        return new Heading(level, inline(text));
    }

    private static Heading heading(String id, int level, Text... text) {
        return new Heading(id, level, inline(text));
    }

    private static Paragraph paragraph(Text... text) {
        return new Paragraph(inline(text));
    }

    private static EquationBlock targetEquationBlock() {
        return new EquationBlock(new MathSequence(List.of(
                new MathIdentifier("v"),
                new MathOperator("=", MathOperatorRole.RELATION),
                new MathFraction(
                        new MathSequence(List.of(new MathSymbol("Δ", MathSymbolKind.GREEK), new MathIdentifier("x"))),
                        new MathSequence(List.of(new MathSymbol("Δ", MathSymbolKind.GREEK), new MathIdentifier("t")))))));
    }

    private static Text text(String content, TextMark... marks) {
        return new Text(content, Set.of(marks));
    }

    private static InlineContent inline(Text... text) {
        return new InlineContent(List.of(text).stream()
                .map(InlineNode.class::cast)
                .toList());
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            return text.length() * 5;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return style.isHeading() ? 12 : 10;
        }
    }

    private static final class FixedMathTextMeasurer implements MathTextMeasurer {
        @Override
        public MathTextMetrics measureText(String content, MathTextKind kind) {
            return new MathTextMetrics(content.length() * 5, 7, 3);
        }
    }

    private static ScholarTypography typographyWithParagraphSpacing(int paragraphSpacingAfter) {
        var base = ScholarTypography.defaultProfile();
        return new ScholarTypography(
                new EnumMap<TypographyRole, TypographyRoleStyle>(base.roleStyles()),
                paragraphSpacingAfter,
                base.headingSpacingBefore(),
                base.headingSpacingAfterBase(),
                base.equationSpacingBefore(),
                base.equationSpacingAfter(),
                base.minPageMargin(),
                base.maxReadableContentWidth(),
                base.mathRuleThickness());
    }
}
