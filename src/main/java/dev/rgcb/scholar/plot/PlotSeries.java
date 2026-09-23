package dev.rgcb.scholar.plot;

import dev.rgcb.scholar.data.DatasetPlotBinding;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PlotSeries(String name, PlotSeriesKind kind, List<DataPoint> points,
                         Optional<DatasetPlotBinding> datasetBinding, Optional<String> fitAnalysisId) {
    public PlotSeries(String name, PlotSeriesKind kind, List<DataPoint> points) {
        this(name, kind, points, Optional.empty(), Optional.empty());
    }

    public PlotSeries(String name, PlotSeriesKind kind, List<DataPoint> points,
                      Optional<DatasetPlotBinding> datasetBinding) {
        this(name, kind, points, datasetBinding, Optional.empty());
    }

    public PlotSeries(String name, PlotSeriesKind kind, DatasetPlotBinding datasetBinding) {
        this(name, kind, List.of(), Optional.of(datasetBinding), Optional.empty());
    }

    public static PlotSeries fit(String name, String analysisId) {
        return new PlotSeries(name, PlotSeriesKind.LINE, List.of(), Optional.empty(), Optional.of(analysisId));
    }

    public PlotSeries {
        name = Objects.requireNonNull(name, "name");
        kind = Objects.requireNonNull(kind, "kind");
        points = List.copyOf(Objects.requireNonNull(points, "points"));
        datasetBinding = Objects.requireNonNull(datasetBinding, "datasetBinding");
        fitAnalysisId = Objects.requireNonNull(fitAnalysisId, "fitAnalysisId");
        if (fitAnalysisId.filter(String::isBlank).isPresent() || fitAnalysisId.isPresent()
                && (datasetBinding.isPresent() || !points.isEmpty() || kind != PlotSeriesKind.LINE)) {
            throw new IllegalArgumentException("Fit series must contain only an analysis reference.");
        }
    }

    public PlotSeries withDatasetBinding(DatasetPlotBinding binding) {
        return new PlotSeries(name, kind, points, Optional.of(binding), Optional.empty());
    }

    public PlotSeries withPoints(List<DataPoint> replacement) {
        return fitAnalysisId.isPresent() ? new PlotSeries(name, kind, replacement)
                : new PlotSeries(name, kind, replacement, datasetBinding);
    }
}
