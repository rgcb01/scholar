package dev.rgcb.scholar.editor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class EditorHistory {
    public static final int DEFAULT_CAPACITY = 100;

    private final int capacity;
    private final Deque<EditorState> undo = new ArrayDeque<>();
    private final Deque<EditorState> redo = new ArrayDeque<>();
    private EditorState current;
    private boolean typingGroupOpen;
    private EditorSelection typingGroupEnd;

    public EditorHistory(EditorState initialState) {
        this(initialState, DEFAULT_CAPACITY);
    }

    public EditorHistory(EditorState initialState, int capacity) {
        current = Objects.requireNonNull(initialState, "initialState");
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive.");
        }
        this.capacity = capacity;
    }

    public EditorState current() {
        return current;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    public void setCurrent(EditorState state) {
        current = Objects.requireNonNull(state, "state").withoutExplicitTypingMarks();
        closeTypingGroup();
    }

    public void replaceCurrent(EditorState state) {
        current = Objects.requireNonNull(state, "state");
        closeTypingGroup();
    }

    public boolean applyTyping(EditResult result, String insertedText) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(insertedText, "insertedText");
        if (!result.changed()) {
            return false;
        }

        var resultState = result.editorState();
        if (canCoalesceTyping(resultState, insertedText)) {
            current = resultState;
            typingGroupEnd = resultState.selection();
        } else {
            var opensTypingGroup = isPlainContiguousInsertion(resultState, insertedText);
            pushUndo(current);
            current = resultState;
            typingGroupOpen = opensTypingGroup;
            typingGroupEnd = typingGroupOpen ? resultState.selection() : null;
        }
        redo.clear();
        return true;
    }

    public boolean applyEdit(EditResult result) {
        Objects.requireNonNull(result, "result");
        closeTypingGroup();
        if (!result.changed()) {
            return false;
        }
        pushUndo(current);
        current = result.editorState();
        redo.clear();
        return true;
    }

    public boolean undo() {
        closeTypingGroup();
        if (undo.isEmpty()) {
            return false;
        }
        redo.addLast(current);
        current = undo.removeLast();
        return true;
    }

    public boolean redo() {
        closeTypingGroup();
        if (redo.isEmpty()) {
            return false;
        }
        pushUndo(current);
        current = redo.removeLast();
        return true;
    }

    public void closeTypingTransaction() {
        closeTypingGroup();
    }

    private boolean canCoalesceTyping(EditorState resultState, String insertedText) {
        return typingGroupOpen
                && typingGroupEnd != null
                && typingGroupEnd.equals(current.selection())
                && isPlainContiguousInsertion(resultState, insertedText);
    }

    private boolean isPlainContiguousInsertion(EditorState resultState, String insertedText) {
        if (insertedText.isEmpty()) {
            return false;
        }
        if (current.isEquationEditingSelection() || resultState.isEquationEditingSelection()) {
            return current.isEquationEditingSelection()
                    && resultState.isEquationEditingSelection()
                    && current.equationEditingSelection().blockIndex() == resultState.equationEditingSelection().blockIndex()
                    && !current.hasSelection()
                    && !resultState.hasSelection();
        }
        if (current.isTableEditingSelection() || resultState.isTableEditingSelection()) {
            if (!current.isTableEditingSelection() || !resultState.isTableEditingSelection()) {
                return false;
            }
            var currentSelection = current.tableEditingSelection();
            var resultSelection = resultState.tableEditingSelection();
            if (!currentSelection.selection().isCaret() || !resultSelection.selection().isCaret()) {
                return false;
            }
            var insertionStart = currentSelection.selection().activeOffset();
            return currentSelection.blockIndex() == resultSelection.blockIndex()
                    && currentSelection.selection().cell().equals(resultSelection.selection().cell())
                    && insertionStart + TextBoundary.characterCount(insertedText) == resultSelection.selection().activeOffset();
        }
        if (resultState.hasSelection()) {
            return false;
        }
        var insertionStart = current.hasSelection() ? current.selectionRange().start() : current.caret();
        return insertionStart.blockIndex() == resultState.caret().blockIndex()
                && insertionStart.characterOffset() + TextBoundary.characterCount(insertedText) == resultState.caret().characterOffset();
    }

    private void pushUndo(EditorState snapshot) {
        undo.addLast(snapshot);
        while (undo.size() > capacity) {
            undo.removeFirst();
        }
    }

    private void closeTypingGroup() {
        typingGroupOpen = false;
        typingGroupEnd = null;
    }
}
