# ADR 0025: Layout/Editor Mapping Boundary

## Status

Accepted

## Context

Read-only layout originally mapped `Document` to visual text runs without preserving source positions. Caret rendering needs to map a logical `DocumentPosition` to visual geometry while keeping pixels and Minecraft types out of the AST.

## Decision

Carry logical source ranges in Minecraft-independent layout output. Laid-out text runs include the source block index and logical character start/end offsets represented by that run.

Caret geometry is computed from `DocumentPosition`, `LaidOutDocument`, and the same `TextMeasurer` used for layout. Minecraft code only renders the resulting rectangle.

## Alternatives Considered

- Store visual caret or layout data in the AST.
- Compute caret positions only inside Minecraft rendering code.
- Delay source mapping until mouse hit-testing.

## Consequences

Caret rendering can stay independent of Minecraft and consistent with layout measurement. Layout output becomes slightly richer, and future mouse hit-testing has a natural place to start.
