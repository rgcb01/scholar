package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.layout.LaidOutBlock;
import dev.rgcb.scholar.layout.LaidOutBlockKind;
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.layout.LaidOutLine;
import dev.rgcb.scholar.layout.LaidOutText;
import dev.rgcb.scholar.layout.TextMeasurer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class VisualLineNavigator {
    public Optional<EditorSelection> moveUp(
            EditorState state,
            LaidOutDocument document,
            TextMeasurer textMeasurer,
            int preferredX,
            boolean allowAtomicBlockSelection
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        if (state.isEquationEditingSelection()) {
            return Optional.empty();
        }
        if (state.isBlockSelection()) {
            return moveUpFromBlockSelection(state, document, textMeasurer, preferredX);
        }

        var position = state.active();
        var currentLine = lineContaining(document, position);
        if (currentLine.isEmpty()) {
            return Optional.empty();
        }
        var line = currentLine.orElseThrow();
        if (line.lineIndex() > 0) {
            return Optional.of(caretSelection(closestPosition(lineAt(document, line.blockIndex(), line.lineIndex() - 1), textMeasurer, preferredX)));
        }

        for (var blockIndex = position.blockIndex() - 1; blockIndex >= 0; blockIndex--) {
            var block = document.blocks().get(blockIndex);
            if (isEditableTextBlock(block)) {
                return lastTextLine(blockIndex, block)
                        .map(target -> caretSelection(closestPosition(target, textMeasurer, preferredX)));
            }
            if (allowAtomicBlockSelection) {
                return Optional.of(new BlockSelection(blockIndex));
            }
            return Optional.of(caretSelection(lineStart(line.line(), line.blockIndex())));
        }
        return Optional.empty();
    }

    public Optional<EditorSelection> moveDown(
            EditorState state,
            LaidOutDocument document,
            TextMeasurer textMeasurer,
            int preferredX,
            boolean allowAtomicBlockSelection
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        if (state.isEquationEditingSelection()) {
            return Optional.empty();
        }
        if (state.isBlockSelection()) {
            return moveDownFromBlockSelection(state, document, textMeasurer, preferredX);
        }

        var position = state.active();
        var currentLine = lineContaining(document, position);
        if (currentLine.isEmpty()) {
            return Optional.empty();
        }
        var line = currentLine.orElseThrow();
        var block = document.blocks().get(line.blockIndex());
        if (line.lineIndex() < block.lines().size() - 1) {
            return Optional.of(caretSelection(closestPosition(lineAt(document, line.blockIndex(), line.lineIndex() + 1), textMeasurer, preferredX)));
        }

        for (var blockIndex = position.blockIndex() + 1; blockIndex < document.blocks().size(); blockIndex++) {
            var nextBlock = document.blocks().get(blockIndex);
            if (isEditableTextBlock(nextBlock)) {
                return firstTextLine(blockIndex, nextBlock)
                        .map(target -> caretSelection(closestPosition(target, textMeasurer, preferredX)));
            }
            if (allowAtomicBlockSelection) {
                return Optional.of(new BlockSelection(blockIndex));
            }
            return Optional.of(caretSelection(lineEnd(line.line(), line.blockIndex())));
        }
        return Optional.empty();
    }

    public Optional<DocumentPosition> lineStart(DocumentPosition position, LaidOutDocument document) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(document, "document");
        return lineContaining(document, position).map(line -> lineStart(line.line(), line.blockIndex()));
    }

    public Optional<DocumentPosition> lineEnd(DocumentPosition position, LaidOutDocument document) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(document, "document");
        return lineContaining(document, position).map(line -> lineEnd(line.line(), line.blockIndex()));
    }

    public int preferredXForBlockSelection(BlockSelection selection, LaidOutDocument document) {
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(document, "document");
        var block = document.blocks().get(selection.blockIndex());
        return block.x() + block.width() / 2;
    }

    private Optional<EditorSelection> moveUpFromBlockSelection(
            EditorState state,
            LaidOutDocument document,
            TextMeasurer textMeasurer,
            int preferredX
    ) {
        var selectedBlockIndex = state.blockSelection().blockIndex();
        for (var blockIndex = selectedBlockIndex - 1; blockIndex >= 0; blockIndex--) {
            var block = document.blocks().get(blockIndex);
            if (isEditableTextBlock(block)) {
                return lastTextLine(blockIndex, block)
                        .map(line -> caretSelection(closestPosition(line, textMeasurer, preferredX)));
            }
            return Optional.of(new BlockSelection(blockIndex));
        }
        return Optional.empty();
    }

    private Optional<EditorSelection> moveDownFromBlockSelection(
            EditorState state,
            LaidOutDocument document,
            TextMeasurer textMeasurer,
            int preferredX
    ) {
        var selectedBlockIndex = state.blockSelection().blockIndex();
        for (var blockIndex = selectedBlockIndex + 1; blockIndex < document.blocks().size(); blockIndex++) {
            var block = document.blocks().get(blockIndex);
            if (isEditableTextBlock(block)) {
                return firstTextLine(blockIndex, block)
                        .map(line -> caretSelection(closestPosition(line, textMeasurer, preferredX)));
            }
            return Optional.of(new BlockSelection(blockIndex));
        }
        return Optional.empty();
    }

    private static Optional<VisualLine> lineContaining(LaidOutDocument document, DocumentPosition position) {
        if (position.blockIndex() < 0 || position.blockIndex() >= document.blocks().size()) {
            return Optional.empty();
        }
        var block = document.blocks().get(position.blockIndex());
        if (!isEditableTextBlock(block)) {
            return Optional.empty();
        }
        VisualLine atomicEnd = null;
        for (var lineIndex = 0; lineIndex < block.lines().size(); lineIndex++) {
            var line = block.lines().get(lineIndex);
            if (line.textRuns().isEmpty() && position.characterOffset() == 0) {
                return Optional.of(new VisualLine(position.blockIndex(), lineIndex, line));
            }
            for (var run : line.textRuns()) {
                if (run.sourceBlockIndex() == position.blockIndex()
                        && position.characterOffset() >= run.sourceStart()
                        && position.characterOffset() <= run.sourceEnd()) {
                    if (run.atomic() && position.characterOffset() == run.sourceEnd()) {
                        atomicEnd = new VisualLine(position.blockIndex(), lineIndex, line);
                        continue;
                    }
                    return Optional.of(new VisualLine(position.blockIndex(), lineIndex, line));
                }
            }
        }
        return Optional.ofNullable(atomicEnd);
    }

    private static TextSelection caretSelection(DocumentPosition position) {
        return new TextSelection(position, position);
    }

    private static Optional<VisualLine> firstTextLine(int blockIndex, LaidOutBlock block) {
        for (var lineIndex = 0; lineIndex < block.lines().size(); lineIndex++) {
            var line = block.lines().get(lineIndex);
            if (hasSourceRun(line, blockIndex)) {
                return Optional.of(new VisualLine(blockIndex, lineIndex, line));
            }
        }
        return block.lines().isEmpty()
                ? Optional.empty()
                : Optional.of(new VisualLine(blockIndex, 0, block.lines().get(0)));
    }

    private static VisualLine lineAt(LaidOutDocument document, int blockIndex, int lineIndex) {
        return new VisualLine(blockIndex, lineIndex, document.blocks().get(blockIndex).lines().get(lineIndex));
    }

    private static Optional<VisualLine> lastTextLine(int blockIndex, LaidOutBlock block) {
        for (var lineIndex = block.lines().size() - 1; lineIndex >= 0; lineIndex--) {
            var line = block.lines().get(lineIndex);
            if (hasSourceRun(line, blockIndex)) {
                return Optional.of(new VisualLine(blockIndex, lineIndex, line));
            }
        }
        return block.lines().isEmpty()
                ? Optional.empty()
                : Optional.of(new VisualLine(blockIndex, block.lines().size() - 1, block.lines().get(block.lines().size() - 1)));
    }

    private static boolean hasSourceRun(LaidOutLine line, int blockIndex) {
        return line.textRuns().stream().anyMatch(run -> run.sourceBlockIndex() == blockIndex);
    }

    private static DocumentPosition closestPosition(
            VisualLine visualLine,
            TextMeasurer textMeasurer,
            int preferredX
    ) {
        var candidates = candidatesForLine(visualLine.line(), visualLine.blockIndex(), textMeasurer);
        if (candidates.isEmpty()) {
            return new DocumentPosition(visualLine.blockIndex(), 0);
        }
        return candidates.stream()
                .min(Comparator
                        .comparingInt((CaretCandidate candidate) -> Math.abs(candidate.x() - preferredX))
                        .thenComparing(Comparator.comparingInt(CaretCandidate::offset).reversed()))
                .map(candidate -> new DocumentPosition(visualLine.blockIndex(), candidate.offset()))
                .orElseGet(() -> new DocumentPosition(visualLine.blockIndex(), 0));
    }

    private static List<CaretCandidate> candidatesForLine(LaidOutLine line, int blockIndex, TextMeasurer textMeasurer) {
        var candidates = new ArrayList<CaretCandidate>();
        if (line.textRuns().isEmpty()) {
            candidates.add(new CaretCandidate(0, line.x()));
            return List.copyOf(candidates);
        }
        for (var run : line.textRuns()) {
            if (run.sourceBlockIndex() != blockIndex) {
                continue;
            }
            if (run.atomic()) {
                candidates.add(new CaretCandidate(run.sourceStart(), run.x()));
                candidates.add(new CaretCandidate(run.sourceEnd(), run.x() + run.width()));
                continue;
            }
            var characterCount = TextBoundary.characterCount(run.text());
            for (var offset = 0; offset <= characterCount; offset++) {
                var prefix = TextBoundary.substring(run.text(), 0, offset);
                var x = line.x() + run.x() + textMeasurer.measureWidth(prefix, run.style());
                candidates.add(new CaretCandidate(run.sourceStart() + offset, x));
            }
        }
        return List.copyOf(candidates);
    }

    private static DocumentPosition lineStart(LaidOutLine line, int blockIndex) {
        if (line.textRuns().isEmpty()) {
            return new DocumentPosition(blockIndex, 0);
        }
        return line.textRuns().stream()
                .filter(run -> run.sourceBlockIndex() == blockIndex)
                .findFirst()
                .map(run -> new DocumentPosition(blockIndex, run.sourceStart()))
                .orElseGet(() -> new DocumentPosition(blockIndex, 0));
    }

    private static DocumentPosition lineEnd(LaidOutLine line, int blockIndex) {
        if (line.textRuns().isEmpty()) {
            return new DocumentPosition(blockIndex, 0);
        }
        LaidOutText last = null;
        for (var run : line.textRuns()) {
            if (run.sourceBlockIndex() == blockIndex) {
                last = run;
            }
        }
        return last == null
                ? new DocumentPosition(blockIndex, 0)
                : new DocumentPosition(blockIndex, last.sourceEnd());
    }

    private static boolean isEditableTextBlock(LaidOutBlock block) {
        return block.kind() == LaidOutBlockKind.PARAGRAPH || block.kind() == LaidOutBlockKind.HEADING;
    }

    private record VisualLine(int blockIndex, int lineIndex, LaidOutLine line) {
    }

    private record CaretCandidate(int offset, int x) {
    }
}
