package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceResolver;
import dev.rgcb.scholar.document.Text;
import java.util.Objects;
import java.util.Optional;

public final class PlainTextClipboard {
    private final DocumentEditor editor;

    public PlainTextClipboard(DocumentEditor editor) {
        this.editor = Objects.requireNonNull(editor, "editor");
    }

    public Optional<String> copy(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (!state.hasSelection()) {
            return Optional.empty();
        }
        return Optional.of(selectedText(state.document(), state.selectionRange()));
    }

    public Optional<ClipboardEditResult> cut(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (!state.hasSelection()) {
            return Optional.empty();
        }
        var range = state.selectionRange();
        var text = selectedText(state.document(), range);
        var result = editor.replaceRange(state.document(), range, "");
        return Optional.of(new ClipboardEditResult(text, result));
    }

    public Optional<EditResult> paste(EditorState state, String clipboardText) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(clipboardText, "clipboardText");
        if (state.isBlockSelection()) {
            return Optional.empty();
        }
        if (clipboardText.isEmpty()) {
            return Optional.empty();
        }
        var normalized = normalizeMultilineClipboardText(clipboardText);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(editor.insertText(state, normalized));
    }

    public String normalizeMultilineClipboardText(String clipboardText) {
        Objects.requireNonNull(clipboardText, "clipboardText");
        return clipboardText
                .replace("\r\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ');
    }

    private static String selectedText(Document document, DocumentRange range) {
        if (!range.isSingleBlock()) {
            return selectedMultiBlockText(document, range);
        }
        if (range.start().blockIndex() < 0 || range.start().blockIndex() >= document.blocks().size()) {
            throw new IllegalArgumentException("blockIndex is outside the document.");
        }
        var block = document.blocks().get(range.start().blockIndex());
        if (!EditableInlineBlock.supports(block)) {
            throw new IllegalArgumentException("Only Paragraph and Heading text blocks are supported by plain-text clipboard operations.");
        }

        validateInlineRange(EditableInlineBlock.contentOf(block), range.start().characterOffset(), range.end().characterOffset());
        return inlineText(document, EditableInlineBlock.contentOf(block), range.start().characterOffset(), range.end().characterOffset());
    }

    private static String selectedMultiBlockText(Document document, DocumentRange range) {
        validateContiguousEditableRange(document, range);
        var text = new StringBuilder();
        text.append(inlineText(
                document,
                EditableInlineBlock.contentOf(document.blocks().get(range.start().blockIndex())),
                range.start().characterOffset(),
                InlineContentEditor.characterCount(EditableInlineBlock.contentOf(document.blocks().get(range.start().blockIndex())))));

        for (var blockIndex = range.start().blockIndex() + 1; blockIndex < range.end().blockIndex(); blockIndex++) {
            text.append('\n');
            text.append(inlineText(document, EditableInlineBlock.contentOf(document.blocks().get(blockIndex)), 0, InlineContentEditor.characterCount(EditableInlineBlock.contentOf(document.blocks().get(blockIndex)))));
        }

        text.append('\n');
        text.append(inlineText(document, EditableInlineBlock.contentOf(document.blocks().get(range.end().blockIndex())), 0, range.end().characterOffset()));
        return text.toString();
    }

    private static void validateContiguousEditableRange(Document document, DocumentRange range) {
        if (range.start().blockIndex() < 0 || range.end().blockIndex() >= document.blocks().size()) {
            throw new IllegalArgumentException("range block index is outside the document.");
        }
        for (var blockIndex = range.start().blockIndex(); blockIndex <= range.end().blockIndex(); blockIndex++) {
            var block = document.blocks().get(blockIndex);
            if (!EditableInlineBlock.supports(block)) {
                throw new IllegalArgumentException("Plain-text copy cannot cross unsupported blocks.");
            }
            TextBoundary.validateRange(
                    logicalBlockText(document, blockIndex),
                    blockIndex == range.start().blockIndex() ? range.start().characterOffset() : 0,
                    blockIndex == range.end().blockIndex() ? range.end().characterOffset() : TextBoundary.characterCount(logicalBlockText(document, blockIndex)));
        }
    }

    private static String logicalBlockText(Document document, int blockIndex) {
        var block = document.blocks().get(blockIndex);
        if (!EditableInlineBlock.supports(block)) {
            throw new IllegalArgumentException("Only Paragraph and Heading text blocks are supported by plain-text clipboard operations.");
        }
        return InlineContentEditor.logicalText(EditableInlineBlock.contentOf(block));
    }

    private static void validateInlineRange(dev.rgcb.scholar.document.InlineContent content, int startOffset, int endOffset) {
        TextBoundary.validateRange(InlineContentEditor.logicalText(content), startOffset, endOffset);
    }

    private static String inlineText(Document document, dev.rgcb.scholar.document.InlineContent content, int startOffset, int endOffset) {
        var text = new StringBuilder();
        var logicalOffset = 0;
        var resolver = new CrossReferenceResolver();
        for (var node : content.nodes()) {
            var nodeLength = InlineContentEditor.characterCount(node);
            var nodeStart = logicalOffset;
            var nodeEnd = nodeStart + nodeLength;
            var selectedStart = Math.max(startOffset, nodeStart);
            var selectedEnd = Math.min(endOffset, nodeEnd);
            if (selectedStart < selectedEnd) {
                if (node instanceof Text run) {
                    text.append(TextBoundary.substring(run.content(), selectedStart - nodeStart, selectedEnd - nodeStart));
                } else if (node instanceof CrossReference reference) {
                    text.append(resolver.resolve(document, reference).displayText());
                } else {
                    throw new IllegalArgumentException("Unsupported inline node: " + node.getClass().getName());
                }
            }
            logicalOffset = nodeEnd;
        }
        return text.toString();
    }
}
