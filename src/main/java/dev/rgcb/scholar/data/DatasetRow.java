package dev.rgcb.scholar.data;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DatasetRow(Optional<String> id, List<DatasetValue> values) {
    public DatasetRow(List<DatasetValue> values) {
        this(Optional.empty(), values);
    }

    public DatasetRow(String id, List<DatasetValue> values) {
        this(Optional.of(id), values);
    }

    public DatasetRow {
        id = Objects.requireNonNull(id, "id").map(String::trim).filter(value -> !value.isEmpty());
        values = List.copyOf(Objects.requireNonNull(values, "values"));
    }

    public DatasetRow withValues(List<DatasetValue> replacement) {
        return new DatasetRow(id, replacement);
    }
}
