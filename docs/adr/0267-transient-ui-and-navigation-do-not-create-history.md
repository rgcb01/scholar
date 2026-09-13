# ADR 0267 - Transient UI And Navigation Do Not Create History

## Status

Accepted

## Context

Scholar has caret movement, selection movement, context menus, outline/TOC navigation, diagram viewport state, and drag previews. These are important interactions but are not semantic document edits.

## Decision

Transient UI, navigation, selection-only transitions, viewport camera changes, and previews update current interaction state without pushing undo entries and without clearing redo.

## Alternatives Considered

- Treat every `EditorState` change as undoable.
- Keep separate undo stacks for UI and document history.

## Consequences

Undo remains a document-edit tool, not a UI-travel log. Selection can still be restored when it is part of an undo snapshot created by a semantic edit.
