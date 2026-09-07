package dev.rgcb.scholar.editor;

import java.util.Objects;

public record PlotPropertyTarget(PlotProperty property) implements PlotEditTarget {
    public PlotPropertyTarget {
        property = Objects.requireNonNull(property, "property");
    }
}
