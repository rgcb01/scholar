# ADR 0204: Electrical Layout Keeps Derived Symbol Geometry Separate From Authored AST

## Status

Accepted

## Context

M18B needs standard schematic symbols while preserving the M18A rule that the document AST stores electrical meaning rather than drawing commands. The existing M17 laid-out model only represented generic rectangular nodes and connections.

A premature generic vector-graphics AST would make Scholar more complicated without evidence that authored arbitrary geometry is needed.

## Decision

`ElectricalComponent` stores only semantic kind, logical bounds, orientation, reference designator, and value label. `ElectricalSymbolLibrary` derives a small normalized presentation vocabulary (line, polyline, circle) in pure Java. `ElectricalSymbolLayoutEngine` maps that normalized geometry into positioned `LaidOutElectricalComponent` primitives.

`LaidOutDiagram` carries generic nodes and electrical components in separate derived collections for this slice. The Minecraft renderer only rasterizes already-positioned primitives and does not decide electrical kind, terminal identity, or orientation.

## Consequences

- Resistor, capacitor, DC voltage source, and ground symbols are deterministic and responsive without storing pixels in the AST.
- Schematic diagonals such as resistor zig-zags are possible without moving electrical semantics into Minecraft rendering code.
- A generic laid-out-element polymorphic refactor is deferred until a second domain such as M19 provides evidence that it reduces real duplication.
- The normalized primitive vocabulary is internal presentation infrastructure, not SVG, not a public symbol language, and not a user-authored vector format.
