# ADR 0181 - First Diagram Element Is Generic Rectangular Node With Perimeter Ports

## Status

Accepted

## Context

M17 needs enough concrete element semantics to validate layout, connectivity, editing, and hit testing, but defining electrical or mechanical symbols inside the foundation would prematurely specialize the architecture.

## Decision

The first M17 element is a generic rectangular `DiagramNode` with local bounds, a plain-string label, and zero or more ports. Initial port placement uses one perimeter side (`LEFT`, `RIGHT`, `TOP`, `BOTTOM`) plus a finite normalized offset in `[0,1]` along that side.

M17 does not claim that all future scientific elements are rectangles. Later domain element families may add richer geometry while reusing the same diagram-level mechanics.

## Alternatives Considered

- Begin directly with resistor/capacitor electrical symbols.
- Create a universal shape language with arbitrary paths, polygons, rotations, and nested primitives now.
- Support free/interior anchors and arbitrary port vectors from the first slice.
- Treat every future symbol as a styled generic rectangle.

## Consequences

M17 gets a small testable vertical slice with useful port orientation for routing. Electrical pins map naturally onto the first placement model. Interior/free-form anchors, rotations, and richer shape semantics remain deferred until later domains provide evidence for them.
