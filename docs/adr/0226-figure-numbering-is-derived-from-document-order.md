# ADR 0226: Figure numbering is derived from document order

## Status
Accepted for M20.

## Context
Displayed figure numbers must stay correct after insertion, deletion, paste, undo, and reorder operations.

## Decision
Do not store displayed figure numbers in `FigureBlock`. Derive "Figure N" from the figure's current order in the containing `Document`.

## Alternatives Considered
- Store authored numeric labels in each figure.
- Update stored numbers during every structural edit.

## Consequences
Renumbering is deterministic and avoids stale authored numbering. References to figures must target stable figure IDs rather than display numbers.
