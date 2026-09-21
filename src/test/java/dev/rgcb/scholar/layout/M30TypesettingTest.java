package dev.rgcb.scholar.layout;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.editor.DocumentHitTester;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import dev.rgcb.scholar.persistence.DocumentJsonCodec;
import dev.rgcb.scholar.persistence.PersistenceResult;
import dev.rgcb.scholar.typography.DocumentStyleResolver;
import dev.rgcb.scholar.validation.DocumentDiagnosticCode;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class M30TypesettingTest {
    private final DocumentLayoutEngine engine = new DocumentLayoutEngine();
    private final TextMeasurer text = new SizedTextMeasurer();

    @Test void layoutSectionBreakRequiresAValidColumnLayoutAndValidatesAsStructuralContent() {
        assertThrows(NullPointerException.class, () -> new LayoutSectionBreak(null));
        assertTrue(DocumentValidator.validate(new Document(List.of(paragraph("a"),
                new LayoutSectionBreak(ColumnLayout.two()), paragraph("b")))).isValid());
        var invalid = new LayoutSectionBreak(new ColumnLayout(2, PhysicalLength.millimetres(500)));
        assertTrue(DocumentValidator.validate(new Document(List.of(paragraph("a"), invalid, paragraph("b"))))
                .diagnostics().stream().anyMatch(d -> d.code() == DocumentDiagnosticCode.INVALID_PAGE_CONFIGURATION));
    }

    @Test void physicalPaperUsesDeterministicUnitsAndOrientation() {
        assertEquals(210_000, PaperSize.a4().width().micrometres());
        assertEquals(297_000, PaperSize.a4().height().micrometres());
        assertEquals(215_900, PaperSize.letter().width().micrometres());
        var portrait = DocumentSettings.blank();
        var landscape = new DocumentSettings(portrait.template(), portrait.paper(), PageOrientation.LANDSCAPE,
                portrait.margins(), portrait.columns(), portrait.decoration());
        assertEquals(portrait.pageWidth(), landscape.pageHeight());
        assertEquals(portrait.pageHeight(), landscape.pageWidth());
    }

    @Test void automaticPaginationContinuesOneSemanticParagraphAcrossPages() {
        var tiny = tinySettings(ColumnLayout.one());
        var paragraph = paragraph("word ".repeat(1_600));
        var layout = engine.layoutPaginated(new Document(List.of(paragraph), List.of(), tiny), text, null);
        assertTrue(layout.pages().size() > 1);
        assertEquals(1, layout.blocks().size());
        assertTrue(layout.blocks().getFirst().lines().stream().map(LaidOutLine::y).distinct().count() > 10);
    }

    @Test void manualPageBreakForcesFollowingContentToNextPage() {
        var document = new Document(List.of(paragraph("before"), new PageBreak(), paragraph("after")), List.of(), tinySettings(ColumnLayout.one()));
        var layout = engine.layoutPaginated(document, text, null);
        assertEquals(2, layout.pages().size());
        assertEquals(LaidOutBlockKind.PAGE_BREAK, layout.blocks().get(1).kind());
        assertTrue(layout.blocks().get(2).y() >= layout.pages().get(1).contentY());
    }

    @Test void twoColumnFlowUsesSecondColumnBeforeNextPage() {
        var blocks = java.util.stream.IntStream.range(0, 8).mapToObj(i -> (BlockNode) paragraph("line ".repeat(8))).toList();
        var layout = engine.layoutPaginated(new Document(blocks, List.of(), tinySettings(ColumnLayout.two())), text, null);
        var firstPage = layout.pages().getFirst();
        assertTrue(layout.blocks().stream().anyMatch(block -> block.x() >= firstPage.columns().get(1).x()));
    }

    @Test void layoutSectionBreakStartsTwoColumnsAtTheCurrentSamePageY() {
        var document = new Document(List.of(
                paragraph("full width top matter"),
                new LayoutSectionBreak(ColumnLayout.two()),
                paragraph("body ".repeat(900))), List.of(), tinySettings(ColumnLayout.one()));
        var layout = engine.layoutPaginated(document, text, null);
        var marker = layout.blocks().get(1);
        var body = layout.blocks().get(2);
        var sectionColumns = layout.pages().getFirst().columns().stream()
                .filter(column -> column.y() == marker.y()).toList();

        assertEquals(LaidOutBlockKind.LAYOUT_SECTION_BREAK, marker.kind());
        assertEquals(2, sectionColumns.size());
        assertEquals(marker.y(), body.lines().stream()
                .filter(line -> line.x() == sectionColumns.get(1).x()).findFirst().orElseThrow().y());
        assertTrue(layout.pages().size() > 1);
        assertTrue(body.lines().stream().anyMatch(line -> line.y() == layout.pages().get(1).contentY()));
    }

    @Test void twoToOneTransitionFromSecondColumnAdvancesConservativelyToNextPage() {
        var document = new Document(List.of(
                paragraph("line ".repeat(8)), paragraph("line ".repeat(8)), paragraph("line ".repeat(8)),
                paragraph("line ".repeat(8)), paragraph("line ".repeat(8)), paragraph("line ".repeat(8)),
                new LayoutSectionBreak(ColumnLayout.one()),
                paragraph("full width tail")), List.of(), tinySettings(ColumnLayout.two()));
        var layout = engine.layoutPaginated(document, text, null);
        var marker = layout.blocks().get(6);
        var tail = layout.blocks().get(7);

        assertTrue(marker.x() > layout.pages().getFirst().contentX());
        assertTrue(tail.y() >= layout.pages().stream()
                .filter(page -> page.index() > 0).findFirst().orElseThrow().contentY());
        var tailPage = layout.pages().stream().filter(page -> tail.y() >= page.y() && tail.y() < page.y() + page.height())
                .findFirst().orElseThrow();
        assertEquals(1, tailPage.columns().stream().filter(column -> column.y() == tailPage.contentY()).count());
    }

    @Test void multipleSectionTransitionsAndPageBreakRemainDeterministic() {
        var document = new Document(List.of(paragraph("top"),
                new LayoutSectionBreak(ColumnLayout.two()), paragraph("columns"),
                new PageBreak(), new LayoutSectionBreak(ColumnLayout.one()), paragraph("tail")),
                List.of(), tinySettings(ColumnLayout.one()));
        var layout = engine.layoutPaginated(document, text, null);
        assertEquals(layout, engine.layoutPaginated(document, text, null));
        assertEquals(LaidOutBlockKind.PAGE_BREAK, layout.blocks().get(3).kind());
        assertEquals(LaidOutBlockKind.LAYOUT_SECTION_BREAK, layout.blocks().get(4).kind());
        assertEquals(1, layout.pages().get(1).columns().stream()
                .filter(column -> column.y() == layout.blocks().get(4).y()).count());
    }

    @Test void pageWidthTableOccupiesContentWidthAndResumesOnFollowingPage() {
        var table = TableBlock.empty(2, 2).withSpan(ContentSpan.PAGE_WIDTH);
        var document = new Document(List.of(paragraph("lead"), table, paragraph("tail")), List.of(), tinySettings(ColumnLayout.two()));
        var layout = engine.layoutPaginated(document, text, null);
        assertEquals(layout.pages().get(1).contentWidth(), layout.blocks().get(1).width());
        assertTrue(layout.blocks().get(2).y() >= layout.pages().get(2).contentY());
    }

    @Test void pageWidthFigureInterruptsAndThenResumesTwoColumnFlowWithoutCorruptingColumnTop() {
        var diagram = new DiagramBlock(new DiagramDefinition("wide", new DiagramCanvas(40, 20), List.of(), List.of()));
        var figure = new FigureBlock("wide-figure", diagram,
                new InlineContent(List.of(new Text("Wide scientific figure", Set.of()))), ContentSpan.PAGE_WIDTH);
        var tailText = "result".repeat(300);
        var document = new Document(List.of(fixedParagraph("lead"), figure, fixedParagraph(tailText)),
                List.of(), tinySettings(ColumnLayout.two()));
        var layout = engine.layoutPaginated(document, text, null);
        var laidOutFigure = layout.blocks().get(1);
        var tail = layout.blocks().get(2);
        var figurePage = pageContaining(layout, laidOutFigure.y());
        var tailPage = pageContaining(layout, tail.y());

        assertEquals(figurePage.contentWidth(), laidOutFigure.width());
        assertTrue(tailPage.index() > figurePage.index());
        assertEquals(tailPage.contentY(), tail.lines().getFirst().y());
        assertTrue(tail.lines().stream().anyMatch(line -> line.x() == tailPage.columns().get(1).x()));
        assertEquals(tailText, tail.lines().stream().flatMap(line -> line.textRuns().stream())
                .map(LaidOutText::text).reduce("", String::concat));
    }

    @Test void sectionFlowAppliesColumnWidthToAtomicTableWithoutChangingItsSpanPolicy() {
        var table = TableBlock.empty(2, 2).withSpan(ContentSpan.COLUMN);
        var document = new Document(List.of(paragraph("top"), new LayoutSectionBreak(ColumnLayout.two()), table),
                List.of(), tinySettings(ColumnLayout.one()));
        var layout = engine.layoutPaginated(document, text, null);
        var sectionTop = layout.blocks().get(1).y();
        var sectionColumn = layout.pages().getFirst().columns().stream()
                .filter(column -> column.y() == sectionTop).findFirst().orElseThrow();
        assertEquals(sectionColumn.width(), layout.blocks().get(2).width());
        assertEquals(ContentSpan.COLUMN, table.span());
    }

    @Test void automaticTablePromotesOnlyWhenItsContentCannotRemainReadableInAColumn() {
        var automatic = tableWithWords("abcdefghijklmnopqrstuvwxyz1234", "abcdefghijklmnopqrstuvwxyz5678");
        var explicitColumn = automatic.withSpan(ContentSpan.COLUMN);
        var settings = tinySettings(ColumnLayout.two());

        var automaticLayout = engine.layoutPaginated(new Document(List.of(automatic), List.of(), settings), text, null);
        var columnLayout = engine.layoutPaginated(new Document(List.of(explicitColumn), List.of(), settings), text, null);

        assertEquals(ContentSpan.AUTO, automatic.span());
        assertEquals(automaticLayout.pages().getFirst().contentWidth(), automaticLayout.blocks().getFirst().width());
        assertEquals(columnLayout.pages().getFirst().columns().getFirst().width(), columnLayout.blocks().getFirst().width());
        assertEquals(ContentSpan.COLUMN, explicitColumn.span());
    }

    @Test void narrowColumnTableOfContentsWrapsWordsWithHierarchyAndClickableContinuationGeometry() {
        var title = "Document Structure & Navigation Ω測定";
        var document = new Document(List.of(
                new TableOfContentsBlock(),
                new Heading("deep", 6, new InlineContent(List.of(new Text(title, Set.of()))))),
                List.of(), tinySettings(ColumnLayout.two()));
        var layout = engine.layoutPaginated(document, text, null);
        var toc = layout.blocks().getFirst();
        var entryLines = toc.lines().stream().skip(1).toList();
        var rendered = entryLines.stream().map(line -> line.textRuns().stream()
                        .map(LaidOutText::text).reduce("", String::concat).trim())
                .reduce("", (left, right) -> left.isEmpty() ? right : left + " " + right)
                .replaceAll("\\s+", " ").trim();
        var expected = new DocumentStructureResolver().resolve(document).sections().getFirst().displayText();
        var column = layout.pages().getFirst().columns().getFirst();

        assertEquals(expected, rendered);
        assertTrue(entryLines.size() > 1);
        assertTrue(entryLines.get(1).x() > entryLines.getFirst().x());
        assertTrue(entryLines.stream().allMatch(line -> line.x() + line.width() <= column.x() + column.width()));
        assertEquals(entryLines.size(), toc.tableOfContents().orElseThrow().entries().size());
        assertTrue(toc.tableOfContents().orElseThrow().entries().stream()
                .allMatch(entry -> entry.targetId().equals("deep") && entry.contains(entry.x(), entry.y())));
    }

    @Test void proseUsesEveryAvailableColumnBeforeAdvancingWithoutLossOrDuplication() {
        assertFlow(640, 1, 1);
        assertFlow(641, 1, 2);
        assertFlow(1_280, 1, 2);
        assertFlow(1_281, 2, 1);
        assertFlow(2_561, 3, 1);
    }

    @Test void pageWidthTableInterruptsAndThenRestartsTwoColumnFlowAtNormalPageTop() {
        var table = tableWithWords("abcdefghijklmnopqrstuvwxyz1234", "abcdefghijklmnopqrstuvwxyz5678");
        var tailText = "tail".repeat(400);
        var document = new Document(List.of(fixedParagraph("lead"), table, fixedParagraph(tailText)),
                List.of(), tinySettings(ColumnLayout.two()));
        var layout = engine.layoutPaginated(document, text, null);
        var laidOutTable = layout.blocks().get(1);
        var tail = layout.blocks().get(2);
        var tablePage = pageContaining(layout, laidOutTable.y());
        var tailPage = pageContaining(layout, tail.y());

        assertEquals(ContentSpan.AUTO, table.span());
        assertEquals(tablePage.contentWidth(), laidOutTable.width());
        assertTrue(tailPage.index() > tablePage.index());
        assertEquals(tailPage.contentY(), tail.lines().getFirst().y());
        assertTrue(tail.lines().stream().anyMatch(line -> line.x() == tailPage.columns().get(1).x()));
        assertEquals(tailText, tail.lines().stream().flatMap(line -> line.textRuns().stream())
                .map(LaidOutText::text).reduce("", String::concat));
    }

    @Test void paragraphFormattingControlsPlacementRhythmAndJustification() {
        var format = new ParagraphFormat(Optional.of(ParagraphAlignment.JUSTIFIED), Optional.of(1500),
                Optional.of(7), Optional.of(9), Optional.of(5), Optional.of(6), Optional.of(3));
        var formatted = new Paragraph(new InlineContent(List.of(new Text("alpha beta gamma delta epsilon zeta", Set.of()))),
                SemanticStyle.BODY_TEXT, format);
        var layout = engine.layoutPaginated(new Document(List.of(formatted, paragraph("tail")), List.of(),
                tinySettings(ColumnLayout.one())), text, null);
        var first = layout.blocks().getFirst();
        assertEquals(layout.pages().getFirst().contentY() + 7, first.y());
        assertTrue(first.lines().getFirst().x() >= layout.pages().getFirst().contentX() + 8);
        assertTrue(first.lines().getFirst().height() > 10);
        if (first.lines().size() > 1) {
            assertEquals(layout.pages().getFirst().columns().getFirst().width() - 5 - 6,
                    first.lines().getFirst().width());
        }
        assertTrue(layout.blocks().get(1).y() >= first.lines().getLast().y() + first.lines().getLast().height() + 9);
    }

    @Test void pageDecorationControlsHeadersFootersAndNumberingWithoutSemanticPageBlocks() {
        var settings = tinySettings(ColumnLayout.one());
        var numbered = engine.layoutPaginated(new Document(List.of(paragraph("x")), List.of(), settings), text, null);
        assertEquals("Scholar", numbered.pages().getFirst().headerText());
        assertTrue(numbered.pages().getFirst().pageNumberVisible());

        var plainSettings = new DocumentSettings(settings.template(), settings.paper(), settings.orientation(),
                settings.margins(), settings.columns(), PageDecoration.none());
        var plain = engine.layoutPaginated(new Document(List.of(paragraph("x")), List.of(), plainSettings), text, null);
        assertFalse(plain.pages().getFirst().pageNumberVisible());
        assertEquals(1, plain.blocks().size());
    }

    @Test void paginationIsDeterministicAndHasNoZoomInput() {
        var document = new Document(List.of(paragraph("word ".repeat(1_600))), List.of(), tinySettings(ColumnLayout.two()));
        var first = engine.layoutPaginated(document, text, null);
        var second = engine.layoutPaginated(document, text, null);
        assertEquals(first, second);
        assertTrue(java.util.Arrays.stream(Document.class.getRecordComponents())
                .noneMatch(component -> component.getName().toLowerCase(java.util.Locale.ROOT).contains("zoom")));
    }

    @Test void hitTestingAcrossAutomaticPageBoundaryKeepsOneSemanticParagraph() {
        var value = "word ".repeat(1_600);
        var layout = engine.layoutPaginated(new Document(List.of(paragraph(value)), List.of(), tinySettings(ColumnLayout.one())), text, null);
        var block = layout.blocks().getFirst();
        var lastLine = block.lines().getLast();
        var hit = new DocumentHitTester().hitTestInBlock(layout, 0,
                lastLine.x() + lastLine.width(), lastLine.y() + 1, text).orElseThrow();
        assertEquals(0, hit.blockIndex());
        assertEquals(value.length(), hit.characterOffset());
    }

    @Test void equationsAndFiguresRemainAtomicAndInsideAContentPage() {
        var equation = new EquationBlock(new MathIdentifier("energy"));
        var diagram = new DiagramBlock(new DiagramDefinition("empty", new DiagramCanvas(40, 24), List.of(), List.of()));
        var figure = FigureBlock.emptyCaption("figure", diagram);
        var document = new Document(List.of(paragraph("lead"), equation, figure));
        var layout = engine.layoutPaginated(document, text,
                (value, kind) -> new MathTextMetrics(value.length() * 4, 7, 3));
        for (var index : List.of(1, 2)) {
            var block = layout.blocks().get(index);
            assertTrue(layout.pages().stream().anyMatch(page -> block.y() >= page.contentY()
                    && block.y() + block.height() <= page.contentY() + page.contentHeight()));
        }
    }

    @Test void fontSizeAffectsMetricsWrappingAndPaginationInput() {
        var small = new TextStyle(Set.of(), 0, dev.rgcb.scholar.typography.TypographyRole.BODY,
                new TextFormat(Optional.empty(), Optional.of(20)));
        var large = small.withFormat(new TextFormat(Optional.empty(), Optional.of(40)));
        assertTrue(text.measureWidth("science", large) > text.measureWidth("science", small));
        assertTrue(text.lineHeight(large) > text.lineHeight(small));
    }

    @Test void styleResolutionUsesTemplateThenSemanticThenLocalOverride() {
        var resolver = new DocumentStyleResolver();
        var base = resolver.resolveText(DocumentTemplateId.IEEE_STYLE, SemanticStyle.BODY_TEXT, TextFormat.none());
        var local = resolver.resolveText(DocumentTemplateId.IEEE_STYLE, SemanticStyle.BODY_TEXT,
                new TextFormat(Optional.of(ScholarFontFamily.SCIENTIFIC_MATH), Optional.of(28)));
        assertNotEquals(28, base.fontSizeHalfPoints().orElseThrow());
        assertEquals(28, local.fontSizeHalfPoints().orElseThrow());
        assertEquals(ScholarFontFamily.SCIENTIFIC_MATH, local.fontFamily().orElseThrow());
    }

    @Test void ieeeTemplateIsPublicationOrientedButExplicitlyNamedStyle() {
        var document = DocumentTemplates.create(DocumentTemplateId.IEEE_STYLE);
        assertEquals(DocumentTemplateId.IEEE_STYLE, document.settings().template());
        assertEquals(1, document.settings().columns().count());
        assertEquals(List.of(SemanticStyle.TITLE, SemanticStyle.AUTHOR, SemanticStyle.AFFILIATION,
                        SemanticStyle.ABSTRACT, SemanticStyle.KEYWORDS),
                document.blocks().subList(0, 5).stream().map(Paragraph.class::cast).map(Paragraph::style).toList());
        assertEquals(new LayoutSectionBreak(ColumnLayout.two()), document.blocks().get(5));
        assertTrue(document.blocks().stream().anyMatch(block -> block instanceof Paragraph p && p.style() == SemanticStyle.ABSTRACT));
        assertTrue(document.blocks().stream().anyMatch(block -> block instanceof Heading));
        assertTrue(document.blocks().stream().filter(Heading.class::isInstance).map(Heading.class::cast)
                .noneMatch(heading -> heading.content().nodes().stream().map(Object::toString)
                        .anyMatch(value -> value.contains("References"))));
        var layout = engine.layoutPaginated(document, text, null);
        assertTrue(layout.blocks().get(6).y() - layout.blocks().getFirst().y() < 120,
                "IEEE top matter should remain compact");
    }

    @Test void v2RoundTripPreservesM30StateAndV1DefaultsRemainReadable() throws Exception {
        var codec = new DocumentJsonCodec();
        var source = new Document(List.of(
                new Paragraph(new InlineContent(List.of(new Text("x2", Set.of(TextMark.UNDERLINE, TextMark.SUPERSCRIPT),
                        new TextFormat(Optional.of(ScholarFontFamily.SOURCE_SANS_3), Optional.of(24))))),
                        SemanticStyle.ABSTRACT, new ParagraphFormat(Optional.of(ParagraphAlignment.JUSTIFIED), Optional.of(1200),
                                Optional.of(2), Optional.of(3), Optional.of(4), Optional.of(5), Optional.of(6))),
                new LayoutSectionBreak(ColumnLayout.two()), new PageBreak(),
                TableBlock.empty(1, 1), TableBlock.empty(1, 1).withSpan(ContentSpan.COLUMN),
                TableBlock.empty(1, 1).withSpan(ContentSpan.PAGE_WIDTH)), List.of(),
                DocumentTemplates.settings(DocumentTemplateId.IEEE_STYLE));
        var encoded = ((PersistenceResult.Success<String>) codec.encode(source)).value();
        assertEquals(source, ((PersistenceResult.Success<Document>) codec.decode(encoded)).value());
        assertEquals(List.of(ContentSpan.AUTO, ContentSpan.COLUMN, ContentSpan.PAGE_WIDTH),
                ((PersistenceResult.Success<Document>) codec.decode(encoded)).value().blocks().stream()
                        .filter(TableBlock.class::isInstance).map(TableBlock.class::cast).map(TableBlock::span).toList());
        var v1 = new String(getClass().getResourceAsStream("/persistence/v1-minimal.scholar.json").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(DocumentSettings.blank(), ((PersistenceResult.Success<Document>) codec.decode(v1)).value().settings());
        var v1Table = """
                {"format":"scholar-document","version":1,"blocks":[{"type":"table","id":null,
                "headerRows":0,"rows":[[[{"type":"text","content":"x","marks":[]}]]],"binding":null}],"datasets":[]}
                """;
        assertEquals(ContentSpan.AUTO, ((TableBlock) ((PersistenceResult.Success<Document>) codec.decode(v1Table))
                .value().blocks().getFirst()).span());
    }

    @Test void validatorRejectsMarginsThatConsumePaper() {
        var huge = PhysicalLength.millimetres(200);
        var invalid = new DocumentSettings(DocumentTemplateId.BLANK, PaperSize.a4(), PageOrientation.PORTRAIT,
                new PageMargins(huge, huge, huge, huge), ColumnLayout.one(), PageDecoration.none());
        var result = DocumentValidator.validate(new Document(List.of(paragraph("x")), List.of(), invalid));
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.code() == DocumentDiagnosticCode.INVALID_PAGE_CONFIGURATION));
    }

    private static DocumentSettings tinySettings(ColumnLayout columns) {
        return new DocumentSettings(DocumentTemplateId.BLANK,
                PaperSize.custom(PhysicalLength.millimetres(80), PhysicalLength.millimetres(60)),
                PageOrientation.PORTRAIT,
                new PageMargins(PhysicalLength.millimetres(5), PhysicalLength.millimetres(5), PhysicalLength.millimetres(5), PhysicalLength.millimetres(5)),
                columns, new PageDecoration("Scholar", "", true));
    }

    private static Paragraph paragraph(String value) {
        return new Paragraph(new InlineContent(List.of(new Text(value, Set.of()))));
    }

    private static Paragraph fixedParagraph(String value) {
        return new Paragraph(new InlineContent(List.of(new Text(value, Set.of(),
                new TextFormat(Optional.empty(), Optional.of(20))))));
    }

    private static TableBlock tableWithWords(String left, String right) {
        return new TableBlock(List.of(new TableRow(List.of(
                new TableCell(new TableCellContent(new InlineContent(List.of(new Text(left, Set.of()))))),
                new TableCell(new TableCellContent(new InlineContent(List.of(new Text(right, Set.of())))))))), 1);
    }

    private void assertFlow(int characterCount, int expectedPages, int expectedColumnsOnLastPage) {
        var value = "x".repeat(characterCount);
        var layout = engine.layoutPaginated(new Document(List.of(fixedParagraph(value)), List.of(),
                tinySettings(ColumnLayout.two())), text, null);
        var block = layout.blocks().getFirst();
        assertEquals(expectedPages, layout.pages().size(), "characters=" + characterCount);
        assertEquals(value, block.lines().stream().flatMap(line -> line.textRuns().stream())
                .map(LaidOutText::text).reduce("", String::concat));
        var lastPage = layout.pages().getLast();
        assertEquals(expectedColumnsOnLastPage, block.lines().stream()
                .filter(line -> line.y() >= lastPage.contentY()
                        && line.y() < lastPage.contentY() + lastPage.contentHeight())
                .map(LaidOutLine::x).distinct().count(), "characters=" + characterCount);
    }

    private static LaidOutPage pageContaining(LaidOutDocument document, int y) {
        return document.pages().stream().filter(page -> y >= page.y() && y < page.y() + page.height())
                .findFirst().orElseThrow();
    }

    private static final class SizedTextMeasurer implements TextMeasurer {
        public int measureWidth(String value, TextStyle style) {
            return Math.max(1, Math.round(value.length() * style.format().fontSizeHalfPoints().orElse(20) / 20.0f));
        }
        public int lineHeight(TextStyle style) {
            return Math.max(1, Math.round(10 * style.format().fontSizeHalfPoints().orElse(20) / 20.0f));
        }
    }
}
