package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.transfer.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class M30EditorSemanticsTest {
    @Test void pageBreakAndPageSettingsAreSingleUndoableSemanticTransactions() {
        var session = session(paragraph("abc"));
        assertTrue(session.insertPageBreak());
        assertEquals(1, session.undoDepth());
        assertTrue(session.current().document().blocks().stream().anyMatch(PageBreak.class::isInstance));
        assertTrue(session.undo());
        assertFalse(session.current().document().blocks().stream().anyMatch(PageBreak.class::isInstance));

        assertTrue(session.updateDocumentSettings(settings -> settings.withColumns(ColumnLayout.two())));
        assertEquals(2, session.current().document().settings().columns().count());
        assertTrue(session.undo());
        assertEquals(1, session.current().document().settings().columns().count());
        assertTrue(session.redo());
        assertEquals(2, session.current().document().settings().columns().count());
    }

    @Test void localFormattingSurvivesSplitAndTransferExtraction() {
        var formatted = new Text("science", Set.of(TextMark.UNDERLINE),
                new TextFormat(Optional.of(ScholarFontFamily.SOURCE_SANS_3), Optional.of(28)));
        var paragraph = new Paragraph(new InlineContent(List.of(formatted)), SemanticStyle.ABSTRACT,
                new ParagraphFormat(Optional.of(ParagraphAlignment.JUSTIFIED), Optional.of(1200), Optional.empty(),
                        Optional.of(4), Optional.empty(), Optional.empty(), Optional.of(3)));
        var document = new Document(List.of(paragraph));
        var editor = new DocumentEditor();
        var state = new EditorState(document, new TextSelection(new DocumentPosition(0, 3), new DocumentPosition(0, 3)), Optional.empty());
        var split = editor.insertParagraphBreak(state, Set.of());
        assertEquals(SemanticStyle.ABSTRACT, ((Paragraph) split.document().blocks().get(0)).style());
        assertEquals(28, ((Text) ((Paragraph) split.document().blocks().get(1)).content().nodes().getFirst()).format().fontSizeHalfPoints().orElseThrow());

        var extracted = new FragmentExtractor().extract(document, new FragmentExtractionRequest.Blocks(List.of(0)));
        var success = assertInstanceOf(ExtractionResult.Success.class, extracted);
        var root = (Paragraph) ((FragmentContent.Blocks) success.fragment().content()).roots().getFirst();
        assertEquals(paragraph, root);
    }

    @Test void applyingSameSemanticSettingsIsHistoryNoOp() {
        var session = session(paragraph("abc"));
        assertFalse(session.updateDocumentSettings(settings -> settings));
        assertEquals(0, session.undoDepth());
    }

    @Test void deletingLayoutSectionBreakReflowsSemanticallyAndUndoRedoRestoreItExactly() {
        var marker = new LayoutSectionBreak(ColumnLayout.two());
        var original = new Document(List.of(paragraph("before"), marker, paragraph("after")));
        var session = new EditorSession(new EditorState(original, new BlockSelection(1), Optional.empty()));
        assertTrue(session.deleteForward());
        assertEquals(List.of(paragraph("before"), paragraph("after")), session.current().document().blocks());
        var deleted = session.current();
        assertTrue(session.undo());
        assertEquals(original, session.current().document());
        assertTrue(session.redo());
        assertEquals(deleted, session.current());
    }

    @Test void structuredTransferPreservesPageBreakAndLocalFormattingButNotDocumentSettings() {
        var formatted = new Paragraph(new InlineContent(List.of(new Text("formatted", Set.of(TextMark.UNDERLINE),
                new TextFormat(Optional.of(ScholarFontFamily.SOURCE_SANS_3), Optional.of(26))))),
                SemanticStyle.KEYWORDS, new ParagraphFormat(Optional.of(ParagraphAlignment.RIGHT), Optional.of(1500),
                        Optional.of(2), Optional.of(3), Optional.of(4), Optional.of(5), Optional.of(6)));
        var sectionBreak = new LayoutSectionBreak(ColumnLayout.two());
        var source = new Document(List.of(formatted, sectionBreak, new PageBreak()), List.of(),
                DocumentTemplates.settings(DocumentTemplateId.IEEE_STYLE));
        var extraction = assertInstanceOf(ExtractionResult.Success.class,
                new FragmentExtractor().extract(source, new FragmentExtractionRequest.Blocks(List.of(0, 1, 2))));
        var destination = new Document(List.of(paragraph("destination")));
        var plan = assertInstanceOf(PlanningResult.Success.class,
                new TransferPlanner().plan(extraction.fragment(),
                        new TransferContext(destination, Optional.empty(), SourceTransferMetadata.unknown()))).plan();
        var transfer = assertInstanceOf(MaterializationResult.Success.class,
                new TransferMaterializer().materialize(extraction.fragment(), plan)).transfer();
        var blocks = assertInstanceOf(FragmentContent.Blocks.class, transfer.content()).roots();
        assertEquals(formatted, blocks.getFirst());
        assertSame(sectionBreak, blocks.get(1));
        assertInstanceOf(PageBreak.class, blocks.get(2));
        assertEquals(DocumentSettings.blank(), destination.settings());
    }

    @Test void transferAndHistoryPreserveExactAutomaticAndExplicitTableSpans() {
        var tables = List.of(TableBlock.empty(1, 1), TableBlock.empty(1, 1).withSpan(ContentSpan.COLUMN),
                TableBlock.empty(1, 1).withSpan(ContentSpan.PAGE_WIDTH));
        var source = new Document(List.copyOf(tables));
        var extraction = assertInstanceOf(ExtractionResult.Success.class,
                new FragmentExtractor().extract(source, new FragmentExtractionRequest.Blocks(List.of(0, 1, 2))));
        var destination = new Document(List.of(paragraph("destination")));
        var plan = assertInstanceOf(PlanningResult.Success.class,
                new TransferPlanner().plan(extraction.fragment(),
                        new TransferContext(destination, Optional.empty(), SourceTransferMetadata.unknown()))).plan();
        var transfer = assertInstanceOf(MaterializationResult.Success.class,
                new TransferMaterializer().materialize(extraction.fragment(), plan)).transfer();
        var transferred = assertInstanceOf(FragmentContent.Blocks.class, transfer.content()).roots();

        assertEquals(List.of(ContentSpan.AUTO, ContentSpan.COLUMN, ContentSpan.PAGE_WIDTH), transferred.stream()
                .map(TableBlock.class::cast).map(TableBlock::span).toList());

        var session = new EditorSession(new EditorState(source, new BlockSelection(0), Optional.empty()));
        assertTrue(session.deleteForward());
        assertTrue(session.undo());
        assertEquals(tables, session.current().document().blocks());
        assertTrue(session.redo());
        assertEquals(List.of(tables.get(1), tables.get(2)), session.current().document().blocks());
    }

    private static EditorSession session(Paragraph paragraph) {
        return new EditorSession(new EditorState(new Document(List.of(paragraph)),
                new TextSelection(new DocumentPosition(0, 1), new DocumentPosition(0, 1)), Optional.empty()));
    }

    private static Paragraph paragraph(String value) {
        return new Paragraph(new InlineContent(List.of(new Text(value, Set.of()))));
    }
}
