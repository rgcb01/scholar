# ADR 0221: Mechanical dimensions are semantic diagram elements

## Status
Accepted for M19B.

## Decision
Represent a mechanical dimension as a dedicated `MechanicalDimension` DiagramElement with a `MechanicalDimensionKind` and authored logical bounds. The numeric callout is derived from geometry and is not persisted as free-form presentation text.

## Consequences
- Dimension rendering can change without rewriting document semantics.
- Generic diagram dragging, history, clipboard and viewport infrastructure remain reusable.
- M19C can layer constraints on the same measurement vocabulary.
- M19B deliberately does not claim associative CAD-style references or solver behavior.
