package dev.rgcb.scholar.document;

import dev.rgcb.scholar.data.ScientificDataset;
import java.util.List;
import java.util.Objects;

/**
 * The semantic source of truth for a Scholar document.
 */
public record Document(List<BlockNode> blocks, List<ScientificDataset> datasets, DocumentSettings settings) {
    public Document(List<BlockNode> blocks) {
        this(blocks, List.of(), DocumentSettings.blank());
    }

    public Document(List<BlockNode> blocks, List<ScientificDataset> datasets) {
        this(blocks, datasets, DocumentSettings.blank());
    }

    public Document {
        blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks"));
        datasets = List.copyOf(Objects.requireNonNull(datasets, "datasets"));
        settings = Objects.requireNonNull(settings, "settings");
        var ids = new java.util.HashSet<String>();
        for (var dataset : datasets) {
            if (!ids.add(dataset.id())) {
                throw new IllegalArgumentException("dataset IDs must be unique.");
            }
        }
    }

    public Document withSettings(DocumentSettings replacement) { return new Document(blocks, datasets, replacement); }
}
