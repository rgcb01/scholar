package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.Document;
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

        var text = new StringBuilder();
        for (var node : EditableInlineBlock.contentOf(block).nodes()) {
            if (!(node instanceof Text textNode)) {
                throw new IllegalArgumentException("Only Text inline nodes are supported by plain-text clipboard operations.");
            }
            text.append(textNode.content());
        }
        TextBoundary.validateRange(text.toString(), range.start().characterOffset(), range.end().characterOffset());
        return TextBoundary.substring(text.toString(), range.start().characterOffset(), range.end().characterOffset());
    }

    private static String selectedMultiBlockText(Document document, DocumentRange range) {
        validateContiguousEditableRange(document, range);
        var text = new StringBuilder();
        text.append(TextBoundary.substring(
                blockText(document, range.start().blockIndex()),
                range.start().characterOffset(),
                TextBoundary.characterCount(blockText(document, range.start().blockIndex()))));

        for (var blockIndex = range.start().blockIndex() + 1; blockIndex < range.end().blockIndex(); blockIndex++) {
            text.append('\n');
            text.append(blockText(document, blockIndex));
        }

        text.append('\n');
        text.append(TextBoundary.substring(blockText(document, range.end().blockIndex()), 0, range.end().characterOffset()));
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
                    blockText(document, blockIndex),
                    blockIndex == range.start().blockIndex() ? range.start().characterOffset() : 0,
                    blockIndex == range.end().blockIndex() ? range.end().characterOffset() : TextBoundary.characterCount(blockText(document, blockIndex)));
        }
    }

    private static String blockText(Document document, int blockIndex) {
        var block = document.blocks().get(blockIndex);
        if (!EditableInlineBlock.supports(block)) {
            throw new IllegalArgumentException("Only Paragraph and Heading text blocks are supported by plain-text clipboard operations.");
        }

        var text = new StringBuilder();
        for (var node : EditableInlineBlock.contentOf(block).nodes()) {
            if (!(node instanceof Text textNode)) {
                throw new IllegalArgumentException("Only Text inline nodes are supported by plain-text clipboard operations.");
            }
            text.append(textNode.content());
        }
        return text.toString();
    }
}
