package dev.rgcb.scholar.data;

import java.util.Objects;

public record DatasetPlotBinding(String datasetId, String xColumnId, String yColumnId) {
    public DatasetPlotBinding {
        datasetId = normalizeId(datasetId, "datasetId");
        xColumnId = normalizeId(xColumnId, "xColumnId");
        yColumnId = normalizeId(yColumnId, "yColumnId");
    }

    private static String normalizeId(String value, String name) {
        var normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return normalized;
    }
}
