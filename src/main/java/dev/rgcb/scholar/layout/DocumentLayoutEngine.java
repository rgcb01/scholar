package dev.rgcb.scholar.layout;

import dev.rgcb.scholar.diagram.layout.DiagramLayoutEngine;
import dev.rgcb.scholar.diagram.layout.DiagramViewport;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.editor.TextBoundary;
import dev.rgcb.scholar.math.layout.MathLayoutEngine;
import dev.rgcb.scholar.math.layout.MathTextMeasurer;
import dev.rgcb.scholar.plot.layout.PlotLayoutEngine;
import dev.rgcb.scholar.typography.ScholarTypography;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Minecraft-independent layout engine for read-only Scholar documents.
 */
public final class DocumentLayoutEngine {
    private static final int BLOCK_X = 0;
    private static final int MIN_EQUATION_BLOCK_WIDTH = 48;
    private static final int MIN_EQUATION_BLOCK_HEIGHT = 18;

    private final MathLayoutEngine mathLayoutEngine = new MathLayoutEngine();
    private final TableLayoutEngine tableLayoutEngine = new TableLayoutEngine();
    private final PlotLayoutEngine plotLayoutEngine = new PlotLayoutEngine();
    private final DiagramLayoutEngine diagramLayoutEngine = new DiagramLayoutEngine();
    private final ScholarTypography typography;

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

        for (var blockIndex = 0; blockIndex < document.blocks().size(); blockIndex++) {
            var block = document.blocks().get(blockIndex);
            if (block instanceof Heading heading) {
                y += typography.headingSpacingBefore(heading.level(), blocks.isEmpty());
                var style = TextStyle.heading(heading.level());
                var laidOut = layoutTextBlock(
                        LaidOutBlockKind.HEADING,
                        heading.level(),
                        heading.content(),
                        style,
                        contentWidth,
                        y,
                        textMeasurer,
                        blockIndex);
                blocks.add(laidOut);
                y = laidOut.y() + laidOut.height() + typography.headingSpacingAfter(heading.level());
            } else if (block instanceof Paragraph paragraph) {
                var laidOut = layoutTextBlock(
                        LaidOutBlockKind.PARAGRAPH,
                        0,
                        paragraph.content(),
                        null,
                        contentWidth,
                        y,
                        textMeasurer,
                        blockIndex);
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
                var table = tableLayoutEngine.layout(tableBlock, blockIndex, BLOCK_X, y, contentWidth, textMeasurer);
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
                var plot = plotLayoutEngine.layout(plotBlock, blockIndex, BLOCK_X, y, contentWidth, textMeasurer);
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
            } else {
                throw new IllegalArgumentException("Unsupported block node: " + block.getClass().getName());
            }
        }

        var height = blocks.isEmpty() ? 0 : Math.max(0, y - trailingSpacing(blocks.get(blocks.size() - 1)));
        return new LaidOutDocument(contentWidth, height, blocks);
    }

    private static LaidOutBlock layoutTextBlock(
            LaidOutBlockKind kind,
            int headingLevel,
            InlineContent content,
            TextStyle forcedStyle,
            int contentWidth,
            int y,
            TextMeasurer textMeasurer,
            int sourceBlockIndex
    ) {
        var builder = new LineBuilder(contentWidth, y, textMeasurer);
        var sourceOffset = 0;
        for (InlineNode node : content.nodes()) {
            if (node instanceof Text text) {
                var style = forcedStyle == null ? TextStyle.paragraph(text.marks()) : forcedStyle.withMarks(text.marks());
                builder.append(text.content(), style, sourceBlockIndex, sourceOffset);
                sourceOffset += TextBoundary.characterCount(text.content());
            } else {
                throw new IllegalArgumentException("Unsupported inline node: " + node.getClass().getName());
            }
        }

        var lines = builder.finish();
        if (lines.isEmpty()) {
            var emptyLineHeight = textMeasurer.lineHeight(forcedStyle == null ? TextStyle.paragraph() : forcedStyle);
            lines = List.of(new LaidOutLine(0, y, 0, emptyLineHeight, List.of()));
        }
        var height = blockHeight(lines, y);
        return new LaidOutBlock(kind, headingLevel, BLOCK_X, y, contentWidth, height, lines);
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

    private int trailingSpacing(LaidOutBlock lastBlock) {
        if (lastBlock.kind() == LaidOutBlockKind.HEADING) {
            return typography.headingSpacingAfter(lastBlock.headingLevel());
        }
        if (lastBlock.kind() == LaidOutBlockKind.EQUATION) {
            return typography.equationSpacingAfter();
        }
        if (lastBlock.kind() == LaidOutBlockKind.TABLE || lastBlock.kind() == LaidOutBlockKind.PLOT || lastBlock.kind() == LaidOutBlockKind.DIAGRAM) {
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

        private boolean fits(String text, TextStyle style) {
            return x + textMeasurer.measureWidth(text, style) <= contentWidth;
        }

        private int fittingLength(String text, TextStyle style) {
            if (fits(text, style)) {
                return text.length();
            }

            var length = 1;
            for (var next = 2; next <= text.length(); next++) {
                if (x + textMeasurer.measureWidth(text.substring(0, next), style) > contentWidth) {
                    break;
                }
                length = next;
            }
            return length;
        }

        private void appendPiece(String text, TextStyle style, int sourceBlockIndex, int sourceStart, int sourceEnd) {
            var width = textMeasurer.measureWidth(text, style);
            var height = textMeasurer.lineHeight(style);
            currentRuns.add(new LaidOutText(text, style, x, y, width, sourceBlockIndex, sourceStart, sourceEnd));
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
            for (var index = 0; index < text.length(); index++) {
                var character = text.charAt(index);
                var characterWhitespace = Character.isWhitespace(character);
                if (!current.isEmpty() && characterWhitespace != whitespace) {
                    tokens.add(new SourceToken(current.toString(), tokenStart, sourceOffset));
                    current.setLength(0);
                    tokenStart = sourceOffset;
                }
                current.append(character);
                whitespace = characterWhitespace;
                sourceOffset += TextBoundary.characterCount(String.valueOf(character));
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
