# ADR 0211 — Electrical Nets Are Derived From Semantic Connectivity, Not Wire Geometry

## Status
Accepted for M18E.

## Decision
`ElectricalNetResolver` derives nets from semantic `DiagramConnection` endpoint edges plus the internal equivalence of all ports on an `ElectricalJunction`.

Rendered paths, elbows, crossings, pixel contact, and line overlap are never inputs to net resolution.

## Consequences
- Visually crossing wires remain electrically independent by default.
- Moving, zooming, resizing, or rerouting a diagram cannot change electrical topology.
- Future simulation can consume a deterministic connectivity snapshot without depending on Minecraft rendering.
