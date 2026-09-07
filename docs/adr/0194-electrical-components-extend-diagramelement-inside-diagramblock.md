# ADR 0194 - Electrical Components Extend DiagramElement Inside DiagramBlock

## Status

Accepted

## Context

M17 was built so domain-specific scientific elements could reuse one canvas, selection, routing, editing, history, and clipboard foundation. Introducing a separate `ElectricalDiagramBlock` would duplicate those mechanics and weaken the reuse boundary just as the first real domain arrives.

## Decision

M18 adds an immutable `ElectricalComponent` that implements `DiagramElement` and lives inside the existing `DiagramDefinition` owned by `DiagramBlock`.

Electrical components may coexist with generic `DiagramNode` elements. M18 will evolve generic layout/editor code only where concrete polymorphism is required; it will not create a parallel electrical document/editor stack.

## Alternatives Considered

- Add a separate `ElectricalDiagramBlock` and electrical canvas model.
- Encode electrical components as labeled generic `DiagramNode` objects.
- Store electrical metadata in arbitrary string maps on generic nodes.

## Consequences

M18 gets real domain semantics while reusing proven M17 mechanics. Some M17 code that currently assumes `DiagramNode` will need targeted generalization to multiple `DiagramElement` implementations.
