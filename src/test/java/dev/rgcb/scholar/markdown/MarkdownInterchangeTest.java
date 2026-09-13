package dev.rgcb.scholar.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.table.clipboard.TableTsvSerializer;
import dev.rgcb.scholar.editor.DocumentPosition;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.editor.EditorState;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MarkdownInterchangeTest {
    private final MarkdownParser parser = new MarkdownParser();
    private final MarkdownSerializer serializer = new MarkdownSerializer();

    @Test
    void parsesPlainParagraph() {
        var result = parser.parse("Velocity describes motion.");

        assertFalse(result.hasDiagnostics());
        var paragraph = paragraphAt(result.document(), 0);
        assertEquals(List.of(new Text("Velocity describes motion.", Set.of())), paragraph.content().nodes());
    }

    @Test
    void parsesMultipleParagraphs() {
        var result = parser.parse("First paragraph.\n\nSecond paragraph.");

        assertFalse(result.hasDiagnostics());
        assertEquals(2, result.document().blocks().size());
        assertEquals(new Text("First paragraph.", Set.of()), paragraphAt(result.document(), 0).content().nodes().get(0));
        assertEquals(new Text("Second paragraph.", Set.of()), paragraphAt(result.document(), 1).content().nodes().get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"This is one\nparagraph.", "This is one\r\nparagraph."})
    void joinsMultilineParagraphsWithSingleSpace(String source) {
        var result = parser.parse(source);

        assertFalse(result.hasDiagnostics());
        assertEquals(new Text("This is one paragraph.", Set.of()), paragraphAt(result.document(), 0).content().nodes().get(0));
    }

    @Test
    void parsesHeadingsLevelsOneThroughSix() {
        var result = parser.parse("""
                # H1
                ## H2
                ### H3
                #### H4
                ##### H5
                ###### H6
                """);

        assertFalse(result.hasDiagnostics());
        assertEquals(6, result.document().blocks().size());
        for (var level = 1; level <= 6; level++) {
            var heading = headingAt(result.document(), level - 1);
            assertEquals(level, heading.level());
            assertEquals(new Text("H" + level, Set.of()), heading.content().nodes().get(0));
        }
    }

    @Test
    void preservesHashWithoutRequiredHeadingSpace() {
        var result = parser.parse("#NoSpace");

        assertTrue(result.hasDiagnostics());
        assertEquals(MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX, result.diagnostics().get(0).kind());
        assertEquals(new Text("#NoSpace", Set.of()), paragraphAt(result.document(), 0).content().nodes().get(0));
    }

    @Test
    void preservesSevenHashHeadingAsParagraphText() {
        var result = parser.parse("####### Too deep");

        assertTrue(result.hasDiagnostics());
        assertEquals(MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX, result.diagnostics().get(0).kind());
        assertEquals(new Text("####### Too deep", Set.of()), paragraphAt(result.document(), 0).content().nodes().get(0));
    }

    @Test
    void parsesBoldItalicAndBoldItalicText() {
        var result = parser.parse("This has **bold**, *italic*, and ***both*** text.");

        assertFalse(result.hasDiagnostics());
        assertEquals(List.of(
                new Text("This has ", Set.of()),
                new Text("bold", Set.of(TextMark.BOLD)),
                new Text(", ", Set.of()),
                new Text("italic", Set.of(TextMark.ITALIC)),
                new Text(", and ", Set.of()),
                new Text("both", Set.of(TextMark.BOLD, TextMark.ITALIC)),
                new Text(" text.", Set.of())), paragraphAt(result.document(), 0).content().nodes());
    }

    @Test
    void preservesUnmatchedSupportedDelimiter() {
        var result = parser.parse("This has ** unmatched delimiters.");

        assertTrue(result.hasDiagnostics());
        assertEquals(new Text("This has ** unmatched delimiters.", Set.of()), paragraphAt(result.document(), 0).content().nodes().get(0));
    }

    @Test
    void preservesNestedEmphasisAsLiteralContent() {
        var result = parser.parse("This is **bold and *italic*** text.");

        assertTrue(result.hasDiagnostics());
        assertEquals(new Text("This is **bold and *italic*** text.", Set.of()), paragraphAt(result.document(), 0).content().nodes().get(0));
    }

    @Test
    void parsesEscapedSupportedCharacters() {
        var result = parser.parse("\\# not a heading\n\n\\*not italic\\*\n\nBackslash: \\\\");

        assertFalse(result.hasDiagnostics());
        assertEquals(new Text("# not a heading", Set.of()), paragraphAt(result.document(), 0).content().nodes().get(0));
        assertEquals(new Text("*not italic*", Set.of()), paragraphAt(result.document(), 1).content().nodes().get(0));
        assertEquals(new Text("Backslash: \\", Set.of()), paragraphAt(result.document(), 2).content().nodes().get(0));
    }

    @Test
    void parsesLfAndCrlfIdentically() {
        var lf = parser.parse("# Motion\n\nVelocity changes.");
        var crlf = parser.parse("# Motion\r\n\r\nVelocity changes.");

        assertEquals(lf.document(), crlf.document());
        assertEquals(lf.diagnostics(), crlf.diagnostics());
    }

    @Test
    void preservesRecognizableUnsupportedSyntaxWithDiagnostic() {
        var result = parser.parse("[Google](https://google.com)");

        assertTrue(result.hasDiagnostics());
        assertEquals(MarkdownDiagnosticKind.UNSUPPORTED_SYNTAX, result.diagnostics().get(0).kind());
        assertEquals(new Text("[Google](https://google.com)", Set.of()), paragraphAt(result.document(), 0).content().nodes().get(0));
    }

    @Test
    void serializesCanonicalMarkdown() {
        var document = new Document(List.of(
                new Heading(1, inline(new Text("Motion", Set.of()))),
                new Paragraph(inline(
                        new Text("Velocity", Set.of(TextMark.BOLD)),
                        new Text(" describes ", Set.of()),
                        new Text("motion", Set.of(TextMark.ITALIC)),
                        new Text(" and ", Set.of()),
                        new Text("change", Set.of(TextMark.BOLD, TextMark.ITALIC)),
                        new Text(".", Set.of())))));

        assertEquals("""
                # Motion

                **Velocity** describes *motion* and ***change***.
                """, serializer.serialize(document));
    }

    @Test
    void serializesHeadingMarkdownWithoutDerivedSectionNumbers() {
        var document = new Document(List.of(
                new Heading("motion", 1, inline(new Text("Motion", Set.of()))),
                new Heading("average", 2, inline(new Text("Average velocity", Set.of())))));

        assertEquals("""
                # Motion

                ## Average velocity
                """, serializer.serialize(document));
    }

    @Test
    void serializesTableOfContentsAsMinimalReadablePlaceholder() {
        var document = new Document(List.of(new TableOfContentsBlock()));

        assertEquals("Contents\n", serializer.serialize(document));
    }

    @Test
    void escapesLiteralSyntaxCharactersDuringSerialization() {
        var document = new Document(List.of(new Paragraph(inline(
                new Text("# heading marker, *stars*, and \\ slash", Set.of())))));

        assertEquals("\\# heading marker, \\*stars\\*, and \\\\ slash\n", serializer.serialize(document));
    }

    @Test
    void roundTripsDocumentThroughCanonicalMarkdown() {
        var document = new Document(List.of(
                new Heading(1, inline(new Text("Motion", Set.of()))),
                new Paragraph(inline(
                        new Text("Velocity", Set.of(TextMark.BOLD)),
                        new Text(" describes how quickly position changes.", Set.of()))),
                new Paragraph(inline(
                        new Text("Both", Set.of(TextMark.BOLD, TextMark.ITALIC)),
                        new Text(" marks round-trip.", Set.of())))));

        var serialized = serializer.serialize(document);
        var parsed = parser.parse(serialized);

        assertFalse(parsed.hasDiagnostics());
        assertEquals(document, parsed.document());
    }

    @Test
    void serializesEditorAppliedBoldItalicAndCombinedMarks() {
        var session = new EditorSession(new Document(List.of(new Paragraph(inline(new Text("velocity change both", Set.of()))))), 0);

        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 0), new DocumentPosition(0, 8)));
        session.toggleMark(TextMark.BOLD);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 9), new DocumentPosition(0, 15)));
        session.toggleMark(TextMark.ITALIC);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 16), new DocumentPosition(0, 20)));
        session.toggleMark(TextMark.BOLD);
        session.toggleMark(TextMark.ITALIC);

        assertEquals("**velocity** *change* ***both***\n", serializer.serialize(session.current().document()));
    }

    @Test
    void serializesEditorBlockStyleTransformationsAndRoundTrips() {
        var session = new EditorSession(new Document(List.of(new Paragraph(inline(
                new Text("Results", Set.of(TextMark.BOLD)))))), 0);

        session.setBlockStyle(dev.rgcb.scholar.editor.BlockStyle.heading(2));

        assertEquals("## **Results**\n", serializer.serialize(session.current().document()));
        var parsedHeading = parser.parse(serializer.serialize(session.current().document()));
        assertFalse(parsedHeading.hasDiagnostics());
        assertEquals(session.current().document(), parsedHeading.document());

        session.setBlockStyle(dev.rgcb.scholar.editor.BlockStyle.paragraph());

        assertEquals("**Results**\n", serializer.serialize(session.current().document()));
        var parsedParagraph = parser.parse(serializer.serialize(session.current().document()));
        assertFalse(parsedParagraph.hasDiagnostics());
        assertEquals(session.current().document(), parsedParagraph.document());
    }

    @Test
    void serializesParagraphSplitAndRoundTripsSemantically() {
        var session = new EditorSession(new Document(List.of(new Paragraph(inline(new Text("AlphaBeta", Set.of()))))), 0);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 5)));

        session.enter();

        assertEquals("Alpha\n\nBeta\n", serializer.serialize(session.current().document()));
        var parsed = parser.parse(serializer.serialize(session.current().document()));
        assertFalse(parsed.hasDiagnostics());
        assertEquals(session.current().document(), parsed.document());
    }

    @Test
    void serializesHeadingEndEnterAndRoundTripsSemantically() {
        var session = new EditorSession(new Document(List.of(new Heading(2, inline(new Text("Results", Set.of()))))), 0);

        session.enter();
        session.typeText("Body");

        assertEquals("## Results\n\nBody\n", serializer.serialize(session.current().document()));
        var parsed = parser.parse(serializer.serialize(session.current().document()));
        assertFalse(parsed.hasDiagnostics());
        assertEquals(session.current().document(), parsed.document());
    }

    @Test
    void serializesBoundaryJoinAndRoundTripsSemantically() {
        var session = new EditorSession(new Document(List.of(
                new Paragraph(inline(new Text("A", Set.of(TextMark.BOLD)))),
                new Paragraph(inline(new Text("B", Set.of(TextMark.BOLD)))))), 0);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 1)));

        session.deleteForward();

        assertEquals("**A****B**\n", serializer.serialize(session.current().document()));
        var parsed = parser.parse(serializer.serialize(session.current().document()));
        assertFalse(parsed.hasDiagnostics());
        assertEquals(session.current().document(), parsed.document());
    }

    @Test
    void serializesOneColumnAndTwoByTwoTablesCanonically() {
        assertEquals("""
                | A |
                | --- |
                """, serializer.serialize(new Document(List.of(new TableBlock(List.of(row(cell("A"))), 1)))));

        var document = new Document(List.of(new TableBlock(List.of(
                row(cell("Quantity"), cell("Value")),
                row(cell("Voltage"), cell("12"))), 1)));

        assertEquals("""
                | Quantity | Value |
                | --- | --- |
                | Voltage | 12 |
                """, serializer.serialize(document));
    }

    @Test
    void serializesMultipleBodyRowsEmptyCellsUnicodeAndInlineFormattingInTables() {
        var document = new Document(List.of(new TableBlock(List.of(
                row(cell(new Text("Quantity", Set.of(TextMark.BOLD))), TableCell.empty()),
                row(cell(new Text("Ω", Set.of(TextMark.ITALIC))), cell(new Text("Δx", Set.of(TextMark.BOLD, TextMark.ITALIC)))),
                row(TableCell.empty(), cell("café e\u0301 θ λ"))), 1)));

        assertEquals("""
                | **Quantity** |  |
                | --- | --- |
                | *Ω* | ***Δx*** |
                |  | café é θ λ |
                """, serializer.serialize(document));
    }

    @Test
    void serializesLiteralPipesAndBackslashesInTableCells() {
        var document = new Document(List.of(new TableBlock(List.of(
                row(cell("Expression"), cell("Meaning")),
                row(cell("A | B"), cell("A \\ B")),
                row(cell("A \\| B"), cell("plain"))), 1)));

        assertEquals("""
                | Expression | Meaning |
                | --- | --- |
                | A \\| B | A \\\\ B |
                | A \\\\\\| B | plain |
                """, serializer.serialize(document));
        var parsed = parser.parse(serializer.serialize(document));
        assertFalse(parsed.hasDiagnostics());
        assertEquals(document, parsed.document());
    }

    @Test
    void rejectsHeaderlessTableSerialization() {
        var document = new Document(List.of(new TableBlock(List.of(row(cell("A"), cell("B"))), 0)));

        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(document));
    }

    @Test
    void parsesCanonicalTableWithSemanticHeaderAndMultipleBodyRows() {
        var result = parser.parse("""
                | Quantity | Value | Unit |
                | --- | --- | --- |
                | Voltage | 12 | V |
                | Current | 2 | A |
                """);

        assertFalse(result.hasDiagnostics());
        var table = tableAt(result.document(), 0);
        assertEquals(1, table.headerRowCount());
        assertEquals(3, table.columnCount());
        assertEquals(3, table.rows().size());
        assertEquals("Quantity", cellText(table, 0, 0));
        assertEquals("Current", cellText(table, 2, 0));
    }

    @Test
    void parsesEmptyCellsAndEmptyHeaderCells() {
        var result = parser.parse("""
                | A |  |
                | --- | --- |
                |  | D |
                """);

        assertFalse(result.hasDiagnostics());
        var table = tableAt(result.document(), 0);
        assertEquals(1, table.headerRowCount());
        assertTrue(table.rows().get(0).cells().get(1).content().content().nodes().isEmpty());
        assertTrue(table.rows().get(1).cells().get(0).content().content().nodes().isEmpty());
        assertEquals("D", cellText(table, 1, 1));
    }

    @Test
    void parsesEscapedPipeBackslashAndInlineFormattingInsideCells() {
        var result = parser.parse("""
                | Expression | Description |
                | --- | --- |
                | A \\| B | **Voltage** and *Current* |
                | A \\\\ B | ***both*** |
                """);

        assertFalse(result.hasDiagnostics());
        var table = tableAt(result.document(), 0);
        assertEquals("A | B", cellText(table, 1, 0));
        assertEquals("A \\ B", cellText(table, 2, 0));
        assertEquals(List.of(
                new Text("Voltage", Set.of(TextMark.BOLD)),
                new Text(" and ", Set.of()),
                new Text("Current", Set.of(TextMark.ITALIC))),
                table.rows().get(1).cells().get(1).content().content().nodes());
        assertEquals(new Text("both", Set.of(TextMark.BOLD, TextMark.ITALIC)),
                table.rows().get(2).cells().get(1).content().content().nodes().get(0));
    }

    @Test
    void parsesUnicodeInTableCellsWithoutSpecialTableLogic() {
        var result = parser.parse("""
                | Symbol | Value |
                | --- | --- |
                | Ω Δx θ λ | café é |
                """);

        assertFalse(result.hasDiagnostics());
        assertEquals("Ω Δx θ λ", cellText(tableAt(result.document(), 0), 1, 0));
        assertEquals("café e\u0301", cellText(tableAt(result.document(), 0), 1, 1));
    }

    @Test
    void doesNotTreatOrdinaryPipesAsTables() {
        var result = parser.parse("A | B is ordinary paragraph text.");

        assertFalse(result.hasDiagnostics());
        assertEquals(new Text("A | B is ordinary paragraph text.", Set.of()), paragraphAt(result.document(), 0).content().nodes().get(0));
    }

    @Test
    void reportsMalformedTableSeparatorAndPreservesSourceAsParagraph() {
        var result = parser.parse("""
                | A | B |
                | -- | --- |
                """);

        assertTrue(result.hasDiagnostics());
        assertEquals(MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX, result.diagnostics().get(0).kind());
        assertEquals(new Text("| A | B | | -- | --- |", Set.of()), paragraphAt(result.document(), 0).content().nodes().get(0));
    }

    @Test
    void rejectsSeparatorCountMismatchAndRaggedBodyRows() {
        var separatorMismatch = parser.parse("""
                | A | B |
                | --- |
                """);
        var raggedBody = parser.parse("""
                | A | B |
                | --- | --- |
                | C |
                """);

        assertTrue(separatorMismatch.hasDiagnostics());
        assertEquals(MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX, separatorMismatch.diagnostics().get(0).kind());
        assertFalse(separatorMismatch.document().blocks().get(0) instanceof TableBlock);
        assertTrue(raggedBody.hasDiagnostics());
        assertEquals(MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX, raggedBody.diagnostics().get(0).kind());
        assertFalse(raggedBody.document().blocks().get(0) instanceof TableBlock);
    }

    @Test
    void rejectsAlignmentSeparatorSyntaxInsteadOfDiscardingIt() {
        var result = parser.parse("""
                | A | B |
                | :--- | ---: |
                """);

        assertTrue(result.hasDiagnostics());
        assertEquals(MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX, result.diagnostics().get(0).kind());
        assertFalse(result.document().blocks().get(0) instanceof TableBlock);
    }

    @Test
    void terminatesTableBeforeNonTableBlockWithoutBlankLine() {
        var result = parser.parse("""
                | A | B |
                | --- | --- |
                | C | D |
                ## Next
                """);

        assertFalse(result.hasDiagnostics());
        assertInstanceOf(TableBlock.class, result.document().blocks().get(0));
        assertEquals(2, headingAt(result.document(), 1).level());
        assertEquals("Next", ((Text) headingAt(result.document(), 1).content().nodes().get(0)).content());
    }

    @Test
    void markdownTableSerializeParseRoundTripsStructurally() {
        var document = new Document(List.of(
                new Paragraph(inline(new Text("Before", Set.of()))),
                new TableBlock(List.of(
                        row(cell(new Text("**literal**", Set.of())), cell(new Text("A | B", Set.of(TextMark.BOLD)))),
                        row(TableCell.empty(), cell(new Text("Ω café e\u0301", Set.of(TextMark.ITALIC))))), 1),
                new Heading(2, inline(new Text("After", Set.of())))));

        var parsed = parser.parse(serializer.serialize(document));

        assertFalse(parsed.hasDiagnostics());
        assertEquals(document, parsed.document());
    }

    @Test
    void markdownTableParseSerializesToCanonicalForm() {
        var result = parser.parse("""
                |  A  | B |
                | ----- | --- |
                | C |  D  |
                """);

        assertFalse(result.hasDiagnostics());
        assertEquals("""
                | A | B |
                | --- | --- |
                | C | D |
                """, serializer.serialize(result.document()));
    }

    @Test
    void markdownTableSerializationDoesNotChangeTsvFallback() {
        var table = new TableBlock(List.of(
                row(cell("Quantity"), cell("Value")),
                row(cell(new Text("Ω", Set.of(TextMark.BOLD))), cell("12"))), 1);

        assertEquals("Quantity\tValue\nΩ\t12", new TableTsvSerializer().serialize(table));
        assertEquals("""
                | Quantity | Value |
                | --- | --- |
                | **Ω** | 12 |
                """, serializer.serialize(new Document(List.of(table))));
    }

    private static dev.rgcb.scholar.document.InlineContent inline(InlineNode... nodes) {
        return new dev.rgcb.scholar.document.InlineContent(List.of(nodes));
    }

    private static Heading headingAt(Document document, int index) {
        return assertInstanceOf(Heading.class, document.blocks().get(index));
    }

    private static Paragraph paragraphAt(Document document, int index) {
        return assertInstanceOf(Paragraph.class, document.blocks().get(index));
    }

    private static TableBlock tableAt(Document document, int index) {
        return assertInstanceOf(TableBlock.class, document.blocks().get(index));
    }

    private static TableRow row(TableCell... cells) {
        return new TableRow(List.of(cells));
    }

    private static TableCell cell(String text) {
        return cell(new Text(text, Set.of()));
    }

    private static TableCell cell(Text... text) {
        return new TableCell(new TableCellContent(new InlineContent(List.of(text).stream()
                .map(InlineNode.class::cast)
                .toList())));
    }

    private static String cellText(TableBlock table, int row, int column) {
        return table.rows().get(row).cells().get(column).content().content().nodes().stream()
                .map(Text.class::cast)
                .map(Text::content)
                .reduce("", String::concat);
    }
}
