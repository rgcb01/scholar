package dev.rgcb.scholar.client;

import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.*;
import java.util.ArrayList;
import java.util.List;

/** Deterministic development fixtures, not a document authoring API. */
public final class DevelopmentStressDocument {
    public enum Profile { SMALL, MEDIUM, LARGE, DATA_HEAVY, DIAGRAM_HEAVY, MIXED_STRESS }

    private DevelopmentStressDocument() { }

    public static Document create(Profile profile) {
        var base = DevelopmentDocument.createPersistenceFixture();
        if (profile == Profile.SMALL) {
            return base;
        }
        var count = switch (profile) {
            case MEDIUM -> 160;
            case LARGE -> 1000;
            case DATA_HEAVY -> 120;
            case DIAGRAM_HEAVY -> 180;
            case MIXED_STRESS -> 240;
            default -> throw new IllegalArgumentException("Unsupported profile");
        };
        var candidates = profile == Profile.DIAGRAM_HEAVY
                ? base.blocks().stream().filter(block -> block instanceof DiagramBlock
                        || block instanceof Paragraph || block instanceof Heading).toList()
                : base.blocks();
        var blocks = new ArrayList<BlockNode>();
        for (var i = 0; i < count; i++) {
            var block = candidates.get(i % candidates.size());
            var cycle = i / candidates.size();
            blocks.add(withFixtureIdentity(block, cycle));
        }
        var datasets = base.datasets();
        if (profile == Profile.DATA_HEAVY) {
            datasets = datasets.stream().map(dataset -> {
                var rows = new ArrayList<DatasetRow>();
                if (!dataset.rows().isEmpty()) {
                    for (var i = 0; i < 2000; i++) {
                        rows.add(new DatasetRow("stress-row-" + i,
                                dataset.rows().get(i % dataset.rows().size()).values()));
                    }
                }
                return new ScientificDataset(dataset.id(), dataset.displayName(), dataset.columns(), rows);
            }).toList();
        }
        return new Document(blocks, datasets);
    }

    private static BlockNode withFixtureIdentity(BlockNode block, int cycle) {
        if (cycle == 0) { return block; }
        var suffix = "-stress-" + cycle;
        if (block instanceof Heading heading && heading.id().isPresent()) {
            return heading.withId(heading.id().orElseThrow() + suffix);
        }
        if (block instanceof EquationBlock equation && equation.id().isPresent()) {
            return equation.withId(equation.id().orElseThrow() + suffix);
        }
        if (block instanceof TableBlock table && table.id().isPresent()) {
            return table.withId(table.id().orElseThrow() + suffix);
        }
        if (block instanceof FigureBlock figure) {
            return new FigureBlock(figure.id() + suffix, figure.content(), figure.caption());
        }
        return block;
    }
}
