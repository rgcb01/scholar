# ADR 0183 - Diagram Layout And Hit Testing Are Pure Java

## Status

Accepted

## Context

Scholar's existing document, math, table, and plot architecture keeps semantic/layout logic outside Minecraft rendering. Diagrams add more geometry, routing, and reverse mapping, making it especially important not to let the client renderer become the only place where object positions are known.

## Decision

M17 uses a Minecraft-independent `DiagramLayoutEngine` that resolves responsive canvas geometry, elements, ports, labels, and routed connections into `LaidOutDiagram`. Hit testing operates against that laid-out geometry in pure Java.

The Minecraft renderer consumes positioned geometry only. It does not resolve endpoints, route connections, convert semantic coordinates, or decide hit targets.

Initial overlapping hit priority is port, then element, then connection, then canvas. Connection hits use a bounded distance tolerance rather than pixel-perfect equality.

## Alternatives Considered

- Compute diagram geometry directly in `GuiGraphics` rendering code.
- Hit test against the semantic AST without using the current layout.
- Duplicate routing/coordinate logic separately in renderer and editor.

## Consequences

Diagram geometry and interaction remain unit-testable without launching Minecraft. Rendering and editor mapping share one layout truth, reducing visual/interaction drift and preserving the established core dependency boundary.
