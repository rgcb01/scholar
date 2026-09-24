package dev.rgcb.scholar.interchange;

import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.layout.*;
import java.util.List;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PdfDocumentExporterTest {
    @Test void customLandscapePageAndFurnitureUseDocumentSettingsNotViewportZoom() throws Exception {
        var settings = new DocumentSettings(DocumentTemplateId.BLANK,
                PaperSize.custom(PhysicalLength.millimetres(180), PhysicalLength.millimetres(240)),
                PageOrientation.LANDSCAPE, PageMargins.narrow(), ColumnLayout.one(),
                new PageDecoration("Lab header", "Lab footer", true));
        var document = new Document(List.of(new Paragraph(new InlineContent(List.of(new Text("Result", Set.of()))))),
                List.of(), settings);
        var layout = new DocumentLayoutEngine().layoutPaginated(document, new TextMeasurer() {
            public int measureWidth(String value, TextStyle style) { return value.length() * 5; }
            public int lineHeight(TextStyle style) { return 11; }
        }, null);
        for (var ignoredZoom : new double[] {0.5, 1.0, 2.0}) {
            try (var pdf = Loader.loadPDF(new PdfDocumentExporter().export(document, layout))) {
                assertEquals(240 * 72 / 25.4, pdf.getPage(0).getMediaBox().getWidth(), 0.1);
                assertEquals(180 * 72 / 25.4, pdf.getPage(0).getMediaBox().getHeight(), 0.1);
                var content = new PDFTextStripper().getText(pdf);
                assertTrue(content.contains("Lab header"));
                assertTrue(content.contains("Lab footer"));
                assertTrue(content.contains("1"));
            }
        }
    }

    @Test void exportsPhysicalPageAndUnicodeWithoutEditorChrome() throws Exception {
        var document = new Document(List.of(
                new Paragraph(new InlineContent(List.of(new Text("Greek λ and café", Set.of()))))));
        var layout = new DocumentLayoutEngine().layoutPaginated(document, new TextMeasurer() {
            public int measureWidth(String value, TextStyle style) { return value.length() * 5; }
            public int lineHeight(TextStyle style) { return 11; }
        }, null);
        var bytes = new PdfDocumentExporter().export(document, layout);
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(layout.pages().size(), pdf.getNumberOfPages());
            assertEquals(612f, pdf.getPage(0).getMediaBox().getWidth(), 0.1f);
            assertEquals(792f, pdf.getPage(0).getMediaBox().getHeight(), 0.1f);
            var content = new PDFTextStripper().getText(pdf);
            assertTrue(content.contains("Greek λ"), content);
            assertTrue(content.contains("and café"), content);
            assertFalse(content.contains("ribbon"));
        }
    }
}
