package dev.rgcb.scholar.plot.clipboard;

import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.AxisRange;
import dev.rgcb.scholar.plot.PlotSeries;
import java.util.Objects;

/**
 * Deterministic, human-readable plain-text fallback for whole-plot clipboard operations.
 * This format is intentionally not parsed back into PlotBlock; native Scholar clipboard
 * payloads preserve the lossless semantic representation.
 */
public final class PlotPlainTextSerializer {
    public String serialize(PlotBlock plot) {
        Objects.requireNonNull(plot, "plot");
        var definition = plot.definition();
        var output = new StringBuilder();
        output.append("Plot: ").append(sanitize(definition.title())).append('\n');
        appendAxis(output, "X", definition.xAxis());
        appendAxis(output, "Y", definition.yAxis());
        output.append("Legend: ").append(definition.legendVisible() ? "on" : "off").append('\n');
        output.append("Grid: ").append(definition.gridVisible() ? "on" : "off").append('\n');
        output.append("Height: ").append(definition.height());

        for (var series : definition.series()) {
            output.append("\n\n");
            appendSeries(output, series);
        }
        return output.toString();
    }

    private static void appendAxis(StringBuilder output, String axisName, AxisDefinition axis) {
        output.append(axisName).append(" Axis: ").append(sanitize(axis.label())).append('\n');
        output.append(axisName).append(" Range: ");
        if (axis.explicitRange().isPresent()) {
            var range = axis.explicitRange().orElseThrow();
            output.append(formatRange(range));
        } else {
            output.append("auto");
        }
        output.append('\n');
    }

    private static String formatRange(AxisRange range) {
        return "[" + Double.toString(range.min()) + ", " + Double.toString(range.max()) + "]";
    }

    private static void appendSeries(StringBuilder output, PlotSeries series) {
        output.append("Series: ")
                .append(sanitize(series.name()))
                .append(" [")
                .append(series.kind().name())
                .append("]\n");
        output.append("x\ty");
        for (var point : series.points()) {
            output.append('\n')
                    .append(Double.toString(point.x()))
                    .append('\t')
                    .append(Double.toString(point.y()));
        }
    }

    private static String sanitize(String text) {
        return Objects.requireNonNull(text, "text")
                .replace("\r\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
    }
}
