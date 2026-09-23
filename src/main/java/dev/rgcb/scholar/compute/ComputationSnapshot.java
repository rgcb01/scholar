package dev.rgcb.scholar.compute;

import java.util.Map;

/** Derived results indexed by document block position for one immutable document. */
public record ComputationSnapshot(Map<Integer, ComputationResult> results) {
    public ComputationSnapshot { results = Map.copyOf(results); }
}
