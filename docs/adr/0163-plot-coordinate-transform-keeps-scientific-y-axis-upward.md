# ADR 0163 - Plot Coordinate Transform Keeps Scientific Y Axis Upward

## Status

Accepted

## Context

Document/screen Y coordinates increase downward while scientific plot Y values conventionally increase upward.

## Decision

The pure-Java `PlotCoordinateTransform` maps X minimum to the left and X maximum to the right, while mapping Y minimum to the bottom and Y maximum to the top of the plot area. Values outside the resolved range are not clamped; clipping belongs to later series geometry/rendering.

## Alternatives Considered

- Clamp values during coordinate transformation.
- Invert Y in the Minecraft renderer.
- Store screen-space points in PlotBlock.

## Consequences

Scientific coordinate semantics stay independent of Minecraft and future series clipping can be implemented separately from numeric transformation.
