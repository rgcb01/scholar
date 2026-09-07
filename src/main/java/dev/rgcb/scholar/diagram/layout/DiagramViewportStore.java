package dev.rgcb.scholar.diagram.layout;

import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Transient diagram viewport registry that follows unchanged diagram blocks when
 * document-level structural edits shift snapshot-local block indices.
 *
 * <p>Viewport state is deliberately outside the Scholar document AST. Block
 * selections and document editing use snapshot-local indices, so a raw
 * {@code Map<Integer, DiagramViewport>} can accidentally attach one diagram's
 * camera to a different diagram after cut/paste, block insertion, or deletion.
 * This store reconciles cameras against successive immutable document snapshots
 * using block object identity first and same-slot replacement second.</p>
 *
 * <p>When a whole diagram is duplicated and the same immutable block instance
 * appears at multiple new indices, the old camera stays with the original slot
 * when that slot still exists. New duplicates intentionally start at Fit rather
 * than sharing mutable/transient camera state.</p>
 */
public final class DiagramViewportStore {
    private final Map<Integer, DiagramViewport> viewports = new HashMap<>();
    private Document previousDocument;

    public DiagramViewport viewportFor(int blockIndex, DiagramBlock diagram) {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must not be negative.");
        }
        Objects.requireNonNull(diagram, "diagram");
        return viewports.getOrDefault(blockIndex, DiagramViewport.fit(diagram.definition().canvas()));
    }

    public Optional<DiagramViewport> storedViewport(int blockIndex) {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must not be negative.");
        }
        return Optional.ofNullable(viewports.get(blockIndex));
    }

    public void put(int blockIndex, DiagramViewport viewport) {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must not be negative.");
        }
        viewports.put(blockIndex, Objects.requireNonNull(viewport, "viewport"));
    }

    public void reset(int blockIndex, DiagramBlock diagram) {
        Objects.requireNonNull(diagram, "diagram");
        put(blockIndex, DiagramViewport.fit(diagram.definition().canvas()));
    }

    /**
     * Reconciles stored cameras from the previous semantic document snapshot to
     * the supplied current snapshot. Call this only for committed document
     * states, never transient drag-preview documents.
     */
    public void reconcile(Document document) {
        Objects.requireNonNull(document, "document");
        if (previousDocument == null) {
            viewports.entrySet().removeIf(entry -> diagramAt(document, entry.getKey()).isEmpty());
            previousDocument = document;
            return;
        }
        if (previousDocument == document) {
            return;
        }

        var remapped = new HashMap<Integer, DiagramViewport>();
        var relocatedOldIndices = new HashSet<Integer>();
        var entries = viewports.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        // First, follow exact unchanged DiagramBlock objects across block-index
        // shifts. Object identity is intentional here: record equality cannot
        // distinguish two lossless clipboard duplicates with equal content.
        for (var entry : entries) {
            var oldIndex = entry.getKey();
            var oldBlock = diagramAt(previousDocument, oldIndex).orElse(null);
            if (oldBlock == null) {
                continue;
            }
            var matches = identityIndices(document, oldBlock);
            var destination = preferredIdentityDestination(oldIndex, matches);
            if (destination >= 0 && !remapped.containsKey(destination)) {
                remapped.put(destination, entry.getValue());
                relocatedOldIndices.add(oldIndex);
            }
        }

        // If the old block object disappeared but a diagram still occupies the
        // same slot and that new object did not merely shift in from elsewhere,
        // treat it as an immutable replacement of the same authored diagram.
        // This preserves zoom/pan across rotate, drag, resize, annotation edits,
        // undo, and redo while avoiding leakage onto a neighboring shifted block.
        for (var entry : entries) {
            var oldIndex = entry.getKey();
            if (relocatedOldIndices.contains(oldIndex) || remapped.containsKey(oldIndex)) {
                continue;
            }
            var replacement = diagramAt(document, oldIndex).orElse(null);
            if (replacement == null) {
                continue;
            }
            var previousMatches = identityIndices(previousDocument, replacement);
            if (previousMatches.isEmpty() || previousMatches.contains(oldIndex)) {
                remapped.put(oldIndex, entry.getValue());
            }
        }

        viewports.clear();
        viewports.putAll(remapped);
        previousDocument = document;
    }

    private static int preferredIdentityDestination(int oldIndex, List<Integer> matches) {
        if (matches.isEmpty()) {
            return -1;
        }
        if (matches.contains(oldIndex)) {
            return oldIndex;
        }
        return matches.size() == 1 ? matches.get(0) : -1;
    }

    private static Optional<DiagramBlock> diagramAt(Document document, int blockIndex) {
        if (blockIndex < 0 || blockIndex >= document.blocks().size()) {
            return Optional.empty();
        }
        var block = document.blocks().get(blockIndex);
        return block instanceof DiagramBlock diagram ? Optional.of(diagram) : Optional.empty();
    }

    private static List<Integer> identityIndices(Document document, DiagramBlock target) {
        var matches = new ArrayList<Integer>();
        for (var index = 0; index < document.blocks().size(); index++) {
            if (document.blocks().get(index) == target) {
                matches.add(index);
            }
        }
        return List.copyOf(matches);
    }
}
