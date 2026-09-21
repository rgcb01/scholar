package dev.rgcb.scholar.application;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.client.DevelopmentDocument;
import dev.rgcb.scholar.clipboard.DocumentFragmentClipboardPayload;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import dev.rgcb.scholar.persistence.PersistenceResult;
import dev.rgcb.scholar.transfer.ExtractionResult;
import dev.rgcb.scholar.transfer.FragmentExtractionRequest;
import dev.rgcb.scholar.transfer.FragmentExtractor;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M30GoldenScenarioTest {
    @TempDir Path directory;

    @Test void ieeeScientificDocumentPersistsReflowsTransfersAndUndoesDeterministically() {
        var source = DevelopmentDocument.createVisualQa();
        assertEquals(DocumentTemplateId.IEEE_STYLE, source.settings().template());
        assertEquals(1, source.settings().columns().count());
        var sectionIndex = java.util.stream.IntStream.range(0, source.blocks().size())
                .filter(index -> source.blocks().get(index).equals(new LayoutSectionBreak(ColumnLayout.two())))
                .findFirst().orElseThrow();
        assertTrue(source.blocks().stream().anyMatch(PageBreak.class::isInstance));
        assertTrue(source.blocks().stream().anyMatch(block -> block instanceof FigureBlock figure
                && figure.span() == ContentSpan.PAGE_WIDTH));
        assertTrue(DocumentValidator.validate(source).isValid());

        var engine = new DocumentLayoutEngine();
        TextMeasurer text = new DeterministicTextMeasurer();
        var firstLayout = engine.layoutPaginated(source, text,
                (value, kind) -> new MathTextMetrics(value.length() * 5, 8, 3));
        assertTrue(firstLayout.pages().size() > 2);
        var sectionTop = firstLayout.blocks().get(sectionIndex).y();
        var sectionColumns = firstLayout.pages().getFirst().columns().stream()
                .filter(column -> column.y() == sectionTop).toList();
        assertEquals(2, sectionColumns.size());
        assertTrue(firstLayout.blocks().stream().skip(sectionIndex + 1)
                .flatMap(block -> block.lines().stream())
                .anyMatch(line -> line.x() == sectionColumns.get(1).x() && line.y() >= sectionTop));
        assertTrue(firstLayout.blocks().stream().flatMap(block -> block.lines().stream())
                .anyMatch(line -> line.y() == firstLayout.pages().get(1).contentY()));

        var repository = new FileScholarDocumentRepository(directory);
        var created = success(repository.createDocument("M30 IEEE Golden", source));
        var reopened = success(new ScholarApplication(new FileScholarDocumentRepository(directory))
                .openDocument(created.descriptor().id())).session().current().document();
        assertEquals(source, reopened);
        assertEquals(firstLayout, engine.layoutPaginated(reopened, text,
                (value, kind) -> new MathTextMetrics(value.length() * 5, 8, 3)));

        var extraction = assertInstanceOf(ExtractionResult.Success.class,
                new FragmentExtractor().extract(source, new FragmentExtractionRequest.Blocks(List.of(0, 1, 2))));
        var destination = new EditorSession(new Document(List.of(paragraph("Destination"))), 0);
        assertTrue(destination.pasteFromClipboard(Optional.of(new DocumentFragmentClipboardPayload(
                extraction.fragment(), extraction.sourceMetadata())), "Scholar typesetting"));
        assertEquals(DocumentSettings.blank(), destination.current().document().settings());
        assertTrue(destination.current().document().blocks().stream().anyMatch(block -> block instanceof Paragraph paragraph
                && paragraph.style() == SemanticStyle.TITLE));
        var pasted = destination.current().document();
        assertTrue(destination.undo());
        assertTrue(destination.redo());
        assertEquals(pasted, destination.current().document());
        assertTrue(DocumentValidator.validate(destination.current().document()).isValid());
    }

    private static Paragraph paragraph(String value) {
        return new Paragraph(new InlineContent(List.of(new Text(value, Set.of()))));
    }

    private static <T> T success(PersistenceResult<T> result) {
        if (result instanceof PersistenceResult.Success<T> success) {
            return success.value();
        }
        fail("Expected persistence success: " + result);
        throw new AssertionError();
    }

    private static final class DeterministicTextMeasurer implements TextMeasurer {
        public int measureWidth(String value, TextStyle style) {
            return Math.max(1, Math.round(value.length() * 5 * style.format().fontSizeHalfPoints().orElse(20) / 20.0f));
        }

        public int lineHeight(TextStyle style) {
            return Math.max(1, Math.round(10 * style.format().fontSizeHalfPoints().orElse(20) / 20.0f));
        }
    }
}
