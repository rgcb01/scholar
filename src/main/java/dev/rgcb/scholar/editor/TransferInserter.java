package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.transfer.*;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Pure staging in the editor layer; callers alone own history commit. */
public final class TransferInserter {
    private final DocumentEditor editor;
    public TransferInserter(DocumentEditor editor) { this.editor = java.util.Objects.requireNonNull(editor); }

    public boolean supports(EditorState state, FragmentContent content) {
        if (content instanceof FragmentContent.ResourcePrimary) {
            return state.isTextSelection() || state.isBlockSelection();
        }
        if (content instanceof FragmentContent.InlineSegments) { return state.isTextSelection(); }
        if (state.isBlockSelection() && content instanceof FragmentContent.Blocks blocks && blocks.roots().size() == 1) {
            var root = blocks.roots().get(0);
            if (root instanceof TableBlock || root instanceof PlotBlock || root instanceof DiagramBlock || root instanceof FigureBlock) {
                return matchingReplacement(state, content);
            }
        }
        return (state.isTextSelection() || state.isBlockSelection()) && editor.supportsInsertBlock(state);
    }

    public TransferContext contextFor(EditorState state, DocumentFragment fragment,
                                      RuntimeDocumentToken token, SourceTransferMetadata metadata) {
        var removed = new HashSet<Integer>();
        var scope = fragment.content() instanceof FragmentContent.ResourcePrimary ? TransferContext.Scope.RESOURCES
                : fragment.content() instanceof FragmentContent.InlineSegments ? TransferContext.Scope.INLINE : TransferContext.Scope.BLOCKS;
        if (matchingReplacement(state, fragment.content())) {
            removed.add(state.blockSelection().blockIndex());
        } else if (fragment.content() instanceof FragmentContent.Blocks && state.isTextSelection()) {
            // Stage only receiving shape; the marker has no identity or semantic dependencies.
            var shape = editor.insertBlocks(state, List.of(new EquationBlock(new MathSequence(List.of()))));
            var survivors = new HashSet<StableIdentityKey>();
            shape.document().blocks().forEach(block -> FragmentIdentityIndex.blockIdentity(block).ifPresent(survivors::add));
            for (var i = 0; i < state.document().blocks().size(); i++) {
                var key = FragmentIdentityIndex.blockIdentity(state.document().blocks().get(i));
                if (key.isPresent() && !survivors.contains(key.get())) { removed.add(i); }
            }
        } else if (fragment.content() instanceof FragmentContent.InlineSegments && state.isTextSelection()) {
            var range = state.selectionRange();
            for (var i = range.start().blockIndex() + 1; i <= range.end().blockIndex(); i++) { removed.add(i); }
        }
        return new TransferContext(state.document(), Optional.ofNullable(token), metadata, scope, removed);
    }

    public TransferInsertionResult stage(EditorState captured, EditorState current, MaterializedTransfer transfer) {
        if (captured != current || current.document() != transfer.plan().context().destination()) {
            return failure(TransferDiagnostic.Code.STALE_DESTINATION, "Destination state changed after planning.");
        }
        if (!supports(current, transfer.content())) {
            return failure(TransferDiagnostic.Code.UNSUPPORTED_DESTINATION, "Unsupported semantic receiving scope.");
        }
        try {
            // Recheck approved survivor facts against the exact selection being staged.
            var expected = contextFor(current, transfer.plan().fragment(),
                    transfer.plan().context().destinationToken().orElse(null),
                    transfer.plan().context().sourceMetadata());
            if (!expected.removedBlockIndices().equals(transfer.plan().context().removedBlockIndices())) {
                return failure(TransferDiagnostic.Code.INSERTION_VALIDATION_FAILURE, "Plan survivor facts disagree with receiving shape.");
            }
            EditResult staged;
            if (transfer.content() instanceof FragmentContent.Blocks blocks) {
                if (matchingReplacement(current, transfer.content())) {
                    var roots = new ArrayList<BlockNode>(current.document().blocks());
                    roots.set(current.blockSelection().blockIndex(), blocks.roots().get(0));
                    var document = new Document(roots, current.document().datasets(), current.document().settings());
                    staged = new EditResult(document, current.selection(), Optional.empty(), !document.equals(current.document()));
                } else { staged = editor.insertBlocks(current, blocks.roots()); }
            } else if (transfer.content() instanceof FragmentContent.InlineSegments inline) {
                staged = editor.insertInlineSegments(current, inline.segments());
            } else { staged = new EditResult(current.document(), current.selection(), current.explicitTypingMarks(), false); }
            var resources = new ArrayList<>(staged.document().datasets());
            resources.addAll(transfer.resourceAdditions());
            var candidate = new Document(staged.document().blocks(), resources, staged.document().settings());
            var diagnostics = new ArrayList<>(transfer.plan().diagnostics());
            var validation = DocumentValidator.validate(candidate);
            if (!validation.isValid() || !new EditorSelectionValidator().isValid(candidate, staged.selection())) {
                return failure(TransferDiagnostic.Code.INSERTION_VALIDATION_FAILURE, "Candidate document or selection failed validation: " + validation.diagnostics());
            }
            return new TransferInsertionResult.Success(new EditResult(candidate, staged.selection(), staged.explicitTypingMarks(),
                    !candidate.equals(current.document())), diagnostics);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(TransferDiagnostic.Code.INSERTION_VALIDATION_FAILURE, exception.getMessage());
        }
    }

    private static boolean matchingReplacement(EditorState state, FragmentContent content) {
        if (!state.isBlockSelection() || !(content instanceof FragmentContent.Blocks blocks) || blocks.roots().size() != 1) { return false; }
        var root = blocks.roots().get(0);
        return (root instanceof TableBlock || root instanceof PlotBlock || root instanceof DiagramBlock || root instanceof FigureBlock)
                && state.document().blocks().get(state.blockSelection().blockIndex()).getClass() == root.getClass();
    }

    private static TransferInsertionResult failure(TransferDiagnostic.Code code, String message) {
        return new TransferInsertionResult.Failure(List.of(new TransferDiagnostic(TransferDiagnostic.Severity.ERROR, code,
                message, Optional.empty(), Optional.empty())));
    }
}
