# ADR 0220: Mechanical primitives extend DiagramElement without generic mechanical fields

## Status
Accepted for M19A.

## Context
M19 is the second scientific domain to consume the M17 diagram foundation. The project needs basic mechanical drawing geometry without contaminating generic diagram nodes or electrical elements with discipline-specific fields.

## Decision
M19A introduces `MechanicalPrimitive` as a separate `DiagramElement` implementation with a `MechanicalPrimitiveKind` and authored logical bounds. Primitive-specific rendering remains derived. M19A primitives expose no generic ports and introduce no dimensions, constraints, loads, materials, or simulation state.

## Consequences
The open `DiagramElement` boundary is validated by a second real domain. Generic canvas validation, dragging, viewport behavior, clipboard snapshots, undo/redo, and document ownership are reused. Mechanical-domain semantics can evolve independently in later M19 milestones without widening `DiagramNode` or electrical records.
