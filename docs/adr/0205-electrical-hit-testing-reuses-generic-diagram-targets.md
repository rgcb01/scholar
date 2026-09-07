# ADR 0205: Electrical Hit Testing Reuses Generic Diagram Targets

## Status

Accepted

## Context

M18C must make schematic components and their derived terminals selectable without creating a second electrical editor-selection hierarchy. M17 already defines stable semantic edit targets for diagram elements, ports, connections, title, and canvas.

Electrical symbols are sparse derived geometry: a component may contain thin lines, a circle, annotations, and whitespace inside its authored logical bounds. Exact one-pixel hit testing would be frustrating at Minecraft GUI scale, while selecting only the whole bounds would ignore the semantic terminal priority required for wiring.

## Decision

Electrical terminal hit regions participate in `DiagramHitTester` before their owning component and map to the existing `DiagramPortTarget` using the stable terminal ID. Electrical component symbol geometry, reference designator, value label, and a bounded forgiving component rectangle map to the existing `DiagramElementTarget`.

Hit testing remains pure Java over `LaidOutDiagram`/`LaidOutElectricalComponent`; Minecraft rendering does not decide electrical selection semantics. Existing priority remains effectively `port > element > connection > canvas`.

## Consequences

- Rotated terminals remain selectable by stable semantic IDs even though their visual side changes.
- M17 connection-draft semantics can target electrical terminals without a new electrical port type.
- M18D can add drag/rotate/edit/delete operations on the same `DiagramEditingSelection` target model.
- Sparse symbols such as an open SPST switch remain easy to select without requiring pixel-perfect clicks.
- No `ElectricalEditingSelection` or electrical-only hit-test state is introduced.
