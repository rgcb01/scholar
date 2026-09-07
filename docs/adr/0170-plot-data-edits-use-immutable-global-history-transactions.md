# ADR 0170 - Plot Data Edits Use Immutable Global History Transactions

## Status

Accepted

## Context

M16E introduces edits to titles, labels, series metadata, series kinds, and finite XY points. Maintaining a separate plot-local history would conflict with Scholar's document-level undo/redo model.

## Decision

Plot edits rebuild immutable `PlotDefinition` / `PlotBlock` values and replace the owning block through the existing global `EditorHistory`. Each semantic mutation is one history transaction; target navigation itself is not undoable.

## Alternatives Considered

- Mutable PlotDefinition state.
- A plot-local undo stack.
- Direct client-side mutation followed by synchronization.

## Consequences

Undo/redo restores both document content and `PlotEditingSelection` consistently with tables and equations. Core plot editing remains Minecraft-independent.
