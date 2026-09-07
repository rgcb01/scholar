# 0031 Snapshot-Based Editor Undo/Redo

Status: Accepted

## Context

Scholar's document model is immutable, and the editor needs undo/redo without adding transient session state to the semantic AST.

## Decision

Use snapshot-based undo/redo history in a Minecraft-independent `EditorHistory`. Each snapshot is the current immutable `EditorState`, which contains the `Document`, `anchor`, and `active` positions. History is bounded to 100 undo snapshots by default.

## Alternatives Considered

- Command inversion: deferred because a full command framework is not needed for this milestone.
- Store history in `Document`: rejected because undo/redo is editor session state, not semantic content.
- Store layout or scroll state in history: rejected because layout is derived and scroll is client viewport state.

## Consequences

Undo/redo is simple, deterministic, and benefits from immutable document snapshots. Memory use is bounded, and future command-level history can replace or refine this without changing the AST.
