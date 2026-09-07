# ADR 0185 - Electrical And Mechanical Domain Semantics Are Deferred Above M17 Foundation

## Status

Accepted

## Context

M17 exists specifically so later electrical and mechanical diagram features can share robust diagram mechanics. Trying to encode every future discipline in a universal M17 meta-model would add fields and abstractions before their semantics are understood.

## Decision

M17 owns only reusable diagram mechanics: canvas/coordinates, generic nodes, ports, connectivity, derived routing, layout, hit testing, embedded selection/editing, drag/history integration, and whole-block clipboard.

Electrical vocabulary and rules belong to M18. Mechanical vocabulary and rules belong to M19. M17 does not add resistor/capacitor/source fields, physical units, pin electrical types, gears, springs, constraints, forces, or simulation state merely to appear extensible.

Future domain-specific element families may extend the diagram model where concrete requirements justify it.

## Alternatives Considered

- Design a universal scientific-symbol schema during M17.
- Put electrical symbols directly into the first DiagramElement enum.
- Make all domain behavior arbitrary key/value metadata.
- Build M18 independently and duplicate editor/layout mechanics.

## Consequences

The foundation stays small and evidence-driven while preserving a clear reuse boundary. M18/M19 may require targeted evolution of M17, but those changes will be justified by real domain behavior rather than speculation.
