package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.transfer.ExtractionResult;
import dev.rgcb.scholar.transfer.FragmentExtractionRequest;
import dev.rgcb.scholar.transfer.FragmentExtractor;
import dev.rgcb.scholar.transfer.RuntimeDocumentToken;
import dev.rgcb.scholar.transfer.TransferDiagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Adapts document selections; nested clipboard domains stay specialized. */
public final class EditorFragmentExtractionAdapter {
    public FragmentExtractionRequest requestFor(Document source, EditorSelection selection) {
        if (!new EditorSelectionValidator().isValid(source, selection)) {
            return new FragmentExtractionRequest.Rejected(TransferDiagnostic.Code.MALFORMED_FRAGMENT, "Invalid editor selection.");
        }
        if (selection instanceof BlockSelection block) {
            return new FragmentExtractionRequest.Blocks(List.of(block.blockIndex()));
        }
        if (selection instanceof TextSelection text) {
            if (text.isCaret()) {
                return unsupported("A collapsed text caret has no content to copy.");
            }
            var range = text.range();
            var segments = new ArrayList<InlineContent>();
            for (var index = range.start().blockIndex(); index <= range.end().blockIndex(); index++) {
                var content = EditableInlineBlock.contentOf(source.blocks().get(index));
                var start = index == range.start().blockIndex() ? range.start().characterOffset() : 0;
                var end = index == range.end().blockIndex() ? range.end().characterOffset() : InlineContentEditor.characterCount(content);
                segments.add(InlineContentEditor.slice(content, start, end));
            }
            return new FragmentExtractionRequest.InlineSegments(segments);
        }
        return unsupported("Nested selection remains in its existing local clipboard domain: " + selection.getClass().getSimpleName());
    }

    public ExtractionResult extract(Document source, EditorSelection selection, Optional<RuntimeDocumentToken> token) {
        return new FragmentExtractor().extract(source, requestFor(source, selection), token);
    }

    private static FragmentExtractionRequest unsupported(String message) {
        return new FragmentExtractionRequest.Rejected(TransferDiagnostic.Code.UNSUPPORTED_SOURCE_SELECTION, message);
    }
}
