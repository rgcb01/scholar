package dev.rgcb.scholar.plot.layout;

import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Pure-Java layout for PlotBlock. M16C resolves linear ranges, deterministic
 * nice ticks and data-to-plot transforms. M16D additionally derives clipped
 * LINE/SCATTER series geometry and deterministic non-color-only styling.
 */
public final class PlotLayoutEngine {
    public static final int OUTER_PADDING = 8;
    public static final int RIGHT_PADDING = 8;
    public static final int AXIS_LABEL_GAP = 5;
    public static final int TITLE_GAP = 6;
    public static final int Y_LABEL_GAP = 3;
    public static final int TICK_LENGTH = 4;
    public static final int TICK_LABEL_GAP = 3;
    public static final int LEGEND_MARGIN = 4;
    public static final int LEGEND_PADDING = 4;
    public static final int LEGEND_SAMPLE_WIDTH = 16;
    public static final int LEGEND_SAMPLE_GAP = 4;
    public static final int LEGEND_ITEM_GAP = 2;
    public static final int X_TICK_MIN_SPACING = 56;
    public static final int Y_TICK_MIN_SPACING = 28;
    public static final int TICK_LABEL_MIN_GAP = 2;

    private final PlotRangeResolver rangeResolver = new PlotRangeResolver();
    private final NiceTickGenerator tickGenerator = new NiceTickGenerator();
    private final PlotSeriesLayoutEngine seriesLayoutEngine = new PlotSeriesLayoutEngine();

    public LaidOutPlot layout(
            PlotBlock plotBlock,
            int sourceBlockIndex,
            int x,
            int y,
            int width,
            TextMeasurer textMeasurer
    ) {
        Objects.requireNonNull(plotBlock, "plotBlock");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        if (sourceBlockIndex < 0) {
            throw new IllegalArgumentException("sourceBlockIndex must not be negative.");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive.");
        }

        var definition = plotBlock.definition();
        var titleStyle = TextStyle.paragraph(Set.of(TextMark.BOLD));
        var labelStyle = TextStyle.paragraph();
        var tickStyle = TextStyle.paragraph();
        var ranges = rangeResolver.resolve(definition);
        var tickLineHeight = textMeasurer.lineHeight(tickStyle);

        var title = layoutCenteredLabel(
                definition.title(), x, y + OUTER_PADDING, width, titleStyle, textMeasurer);
        var topCursor = y + OUTER_PADDING + title.map(label -> label.height() + TITLE_GAP).orElse(0);

        // Until rotated text is justified, the Y-axis label is laid out as a
        // horizontal scientific label above the plot area. It no longer shares
        // the plot frame/tick gutter as it did in the M16B placeholder.
        var yAxisLabel = layoutLabel(
                definition.yAxis().label(), x + OUTER_PADDING, topCursor, labelStyle, textMeasurer);
        topCursor += yAxisLabel.map(label -> label.height() + Y_LABEL_GAP).orElse(0);

        var xAxisLabelHeight = definition.xAxis().label().isBlank()
                ? 0
                : AXIS_LABEL_GAP + textMeasurer.lineHeight(labelStyle);
        var bottomReservation = OUTER_PADDING + TICK_LENGTH + TICK_LABEL_GAP + tickLineHeight + xAxisLabelHeight;
        var plotAreaY = topCursor;
        var desiredBottom = y + definition.height() - bottomReservation;
        var plotAreaHeight = Math.max(1, desiredBottom - plotAreaY);

        // Tick density is a layout concern: narrow plots should not retain the
        // same semantic tick count as wide plots and then stack labels on top
        // of one another. Resolve Y first because its labels determine the
        // left gutter, then resolve X from the remaining plot-area width.
        var semanticYTicks = tickGenerator.generate(
                ranges.yRange(),
                targetTickCount(plotAreaHeight, Y_TICK_MIN_SPACING));
        var maxYTickLabelWidth = semanticYTicks.stream()
                .mapToInt(tick -> textMeasurer.measureWidth(tick.label(), tickStyle))
                .max()
                .orElse(0);
        var desiredLeftGutter = OUTER_PADDING + maxYTickLabelWidth + TICK_LABEL_GAP + TICK_LENGTH;
        var leftGutter = width < 24 ? 0 : Math.min(desiredLeftGutter, Math.max(0, width / 3));
        var rightPadding = Math.min(RIGHT_PADDING, Math.max(0, width - leftGutter - 1));
        var plotAreaX = x + leftGutter;
        var plotAreaWidth = Math.max(1, width - leftGutter - rightPadding);
        var semanticXTicks = tickGenerator.generate(
                ranges.xRange(),
                targetTickCount(plotAreaWidth, X_TICK_MIN_SPACING));

        var transform = new PlotCoordinateTransform(
                ranges.xRange(),
                ranges.yRange(),
                plotAreaX,
                plotAreaY,
                plotAreaWidth,
                plotAreaHeight);

        var xTicks = layoutXTicks(
                semanticXTicks,
                transform,
                x,
                width,
                plotAreaY + plotAreaHeight,
                tickStyle,
                textMeasurer);
        var yTicks = layoutYTicks(
                semanticYTicks,
                transform,
                plotAreaX,
                x,
                tickStyle,
                textMeasurer);

        var xAxisLabel = layoutCenteredLabel(
                definition.xAxis().label(),
                plotAreaX,
                plotAreaY + plotAreaHeight + TICK_LENGTH + TICK_LABEL_GAP + tickLineHeight + AXIS_LABEL_GAP,
                plotAreaWidth,
                labelStyle,
                textMeasurer);
        var laidOutSeries = seriesLayoutEngine.layout(definition.series(), transform);
        var legend = layoutLegend(
                definition.legendVisible(),
                laidOutSeries,
                plotAreaX,
                plotAreaY,
                plotAreaWidth,
                plotAreaHeight,
                labelStyle,
                textMeasurer);

        return new LaidOutPlot(
                sourceBlockIndex,
                x,
                y,
                width,
                definition.height(),
                plotAreaX,
                plotAreaY,
                plotAreaWidth,
                plotAreaHeight,
                title,
                xAxisLabel,
                yAxisLabel,
                ranges.xRange(),
                ranges.yRange(),
                xTicks,
                yTicks,
                transform,
                definition.gridVisible(),
                laidOutSeries,
                legend);
    }


    private static int targetTickCount(int availableLength, int minimumSpacing) {
        if (minimumSpacing <= 0) {
            throw new IllegalArgumentException("minimumSpacing must be positive.");
        }
        var bySpace = 1 + Math.max(0, availableLength) / minimumSpacing;
        return Math.max(2, Math.min(NiceTickGenerator.DEFAULT_TARGET_TICKS, bySpace));
    }

    private static Optional<LaidOutPlotLegend> layoutLegend(
            boolean legendVisible,
            List<LaidOutPlotSeries> series,
            int plotAreaX,
            int plotAreaY,
            int plotAreaWidth,
            int plotAreaHeight,
            TextStyle style,
            TextMeasurer textMeasurer
    ) {
        if (!legendVisible) {
            return Optional.empty();
        }
        var named = series.stream().filter(item -> !item.name().isBlank()).toList();
        if (named.isEmpty()) {
            return Optional.empty();
        }

        var lineHeight = textMeasurer.lineHeight(style);
        var maxLabelWidth = named.stream()
                .mapToInt(item -> textMeasurer.measureWidth(item.name(), style))
                .max()
                .orElse(0);
        var width = LEGEND_PADDING * 2 + LEGEND_SAMPLE_WIDTH + LEGEND_SAMPLE_GAP + maxLabelWidth;
        var height = LEGEND_PADDING * 2 + named.size() * lineHeight + Math.max(0, named.size() - 1) * LEGEND_ITEM_GAP;
        if (width + LEGEND_MARGIN * 2 > plotAreaWidth || height + LEGEND_MARGIN * 2 > plotAreaHeight) {
            return Optional.empty();
        }

        var legendX = plotAreaX + plotAreaWidth - LEGEND_MARGIN - width;
        var legendY = plotAreaY + LEGEND_MARGIN;
        var items = new ArrayList<LaidOutPlotLegendItem>(named.size());
        for (var i = 0; i < named.size(); i++) {
            var seriesItem = named.get(i);
            var itemTop = legendY + LEGEND_PADDING + i * (lineHeight + LEGEND_ITEM_GAP);
            var sampleX1 = legendX + LEGEND_PADDING;
            var sampleX2 = sampleX1 + LEGEND_SAMPLE_WIDTH;
            var sampleY = itemTop + lineHeight / 2;
            var labelX = sampleX2 + LEGEND_SAMPLE_GAP;
            var labelWidth = textMeasurer.measureWidth(seriesItem.name(), style);
            var label = new LaidOutPlotLabel(
                    seriesItem.name(),
                    labelX,
                    itemTop,
                    labelWidth,
                    lineHeight,
                    style);
            items.add(new LaidOutPlotLegendItem(
                    seriesItem.seriesIndex(),
                    seriesItem.kind(),
                    seriesItem.style(),
                    sampleX1,
                    sampleX2,
                    sampleY,
                    label));
        }
        return Optional.of(new LaidOutPlotLegend(legendX, legendY, width, height, items));
    }

    private static List<LaidOutPlotTick> layoutXTicks(
            List<AxisTick> ticks,
            PlotCoordinateTransform transform,
            int blockX,
            int blockWidth,
            int axisY,
            TextStyle style,
            TextMeasurer textMeasurer
    ) {
        var result = new ArrayList<LaidOutPlotTick>(ticks.size());
        var minLabelX = blockX;
        var maxBlockX = blockX + blockWidth;
        for (var tick : ticks) {
            var coordinate = (int) Math.round(transform.mapX(tick.value()));
            var labelWidth = textMeasurer.measureWidth(tick.label(), style);
            var labelX = coordinate - labelWidth / 2;
            labelX = Math.max(minLabelX, Math.min(labelX, maxBlockX - labelWidth));
            var label = new LaidOutPlotLabel(
                    tick.label(),
                    labelX,
                    axisY + TICK_LENGTH + TICK_LABEL_GAP,
                    labelWidth,
                    textMeasurer.lineHeight(style),
                    style);
            result.add(new LaidOutPlotTick(tick.value(), coordinate, label));
        }
        return withoutOverlappingLabels(result, true);
    }

    private static List<LaidOutPlotTick> layoutYTicks(
            List<AxisTick> ticks,
            PlotCoordinateTransform transform,
            int axisX,
            int blockX,
            TextStyle style,
            TextMeasurer textMeasurer
    ) {
        var result = new ArrayList<LaidOutPlotTick>(ticks.size());
        var lineHeight = textMeasurer.lineHeight(style);
        for (var tick : ticks) {
            var coordinate = (int) Math.round(transform.mapY(tick.value()));
            var labelWidth = textMeasurer.measureWidth(tick.label(), style);
            var labelX = Math.max(blockX, axisX - TICK_LENGTH - TICK_LABEL_GAP - labelWidth);
            var label = new LaidOutPlotLabel(
                    tick.label(),
                    labelX,
                    coordinate - lineHeight / 2,
                    labelWidth,
                    lineHeight,
                    style);
            result.add(new LaidOutPlotTick(tick.value(), coordinate, label));
        }
        return withoutOverlappingLabels(result, false);
    }

    private static List<LaidOutPlotTick> withoutOverlappingLabels(
            List<LaidOutPlotTick> ticks,
            boolean horizontal
    ) {
        if (ticks.size() < 2) {
            return List.copyOf(ticks);
        }
        var kept = new ArrayList<LaidOutPlotTick>(ticks.size());
        for (var tick : ticks) {
            var label = tick.label();
            var start = horizontal ? label.x() : label.y();
            var end = start + (horizontal ? label.width() : label.height());
            var overlaps = false;
            for (var existing : kept) {
                var existingLabel = existing.label();
                var existingStart = horizontal ? existingLabel.x() : existingLabel.y();
                var existingEnd = existingStart + (horizontal ? existingLabel.width() : existingLabel.height());
                if (start < existingEnd + TICK_LABEL_MIN_GAP && end + TICK_LABEL_MIN_GAP > existingStart) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                kept.add(tick);
            }
        }
        return List.copyOf(kept);
    }

    private static Optional<LaidOutPlotLabel> layoutCenteredLabel(
            String text,
            int x,
            int y,
            int availableWidth,
            TextStyle style,
            TextMeasurer textMeasurer
    ) {
        if (text.isBlank()) {
            return Optional.empty();
        }
        var width = textMeasurer.measureWidth(text, style);
        var drawX = x + Math.max(0, (availableWidth - width) / 2);
        return Optional.of(new LaidOutPlotLabel(text, drawX, y, width, textMeasurer.lineHeight(style), style));
    }

    private static Optional<LaidOutPlotLabel> layoutLabel(
            String text,
            int x,
            int y,
            TextStyle style,
            TextMeasurer textMeasurer
    ) {
        if (text.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new LaidOutPlotLabel(
                text,
                x,
                y,
                textMeasurer.measureWidth(text, style),
                textMeasurer.lineHeight(style),
                style));
    }
}
