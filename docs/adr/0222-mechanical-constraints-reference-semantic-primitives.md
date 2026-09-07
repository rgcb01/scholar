# ADR 0222: Mechanical constraints reference semantic primitives

## Status
Accepted for M19C.

## Decision
Represent geometric relationships as dedicated `MechanicalConstraint` diagram elements that
reference mechanical primitive IDs. Constraint marker geometry is derived and is not persisted as
the source of truth.

M19C uses a deliberately bounded deterministic reconciliation pass instead of introducing a
general CAD solver.

## Consequences
- Relationships survive layout, zoom/pan, clipboard, and document reflow without encoding pixels.
- Deleting a referenced primitive can deterministically remove dependent constraints.
- Horizontal/vertical and parallel/perpendicular can operate on semantic orientation.
- Coincident currently means center-coincident because editable primitive endpoints are not yet a
  first-class authored concept.
- A future solver or endpoint-handle model can extend the same semantic relationship vocabulary
  without replacing the rendering contract.
