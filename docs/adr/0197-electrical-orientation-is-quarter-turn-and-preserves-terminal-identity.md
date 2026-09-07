# ADR 0197 - Electrical Orientation Is Quarter Turn And Preserves Terminal Identity

## Status

Accepted

## Context

Electrical symbols commonly need horizontal and vertical orientations, and polarized components also need reversal. Arbitrary-angle geometry would add complexity without improving the first schematic-authoring slice.

## Decision

M18 orientation is limited to `DEG_0`, `DEG_90`, `DEG_180`, and `DEG_270`. Rotation transforms symbol geometry and derived terminal placement around component bounds while preserving stable terminal IDs and existing connection endpoints.

Reference designators and value labels remain upright relative to the document rather than rotating with symbol strokes.

## Alternatives Considered

- Arbitrary floating-point rotation angles.
- Mirror flags plus arbitrary rotation in the first slice.
- Recreate ports/connections when a component rotates.

## Consequences

Rotation is deterministic, easy to test, and sufficient for conventional first-pass schematics. Mirroring and arbitrary angles remain explicit future decisions.
