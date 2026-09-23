package dev.rgcb.scholar.typography;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.client.render.DocumentViewTransform;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextFormat;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.editor.CaretGeometryResolver;
import dev.rgcb.scholar.editor.DocumentPosition;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.layout.TextCaretMetrics;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScientificLineMetricsTest {
    private final ScholarTypography typography = ScholarTypography.defaultProfile();
    private final TextMeasurer measurer = new TextMeasurer() {
        @Override public int measureWidth(String text, TextStyle style) {
            var scale = style.marks().contains(TextMark.SUBSCRIPT) || style.marks().contains(TextMark.SUPERSCRIPT)
                    ? ScholarTypography.SCRIPT_SCALE : 1.0f;
            return Math.round(text.length() * 10 * scale);
        }

        @Override public int lineHeight(TextStyle style) {
            return typography.documentLineHeight(style, 9, typography.resolve(style.role(), style.marks()).lineHeightAdjustment());
        }

        @Override public TextCaretMetrics caretMetrics(TextStyle style, int lineHeight) {
            return typography.documentCaretMetrics(style, lineHeight);
        }
    };

    @Test
    void lineBoxContainsNormalSubscriptAndSuperscriptExtents() {
        var body = TextStyle.paragraph();
        var height = measurer.lineHeight(body);
        assertTrue(height >= Math.ceil(3 + 11 + 1));
        assertTrue(height >= Math.ceil(3 + 3 + 11 * ScholarTypography.SCRIPT_SCALE + 1));
        assertEquals(height, measurer.lineHeight(body.withMarks(Set.of(TextMark.SUBSCRIPT))));
        assertEquals(height, measurer.lineHeight(body.withMarks(Set.of(TextMark.SUPERSCRIPT))));
        assertEquals(11, typography.documentGlyphEm(9));
    }

    @Test void opticalCaretBoundsAreIndependentOfReservedScriptSpaceAndHeadingLeading() {
        var body = TextStyle.paragraph();
        var heading = TextStyle.heading(1);
        var superscript = body.withMarks(Set.of(TextMark.SUPERSCRIPT));
        var subscript = body.withMarks(Set.of(TextMark.SUBSCRIPT));
        for (var style : List.of(body, heading, superscript, subscript)) {
            var metrics = typography.documentCaretMetrics(style, measurer.lineHeight(style));
            assertEquals(-1, metrics.topInset());
            assertEquals(10, metrics.height());
            assertTrue(metrics.height() < measurer.lineHeight(style));
        }
        var large = body.withFormat(new TextFormat(Optional.empty(), Optional.of(40)));
        var metrics = typography.documentCaretMetrics(large, measurer.lineHeight(large));
        assertEquals(-2, metrics.topInset());
        assertEquals(20, metrics.height());
    }

    @Test
    void repeatedUnicodeScriptLinesDoNotOverlapAndCaretUsesSameRuns() {
        var paragraph = new Paragraph(new InlineContent(List.of(
                new Text("H₂O H₂O H₂O H₂O x² x² x² x² CO₂ + H₂O m/s²", Set.of()))));
        var layout = new DocumentLayoutEngine().layout(new Document(List.of(paragraph)), 100, measurer);
        var lines = layout.blocks().getFirst().lines();
        assertTrue(lines.size() >= 3);
        for (var i = 1; i < lines.size(); i++) {
            assertTrue(lines.get(i).y() >= lines.get(i - 1).y() + lines.get(i - 1).height());
            assertTrue(lines.get(i).height() >= measurer.lineHeight(TextStyle.paragraph()));
        }
        var caret = new CaretGeometryResolver().resolve(new DocumentPosition(0, 3), layout, measurer);
        var view = new DocumentViewTransform(40, 60, 1.5);
        assertEquals(40 + caret.x(), view.logicalX(view.screenX(40 + caret.x())), 1.0e-9);
    }

    @Test
    void caretAfterExplicitScriptsMatchesMeasuredGlyphAdvance() {
        var paragraph = new Paragraph(new InlineContent(List.of(
                new Text("H", Set.of()), new Text("2", Set.of(TextMark.SUBSCRIPT)),
                new Text("O", Set.of()), new Text("x", Set.of()),
                new Text("2", Set.of(TextMark.SUPERSCRIPT)))));
        var layout = new DocumentLayoutEngine().layout(new Document(List.of(paragraph)), 150, measurer);
        var resolver = new CaretGeometryResolver();
        assertEquals(18, resolver.resolve(new DocumentPosition(0, 2), layout, measurer).x());
        assertEquals(46, resolver.resolve(new DocumentPosition(0, 5), layout, measurer).x());
        assertEquals(measurer.lineHeight(TextStyle.paragraph()), layout.blocks().getFirst().lines().getFirst().height());
    }

    @Test void caretVisualBoxUsesGlyphMetricsAndOneZoomTransformForProseScriptsAndEmptyText() {
        var paragraph = new Paragraph(new InlineContent(List.of(new Text("Normal H₂O x² m/s²", Set.of()))));
        var scripts = new Paragraph(new InlineContent(List.of(new Text("x", Set.of()),
                new Text("2", Set.of(TextMark.SUPERSCRIPT)))));
        for (var content : List.of(paragraph, scripts, new Paragraph(new InlineContent(List.of())))) {
            var layout = new DocumentLayoutEngine().layout(new Document(List.of(content)), 150, measurer);
            var line = layout.blocks().getFirst().lines().getLast();
            var caret = new CaretGeometryResolver().resolve(new DocumentPosition(0,
                    content.content().nodes().stream().mapToInt(node -> ((Text) node).content().length()).sum()), layout, measurer);
            assertEquals(line.y() - 1, caret.y());
            assertEquals(10, caret.height());
            assertEquals(line.y() + 9, caret.bottom());
            assertTrue(caret.height() < line.height());
            for (var zoom : new double[]{0.75, 1.0, 1.25, 1.5, 2.0}) {
                var view = new DocumentViewTransform(40, 60, zoom);
                var top = view.screenY(60 + caret.y() - 17);
                var bottom = view.screenY(60 + caret.bottom() - 17);
                assertEquals(10 * zoom, bottom - top, 1.0e-9);
                var finalRect = view.caretRect(caret, 40, 60, 17);
                assertEquals(10 * zoom, finalRect.bottom() - finalRect.top(), 1.0);
                assertEquals(40 + caret.x(), view.logicalX(view.screenX(40 + caret.x())), 1.0e-9);
            }
        }
    }

    @Test void wrappedAndHeadingCaretsUseTheirOwnLogicalLineBoxes() {
        var wrapped = new Paragraph(new InlineContent(List.of(new Text("H₂O x² m/s² ".repeat(8), Set.of()))));
        var layout = new DocumentLayoutEngine().layout(new Document(List.of(wrapped)), 90, measurer);
        var caret = new CaretGeometryResolver().resolve(new DocumentPosition(0, 24), layout, measurer);
        assertTrue(caret.y() > layout.blocks().getFirst().lines().getFirst().y());
        assertEquals(10, caret.height());
        assertFinalScreenHeight(caret);
        var heading = new dev.rgcb.scholar.document.Heading(2,
                new InlineContent(List.of(new Text("Heading", Set.of()))));
        var headingLayout = new DocumentLayoutEngine().layout(new Document(List.of(heading)), 150, measurer);
        var headingCaret = new CaretGeometryResolver().resolve(new DocumentPosition(0, 7), headingLayout, measurer);
        assertEquals(10, headingCaret.height());
        assertFinalScreenHeight(headingCaret);
    }

    private static void assertFinalScreenHeight(dev.rgcb.scholar.editor.CaretGeometry caret) {
        for (var zoom : new double[]{0.75, 1.0, 1.25, 1.5, 2.0}) {
            var rect = new DocumentViewTransform(40, 60, zoom).caretRect(caret, 40, 60, 37);
            assertEquals(caret.height() * zoom, rect.bottom() - rect.top(), 1.0);
        }
    }
}
