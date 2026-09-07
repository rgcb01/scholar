package dev.rgcb.scholar.plot.layout;

import java.util.Objects;

/** Positioned tick value/label for one axis. coordinate is x for X ticks and y for Y ticks. */
public record LaidOutPlotTick(double value, int coordinate, LaidOutPlotLabel label) {
    public LaidOutPlotTick {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Tick value must be finite.");
        }
        label = Objects.requireNonNull(label, "label");
    }
}
