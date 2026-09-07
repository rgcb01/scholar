# ADR 0172 - Plot Hit Testing Uses Pure Java Laid Out Geometry

## Status

Accepted

## Context

Mouse-driven plot editing needs to map visible title/axis labels, legend items, series geometry, and points back to semantic edit targets. Doing this directly in Minecraft UI code would duplicate plot geometry knowledge.

## Decision

`PlotHitTester` consumes `LaidOutPlot` and returns semantic `PlotEditTarget` values in pure Java. Minecraft input routing supplies document-local coordinates but does not calculate plot semantics.

## Alternatives Considered

- Hit-test directly inside `ScholarEditorScreen`.
- Recompute scientific coordinate transforms during mouse input.
- Store Minecraft screen hit boxes inside PlotBlock.

## Consequences

Plot mouse editing remains testable and independent from Minecraft. Existing layout metadata becomes the single source of visible plot geometry for both rendering and hit testing.
