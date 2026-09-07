# ADR 0171 - Plot Structural Editing Is Explicit And Does Not Infer Data

## Status

Accepted

## Context

Adding/removing series and points can easily drift toward spreadsheet-like inference or implicit behavior. Scholar's existing authoring model favors explicit structural commands.

## Decision

M16E adds explicit commands for adding LINE/SCATTER series, changing selected series kind, adding/deleting points, and deleting series. New points begin as the deterministic finite value `(0, 0)` and are edited explicitly; normal typing does not infer plot structure.

## Alternatives Considered

- Infer new series or points from typed text.
- Automatically append points from pasted tabular data.
- Generate new point values from neighboring data.

## Consequences

Plot structure remains predictable and undoable. Spreadsheet-style data import/paste remains a later interchange feature rather than hidden editing behavior.
