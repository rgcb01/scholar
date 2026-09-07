package dev.rgcb.scholar.document;

import java.util.List;

/**
 * The semantic source of truth for a Scholar document.
 */
public record Document(List<BlockNode> blocks) {
    public Document {
        blocks = List.copyOf(blocks);
    }
}
