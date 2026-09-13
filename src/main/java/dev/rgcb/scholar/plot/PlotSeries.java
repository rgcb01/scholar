package dev.rgcb.scholar.plot;

import dev.rgcb.scholar.data.DatasetPlotBinding;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PlotSeries(String name, PlotSeriesKind kind, List<DataPoint> points, Optional<DatasetPlotBinding> datasetBinding) {
    public PlotSeries(String name, PlotSeriesKind kind, List<DataPoint> points) {
        this(name, kind, points, Optional.empty());
    }

    public PlotSeries(String name, PlotSeriesKind kind, DatasetPlotBinding datasetBinding) {
        this(name, kind, List.of(), Optional.of(datasetBinding));
    }

    public PlotSeries {
        name = Objects.requireNonNull(name, "name");
        kind = Objects.requireNonNull(kind, "kind");
        points = List.copyOf(Objects.requireNonNull(points, "points"));
        datasetBinding = Objects.requireNonNull(datasetBinding, "datasetBinding");
    }

    public PlotSeries withDatasetBinding(DatasetPlotBinding binding) {
        return new PlotSeries(name, kind, points, Optional.of(binding));
    }

    public PlotSeries withPoints(List<DataPoint> replacement) {
        return new PlotSeries(name, kind, replacement, datasetBinding);
    }
}
