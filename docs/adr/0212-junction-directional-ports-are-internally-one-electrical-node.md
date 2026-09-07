# ADR 0212 — Junction Directional Ports Are Internally One Electrical Node

## Status
Accepted for M18E.

## Decision
An `ElectricalJunction` exposes four directional ports (`left`, `right`, `top`, `bottom`) so the existing orthogonal router can preserve useful approach directions. The electrical resolver unions those four endpoints into one node.

## Consequences
- Routing remains direction-aware without adding a special center-port concept to generic M17.
- Multiple branches can meet at one explicit dot.
- The authored port chosen for a wire affects only routing approach, not electrical identity.
