# ADR 0166 - Scatter Series Render Visible Points With Deterministic Marker Shapes

## Status

Accepted

## Context

SCATTER series need discrete visible marks rather than connecting lines, and multiple series should not become distinguishable only by color.

## Decision

SCATTER series render only points inside the resolved plot range. Layout assigns a deterministic marker shape from a small derived cycle based on series index. Marker choice is presentation state and is not stored in `PlotDefinition`.

## Alternatives Considered

- Render every scatter series with identical points distinguished only by color.
- Store marker shape as authored semantic AST state in the first plot slice.
- Clamp out-of-range scatter points onto the plot border.

## Consequences

Scatter plots remain readable in grayscale/color-limited contexts and semantic plot data stays independent from first-slice presentation defaults. Authored marker controls can be added later without changing existing data-point semantics.
