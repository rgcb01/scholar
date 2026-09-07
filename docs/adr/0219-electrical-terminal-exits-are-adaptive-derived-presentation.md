# ADR 0219 — Electrical Terminal Exits Are Adaptive Derived Presentation

## Status
Accepted for M18H.

## Context
M18C introduced a fixed 14 px straight exit at electrical terminals before the generic orthogonal router could turn. The extra clearance made early compact symbols readable, but later branched/manual QA exposed a presentation artifact: if a terminal's outward side pointed away from the actual counterpart, the route could travel outward and immediately back over itself before continuing. The resulting short spike looked like a terminal lead protruding beyond its connection point.

The terminal anchor itself is already correct and is used by selection, snapping, stable `DiagramEndpoint` references, clipboard semantics, undo/redo, and derived electrical nets. Moving that anchor to solve a raster artifact would couple semantic geometry to presentation.

## Decision
Keep every electrical terminal/junction anchor exactly where the semantic diagram model and orientation place it.

For derived electrical connection layout only:

- `ELECTRICAL_PORT_EXIT_LENGTH` is a maximum of 8 px rather than a mandatory 14 px reservation.
- An electrical endpoint receives only the portion of that maximum that lies between the terminal and the counterpart in the terminal's outward half-plane.
- If the counterpart is behind the terminal relative to its outward side, the artificial exit length is zero.
- Junction exits remain zero.
- After the existing generic router produces an electrical/junction path, redundant collinear vertices are removed. This preserves the same orthogonal connection while eliminating out-and-back spikes and duplicate straight vertices.
- First and last route points remain the exact semantic laid-out endpoint coordinates. Coincident endpoints still produce the required two-point degenerate route.

## Consequences
- The manual-QA protrusion is fixed without changing authored `DiagramBounds`, terminal placements, stable IDs, snapping, port hit bounds, component dragging, clipboard data, history, or net semantics.
- The generic M17 router remains the only routing engine; M18 adds only domain-specific derived exit parameters and route normalization.
- Clean straight connections may contain fewer route vertices than before. This is derived layout state and is not serialized.
- Connection hit testing and labels continue to consume the final laid-out path, so they stay aligned with what Minecraft renders.
