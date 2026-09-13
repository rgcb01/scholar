# ADR 0275 - Nested Editor Exit Restores Valid Outer Selection

## Status

Accepted

## Context

Equations, tables, plots, diagrams, and figure captions are edited through dedicated selection modes. Exiting those modes must leave the editor in a valid state without inventing document edits.

## Decision

Nested editor exit returns to a valid outer selection, normally the containing atomic block selection where that concept applies, and does not mutate the document or create undo history.

## Alternatives Considered

- Collapse to the nearest text caret after every nested exit.
- Keep invalid nested selections until the next document mutation repairs them.

## Consequences

Focus recovery is deterministic and validator-friendly. Future nested editors must provide an explicit outer selection recovery path.

