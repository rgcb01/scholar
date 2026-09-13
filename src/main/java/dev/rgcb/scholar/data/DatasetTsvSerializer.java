package dev.rgcb.scholar.data;

import java.util.Objects;
import java.util.stream.Collectors;

public final class DatasetTsvSerializer {
    public String serialize(ScientificDataset dataset) {
        Objects.requireNonNull(dataset, "dataset");
        var output = new StringBuilder();
        output.append("Dataset: ").append(dataset.displayLabel()).append('\n').append('\n');
        output.append(dataset.columns().stream()
                .map(DatasetColumn::displayName)
                .collect(Collectors.joining("\t")));
        for (var row : dataset.rows()) {
            output.append('\n');
            output.append(row.values().stream()
                    .map(DatasetValue::displayText)
                    .collect(Collectors.joining("\t")));
        }
        return output.toString();
    }
}
