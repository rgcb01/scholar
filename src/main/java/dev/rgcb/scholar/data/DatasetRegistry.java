package dev.rgcb.scholar.data;

import dev.rgcb.scholar.document.Document;
import java.util.Objects;
import java.util.Optional;

public final class DatasetRegistry {
    public Optional<ScientificDataset> find(Document document, String datasetId) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(datasetId, "datasetId");
        return document.datasets().stream()
                .filter(dataset -> dataset.id().equals(datasetId))
                .findFirst();
    }
}
