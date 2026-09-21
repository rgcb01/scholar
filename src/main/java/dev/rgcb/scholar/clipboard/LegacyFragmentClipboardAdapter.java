package dev.rgcb.scholar.clipboard;

import dev.rgcb.scholar.data.clipboard.DatasetClipboardPayload;
import dev.rgcb.scholar.diagram.clipboard.DiagramClipboardPayload;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.figure.clipboard.FigureClipboardPayload;
import dev.rgcb.scholar.plot.clipboard.PlotClipboardPayload;
import dev.rgcb.scholar.table.clipboard.TableClipboardPayload;
import dev.rgcb.scholar.transfer.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Compatibility shape conversion only. All policy is delegated to the new transfer pipeline. */
public final class LegacyFragmentClipboardAdapter {
    private LegacyFragmentClipboardAdapter() {}
    public static Optional<DocumentFragmentClipboardPayload> adapt(ScholarClipboardPayload payload) {
        if (payload instanceof DocumentFragmentClipboardPayload fragment) { return Optional.of(fragment); }
        DocumentFragment fragment;
        if (payload instanceof InlineContentClipboardPayload inline) {
            fragment = new DocumentFragment(new FragmentContent.InlineSegments(List.of(inline.content())), List.of());
        } else if (payload instanceof DatasetClipboardPayload dataset) {
            fragment = new DocumentFragment(new FragmentContent.ResourcePrimary(Set.of(
                    new StableIdentityKey(StableIdentityKind.DATASET, dataset.dataset().id()))), List.of(dataset.dataset()));
        } else {
            BlockNode block;
            if (payload instanceof DocumentBlockClipboardPayload root) { block = root.block(); }
            else if (payload instanceof TableClipboardPayload table) { block = table.table(); }
            else if (payload instanceof PlotClipboardPayload plot) { block = plot.plot(); }
            else if (payload instanceof DiagramClipboardPayload diagram) { block = diagram.diagram(); }
            else if (payload instanceof FigureClipboardPayload figure) { block = figure.figure(); }
            else { return Optional.empty(); }
            fragment = new DocumentFragment(new FragmentContent.Blocks(List.of(block)), List.of());
        }
        return Optional.of(new DocumentFragmentClipboardPayload(fragment, SourceTransferMetadata.unknown()));
    }
}
