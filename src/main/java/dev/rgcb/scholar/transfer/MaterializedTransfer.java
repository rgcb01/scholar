package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.data.ScientificDataset;
import java.util.List;
import java.util.Objects;

/** Prepared semantic values only; construction is owned by the materializer. */
public final class MaterializedTransfer {
    private final TransferPlan plan;
    private final FragmentContent content;
    private final List<ScientificDataset> resourceAdditions;

    MaterializedTransfer(TransferPlan plan, FragmentContent content, List<ScientificDataset> additions) {
        this.plan = Objects.requireNonNull(plan);
        this.content = Objects.requireNonNull(content);
        this.resourceAdditions = List.copyOf(additions);
    }

    public TransferPlan plan() { return plan; }
    public FragmentContent content() { return content; }
    public List<ScientificDataset> resourceAdditions() { return resourceAdditions; }
}
