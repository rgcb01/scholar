package dev.rgcb.scholar.interchange;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.layout.*;
import dev.rgcb.scholar.math.layout.*;
import dev.rgcb.scholar.plot.layout.*;
import dev.rgcb.scholar.diagram.layout.*;
import dev.rgcb.scholar.electrical.layout.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

/** Paints Scholar's existing paginated geometry into physical PDF pages. */
public final class PdfDocumentExporter {
    public byte[] export(Document source, LaidOutDocument layout) throws IOException {
        Objects.requireNonNull(source);
        Objects.requireNonNull(layout);
        if (!layout.paginated()) throw new IllegalArgumentException("PDF requires paginated Scholar layout");
        if (layout.pages().isEmpty() || layout.pages().stream().anyMatch(page ->
                page.width() != source.settings().pageWidth() || page.height() != source.settings().pageHeight())) {
            throw new IllegalArgumentException("PDF layout does not match document page settings");
        }
        var paper = source.settings().paper();
        var widthUm = source.settings().orientation() == dev.rgcb.scholar.document.PageOrientation.PORTRAIT
                ? paper.width().micrometres() : paper.height().micrometres();
        var heightUm = source.settings().orientation() == dev.rgcb.scholar.document.PageOrientation.PORTRAIT
                ? paper.height().micrometres() : paper.width().micrometres();
        var widthPt = (float) (widthUm * 72.0 / 25_400.0);
        var heightPt = (float) (heightUm * 72.0 / 25_400.0);
        try (var pdf = new PDDocument()) {
            var fonts = new Fonts(pdf);
            for (var page : layout.pages()) {
                var pdfPage = new PDPage(new PDRectangle(widthPt, heightPt));
                pdf.addPage(pdfPage);
                try (var stream = new PDPageContentStream(pdf, pdfPage)) {
                    var canvas = new Canvas(stream, fonts, page, widthPt, heightPt);
                    canvas.text(page.headerText(), page.x() + page.width() / 2, page.y() + 8, 8, false, false, true);
                    var footer = page.footerText();
                    if (page.pageNumberVisible()) footer = footer.isEmpty() ? Integer.toString(page.index() + 1) : footer + "  " + (page.index() + 1);
                    canvas.text(footer, page.x() + page.width() / 2, page.y() + page.height() - 14, 8, false, false, true);
                    stream.saveGraphicsState();
                    stream.addRect(0, 0, widthPt, heightPt);
                    stream.clip();
                    for (var block : layout.blocks()) {
                        if (block.y() + block.height() < page.y() || block.y() > page.y() + page.height()) continue;
                        canvas.block(block);
                    }
                    stream.restoreGraphicsState();
                }
            }
            var output = new ByteArrayOutputStream();
            pdf.save(output);
            return output.toByteArray();
        }
    }

    private static final class Fonts {
        private final PDType0Font regular, italic, bold, boldItalic, math;
        private Fonts(PDDocument pdf) throws IOException {
            regular = load(pdf, "/assets/scholar/font/source_sans_3/regular.ttf");
            italic = load(pdf, "/assets/scholar/font/source_sans_3/italic.ttf");
            bold = load(pdf, "/assets/scholar/font/source_sans_3/bold.ttf");
            boldItalic = load(pdf, "/assets/scholar/font/source_sans_3/bold_italic.ttf");
            math = load(pdf, "/assets/scholar/font/noto_sans_math/regular.ttf");
        }
        private static PDType0Font load(PDDocument pdf, String path) throws IOException {
            try (var input = PdfDocumentExporter.class.getResourceAsStream(path)) {
                if (input == null) throw new IOException("Missing licensed Scholar font: " + path);
                return PDType0Font.load(pdf, input, true);
            }
        }
        private PDType0Font select(boolean boldFace, boolean italicFace) {
            return boldFace ? italicFace ? boldItalic : bold : italicFace ? italic : regular;
        }
    }

    private static final class Canvas {
        private static final int[] SERIES_COLORS = {0x1F5E9C, 0xB05A2A, 0x2F7D4A, 0x6E4A9E, 0x9A3E3E, 0x2D7C83};
        private static final dev.rgcb.scholar.typography.ScholarTypography TYPOGRAPHY =
                dev.rgcb.scholar.typography.ScholarTypography.defaultProfile();
        private final PDPageContentStream stream;
        private final Fonts fonts;
        private final LaidOutPage page;
        private final float sx, sy, height;

        private Canvas(PDPageContentStream stream, Fonts fonts, LaidOutPage page, float width, float height) {
            this.stream = stream;
            this.fonts = fonts;
            this.page = page;
            this.sx = width / page.width();
            this.sy = height / page.height();
            this.height = height;
        }

        private float x(double value) { return (float) ((value - page.x()) * sx); }
        private float y(double value) { return height - (float) ((value - page.y()) * sy); }
        private void color(int rgb) throws IOException { stream.setNonStrokingColor(((rgb >>> 16) & 255) / 255f, ((rgb >>> 8) & 255) / 255f, (rgb & 255) / 255f); }

        private void text(String value, double x, double top, float size, boolean bold, boolean italic, boolean centered) throws IOException {
            text(value, x, top, size, bold, italic, centered, -1);
        }

        private void text(String value, double x, double top, float size, boolean bold, boolean italic,
                          boolean centered, int targetWidth) throws IOException {
            text(value, x, top, size, bold, italic, centered, targetWidth, 0x241F1A, false);
        }

        private void text(String value, double x, double top, float size, boolean bold, boolean italic,
                          boolean centered, int targetWidth, int rgb, boolean mathFamily) throws IOException {
            if (value == null || value.isEmpty()) return;
            var font = mathFamily ? fonts.math : fonts.select(bold, italic);
            try { font.encode(value); }
            catch (IllegalArgumentException unsupported) { font = fonts.math; }
            var pointSize = size * sy;
            var px = x(x);
            if (centered) px -= font.getStringWidth(value) / 1000f * pointSize / 2;
            color(rgb);
            stream.beginText();
            stream.setFont(font, pointSize);
            var measured = font.getStringWidth(value) / 1000f * pointSize;
            stream.setHorizontalScaling(targetWidth > 0 && measured > 0
                    ? Math.max(1f, targetWidth * sx / measured * 100f) : 100f);
            stream.newLineAtOffset(px, y(top + size * 0.86));
            stream.showText(value);
            stream.endText();
        }

        private void run(LaidOutText run) throws IOException {
            var marks = run.style().marks();
            var resolved = TYPOGRAPHY.resolve(run.style().role(), marks);
            var heading = run.style().headingLevel();
            var size = (heading == 1 ? 14f : heading == 2 ? 12f : 10f)
                    * run.style().format().fontSizeHalfPoints().orElse(20) / 20f;
            if (marks.contains(TextMark.SUPERSCRIPT) || marks.contains(TextMark.SUBSCRIPT)) size *= 0.75f;
            var top = run.y() + 3 + (marks.contains(TextMark.SUPERSCRIPT) ? -3 : marks.contains(TextMark.SUBSCRIPT) ? 3 : 0);
            text(run.text(), run.x(), top, size, resolved.bold(), resolved.italic(), false, run.width(),
                    resolved.color(), run.style().format().fontFamily().orElse(null)
                            == dev.rgcb.scholar.document.ScholarFontFamily.SCIENTIFIC_MATH);
            if (marks.contains(TextMark.UNDERLINE)) {
                line(run.x(), top + size, run.x() + run.width(), top + size, 0.6f);
            }
        }

        private void lines(java.util.List<LaidOutLine> lines) throws IOException {
            for (var line : lines) for (var run : line.textRuns()) run(run);
        }

        private void line(double x1, double y1, double x2, double y2, float width) throws IOException {
            stream.setStrokingColor(55 / 255f, 58 / 255f, 59 / 255f);
            stream.setLineWidth(Math.max(0.4f, width * sx));
            stream.moveTo(x(x1), y(y1)); stream.lineTo(x(x2), y(y2)); stream.stroke();
        }

        private void gridLine(double x1, double y1, double x2, double y2) throws IOException {
            stream.setStrokingColor(0.82f, 0.83f, 0.82f);
            stream.setLineWidth(0.4f);
            stream.moveTo(x(x1), y(y1)); stream.lineTo(x(x2), y(y2)); stream.stroke();
        }

        private void rect(double x, double y, double w, double h, boolean fill) throws IOException {
            stream.addRect(x(x), y(y + h), (float) (w * sx), (float) (h * sy));
            if (fill) { color(0xEDEFF0); stream.fill(); }
            else { stream.setStrokingColor(86 / 255f, 89 / 255f, 90 / 255f); stream.setLineWidth(0.5f); stream.stroke(); }
        }

        private void circle(double cx, double cy, double radius, boolean fill) throws IOException {
            var c = radius * 0.55228475;
            stream.moveTo(x(cx + radius), y(cy));
            stream.curveTo(x(cx + radius), y(cy - c), x(cx + c), y(cy - radius), x(cx), y(cy - radius));
            stream.curveTo(x(cx - c), y(cy - radius), x(cx - radius), y(cy - c), x(cx - radius), y(cy));
            stream.curveTo(x(cx - radius), y(cy + c), x(cx - c), y(cy + radius), x(cx), y(cy + radius));
            stream.curveTo(x(cx + c), y(cy + radius), x(cx + radius), y(cy + c), x(cx + radius), y(cy));
            stream.closePath();
            if (fill) stream.fill();
            else { stream.setStrokingColor(0.25f, 0.27f, 0.28f); stream.setLineWidth(0.7f); stream.stroke(); }
        }

        private void block(LaidOutBlock block) throws IOException {
            if (block.math().isPresent()) {
                var root = block.math().orElseThrow().root();
                math(root, block.x() + Math.max(0, (block.width() - root.width()) / 2), block.y() + root.ascent());
            } else if (block.figure().isPresent()) {
                var figure = block.figure().orElseThrow();
                block(figure.content());
                lines(figure.captionLines());
            } else if (block.table().isPresent()) table(block.table().orElseThrow());
            else if (block.plot().isPresent()) plot(block.plot().orElseThrow());
            else if (block.diagram().isPresent()) diagram(block.diagram().orElseThrow());
            else lines(block.lines());
        }

        private void table(LaidOutTable table) throws IOException {
            for (var row : table.rows()) for (var cell : row.cells()) {
                if (row.rowIndex() < table.headerRowCount()) rect(cell.x(), cell.y(), cell.width(), cell.height(), true);
                rect(cell.x(), cell.y(), cell.width(), cell.height(), false);
                lines(cell.lines());
            }
        }

        private void math(MathBox box, double left, double baseline) throws IOException {
            for (var primitive : box.primitives()) {
                if (primitive instanceof MathGlyphRun glyph) {
                    text(glyph.content(), left + glyph.x(), baseline + glyph.baselineOffset() - glyph.ascent(),
                            (float) (10 * glyph.scale()), false, false, false, -1, 0x241F1A, true);
                } else if (primitive instanceof MathHorizontalRule rule) {
                    line(left + rule.x(), baseline + rule.y(), left + rule.x() + rule.width(), baseline + rule.y(), rule.thickness());
                } else if (primitive instanceof MathLineSegment segment) {
                    line(left + segment.x1(), baseline + segment.y1(), left + segment.x2(), baseline + segment.y2(), segment.thickness());
                }
            }
            for (var child : box.children()) math(child.box(), left + child.x(), baseline + child.baselineOffset());
        }

        private void plot(LaidOutPlot plot) throws IOException {
            var left = plot.plotAreaX(); var top = plot.plotAreaY();
            var right = left + plot.plotAreaWidth(); var bottom = top + plot.plotAreaHeight();
            if (plot.gridVisible()) {
                for (var tick : plot.xTicks()) gridLine(tick.coordinate(), top, tick.coordinate(), bottom);
                for (var tick : plot.yTicks()) gridLine(left, tick.coordinate(), right, tick.coordinate());
            }
            rect(left, top, plot.plotAreaWidth(), plot.plotAreaHeight(), false);
            stream.saveGraphicsState();
            stream.addRect(x(left), y(bottom), plot.plotAreaWidth() * sx, plot.plotAreaHeight() * sy);
            stream.clip();
            for (var series : plot.series()) {
                if (series.kind() == dev.rgcb.scholar.plot.PlotSeriesKind.LINE) {
                    for (var segment : series.lineSegments()) seriesLine(segment.x1(), segment.y1(), segment.x2(), segment.y2(), series.style());
                } else for (var point : series.points()) marker(point.x(), point.y(), series.style());
            }
            stream.restoreGraphicsState();
            for (var tick : plot.xTicks()) label(tick.label());
            for (var tick : plot.yTicks()) label(tick.label());
            if (plot.title().isPresent()) label(plot.title().orElseThrow());
            if (plot.xAxisLabel().isPresent()) label(plot.xAxisLabel().orElseThrow());
            if (plot.yAxisLabel().isPresent()) label(plot.yAxisLabel().orElseThrow());
            if (plot.legend().isPresent()) {
                var legend = plot.legend().orElseThrow();
                stream.addRect(x(legend.x()), y(legend.y() + legend.height()),
                        legend.width() * sx, legend.height() * sy);
                color(0xF6F2E8);
                stream.fill();
                rect(legend.x(), legend.y(), legend.width(), legend.height(), false);
                for (var item : legend.items()) {
                    if (item.kind() == dev.rgcb.scholar.plot.PlotSeriesKind.LINE)
                        seriesLine(item.sampleX1(), item.sampleY(), item.sampleX2(), item.sampleY(), item.style());
                    else marker((item.sampleX1() + item.sampleX2()) / 2.0, item.sampleY(), item.style());
                    label(item.label());
                }
            }
        }

        private void seriesLine(double x1, double y1, double x2, double y2, LaidOutPlotSeriesStyle style) throws IOException {
            var rgb = SERIES_COLORS[Math.floorMod(style.styleIndex(), SERIES_COLORS.length)];
            stream.setStrokingColor(((rgb >>> 16) & 255) / 255f, ((rgb >>> 8) & 255) / 255f, (rgb & 255) / 255f);
            stream.setLineWidth(Math.max(0.8f, sx));
            var dash = switch (style.linePattern()) {
                case SOLID -> new float[0];
                case DASHED -> new float[] {5, 3};
                case DOTTED -> new float[] {1, 3};
                case DASH_DOT -> new float[] {6, 2, 1, 2};
                case LONG_DASH -> new float[] {8, 4};
                case DENSE_DOT -> new float[] {1, 2};
            };
            stream.setLineDashPattern(dash, 0);
            stream.moveTo(x(x1), y(y1)); stream.lineTo(x(x2), y(y2)); stream.stroke();
            stream.setLineDashPattern(new float[0], 0);
        }

        private void marker(double cx, double cy, LaidOutPlotSeriesStyle style) throws IOException {
            var rgb = SERIES_COLORS[Math.floorMod(style.styleIndex(), SERIES_COLORS.length)];
            color(rgb);
            switch (style.markerShape()) {
                case SQUARE -> { stream.addRect(x(cx - 2), y(cy + 2), 4 * sx, 4 * sy); stream.fill(); }
                case CIRCLE -> circle(cx, cy, 2.2, true);
                case DIAMOND -> {
                    stream.moveTo(x(cx), y(cy - 2.5)); stream.lineTo(x(cx + 2.5), y(cy));
                    stream.lineTo(x(cx), y(cy + 2.5)); stream.lineTo(x(cx - 2.5), y(cy));
                    stream.closePath(); stream.fill();
                }
                case TRIANGLE -> {
                    stream.moveTo(x(cx), y(cy - 2.5)); stream.lineTo(x(cx + 2.5), y(cy + 2.5));
                    stream.lineTo(x(cx - 2.5), y(cy + 2.5)); stream.closePath(); stream.fill();
                }
                case CROSS -> { seriesLine(cx - 2, cy, cx + 2, cy, style); seriesLine(cx, cy - 2, cx, cy + 2, style); }
                case X -> { seriesLine(cx - 2, cy - 2, cx + 2, cy + 2, style); seriesLine(cx - 2, cy + 2, cx + 2, cy - 2, style); }
            }
        }

        private void label(LaidOutPlotLabel label) throws IOException {
            text(label.text(), label.x(), label.y(), 8, false, false, false, label.width());
        }

        private void diagram(LaidOutDiagram diagram) throws IOException {
            rect(diagram.workspaceX(), diagram.workspaceY(), diagram.workspaceWidth(), diagram.workspaceHeight(), false);
            for (var connection : diagram.connections()) {
                for (var i = 1; i < connection.path().size(); i++) {
                    var a = connection.path().get(i - 1); var b = connection.path().get(i);
                    line(a.x(), a.y(), b.x(), b.y(), 1);
                }
                if (connection.label().isPresent()) electricalLabel(connection.label().orElseThrow());
            }
            for (var node : diagram.nodes()) {
                rect(node.x(), node.y(), node.width(), node.height(), false);
                if (node.label().isPresent()) diagramLabel(node.label().orElseThrow());
            }
            for (var component : diagram.electricalComponents()) {
                for (var primitive : component.primitives()) {
                    if (primitive instanceof LaidOutElectricalLine segment) line(segment.start().x(), segment.start().y(), segment.end().x(), segment.end().y(), 1);
                    else if (primitive instanceof LaidOutElectricalPolyline polyline) for (var i = 1; i < polyline.points().size(); i++) {
                        var a = polyline.points().get(i - 1); var b = polyline.points().get(i);
                        line(a.x(), a.y(), b.x(), b.y(), 1);
                    } else if (primitive instanceof LaidOutElectricalCircle circle) circle(circle.center().x(), circle.center().y(), circle.radius(), false);
                }
                if (component.referenceDesignator().isPresent()) electricalLabel(component.referenceDesignator().orElseThrow());
                if (component.valueLabel().isPresent()) electricalLabel(component.valueLabel().orElseThrow());
            }
            for (var junction : diagram.electricalJunctions()) {
                color(0x1F5E9C);
                circle(junction.centerX(), junction.centerY(), 2, true);
                if (junction.netLabel().isPresent()) electricalLabel(junction.netLabel().orElseThrow());
            }
            for (var primitive : diagram.mechanicalPrimitives()) mechanicalPrimitive(primitive);
            for (var dimension : diagram.mechanicalDimensions()) mechanicalDimension(dimension);
            for (var symbol : diagram.mechanicalSymbols()) mechanicalSymbol(symbol);
            for (var constraint : diagram.mechanicalConstraints()) {
                var b = constraint.bounds();
                line(b.x(), b.y() + b.height() / 2.0, b.x() + b.width(), b.y() + b.height() / 2.0, 0.7f);
            }
            for (var annotation : diagram.mechanicalAnnotations()) diagramLabel(annotation.label());
            for (var reference : diagram.mechanicalPartReferences()) {
                var balloon = reference.balloonBounds();
                var cx = balloon.x() + balloon.width() / 2.0;
                var cy = balloon.y() + balloon.height() / 2.0;
                line(cx, cy, reference.targetX(), reference.targetY(), 0.7f);
                circle(cx, cy, Math.max(3, Math.min(balloon.width(), balloon.height()) / 2.0), false);
                diagramLabel(reference.itemLabel());
            }
            if (diagram.title().isPresent()) diagramLabel(diagram.title().orElseThrow());
        }

        private void mechanicalPrimitive(dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalPrimitive primitive) throws IOException {
            var b = primitive.bounds();
            var left = b.x(); var top = b.y(); var right = left + b.width(); var bottom = top + b.height();
            var cx = (left + right) / 2.0; var cy = (top + bottom) / 2.0;
            var vertical = primitive.orientation() == dev.rgcb.scholar.mechanical.MechanicalOrientation.DEG_90;
            switch (primitive.kind()) {
                case LINE -> { if (vertical) line(cx, top, cx, bottom, 0.8f); else line(left, cy, right, cy, 0.8f); }
                case CENTERLINE -> {
                    if (vertical) for (var pos = top; pos < bottom; pos += 9) line(cx, pos, cx, Math.min(bottom, pos + 5), 0.6f);
                    else for (var pos = left; pos < right; pos += 9) line(pos, cy, Math.min(right, pos + 5), cy, 0.6f);
                }
                case RECTANGLE -> rect(left, top, b.width(), b.height(), false);
                case CIRCLE -> circle(cx, cy, Math.max(1, Math.min(b.width(), b.height()) / 2.0), false);
                case ARC -> {
                    var radius = Math.max(1, Math.min(b.width(), b.height()) / 2.0);
                    for (var i = 1; i <= 16; i++) {
                        var a = Math.PI * (i - 1) / 16; var next = Math.PI * i / 16;
                        line(cx + Math.cos(a) * radius, cy - Math.sin(a) * radius,
                                cx + Math.cos(next) * radius, cy - Math.sin(next) * radius, 0.8f);
                    }
                }
                case ARROW -> {
                    if (vertical) { line(cx, bottom, cx, top, 0.8f); line(cx, top, cx - 4, top + 5, 0.8f); line(cx, top, cx + 4, top + 5, 0.8f); }
                    else { line(left, cy, right, cy, 0.8f); line(right, cy, right - 5, cy - 4, 0.8f); line(right, cy, right - 5, cy + 4, 0.8f); }
                }
                case REFERENCE_POINT -> { line(cx - 4, cy, cx + 4, cy, 0.8f); line(cx, cy - 4, cx, cy + 4, 0.8f); color(0x343A3D); circle(cx, cy, 1.5, true); }
            }
        }

        private void mechanicalDimension(dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalDimension dimension) throws IOException {
            var b = dimension.bounds();
            var l = b.x(); var t = b.y(); var r = l + b.width(); var bottom = t + b.height();
            var cx = (l + r) / 2.0; var cy = (t + bottom) / 2.0;
            switch (dimension.kind()) {
                case HORIZONTAL -> {
                    var ruleY = t + Math.max(3, b.height() / 3);
                    line(l, bottom, l, ruleY, 0.65f); line(r, bottom, r, ruleY, 0.65f);
                    line(l, ruleY, r, ruleY, 0.65f); arrows(l, ruleY, r, ruleY);
                }
                case VERTICAL -> {
                    var ruleX = l + Math.max(3, b.width() / 3);
                    line(r, t, ruleX, t, 0.65f); line(r, bottom, ruleX, bottom, 0.65f);
                    line(ruleX, t, ruleX, bottom, 0.65f); arrows(ruleX, t, ruleX, bottom);
                }
                case ALIGNED -> { line(l, bottom, r, t, 0.65f); arrows(l, bottom, r, t); }
                case RADIUS -> {
                    var radius = Math.max(1, Math.min(b.width(), b.height()) / 2.0);
                    circle(cx, cy, radius, false); line(cx, cy, cx + radius, cy, 0.65f);
                    arrow(cx + radius, cy, cx, cy);
                }
                case DIAMETER -> {
                    var radius = Math.max(1, Math.min(b.width(), b.height()) / 2.0);
                    circle(cx, cy, radius, false); line(cx - radius, cy, cx + radius, cy, 0.65f);
                    arrows(cx - radius, cy, cx + radius, cy);
                }
                case ANGLE -> {
                    line(l, bottom, r, bottom, 0.65f); line(l, bottom, r, t, 0.65f);
                    var radius = Math.max(4, Math.min(b.width(), b.height()) / 3.0);
                    var angle = Math.atan2(b.height(), Math.max(1, b.width()));
                    for (var i = 1; i <= 16; i++) {
                        var a = angle * (i - 1) / 16; var next = angle * i / 16;
                        line(l + Math.cos(a) * radius, bottom - Math.sin(a) * radius,
                                l + Math.cos(next) * radius, bottom - Math.sin(next) * radius, 0.65f);
                    }
                }
            }
            var label = dimension.label();
            stream.addRect(x(label.x() - 2), y(label.y() + label.height() + 1),
                    (label.width() + 4) * sx, (label.height() + 2) * sy);
            stream.setNonStrokingColor(1f, 1f, 1f);
            stream.fill();
            diagramLabel(label);
        }

        private void arrows(double x1, double y1, double x2, double y2) throws IOException {
            arrow(x1, y1, x2, y2); arrow(x2, y2, x1, y1);
        }

        private void arrow(double tipX, double tipY, double towardX, double towardY) throws IOException {
            var angle = Math.atan2(towardY - tipY, towardX - tipX);
            var size = 4.0; var spread = Math.PI / 7;
            line(tipX, tipY, tipX + Math.cos(angle + spread) * size, tipY + Math.sin(angle + spread) * size, 0.65f);
            line(tipX, tipY, tipX + Math.cos(angle - spread) * size, tipY + Math.sin(angle - spread) * size, 0.65f);
        }

        private void mechanicalSymbol(dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalSymbol symbol) throws IOException {
            var b = symbol.bounds();
            var l = b.x(); var t = b.y(); var r = l + b.width(); var bottom = t + b.height();
            var cx = (l + r) / 2.0; var cy = (t + bottom) / 2.0;
            switch (symbol.kind()) {
                case SHAFT -> { line(l, cy - 2, r, cy - 2, 0.8f); line(l, cy + 2, r, cy + 2, 0.8f); line(l, cy - 4, l, cy + 4, 0.8f); line(r, cy - 4, r, cy + 4, 0.8f); }
                case BEARING -> { var radius = Math.max(3, Math.min(b.width(), b.height()) / 2.0); circle(cx, cy, radius, false); circle(cx, cy, Math.max(2, radius / 2), false); line(l, cy, r, cy, 0.8f); }
                case GEAR -> { var radius = Math.max(4, Math.min(b.width(), b.height()) / 2.0 - 3); circle(cx, cy, radius, false); circle(cx, cy, Math.max(2, radius / 3), false); for (var i = 0; i < 8; i++) { var a = Math.PI * 2 * i / 8; line(cx + Math.cos(a) * radius, cy + Math.sin(a) * radius, cx + Math.cos(a) * (radius + 3), cy + Math.sin(a) * (radius + 3), 0.8f); } }
                case SPRING -> { for (var i = 1; i <= 8; i++) line(l + (r - l) * (i - 1) / 8.0, cy + ((i - 1) % 2 == 0 ? -3 : 3), l + (r - l) * i / 8.0, cy + (i % 2 == 0 ? -3 : 3), 0.8f); }
                case PISTON -> { rect(l, t + 3, Math.max(5, b.width() / 4), Math.max(5, b.height() - 6), false); line(l + b.width() / 4.0, cy, r - 4, cy, 0.8f); line(r - 4, t + 2, r - 4, bottom - 2, 0.8f); }
                case BOLT -> { var head = Math.max(5, b.height() - 4); rect(l, t + 2, head, Math.max(4, b.height() - 4), false); line(l + head, cy, r, cy, 0.8f); }
            }
        }

        private void diagramLabel(LaidOutDiagramLabel label) throws IOException {
            text(label.text(), label.x(), label.y(), 8, false, false, false, label.width());
        }

        private void electricalLabel(LaidOutDiagramLabel label) throws IOException {
            text(label.text(), label.x(), label.y(), Math.min(7f, label.height()),
                    false, false, false, label.width());
        }
    }
}
