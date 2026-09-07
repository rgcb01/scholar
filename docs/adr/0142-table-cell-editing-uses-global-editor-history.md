# ADR 142 - Table Cell Editing Uses Global Editor History

## Status

Accepted

## Context

Table cell edits change the immutable document snapshot, and undo/redo should behave like the rest of the editor.

## Decision

`TableEditor` is stateless and returns immutable table edit results. `EditorSession` replaces the edited `TableBlock` inside the document and records edits in the existing global `EditorHistory`.

## Alternatives Considered

- Add a separate table-local history.
- Store mutable cell edit buffers outside the document.
- Commit table edits only when leaving table mode.

## Consequences

Undo and redo restore table content and table editing selection together. Clipboard side effects remain outside history, matching existing editor behavior.
