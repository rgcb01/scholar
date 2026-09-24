package dev.rgcb.scholar.application;

import dev.rgcb.scholar.interchange.MarkdownDocumentExporter;
import dev.rgcb.scholar.interchange.PdfDocumentExporter;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import dev.rgcb.scholar.client.DevelopmentDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class M36ReadabilityExportTest {
    @Test void electricalAndMechanicalDiagramLayoutsCanBePaintedIntoPdf() throws Exception {
        var source = DevelopmentDocument.createVisualQa();
        var layout = new DocumentLayoutEngine().layoutPaginated(source, new TextMeasurer() {
            public int measureWidth(String value, TextStyle style) { return Math.max(1, value.length() * 5); }
            public int lineHeight(TextStyle style) { return 10; }
        }, (value, kind) -> new MathTextMetrics(Math.max(1, value.length() * 5), 8, 3));
        var bytes = new PdfDocumentExporter().export(source, layout);
        java.nio.file.Files.write(java.nio.file.Path.of("build", "m36-diagrams.pdf"), bytes);
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(layout.pages().size(), pdf.getNumberOfPages());
            assertTrue(bytes.length > 10_000);
        }
    }

    @Test void readabilitySampleExportsPagesTablesEquationsFigureAndReferences() throws Exception {
        var source = M34ReadabilityDocument.create();
        var markdown = new MarkdownDocumentExporter().export(source);
        assertTrue(markdown.contains("# Typography"));
        assertTrue(markdown.contains("**Figure 1.**"));
        assertTrue(markdown.contains("Measured response and quadratic fit"));
        assertFalse(markdown.contains("m34-figure"));
        java.awt.Font regular;
        java.awt.Font mathFont;
        try (var regularInput = M36ReadabilityExportTest.class.getResourceAsStream("/assets/scholar/font/source_sans_3/regular.ttf");
             var mathInput = M36ReadabilityExportTest.class.getResourceAsStream("/assets/scholar/font/noto_sans_math/regular.ttf")) {
            regular = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, regularInput);
            mathFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, mathInput);
        }
        var context = new java.awt.font.FontRenderContext(null, true, true);
        TextMeasurer text = new TextMeasurer() {
            public int measureWidth(String value, TextStyle style) {
                var size = (style.headingLevel() == 1 ? 14f : style.headingLevel() == 2 ? 12f : 10f)
                        * style.format().fontSizeHalfPoints().orElse(20) / 20f;
                return Math.max(1, (int) Math.ceil(regular.deriveFont(size).getStringBounds(value, context).getWidth()));
            }
            public int lineHeight(TextStyle style) { return 11; }
        };
        var layout = new DocumentLayoutEngine().layoutPaginated(source, text,
                (value, kind) -> new MathTextMetrics(Math.max(1, (int) Math.ceil(mathFont.deriveFont(10f)
                        .getStringBounds(value, context).getWidth())), 8, 3));
        var pdfBytes = new PdfDocumentExporter().export(source, layout);
        java.nio.file.Files.write(java.nio.file.Path.of("build", "m36-readability.pdf"), pdfBytes);
        try (var pdf = Loader.loadPDF(pdfBytes)) {
            assertEquals(layout.pages().size(), pdf.getNumberOfPages());
            assertTrue(pdf.getNumberOfPages() > 1);
            var content = new PDFTextStripper().getText(pdf);
            assertTrue(content.contains("Typography"));
            assertTrue(content.contains("Quadratic fit"));
            assertTrue(content.contains("Figure 1"));
            assertTrue(content.contains("Measured response with a quadratic fit"));
        }
    }
}
