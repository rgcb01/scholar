package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.Text;

final class EditableInlineBlock {
    private EditableInlineBlock() {
    }

    static boolean supports(BlockNode block) {
        return (block instanceof Paragraph paragraph && textOnly(paragraph.content()))
                || (block instanceof Heading heading && textOnly(heading.content()));
    }

    static InlineContent contentOf(BlockNode block) {
        if (block instanceof Paragraph paragraph) {
            return paragraph.content();
        }
        if (block instanceof Heading heading) {
            return heading.content();
        }
        throw new IllegalArgumentException("Unsupported editable inline block: " + block.getClass().getName());
    }

    static BlockNode withContent(BlockNode block, InlineContent content) {
        if (block instanceof Paragraph paragraph) {
            return new Paragraph(content, paragraph.style(), paragraph.format());
        }
        if (block instanceof Heading heading) {
            return new Heading(heading.level(), content, heading.id());
        }
        throw new IllegalArgumentException("Unsupported editable inline block: " + block.getClass().getName());
    }

    static BlockStyle styleOf(BlockNode block) {
        if (block instanceof Paragraph) {
            return BlockStyle.paragraph();
        }
        if (block instanceof Heading heading) {
            return BlockStyle.heading(heading.level());
        }
        throw new IllegalArgumentException("Unsupported editable inline block: " + block.getClass().getName());
    }

    static BlockNode withStyle(BlockNode block, BlockStyle style) {
        var content = contentOf(block);
        return style.isParagraph()
                ? block instanceof Paragraph paragraph
                        ? new Paragraph(content, paragraph.style(), paragraph.format())
                        : new Paragraph(content)
                : new Heading(style.headingLevel(), content, block instanceof Heading heading ? heading.id() : java.util.Optional.empty());
    }

    private static boolean textOnly(InlineContent content) {
        return content.nodes().stream().allMatch(node -> node instanceof Text || node instanceof CrossReference
                || node instanceof dev.rgcb.scholar.document.QuantityInline);
    }
}
