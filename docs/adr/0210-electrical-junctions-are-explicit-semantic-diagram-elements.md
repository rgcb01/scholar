# ADR 0210 — Electrical Junctions Are Explicit Semantic Diagram Elements

## Status
Accepted for M18E.

## Decision
A free-space electrical splice is represented by `ElectricalJunction implements DiagramElement` inside the existing `DiagramBlock`.

The junction owns stable local identity, logical bounds, four directional connection ports, and an optional authored net label. A filled junction dot is derived during layout/render; it is not persisted as pixels or routed geometry.

## Consequences
- A wire crossing has no electrical meaning unless an explicit junction participates in the connection graph.
- M17 selection, dragging, endpoint validation, clipboard, and history infrastructure remain reusable.
- Junction deletion can remove all incident wire segments as one immutable edit.
