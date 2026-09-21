package dev.rgcb.scholar.layout;

import dev.rgcb.scholar.diagram.layout.DiagramLayoutEngine;
import dev.rgcb.scholar.diagram.layout.DiagramViewport;
import dev.rgcb.scholar.data.DatasetPlotResolver;
import dev.rgcb.scholar.data.DatasetTableResolver;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceResolver;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.DocumentStructure;
import dev.rgcb.scholar.document.DocumentStructureResolver;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.LayoutSectionBreak;
import dev.rgcb.scholar.document.ColumnLayout;
import dev.rgcb.scholar.document.ContentSpan;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PageBreak;
import dev.rgcb.scholar.document.ParagraphAlignment;
import dev.rgcb.scholar.document.ParagraphFormat;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.QuantityInline;
import dev.rgcb.scholar.document.SemanticStyle;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.editor.TextBoundary;
import dev.rgcb.scholar.math.layout.MathLayoutEngine;
import dev.rgcb.scholar.math.layout.MathTextMeasurer;
import dev.rgcb.scholar.plot.layout.PlotLayoutEngine;
import dev.rgcb.scholar.typography.ScholarTypography;
import dev.rgcb.scholar.typography.DocumentStyleResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.text.BreakIterator;
import java.util.Locale;
import java.util.function.BiFunction;
import dev.rgcb.scholar.quantity.ScientificNumberFormatter;

/**
 * Minecraft-independent layout engine for read-only Scholar documents.
 */
public final class DocumentLayoutEngine {
    private static final int BLOCK_X = 0;
    private static final int MIN_EQUATION_BLOCK_WIDTH = 48;
    private static final int MIN_EQUATION_BLOCK_HEIGHT = 18;
    private static final int FIGURE_CAPTION_GAP = 6;
    private static final int TOC_INDENT = 8;
    public static final int PAGE_GAP = 20;

    private final MathLayoutEngine mathLayoutEngine = new MathLayoutEngine();
    private final TableLayoutEngine tableLayoutEngine = new TableLayoutEngine();
    private final PlotLayoutEngine plotLayoutEngine = new PlotLayoutEngine();
    private final DiagramLayoutEngine diagramLayoutEngine = new DiagramLayoutEngine();
    private final DocumentStructureResolver structureResolver = new DocumentStructureResolver();
    private final DatasetTableResolver datasetTableResolver = new DatasetTableResolver();
    private final DatasetPlotResolver datasetPlotResolver = new DatasetPlotResolver();
    private final ScholarTypography typography;
    private final DocumentStyleResolver styleResolver = new DocumentStyleResolver();

    public DocumentLayoutEngine() {
        this(ScholarTypography.defaultProfile());
    }

    public DocumentLayoutEngine(ScholarTypography typography) {
        this.typography = Objects.requireNonNull(typography, "typography");
    }

    public LaidOutDocument layout(Document document, int contentWidth, TextMeasurer textMeasurer) {
        return layout(document, contentWidth, textMeasurer, null);
    }

    public LaidOutDocument layout(
            Document document,
            int contentWidth,
            TextMeasurer textMeasurer,
            MathTextMeasurer mathTextMeasurer
    ) {
        return layout(
                document,
                contentWidth,
                textMeasurer,
                mathTextMeasurer,
                (blockIndex, diagram) -> DiagramViewport.fit(diagram.definition().canvas()));
    }

    public LaidOutDocument layout(
            Document document,
            int contentWidth,
            TextMeasurer textMeasurer,
            MathTextMeasurer mathTextMeasurer,
            BiFunction<Integer, DiagramBlock, DiagramViewport> diagramViewportProvider
    ) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        Objects.requireNonNull(diagramViewportProvider, "diagramViewportProvider");
        if (contentWidth <= 0) {
            throw new IllegalArgumentException("contentWidth must be positive.");
        }

        var blocks = new ArrayList<LaidOutBlock>();
        var y = 0;
        var figureNumber = 1;
        var referenceResolver = new CrossReferenceResolver();
        var structure = structureResolver.resolve(document);

        for (var blockIndex = 0; blockIndex < document.blocks().size(); blockIndex++) {
            var block = document.blocks().get(blockIndex);
            if (block instanceof LayoutSectionBreak) {
                blocks.add(new LaidOutBlock(LaidOutBlockKind.LAYOUT_SECTION_BREAK, 0,
                        BLOCK_X, y, contentWidth, 1, List.of()));
            } else if (block instanceof Heading heading) {
                y += typography.headingSpacingBefore(heading.level(), blocks.isEmpty());
                var style = TextStyle.heading(heading.level());
                var section = structure.sectionAtBlock(blockIndex).orElseThrow();
                var laidOut = layoutTextBlock(
                        LaidOutBlockKind.HEADING,
                        heading.level(),
                        heading.content(),
                        style,
                        section.number().displayText() + " ",
                        contentWidth,
                        y,
                        textMeasurer,
                        blockIndex,
                        document,
                        referenceResolver);
                blocks.add(laidOut);
                y = laidOut.y() + laidOut.height() + typography.headingSpacingAfter(heading.level());
            } else if (block instanceof TableOfContentsBlock) {
                y += blocks.isEmpty() ? 0 : typography.paragraphSpacingAfter();
                var laidOut = layoutTableOfContentsBlock(structure, contentWidth, y, textMeasurer);
                blocks.add(laidOut);
                y = laidOut.y() + laidOut.height() + typography.paragraphSpacingAfter();
            } else if (block instanceof Paragraph paragraph) {
                var laidOut = layoutTextBlock(
                        LaidOutBlockKind.PARAGRAPH,
                        0,
                        paragraph.content(),
                        null,
                        "",
                        contentWidth,
                        y,
                        textMeasurer,
                        blockIndex,
                        document,
                        referenceResolver);
                blocks.add(laidOut);
                y = laidOut.y() + laidOut.height() + typography.paragraphSpacingAfter();
            } else if (block instanceof EquationBlock equationBlock) {
                if (mathTextMeasurer == null) {
                    throw new IllegalArgumentException("mathTextMeasurer is required when laying out equation blocks.");
                }
                y += blocks.isEmpty() ? 0 : typography.equationSpacingBefore();
                var laidOut = layoutEquationBlock(equationBlock, contentWidth, y, mathTextMeasurer);
                blocks.add(laidOut);
                y = laidOut.y() + laidOut.height() + typography.equationSpacingAfter();
            } else if (block instanceof TableBlock tableBlock) {
                y += blocks.isEmpty() ? 0 : typography.paragraphSpacingAfter();
                var table = tableLayoutEngine.layout(datasetTableResolver.resolve(document, tableBlock), blockIndex, BLOCK_X, y, contentWidth, textMeasurer);
                var laidOut = new LaidOutBlock(
                        LaidOutBlockKind.TABLE,
                        0,
                        table.x(),
                        table.y(),
                        table.width(),
                        table.height(),
                        List.of(),
                        Optional.empty(),
                        Optional.of(table));
                blocks.add(laidOut);
                y = laidOut.y() + laidOut.height() + typography.paragraphSpacingAfter();
            } else if (block instanceof PlotBlock plotBlock) {
                y += blocks.isEmpty() ? 0 : typography.paragraphSpacingAfter();
                var plot = plotLayoutEngine.layout(datasetPlotResolver.resolve(document, plotBlock), blockIndex, BLOCK_X, y, contentWidth, textMeasurer);
                var laidOut = new LaidOutBlock(
                        LaidOutBlockKind.PLOT,
                        0,
                        plot.x(),
                        plot.y(),
                        plot.width(),
                        plot.height(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(plot));
                blocks.add(laidOut);
                y = laidOut.y() + laidOut.height() + typography.paragraphSpacingAfter();
            } else if (block instanceof DiagramBlock diagramBlock) {
                y += blocks.isEmpty() ? 0 : typography.paragraphSpacingAfter();
                var diagram = diagramLayoutEngine.layout(
                        diagramBlock,
                        blockIndex,
                        BLOCK_X,
                        y,
                        contentWidth,
                        textMeasurer,
                        Objects.requireNonNull(
                                diagramViewportProvider.apply(blockIndex, diagramBlock),
                                "diagram viewport provider returned null"));
                var laidOut = new LaidOutBlock(
                        LaidOutBlockKind.DIAGRAM,
                        0,
                        diagram.x(),
                        diagram.y(),
                        diagram.width(),
                        diagram.height(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(diagram));
                blocks.add(laidOut);
                y = laidOut.y() + laidOut.height() + typography.paragraphSpacingAfter();
            } else if (block instanceof FigureBlock figureBlock) {
                y += blocks.isEmpty() ? 0 : typography.paragraphSpacingAfter();
                var laidOut = layoutFigureBlock(
                        figureBlock,
                        figureNumber++,
                        blockIndex,
                        contentWidth,
                        y,
                        textMeasurer,
                        diagramViewportProvider,
                        document,
                        referenceResolver);
                blocks.add(laidOut);
                y = laidOut.y() + laidOut.height() + typography.paragraphSpacingAfter();
            } else {
                throw new IllegalArgumentException("Unsupported block node: " + block.getClass().getName());
            }
        }

        var height = blocks.isEmpty() ? 0 : Math.max(0, y - trailingSpacing(blocks.get(blocks.size() - 1)));
        return new LaidOutDocument(contentWidth, height, blocks);
    }

    /**
     * Lays out a document in stable logical document units. Page assignment is
     * derived and is never written back into the semantic document.
     */
    public LaidOutDocument layoutPaginated(
            Document document,
            TextMeasurer textMeasurer,
            MathTextMeasurer mathTextMeasurer
    ) {
        return layoutPaginated(
                document,
                textMeasurer,
                mathTextMeasurer,
                (blockIndex, diagram) -> DiagramViewport.fit(diagram.definition().canvas()));
    }

    public LaidOutDocument layoutPaginated(
            Document document,
            TextMeasurer textMeasurer,
            MathTextMeasurer mathTextMeasurer,
            BiFunction<Integer, DiagramBlock, DiagramViewport> diagramViewportProvider
    ) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        Objects.requireNonNull(diagramViewportProvider, "diagramViewportProvider");

        var cursor = new PageCursor(document);
        var blocks = new ArrayList<LaidOutBlock>();
        var structure = structureResolver.resolve(document);
        var referenceResolver = new CrossReferenceResolver();
        var figureNumber = 1;

        for (var blockIndex = 0; blockIndex < document.blocks().size(); blockIndex++) {
            var block = document.blocks().get(blockIndex);
            if (block instanceof LayoutSectionBreak sectionBreak) {
                blocks.add(new LaidOutBlock(LaidOutBlockKind.LAYOUT_SECTION_BREAK, 0,
                        cursor.x(), cursor.y(), cursor.columnWidth(), 1, List.of()));
                cursor.changeColumns(sectionBreak.columnLayout());
                continue;
            }
            if (block instanceof PageBreak) {
                blocks.add(new LaidOutBlock(
                        LaidOutBlockKind.PAGE_BREAK,
                        0,
                        cursor.x(),
                        cursor.y(),
                        cursor.columnWidth(),
                        0,
                        List.of()));
                cursor.forceNextPage();
                continue;
            }

            if (block instanceof Heading heading) {
                cursor.addVerticalSpace(typography.headingSpacingBefore(heading.level(), blocks.isEmpty()));
                var section = structure.sectionAtBlock(blockIndex).orElseThrow();
                var raw = layoutTextBlock(
                        LaidOutBlockKind.HEADING,
                        heading.level(),
                        heading.content(),
                        TextStyle.heading(heading.level()),
                        section.number().displayText() + " ",
                        cursor.columnWidth(),
                        0,
                        textMeasurer,
                        blockIndex,
                        document,
                        referenceResolver);
                blocks.add(placeTextBlock(raw, cursor, ParagraphFormat.none(), textMeasurer));
                cursor.addVerticalSpace(typography.headingSpacingAfter(heading.level()));
                continue;
            }

            if (block instanceof Paragraph paragraph) {
                var definition = styleResolver.resolve(document.settings().template(), paragraph.style());
                var format = styleResolver.resolveParagraph(document.settings().template(), paragraph.style(), paragraph.format());
                cursor.addVerticalSpace(format.spaceBefore().orElse(0));
                var availableWidth = Math.max(1,
                        cursor.columnWidth() - format.leftIndent().orElse(0) - format.rightIndent().orElse(0));
                var raw = layoutTextBlock(
                        LaidOutBlockKind.PARAGRAPH,
                        0,
                        paragraph.content(),
                        TextStyle.paragraph(definition.marks()).withFormat(definition.text()),
                        "",
                        availableWidth,
                        0,
                        textMeasurer,
                        blockIndex,
                        document,
                        referenceResolver);
                blocks.add(placeTextBlock(raw, cursor, format, textMeasurer));
                cursor.addVerticalSpace(format.spaceAfter().orElse(typography.paragraphSpacingAfter()));
                continue;
            }

            if (block instanceof TableOfContentsBlock) {
                cursor.addVerticalSpace(blocks.isEmpty() ? 0 : typography.paragraphSpacingAfter());
                var raw = layoutTableOfContentsBlock(structure, cursor.columnWidth(), 0, textMeasurer);
                blocks.add(placeTextBlock(raw, cursor, ParagraphFormat.none(), textMeasurer));
                cursor.addVerticalSpace(typography.paragraphSpacingAfter());
                continue;
            }

            var resolvedTable = block instanceof TableBlock table
                    ? datasetTableResolver.resolve(document, table)
                    : null;
            var pageWidth = (block instanceof FigureBlock figure && figure.span() == ContentSpan.PAGE_WIDTH)
                    || (block instanceof TableBlock table && table.span() == ContentSpan.PAGE_WIDTH)
                    || (block instanceof TableBlock table && table.span() == ContentSpan.AUTO
                            && tableLayoutEngine.minimumReadableWidth(resolvedTable, textMeasurer) > cursor.columnWidth());
            var width = pageWidth ? cursor.beginPageWidthBlock() : cursor.columnWidth();
            cursor.addVerticalSpace(blocks.isEmpty() ? 0 : typography.paragraphSpacingAfter());

            LaidOutBlock laidOut;
            if (block instanceof EquationBlock equation) {
                if (mathTextMeasurer == null) {
                    throw new IllegalArgumentException("mathTextMeasurer is required when laying out equation blocks.");
                }
                var measured = layoutEquationBlock(equation, width, 0, mathTextMeasurer);
                cursor.ensureFits(measured.height());
                laidOut = layoutEquationBlockAt(equation, width, cursor.x(), cursor.y(), mathTextMeasurer);
            } else if (block instanceof TableBlock table) {
                var measured = tableLayoutEngine.layout(resolvedTable, blockIndex, 0, 0, width, textMeasurer);
                cursor.ensureFits(measured.height());
                var value = tableLayoutEngine.layout(resolvedTable, blockIndex, cursor.x(), cursor.y(), width, textMeasurer);
                laidOut = new LaidOutBlock(LaidOutBlockKind.TABLE, 0, value.x(), value.y(), value.width(), value.height(), List.of(), Optional.empty(), Optional.of(value));
            } else if (block instanceof PlotBlock plot) {
                var measured = plotLayoutEngine.layout(datasetPlotResolver.resolve(document, plot), blockIndex, 0, 0, width, textMeasurer);
                cursor.ensureFits(measured.height());
                var value = plotLayoutEngine.layout(datasetPlotResolver.resolve(document, plot), blockIndex, cursor.x(), cursor.y(), width, textMeasurer);
                laidOut = new LaidOutBlock(LaidOutBlockKind.PLOT, 0, value.x(), value.y(), value.width(), value.height(), List.of(), Optional.empty(), Optional.empty(), Optional.of(value));
            } else if (block instanceof DiagramBlock diagram) {
                var viewport = Objects.requireNonNull(diagramViewportProvider.apply(blockIndex, diagram), "diagram viewport provider returned null");
                var measured = diagramLayoutEngine.layout(diagram, blockIndex, 0, 0, width, textMeasurer, viewport);
                cursor.ensureFits(measured.height());
                var value = diagramLayoutEngine.layout(diagram, blockIndex, cursor.x(), cursor.y(), width, textMeasurer, viewport);
                laidOut = new LaidOutBlock(LaidOutBlockKind.DIAGRAM, 0, value.x(), value.y(), value.width(), value.height(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(value));
            } else if (block instanceof FigureBlock figure) {
                var measured = layoutFigureBlock(figure, figureNumber, blockIndex, width, 0, textMeasurer, diagramViewportProvider, document, referenceResolver);
                cursor.ensureFits(measured.height());
                laidOut = layoutFigureBlockAt(figure, figureNumber++, blockIndex, width, cursor.x(), cursor.y(), textMeasurer, diagramViewportProvider, document, referenceResolver);
            } else {
                throw new IllegalArgumentException("Unsupported block node: " + block.getClass().getName());
            }
            blocks.add(laidOut);
            cursor.consume(laidOut.height() + typography.paragraphSpacingAfter());
            if (pageWidth) {
                cursor.finishPageWidthBlock();
            }
        }

        return new LaidOutDocument(cursor.pageWidth(), cursor.documentHeight(), blocks, cursor.pages());
    }

    private static LaidOutBlock placeTextBlock(
            LaidOutBlock raw,
            PageCursor cursor,
            ParagraphFormat format,
            TextMeasurer textMeasurer
    ) {
        var placedLines = new ArrayList<LaidOutLine>();
        var firstX = -1;
        var firstY = -1;
        var lastBottom = -1;
        for (var lineIndex = 0; lineIndex < raw.lines().size(); lineIndex++) {
            var line = raw.lines().get(lineIndex);
            var scaledHeight = Math.max(1, line.height() * format.lineSpacingPermille().orElse(1000) / 1000);
            cursor.ensureFits(scaledHeight);
            var indent = format.leftIndent().orElse(0);
            if (lineIndex == 0) {
                indent += format.firstLineIndent().orElse(0);
            }
            var available = Math.max(1, cursor.columnWidth() - indent - format.rightIndent().orElse(0));
            var alignmentOffset = switch (format.alignment().orElse(ParagraphAlignment.LEFT)) {
                case LEFT, JUSTIFIED -> 0;
                case CENTER -> Math.max(0, (available - line.width()) / 2);
                case RIGHT -> Math.max(0, available - line.width());
            };
            var baseX = cursor.x() + indent + alignmentOffset;
            var dx = baseX + line.x();
            var dy = cursor.y();
            var runs = new ArrayList<LaidOutText>();
            var justify = format.alignment().orElse(ParagraphAlignment.LEFT) == ParagraphAlignment.JUSTIFIED
                    && lineIndex + 1 < raw.lines().size();
            var spaces = justify ? line.textRuns().stream().filter(run -> run.text().isBlank()).count() : 0;
            var extra = spaces == 0 ? 0 : Math.max(0, available - line.width());
            var shifted = 0;
            var seenSpaces = 0;
            for (var run : line.textRuns()) {
                runs.add(new LaidOutText(run.text(), run.style(), baseX + run.x() + shifted, dy, run.width(),
                        run.sourceBlockIndex(), run.sourceStart(), run.sourceEnd(), run.atomic()));
                if (run.text().isBlank() && spaces > 0) {
                    seenSpaces++;
                    shifted = extra * seenSpaces / (int) spaces;
                }
            }
            placedLines.add(new LaidOutLine(dx, dy, justify && spaces > 0 ? available : line.width(), scaledHeight, runs));
            if (firstX < 0) {
                firstX = dx;
                firstY = dy;
            }
            lastBottom = dy + scaledHeight;
            cursor.consume(scaledHeight);
        }
        if (placedLines.isEmpty()) {
            var height = textMeasurer.lineHeight(TextStyle.paragraph());
            cursor.ensureFits(height);
            firstX = cursor.x();
            firstY = cursor.y();
            placedLines.add(new LaidOutLine(firstX, firstY, 0, height, List.of()));
            lastBottom = firstY + height;
            cursor.consume(height);
        }
        var tableOfContents = raw.tableOfContents().map(toc -> relocateTableOfContents(raw.lines(), placedLines, toc));
        return new LaidOutBlock(raw.kind(), raw.headingLevel(), firstX, firstY, cursor.columnWidth(), lastBottom - firstY,
                placedLines, raw.math(), raw.table(), raw.plot(), raw.diagram(), raw.figure(), tableOfContents);
    }

    private static LaidOutTableOfContents relocateTableOfContents(
            List<LaidOutLine> sourceLines,
            List<LaidOutLine> placedLines,
            LaidOutTableOfContents source
    ) {
        var entries = new ArrayList<LaidOutTableOfContentsEntry>();
        for (var entry : source.entries()) {
            var firstLine = 0;
            while (firstLine + 1 < sourceLines.size()
                    && sourceLines.get(firstLine + 1).y() <= entry.y()) {
                firstLine++;
            }
            var lastLine = firstLine;
            while (lastLine + 1 < sourceLines.size()
                    && sourceLines.get(lastLine + 1).y() < entry.y() + entry.height()) {
                lastLine++;
            }
            for (var lineIndex = firstLine; lineIndex <= lastLine; lineIndex++) {
                var sourceLine = sourceLines.get(lineIndex);
                var placedLine = placedLines.get(lineIndex);
                var continuationOffset = Math.max(0, sourceLine.x() - entry.x());
                entries.add(new LaidOutTableOfContentsEntry(entry.targetId(), entry.targetBlockIndex(),
                        placedLine.x(), placedLine.y(),
                        Math.max(placedLine.width(), entry.width() - continuationOffset), placedLine.height()));
            }
        }
        return new LaidOutTableOfContents(entries);
    }

    private LaidOutBlock layoutEquationBlockAt(EquationBlock equation, int width, int originX, int y, MathTextMeasurer measurer) {
        var math = mathLayoutEngine.layout(equation.expression(), measurer);
        var blockWidth = math.width() == 0 ? MIN_EQUATION_BLOCK_WIDTH : math.width();
        var blockHeight = math.height() == 0 ? MIN_EQUATION_BLOCK_HEIGHT : math.height();
        var x = originX + (blockWidth <= width ? (width - blockWidth) / 2 : 0);
        return new LaidOutBlock(LaidOutBlockKind.EQUATION, 0, x, y, blockWidth, blockHeight, List.of(), Optional.of(math));
    }

    private LaidOutBlock layoutFigureBlockAt(
            FigureBlock figure,
            int number,
            int sourceBlockIndex,
            int width,
            int x,
            int y,
            TextMeasurer textMeasurer,
            BiFunction<Integer, DiagramBlock, DiagramViewport> diagramViewportProvider,
            Document document,
            CrossReferenceResolver referenceResolver
    ) {
        LaidOutBlock content;
        if (figure.content() instanceof PlotBlock plot) {
            var value = plotLayoutEngine.layout(datasetPlotResolver.resolve(document, plot), sourceBlockIndex, x, y, width, textMeasurer);
            content = new LaidOutBlock(LaidOutBlockKind.PLOT, 0, value.x(), value.y(), value.width(), value.height(), List.of(), Optional.empty(), Optional.empty(), Optional.of(value));
        } else if (figure.content() instanceof DiagramBlock diagram) {
            var value = diagramLayoutEngine.layout(diagram, sourceBlockIndex, x, y, width, textMeasurer,
                    Objects.requireNonNull(diagramViewportProvider.apply(sourceBlockIndex, diagram), "diagram viewport provider returned null"));
            content = new LaidOutBlock(LaidOutBlockKind.DIAGRAM, 0, value.x(), value.y(), value.width(), value.height(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(value));
        } else {
            throw new IllegalArgumentException("Unsupported figure content: " + figure.content().getClass().getName());
        }
        var captionY = content.y() + content.height() + FIGURE_CAPTION_GAP;
        var caption = layoutTextBlock(LaidOutBlockKind.PARAGRAPH, 0, numberedCaption(number, figure.caption()),
                semanticTextStyle(document, SemanticStyle.FIGURE_CAPTION), "", width, captionY,
                textMeasurer, sourceBlockIndex, document, referenceResolver);
        var shiftedLines = caption.lines().stream().map(line -> new LaidOutLine(
                x + line.x(), line.y(), line.width(), line.height(),
                line.textRuns().stream().map(run -> new LaidOutText(
                        run.text(), run.style(), x + run.x(), run.y(), run.width(), run.sourceBlockIndex(), run.sourceStart(), run.sourceEnd(), run.atomic())).toList())).toList();
        var height = captionY + caption.height() - y;
        var value = new LaidOutFigure(figure.id(), number, x, y, width, height, content, captionY, shiftedLines);
        return new LaidOutBlock(LaidOutBlockKind.FIGURE, 0, x, y, width, height, shiftedLines, Optional.empty(), Optional.empty(), content.plot(), content.diagram(), Optional.of(value), Optional.empty());
    }

    private static final class PageCursor {
        private final int pageWidth;
        private final int pageHeight;
        private final int contentWidth;
        private final int contentHeight;
        private final int marginLeft;
        private final int marginTop;
        private int columnCount;
        private int columnGap;
        private int columnWidth;
        private ColumnLayout activeLayout;
        private final String headerText;
        private final String footerText;
        private final boolean pageNumbers;
        private int page;
        private int column;
        private int y;
        private int columnTop;
        private boolean pageWidthMode;
        private final List<ColumnRegion> completedRegions = new ArrayList<>();
        private ColumnRegion activeRegion;

        private PageCursor(Document document) {
            var settings = document.settings();
            pageWidth = settings.pageWidth();
            pageHeight = settings.pageHeight();
            marginLeft = settings.margins().left().logicalUnits();
            marginTop = settings.margins().top().logicalUnits();
            contentWidth = settings.contentWidth();
            contentHeight = settings.contentHeight();
            applyColumns(settings.columns());
            headerText = settings.decoration().headerText();
            footerText = settings.decoration().footerText();
            pageNumbers = settings.decoration().pageNumbers();
            y = pageContentTop();
            columnTop = y;
            activeRegion = new ColumnRegion(page, columnTop, settings.columns());
        }

        private int x() {
            return marginLeft + (pageWidthMode ? 0 : column * (columnWidth + columnGap));
        }

        private int y() {
            return y;
        }

        private int columnWidth() {
            return pageWidthMode ? contentWidth : columnWidth;
        }

        private int pageWidth() {
            return pageWidth;
        }

        private int pageContentTop() {
            return page * (pageHeight + PAGE_GAP) + marginTop;
        }

        private int pageContentBottom() {
            return pageContentTop() + contentHeight;
        }

        private void ensureFits(int height) {
            while (y + height > pageContentBottom() && (y > columnTop || columnTop >= pageContentBottom())) {
                nextColumnOrPage();
            }
        }

        private void consume(int height) {
            y += Math.max(0, height);
        }

        private void addVerticalSpace(int height) {
            if (height <= 0) {
                return;
            }
            y = Math.min(pageContentBottom(), y + height);
        }

        private void nextColumnOrPage() {
            if (!pageWidthMode && column + 1 < columnCount) {
                column++;
                y = columnTop;
            } else {
                advancePage();
            }
        }

        private void forceNextPage() {
            pageWidthMode = false;
            advancePage();
        }

        private void changeColumns(ColumnLayout replacement) {
            Objects.requireNonNull(replacement, "replacement");
            if (columnCount == replacement.count() && columnGap == replacement.gap().logicalUnits()) {
                return;
            }
            if (columnCount > 1 && column > 0) {
                advancePage();
            }
            finishRegion(y);
            column = 0;
            columnTop = y;
            applyColumns(replacement);
            activeRegion = new ColumnRegion(page, columnTop, replacement);
        }

        private void applyColumns(ColumnLayout layout) {
            activeLayout = layout;
            columnCount = layout.count();
            columnGap = layout.gap().logicalUnits();
            columnWidth = Math.max(1, (contentWidth - columnGap * (columnCount - 1)) / columnCount);
        }

        private void advancePage() {
            finishRegion(pageContentBottom());
            page++;
            column = 0;
            y = pageContentTop();
            columnTop = y;
            activeRegion = new ColumnRegion(page, columnTop, activeLayout);
        }

        private void finishRegion(int bottom) {
            if (activeRegion != null && bottom > activeRegion.top()) {
                completedRegions.add(activeRegion.withBottom(bottom));
            }
            activeRegion = null;
        }

        private int beginPageWidthBlock() {
            if (columnCount > 1 && (column != 0 || y != pageContentTop())) {
                forceNextPage();
            }
            pageWidthMode = true;
            column = 0;
            return contentWidth;
        }

        private void finishPageWidthBlock() {
            pageWidthMode = false;
            if (columnCount > 1) {
                forceNextPage();
            }
        }

        private int documentHeight() {
            return (page + 1) * pageHeight + page * PAGE_GAP;
        }

        private List<LaidOutPage> pages() {
            var regions = new ArrayList<>(completedRegions);
            if (activeRegion != null) {
                regions.add(activeRegion.withBottom(pageContentBottom()));
            }
            var result = new ArrayList<LaidOutPage>();
            for (var pageIndex = 0; pageIndex <= page; pageIndex++) {
                var pageY = pageIndex * (pageHeight + PAGE_GAP);
                var columns = new ArrayList<LaidOutColumn>();
                for (var region : regions) {
                    if (region.page() != pageIndex || region.bottom() <= region.top()) continue;
                    var gap = region.layout().gap().logicalUnits();
                    var width = Math.max(1, (contentWidth - gap * (region.layout().count() - 1)) / region.layout().count());
                    for (var columnIndex = 0; columnIndex < region.layout().count(); columnIndex++) {
                        columns.add(new LaidOutColumn(columnIndex,
                                marginLeft + columnIndex * (width + gap), region.top(), width,
                                region.bottom() - region.top()));
                    }
                }
                if (columns.isEmpty()) columns.add(new LaidOutColumn(0, marginLeft, pageY + marginTop, contentWidth, contentHeight));
                result.add(new LaidOutPage(pageIndex, 0, pageY, pageWidth, pageHeight,
                        marginLeft, pageY + marginTop, contentWidth, contentHeight, columns,
                        headerText, footerText, pageNumbers));
            }
            return List.copyOf(result);
        }

        private record ColumnRegion(int page, int top, ColumnLayout layout, int bottom) {
            private ColumnRegion(int page, int top, ColumnLayout layout) { this(page, top, layout, top); }
            private ColumnRegion withBottom(int value) { return new ColumnRegion(page, top, layout, value); }
        }
    }

    private LaidOutBlock layoutFigureBlock(
            FigureBlock figureBlock,
            int number,
            int sourceBlockIndex,
            int contentWidth,
            int y,
            TextMeasurer textMeasurer,
            BiFunction<Integer, DiagramBlock, DiagramViewport> diagramViewportProvider
            ,
            Document document,
            CrossReferenceResolver referenceResolver
    ) {
        LaidOutBlock contentBlock;
        if (figureBlock.content() instanceof PlotBlock plotBlock) {
            var plot = plotLayoutEngine.layout(datasetPlotResolver.resolve(document, plotBlock), sourceBlockIndex, BLOCK_X, y, contentWidth, textMeasurer);
            contentBlock = new LaidOutBlock(
                    LaidOutBlockKind.PLOT,
                    0,
                    plot.x(),
                    plot.y(),
                    plot.width(),
                    plot.height(),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(plot));
        } else if (figureBlock.content() instanceof DiagramBlock diagramBlock) {
            var diagram = diagramLayoutEngine.layout(
                    diagramBlock,
                    sourceBlockIndex,
                    BLOCK_X,
                    y,
                    contentWidth,
                    textMeasurer,
                    Objects.requireNonNull(
                            diagramViewportProvider.apply(sourceBlockIndex, diagramBlock),
                            "diagram viewport provider returned null"));
            contentBlock = new LaidOutBlock(
                    LaidOutBlockKind.DIAGRAM,
                    0,
                    diagram.x(),
                    diagram.y(),
                    diagram.width(),
                    diagram.height(),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(diagram));
        } else {
            throw new IllegalArgumentException("Unsupported figure content: " + figureBlock.content().getClass().getName());
        }

        var captionY = contentBlock.y() + contentBlock.height() + FIGURE_CAPTION_GAP;
                var caption = layoutTextBlock(
                LaidOutBlockKind.PARAGRAPH,
                0,
                numberedCaption(number, figureBlock.caption()),
                semanticTextStyle(document, SemanticStyle.FIGURE_CAPTION),
                "",
                contentWidth,
                captionY,
                textMeasurer,
                sourceBlockIndex,
                document,
                referenceResolver);
        var height = caption.y() + caption.height() - y;
        var figure = new LaidOutFigure(
                figureBlock.id(),
                number,
                BLOCK_X,
                y,
                contentWidth,
                height,
                contentBlock,
                captionY,
                caption.lines());
        return new LaidOutBlock(
                LaidOutBlockKind.FIGURE,
                0,
                BLOCK_X,
                y,
                contentWidth,
                height,
                caption.lines(),
                Optional.empty(),
                Optional.empty(),
                contentBlock.plot(),
                contentBlock.diagram(),
                Optional.of(figure),
                Optional.empty());
    }

    private static InlineContent numberedCaption(int number, InlineContent caption) {
        var nodes = new ArrayList<InlineNode>();
        nodes.add(new Text("Figure " + number + ". ", java.util.Set.of(TextMark.BOLD)));
        nodes.addAll(caption.nodes());
        return new InlineContent(nodes);
    }

    private TextStyle semanticTextStyle(Document document, SemanticStyle style) {
        var definition = styleResolver.resolve(document.settings().template(), style);
        return TextStyle.paragraph(definition.marks()).withFormat(definition.text());
    }

    private static LaidOutBlock layoutTextBlock(
            LaidOutBlockKind kind,
            int headingLevel,
            InlineContent content,
            TextStyle forcedStyle,
            String displayPrefix,
            int contentWidth,
            int y,
            TextMeasurer textMeasurer,
            int sourceBlockIndex,
            Document document,
            CrossReferenceResolver referenceResolver
    ) {
        var builder = new LineBuilder(contentWidth, y, textMeasurer);
        if (!displayPrefix.isEmpty()) {
            builder.appendDisplayOnly(displayPrefix, forcedStyle == null ? TextStyle.paragraph() : forcedStyle);
        }
        var sourceOffset = 0;
        for (InlineNode node : content.nodes()) {
            if (node instanceof Text text) {
                var style = forcedStyle == null ? TextStyle.paragraph(text.marks()).withFormat(text.format())
                        : forcedStyle.withMarks(union(forcedStyle.marks(), text.marks()))
                                .withFormat(forcedStyle.format().overrideWith(text.format()));
                builder.append(text.content(), style, sourceBlockIndex, sourceOffset);
                sourceOffset += TextBoundary.characterCount(text.content());
            } else if (node instanceof CrossReference reference) {
                var style = forcedStyle == null ? TextStyle.paragraph() : forcedStyle;
                builder.appendAtomic(referenceResolver.resolve(document, reference).displayText(), style, sourceBlockIndex, sourceOffset, 1);
                sourceOffset += 1;
            } else if (node instanceof QuantityInline quantity) {
                var style = forcedStyle == null ? TextStyle.paragraph() : forcedStyle;
                builder.appendAtomic(new ScientificNumberFormatter().format(quantity.value(), quantity.notation(), true),
                        style, sourceBlockIndex, sourceOffset, 1);
                sourceOffset += 1;
            } else {
                throw new IllegalArgumentException("Unsupported inline node: " + node.getClass().getName());
            }
        }
        if (sourceOffset == 0 && !displayPrefix.isEmpty()) {
            builder.appendSourceAnchor(forcedStyle == null ? TextStyle.paragraph() : forcedStyle, sourceBlockIndex);
        }

        var lines = builder.finish();
        if (lines.isEmpty()) {
            var emptyLineHeight = textMeasurer.lineHeight(forcedStyle == null ? TextStyle.paragraph() : forcedStyle);
            lines = List.of(new LaidOutLine(0, y, 0, emptyLineHeight, List.of()));
        }
        var height = blockHeight(lines, y);
        return new LaidOutBlock(kind, headingLevel, BLOCK_X, y, contentWidth, height, lines);
    }

    private static LaidOutBlock layoutTableOfContentsBlock(
            DocumentStructure structure,
            int contentWidth,
            int y,
            TextMeasurer textMeasurer
    ) {
        var lines = new ArrayList<LaidOutLine>();
        var headingBuilder = new LineBuilder(contentWidth, y, textMeasurer);
        headingBuilder.appendDisplayText("Contents", TextStyle.heading(2));
        lines.addAll(headingBuilder.finish());
        var currentY = lines.getLast().y() + lines.getLast().height();
        var entries = new ArrayList<LaidOutTableOfContentsEntry>();
        for (var section : structure.sections()) {
            if (section.id().isEmpty()) {
                continue;
            }
            var indent = Math.min(contentWidth - 1, Math.max(0, section.level() - 1) * TOC_INDENT);
            var continuationIndent = Math.min(TOC_INDENT, Math.max(0, contentWidth - indent - 1));
            var entryWidth = Math.max(1, contentWidth - indent);
            var builder = new LineBuilder(Math.max(1, entryWidth - continuationIndent), currentY, textMeasurer);
            builder.appendDisplayText(section.displayText(), TextStyle.paragraph());
            var entryLines = builder.finish();
            for (var lineIndex = 0; lineIndex < entryLines.size(); lineIndex++) {
                var offset = indent + (lineIndex == 0 ? 0 : continuationIndent);
                var line = entryLines.get(lineIndex);
                lines.add(new LaidOutLine(offset, line.y(), line.width(), line.height(),
                        line.textRuns().stream().map(run -> new LaidOutText(run.text(), run.style(),
                                run.x() + offset, run.y(), run.width(), run.sourceBlockIndex(),
                                run.sourceStart(), run.sourceEnd(), run.atomic())).toList()));
            }
            var entryY = entryLines.getFirst().y();
            var entryHeight = entryLines.getLast().y() + entryLines.getLast().height() - entryY;
            entries.add(new LaidOutTableOfContentsEntry(
                    section.id().orElseThrow(),
                    section.blockIndex(),
                    BLOCK_X + indent,
                    entryY,
                    entryWidth,
                    entryHeight));
            currentY += entryHeight;
        }
        var height = blockHeight(lines, y);
        return new LaidOutBlock(
                LaidOutBlockKind.TABLE_OF_CONTENTS,
                0,
                BLOCK_X,
                y,
                contentWidth,
                height,
                lines,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new LaidOutTableOfContents(entries)));
    }

    private LaidOutBlock layoutEquationBlock(
            EquationBlock equationBlock,
            int contentWidth,
            int y,
            MathTextMeasurer mathTextMeasurer
    ) {
        var math = mathLayoutEngine.layout(equationBlock.expression(), mathTextMeasurer);
        var blockWidth = math.width() == 0 ? MIN_EQUATION_BLOCK_WIDTH : math.width();
        var blockHeight = math.height() == 0 ? MIN_EQUATION_BLOCK_HEIGHT : math.height();
        var x = blockWidth <= contentWidth ? (contentWidth - blockWidth) / 2 : BLOCK_X;
        return new LaidOutBlock(
                LaidOutBlockKind.EQUATION,
                0,
                x,
                y,
                blockWidth,
                blockHeight,
                List.of(),
                Optional.of(math));
    }

    private static int blockHeight(List<LaidOutLine> lines, int blockY) {
        var last = lines.get(lines.size() - 1);
        return last.y() + last.height() - blockY;
    }

    private static java.util.Set<TextMark> union(java.util.Set<TextMark> left, java.util.Set<TextMark> right) {
        var result = java.util.EnumSet.noneOf(TextMark.class);
        result.addAll(left);
        result.addAll(right);
        return java.util.Set.copyOf(result);
    }

    private int trailingSpacing(LaidOutBlock lastBlock) {
        if (lastBlock.kind() == LaidOutBlockKind.HEADING) {
            return typography.headingSpacingAfter(lastBlock.headingLevel());
        }
        if (lastBlock.kind() == LaidOutBlockKind.EQUATION) {
            return typography.equationSpacingAfter();
        }
        if (lastBlock.kind() == LaidOutBlockKind.TABLE || lastBlock.kind() == LaidOutBlockKind.PLOT || lastBlock.kind() == LaidOutBlockKind.DIAGRAM || lastBlock.kind() == LaidOutBlockKind.FIGURE) {
            return typography.paragraphSpacingAfter();
        }
        return typography.paragraphSpacingAfter();
    }

    private static final class LineBuilder {
        private final int contentWidth;
        private final TextMeasurer textMeasurer;
        private final List<LaidOutLine> lines = new ArrayList<>();
        private final List<LaidOutText> currentRuns = new ArrayList<>();
        private int y;
        private int x;
        private int lineHeight;
        private boolean pendingSpace;
        private int pendingSpaceBlockIndex;
        private int pendingSpaceSourceStart;
        private TextStyle pendingSpaceStyle;

        private LineBuilder(int contentWidth, int y, TextMeasurer textMeasurer) {
            this.contentWidth = contentWidth;
            this.y = y;
            this.textMeasurer = textMeasurer;
        }

        private void append(String text, TextStyle style, int sourceBlockIndex, int sourceStart) {
            for (var token : tokenize(text)) {
                if (token.text().isBlank()) {
                    pendingSpace = true;
                    pendingSpaceBlockIndex = sourceBlockIndex;
                    pendingSpaceSourceStart = sourceStart + token.sourceStart();
                    pendingSpaceStyle = style;
                    continue;
                }
                appendWord(
                        token.text(),
                        style,
                        sourceBlockIndex,
                        sourceStart + token.sourceStart(),
                        pendingSpace ? sourceStart + token.sourceStart() - 1 : sourceStart + token.sourceStart());
            }
        }

        private void appendDisplayOnly(String text, TextStyle style) {
            appendPendingSpace();
            appendFittingOrSplitAtomic(text, style, -1, 0, 0);
        }

        private void appendDisplayText(String text, TextStyle style) {
            append(text, style, -1, 0);
        }

        private void appendSourceAnchor(TextStyle style, int sourceBlockIndex) {
            appendPiece("", style, sourceBlockIndex, 0, 0);
        }

        private void appendAtomic(String text, TextStyle style, int sourceBlockIndex, int sourceStart, int sourceLength) {
            appendPendingSpace();
            appendAtomicWord(text, style, sourceBlockIndex, sourceStart, sourceStart + sourceLength, sourceStart);
        }

        private void appendWord(String word, TextStyle style, int sourceBlockIndex, int wordSourceStart, int sourceStartWithPendingSpace) {
            var prefix = pendingSpace && !currentRuns.isEmpty() ? " " : "";
            pendingSpace = false;
            var token = prefix + word;
            if (fits(token, style) || currentRuns.isEmpty()) {
                appendFittingOrSplit(token, style, sourceBlockIndex, prefix.isEmpty() ? wordSourceStart : sourceStartWithPendingSpace);
                return;
            }

            finishLine();
            appendFittingOrSplit(word, style, sourceBlockIndex, wordSourceStart);
        }

        private void appendFittingOrSplit(String text, TextStyle style, int sourceBlockIndex, int sourceStart) {
            var remaining = text;
            var remainingSourceStart = sourceStart;
            while (!remaining.isEmpty()) {
                if (!currentRuns.isEmpty() && !fits(remaining, style)) {
                    finishLine();
                }

                var fittingLength = fittingLength(remaining, style);
                var piece = remaining.substring(0, fittingLength);
                var pieceCharacters = TextBoundary.characterCount(piece);
                appendPiece(piece, style, sourceBlockIndex, remainingSourceStart, remainingSourceStart + pieceCharacters);
                remaining = remaining.substring(fittingLength);
                remainingSourceStart += pieceCharacters;
                if (!remaining.isEmpty()) {
                    finishLine();
                }
            }
        }

        private void appendAtomicWord(String word, TextStyle style, int sourceBlockIndex, int sourceStart, int sourceEnd, int sourceStartWithPendingSpace) {
            var prefix = pendingSpace && !currentRuns.isEmpty() ? " " : "";
            pendingSpace = false;
            var token = prefix + word;
            if (fits(token, style) || currentRuns.isEmpty()) {
                appendFittingOrSplitAtomic(token, style, sourceBlockIndex, prefix.isEmpty() ? sourceStart : sourceStartWithPendingSpace, sourceEnd);
                return;
            }

            finishLine();
            appendFittingOrSplitAtomic(word, style, sourceBlockIndex, sourceStart, sourceEnd);
        }

        private void appendFittingOrSplitAtomic(String text, TextStyle style, int sourceBlockIndex, int sourceStart, int sourceEnd) {
            var remaining = text;
            while (!remaining.isEmpty()) {
                if (!currentRuns.isEmpty() && !fits(remaining, style)) {
                    finishLine();
                }

                var fittingLength = fittingLength(remaining, style);
                var piece = remaining.substring(0, fittingLength);
                appendPiece(piece, style, sourceBlockIndex, sourceStart, sourceEnd, sourceBlockIndex >= 0);
                remaining = remaining.substring(fittingLength);
                if (!remaining.isEmpty()) {
                    finishLine();
                }
            }
        }

        private boolean fits(String text, TextStyle style) {
            return x + textMeasurer.measureWidth(text, style) <= contentWidth;
        }

        private int fittingLength(String text, TextStyle style) {
            if (fits(text, style)) {
                return text.length();
            }

            var iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
            iterator.setText(text);
            iterator.first();
            var length = iterator.next();
            for (var next = iterator.next(); next != BreakIterator.DONE; next = iterator.next()) {
                if (x + textMeasurer.measureWidth(text.substring(0, next), style) > contentWidth) {
                    break;
                }
                length = next;
            }
            return length;
        }

        private void appendPiece(String text, TextStyle style, int sourceBlockIndex, int sourceStart, int sourceEnd) {
            appendPiece(text, style, sourceBlockIndex, sourceStart, sourceEnd, false);
        }

        private void appendPiece(String text, TextStyle style, int sourceBlockIndex, int sourceStart, int sourceEnd, boolean atomic) {
            var width = textMeasurer.measureWidth(text, style);
            var height = textMeasurer.lineHeight(style);
            currentRuns.add(new LaidOutText(text, style, x, y, width, sourceBlockIndex, sourceStart, sourceEnd, atomic));
            x += width;
            lineHeight = Math.max(lineHeight, height);
        }

        private void finishLine() {
            appendPendingSpace();
            if (currentRuns.isEmpty()) {
                return;
            }
            lines.add(new LaidOutLine(0, y, x, lineHeight, currentRuns));
            currentRuns.clear();
            y += lineHeight;
            x = 0;
            lineHeight = 0;
            pendingSpace = false;
        }

        private int currentY() {
            return y;
        }

        private List<LaidOutLine> finish() {
            finishLine();
            return lines;
        }

        private void appendPendingSpace() {
            if (!pendingSpace || pendingSpaceStyle == null) {
                return;
            }
            if (!fits(" ", pendingSpaceStyle) && !currentRuns.isEmpty()) {
                lines.add(new LaidOutLine(0, y, x, lineHeight, currentRuns));
                currentRuns.clear();
                y += lineHeight;
                x = 0;
                lineHeight = 0;
            }
            appendPiece(" ", pendingSpaceStyle, pendingSpaceBlockIndex, pendingSpaceSourceStart, pendingSpaceSourceStart + 1);
            pendingSpace = false;
            pendingSpaceStyle = null;
        }

        private static List<SourceToken> tokenize(String text) {
            var tokens = new ArrayList<SourceToken>();
            var current = new StringBuilder();
            var whitespace = false;
            var tokenStart = 0;
            var sourceOffset = 0;
            var iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
            iterator.setText(text);
            var index = iterator.first();
            for (var next = iterator.next(); next != BreakIterator.DONE; next = iterator.next()) {
                var characterWhitespace = Character.isWhitespace(text.codePointAt(index));
                if (!current.isEmpty() && characterWhitespace != whitespace) {
                    tokens.add(new SourceToken(current.toString(), tokenStart, sourceOffset));
                    current.setLength(0);
                    tokenStart = sourceOffset;
                }
                current.append(text, index, next);
                whitespace = characterWhitespace;
                sourceOffset++;
                index = next;
            }
            if (!current.isEmpty()) {
                tokens.add(new SourceToken(current.toString(), tokenStart, sourceOffset));
            }
            return tokens;
        }

        private record SourceToken(String text, int sourceStart, int sourceEnd) {
        }
    }
}
