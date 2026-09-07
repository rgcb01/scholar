# ADR 130 - Vertical Navigation Preserves Ephemeral Preferred Caret X

## Status

Accepted

## Context

Repeated Up and Down movement should preserve the user's intended horizontal column even when moving through shorter visual lines.

## Decision

`EditorSession` owns an ephemeral preferred caret X value for consecutive vertical navigation. It is not part of the document AST, `EditorState`, serialization, or undo snapshots.

## Alternatives Considered

- Store preferred X in `EditorState`.
- Store preferred X in the client screen.
- Recompute preferred X from each destination line.

## Consequences

The behavior is testable in core Java and does not leak visual state into document data. Non-vertical actions must clear the preferred X so stale coordinates do not survive editing, mouse movement, relayout, or undo/redo.
