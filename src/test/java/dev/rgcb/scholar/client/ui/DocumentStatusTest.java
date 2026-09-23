package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.PageBreak;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DocumentStatusTest {
    private static final TextMeasurer MEASURER = new TextMeasurer() {
        @Override public int measureWidth(String value, TextStyle style) { return value.length() * 6; }
        @Override public int lineHeight(TextStyle style) { return 12; }
    };

    @Test void countsAuthoredProseButNotDerivedToc() {
        var document = new Document(List.of(
                new Heading(1, text("Scientific heading")),
                new Paragraph(text("H₂O flows here.")),
                new TableOfContentsBlock()));
        assertEquals(5, DocumentStatus.wordCount(document));
        assertEquals(6, DocumentStatus.wordCount(new Document(List.of(
                new Heading(1, text("Scientific heading")),
                new Paragraph(text("H₂O flows here now."))))));
    }

    @Test void pageIndicatorUsesCaretPageOrVisiblePageCoordinate() {
        var document = new Document(List.of(
                new Paragraph(text("First page")), new PageBreak(),
                new Paragraph(text("Second page"))));
        var layout = new DocumentLayoutEngine().layoutPaginated(document, MEASURER, null);
        assertTrue(layout.pages().size() >= 2);
        assertEquals(1, DocumentStatus.pageAt(layout, layout.pages().getFirst().contentY()));
        assertEquals(2, DocumentStatus.pageAt(layout, layout.pages().get(1).contentY()));
    }

    private static InlineContent text(String value) {
        return new InlineContent(List.of(new Text(value, Set.of())));
    }
}
