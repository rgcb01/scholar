package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class DocumentEditor {
    public EditorState initialState(Document document, int blockIndex) {
        Objects.requireNonNull(document, "document");
        var text = blockText(editableInlineBlock(document, blockIndex));
        return new EditorState(document, new DocumentPosition(blockIndex, TextBoundary.characterCount(text)));
    }

    public EditResult insertText(EditorState state, String text) {
        Objects.requireNonNull(state, "state");
        return insertText(state, text, marksForInsertion(state));
    }

    public EditResult insertText(EditorState state, String text, Set<TextMark> marks) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(marks, "marks");
        if (state.isBlockSelection() || state.isEquationEditingSelection()) {
            return new EditResult(state.document(), state.selection(), Optional.empty(), false);
        }
        if (text.isEmpty()) {
            return new EditResult(state.document(), state.anchor(), state.active(), state.explicitTypingMarks(), false);
        }
        var result = replaceRange(
                state.document(),
                state.hasSelection() ? state.selectionRange() : DocumentRange.caret(state.caret()),
                text,
                marksForReplacement(state, marks));
        return new EditResult(result.document(), result.caret(), state.explicitTypingMarks(), result.changed());
    }

    public EditResult insertCrossReference(EditorState state, CrossReference reference) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(reference, "reference");
        if (state.isBlockSelection() || state.isEquationEditingSelection()) {
            return new EditResult(state.document(), state.selection(), Optional.empty(), false);
        }
        var range = state.hasSelection() ? state.selectionRange() : DocumentRange.caret(state.caret());
        validateEditableRange(state.document(), range);
        if (!range.isSingleBlock()) {
            return new EditResult(state.document(), state.selection(), state.explicitTypingMarks(), false);
        }
        var blockIndex = range.start().blockIndex();
        var block = editableInlineBlock(state.document(), blockIndex);
        var updatedInlineNodes = replaceBlockRange(block, range, reference);
        var updatedBlocks = new ArrayList<BlockNode>(state.document().blocks());
        updatedBlocks.set(blockIndex, EditableInlineBlock.withContent(block, new InlineContent(updatedInlineNodes)));
        var updatedDocument = withBlocks(state.document(), updatedBlocks);
        return new EditResult(
                updatedDocument,
                new DocumentPosition(blockIndex, range.start().characterOffset() + 1),
                Optional.empty(),
                !updatedDocument.equals(state.document()));
    }

    public EditResult insertInlineContent(EditorState state, InlineContent replacementContent) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(replacementContent, "replacementContent");
        if (state.isBlockSelection() || state.isEquationEditingSelection()) {
            return new EditResult(state.document(), state.selection(), Optional.empty(), false);
        }
        var range = state.hasSelection() ? state.selectionRange() : DocumentRange.caret(state.caret());
        validateEditableRange(state.document(), range);
        if (!range.isSingleBlock()) {
            return new EditResult(state.document(), state.selection(), state.explicitTypingMarks(), false);
        }
        var blockIndex = range.start().blockIndex();
        var block = editableInlineBlock(state.document(), blockIndex);
        var before = InlineContentEditor.split(EditableInlineBlock.contentOf(block), range.start().characterOffset()).left();
        var after = InlineContentEditor.split(EditableInlineBlock.contentOf(block), range.end().characterOffset()).right();
        var updatedContent = InlineContentEditor.concat(InlineContentEditor.concat(before, replacementContent), after);
        var updatedBlocks = new ArrayList<BlockNode>(state.document().blocks());
        updatedBlocks.set(blockIndex, EditableInlineBlock.withContent(block, updatedContent));
        var updatedDocument = withBlocks(state.document(), updatedBlocks);
        return new EditResult(
                updatedDocument,
                new DocumentPosition(blockIndex, range.start().characterOffset() + InlineContentEditor.characterCount(replacementContent)),
                Optional.empty(),
                !updatedDocument.equals(state.document()));
    }

    public EditResult moveLeft(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlockSelection()) {
            return moveLeftFromBlockSelection(state);
        }
        if (state.hasSelection()) {
            return new EditResult(state.document(), state.selectionRange().start(), false);
        }
        var blockText = blockText(editableInlineBlock(state.document(), state.caret().blockIndex()));
        if (state.caret().characterOffset() == 0) {
            return moveLeftFromTextBoundary(state);
        }
        var nextOffset = TextBoundary.previousOffset(blockText, state.caret().characterOffset());
        return new EditResult(state.document(), new DocumentPosition(state.caret().blockIndex(), nextOffset), false);
    }

    public EditResult moveRight(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlockSelection()) {
            return moveRightFromBlockSelection(state);
        }
        if (state.hasSelection()) {
            return new EditResult(state.document(), state.selectionRange().end(), false);
        }
        var blockText = blockText(editableInlineBlock(state.document(), state.caret().blockIndex()));
        if (state.caret().characterOffset() == TextBoundary.characterCount(blockText)) {
            return moveRightFromTextBoundary(state);
        }
        var nextOffset = TextBoundary.nextOffset(blockText, state.caret().characterOffset());
        return new EditResult(state.document(), new DocumentPosition(state.caret().blockIndex(), nextOffset), false);
    }

    public EditorState extendLeft(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlockSelection()) {
            return state;
        }
        return state.withActive(previousSelectionPosition(state.document(), state.active()));
    }

    public EditorState extendRight(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlockSelection()) {
            return state;
        }
        return state.withActive(nextSelectionPosition(state.document(), state.active()));
    }

    public EditResult deleteBackward(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlockSelection()) {
            return deleteSelectedBlock(state);
        }
        if (state.hasSelection()) {
            return replaceRange(state.document(), state.selectionRange(), "");
        }
        var paragraphText = blockText(editableInlineBlock(state.document(), state.caret().blockIndex()));
        var caretOffset = state.caret().characterOffset();
        if (caretOffset == 0) {
            var selectedObject = selectPreviousObject(state);
            if (!selectedObject.editorState().equals(state)) {
                return selectedObject;
            }
            return joinWithPreviousBlock(state);
        }
        var start = TextBoundary.previousOffset(paragraphText, caretOffset);
        return replaceRange(
                state.document(),
                new DocumentRange(
                        new DocumentPosition(state.caret().blockIndex(), start),
                        state.caret()),
                "");
    }

    public EditResult deleteForward(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlockSelection()) {
            return deleteSelectedBlock(state);
        }
        if (state.hasSelection()) {
            return replaceRange(state.document(), state.selectionRange(), "");
        }
        var paragraphText = blockText(editableInlineBlock(state.document(), state.caret().blockIndex()));
        var caretOffset = state.caret().characterOffset();
        if (caretOffset == TextBoundary.characterCount(paragraphText)) {
            var selectedObject = selectNextObject(state);
            if (!selectedObject.editorState().equals(state)) {
                return selectedObject;
            }
            return joinWithNextBlock(state);
        }
        var end = TextBoundary.nextOffset(paragraphText, caretOffset);
        return replaceRange(
                state.document(),
                new DocumentRange(
                        state.caret(),
                        new DocumentPosition(state.caret().blockIndex(), end)),
                "");
    }

    public EditResult insertParagraphBreak(EditorState state, Set<TextMark> carriedTypingMarks) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(carriedTypingMarks, "carriedTypingMarks");
        if (state.isBlockSelection()) {
            return new EditResult(state.document(), state.selection(), Optional.empty(), false);
        }
        var splitState = state;
        if (state.hasSelection()) {
            var deletion = replaceRange(state.document(), state.selectionRange(), "", marksForInsertion(state.document(), state.selectionRange().start()));
            splitState = new EditorState(deletion.document(), deletion.caret(), deletion.caret(), state.explicitTypingMarks());
        }

        var blockIndex = splitState.caret().blockIndex();
        var block = editableInlineBlock(splitState.document(), blockIndex);
        var split = InlineContentEditor.split(EditableInlineBlock.contentOf(block), splitState.caret().characterOffset());
        var blockLength = InlineContentEditor.characterCount(EditableInlineBlock.contentOf(block));
        var updatedBlocks = new ArrayList<BlockNode>(splitState.document().blocks());
        var typingMarks = Optional.of(Set.copyOf(carriedTypingMarks));

        if (block instanceof Heading heading && blockLength == 0) {
            updatedBlocks.set(blockIndex, new Paragraph(new InlineContent(List.of())));
            return new EditResult(withBlocks(splitState.document(), updatedBlocks), new DocumentPosition(blockIndex, 0), typingMarks, true);
        }
        if (block instanceof Heading heading && splitState.caret().characterOffset() == 0) {
            updatedBlocks.add(blockIndex, new Paragraph(new InlineContent(List.of())));
            return new EditResult(withBlocks(splitState.document(), updatedBlocks), new DocumentPosition(blockIndex, 0), typingMarks, true);
        }
        if (block instanceof Heading heading && splitState.caret().characterOffset() == blockLength) {
            updatedBlocks.set(blockIndex, new Heading(heading.level(), split.left(), heading.id()));
            updatedBlocks.add(blockIndex + 1, new Paragraph(split.right()));
            return new EditResult(withBlocks(splitState.document(), updatedBlocks), new DocumentPosition(blockIndex + 1, 0), typingMarks, true);
        }

        updatedBlocks.set(blockIndex, EditableInlineBlock.withContent(block, split.left()));
        var rightBlock = block instanceof Heading heading
                ? new Heading(heading.level(), split.right())
                : EditableInlineBlock.withContent(block, split.right());
        updatedBlocks.add(blockIndex + 1, rightBlock);
        return new EditResult(withBlocks(splitState.document(), updatedBlocks), new DocumentPosition(blockIndex + 1, 0), typingMarks, true);
    }

    public EditResult replaceRange(Document document, DocumentRange range, String replacement) {
        return replaceRange(document, range, replacement, marksForInsertion(document, range.start()));
    }

    public EditResult replaceRange(Document document, DocumentRange range, String replacement, Set<TextMark> replacementMarks) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(replacement, "replacement");
        Objects.requireNonNull(replacementMarks, "replacementMarks");
        validateEditableRange(document, range);
        if (!range.isSingleBlock()) {
            return replaceMultiBlockRange(document, range, replacement, Set.copyOf(replacementMarks));
        }
        var blockIndex = range.start().blockIndex();
        var block = editableInlineBlock(document, blockIndex);
        var text = blockText(block);
        TextBoundary.validateRange(text, range.start().characterOffset(), range.end().characterOffset());

        var updatedInlineNodes = replaceBlockRange(block, range, replacement, Set.copyOf(replacementMarks));
        var updatedBlocks = new ArrayList<BlockNode>(document.blocks());
        updatedBlocks.set(blockIndex, EditableInlineBlock.withContent(block, new InlineContent(updatedInlineNodes)));
        var updatedDocument = withBlocks(document, updatedBlocks);
        var updatedCaret = new DocumentPosition(
                blockIndex,
                range.start().characterOffset() + TextBoundary.characterCount(replacement));
        return new EditResult(updatedDocument, updatedCaret, !updatedDocument.equals(document));
    }

    public EditResult toggleMark(EditorState state, TextMark mark) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(mark, "mark");
        if (state.isBlockSelection()) {
            return new EditResult(state.document(), state.selection(), Optional.empty(), false);
        }
        if (!state.hasSelection()) {
            return new EditResult(state.document(), state.anchor(), state.active(), state.explicitTypingMarks(), false);
        }
        var formattingState = formattingState(state, mark);
        if (formattingState == FormattingState.NOT_APPLICABLE) {
            return new EditResult(state.document(), state.anchor(), state.active(), state.explicitTypingMarks(), false);
        }
        return formattingState == FormattingState.ON
                ? removeMark(state, mark)
                : applyMark(state, mark);
    }

    public EditResult applyMark(EditorState state, TextMark mark) {
        return transformMark(state, mark, true);
    }

    public EditResult removeMark(EditorState state, TextMark mark) {
        return transformMark(state, mark, false);
    }

    public FormattingState formattingState(EditorState state, TextMark mark) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(mark, "mark");
        if (state.isBlockSelection() || state.isEquationEditingSelection()) {
            return FormattingState.NOT_APPLICABLE;
        }
        if (!state.hasSelection()) {
            return marksForInsertion(state).contains(mark) ? FormattingState.ON : FormattingState.OFF;
        }

        var range = state.selectionRange();
        var marked = 0;
        var unmarked = 0;
        validateEditableRange(state.document(), range);
        for (var blockIndex = range.start().blockIndex(); blockIndex <= range.end().blockIndex(); blockIndex++) {
            var block = editableInlineBlock(state.document(), blockIndex);
            var localRange = selectedLocalRange(block, range, blockIndex);
            var logicalOffset = 0;
            for (var node : EditableInlineBlock.contentOf(block).nodes()) {
                var nodeLength = InlineContentEditor.characterCount(node);
                var selectedStart = Math.max(localRange.start().characterOffset(), logicalOffset);
                var selectedEnd = Math.min(localRange.end().characterOffset(), logicalOffset + nodeLength);
                if (node instanceof Text text && selectedStart < selectedEnd) {
                    var selectedLength = selectedEnd - selectedStart;
                    if (text.marks().contains(mark)) {
                        marked += selectedLength;
                    } else {
                        unmarked += selectedLength;
                    }
                }
                logicalOffset += nodeLength;
            }
        }

        if (marked > 0 && unmarked > 0) {
            return FormattingState.MIXED;
        }
        if (marked == 0 && unmarked == 0) {
            return FormattingState.NOT_APPLICABLE;
        }
        return marked > 0 ? FormattingState.ON : FormattingState.OFF;
    }

    public Set<TextMark> marksForInsertion(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlockSelection() || state.isEquationEditingSelection()) {
            return Set.of();
        }
        return state.explicitTypingMarks()
                .map(Set::copyOf)
                .orElseGet(() -> marksForInsertion(state.document(), state.caret()));
    }

    public Set<TextMark> marksForReplacement(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlockSelection() || state.isEquationEditingSelection()) {
            return Set.of();
        }
        if (state.hasSelection()) {
            return marksForInsertion(state.document(), state.selectionRange().start());
        }
        return marksForInsertion(state);
    }

    public boolean supportsInlineFormatting(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlockSelection() || state.isEquationEditingSelection()) {
            return false;
        }
        if (state.hasSelection()) {
            try {
                return formattingState(state, TextMark.BOLD) != FormattingState.NOT_APPLICABLE;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        return isEditableInlineBlock(state.document(), state.caret().blockIndex());
    }

    public boolean supportsBlockStyle(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlockSelection() || state.isEquationEditingSelection()) {
            return false;
        }
        try {
            if (state.hasSelection()) {
                validateEditableRange(state.document(), state.selectionRange());
                return true;
            }
            return isEditableInlineBlock(state.document(), state.caret().blockIndex());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public BlockStyle blockStyle(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlockSelection() || state.isEquationEditingSelection()) {
            throw new IllegalStateException("Block selections do not have a block style.");
        }
        return EditableInlineBlock.styleOf(editableInlineBlock(state.document(), state.caret().blockIndex()));
    }

    public BlockStyleSelectionState blockStyleSelectionState(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlockSelection() || state.isEquationEditingSelection()) {
            return BlockStyleSelectionState.notApplicable();
        }
        if (!state.hasSelection() && !isEditableInlineBlock(state.document(), state.caret().blockIndex())) {
            return BlockStyleSelectionState.notApplicable();
        }
        var range = state.hasSelection() ? state.selectionRange() : DocumentRange.caret(state.caret());
        validateEditableRange(state.document(), range);
        BlockStyle first = null;
        for (var blockIndex = range.start().blockIndex(); blockIndex <= range.end().blockIndex(); blockIndex++) {
            var style = EditableInlineBlock.styleOf(editableInlineBlock(state.document(), blockIndex));
            if (first == null) {
                first = style;
            } else if (!first.equals(style)) {
                return BlockStyleSelectionState.mixed();
            }
        }
        return first == null ? BlockStyleSelectionState.notApplicable() : BlockStyleSelectionState.single(first);
    }

    public EditResult setBlockStyle(EditorState state, BlockStyle style) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(style, "style");
        if (state.isBlockSelection() || state.isEquationEditingSelection()) {
            return new EditResult(state.document(), state.selection(), Optional.empty(), false);
        }
        var range = state.hasSelection() ? state.selectionRange() : DocumentRange.caret(state.caret());
        validateEditableRange(state.document(), range);
        var updatedBlocks = new ArrayList<BlockNode>(state.document().blocks());
        for (var blockIndex = range.start().blockIndex(); blockIndex <= range.end().blockIndex(); blockIndex++) {
            var block = editableInlineBlock(state.document(), blockIndex);
            updatedBlocks.set(blockIndex, EditableInlineBlock.withStyle(block, style));
        }
        var updatedDocument = withBlocks(state.document(), updatedBlocks);
        if (updatedDocument.equals(state.document())) {
            return new EditResult(state.document(), state.anchor(), state.active(), state.explicitTypingMarks(), false);
        }
        return new EditResult(updatedDocument, state.anchor(), state.active(), state.explicitTypingMarks(), true);
    }

    public boolean supportsInsertBlock(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (state.isEquationEditingSelection()) {
            return false;
        }
        if (state.isBlockSelection() || !state.hasSelection()) {
            return true;
        }
        try {
            validateEditableRange(state.document(), state.selectionRange());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public EditResult insertEmptyEquation(EditorState state) {
        return insertBlock(state, new EquationBlock(new MathSequence(List.of())));
    }

    public EditResult insertDefaultTable(EditorState state) {
        return insertBlock(state, TableBlock.empty(2, 2));
    }

    public EditResult insertTableOfContents(EditorState state) {
        return insertBlock(state, new TableOfContentsBlock());
    }

    public EditResult insertDefaultPlot(EditorState state) {
        var plot = new PlotBlock(PlotDefinition.of(
                "Sample Plot",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries(
                        "Series A",
                        PlotSeriesKind.LINE,
                        List.of(
                                new DataPoint(0, 0),
                                new DataPoint(1, 1),
                                new DataPoint(2, 4),
                                new DataPoint(3, 9))))));
        return insertBlock(state, plot);
    }

    public EditResult insertDefaultDiagram(EditorState state) {
        var leftId = new DiagramElementId("node-a");
        var rightId = new DiagramElementId("node-b");
        var outId = new DiagramPortId("out");
        var inId = new DiagramPortId("in");
        var diagram = new DiagramBlock(new DiagramDefinition(
                "System Diagram",
                new DiagramCanvas(100, 50),
                List.of(
                        new DiagramNode(
                                leftId,
                                new DiagramBounds(8, 14, 28, 20),
                                "Node A",
                                List.of(new DiagramPort(outId, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)))),
                        new DiagramNode(
                                rightId,
                                new DiagramBounds(64, 14, 28, 20),
                                "Node B",
                                List.of(new DiagramPort(inId, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5))))),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(leftId, outId),
                        new DiagramEndpoint(rightId, inId),
                        ""))));
        return insertBlock(state, diagram);
    }

    public EditResult insertBlock(EditorState state, BlockNode block) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(block, "block");
        if (!supportsInsertBlock(state)) {
            return new EditResult(state.document(), state.selection(), state.explicitTypingMarks(), false);
        }
        if (state.isBlockSelection()) {
            return insertBlockAtPoint(state.document(), new DocumentInsertionPoint(state.blockSelection().blockIndex() + 1), block);
        }
        return state.hasSelection()
                ? replaceSelectionWithBlock(state, block)
                : insertBlockAtTextCaret(state, block);
    }

    public EditResult replaceSelectionWithBlock(EditorState state, BlockNode block) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(block, "block");
        if (state.isBlockSelection() || !state.hasSelection()) {
            return new EditResult(state.document(), state.selection(), state.explicitTypingMarks(), false);
        }

        var range = state.selectionRange();
        validateEditableRange(state.document(), range);
        if (isBoundaryOnlyRange(state.document(), range)) {
            return insertBlockAtPoint(state.document(), new DocumentInsertionPoint(range.end().blockIndex()), block);
        }

        var updatedBlocks = new ArrayList<BlockNode>();
        for (var index = 0; index < range.start().blockIndex(); index++) {
            updatedBlocks.add(state.document().blocks().get(index));
        }

        var startBlock = editableInlineBlock(state.document(), range.start().blockIndex());
        var leftPrefix = InlineContentEditor.split(
                EditableInlineBlock.contentOf(startBlock),
                range.start().characterOffset()).left();
        addLeftPrefixBlock(updatedBlocks, startBlock, leftPrefix);

        var insertedBlockIndex = updatedBlocks.size();
        updatedBlocks.add(block);

        var endBlock = editableInlineBlock(state.document(), range.end().blockIndex());
        var rightSuffix = InlineContentEditor.split(
                EditableInlineBlock.contentOf(endBlock),
                range.end().characterOffset()).right();
        addRightSuffixBlock(updatedBlocks, rightSuffix);

        for (var index = range.end().blockIndex() + 1; index < state.document().blocks().size(); index++) {
            updatedBlocks.add(state.document().blocks().get(index));
        }

        appendAuthoringFallbackIfNeeded(updatedBlocks, insertedBlockIndex);
        var updatedDocument = withBlocks(state.document(), updatedBlocks);
        return new EditResult(
                updatedDocument,
                new BlockSelection(insertedBlockIndex),
                Optional.empty(),
                !updatedDocument.equals(state.document()));
    }

    public EditResult insertBlock(Document document, DocumentInsertionPoint point, BlockNode block) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(block, "block");
        return insertBlockAtPoint(document, point, block);
    }

    public EditResult deleteSelectedBlock(EditorState state) {
        Objects.requireNonNull(state, "state");
        if (!state.isBlockSelection()) {
            return new EditResult(state.document(), state.selection(), state.explicitTypingMarks(), false);
        }
        var blockIndex = state.blockSelection().blockIndex();
        if (blockIndex < 0 || blockIndex >= state.document().blocks().size()) {
            throw new IllegalArgumentException("block selection index is outside the document.");
        }
        var updatedBlocks = new ArrayList<BlockNode>(state.document().blocks());
        updatedBlocks.remove(blockIndex);
        if (updatedBlocks.isEmpty()) {
            updatedBlocks.add(new Paragraph(new InlineContent(List.of())));
            return new EditResult(withBlocks(state.document(), updatedBlocks), new DocumentPosition(0, 0), true);
        }
        var updatedDocument = withBlocks(state.document(), updatedBlocks);
        var nextSelection = selectionAfterBlockDeletion(updatedDocument, blockIndex);
        return new EditResult(updatedDocument, nextSelection, Optional.empty(), true);
    }

    private static List<InlineNode> replaceBlockRange(
            BlockNode block,
            DocumentRange range,
            String replacement,
            Set<TextMark> replacementMarks
    ) {
        var nodes = new ArrayList<InlineNode>();
        var logicalOffset = 0;
        var inserted = false;

        for (var node : EditableInlineBlock.contentOf(block).nodes()) {
            var nodeLength = InlineContentEditor.characterCount(node);
            var nodeStart = logicalOffset;
            var nodeEnd = nodeStart + nodeLength;
            if (nodeEnd <= range.start().characterOffset() || nodeStart >= range.end().characterOffset()) {
                nodes.add(node);
            } else if (node instanceof Text text) {
                var keepBeforeEnd = Math.max(0, range.start().characterOffset() - nodeStart);
                var keepAfterStart = Math.min(TextBoundary.characterCount(text.content()), range.end().characterOffset() - nodeStart);
                if (keepBeforeEnd > 0) {
                    nodes.add(new Text(TextBoundary.substring(text.content(), 0, keepBeforeEnd), text.marks()));
                }
                if (!inserted) {
                    if (!replacement.isEmpty()) {
                        nodes.add(new Text(replacement, replacementMarks));
                    }
                    inserted = true;
                }
                if (keepAfterStart < TextBoundary.characterCount(text.content())) {
                    nodes.add(new Text(TextBoundary.substring(text.content(), keepAfterStart, TextBoundary.characterCount(text.content())), text.marks()));
                }
            } else if (!inserted) {
                if (!replacement.isEmpty()) {
                    nodes.add(new Text(replacement, replacementMarks));
                }
                inserted = true;
            }
            logicalOffset = nodeEnd;
        }

        if (!inserted) {
            var insertionIndex = insertionIndex(block, range.start().characterOffset());
            if (!replacement.isEmpty()) {
                nodes.add(insertionIndex, new Text(replacement, replacementMarks));
            }
        }
        return nodes;
    }

    private static List<InlineNode> replaceBlockRange(BlockNode block, DocumentRange range, InlineNode replacement) {
        var nodes = new ArrayList<InlineNode>();
        var logicalOffset = 0;
        var inserted = false;

        for (var node : EditableInlineBlock.contentOf(block).nodes()) {
            var nodeLength = InlineContentEditor.characterCount(node);
            var nodeStart = logicalOffset;
            var nodeEnd = nodeStart + nodeLength;
            if (nodeEnd <= range.start().characterOffset() || nodeStart >= range.end().characterOffset()) {
                nodes.add(node);
            } else if (node instanceof Text text) {
                var keepBeforeEnd = Math.max(0, range.start().characterOffset() - nodeStart);
                var keepAfterStart = Math.min(TextBoundary.characterCount(text.content()), range.end().characterOffset() - nodeStart);
                if (keepBeforeEnd > 0) {
                    nodes.add(new Text(TextBoundary.substring(text.content(), 0, keepBeforeEnd), text.marks()));
                }
                if (!inserted) {
                    nodes.add(replacement);
                    inserted = true;
                }
                if (keepAfterStart < TextBoundary.characterCount(text.content())) {
                    nodes.add(new Text(TextBoundary.substring(text.content(), keepAfterStart, TextBoundary.characterCount(text.content())), text.marks()));
                }
            } else if (!inserted) {
                nodes.add(replacement);
                inserted = true;
            }
            logicalOffset = nodeEnd;
        }

        if (!inserted) {
            nodes.add(insertionIndex(block, range.start().characterOffset()), replacement);
        }
        return nodes;
    }

    private static int insertionIndex(BlockNode block, int characterOffset) {
        var logicalOffset = 0;
        var nodes = EditableInlineBlock.contentOf(block).nodes();
        for (var index = 0; index < nodes.size(); index++) {
            var node = nodes.get(index);
            if (logicalOffset >= characterOffset) {
                return index;
            }
            logicalOffset += InlineContentEditor.characterCount(node);
            if (logicalOffset >= characterOffset) {
                return index + 1;
            }
        }
        return nodes.size();
    }

    private static EditResult replaceMultiBlockRange(
            Document document,
            DocumentRange range,
            String replacement,
            Set<TextMark> replacementMarks
    ) {
        var leftBlock = editableInlineBlock(document, range.start().blockIndex());
        var rightBlock = editableInlineBlock(document, range.end().blockIndex());
        var leftSplit = InlineContentEditor.split(EditableInlineBlock.contentOf(leftBlock), range.start().characterOffset());
        var rightSplit = InlineContentEditor.split(EditableInlineBlock.contentOf(rightBlock), range.end().characterOffset());
        var replacementContent = replacement.isEmpty()
                ? new InlineContent(List.of())
                : new InlineContent(List.of(new Text(replacement, replacementMarks)));
        var rebuiltContent = InlineContentEditor.concat(
                InlineContentEditor.concat(leftSplit.left(), replacementContent),
                rightSplit.right());

        var updatedBlocks = new ArrayList<BlockNode>(document.blocks());
        updatedBlocks.set(range.start().blockIndex(), EditableInlineBlock.withContent(leftBlock, rebuiltContent));
        for (var index = range.end().blockIndex(); index > range.start().blockIndex(); index--) {
            updatedBlocks.remove(index);
        }

        var updatedDocument = withBlocks(document, updatedBlocks);
        var updatedCaret = new DocumentPosition(
                range.start().blockIndex(),
                range.start().characterOffset() + TextBoundary.characterCount(replacement));
        return new EditResult(updatedDocument, updatedCaret, !updatedDocument.equals(document));
    }

    private static EditResult insertBlockAtTextCaret(EditorState state, BlockNode insertedBlock) {
        var blockIndex = state.caret().blockIndex();
        var block = editableInlineBlock(state.document(), blockIndex);
        var content = EditableInlineBlock.contentOf(block);
        var blockLength = InlineContentEditor.characterCount(content);
        var caretOffset = state.caret().characterOffset();
        TextBoundary.validateRange(blockText(block), caretOffset, caretOffset);

        if (blockLength == 0) {
            var blocks = new ArrayList<BlockNode>(state.document().blocks());
            blocks.set(blockIndex, insertedBlock);
            appendAuthoringFallbackIfNeeded(blocks, blockIndex);
            return new EditResult(withBlocks(state.document(), blocks), new BlockSelection(blockIndex), Optional.empty(), true);
        }

        if (caretOffset == 0) {
            return insertBlockAtPoint(state.document(), new DocumentInsertionPoint(blockIndex), insertedBlock);
        }

        if (caretOffset == blockLength) {
            return insertBlockAtPoint(state.document(), new DocumentInsertionPoint(blockIndex + 1), insertedBlock);
        }

        var split = InlineContentEditor.split(content, caretOffset);
        var blocks = new ArrayList<BlockNode>(state.document().blocks());
        var leftBlock = EditableInlineBlock.withContent(block, split.left());
        var rightBlock = block instanceof Heading
                ? new Paragraph(split.right())
                : EditableInlineBlock.withContent(block, split.right());
        blocks.set(blockIndex, leftBlock);
        blocks.add(blockIndex + 1, insertedBlock);
        blocks.add(blockIndex + 2, rightBlock);
        return new EditResult(withBlocks(state.document(), blocks), new BlockSelection(blockIndex + 1), Optional.empty(), true);
    }

    private static EditResult insertBlockAtPoint(Document document, DocumentInsertionPoint point, BlockNode insertedBlock) {
        validateInsertionPoint(document, point);
        var blocks = new ArrayList<BlockNode>(document.blocks());
        blocks.add(point.blockIndex(), insertedBlock);
        appendAuthoringFallbackIfNeeded(blocks, point.blockIndex());
        return new EditResult(withBlocks(document, blocks), new BlockSelection(point.blockIndex()), Optional.empty(), true);
    }

    private static boolean isBoundaryOnlyRange(Document document, DocumentRange range) {
        if (range.isSingleBlock() || range.start().blockIndex() + 1 != range.end().blockIndex()) {
            return false;
        }
        var startBlock = editableInlineBlock(document, range.start().blockIndex());
        return range.start().characterOffset() == blockLength(startBlock)
                && range.end().characterOffset() == 0;
    }

    private static void addLeftPrefixBlock(List<BlockNode> blocks, BlockNode sourceBlock, InlineContent prefix) {
        if (InlineContentEditor.characterCount(prefix) > 0) {
            blocks.add(EditableInlineBlock.withContent(sourceBlock, prefix));
        }
    }

    private static void addRightSuffixBlock(List<BlockNode> blocks, InlineContent suffix) {
        if (InlineContentEditor.characterCount(suffix) > 0) {
            blocks.add(new Paragraph(suffix));
        }
    }

    private static void validateInsertionPoint(Document document, DocumentInsertionPoint point) {
        if (point.blockIndex() > document.blocks().size()) {
            throw new IllegalArgumentException("insertion point is outside the document.");
        }
    }

    private static void appendAuthoringFallbackIfNeeded(List<BlockNode> blocks, int insertedBlockIndex) {
        if (insertedBlockIndex == blocks.size() - 1) {
            blocks.add(new Paragraph(new InlineContent(List.of())));
        }
    }

    private static EditorSelection selectionAfterBlockDeletion(Document document, int deletedBlockIndex) {
        var previous = deletedBlockIndex - 1;
        if (isEditableInlineBlock(document, previous)) {
            return new TextSelection(new DocumentPosition(previous, blockLength(document.blocks().get(previous))), new DocumentPosition(previous, blockLength(document.blocks().get(previous))));
        }
        if (previous >= 0) {
            return new BlockSelection(previous);
        }

        var sameIndexNext = deletedBlockIndex;
        if (isEditableInlineBlock(document, sameIndexNext)) {
            return new TextSelection(new DocumentPosition(sameIndexNext, 0), new DocumentPosition(sameIndexNext, 0));
        }
        if (sameIndexNext < document.blocks().size()) {
            return new BlockSelection(sameIndexNext);
        }

        throw new IllegalStateException("Unable to choose a valid selection after block deletion.");
    }

    private static int blockLength(BlockNode block) {
        return InlineContentEditor.characterCount(EditableInlineBlock.contentOf(block));
    }

    private static void validateEditableRange(Document document, DocumentRange range) {
        if (range.start().blockIndex() < 0 || range.end().blockIndex() >= document.blocks().size()) {
            throw new IllegalArgumentException("range block index is outside the document.");
        }
        for (var blockIndex = range.start().blockIndex(); blockIndex <= range.end().blockIndex(); blockIndex++) {
            var block = editableInlineBlock(document, blockIndex);
            var text = blockText(block);
            var startOffset = blockIndex == range.start().blockIndex() ? range.start().characterOffset() : 0;
            var endOffset = blockIndex == range.end().blockIndex() ? range.end().characterOffset() : TextBoundary.characterCount(text);
            TextBoundary.validateRange(text, startOffset, endOffset);
        }
    }

    private static DocumentRange selectedLocalRange(BlockNode block, DocumentRange range, int blockIndex) {
        var startOffset = blockIndex == range.start().blockIndex()
                ? range.start().characterOffset()
                : 0;
        var endOffset = blockIndex == range.end().blockIndex()
                ? range.end().characterOffset()
                : InlineContentEditor.characterCount(EditableInlineBlock.contentOf(block));
        return new DocumentRange(new DocumentPosition(blockIndex, startOffset), new DocumentPosition(blockIndex, endOffset));
    }

    private static EditResult moveToPreviousBlockEnd(EditorState state) {
        var previousBlockIndex = state.caret().blockIndex() - 1;
        if (!isEditableInlineBlock(state.document(), previousBlockIndex)) {
            return new EditResult(state.document(), state.caret(), false);
        }
        var previousBlock = state.document().blocks().get(previousBlockIndex);
        return new EditResult(
                state.document(),
                new DocumentPosition(previousBlockIndex, InlineContentEditor.characterCount(EditableInlineBlock.contentOf(previousBlock))),
                false);
    }

    private static EditResult moveLeftFromTextBoundary(EditorState state) {
        var previousBlockIndex = state.caret().blockIndex() - 1;
        if (isStructuralObjectBlock(state.document(), previousBlockIndex)) {
            return new EditResult(state.document(), new BlockSelection(previousBlockIndex), Optional.empty(), false);
        }
        return moveToPreviousBlockEnd(state);
    }

    private static EditResult moveRightFromTextBoundary(EditorState state) {
        var nextBlockIndex = state.caret().blockIndex() + 1;
        if (isStructuralObjectBlock(state.document(), nextBlockIndex)) {
            return new EditResult(state.document(), new BlockSelection(nextBlockIndex), Optional.empty(), false);
        }
        return moveToNextBlockStart(state);
    }

    private static EditResult moveLeftFromBlockSelection(EditorState state) {
        var previousBlockIndex = state.blockSelection().blockIndex() - 1;
        if (previousBlockIndex < 0) {
            return new EditResult(state.document(), state.selection(), Optional.empty(), false);
        }
        if (isEditableInlineBlock(state.document(), previousBlockIndex)) {
            var position = new DocumentPosition(previousBlockIndex, blockLength(state.document().blocks().get(previousBlockIndex)));
            return new EditResult(state.document(), position, Optional.empty(), false);
        }
        return new EditResult(state.document(), new BlockSelection(previousBlockIndex), Optional.empty(), false);
    }

    private static EditResult moveRightFromBlockSelection(EditorState state) {
        var nextBlockIndex = state.blockSelection().blockIndex() + 1;
        if (nextBlockIndex >= state.document().blocks().size()) {
            return new EditResult(state.document(), state.selection(), Optional.empty(), false);
        }
        if (isEditableInlineBlock(state.document(), nextBlockIndex)) {
            var position = new DocumentPosition(nextBlockIndex, 0);
            return new EditResult(state.document(), position, Optional.empty(), false);
        }
        return new EditResult(state.document(), new BlockSelection(nextBlockIndex), Optional.empty(), false);
    }

    private static EditResult selectPreviousObject(EditorState state) {
        var previousBlockIndex = state.caret().blockIndex() - 1;
        if (isStructuralObjectBlock(state.document(), previousBlockIndex)) {
            return new EditResult(state.document(), new BlockSelection(previousBlockIndex), Optional.empty(), false);
        }
        return new EditResult(state.document(), state.selection(), state.explicitTypingMarks(), false);
    }

    private static EditResult selectNextObject(EditorState state) {
        var nextBlockIndex = state.caret().blockIndex() + 1;
        if (isStructuralObjectBlock(state.document(), nextBlockIndex)) {
            return new EditResult(state.document(), new BlockSelection(nextBlockIndex), Optional.empty(), false);
        }
        return new EditResult(state.document(), state.selection(), state.explicitTypingMarks(), false);
    }

    private static EditResult moveToNextBlockStart(EditorState state) {
        var nextBlockIndex = state.caret().blockIndex() + 1;
        if (!isEditableInlineBlock(state.document(), nextBlockIndex)) {
            return new EditResult(state.document(), state.caret(), false);
        }
        return new EditResult(state.document(), new DocumentPosition(nextBlockIndex, 0), false);
    }

    private static DocumentPosition previousSelectionPosition(Document document, DocumentPosition position) {
        var block = editableInlineBlock(document, position.blockIndex());
        if (position.characterOffset() > 0) {
            return new DocumentPosition(position.blockIndex(), TextBoundary.previousOffset(blockText(block), position.characterOffset()));
        }
        var previousBlockIndex = position.blockIndex() - 1;
        if (!isEditableInlineBlock(document, previousBlockIndex)) {
            return position;
        }
        var previousBlock = document.blocks().get(previousBlockIndex);
        return new DocumentPosition(previousBlockIndex, InlineContentEditor.characterCount(EditableInlineBlock.contentOf(previousBlock)));
    }

    private static DocumentPosition nextSelectionPosition(Document document, DocumentPosition position) {
        var block = editableInlineBlock(document, position.blockIndex());
        var length = InlineContentEditor.characterCount(EditableInlineBlock.contentOf(block));
        if (position.characterOffset() < length) {
            return new DocumentPosition(position.blockIndex(), TextBoundary.nextOffset(blockText(block), position.characterOffset()));
        }
        var nextBlockIndex = position.blockIndex() + 1;
        if (!isEditableInlineBlock(document, nextBlockIndex)) {
            return position;
        }
        return new DocumentPosition(nextBlockIndex, 0);
    }

    private static EditResult joinWithPreviousBlock(EditorState state) {
        var rightBlockIndex = state.caret().blockIndex();
        var leftBlockIndex = rightBlockIndex - 1;
        if (!isEditableInlineBlock(state.document(), leftBlockIndex) || !isEditableInlineBlock(state.document(), rightBlockIndex)) {
            return new EditResult(state.document(), state.caret(), false);
        }
        return joinBlocks(state.document(), leftBlockIndex, rightBlockIndex);
    }

    private static EditResult joinWithNextBlock(EditorState state) {
        var leftBlockIndex = state.caret().blockIndex();
        var rightBlockIndex = leftBlockIndex + 1;
        if (!isEditableInlineBlock(state.document(), leftBlockIndex) || !isEditableInlineBlock(state.document(), rightBlockIndex)) {
            return new EditResult(state.document(), state.caret(), false);
        }
        return joinBlocks(state.document(), leftBlockIndex, rightBlockIndex);
    }

    private static EditResult joinBlocks(Document document, int leftBlockIndex, int rightBlockIndex) {
        var leftBlock = document.blocks().get(leftBlockIndex);
        var rightBlock = document.blocks().get(rightBlockIndex);
        var oldLeftLength = InlineContentEditor.characterCount(EditableInlineBlock.contentOf(leftBlock));
        var joinedContent = InlineContentEditor.concat(
                EditableInlineBlock.contentOf(leftBlock),
                EditableInlineBlock.contentOf(rightBlock));
        var updatedBlocks = new ArrayList<BlockNode>(document.blocks());
        updatedBlocks.set(leftBlockIndex, EditableInlineBlock.withContent(leftBlock, joinedContent));
        updatedBlocks.remove(rightBlockIndex);
        return new EditResult(withBlocks(document, updatedBlocks), new DocumentPosition(leftBlockIndex, oldLeftLength), true);
    }

    private static boolean isEditableInlineBlock(Document document, int blockIndex) {
        return blockIndex >= 0
                && blockIndex < document.blocks().size()
                && EditableInlineBlock.supports(document.blocks().get(blockIndex));
    }

    private static boolean isStructuralObjectBlock(Document document, int blockIndex) {
        return blockIndex >= 0
                && blockIndex < document.blocks().size()
                && !EditableInlineBlock.supports(document.blocks().get(blockIndex));
    }

    private static boolean hasMultiBlockSelection(EditorState state) {
        return state.hasSelection() && !state.selectionRange().isSingleBlock();
    }

    private static Set<TextMark> marksForReplacement(EditorState state, Set<TextMark> requestedMarks) {
        if (!hasMultiBlockSelection(state)) {
            return Set.copyOf(requestedMarks);
        }
        return marksForInsertion(state.document(), state.selectionRange().start());
    }

    private EditResult transformMark(EditorState state, TextMark mark, boolean add) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(mark, "mark");
        if (!state.hasSelection()) {
            return new EditResult(state.document(), state.anchor(), state.active(), state.explicitTypingMarks(), false);
        }

        var range = state.selectionRange();
        validateEditableRange(state.document(), range);
        var updatedBlocks = new ArrayList<BlockNode>(state.document().blocks());
        for (var blockIndex = range.start().blockIndex(); blockIndex <= range.end().blockIndex(); blockIndex++) {
            var block = editableInlineBlock(state.document(), blockIndex);
            var localRange = selectedLocalRange(block, range, blockIndex);
            if (localRange.start().characterOffset() == localRange.end().characterOffset()) {
                continue;
            }
            updatedBlocks.set(blockIndex, EditableInlineBlock.withContent(
                    block,
                    new InlineContent(transformInlineMark(block, localRange, mark, add))));
        }

        var updatedDocument = withBlocks(state.document(), updatedBlocks);
        return new EditResult(
                updatedDocument,
                state.anchor(),
                state.active(),
                state.explicitTypingMarks(),
                !updatedDocument.equals(state.document()));
    }

    private static List<InlineNode> transformInlineMark(BlockNode block, DocumentRange range, TextMark mark, boolean add) {
        var updatedNodes = new ArrayList<InlineNode>();
        var logicalOffset = 0;
        for (var node : EditableInlineBlock.contentOf(block).nodes()) {
            var nodeLength = InlineContentEditor.characterCount(node);
            var nodeStart = logicalOffset;
            var nodeEnd = nodeStart + nodeLength;
            var selectedStart = Math.max(range.start().characterOffset(), nodeStart);
            var selectedEnd = Math.min(range.end().characterOffset(), nodeEnd);

            if (selectedStart >= selectedEnd) {
                updatedNodes.add(node);
            } else if (node instanceof Text text) {
                addTextSegment(updatedNodes, text, 0, selectedStart - nodeStart, text.marks());
                addTextSegment(updatedNodes, text, selectedStart - nodeStart, selectedEnd - nodeStart, transformedMarks(text.marks(), mark, add));
                addTextSegment(updatedNodes, text, selectedEnd - nodeStart, nodeLength, text.marks());
            } else {
                updatedNodes.add(node);
            }
            logicalOffset = nodeEnd;
        }
        return updatedNodes;
    }

    private static Set<TextMark> transformedMarks(Set<TextMark> source, TextMark mark, boolean add) {
        var marks = source.isEmpty() ? EnumSet.noneOf(TextMark.class) : EnumSet.copyOf(source);
        if (add) {
            marks.add(mark);
        } else {
            marks.remove(mark);
        }
        return Set.copyOf(marks);
    }

    private static void addTextSegment(List<InlineNode> nodes, Text source, int startOffset, int endOffset, Set<TextMark> marks) {
        if (startOffset >= endOffset) {
            return;
        }
        nodes.add(new Text(TextBoundary.substring(source.content(), startOffset, endOffset), marks));
    }

    private static Set<TextMark> marksForInsertion(Document document, DocumentPosition position) {
        return marksForInsertion(editableInlineBlock(document, position.blockIndex()), position.characterOffset());
    }

    private static Set<TextMark> marksForInsertion(BlockNode block, int characterOffset) {
        var logicalOffset = 0;
        Text previous = null;
        Text following = null;
        for (var node : EditableInlineBlock.contentOf(block).nodes()) {
            if (!(node instanceof Text text)) {
                logicalOffset += InlineContentEditor.characterCount(node);
                continue;
            }
            var nodeStart = logicalOffset;
            var nodeEnd = nodeStart + TextBoundary.characterCount(text.content());
            if (characterOffset > nodeStart && characterOffset <= nodeEnd) {
                return text.marks();
            }
            if (characterOffset == nodeStart) {
                following = text;
                break;
            }
            previous = text;
            logicalOffset = nodeEnd;
        }
        if (previous != null) {
            return previous.marks();
        }
        if (following != null) {
            return following.marks();
        }
        return Set.of();
    }

    private static BlockNode editableInlineBlock(Document document, int blockIndex) {
        if (blockIndex < 0 || blockIndex >= document.blocks().size()) {
            throw new IllegalArgumentException("blockIndex is outside the document.");
        }
        var block = document.blocks().get(blockIndex);
        if (!EditableInlineBlock.supports(block)) {
            throw new IllegalArgumentException("Only Paragraph and Heading text blocks are editable in Milestone 8B: " + block.getClass().getName());
        }
        return block;
    }

    private static Document withBlocks(Document document, List<BlockNode> blocks) {
        return new Document(blocks, document.datasets());
    }

    private static String blockText(BlockNode block) {
        return InlineContentEditor.logicalText(EditableInlineBlock.contentOf(block));
    }
}
