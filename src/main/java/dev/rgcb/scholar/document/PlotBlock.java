package dev.rgcb.scholar.document;

import dev.rgcb.scholar.plot.PlotDefinition;
import java.util.Objects;

public record PlotBlock(PlotDefinition definition) implements BlockNode {
    public PlotBlock {
        definition = Objects.requireNonNull(definition, "definition");
    }
}
