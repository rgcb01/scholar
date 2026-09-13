package dev.rgcb.scholar.document;

import java.util.List;
import java.util.Objects;

/**
 * Semantic scientific figure that wraps existing visual document content.
 *
 * <p>The stable id is authored semantic identity for future references. The
 * displayed figure number is derived from document order and is not stored here.</p>
 */
public record FigureBlock(
        String id,
        BlockNode content,
        InlineContent caption
) implements BlockNode {
    public FigureBlock {
        id = Objects.requireNonNull(id, "id").trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Figure id must not be blank.");
        }
        content = Objects.requireNonNull(content, "content");
        caption = Objects.requireNonNull(caption, "caption");
        if (!supportsContent(content)) {
            throw new IllegalArgumentException("Figure content must be a supported scientific visual block.");
        }
    }

    public static FigureBlock emptyCaption(String id, BlockNode content) {
        return new FigureBlock(id, content, new InlineContent(List.of()));
    }

    public FigureBlock withContent(BlockNode replacement) {
        return new FigureBlock(id, replacement, caption);
    }

    public FigureBlock withCaption(InlineContent replacement) {
        return new FigureBlock(id, content, replacement);
    }

    public FigureBlock withId(String replacement) {
        return new FigureBlock(replacement, content, caption);
    }

    public static boolean supportsContent(BlockNode content) {
        return content instanceof DiagramBlock || content instanceof PlotBlock;
    }
}
