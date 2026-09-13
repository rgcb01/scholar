# ADR 0274 - Transient Focus Is Outside Semantic History

## Status

Accepted

## Context

Focus changes, popup visibility, menu state, viewport pan/zoom, drag previews, and target traversal are interaction state. Recording them as semantic history would add undo noise and make document undo/redo harder to reason about.

## Decision

Transient focus and UI interaction state do not create undo history. History records semantic document edit snapshots only.

## Alternatives Considered

- Store every focus movement in undo history.
- Store popup and viewport state with document snapshots.

## Consequences

Undo/redo remains document-oriented. Selection/focus can still be restored when it belongs to a semantic edit snapshot, while purely transient UI state is cleared or recomputed.

