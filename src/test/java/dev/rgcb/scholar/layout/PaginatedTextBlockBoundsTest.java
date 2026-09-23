package dev.rgcb.scholar.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.ColumnLayout;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.LayoutSectionBreak;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.application.ScholarDocuments;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PaginatedTextBlockBoundsTest {
    private static final TextMeasurer MEASURER = new TextMeasurer() {
        @Override public int measureWidth(String text, TextStyle style) { return text.length() * 8; }
        @Override public int lineHeight(TextStyle style) { return 15; }
    };

    @Test void oneParagraphCrossingColumnsHasNonnegativeBoundsContainingEveryPlacedLine() {
        var paragraph = new Paragraph(new InlineContent(List.of(new Text(
                "Lorem ipsum dolor sit amet consectetur adipiscing elit. ".repeat(200), Set.of()))));
        var document = new Document(List.of(new LayoutSectionBreak(ColumnLayout.two()), paragraph));
        var layout = new DocumentLayoutEngine().layoutPaginated(document, MEASURER, null);
        var block = layout.blocks().get(1);
        var lines = block.lines();

        assertTrue(lines.size() > 20);
        assertTrue(lines.stream().anyMatch(line -> line.x() > lines.getFirst().x()
                && line.y() < lines.getFirst().y() + 300));
        assertTrue(block.height() >= 0);
        for (var line : lines) {
            assertTrue(block.x() <= line.x());
            assertTrue(block.y() <= line.y());
            assertTrue(block.x() + block.width() >= line.x() + line.width());
            assertTrue(block.y() + block.height() >= line.y() + line.height());
        }
        assertEquals(lines.stream().mapToInt(LaidOutLine::y).min().orElseThrow(), block.y());
    }

    @Test void productionM34SampleCanBePaginatedWithoutNegativeTextBounds() {
        var layout = new DocumentLayoutEngine().layoutPaginated(ScholarDocuments.m34Readability(), MEASURER,
                (content, kind) -> new MathTextMetrics(content.length() * 8, 8, 4));
        assertTrue(layout.blocks().size() > 20);
        assertTrue(layout.blocks().stream().allMatch(block -> block.height() >= 0));
        for (var block : layout.blocks()) {
            for (var line : block.lines()) {
                assertTrue(block.y() <= line.y());
                assertTrue(block.y() + block.height() >= line.y() + line.height());
            }
        }
    }
}
