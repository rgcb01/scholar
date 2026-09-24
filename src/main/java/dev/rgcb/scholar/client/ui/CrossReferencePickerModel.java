package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.document.CrossReferenceTarget;
import java.util.List;
import java.util.Optional;

/** Transient picker navigation; target identity remains owned by CrossReferenceTarget. */
public final class CrossReferencePickerModel {
    public static final int PAGE_SIZE = 8;
    private List<CrossReferenceTarget> targets = List.of();
    private int selectedIndex;

    public void setTargets(List<CrossReferenceTarget> targets) {
        this.targets = List.copyOf(targets);
        selectedIndex = 0;
    }

    public List<CrossReferenceTarget> visibleTargets() {
        var first = page() * PAGE_SIZE;
        return targets.subList(first, Math.min(targets.size(), first + PAGE_SIZE));
    }

    public boolean isEmpty() { return targets.isEmpty(); }
    public int rowCapacity() { return Math.min(PAGE_SIZE, targets.size()); }
    public int page() { return selectedIndex / PAGE_SIZE; }
    public int pageCount() { return Math.max(1, (targets.size() + PAGE_SIZE - 1) / PAGE_SIZE); }
    public int selectedRow() { return selectedIndex % PAGE_SIZE; }

    public void moveSelection(int delta) {
        if (!targets.isEmpty()) selectedIndex = Math.max(0, Math.min(targets.size() - 1, selectedIndex + delta));
    }

    public void movePage(int delta) {
        if (targets.isEmpty()) return;
        var destination = Math.max(0, Math.min(pageCount() - 1, page() + delta));
        selectedIndex = Math.min(targets.size() - 1, destination * PAGE_SIZE + selectedRow());
    }

    public Optional<CrossReferenceTarget> selected() {
        return targets.isEmpty() ? Optional.empty() : Optional.of(targets.get(selectedIndex));
    }

    public Optional<CrossReferenceTarget> selectVisibleRow(int row) {
        var index = page() * PAGE_SIZE + row;
        if (row < 0 || index >= targets.size() || row >= PAGE_SIZE) return Optional.empty();
        selectedIndex = index;
        return selected();
    }
}
