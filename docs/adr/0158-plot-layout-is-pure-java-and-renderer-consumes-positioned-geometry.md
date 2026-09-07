# ADR 0158 - Plot Layout Is Pure Java And Renderer Consumes Positioned Geometry

## Status

Accepted

## Context

Scholar keeps scientific/document layout testable outside Minecraft. Plot layout will later own ranges, ticks, coordinate transforms, and series geometry.

## Decision

`PlotLayoutEngine` and laid-out plot values remain pure Java. The Minecraft renderer consumes positioned plot geometry and does not calculate scientific ranges, ticks, data transforms, or series layout.

## Alternatives Considered

- Compute plot geometry directly in `MinecraftDocumentRenderer`.
- Store Minecraft rendering types in the plot model.

## Consequences

Plot behavior remains deterministic and unit-testable, and future rendering/export backends can consume the same semantic/layout pipeline.
