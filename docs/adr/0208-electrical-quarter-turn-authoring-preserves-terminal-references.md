# ADR 0208: Electrical Quarter Turn Authoring Preserves Terminal References

## Status

Accepted

## Context

Electrical components must be rotatable during authoring without breaking wires. Connectivity already references stable semantic terminal IDs, while symbol geometry and terminal placement are derived from orientation.

## Decision

M18D supports clockwise and counterclockwise 90-degree rotations only. Rotation changes `ElectricalOrientation`, swaps authored bounds width/height when orientation parity changes, preserves the component center when possible, and clamps the resulting bounds inside the logical canvas.

Component ID, kind, reference/value annotations, terminal IDs, and existing `DiagramConnection` endpoints remain unchanged. Layout/routing subsequently derives the new visible terminal positions and wire paths.

## Consequences

- A wire continues to reference, for example, `d1/anode` across all four orientations.
- Rotation is one immutable global-history edit and is fully undoable/redoable.
- No wire endpoints are rewritten merely because the visual side of a terminal changed.
- Arbitrary-angle rotation, mirroring, and freeform symbol transforms remain deferred.
