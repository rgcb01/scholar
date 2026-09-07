package dev.rgcb.scholar.document;

import java.util.List;

/**
 * Ordered inline content inside a block node.
 */
public record InlineContent(List<InlineNode> nodes) {
    public InlineContent {
        nodes = List.copyOf(nodes);
    }
}
