# M24F Implementation Report

Status: Implemented.

M24F adds an explicit input/focus contract and a small core API for identifying the current semantic input owner. The implementation keeps the existing screen dispatch architecture and strengthens it with pure Java tests.

## Implemented

- Added `EditorFocusOwner`.
- Added `EditorSession.focusOwner()`.
- Isolated toolbar dropdown clicks so closing a popup cannot also retarget the document.
- Added validator-backed input/focus consistency tests.
- Added `/scholar_dev_editor` fixture text for M24F manual QA.
- Documented dispatch priority, event consumption rules, nested entry/exit, Escape/Enter/Tab/Ctrl+A behavior, popup isolation, and history expectations.
- Added accepted ADRs 0271-0275.

## Behavior Confirmed By Tests

- Only one semantic focus owner is derived from the current selection.
- Entering equation/table/plot/diagram/figure-caption editing is selection-only and does not create undo history.
- Leaving table/plot/diagram editing is selection-only and does not create undo history.
- Text input in a table cell changes only that table cell.
- Text input in a figure caption changes only that caption.
- `Ctrl+A` targets the active scope for document text, math, table cell text, and figure captions.
- Table Tab and Shift+Tab traversal are focus movement, not document mutation.
- Undo/redo restore nested selections coherently.
- Explicit state changes clear transient diagram drag state.
- A golden input-like sequence keeps document and selection validity intact.

## Manual QA Fixture

Use `/scholar_dev_editor`, then find `M24F input and focus fixture`.

Suggested checks:

- Open each popup and verify character input does not edit the document behind it.
- Press Escape with a popup, context menu, diagram drag, diagram connection, and nested editor active; only one layer should unwind.
- Use Tab and Shift+Tab inside table, plot, and diagram editing.
- Use Ctrl+A in prose, an equation, a table cell, and a figure caption.
- Use Cut/Copy/Paste shortcuts in each active scope.
- Use undo/redo after nested edits and confirm the selection/focus is valid.
