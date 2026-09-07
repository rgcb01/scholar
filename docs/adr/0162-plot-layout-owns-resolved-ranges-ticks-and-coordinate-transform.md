# ADR 0162 - Plot Layout Owns Resolved Ranges Ticks And Coordinate Transform

## Status

Accepted

## Context

M16B established that the Minecraft renderer consumes positioned plot geometry. M16C adds the scientific mapping required before series can be rendered.

## Decision

`PlotLayoutEngine` resolves explicit/automatic axis ranges, generates ticks, measures tick labels, establishes plot gutters, and creates the data-to-plot coordinate transform. `LaidOutPlot` carries these resolved values to the renderer.

## Alternatives Considered

- Compute ranges/ticks/transforms in `MinecraftDocumentRenderer`.
- Store resolved ticks in PlotDefinition.
- Let future series renderers recompute transforms independently.

## Consequences

M16D series rendering can consume one authoritative transform, and future non-Minecraft export renderers can reuse the same layout result.
