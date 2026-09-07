# ADR 0195 - Electrical Component Kinds Own Stable Terminal Schemas

## Status

Accepted

## Context

Electrical connections need semantically stable terminals. Authoring arbitrary perimeter ports on every component instance would duplicate data and could let a resistor's terminal identity drift when the symbol rotates.

## Decision

Each `ElectricalComponentKind` owns a stable terminal schema with persistent `DiagramPortId` values and base normalized perimeter placements. `ElectricalComponent.ports()` is derived from kind, orientation, and bounds so existing M17 endpoint validation and connection references remain reusable.

Terminal IDs are compatibility contracts for a component kind. Rotation changes derived placement, never terminal identity.

## Alternatives Considered

- Store a free-form `List<DiagramPort>` on every electrical component instance.
- Reference terminals only by numeric index.
- Reference wires directly to screen coordinates.

## Consequences

Existing connections survive rotation and symbol-layout changes. Adding future multi-terminal devices remains possible without changing the generic endpoint model, but terminal IDs for an established kind must be treated as stable data contracts.
