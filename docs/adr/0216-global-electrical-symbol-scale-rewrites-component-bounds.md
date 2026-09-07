# ADR 0216 — Global Electrical Symbol Scale Rewrites Component Bounds

## Status
Accepted for M18E.5.

## Decision
Global electrical symbol scaling is an authored bulk edit over `ElectricalComponent.bounds()` rather than a renderer-only magnification setting.

Every electrical component is uniformly scaled around its current center and clamped inside the logical canvas. Component IDs, kinds, quarter-turn orientation, reference/value annotations, stable terminal schemas, and `DiagramConnection` endpoints remain unchanged. Generic diagram nodes and explicit electrical junctions are not scaled by this command.

## Consequences
- Symbol scale survives normal document editing and global undo/redo.
- Terminal identity and net topology remain stable because scaling changes geometry, not endpoint references.
- The command is useful for matching symbol density to larger/smaller authored logical canvases.
- This does not perform the final M18 terminal-lead visual polish; terminal rendering remains a separate final closeout item.
