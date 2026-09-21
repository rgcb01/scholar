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
        InlineContent caption,
        ContentSpan span
) implements BlockNode {
    public FigureBlock(String id, BlockNode content, InlineContent caption) { this(id, content, caption, ContentSpan.COLUMN); }
    public FigureBlock {
        id = Objects.requireNonNull(id, "id").trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Figure id must not be blank.");
        }
        content = Objects.requireNonNull(content, "content");
        caption = Objects.requireNonNull(caption, "caption");
        span = Objects.requireNonNull(span, "span");
        if (!supportsContent(content)) {
            throw new IllegalArgumentException("Figure content must be a supported scientific visual block.");
        }
    }

    public static FigureBlock emptyCaption(String id, BlockNode content) {
        return new FigureBlock(id, content, new InlineContent(List.of()), ContentSpan.COLUMN);
    }

    public FigureBlock withContent(BlockNode replacement) {
        return new FigureBlock(id, replacement, caption, span);
    }

    public FigureBlock withCaption(InlineContent replacement) {
        return new FigureBlock(id, content, replacement, span);
    }

    public FigureBlock withId(String replacement) {
        return new FigureBlock(replacement, content, caption, span);
    }

    public FigureBlock withSpan(ContentSpan replacement) { return new FigureBlock(id, content, caption, replacement); }

    public static boolean supportsContent(BlockNode content) {
        return content instanceof DiagramBlock || content instanceof PlotBlock;
    }
}
